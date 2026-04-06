package src;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class WebServer {
    private static final CryptoService cryptoService = new CryptoService();
    private static final FileService fileService = new FileService("notes.txt");

    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        server.createContext("/", new StaticFileHandler());
        server.createContext("/api/write", new WriteHandler());
        server.createContext("/api/read", new ReadHandler());
        server.createContext("/api/raw", new RawHandler());
        
        server.setExecutor(null); // creates a default executor
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

    static class WriteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                // Simple JSON parsing (avoiding external libraries)
                String passphrase = extractJsonValue(body, "passphrase");
                String text = extractJsonValue(body, "text");

                if (passphrase == null || passphrase.isBlank()) {
                    sendJsonResponse(exchange, 400, "{\"success\": false, \"error\": \"Passphrase is required\"}");
                    return;
                }
                if (text == null || text.isBlank()) {
                    sendJsonResponse(exchange, 400, "{\"success\": false, \"error\": \"Text is required\"}");
                    return;
                }

                try {
                    String encrypted = cryptoService.encrypt(text, passphrase);
                    fileService.write(encrypted);
                    sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Saved successfully to " + fileService.path().replace("\\", "\\\\") + "\"}");
                } catch (Exception e) {
                    sendJsonResponse(exchange, 500, "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}");
                }
            } else {
                sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
            }
        }
    }

    static class ReadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String passphrase = extractJsonValue(body, "passphrase");

                if (passphrase == null || passphrase.isBlank()) {
                    sendJsonResponse(exchange, 400, "{\"success\": false, \"error\": \"Passphrase is required\"}");
                    return;
                }

                if (!fileService.exists()) {
                    sendJsonResponse(exchange, 404, "{\"success\": false, \"error\": \"No file found yet. Write something first.\"}");
                    return;
                }

                String payload = fileService.read();
                if (payload.isBlank()) {
                    sendJsonResponse(exchange, 400, "{\"success\": false, \"error\": \"File is empty.\"}");
                    return;
                }

                try {
                    String plain = cryptoService.decrypt(payload, passphrase);
                    sendJsonResponse(exchange, 200, "{\"success\": true, \"text\": \"" + escapeJson(plain) + "\"}");
                } catch (Exception e) {
                    sendJsonResponse(exchange, 400, "{\"success\": false, \"error\": \"Decryption failed. Check passphrase or file content.\"}");
                }
            } else {
                sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
            }
        }
    }

    static class RawHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                if (!fileService.exists()) {
                    sendJsonResponse(exchange, 404, "{\"success\": false, \"error\": \"No file found yet.\"}");
                    return;
                }

                String raw = fileService.read();
                if (raw.isBlank()) {
                    sendJsonResponse(exchange, 400, "{\"success\": false, \"error\": \"File is empty.\"}");
                    return;
                }

                sendJsonResponse(exchange, 200, "{\"success\": true, \"text\": \"" + escapeJson(raw) + "\"}");
            } else {
                sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
            }
        }
    }

    private static String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int index = json.indexOf(searchKey);
        if (index == -1) return null;
        
        index += searchKey.length();
        while (index < json.length() && (json.charAt(index) == ' ' || json.charAt(index) == '\t')) {
            index++;
        }
        
        if (index >= json.length() || json.charAt(index) != '\"') return null;
        index++; // skip quote
        
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        while (index < json.length()) {
            char c = json.charAt(index);
            if (escape) {
                if (c == 'n') sb.append('\n');
                else if (c == 't') sb.append('\t');
                else if (c == '"') sb.append('"');
                else if (c == '\\') sb.append('\\');
                else sb.append(c);
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
            index++;
        }
        return sb.toString();
    }
    
    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\b", "\\b")
                   .replace("\f", "\\f")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        sendResponse(exchange, statusCode, response, "application/json");
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response, String contentType) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        // Important: set CORS headers for ease of development although not strictly needed when served from same host
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
