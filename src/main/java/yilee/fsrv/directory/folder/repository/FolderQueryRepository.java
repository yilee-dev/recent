package yilee.fsrv.directory.folder.repository;

import yilee.fsrv.directory.folder.domain.dto.CursorPageResponse;
import yilee.fsrv.directory.folder.domain.dto.CursorResult;
import yilee.fsrv.directory.folder.domain.dto.FolderDto;

public interface FolderQueryRepository {
    CursorResult<FolderDto> findChild(Long folderId, Long viewerId, Long nextCursor, int size);
}
