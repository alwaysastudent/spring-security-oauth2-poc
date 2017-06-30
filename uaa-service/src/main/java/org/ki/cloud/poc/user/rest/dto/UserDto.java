package org.ki.cloud.poc.user.rest.dto;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Email;
import org.ki.cloud.poc.user.data.entity.type.UserRoleType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
/**
 * 
 * @author Karthik Iyer
 *
 */
@Data
@NoArgsConstructor
@JsonRootName("user")
@EqualsAndHashCode
@JsonIgnoreProperties(value = "password", allowSetters = true)
public class UserDto {

	@Email(message = "Not a well formed email address")
	@NotNull(message = "Email empty")
	private String email;
	@NotNull(message = "Missing mandatory field")
	private String name, phone, password;
	
	@JsonIgnoreProperties(value = "guid", allowGetters = true)
	private String guid;

	@NotNull
	private boolean active;

	@NotNull(message = "Roles missing")
	private UserRoleType[] roles;

}
