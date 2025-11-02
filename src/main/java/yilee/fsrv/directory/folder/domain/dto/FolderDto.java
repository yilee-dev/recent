package yilee.fsrv.directory.folder.domain.dto;

import lombok.Data;
import lombok.Getter;
import yilee.fsrv.directory.folder.domain.enums.FolderScope;

import java.time.LocalDateTime;

public record FolderDto (
        Long id,
        Long parentId,
        Long ownerId,
        String name,
        FolderScope scope,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt,
        Boolean isDeleted
) {
}
