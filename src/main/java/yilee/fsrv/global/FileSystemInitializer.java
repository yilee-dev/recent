package yilee.fsrv.global;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import yilee.fsrv.infra.storage.StorageService;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileSystemInitializer implements ApplicationRunner {

    private final StorageService storageService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Path rootPath = storageService.getRootPath();
        if (!Files.exists(rootPath)) {
            Files.createDirectories(rootPath);
            log.info("Root directory created: {}", rootPath);
        } else {
            log.info("Root directory already exists: {}", rootPath);
        }
    }
}
