package cc.aimaxxing.arithmetics;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import jakarta.xml.ws.Endpoint;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Main {

    private static final int PORT = 8081;

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        // Virtual threads for efficient concurrent handling (Java 21)
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        server.createContext("/health", Main::handleHealth);

        Endpoint soapEndpoint = Endpoint.create(new ArithmeticsServiceImpl());
        soapEndpoint.publish(server.createContext("/arithmetics"));

        server.start();

        System.out.printf("Arithmetics SOAP service started on port %d%n", PORT);
        System.out.printf("  WSDL : http://0.0.0.0:%d/arithmetics?wsdl%n", PORT);
        System.out.printf("  Health: http://0.0.0.0:%d/health%n", PORT);

        Thread.currentThread().join();
    }

    private static void handleHealth(HttpExchange exchange) throws IOException {
        byte[] body = "{\"status\":\"UP\",\"service\":\"arithmetics\"}".getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }
}
