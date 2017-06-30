package org.ki.cloud.poc.user.auth.resource;

import java.util.Map;

import org.springframework.boot.autoconfigure.security.oauth2.resource.JwtAccessTokenConverterConfigurer;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

/**
 *
 * @author Karthik Iyer
 *
 */
public class JwtTokenConverter extends DefaultAccessTokenConverter
    implements JwtAccessTokenConverterConfigurer {

  @Override
  public void configure(JwtAccessTokenConverter converter) {
    converter.setAccessTokenConverter(this);
  }

  @Override
  public OAuth2Authentication extractAuthentication(Map<String, ?> map) {
    OAuth2Authentication auth = super.extractAuthentication(map);
    // populate details from the provided map, which contains the whole JWT.
    auth.setDetails(map);
    return auth;
  }
}