package org.ki.cloud.poc.user.rest;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
/**
 * 
 * @author Karthik Iyer
 *
 */
@Service
@Slf4j
public class UserServiceSecurity {

	public boolean doesGuidMatch(Authentication authentication, String guid) {
		final OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails) authentication
				.getDetails();
		final Map<?, ?> map = (Map<?, ?>) details.getDecodedDetails();
		String authGuid = (String) map.get("user_guid");
		log.info("Check the auth user guid = {} can operate on param user guid = {}",
				authGuid, guid);
		return guid.equals(authGuid);
	}

	public boolean doesGuidExist(Authentication authentication) {
		final OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails) authentication
				.getDetails();
		final Map<?, ?> map = (Map<?, ?>) details.getDecodedDetails();
		String authGuid = (String) map.get("user_guid");
		return authGuid != null;
	}
}