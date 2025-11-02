package yilee.fsrv.directory.folder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import yilee.fsrv.directory.folder.domain.dto.*;
import yilee.fsrv.directory.folder.repository.FolderRepository;
import yilee.fsrv.directory.folder.service.FolderService;
import yilee.fsrv.login.domain.dto.MemberDto;

@Slf4j
@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;
    private final FolderRepository folderRepository;

    @PostMapping
    public CreateFolderResponse createFolder(@Valid @RequestBody CreateFolderRequest request, @AuthenticationPrincipal(errorOnInvalidType = false) MemberDto principal) {
        Long creatorId = principal != null ? principal.getId() : null;
        return folderService.createFolder(request, creatorId);
    }

    @GetMapping
    public CursorPageResponse<FolderDto> listRootFolders(
            @RequestParam(required = false) Long folderCursor,
            @RequestParam(defaultValue = "20") int folderSize,
            @AuthenticationPrincipal(errorOnInvalidType = false) MemberDto principal
    ) {
        log.info("=================principal = {}", principal);
        Long viewerId = principal != null ? principal.getId() : null;
        CursorResult<FolderDto> child = folderRepository.findChild(null, viewerId, folderCursor, folderSize);
        return new CursorPageResponse<>(child.items(), child.nextCursor(), child.hasNextCursor(), child.items().size());
    }

    @GetMapping("/{folderId}")
    public CursorPageResponse<FolderDto> getDirectoryOverview(
            @PathVariable Long folderId,
            @RequestParam(required = false) Long folderCursor,
            @RequestParam(defaultValue = "20") int folderSize,
            @AuthenticationPrincipal(errorOnInvalidType = false) MemberDto principal
    ) {
        log.info("=================principal = {}", principal);
        Long viewerId = principal != null ? principal.getId() : null;
        return folderService.getFolders(folderId, viewerId, folderCursor, folderSize);
    }
}
