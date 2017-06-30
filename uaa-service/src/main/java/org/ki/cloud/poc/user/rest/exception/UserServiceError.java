package org.ki.cloud.poc.user.rest.exception;

import org.springframework.http.HttpStatus;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author Karthik Iyer
 *
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class UserServiceError extends RuntimeException {
  private static final long serialVersionUID = -6018610119996386568L;

  private final String error;

  private final HttpStatus status;

  public UserServiceError(String e, HttpStatus s) {
    super(e);
    this.error = e;
    this.status = s;
  }

}
