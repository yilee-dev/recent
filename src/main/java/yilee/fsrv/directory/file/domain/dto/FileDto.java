package yilee.fsrv.directory.file.domain.dto;

import jakarta.persistence.*;
import lombok.Builder;
import yilee.fsrv.directory.file.domain.entity.FileArtifact;
import yilee.fsrv.directory.file.domain.entity.FileObject;
import yilee.fsrv.directory.file.domain.enums.FileType;
import yilee.fsrv.directory.folder.domain.entity.FolderObject;

import java.time.LocalDateTime;

public record FileDto (
    Long id,

    String originalFilename,

    Long fileSize,

    FileType fileType

) {
    public static FileDto from(FileObject fileObject) {
        return new FileDto(fileObject.getId(), fileObject.getOriginalFilename(), fileObject.getFileSize(), fileObject.getFileType());
    }
}
