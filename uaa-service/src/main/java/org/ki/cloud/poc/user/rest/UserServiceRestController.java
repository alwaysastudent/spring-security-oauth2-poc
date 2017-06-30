package org.ki.cloud.poc.user.rest;

import java.security.Principal;
import java.util.Optional;

import javax.validation.Valid;

import org.ki.cloud.poc.user.rest.dto.UserDto;
import org.ki.cloud.poc.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

/**
 *
 *
 * @author Karthik Iyer
 *
 */
@RestController
@Slf4j
public class UserServiceRestController {

  private final UserService userService;

  public UserServiceRestController(UserService userService) {
    this.userService = userService;
  }

  @PreAuthorize("isAuthenticated() && (hasRole('ROLE_USER') || hasRole('ROLE_CS')) && #oauth2.hasScope('read')")
  @GetMapping(value = { "/user/{guid}", "/user" })
  public UserDto getUser(@PathVariable Optional<String> guid,
      @AuthenticationPrincipal Principal principal, OAuth2Authentication authentication) {
    // TODO - Change this to get user by Guid.
    log.info("Getting the user = {} ", principal.getName());
    return userService.getUser(principal.getName());
  }

  @PreAuthorize("hasRole('ROLE_WEB') && #oauth2.hasScope('write')")
  @PostMapping("/user/create")
  @ResponseStatus(HttpStatus.CREATED)
  public void createUser(@Valid @RequestBody UserDto user) {
    log.info("Creating the user = {}", user);
    userService.createUser(user);
  }

  @PreAuthorize("isAuthenticated() && #oauth2.hasScope('write') && (hasRole('ROLE_CS') || @userServiceSecurity.doesGuidMatch(authentication, #guid))")
  @PutMapping("/user/update/{guid}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateUser(@RequestBody UserDto user, @PathVariable String guid) {
    log.info("Updating the user = {}", user);
    userService.updateUser(user);
  }

}
