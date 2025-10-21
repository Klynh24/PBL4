package pbl.backend.kchi.helper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.Map;
import java.util.HashMap;
import pbl.backend.kchi.resources.ErrorResource;


@ControllerAdvice//được gọi khi có request bắn vào controllers
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleVlidException(MethodArgumentNotValidException exception) {

        Map<String, String> errors = new HashMap<>();
        exception.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName,errorMessage);
        });

        ErrorResource errorResource = new ErrorResource("Có vấn đề xảy ra trong quá trình kiểm tra dữ liệu", errors);
        return new ResponseEntity<>(errorResource, HttpStatus.BAD_REQUEST);
    }
}
