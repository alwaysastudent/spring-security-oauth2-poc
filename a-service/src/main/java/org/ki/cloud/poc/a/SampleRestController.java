package org.ki.cloud.poc.a;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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
@EnableCircuitBreaker
@RequiredArgsConstructor
public class SampleRestController {

  @Value("${spring.application.name}")
  private String from;

  private final SampleRestService service;

  private final BReader feignClientForB;

  @ResponseBody
  @RequestMapping("/")
  @PreAuthorize("isAuthenticated() && #oauth2.hasScope('write') && @sampleSecurityService.hasPermission(authentication)")
  public String a(@RequestParam(required = false, name = "useFeign") boolean useFeign,
      OAuth2Authentication authentication) {

    log.info("The jwt is {}",
        ((OAuth2AuthenticationDetails) authentication.getDetails()).getTokenValue());
    if (useFeign) {
      log.info("Calling outbound with feign");
      return feignClientForB.get(from);
    }
    return service.callB(from);
  }

}

@FeignClient(name = "b-service", fallback = BReaderImpl.class)
interface BReader {

  @LoadBalanced
  @RequestMapping(method = RequestMethod.GET, value = "/")
  public String get(@RequestParam("from") String from);
}

@Service
class BReaderImpl implements BReader {

  @Override
  public String get(String from) {
    return "You can not reach the b-service";
  }

}
