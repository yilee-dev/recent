package yilee.fsrv.directory.file.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import yilee.fsrv.directory.acl.helper.PermissionChecker;
import yilee.fsrv.directory.file.domain.dto.FileDto;
import yilee.fsrv.directory.file.domain.dto.FileSummaryDto;
import yilee.fsrv.directory.file.domain.dto.UploadFileRequest;
import yilee.fsrv.directory.file.domain.entity.FileObject;
import yilee.fsrv.directory.file.domain.enums.FileType;
import yilee.fsrv.directory.file.repository.FileRepository;
import yilee.fsrv.directory.file.service.FileService;
import yilee.fsrv.directory.file.service.FileSummaryService;
import yilee.fsrv.directory.folder.domain.dto.CursorPageResponse;
import yilee.fsrv.directory.folder.domain.dto.CursorResult;
import yilee.fsrv.directory.folder.exception.DomainException;
import yilee.fsrv.directory.folder.exception.InvalidFolderCreatorException;
import yilee.fsrv.directory.folder.exception.NotEnoughPermission;
import yilee.fsrv.infra.storage.StorageService;
import yilee.fsrv.login.domain.dto.MemberDto;
import yilee.fsrv.login.domain.entity.Member;
import yilee.fsrv.login.repository.MemberRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Set;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;
    private final FileRepository fileRepository;
    private final StorageService storageService;
    private final PermissionChecker permissionChecker;
    private final MemberRepository memberRepository;

    @GetMapping("/summary")
    public FileSummaryDto summary(
            @RequestParam(required = false) Long folderId, // 루트면 null
            @AuthenticationPrincipal(errorOnInvalidType = false) MemberDto principal
    ) {
        Long viewerId = principal != null ? principal.getId() : null;
        return fileService.summarize(folderId, viewerId);
    }


//    @GetMapping
//    public CursorPageResponse<FileDto> getFiles(
//            @RequestParam Long folderId,
//            @RequestParam(required = false) Long cursor,
//            @RequestParam(defaultValue = "20") int size,
//            @RequestParam(required = false) Set<FileType> types,
//            @AuthenticationPrincipal(errorOnInvalidType = false) MemberDto principal
//    ) {
//        Long viewerId = principal != null ? principal.getId() : null;
//        CursorResult<FileDto> r = fileRepository.findByFolderVisible(folderId, viewerId, cursor, size, types);
//        return new CursorPageResponse<>(r.items(), r.nextCursor(), r.hasNextCursor(), r.items().size());
//    }
    @GetMapping
    public CursorPageResponse<FileDto> list(
        @RequestParam(required = false) Long folderId,
        @RequestParam(required = false) FileType fileType,   // ✅ enum으로
        @RequestParam(required = false, name = "fileCursor") Long fileCursor,
        @RequestParam(defaultValue = "20", name = "fileSize") int fileSize,
        @AuthenticationPrincipal(errorOnInvalidType = false) MemberDto principal
    ) {
        Long viewerId = principal != null ? principal.getId() : null;
        var cr = fileService.listVisibleByType(folderId, viewerId, fileType, fileCursor, fileSize);
        return new CursorPageResponse<>(cr.items(), cr.nextCursor(), cr.hasNextCursor(), cr.items().size());
    }


    @PostMapping
    public FileDto upload(
            UploadFileRequest uploadFileRequest,
            @AuthenticationPrincipal(errorOnInvalidType = false) MemberDto principal
            ) {
        Long uploaderId = principal != null ? principal.getId() : null;
        return fileService.upload(uploadFileRequest, uploaderId);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id,
                                             @AuthenticationPrincipal(errorOnInvalidType = false)MemberDto principal) {
        FileObject f = fileRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Not found file: " + id));
        if (f.isDeleted()) throw new DomainException("FILE_DELETED");

        if (!f.getFolder().isSharedFolder()) {
            Long viewerId = principal != null ? principal.getId() : null;
            if (viewerId == null) throw new NotEnoughPermission("LOGIN_REQUIRED");

            Member m = memberRepository.findById(viewerId)
                    .orElseThrow(() -> new InvalidFolderCreatorException("INVALID_CREATOR"));

            if (!permissionChecker.canRead(m, f.getFolder())) {
                throw new NotEnoughPermission("Not enough Read permission");
            }
        }

        Path abs = storageService.getRootPath().resolve(f.getStoragePath()).normalize();
        if (!Files.exists(abs)) throw new NoSuchElementException("물리 파일 없음");

        Resource res = new FileSystemResource(abs);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + f.getOriginalFilename() + "\"")
                .contentType(MediaType.parseMediaType(
                        f.getContentType() != null ? f.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .contentLength(f.getFileSize() != null ? f.getFileSize() : abs.toFile().length())
                .body(res);
    }
}
