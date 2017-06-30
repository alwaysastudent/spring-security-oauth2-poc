package org.ki.cloud.poc.user.auth.resource;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.ki.cloud.poc.user.auth.cache.JwtCache;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.authentication.BearerTokenExtractor;

import lombok.AllArgsConstructor;

/**
 *
 * @author Karthik Iyer
 *
 */
@Configuration
@EnableResourceServer
@EnableGlobalMethodSecurity(prePostEnabled = true)
@AllArgsConstructor
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {

  private final JwtCache jwtCache;

  @Override
  public void configure(HttpSecurity http) throws Exception {
    http.cors().and().anonymous().and().authorizeRequests().antMatchers("/user/**")
        .authenticated().and().authorizeRequests().antMatchers("/token/**")
        .authenticated();

  }

  @Override
  public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
    resources.tokenExtractor(new BearerTokenExtractor() {

      @Override
      protected String extractToken(HttpServletRequest request) {
        return Optional.ofNullable(jwtCache.get(super.extractToken(request)))
            .orElse(super.extractToken(request));
      }

    });
  }

}