package yilee.fsrv.directory.file.domain.dto;

public record FileSummaryDto(
        long totalCount, long totalSize,
        long normalCount, long normalSize,
        long installCount, long installSize,
        long uninstallCount, long uninstallSize
) { }
