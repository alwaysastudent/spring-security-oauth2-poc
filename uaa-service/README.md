

## Token API usage

High level usage reference

### grant_type - client_credentials

```
curl -v -X POST -H "Accept: application/json" --user web:secret  -d "grant_type=client_credentials&scope=read+write&client_secret=secret&client_id=web"  http://localhost:9191/uaa/oauth/token

< HTTP/1.1 200 OK

{
  "access_token": "7ebc42d6-721a-4081-8380-b84530e06bf3",
  "token_type": "bearer",
  "expires_in": 43199,
  "scope": "read write"
}
```

### grant_type - password

```
curl -v -X POST -H "Accept: application/json" --user web:secret  -d "username=foo@yahoo.com&password=password&grant_type=password&scope=read+write&client_secret=secret&client_id=web"  http://localhost:9191/uaa/oauth/token


< HTTP/1.1 200 OK

{
  "access_token": "f44d6014-6a12-47da-a31c-03507407c342",
  "token_type": "bearer",
  "refresh_token": "226852eb-3f9f-48e8-bf15-2b3427f24f80",
  "expires_in": 43199,
  "scope": "read write",
  "refresh_token_expires_in": 2592000,
  "user_guid": "1527a98f-7ce1-4d12-a70f-7c7ece533b86"
}
```

### exchange token 

Normally used by the AuthN resource servers to exchange opaque tokens with their associated JWT.

```
export TOKEN=f44d6014-6a12-47da-a31c-03507407c342

curl -v -H "Authorization: Bearer $TOKEN" http://localhost:9191/uaa/token/exchange

< HTTP/1.1 200 OK

eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJyZWZyZXNoX3Rva2VuX2V4cGlyZXNfaW4iOjI1OTE5OTksInVzZXJfbmFtZSI6ImZvb0B5YWhvby5jb20iLCJzY29wZSI6WyJyZWFkIiwid3JpdGUiXSwidXNlcl9ndWlkIjoiOTgxZTBhOGMtZWM2YS00ZTRkLTliZDUtODRlYTcyYTUwODcxIiwiZXhwIjoxNDk4NTgxOTc4LCJhdXRob3JpdGllcyI6WyJST0xFX0JVWUVSIiwiUk9MRV9VU0VSIiwiUk9MRV9TRUxMRVIiXSwianRpIjoiZTcyNGFlMWUtOTQ4ZC00N2Y1LWEwODUtMGEyNjljZjQ2MzFkIiwiY2xpZW50X2lkIjoid2ViIn0.NB6BbAGYO9dtbIzHM94CwuYoXQJRqe_togIYoOTyeW6ECXGYd-f50dWzex6OfsvwRybHWqxf4qNkSWGtXTW2liV4lNw4brFulPVJDTNHMbharOapXW4jX0o97IGHjMB-YOH2bkXdne7CU6vVS6qT80Gnt3W8yFTK_o_igeHm-lo

```

### check_token - claims

```
export TOKEN=f44d6014-6a12-47da-a31c-03507407c342

curl -v   --user web:secret  "http://localhost:9191/uaa/oauth/check_token" -d "token=$TOKEN"

< HTTP/1.1 200 OK

{
  "refresh_token_expires_in": 2591999,
  "user_name": "foo@yahoo.com",
  "scope": [
    "read",
    "write"
  ],
  "user_guid": "981e0a8c-ec6a-4e4d-9bd5-84ea72a50871",
  "exp": 1498581978,
  "authorities": [
    "ROLE_BUYER",
    "ROLE_USER",
    "ROLE_SELLER"
  ],
  "jti": "e724ae1e-948d-47f5-a085-0a269cf4631d",
  "client_id": "web"
}
```

## User API usage

A sample user creation and access flow.

### get access token with client credentials, grant type

```
curl -v -X POST -H "Accept: application/json" --user web:secret  -d "grant_type=client_credentials&scope=read+write&client_secret=secret&client_id=web"  http://localhost:9191/uaa/oauth/token

< HTTP/1.1 200 OK

{
  "access_token": "7ebc42d6-721a-4081-8380-b84530e06bf3",
  "token_type": "bearer",
  "expires_in": 43199,
  "scope": "read write"
}
```


###  create user with the above access_token

```
export TOKEN=7ebc42d6-721a-4081-8380-b84530e06bf3

curl  -v -H "Authorization: Bearer $TOKEN" -H "Content-type: application/json"  -d '{"email":"blah@yahoo.com","name":"blah","password":"password", "phone":"0000000000", "active": "true", "roles":["ROLE_SELLER","ROLE_BUYER", "ROLE_USER"]}' http://localhost:9191/uaa/user/create

< HTTP/1.1 201 Created
```


###  get access token with password, grant type (login user)

```
curl -v -X POST -H "Accept: application/json" --user web:secret  -d "username=foo@yahoo.com&password=password&grant_type=password&scope=read+write&client_secret=secret&client_id=web"  http://localhost:9191/uaa/oauth/token


< HTTP/1.1 200 OK

{
  "access_token": "f44d6014-6a12-47da-a31c-03507407c342",
  "token_type": "bearer",
  "refresh_token": "226852eb-3f9f-48e8-bf15-2b3427f24f80",
  "expires_in": 43199,
  "scope": "read write",
  "refresh_token_expires_in": 2592000,
  "user_guid": "1527a98f-7ce1-4d12-a70f-7c7ece533b86"
}
```


### get user details including the user guid

```
export TOKEN=f44d6014-6a12-47da-a31c-03507407c342

curl -v -H "Authorization: Bearer $TOKEN" http://localhost:9191/uaa/user/

< HTTP/1.1 200 OK

{
  "email": "foo@yahoo.com",
  "name": "foo",
  "phone": "0000000000",
  "guid": "1527a98f-7ce1-4d12-a70f-7c7ece533b86",
  "active": true,
  "roles": [
    "ROLE_BUYER",
    "ROLE_SELLER",
    "ROLE_USER"
  ]
}
```


### update user with the above guid

```
curl  -v -H "Authorization: Bearer $TOKEN" -H "Content-Type: Application/json" -X PUT -d '{"email":"foo@yahoo.com","name":"blah","password":"password", "phone":"0000000000", "active": "true", "roles":["ROLE_SELLER","ROLE_BUYER", "ROLE_USER", "ROLE_CS"]}'  http://localhost:9191/uaa/user/update/$GUID

< HTTP/1.1 204 No Content
```

# Datamodel

<img width="276" alt="dmodel" src="https://user-images.githubusercontent.com/1873570/27723000-a145cbe2-5d1f-11e7-9223-e64e5d5e63f4.png">

 
