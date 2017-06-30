package org.ki.cloud.poc.user.data.entity.converter;

import java.util.stream.Stream;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.ki.cloud.poc.user.data.entity.type.UserRoleType;
import org.ki.cloud.poc.user.rest.exception.UserServiceError;
import org.springframework.http.HttpStatus;
/**
 * 
 * @author Karthik Iyer
 *
 */
@Converter(autoApply = false)
public class UserRoleConverter implements AttributeConverter<UserRoleType, String> {

	@Override
	public String convertToDatabaseColumn(UserRoleType role) {
		return role.toString();
	}

	@Override
	public UserRoleType convertToEntityAttribute(String role) {
		return Stream.of(UserRoleType.values()).filter(s -> s.toString().equals(role))
				.findFirst().orElseThrow(() -> new UserServiceError(
						String.format("Unsupported role type %s.", role), HttpStatus.BAD_REQUEST));
	}
}