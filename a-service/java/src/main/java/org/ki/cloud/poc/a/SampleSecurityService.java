package org.ki.cloud.poc.a;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.stereotype.Service;

@Service
public class SampleSecurityService {

	public boolean hasPermission(Authentication authentication) {
		final OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails) authentication
				.getDetails();
		final Map<?, ?> map = (Map<?, ?>) details.getDecodedDetails();
		String authGuid = (String) map.get("user_guid");
		return authGuid != null;
	}
}