package org.ki.cloud.poc.user.rest.dto.mapper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.ki.cloud.poc.user.data.entity.UserEntity;
import org.ki.cloud.poc.user.data.entity.type.UserRoleType;
import org.ki.cloud.poc.user.rest.dto.UserDto;
import org.ki.cloud.poc.user.rest.exception.UserServiceError;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;

/**
 *
 * @author Karthik Iyer
 *
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

  public static final UserMapper INSTANCE = (UserMapper) Mappers
      .getMapper(UserMapper.class);

  @Mapping(source = "username", target = "email")
  public abstract UserDto entityToDto(UserEntity entity);

  @Mapping(source = "email", target = "username")
  @Mapping(target = "guid", ignore = true)
  public abstract UserEntity dtoToEntity(UserDto dto);

  default UserEntity updateEntityFromDto(UserDto dto, UserEntity entity) {
    if (dto == null) {
      return null;
    }
    if (!entity.getUsername().equals(dto.getEmail()))
      throw new UserServiceError("modifying not allowed, email", HttpStatus.BAD_REQUEST);
    entity.setPassword(dto.getPassword());
    entity.setName(dto.getName());
    entity.setPhone(dto.getPhone());
    entity.setActive(dto.isActive());
    Set<UserRoleType> updatedRoles = Optional.ofNullable(dto.getRoles())
        .map(r -> new HashSet<>(Arrays.asList(r))).orElse(new HashSet<>());
    entity.getRoles().retainAll(updatedRoles);
    updatedRoles.forEach((roleType) -> {
      if (entity.getRoles().stream().noneMatch(ur -> (ur == roleType)))
        entity.addRole(roleType);
    });

    return entity;
  }

}