package yilee.fsrv.directory.folder.domain.dto;

import java.util.List;

public record CursorResult<T>(
        List<T> items,
        Long nextCursor,
        boolean hasNextCursor
) {
}
