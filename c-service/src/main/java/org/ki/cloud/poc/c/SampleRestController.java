package org.ki.cloud.poc.c;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Karthik Iyer
 *
 */
@RestController
@Slf4j
public class SampleRestController {

  @Value("${spring.application.name}")
  private String from;

  @ResponseBody
  @RequestMapping("/")
  @PreAuthorize("isAuthenticated() && #oauth2.hasScope('write') && @sampleSecurityService.hasPermission(authentication)")
  public String c(@RequestParam("from") String pFrom,
      OAuth2Authentication authentication) {
    log.info("The jwt is {}",
        ((OAuth2AuthenticationDetails) authentication.getDetails()).getTokenValue());
    return String.format(pFrom + " -> " + from);
  }

}
