package yilee.fsrv.directory.folder.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import yilee.fsrv.directory.acl.entity.FolderAcl;
import yilee.fsrv.directory.acl.enums.FolderPermission;
import yilee.fsrv.directory.acl.helper.PermissionChecker;
import yilee.fsrv.directory.acl.repository.FolderAclRepository;
import yilee.fsrv.directory.folder.domain.dto.*;
import yilee.fsrv.directory.folder.domain.entity.FolderObject;
import yilee.fsrv.directory.folder.domain.enums.FolderScope;
import yilee.fsrv.directory.folder.exception.*;
import yilee.fsrv.directory.folder.repository.FolderClosureRepository;
import yilee.fsrv.directory.folder.repository.FolderMetricsRepository;
import yilee.fsrv.directory.folder.repository.FolderRepository;
import yilee.fsrv.infra.storage.StorageService;
import yilee.fsrv.login.domain.entity.Member;
import yilee.fsrv.login.domain.enums.MemberRole;
import yilee.fsrv.login.repository.MemberRepository;

import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FolderService {

    private final FolderRepository folderRepository;
    private final FolderAclRepository folderAclRepository;
    private final PermissionChecker permissionChecker;
    private final MemberRepository memberRepository;
    private final FolderClosureRepository folderClosureRepository;
    private final FolderMetricsRepository folderMetricsRepository;
    private final StorageService storageService;

    @Transactional
    public CreateFolderResponse createFolder(CreateFolderRequest req, Long creatorId) {
        if (creatorId == null) {
            throw new NotEnoughPermission("Folder creation requires MANAGER or ADMIN permission");
        }

        Member creator = memberRepository.findById(creatorId)
                .orElseThrow(() -> new InvalidFolderCreatorException("creatorId=" + creatorId));

        FolderObject parent = (req.parentId() == null) ? null :
                folderRepository.findById(req.parentId())
                        .orElseThrow(() -> new FolderNotFoundException("parentId=" + req.parentId()));

        if (parent == null) {
            boolean allowed = creator.getMemberRoleList().contains(MemberRole.MANAGER);
            if (!allowed) throw new NotEnoughPermission("Not enough manager permission");
        } else {
            if (parent.isDeleted()) throw new FolderAlreadyDeletedException("Not exists parent folder");
            if (!permissionChecker.canWrite(creator, parent)) {
                throw new NotEnoughPermission("Not enough write permission");
            }
        }

        FolderScope scope = req.scope() == null ? FolderScope.SHARED : req.scope();
        FolderObject folder;
        try {
            folder = folderRepository.save(FolderObject.builder()
                    .name(req.name())
                    .parent(parent)
                    .scope(scope)
                    .ownerId(creator.getId())
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateFolderNameException("uk_parent_name: parentId,name");
        }

        FolderAcl ownerAcl = FolderAcl.builder()
                .folder(folder)
                .subjectId(creator.getId())
                .permission(FolderPermission.WRITE)
                .build();

        folderAclRepository.save(ownerAcl);

        // closure/metrics
        folderClosureRepository.insertSelf(folder.getId());
        folderMetricsRepository.initIfAbsent(folder.getId());
        if (parent != null) {
            folderClosureRepository.linkFromParentAncestors(parent.getId(), folder.getId());
            folderMetricsRepository.incDirectFolder(parent.getId());
            folderMetricsRepository.incDescFolderAncestorsOf(folder.getId());
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { storageService.createFolder(folder); }
        });

        return new CreateFolderResponse(FolderDto.ofNew(folder));
    }

    public CursorPageResponse<FolderDto> getFolders(Long folderId, Long viewerId, Long folderCursor, int folderSize) {
        FolderObject parent = folderRepository.findById(folderId)
                .orElseThrow(() -> new FolderNotFoundException("folderId=" + folderId));

        if (parent.isDeleted()) throw new FolderAlreadyDeletedException("FOLDER_DELETED");

        if (parent.getScope() == FolderScope.PRIVATE) {
            if (viewerId == null) {
                throw new NotEnoughPermission("LOGIN_REQUIRED");
            }

            Member member = memberRepository.findById(viewerId)
                    .orElseThrow(() -> new NoSuchElementException("Not found member=" + viewerId));

            if (!permissionChecker.canRead(member, parent)) {
                throw new NotEnoughPermission("Not enough permission");
            }
        }

        CursorResult<FolderDto> child = folderRepository.findChild(folderId, viewerId, folderCursor, folderSize);

        return new CursorPageResponse<>(child.items(), child.nextCursor(), child.hasNextCursor(), child.items().size());
    }
}
