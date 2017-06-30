package org.ki.cloud.poc.user.auth;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 *
 * @author Karthik Iyer
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AuthUser extends User {

  private static final long serialVersionUID = -543878367829581182L;

  @Getter
  private String guid;

  public AuthUser(String username, String password, boolean enabled, String guid,
      Collection<? extends GrantedAuthority> authorities) {
    super(username, password, enabled, enabled, enabled, enabled, authorities);
    this.guid = guid;

  }

}
