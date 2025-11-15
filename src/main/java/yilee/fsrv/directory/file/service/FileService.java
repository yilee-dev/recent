package yilee.fsrv.directory.file.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import yilee.fsrv.directory.acl.enums.FolderPermission;
import yilee.fsrv.directory.acl.helper.PermissionChecker;
import yilee.fsrv.directory.file.domain.dto.FileDto;
import yilee.fsrv.directory.file.domain.dto.FileSummaryDto;
import yilee.fsrv.directory.file.domain.dto.UploadFileRequest;
import yilee.fsrv.directory.file.domain.entity.FileArtifact;
import yilee.fsrv.directory.file.domain.entity.FileObject;
import yilee.fsrv.directory.file.domain.enums.FileType;
import yilee.fsrv.directory.file.repository.FileArtifactRepository;
import yilee.fsrv.directory.file.repository.FileRepository;
import yilee.fsrv.directory.folder.domain.dto.CursorResult;
import yilee.fsrv.directory.folder.domain.entity.FolderObject;
import yilee.fsrv.directory.folder.domain.enums.FolderScope;
import yilee.fsrv.directory.folder.exception.*;
import yilee.fsrv.directory.folder.repository.FolderMetricsRepository;
import yilee.fsrv.directory.folder.repository.FolderRepository;
import yilee.fsrv.infra.storage.StorageService;
import yilee.fsrv.infra.storage.exception.CreateStorageException;
import yilee.fsrv.login.domain.entity.Member;
import yilee.fsrv.login.repository.MemberRepository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FileService {

    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;
    private final MemberRepository memberRepository;
    private final FolderMetricsRepository metricsRepository;
    private final PermissionChecker permissionChecker;
    private final StorageService storageService;
    private final FileArtifactRepository fileArtifactRepository;

    public FileSummaryDto summarize(Long folderId, Long viewerId) {
        return fileRepository.summarizeVisibleJPQL(
                folderId,
                viewerId,
                FileType.NORMAL, FileType.INSTALL, FileType.UNINSTALL,
                Arrays.asList(FolderPermission.READ, FolderPermission.WRITE)
        );
    }

    public CursorResult<FileDto> listVisibleByType(Long folderId, Long viewerId,
                                                   FileType fileType, Long cursorId, int size) {
        var list = fileRepository.findVisibleByTypeJPQL(
                folderId,
                viewerId,
                fileType,
                cursorId,
                Arrays.asList(FolderPermission.READ, FolderPermission.WRITE),
                PageRequest.of(0, size + 1)
        );

        boolean hasNext = list.size() > size;
        if (hasNext) list = list.subList(0, size);

        Long next = list.isEmpty() ? null : list.get(list.size() - 1).getId();
        List<FileDto> items = list.stream().map(FileDto::from).collect(Collectors.toList());
        return new CursorResult<>(items, next, hasNext);
    }
//
//    @Transactional(readOnly = true)
//    public FileSummaryDto summarize(Long folderId, Long viewerId) {
//        return fileRepository.summarizeVisibleJPQL(
//                folderId, viewerId,
//                FileType.NORMAL, FileType.INSTALL, FileType.UNINSTALL,
//                FolderScope.SHARED,
//                Arrays.asList(FolderPermission.READ, FolderPermission.WRITE)
//        );
//    }
//
//    @Transactional(readOnly = true)
//    public CursorResult<FileDto> listVisibleByType(Long folderId, Long viewerId,
//                                                   FileType fileType, Long cursorId, int size) {
//
//        Collection<FolderPermission> perms =
//                Arrays.asList(FolderPermission.READ, FolderPermission.WRITE);
//        PageRequest pageable = PageRequest.of(0, size + 1);
//
//        var list = fileRepository.findVisibleByTypeJPQL(
//                folderId,
//                viewerId,
//                fileType,
//                cursorId,
//                FolderScope.SHARED,
//                perms,
//                pageable
//        );
//
//        boolean hasNext = list.size() > size;
//        if (hasNext) list = list.subList(0, size);
//
//        Long next = list.isEmpty() ? null : list.get(list.size() - 1).getId();
//        var items = list.stream().map(FileDto::from).collect(Collectors.toList());
//        return new CursorResult<>(items, next, hasNext);
//    }

    @Transactional
    public FileDto upload(UploadFileRequest request, Long uploaderId) {
        FolderObject folder = folderRepository.findById(request.folderId())
                .orElseThrow(() -> new FolderNotFoundException("folderId=" + request.folderId()));
        if (folder.isDeleted()) throw new FolderAlreadyDeletedException("FOLDER_DELETED");

        Member uploader = memberRepository.findById(uploaderId)
                .orElseThrow(() -> new InvalidFolderCreatorException("uploaderId=" + uploaderId));

        if (!permissionChecker.canWrite(uploader, folder)) {
            throw new NotEnoughPermission("Not enough write permission");
        }

        MultipartFile file = request.file();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("EMPTY_FILE");
        }

        String originalName = file.getOriginalFilename();
        String ext = extractExt(originalName);
        String contentType = coalesce(file.getContentType(), guessContentType(originalName));

        // 2) 버전 계산 (유니크 충돌 방지)
        int nextVersion = fileRepository.findMaxVersion(folder.getId(), originalName)
                .map(v -> v + 1)
                .orElse(1);

        // 3) 랜덤 저장 파일명 생성 및 물리 저장
        String storeFilename = storageService.generateStoreName(originalName);
        StorageService.StoredFile stored;
        try (InputStream in = file.getInputStream()) {
            stored = storageService.saveFileUnderFolder(folder, in, storeFilename);
        } catch (IOException e) {
            throw new CreateStorageException("Create storage exception", e);
        }

        FileArtifact artifact = null;
        if (request.fileType() != FileType.NORMAL) {
            artifact = fileArtifactRepository.save(FileArtifact.builder()
                    .platform(request.platform())
                    .processName(request.processName())
                    .installPath(request.installPath())
                    .commandLine(request.commandLine())
                    .downloadUrl(request.downloadUrl())
                    .checksumSha256(stored.sha256())
                    .build());
        }

        FileObject fo;
        try {
            fo = fileRepository.save(FileObject.builder()
                    .folder(folder)
                    .ownerId(uploader.getId())
                    .originalFilename(originalName)
                    .storeFilename(storeFilename)
                    .extension(ext)
                    .checksumSha256(stored.sha256())
                    .storagePath(stored.relativePath())
                    .fileSize(stored.size())
                    .contentType(contentType)
                    .fileType(request.fileType())
                    .version(nextVersion)
                    .uploadedAt(LocalDateTime.now())
                    .artifact(artifact)
                    .build());
        } catch (RuntimeException e) {
            try {
                Files.deleteIfExists(storageService.getRootPath().resolve(stored.relativePath()));
            } catch (Exception ignore) {}
            throw e;
        }

        // 6) 메트릭 반영
        metricsRepository.incDirectFile(folder.getId(), stored.size());
        metricsRepository.incDescFileAncestorsOf(folder.getId(), stored.size());

        return new FileDto(fo.getId(), fo.getOriginalFilename(), fo.getFileSize(), fo.getFileType());
    }

    private String extractExt(String name) {
        int p = (name == null) ? -1 : name.lastIndexOf('.');
        return p > -1 ? name.substring(p + 1) : null;
    }

    private static String guessContentType(String filename) {
        try {
            return filename != null ? Files.probeContentType(Path.of(filename)) : null;
        } catch (IOException e) {
            return null;
        }
    }

    private static <T> T coalesce(T a, T b) { return a != null ? a : b; }
}
