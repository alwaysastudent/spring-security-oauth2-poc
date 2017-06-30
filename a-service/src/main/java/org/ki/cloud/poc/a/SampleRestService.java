package org.ki.cloud.poc.a;

import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Service;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

import lombok.AllArgsConstructor;
/**
 * 
 * @author Karthik Iyer
 *
 */
@Service
@AllArgsConstructor
public class SampleRestService {

	private final OAuth2RestTemplate oAuth2RestTemplate;

	@HystrixCommand(fallbackMethod = "fallback")
	public String callB(String from) {
		final ResponseEntity<String> forEntity = oAuth2RestTemplate
				.getForEntity("http://b-service/?from={from}", String.class, from);
		return forEntity.getBody();
	}

	public String fallback(String from) {
		return "You can not reach the b-service";
	}

}
