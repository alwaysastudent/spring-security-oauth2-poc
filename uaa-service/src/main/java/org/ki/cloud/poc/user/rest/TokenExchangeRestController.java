package org.ki.cloud.poc.user.rest;

import org.ki.cloud.poc.user.auth.cache.JwtCache;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
/**
 * 
 * @author Karthik Iyer
 *
 */
@Slf4j
@RestController
@AllArgsConstructor
public class TokenExchangeRestController {

	public final JwtCache cache;

	@PreAuthorize("isAuthenticated() && ((hasRole('ROLE_USER') || hasRole('ROLE_CS') && @userServiceSecurity.doesGuidExist(authentication)) || hasRole('ROLE_WEB'))")
	@GetMapping("/token/exchange")
	public String exchangeJWT(OAuth2Authentication authentication) {
		log.info("Giving the JWT for {}", authentication.getPrincipal());
		return ((OAuth2AuthenticationDetails) SecurityContextHolder.getContext()
				.getAuthentication().getDetails()).getTokenValue();
	}

}
