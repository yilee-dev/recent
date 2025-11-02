package yilee.fsrv.directory.folder.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Service;
import yilee.fsrv.directory.folder.domain.enums.FolderScope;

public record CreateFolderRequest(
        Long parentId,
        @NotBlank(message = "{NotBlank.folder.name}")
        @Size(max = 50) String name,
        @NotNull(message = "{NotNull.folder.scope}")FolderScope scope) {
}
