package yilee.fsrv.directory.folder.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import yilee.fsrv.directory.folder.domain.dto.CreateFolderRequest;
import yilee.fsrv.directory.folder.domain.enums.FolderScope;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FolderServiceTest {

    @Autowired
    FolderService folderService;

    @Test
    @Commit
    @Transactional
    void init() {

        for (int i = 0; i < 45; i++) {
            CreateFolderRequest sharedFolder = new CreateFolderRequest(null, "shared" + i, FolderScope.SHARED);
            CreateFolderRequest privateFolder = new CreateFolderRequest(null, "private" + i, FolderScope.PRIVATE);

            folderService.createFolder(sharedFolder, 1L);
            folderService.createFolder(privateFolder, 1L);
        }
    }

}