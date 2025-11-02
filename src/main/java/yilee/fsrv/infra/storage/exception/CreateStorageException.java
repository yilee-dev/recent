package yilee.fsrv.infra.storage.exception;

public class CreateStorageException extends RuntimeException {
    public CreateStorageException(String message) {
        super(message);
    }

    public CreateStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
