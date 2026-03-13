## Minimalist HTTP Server and Client

### Requirements

- **Java 25** if you want to run the server and client locally
- **Docker / Docker Compose** if you want to run the server and client as services, no need of Java 25 installed because it's already embedded in the Docker images.

### Files
- src
  - client
    - HTTPServer.java
    - Dockerfile
  - server
    - HTTPServer.java
    - Dockerfile
  - docker-compose.yml
  - README.md

### HTTP Server

Uses only TCP server socket as an example of how an HTTP server works under the hood

Compatible with any http client (postman, bruno, curl ...)

Served endpoints:

- GET /file
- POST /file
- PUT /file
- DELETE /file

Server could start by running the following command :

```bash
java HTTPServer.java
```

It is listening on port 8888.

### Http Client

Communicate with the minimalist HTTP server written in Java

Can execute HTTP requests to get, post, put, and delete file from the HTTP server

Client could start by running the following command :

```bash
java client.HTTPClient.java --verb <GET|POST|PUT|DELETE> --data <payload> --host <host> --port <port>
```

**Warning:** host and port are optional parameters. They are set to localhost and 8888 by default.
You should use them if you want to use the server and client as running services in Docker containers.


### Build services and start the http server

```bash
sudo docker-compose up --build
```

### Use the http client through Docker

```
sudo docker-compose run --rm http-client-service --host http-server-service --port 8888 --verb GET
```