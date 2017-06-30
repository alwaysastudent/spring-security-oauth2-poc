package org.ki.cloud.poc.user.rest.dto;

import com.fasterxml.jackson.annotation.JsonRootName;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author Karthik Iyer
 *
 */
@JsonRootName("error")
@Data
@NoArgsConstructor
public class ErrorDto {

  private String message;

  public ErrorDto(String message) {
    this.message = message;
  }

}
