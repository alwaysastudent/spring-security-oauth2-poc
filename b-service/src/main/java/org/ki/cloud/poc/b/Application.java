package org.ki.cloud.poc.b;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;

/**
 *
 * @author Karthik Iyer
 *
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableHystrix
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);

  }

}
