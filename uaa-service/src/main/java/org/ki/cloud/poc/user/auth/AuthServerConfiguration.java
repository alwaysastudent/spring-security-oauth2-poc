package org.ki.cloud.poc.user.auth;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.ki.cloud.poc.user.auth.cache.JwtCache;
import org.ki.cloud.poc.user.auth.resource.JwtTokenConverter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.common.DefaultExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.ExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.security.oauth2.provider.approval.ApprovalStore;
import org.springframework.security.oauth2.provider.approval.InMemoryApprovalStore;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
/**
 * 
 * @author Karthik Iyer
 *
 */
@Configuration
@EnableAuthorizationServer
@RequiredArgsConstructor
@Import(Oauth2Config.class)
public class AuthServerConfiguration extends AuthorizationServerConfigurerAdapter {

	private final AuthenticationManager authenticationManager;

	private final UserDetailsService userDetailsService;

	private final TokenStore tokenStore;

	private final TokenEnhancer jwtAccessTokenConverter;

	private final ApprovalStore approvalStore;

	@Override
	public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
		clients.inMemory().withClient("web").secret("secret").authorities("ROLE_WEB")
				.scopes("read", "write", "trust")
				.authorizedGrantTypes("client_credentials", "password", "refresh_token",
						"authorization_code", "implicit");
	}

	@Override
	public void configure(AuthorizationServerEndpointsConfigurer endpoints)
			throws Exception {
		endpoints.tokenStore(tokenStore).tokenEnhancer(jwtAccessTokenConverter)
				.reuseRefreshTokens(false).approvalStore(approvalStore)
				.authenticationManager(authenticationManager)
				.userDetailsService(userDetailsService);
	}

	@Override
	public void configure(AuthorizationServerSecurityConfigurer oauthServer)
			throws Exception {
		oauthServer.tokenKeyAccess("permitAll()").checkTokenAccess("isAuthenticated()");
	}

}

@Configuration
@Slf4j
@RequiredArgsConstructor
class Oauth2Config {

	private final JwtCache jwtCache;

	@Bean
	public ApprovalStore approvalStore() throws Exception {
		InMemoryApprovalStore store = new InMemoryApprovalStore();
		return store;
	}

	@Bean
	public TokenStore tokenStore(ApprovalStore approvalStore) {
		JwtTokenStore jwtTokenStore = new JwtTokenStore(jwtAccessTokenConverter()) {

			@Override
			public OAuth2AccessToken readAccessToken(String tokenValue) {
				return super.readAccessToken(
						Optional.ofNullable(jwtCache.get(tokenValue)).orElse(tokenValue));
			}

			@Override
			public OAuth2RefreshToken readRefreshToken(String tokenValue) {

				OAuth2RefreshToken readRefreshToken = super.readRefreshToken(Optional
						.ofNullable(jwtCache.get(tokenValue))
						.orElseThrow(() -> new InvalidTokenException(
								String.format("Invalid refresh token: %s", tokenValue))));

				return new DefaultExpiringOAuth2RefreshToken(tokenValue,
						((DefaultExpiringOAuth2RefreshToken) readRefreshToken)
								.getExpiration());
			}

			@Override
			public OAuth2Authentication readAuthenticationForRefreshToken(
					OAuth2RefreshToken token) {

				return super.readAuthenticationForRefreshToken(Optional
						.ofNullable(jwtCache.get(token.getValue()))
						.map(jwt -> new DefaultExpiringOAuth2RefreshToken(jwt,
								((DefaultExpiringOAuth2RefreshToken) token)
										.getExpiration()))
						.orElseThrow(() -> new InvalidTokenException(String
								.format("Invalid refresh token: %s", token.getValue()))));

			}

			@Override
			public void removeRefreshToken(OAuth2RefreshToken token) {
				super.removeRefreshToken(Optional
						.ofNullable(jwtCache.get(token.getValue()))
						.map(jwt -> new DefaultExpiringOAuth2RefreshToken(jwt,
								((DefaultExpiringOAuth2RefreshToken) token)
										.getExpiration()))
						.orElseThrow(() -> new InvalidTokenException(String
								.format("Invalid refresh token: %s", token.getValue()))));
				jwtCache.remove(token.getValue());
			}

			@Override
			public void storeRefreshToken(OAuth2RefreshToken refreshToken,
					OAuth2Authentication authentication) {

				if (null != approvalStore) {
					String tokenValue = Optional.of(jwtCache.get(refreshToken.getValue()))
							.orElseThrow(() -> new InvalidTokenException(
									String.format("Invalid refresh token: %s",
											refreshToken.getValue())));
					OAuth2Authentication auth = readAuthentication(tokenValue);
					String clientId = auth.getOAuth2Request().getClientId();
					Authentication user = auth.getUserAuthentication();
					if (user != null) {
						Date date = null;
						if (refreshToken instanceof DefaultExpiringOAuth2RefreshToken) {
							date = ((DefaultExpiringOAuth2RefreshToken) refreshToken)
									.getExpiration();
						}
						else {
							date = new Date();
						}
						Collection<Approval> approvals = new ArrayList<Approval>();
						for (String scope : auth.getOAuth2Request().getScope()) {
							approvals.add(new Approval(user.getName(), clientId, scope,
									date, Approval.ApprovalStatus.APPROVED));
						}
						approvalStore.addApprovals(approvals);
					}
				}
			}
		};
		jwtTokenStore.setApprovalStore(approvalStore);
		return jwtTokenStore;
	}

