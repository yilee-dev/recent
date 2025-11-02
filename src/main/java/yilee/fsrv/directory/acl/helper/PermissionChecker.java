package yilee.fsrv.directory.acl.helper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import yilee.fsrv.directory.acl.enums.FolderPermission;
import yilee.fsrv.directory.acl.repository.FolderAclRepository;
import yilee.fsrv.directory.folder.domain.entity.FolderObject;
import yilee.fsrv.directory.folder.domain.enums.FolderScope;
import yilee.fsrv.login.domain.entity.Member;
import yilee.fsrv.login.domain.enums.MemberRole;

import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PermissionChecker {
    private final FolderAclRepository folderAclRepository;

    public boolean hasPermission(Member member, FolderObject folder, FolderPermission required) {
        if (folder.isDeleted()) return false;

        if (member == null) {
            return required == FolderPermission.READ && folder.getScope() == FolderScope.SHARED;
        }

        if (Objects.equals(folder.getOwnerId(), member.getId())) {
            return true;
        }

        if (folder.getScope() == FolderScope.SHARED && required == FolderPermission.READ) return true;

        var roles = member.getMemberRoleList();
        boolean isManager = roles.contains(MemberRole.MANAGER);
        boolean isAdmin = roles.contains(MemberRole.ADMIN);

        switch (required) {
            case READ -> {
                return existsAclOrHigher(folder.getId(), member.getId(), required);
            }
            case WRITE -> {
                if (isManager || isAdmin) return true;
                return existsAclOrHigher(folder.getId(), member.getId(), required);
            }
            case DELETE -> {
                if (isAdmin) return true;
                return existsAclOrHigher(folder.getId(), member.getId(), required);
            }
            default -> {
                return existsAclOrHigher(folder.getId(), member.getId(), required);
            }
        }
    }

    private boolean existsAclOrHigher(Long folderId, Long memberId, FolderPermission required) {
        Set<FolderPermission> acceptable = PermissionResolver.requireOrHigher(required);
        return folderAclRepository.existsByFolderIdAndSubjectIdAndPermissionIn(folderId, memberId, acceptable);
    }

    public boolean canRead(Member m, FolderObject f) {
        return hasPermission(m, f, FolderPermission.READ);
    }

    public boolean canWrite(Member m, FolderObject f) {
        return hasPermission(m, f, FolderPermission.WRITE);
    }

    public boolean canDelete(Member m, FolderObject f) {
        return hasPermission(m, f, FolderPermission.DELETE);
    }
}
