package org.ki.cloud.poc.c;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.autoconfigure.security.oauth2.resource.JwtAccessTokenConverterConfigurer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.hystrix.security.SecurityContextConcurrencyStrategy;
import org.springframework.cloud.security.oauth2.client.feign.OAuth2FeignRequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestAttributes;

import feign.RequestInterceptor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Karthik Iyer
 *
 */
@Configuration
@Import(Oauth2ClientConfig.class)
@EnableResourceServer
@EnableFeignClients
@EnableGlobalMethodSecurity(prePostEnabled = true)
@AllArgsConstructor
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {

	private final ResourceServerTokenServices tokenServices;

	/**
	 * Configure the access rules for securing the resources.
	 */
	@Override
	public void configure(HttpSecurity http) throws Exception {
		http.cors().and().authorizeRequests().antMatchers("/**").authenticated();

	}

	/**
	 * Customizing the default resource specific configurations. We have defined a custom
	 * {@link ResourceServerTokenServices} which would exchange an opaque token with a JWT
	 * from the uaa-service if found.
	 */
	@Override
	public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
		resources.tokenServices(tokenServices);
	}

}

@Slf4j
@Configuration
@AllArgsConstructor
class Oauth2ClientConfig {

	private final JwtAccessTokenConverter jwtAccessTokenConverter;

	/**
	 * Defines the {@link AccessTokenConverter} with custom {@link JwtTokenConverter}
	 * 
	 * @return
	 */
	@Bean
	protected AccessTokenConverter jwtTokenConverter() {
		JwtTokenConverter jwtTokenConverter = new JwtTokenConverter();
		jwtAccessTokenConverter.setAccessTokenConverter(jwtTokenConverter);
		return jwtTokenConverter;
	}

	@Bean
	public TokenStore tokenStore() {
		return new JwtTokenStore(jwtAccessTokenConverter);
	}

	@Bean
	public ResourceServerTokenServices tokenService(TokenStore tokenStore,
			RestTemplate exchangeRestTemplate) {

		return new DefaultTokenServices() {

			{
				{
					setTokenStore(tokenStore);
				}
			}

			@Override
			public OAuth2AccessToken readAccessToken(String accessToken) {
				try {
					tokenStore.readAccessToken(accessToken);
				}
				catch (InvalidTokenException e) {
					// Exception coz it is not an expected JWT, let us try to exchange
					accessToken = exchaneForJwt(accessToken);

				}

				return super.readAccessToken(accessToken);
			}

			@Override
			public OAuth2Authentication loadAuthentication(String accessTokenValue)
					throws AuthenticationException, InvalidTokenException {
				try {
					tokenStore.readAccessToken(accessTokenValue);
				}
				catch (InvalidTokenException e) {
					// Exception coz it is not an expected JWT, let us try to exchange
					accessTokenValue = exchaneForJwt(accessTokenValue);

				}

				return super.loadAuthentication(accessTokenValue);
			}

			private String exchaneForJwt(final String token) {
				log.info("Exchanging the opaque token, {} for a JWT", token);
				HttpHeaders headers = new HttpHeaders();
				headers.add("Authorization", "Bearer " + token);
				HttpEntity<String> entity = new HttpEntity<String>(headers);
				exchangeRestTemplate.setErrorHandler(new DefaultResponseErrorHandler() {

					@Override
					public void handleError(ClientHttpResponse response)
							throws IOException {
						HttpStatus statusCode = getHttpStatusCode(response);
						switch (statusCode.series()) {
						case CLIENT_ERROR:
							throw new InvalidRequestException(String.format(
									"Bad Request (most likely the token is invalid), %s ",
									token));
						default:
							super.handleError(response);
						}

					}

				});
				ResponseEntity<String> response = exchangeRestTemplate.exchange(
						"http://uaa-service/uaa/token/exchange", HttpMethod.GET, entity,
						String.class, token);
				String jwt = response.getBody();

				try {
					tokenStore.readAccessToken(jwt);
					return jwt;
				}
				catch (InvalidTokenException e) {
					throw new InvalidTokenException("Invalid token: " + token);
				}
			}
		};
	}

	/**
	 * Define the {@link OAuth2RestTemplate} which supports Token relay
	 * 
	 * @param oauth2ClientContext
	 * @param details
	 * @return
	 */
	@Bean
	@LoadBalanced
	public OAuth2RestTemplate oAuth2RestTemplate(OAuth2ClientContext oAuth2ClientContext,
			OAuth2ProtectedResourceDetails details) {
		return new OAuth2RestTemplate(details, oAuth2ClientContext);
	}

	/**
	 * This is to work around a flaw within the {@link SecurityContextConcurrencyStrategy}
	 * that does not propagate the {@link RequestAttributes} when hystrix is enabled. I'm
	 * working on a solution for this and will issue a PR for review and attached with
	 * this ticket.
	 * 
	 * https://github.com/spring-cloud/spring-cloud-netflix/issues/1336#issuecomment-312023007
	 * 
	 */
	@Bean
	public OAuth2ClientContext oAuth2ClientContext() {
		return new DefaultOAuth2ClientContext() {
			private static final long serialVersionUID = -7563661585547122665L;

			@Override
			public OAuth2AccessToken getAccessToken() {
				return Optional.ofNullable(super.getAccessToken())
						.orElse(new DefaultOAuth2AccessToken(
								((OAuth2AuthenticationDetails) SecurityContextHolder
										.getContext().getAuthentication().getDetails())
												.getTokenValue()));
			}

		};
	}

	/**
	 * Making the feign clients oauth2 aware.
	 * 
	 * @param oAuth2ClientContext
	 * @param resource
	 * @return
	 */
	@Bean
	public RequestInterceptor oAuth2FeignRequestInterceptor(
			OAuth2ClientContext oAuth2ClientContext,
			OAuth2ProtectedResourceDetails resource) {
		return new OAuth2FeignRequestInterceptor(oAuth2ClientContext, resource);
	}

	@Bean
	@LoadBalanced
	public RestTemplate exchangeRestTemplate() {
		return new RestTemplate();
	}
}

/**
 * Customizes the {@link JwtAccessTokenConverter} to set up a custom
 * {@link AccessTokenConverter} which would add additional details in to the
 * {@link OAuth2Authentication} context
 * 
 * @author kariyer
 *
 */
class JwtTokenConverter extends DefaultAccessTokenConverter
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