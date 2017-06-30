package org.ki.cloud.poc.user.rest.exception;

import org.ki.cloud.poc.user.rest.dto.ErrorDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Karthik Iyer
 *
 */
@Slf4j
@RestControllerAdvice
public class UserServiceRestControllerAdvice {

  @ExceptionHandler({ UserServiceError.class })
  public ResponseEntity<ErrorDto> handleCheckedErrors(UserServiceError e) {
    log.info("Handling the error, {}", e.getMessage());
    return new ResponseEntity<ErrorDto>(new ErrorDto(e.getError()), e.getStatus());
  }

  @ExceptionHandler({ MethodArgumentNotValidException.class })
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorDto handleArgValidationException(MethodArgumentNotValidException e) {
    String message = e.getBindingResult().getFieldError().getDefaultMessage();
    return new ErrorDto(String.format("Validation failed, %s", message));
  }

  @ExceptionHandler({ AccessDeniedException.class })
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ErrorDto handleAccessDeniedException(AccessDeniedException e) {
    return new ErrorDto(
        String.format("Unauthorized access requested, %s", e.getMessage()));
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ErrorDto handleDeserializationErrors(HttpMessageNotReadableException e) {

    Throwable mostSpecificCause = e.getMostSpecificCause();
    if (mostSpecificCause != null) {
      String message = mostSpecificCause.getMessage();
      return new ErrorDto("Bad request, " + message);
    }
    return new ErrorDto(e.getMessage());
  }

  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(value = Exception.class)
  public ErrorDto handleUnCheckedErrors(Exception e) {
    log.error("Exception Occured ", e);
    return new ErrorDto("Unexpected error happened!");
  }

}
