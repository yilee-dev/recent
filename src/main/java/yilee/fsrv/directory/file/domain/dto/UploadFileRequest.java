package yilee.fsrv.directory.file.domain.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;
import yilee.fsrv.directory.file.domain.enums.FileType;

public record UploadFileRequest(
        @Nullable Long folderId,
        FileType fileType,
        String platform,
        String processName,
        String installPath,
        String commandLine,
        String downloadUrl,
        MultipartFile file
        ) {
}
