package org.ki.cloud.poc.user.service;

import java.util.Optional;

import org.ki.cloud.poc.user.data.UserRepository;
import org.ki.cloud.poc.user.data.entity.UserEntity;
import org.ki.cloud.poc.user.rest.dto.UserDto;
import org.ki.cloud.poc.user.rest.dto.mapper.UserMapper;
import org.ki.cloud.poc.user.rest.exception.UserServiceError;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
/**
 * 
 * @author Karthik Iyer
 *
 */
@Service
public class UserService {

	private final UserRepository repository;

	private final UserMapper mapper = UserMapper.INSTANCE;

	public UserService(UserRepository repository) {
		this.repository = repository;
	}

	public UserDto getUser(String userName) {
		return mapper.entityToDto(repository.findByUsername(userName).orElseThrow(
				() -> new UserServiceError("User Not Found", HttpStatus.NOT_FOUND)));
	}

	public boolean createUser(final UserDto userDto) {
		Optional<UserEntity> optionalUser = repository.findByUsername(userDto.getEmail());
		optionalUser.ifPresent(s -> {
			throw new UserServiceError("User Already Exist", HttpStatus.CONFLICT);
		});
		UserEntity entity = mapper.dtoToEntity(userDto);
		return repository.save(entity) != null;
	}

	public boolean updateUser(final UserDto userDto) {
		Optional<UserEntity> optionalUser = repository.findByUsername(userDto.getEmail());
		return optionalUser
				.map(e -> repository.save(mapper.updateEntityFromDto(userDto, e)) != null)
				.orElseThrow(() -> new UserServiceError("User does not exist",
						HttpStatus.BAD_REQUEST));
	}

}
