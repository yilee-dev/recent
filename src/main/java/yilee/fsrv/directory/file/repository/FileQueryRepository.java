package yilee.fsrv.directory.file.repository;

import jakarta.annotation.Nullable;
import yilee.fsrv.directory.file.domain.dto.FileDto;
import yilee.fsrv.directory.file.domain.enums.FileType;
import yilee.fsrv.directory.folder.domain.dto.CursorResult;

import java.util.Set;

public interface FileQueryRepository {
    CursorResult<FileDto> findByFolderVisible(Long folderId, Long viewerId, Long cursor, int size, @Nullable Set<FileType> types);
}
