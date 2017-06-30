# Table of Contents  

- [spring-security-oauth2 POC](#spring-security-oauth2-poc)
   - [Components](#components)
   - [Description](#description)
   - [How to run](#how-to-run)
- [Authorization server](#authorization-server-uaa-service)
- [Resource Server](#resource-server)  
   - [Features](#features)
   - [Customizations](#customizations)
   - [Access control](#access-control)



# spring-security-oauth2 POC

This project evaluates the `spring-security-oauth2` as an option for securing our resource servers as we evolve our platform on a `spring-cloud` stack.

## Components

- **Resource Servers** - [a-service](a-service), [b-service](b-service), [c-service](c-service) all have copies of the same resource server configuration, and demonstrate the token exchange, token-relay and declarative expression-based access control capabilities. The resource servers are  aligned to the AuthN investigations we have been doing.
- **Authorization server** - [uaa-service](uaa-service), this custom authorization server is a throw away work, since we have already an existing identity server. So in this POC the scope of the `uaa-service` is just for setting up data, issuing tokens and to an extent evaluate the potential of `spring-security-oauth2` as an identity server.

## Description

In this POC we request an endpoint at `a-service:8001`, that would call another endpoint at `b-service:8002` which would call the  `c-service:8003` to finally return a string back to the client.

The call to `a-service:8001` is made using an opaque access token, within the `Authorization: Bearer` header. The resource server would exchange the opaque token with an equivalent JWT from the `uaa-service:9191` before the call gets to any `RestController`. The JWT will be stored as a part of `OAuth2Authentication` within the `SecurityContext`. The `OAuth2RestTemplate` and `FeignClient` will relay this JWT to the downstream server `b-service:8002` which again would relay the same to the `c-service:8003` which sends the final response back.

The above interaction displays the capabilities of token relay from service to service, and also the AuthN awareness of the resource servers. If and only if the incoming access token is an opaque one, the resource server would consult the `uaa-service:9191` for exchanging the token with a JWT, otherwise if the incoming access token is a JWT it will be honored as is.

Following is the zipkin dependency graph for this interaction,

<img width="679" alt="zipkin" src="https://user-images.githubusercontent.com/1873570/27722723-194ea3f4-5d1e-11e7-9d47-eb10de24afc2.png">

Also it has been made sure that the `FeignClient` + `Hystrix` + `Ribbon` works fine along with the `Oauth2RestTemplate` for relaying the tokens without issues.

## How to Run

Please follow the steps

#### build

```
bash-3.2$ git clone https://github.com/alwaysastudent/spring-security-oauth2-poc.git
bash-3.2$ cd spring-security-oauth2-poc
bash-3.2$ mvn clean install
```

split your terminal and run each of the 4 services individually or you can run them concurrently, if you like, from a single command line and monitor the logs on stdout.


#### start services (see concurrent option below)

```
bash-3.2$ java -jar ./uaa-service/target/uaa-service-0.0.1-SNAPSHOT.jar
bash-3.2$ java -jar ./a-service/target/a-service-0.0.1-SNAPSHOT.jar
bash-3.2$ java -jar ./a-service/target/b-service-0.0.1-SNAPSHOT.jar
bash-3.2$ java -jar ./a-service/target/c-service-0.0.1-SNAPSHOT.jar
```

#### start services concurrently

```
bash-3.2$ trap 'kill %1; kill %2; kill %3' SIGINT; java -jar ./uaa-service/target/uaa-service-0.0.1-SNAPSHOT.jar &  java -jar ./a-service/target/a-service-0.0.1-SNAPSHOT.jar & java -jar ./b-service/target/b-service-0.0.1-SNAPSHOT.jar & java -jar ./c-service/target/c-service-0.0.1-SNAPSHOT.jar
```

it will take around a minute to start if your machine is doing well

Note - For stopping hit Ctrl+C couple of times. Also don't forget to remove the trap. After that verify all your processes are gone using `jps`

```
trap - SIGINT
```

#### verify

verify from the other terminal if all 4 are still running

```
bash-3.2$ jps | grep SNAPSHOT
59557 uaa-service-0.0.1-SNAPSHOT.jar
59558 a-service-0.0.1-SNAPSHOT.jar
59559 b-service-0.0.1-SNAPSHOT.jar
59560 c-service-0.0.1-SNAPSHOT.jar
```


#### generate the token from uaa-service

The `uua-service` comes preset with a client `web:secret` and 2 users - `foo@yahoo.com/password`, `bar@yahoo.com/password`

Read more on [uaa-service](uaa-service/README.md)

```
curl -v -X POST -H "Accept: application/json" --user web:secret  -d "username=foo@yahoo.com&password=password&grant_type=password&scope=read+write&client_secret=secret&client_id=web"  http://localhost:9191/uaa/oauth/token

< HTTP/1.1 200

{
  "access_token": "99b644da-5610-44e4-bf32-afae742b97a2",
  "token_type": "bearer",
  "refresh_token": "9386f20b-8106-4e78-b1b9-f056cf216310",
  "expires_in": 43199,
  "scope": "read write",
  "refresh_token_expires_in": 2591999,
  "user_guid": "6a7ecdaf-c8f1-4f1a-a351-49197d391b68"
}

```

#### call the  resource server, a-service:8001

```
export TOKEN=99b644da-5610-44e4-bf32-afae742b97a2
curl -v -H "Authorization: Bearer $TOKEN" http://localhost:8001/

< HTTP/1.1 200

a-service -> b-service -> c-service

------


curl -v -H "Authorization: Bearer $TOKEN" http://localhost:8001/?useFeign=true

< HTTP/1.1 200

a-service -> b-service -> c-service

```

#### monitor logs

You would get the logs similar to the below, where you could see the exchange call from `a-service` and also the JWTs being propagated and logged.



```
2017-06-27 10:57:39.207  INFO [a-service,5200fdcfecb89f02,5200fdcfecb89f02,false] 71819 --- [nio-8001-exec-6] c.s.cloud.poc.a.ResourceServerConfig     : Exchanging the opaque token, 1b255221-59c6-480f-8783-22bc2a900c28 for a JWT

2017-06-27 10:57:39.212  INFO [uaa-service,5200fdcfecb89f02,c25dd461eeed1046,false] 71818 --- [nio-9191-exec-9] c.s.c.p.u.r.TokenExchangeRestController  : Giving the JWT for foo@yahoo.com

2017-06-27 10:57:39.219  INFO [a-service,5200fdcfecb89f02,5200fdcfecb89f02,false] 71819 --- [nio-8001-exec-6] c.s.cloud.poc.a.SampleRestController     : The jwt is eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJyZWZyZXNoX3Rva2VuX2V4cGlyZXNfaW4iOjI1OTIwMDAsInVzZXJfbmFtZSI6ImZvb0B5YWhvby5jb20iLCJzY29wZSI6WyJyZWFkIiwid3JpdGUiXSwidXNlcl9ndWlkIjoiNTc0NTk5N2MtMDZmMy00ZDMwLThiYjUtNjliYjE5MzNhMjdmIiwiZXhwIjoxNDk4NjI5NDQzLCJhdXRob3JpdGllcyI6WyJST0xFX0JVWUVSIiwiUk9MRV9VU0VSIiwiUk9MRV9TRUxMRVIiXSwianRpIjoiMWIyNTUyMjEtNTljNi00ODBmLTg3ODMtMjJiYzJhOTAwYzI4IiwiY2xpZW50X2lkIjoid2ViIn0.L2Q_1M3d5lntvATq52YlbbstnErEAnKIzUU2gQVl1rbhR3uI6IBy6WqLwjGJmizw8rx4IiUAWvMkeYzsF3GxpBEh4fJDonUaqVZDoIOtzj7ntaE121A4F43Y7chhsfVo5uX_2EkNUpTIBemLo3Jeknnb2bGdNKgLmlQ2-nEgC1c

2017-06-27 10:57:39.228  INFO [b-service,5200fdcfecb89f02,2e0742648c2b1533,false] 71820 --- [nio-8002-exec-3] c.s.cloud.poc.b.SampleRestController     : The jwt is eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJyZWZyZXNoX3Rva2VuX2V4cGlyZXNfaW4iOjI1OTIwMDAsInVzZXJfbmFtZSI6ImZvb0B5YWhvby5jb20iLCJzY29wZSI6WyJyZWFkIiwid3JpdGUiXSwidXNlcl9ndWlkIjoiNTc0NTk5N2MtMDZmMy00ZDMwLThiYjUtNjliYjE5MzNhMjdmIiwiZXhwIjoxNDk4NjI5NDQzLCJhdXRob3JpdGllcyI6WyJST0xFX0JVWUVSIiwiUk9MRV9VU0VSIiwiUk9MRV9TRUxMRVIiXSwianRpIjoiMWIyNTUyMjEtNTljNi00ODBmLTg3ODMtMjJiYzJhOTAwYzI4IiwiY2xpZW50X2lkIjoid2ViIn0.L2Q_1M3d5lntvATq52YlbbstnErEAnKIzUU2gQVl1rbhR3uI6IBy6WqLwjGJmizw8rx4IiUAWvMkeYzsF3GxpBEh4fJDonUaqVZDoIOtzj7ntaE121A4F43Y7chhsfVo5uX_2EkNUpTIBemLo3Jeknnb2bGdNKgLmlQ2-nEgC1c

2017-06-27 10:57:39.237  INFO [c-service,5200fdcfecb89f02,dd8f9d32641a6c45,false] 71821 --- [nio-8003-exec-3] c.s.cloud.poc.c.SampleRestController     : The jwt is eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJyZWZyZXNoX3Rva2VuX2V4cGlyZXNfaW4iOjI1OTIwMDAsInVzZXJfbmFtZSI6ImZvb0B5YWhvby5jb20iLCJzY29wZSI6WyJyZWFkIiwid3JpdGUiXSwidXNlcl9ndWlkIjoiNTc0NTk5N2MtMDZmMy00ZDMwLThiYjUtNjliYjE5MzNhMjdmIiwiZXhwIjoxNDk4NjI5NDQzLCJhdXRob3JpdGllcyI6WyJST0xFX0JVWUVSIiwiUk9MRV9VU0VSIiwiUk9MRV9TRUxMRVIiXSwianRpIjoiMWIyNTUyMjEtNTljNi00ODBmLTg3ODMtMjJiYzJhOTAwYzI4IiwiY2xpZW50X2lkIjoid2ViIn0.L2Q_1M3d5lntvATq52YlbbstnErEAnKIzUU2gQVl1rbhR3uI6IBy6WqLwjGJmizw8rx4IiUAWvMkeYzsF3GxpBEh4fJDonUaqVZDoIOtzj7ntaE121A4F43Y7chhsfVo5uX_2EkNUpTIBemLo3Jeknnb2bGdNKgLmlQ2-nEgC1c
```

#### zipkin dependency graph

<img width="679" alt="zipkin" src="https://user-images.githubusercontent.com/1873570/27722723-194ea3f4-5d1e-11e7-9d47-eb10de24afc2.png">

------------

# Authorization server (uaa-service)

The `spring-security-oauth2` based `uaa-service`, tries to mimic our identity server by issuing “opaque tokens” instead of JWT. This is a throw away work, since we have got an identity server already. But it was still worth investigating to learn the possibilities with this framework. It took a bit of traveling through the `spring-securtity-oauth2` code to figure some of these out, because `spring-security-oauth2` does not by default give an opaque token for a JWT.

The primary work in this POC are for supporting opaque tokens, embed custom claims within the JWT and protect the resources under the server itself.

Coming to the point, the `uaa-service` has got primarily the following endpoints in it

### oauth endpoints

1) `POST /uaa/oauth/token` - generate access tokens and refresh tokens based on the grant_type(client_credentials, password). The tokens will have scopes and roles associated to them. Roles are associated to the user, where as scopes is defined as a part of authorized clients. For configurations check [AuthServerConfiguration](uaa-service/src/main/java/org/ki/cloud/poc/user/auth/AuthServerConfiguration.java)
2) `GET /uaa/token/exchange` - exchanges the opaque token from the `Authorization: Bearer` for the authenticated JWT. Normally used by the AuthN resource servers to exchange opaque tokens with their associated JWT. Check [TokenExchangeRestController](uaa-service/src/main/java/org/ki/cloud/poc/user/rest/TokenExchangeRestController.java)
3) `POST uaa/oauth/check_token` - gives out the claims map associated with a token; protected with Basic auth `--user web:secret`
4) `GET /uaa/oauth/token_key` - gives out the public key for verifying the JWT signature

[Check usage ](uaa-service/README.md#token-api-usage)

### user endpoints

Check [UserServiceRestController](uaa-service/src/main/java/org/ki/cloud/poc/user/rest/UserServiceRestController.java)

1) `POST /uaa/user/create` - creates users for whom you can specify the roles from an enumeration of ROLE_ADMIN, ROLE_SELLER, ROLE_BUYER, ROLE_CS, ROLE_USER. Use the access token generated with the `grant_type=client_credentials`.
2) `GET /uaa/user` - self lookup which shows user details and the associated roles; protected by RBAC and scope
3) `GET /uaa/user/{guid}` - self or proxy lookup protected by an RBAC policy
4) `PUT /uaa/user/update/{guid}` – updates the user details including the associated roles

[Check usage ](uaa-service/README.md#user-api-usage)


Note - The opaque tokens are stored in memory cache and by default the user details are stored in the H2, which can be moved to oracle.

------------

# Resource Server

The resource servers are based on `spring-mvc` and `spring-security-oauth2`. They try to implement 'AuthN' mechanism with some minimum customization/configurations. The challenging part is that the documentation is all over the place and very generic, you have to dig through the code for figuring out some of the customizations. But like any other spring project, `spring-security-oauth2` comes with a lot of hooks to latch on and customize to our needs.

## Features

The resource server configuration support

- Awareness of opaque tokens on the Authorization header - the same will be exchanged with a JWT on entry
- Acceptance of JWT on the Authorization header
- Token relay using OAuth2RestTemplate + Hystrix
- Token relay while client side load-balancing using FeignClient + Hystrix + Ribbon
- Expression-based access control

## Customizations

Following are the some of the customizations done in order to secure the `spring-mvc` endpoints and conduct the token relay.

1) Setting up a [ResourceServerTokenServices](a-service/src/main/java/org/ki/cloud/poc/a/ResourceServerConfig.java#L123) which would exchange the opaque token for a JWT. If the token in the JWT format, it won't make this exchange call.
2) A custom [OAuth2ClientContext](a-service/src/main/java/org/ki/cloud/poc/a/ResourceServerConfig.java#L211) with a line of code to override the default behavior – This is to work around a flaw with the default setup that flops while using Hystrix. Hystrix wraps the calls outbound in its own threads/callables and the request attributes and security context has to be shared appropriately. I am working on a PR to fix this [issue](https://github.com/spring-cloud/spring-cloud-netflix/issues/1336#issuecomment-312023007).
3) A custom [AccessTokenConverter](a-service/src/main/java/org/ki/cloud/poc/a/ResourceServerConfig.java#L256) to add all the additional claims into the OAuth2Authentication as additional details.

All the resource server configurations can be found in the [ResourceServerConfig](a-service/src/main/java/org/ki/cloud/poc/a/ResourceServerConfig.java) and on the [application.yml](a-service/src/main/resources/application.yml) .

## Access control

Spring security gives the ability to use SpEL expressions as an access control mechanism which allows us to encapsulate complicated Boolean logic to be in a single expression. It comes with a dozen of built-in expressions and more over allows us to write custom expressions. Some of this capabilities are used in this project and can be found under
- [UserServiceRestController](uaa-service/src/main/java/org/ki/cloud/poc/user/rest/UserServiceRestController.java#L59)
- [UserServiceSecurity](uaa-service/src/main/java/org/ki/cloud/poc/user/rest/UserServiceSecurity.java)
- [TokenExchangeRestController](uaa-service/src/main/java/org/ki/cloud/poc/user/rest/TokenExchangeRestController.java#L22)
- [SampleRestController](a-service/src/main/java/org/ki/cloud/poc/a/SampleRestController.java#L34)

-------------
