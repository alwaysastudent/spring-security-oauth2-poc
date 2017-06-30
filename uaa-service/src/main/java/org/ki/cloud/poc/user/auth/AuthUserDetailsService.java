package org.ki.cloud.poc.user.auth;

import java.util.Optional;

import org.ki.cloud.poc.user.data.UserRepository;
import org.ki.cloud.poc.user.data.entity.UserEntity;
import org.ki.cloud.poc.user.data.entity.type.UserRoleType;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 *
 * @author Karthik Iyer
 *
 */
@Service
public class AuthUserDetailsService implements UserDetailsService {

  private final UserRepository repository;

  AuthUserDetailsService(UserRepository repository) {
    this.repository = repository;
  }

  @Override
  public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {

    Optional<UserEntity> userEntity = repository.findByUsername(s);

    return userEntity
        .map(
            e -> new AuthUser(e.getUsername(), e.getPassword(), e.isActive(), e.getGuid(),
                AuthorityUtils.createAuthorityList(e.getRoles().stream()
                    .map(UserRoleType::toString).toArray(String[]::new))))
        .orElseThrow(() -> new UsernameNotFoundException("couldn't find  " + s + "!"));
  }
}