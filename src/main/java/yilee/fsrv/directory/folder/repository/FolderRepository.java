package yilee.fsrv.directory.folder.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yilee.fsrv.directory.folder.domain.entity.FolderObject;
import yilee.fsrv.directory.folder.domain.enums.FolderScope;

public interface FolderRepository extends JpaRepository<FolderObject, Long>, FolderQueryRepository {
    boolean existsByParentIsNullAndNameAndScope(String shared, FolderScope folderScope);
}
