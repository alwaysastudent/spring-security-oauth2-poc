package org.ki.cloud.poc.user.auth.cache;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Karthik Iyer
 *
 */
@Repository
public class JwtCache {

  @CachePut(value = "token", key = "#token", condition = "#token != null", unless = "#result == null")
  public String put(String token, String jwt) {
    return jwt;
  }

  @Cacheable(value = "token", key = "#token", unless = "#result == null")
  public String get(String token) {
    return null;
  }

  @CacheEvict(value = "token", key = "#token")
  public void remove(String token) {
    return;
  }

}