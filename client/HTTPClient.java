/**
 * Minimalist HTTP client
 * Communicate with the minimalist HTTP server written in Java
 * Usage: java client.HTTPClient.java <GET|POST|PUT|DELETE> <payload>
 * Can execute HTTP requests to get, post, put, and delete file from the HTTP server
 *
 * @author raphaelgiaquinto
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.io.*;

public enum HTTPClientCommand {
    GET, POST, PUT, DELETE;
}


String host = "127.0.0.1";
int port = 8888;

/**
 * Main that starts the client
 * Handles HTTP requests from command line arguments
 */
void main(String[] args) {
    if (args.length == 0) {
        IO.println("Usage: java client.HTTPClient --verb <GET|POST|PUT|DELETE> --data <payload>");
        return;
    }

    List<String> argsList = new ArrayList<>(List.of(args));

    int hostArg = argsList.indexOf("--host");
    int portArg = argsList.indexOf("--port");

    if (hostArg != -1 && portArg != -1) {
        host = argsList.get(hostArg + 1);
        port = Integer.parseInt(argsList.get(portArg + 1));
    }

    int verbArg = argsList.indexOf("--verb");

    String verbValue;

    if (verbArg == -1) {
        IO.println("Usage: java client.HTTPClient --verb <GET|POST|PUT|DELETE> --data <payload>");
        return;
    } else {
        verbValue = argsList.get(verbArg + 1);
    }
    int dataArg = argsList.indexOf("--data");
    String dataValue = null;
    if (dataArg != -1) {
        dataValue = argsList.get(dataArg + 1);
    }
    try {
        HTTPClientCommand clientCommand = HTTPClientCommand.valueOf(verbValue.toUpperCase());
        switch (clientCommand) {
            case GET -> handleGetCommand();
            case POST -> handlePostCommand(dataValue);
            case PUT -> handlePutCommand(dataValue);
            case DELETE -> handleDeleteCommand();
        }
    } catch (Exception e) {
        IO.println("Could not parse command properly");
    }
}

/**
 * Handle the GET command
 * Call the HTTPServer to get the file content
 */
private void handleGetCommand() {
    try (var clientSocket = new Socket(host, port)) {
        var outputStream = clientSocket.getOutputStream();
        String requestLine = "GET /file HTTP/1.1";
        String request = """
        %s
        Host: %s: %d
        Connection: close
        
        """.formatted(requestLine, host, port).replace("\n", "\r\n");
        outputStream.write(request.getBytes(StandardCharsets.US_ASCII));
        outputStream.flush();
        printServerResponse(clientSocket);
    } catch (Exception e) {
        IO.println("Could not connect to server: " + e.getMessage());
    }
}

private void handlePostCommand(String payload) {
    try (var clientSocket = new Socket(host, port)) {
        var outputStream = clientSocket.getOutputStream();
        var bytes = payload.getBytes(StandardCharsets.UTF_8);
        String requestLine = "POST /file HTTP/1.1";
        String request = """
        %s
        Host: %s: %d
        Content-Type: text/plain
        Content-Length: %d
        Connection: close
        
        """.formatted(requestLine, host, port, bytes.length).replace("\n", "\r\n");
        outputStream.write(request.getBytes(StandardCharsets.US_ASCII));
        outputStream.write(bytes);
        outputStream.flush();
        printServerResponse(clientSocket);
    } catch (Exception e) {
        IO.println("Could not connect to server: " + e.getMessage());
    }
}

private void handlePutCommand(String payload) {
    try (var clientSocket = new Socket(host, port)) {
        var outputStream = clientSocket.getOutputStream();
        var bytes = payload.getBytes(StandardCharsets.UTF_8);
        String requestLine = "PUT /file HTTP/1.1";
        String request = """
        %s
        Host: %s: %d
        Content-Type: text/plain
        Content-Length: %d
        Connection: close
        
        """.formatted(requestLine, host, port, bytes.length).replace("\n", "\r\n");
        outputStream.write(request.getBytes(StandardCharsets.US_ASCII));
        outputStream.write(bytes);
        outputStream.flush();
        printServerResponse(clientSocket);
    } catch (Exception e) {
        IO.println("Could not connect to server: " + e.getMessage());
    }
}

private void handleDeleteCommand() {
    try (var clientSocket = new Socket(host, port)) {
        var outputStream = clientSocket.getOutputStream();
        String requestLine = "DELETE /file HTTP/1.1";
        String request = """
        %s
        Host: %s: %d
        Connection: close
        
        """.formatted(requestLine, host, port).replace("\n", "\r\n");
        outputStream.write(request.getBytes(StandardCharsets.US_ASCII));
        outputStream.flush();
        printServerResponse(clientSocket);
    } catch (Exception e) {
        IO.println("Could not connect to server: " + e.getMessage());
    }
}

private void printServerResponse(Socket clientSocket) throws IOException{
    var inputStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    List<String> dataReceived = new ArrayList<>();
    String value;
    dataReceived.add("---headers---");
    while ((value = inputStream.readLine()) != null && !value.isEmpty()) {
        dataReceived.add(value);
    }
    dataReceived.add("---body---");
    while ((value = inputStream.readLine()) != null) {
        dataReceived.add(value);
    }
    for (String data : dataReceived) {
        IO.println(data);
    }
}