	@Bean
	protected JwtAccessTokenConverter jwtAccessTokenConverter() {
		JwtAccessTokenConverter converter = new JwtAccessTokenConverter() {
			@Override
			public OAuth2AccessToken enhance(OAuth2AccessToken accessToken,
					OAuth2Authentication authentication) {
				final DefaultOAuth2AccessToken preEnhancedToken = new DefaultOAuth2AccessToken(
						accessToken);
				final Map<String, Object> additionalInfo = new HashMap<>();
				if (authentication.getPrincipal() instanceof AuthUser) {
					additionalInfo.put("user_guid",
							((AuthUser) authentication.getPrincipal()).getGuid());
					// exposing the refresh token expiry for password grants only
					additionalInfo.put("refresh_token_expires_in",
							(((ExpiringOAuth2RefreshToken) preEnhancedToken
									.getRefreshToken()).getExpiration().getTime()
									- System.currentTimeMillis()) / 1000);
				}

				preEnhancedToken.setAdditionalInformation(additionalInfo);
				OAuth2AccessToken enhancedToken = super.enhance(preEnhancedToken,
						authentication);
				log.info("old token = {}, new token = {}", preEnhancedToken.getValue(),
						enhancedToken.getValue());
				log.info("old refresh token = {}, new refresh token = {}",
						preEnhancedToken.getRefreshToken().getValue(),
						enhancedToken.getRefreshToken().getValue());
				jwtCache.put(preEnhancedToken.getValue(), enhancedToken.getValue());
				jwtCache.put(preEnhancedToken.getRefreshToken().getValue(),
						enhancedToken.getRefreshToken().getValue());
				return preEnhancedToken;
			}
		};
		converter.setAccessTokenConverter(jwttokenConverter());

		final KeyPair keyPair = new KeyStoreKeyFactory(
				new ClassPathResource("spring.jks"), "spring".toCharArray())
						.getKeyPair("spring", "spring".toCharArray());

		converter.setKeyPair(keyPair);
		return converter;
	}

	@Bean
	protected AccessTokenConverter jwttokenConverter() {
		return new JwtTokenConverter();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(Arrays.asList("*"));
		configuration.setAllowedMethods(Arrays.asList("OPTIONS", "HEAD", "GET", "POST",
				"PUT", "DELETE", "PATCH"));
		configuration.setAllowCredentials(true);
		configuration.setAllowedHeaders(
				Arrays.asList("Origin", "Content-Type", "Accept", "Authorization"));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	public CorsFilter corsFilter(CorsConfigurationSource corsConfigurationSource) {
		return new CorsFilter(corsConfigurationSource);
	}

	@Bean
	public FilterRegistrationBean corsFilterRegistrationBean(CorsFilter corsFilter) {
		FilterRegistrationBean bean = new FilterRegistrationBean();
		bean.setFilter(corsFilter);
		bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return bean;
	}

}
