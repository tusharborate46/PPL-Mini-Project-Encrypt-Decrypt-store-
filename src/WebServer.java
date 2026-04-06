package src;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class WebServer {
    private static final FileService fileService = new FileService("messages.json");
    private static String messagesJson = "[]";

    public static void main(String[] args) throws IOException {
        if (fileService.exists()) {
            String content = fileService.read().trim();
            if (content.startsWith("[") && content.endsWith("]")) {
                messagesJson = content;
            } else {
                fileService.write(messagesJson);
            }
        } else {
            fileService.write(messagesJson);
        }

        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        server.createContext("/", new StaticFileHandler());
        server.createContext("/api/messages", new MessagesHandler());
        
        server.setExecutor(null);
        System.out.println("Server started on http://localhost:" + port);
        server.start();
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if ("/".equals(path)) {
                path = "/index.html";
            }
            
            Path filePath = Path.of("public", path);
            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                sendResponse(exchange, 404, "File not found", "text/plain");
                return;
            }

            String contentType = "text/plain";
            if (path.endsWith(".html")) contentType = "text/html";
            else if (path.endsWith(".css")) contentType = "text/css";
            else if (path.endsWith(".js")) contentType = "application/javascript";
            else if (path.endsWith(".png")) contentType = "image/png";

            byte[] content = Files.readAllBytes(filePath);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, content.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(content);
            }
        }
    }

    static class MessagesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if ("GET".equals(exchange.getRequestMethod())) {
                    sendJsonResponse(exchange, 200, messagesJson);
                } else if ("POST".equals(exchange.getRequestMethod())) {
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).trim();
                    
                    // Simple validation
                    if (!body.startsWith("{") || !body.endsWith("}")) {
                        sendJsonResponse(exchange, 400, "{\"error\": \"Invalid JSON\"}");
                        return;
                    }

                    synchronized (WebServer.class) {
                        if (messagesJson.equals("[]")) {
                            messagesJson = "[\n  " + body + "\n]";
                        } else {
                            // remove last bracket
                            messagesJson = messagesJson.substring(0, messagesJson.lastIndexOf("]")) + ",\n  " + body + "\n]";
                        }
                        fileService.write(messagesJson);
                    }
                    sendJsonResponse(exchange, 200, "{\"success\": true}");
                } else {
                    sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, 500, "{\"error\": \"Server error\"}");
            }
        }
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        sendResponse(exchange, statusCode, response, "application/json");
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response, String contentType) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        // Important: set CORS headers
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
