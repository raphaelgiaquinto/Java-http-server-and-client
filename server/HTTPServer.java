/**
 * Minimalist HTTP server written in Java
 * Uses only TCP server socket as an example of how an HTTP server works under the hood
 * Implement HTTP protocol 1.1
 * Compatible with any http client (postman, bruno, curl ...)
 *  Served endpoints:
 *      GET /file
 *      POST /file
 *      PUT /file
 *      DELETE /file
 *  Handle 404 not found route
 *  Server could start by running 'java ./server.HTTPServer.java'
 *  Listening on port 8888
 * 
 *  @author raphaelgiaquinto
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.lang.IO;

static final int PORT = 8888;
static final String HOST = "127.0.0.1";
static final String RESOURCE_FILE = "/data/resource.txt";
/**
 * Main function that starts the server
 * Handle connections by creating threads (this is for example, please do not use this in production :-) . Instead use a thread pool manager)
 */
void main() {
    try (var serverSocket = new ServerSocket(PORT)) {
        log("HTTP server is listening on " + HOST + ":" + PORT);
        while (true) {
            var clientSocket = serverSocket.accept();
            new ConnectionHandler(clientSocket).start();
        }
    } catch (Exception e) {
        log("Could not start server: " + e.getMessage());
    }
}

/**
 * Connection handling class
 * How a single connection is managed when a new thread is created for a client
 */
private static class ConnectionHandler extends Thread {

    private final Socket clientSocket;

    private enum RequestLineKey {
        VERB, PATH, PROTOCOL
    }

    private enum HttpVerb {
        GET, POST, PUT, DELETE
    }

