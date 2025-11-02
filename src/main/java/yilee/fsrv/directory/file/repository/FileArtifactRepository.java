package yilee.fsrv.directory.file.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yilee.fsrv.directory.file.domain.entity.FileArtifact;

public interface FileArtifactRepository extends JpaRepository<FileArtifact, Long> {
}
