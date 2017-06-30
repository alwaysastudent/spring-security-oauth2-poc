package org.ki.cloud.poc.user;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.sql.DataSource;

import org.ki.cloud.poc.user.data.UserRepository;
import org.ki.cloud.poc.user.data.entity.UserEntity;
import org.ki.cloud.poc.user.data.entity.type.UserRoleType;
import org.modelmapper.ModelMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.config.java.AbstractCloudConfig;
import org.springframework.cloud.service.PooledServiceConnectorConfig.PoolConfig;
import org.springframework.cloud.service.relational.DataSourceConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import lombok.AllArgsConstructor;

/**
 *
 * @author Karthik Iyer
 *
 */
@EnableDiscoveryClient
@SpringBootApplication
@EnableJpaRepositories
@EnableJpaAuditing
@EnableCaching(proxyTargetClass = true)
public class UserServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(UserServiceApplication.class, args);
  }

  @Bean
  public ModelMapper getModelMapper() {
    return new ModelMapper();
  }

  @Profile("cloud")
  public static class CloudDataSourceCustomization extends AbstractCloudConfig {

    @Bean
    public DataSource dataSource() {
      PoolConfig poolConfig = new PoolConfig(5, 30, 3000);
      List<String> dataSourceNames = Arrays.asList("HikariCpPooledDataSourceCreator",
          "TomcatJdbcPooledDataSourceCreator");
      DataSourceConfig dbConfig = new DataSourceConfig(poolConfig, null, dataSourceNames);
      return connectionFactory().dataSource(dbConfig);
    }

  }

  @Configuration
  @Profile(value = { "default" })
  @AllArgsConstructor
  public static class LocalDataSetup implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {

      userRepository.save(Arrays.asList(new UserEntity[] {
          new UserEntity("foo@yahoo.com", "password", "foo", "0000000000", true,
              new HashSet<UserRoleType>(Arrays.asList(UserRoleType.ROLE_USER,
                  UserRoleType.ROLE_BUYER, UserRoleType.ROLE_SELLER))),
          new UserEntity("bar@yahoo.com", "password", "bar", "0000000000", true,
              new HashSet<UserRoleType>(Arrays.asList(UserRoleType.ROLE_USER,
                  UserRoleType.ROLE_BUYER, UserRoleType.ROLE_SELLER))

          ) }));

    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public org.h2.tools.Server h2WebConsonleServer() throws SQLException {
      return org.h2.tools.Server.createWebServer("-web", "-webAllowOthers", "-webDaemon",
          "-webPort", "8082");
    }

  }

}