    public ConnectionHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    /**
     * Reading the client request
     * Respond according to the client request
     */
    @Override
    public void run() {
        try {
            var inputStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            var requestFromClient = new ArrayList<String>();
            String value = null;
            while ((value = inputStream.readLine()) != null && !value.isEmpty()) {
                requestFromClient.add(value);
            }
            Map<RequestLineKey, String> requestLine = parseRequestLine(requestFromClient.getFirst());
            if (requestLine == null) {
                badRequest(clientSocket);
            } else {
                Map<String, String> headers = parseHeaders(requestFromClient);
                String body = null;
                if (headers.containsKey("content-length")) {
                    int contentLength = Integer.parseInt(headers.get("content-length").replace(" ", ""));
                    char[] buffer = new char[contentLength];
                    inputStream.read(buffer, 0, contentLength);
                    body = new String(buffer);
                }
                if (body != null)
                    log("received body: "+body);
                handleRequest(requestLine.get(RequestLineKey.PATH), requestLine.get(RequestLineKey.VERB), body, clientSocket);
            }
            
        } catch (Exception e) {
            try {
                internalError(clientSocket);
                log("Internal error occurred: "+e.getMessage());
            } catch (IOException ex) {
                log("Could not send internal server error response");
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * If the client request is correct, choose the right action/response to do
     * @param path
     * @param verb
     * @param clientSocket
     * @throws IOException
     */
    private void handleRequest(String path, String verb, String body, Socket clientSocket) throws IOException {
        log("request received [" + verb + "] "+path);
        if (HttpVerb.GET.name().equals(verb) && "/file".equals(path)) {
            getFile(clientSocket);
        } else if (HttpVerb.POST.name().equals(verb) && "/file".equals(path)) {
            postFile(clientSocket, body);
        } else if (HttpVerb.PUT.name().equals(verb) && "/file".equals(path)) {
            putFile(clientSocket, body);
        } else if (HttpVerb.DELETE.name().equals(verb) && "/file".equals(path)) {
            deleteFile(clientSocket);
        }
        else {
            notFound(clientSocket);
        }
    }

    /**
     * Response on GET /file
     * Returns the resource file content if it exists
     * If it is not the case, we should return a 404 not found
     * @param clientSocket
     * @throws IOException
     */
    private void getFile(Socket clientSocket) throws IOException {
        var file = new File(RESOURCE_FILE);
        if (!file.exists()) {
            notFound(clientSocket);
        } else {
            FileInputStream fis = new FileInputStream(file);
            var bytes = new byte[(int) file.length()];
            fis.read(bytes);
            var requestLine = "HTTP/1.1 200 OK";
            var body = new String(bytes, StandardCharsets.UTF_8);
            byte[] bytesBody = body.getBytes(StandardCharsets.UTF_8);
            var request = """
            %s
            Content-Type: text/plain
            Content-Length: %d
            Connection: close
    
            """.formatted(requestLine, bytesBody.length).replace("\n", "\r\n");

            var outputStream = clientSocket.getOutputStream();
            outputStream.write(request.getBytes(StandardCharsets.US_ASCII));
            outputStream.write(bytesBody);
            outputStream.flush();
            clientSocket.close();
            log(requestLine);
        }
    }

    /**
     * Response on POST /file
     * Create a new file resource.txt with the given content from body
     * @param clientSocket
     * @param body
     * @throws IOException
     */
    private void postFile(Socket clientSocket, String body) throws IOException {
        var file = new File(RESOURCE_FILE);
        if (file.exists()) {
            conflict(clientSocket);
        } else {
            var fos = new FileOutputStream(file);
            if (body != null) {
                var bytes = body.getBytes(StandardCharsets.UTF_8);
                fos.write(bytes);
            }
            fos.close();
            var requestLine = "HTTP/1.1 201 Created";
            var request = """
            %s
            Content-Type: text/plain
            Content-Length: 0
            Connection: close
    
            """.formatted(requestLine).replace("\n", "\r\n");

            var outputStream = clientSocket.getOutputStream();
            outputStream.write(request.getBytes(StandardCharsets.US_ASCII));
            outputStream.flush();
            clientSocket.close();
            log(requestLine);
        }
    }

    private void putFile(Socket clientSocket, String body) throws IOException {
        var file = new File(RESOURCE_FILE);
        var requestLine = "HTTP/1.1 200 OK";
        if (!file.exists()) {
            file.createNewFile();
            requestLine = "HTTP/1.1 201 Created";
        }
        var fos = new FileOutputStream(file);
        if (body != null) {
            var bytes = body.getBytes(StandardCharsets.UTF_8);
            fos.write(bytes);
        }
        fos.close();
        var request = """
        %s
        Content-Type: text/plain
        Content-Length: 0
        Connection: close

        """.formatted(requestLine).replace("\n", "\r\n");

        var outputStream = clientSocket.getOutputStream();
        outputStream.write(request.getBytes(StandardCharsets.US_ASCII));
        outputStream.flush();
        clientSocket.close();
        log(requestLine);

    }

    private void deleteFile(Socket clientSocket) throws IOException {
        var file = new File(RESOURCE_FILE);
        if (!file.exists()) {
            notFound(clientSocket);
        } else {
            file.delete();
            var requestLine = "HTTP/1.1 200 OK";
            var request = """
            %s
            Content-Type: text/plain
            Connection: close
            
            """.formatted(requestLine).replace("\n", "\r\n");

            var outputStream = clientSocket.getOutputStream();
            outputStream.write(request.getBytes(StandardCharsets.US_ASCII));
            outputStream.flush();
            clientSocket.close();
            log(requestLine);
        }
    }

    /**
     * Handle 404 not found response
     * This is when a client ask for something that could not find an answer to his request :(
     * @param clientSocket
     * @throws IOException
     */
    private void notFound(Socket clientSocket) throws IOException {
        var outputStream = clientSocket.getOutputStream();
        var requestLine = "HTTP/1.1 404 NOT_FOUND";
        var request = """
        %s
        Content-Type: text/plain
        Content-Length: 0
        Connection: close

        """.formatted(requestLine).replace("\n", "\r\n");
        outputStream.write(request.getBytes(StandardCharsets.US_ASCII));
        outputStream.flush();
        clientSocket.close();
        log(requestLine);
    }

    private void conflict(Socket clientSocket) throws IOException {
        var outputStream = clientSocket.getOutputStream();
        var requestLine = "HTTP/1.1 409 CONFLICT";
        var request = """
        %s
        Content-Type: text/plain
        Content-Length: 0
        Connection: close
        
        """.formatted(requestLine).replace("\n", "\r\n");
        outputStream.write(request.getBytes(StandardCharsets.US_ASCII));
        outputStream.flush();
        clientSocket.close();
        log(requestLine);
    }

    /**
     * Handle the 400 bad request
     * This is when the request sent by the client if not correctly formatted
     */
    private void badRequest(Socket clientSocket) throws IOException {
        var outputStream = clientSocket.getOutputStream();
        var requestLine = "HTTP/1.1 400 BAD_REQUEST";
        var request = """
        %s
        Content-Type: text/plain
        Content-Length: 0
        Connection: close

        """.formatted(requestLine).replace("\n", "\r\n");
        outputStream.write(request.getBytes(StandardCharsets.US_ASCII));
        outputStream.flush();
        clientSocket.close();
        log(requestLine);
    }

    /**
     * Handle the 503 server error
     * This is when the request could not be process because of an internal server error
     */
    private void internalError(Socket clientSocket) throws IOException {
        var outputStream = clientSocket.getOutputStream();
        var requestLine = "HTTP/1.1 503 INTERNAL_SERVER_ERROR";
        var request = """
        %s
        Content-Type: text/plain
        Content-Length: 0
        Connection: close

        """.formatted(requestLine).replace("\n", "\r\n");
        outputStream.write(request.getBytes(StandardCharsets.US_ASCII));
        outputStream.flush();
        clientSocket.close();
        log(requestLine);
    }

    /**
     * Parse the first line of the client request
     * This is how we know if the request could be handle properly or not
     * @param requestLine
     * @return
     */
    private Map<RequestLineKey, String> parseRequestLine(String requestLine) {
        Map<RequestLineKey, String> parsedRequestLine = new HashMap<>();
        var split = requestLine.split(" ");
        if (split.length != 3) {
            log("Could not parse request line properly "+requestLine);
            return null;
        }
        var verb = split[0];
        try {
            HttpVerb.valueOf(verb);
        } catch(Exception ex) {
            log("HTTP verb "+ verb +" not handled by the server");
            return null;
        }
        parsedRequestLine.put(RequestLineKey.VERB, split[0]);
        parsedRequestLine.put(RequestLineKey.PATH, split[1]);
        parsedRequestLine.put(RequestLineKey.PROTOCOL, split[2]);
        return parsedRequestLine;
    }

    private Map<String, String> parseHeaders(List<String> requestFromClient) {
        Map<String, String> headers = new HashMap<>();
        for (int i = 1; i < requestFromClient.size(); i++) {
            var split = requestFromClient.get(i).split(":");
            if (split.length == 2) {
                headers.put(split[0].toLowerCase(), split[1]);
            }
        }
        return headers;
    }
}

/**
 * Logging function
 * @param message
 */
static void log(String message) {
    LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    IO.println("["+ now + "] : " + message);
}


