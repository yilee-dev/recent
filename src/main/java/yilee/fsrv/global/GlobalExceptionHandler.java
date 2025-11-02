package yilee.fsrv.global;

import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.aspectj.weaver.ast.Not;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import yilee.fsrv.directory.folder.exception.DomainException;
import yilee.fsrv.directory.folder.exception.FolderAlreadyDeletedException;
import yilee.fsrv.directory.folder.exception.FolderNotFoundException;
import yilee.fsrv.directory.folder.exception.NotEnoughPermission;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final MessageSource messageSource;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> methodArgumentNotValidException(MethodArgumentNotValidException exception, Locale locale) {
        var errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> {
                            Map<String, Object> m = new LinkedHashMap<>();
                            m.put("field", fieldError.getField());
                            m.put("code", fieldError.getCode());
                            m.put("rejectedValue", fieldError.getRejectedValue());
                            m.put("status", 400);
                            m.put("errors", resolve(fieldError, locale));
                            return m;
                }).toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "VALIDATION_FAILED");
        body.put("errors", errors);

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(NotEnoughPermission.class)
    public ResponseEntity<?> notEnoughPermission(NotEnoughPermission exception) {
        int status = "LOGIN_REQUIRED".equals(exception.getMessage()) ? 401 : 403;
        return ResponseEntity.status(status)
                .body(Map.of("errors", "NOT_ENOUGH_PERMISSION",
                        "message", exception.getMessage()));
    }

    @ExceptionHandler(FolderNotFoundException.class)
    public ResponseEntity<?> folderNotFoundException(FolderNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("errors", "NOT_FOUND_FOLDER", "message", exception.getMessage()));
    }

    @ExceptionHandler(FolderAlreadyDeletedException.class)
    public ResponseEntity<?> folderAlreadyDeletedException(FolderAlreadyDeletedException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("errors", "FOLDER_DELETED", "message", exception.getMessage()));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<?> domainException(DomainException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("errors", "DOMAIN_ERROR", "message", exception.getMessage()));
    }

    public String resolve(FieldError fieldError, Locale locale) {
        Locale validatedLocale = locale != null ? locale : Locale.KOREA;
        String defaultMessage = fieldError.getDefaultMessage();

        if (defaultMessage.startsWith("{") && defaultMessage.endsWith("}")) {
            try {
                String key = defaultMessage.substring(1, defaultMessage.length() - 1);
                return messageSource.getMessage(key, fieldError.getArguments(), validatedLocale);
            } catch (Exception ignore) {}
        }

        return defaultMessage;
    }
}
