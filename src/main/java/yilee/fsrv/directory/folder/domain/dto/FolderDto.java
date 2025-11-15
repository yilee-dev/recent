package yilee.fsrv.directory.folder.domain.dto;

import lombok.Data;
import lombok.Getter;
import yilee.fsrv.directory.folder.domain.entity.FolderObject;
import yilee.fsrv.directory.folder.domain.enums.FolderScope;

import java.time.LocalDateTime;

public record FolderDto(
        Long id,
        Long parentId,
        Long ownerId,
        String name,
        FolderScope scope,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt,
        boolean deleted,
        long directFileCount,
        long directFolderCount  // ✅ 추가
) {
    public static FolderDto ofNew(FolderObject f) {
        return new FolderDto(
                f.getId(),
                f.getParent() != null ? f.getParent().getId() : null,
                f.getOwnerId(),
                f.getName(),
                f.getScope(),
                f.getCreatedAt(),
                f.getUpdatedAt(),
                f.getDeletedAt(),
                f.isDeleted(),
                0L,
                0L
        );
    }
}