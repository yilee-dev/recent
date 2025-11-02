package yilee.fsrv.directory.file.repository;
import io.micrometer.observation.ObservationFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yilee.fsrv.directory.file.domain.entity.FileObject;

import java.util.Optional;

public interface FileRepository extends JpaRepository<FileObject, Long>, FileQueryRepository {
    @Query("select max(f.version) from FileObject f where f.folder.id = :folderId and f.originalFilename = :name")
    Optional<Integer> findMaxVersion(@Param("folderId") Long folderId, @Param("name") String originalName);
}
