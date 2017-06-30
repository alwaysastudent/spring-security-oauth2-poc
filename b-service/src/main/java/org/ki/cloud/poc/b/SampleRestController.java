package org.ki.cloud.poc.b;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Karthik Iyer
 *
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SampleRestController {

  @Value("${spring.application.name}")
  private String from;

  private final OAuth2RestTemplate restTemplate;

  @ResponseBody
  @RequestMapping("/")
  @PreAuthorize("isAuthenticated() && #oauth2.hasScope('write') && @sampleSecurityService.hasPermission(authentication)")
  public String b(@RequestParam("from") String pFrom,
      OAuth2Authentication authentication) {
    log.info("The jwt is {}",
        ((OAuth2AuthenticationDetails) authentication.getDetails()).getTokenValue());
    final ResponseEntity<String> forEntity = restTemplate.getForEntity(
        "http://c-service?from={from}", String.class, pFrom + " -> " + from);
    final String body = forEntity.getBody();
    return String.format(body);
  }

}
