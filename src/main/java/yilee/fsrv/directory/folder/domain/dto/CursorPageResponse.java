package yilee.fsrv.directory.folder.domain.dto;

import java.util.List;

public record CursorPageResponse<T>(
        List<T> items,
        Long nextCursor,
        boolean hasNextCursor,
        int size
) {
}
