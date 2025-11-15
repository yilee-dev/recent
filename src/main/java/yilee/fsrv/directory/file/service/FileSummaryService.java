package yilee.fsrv.directory.file.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import yilee.fsrv.directory.file.domain.dto.FileSummaryDto;
import yilee.fsrv.directory.file.repository.FileRepository;

@Service
@RequiredArgsConstructor
public class FileSummaryService {
    private final FileRepository fileRepository;

}
