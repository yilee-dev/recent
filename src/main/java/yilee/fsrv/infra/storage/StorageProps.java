package yilee.fsrv.infra.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "file.store")
@Getter @Setter
public class StorageProps {

    private Path root;
}
