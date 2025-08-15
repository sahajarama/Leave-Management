import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class LeaveManagementApp {

    static class LeaveRequest {
        String employeeName;
        String employeeId;
        String reason;

        LeaveRequest(String employeeName, String employeeId, String reason) {
            this.employeeName = employeeName;
            this.employeeId = employeeId;
            this.reason = reason;
        }
    }

    private static List<LeaveRequest> requests = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/", new LoginHandler());
        server.createContext("/request", new RequestHandler());
        server.createContext("/view", new ViewHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server started at http://localhost:8000");
    }

    static class LoginHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes());
                String[] params = body.split("&");
                String name = params[0].split("=")[1].replace("+", " ");
                String id = params[1].split("=")[1];
                exchange.getResponseHeaders().add("Location", "/request?name=" + name + "&id=" + id);
                exchange.sendResponseHeaders(302, -1);
                return;
            }

            String html = """
                <html>
                <head>
                <style>
                    body { font-family: Arial; background: #f2f2f2; }
                    form { margin: 100px auto; width: 300px; padding: 20px; background: white; border-radius: 8px; }
                    input { width: 100%; padding: 8px; margin: 8px 0; }
                    button { background: #4CAF50; color: white; padding: 10px; border: none; width: 100%; }
                </style>
                </head>
                <body>
                    <form method="POST">
                        <h2>Employee Login</h2>
                        <input name="name" placeholder="Employee Name" required>
                        <input name="id" placeholder="Employee ID" required>
                        <button type="submit">Login</button>
                    </form>
                </body>
                </html>
            """;
            sendResponse(exchange, html);
        }
    }

    static class RequestHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String name = query.split("&")[0].split("=")[1].replace("+", " ");
            String id = query.split("&")[1].split("=")[1];

            if ("POST".equals(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes());
                String reason = body.split("=")[1].replace("+", " ");
                requests.add(new LeaveRequest(name, id, reason));
                exchange.getResponseHeaders().add("Location", "/view");
                exchange.sendResponseHeaders(302, -1);
                return;
            }

            String html = """
                <html>
                <head>
                <style>
                    body { font-family: Arial; background: #e6f2ff; }
                    form { margin: 100px auto; width: 400px; padding: 20px; background: white; border-radius: 8px; }
                    textarea { width: 100%; height: 100px; margin: 8px 0; }
                    button { background: #2196F3; color: white; padding: 10px; border: none; width: 100%; }
                </style>
                </head>
                <body>
                    <form method="POST">
                        <h2>Leave Request</h2>
                        <textarea name="reason" placeholder="Reason for leave" required></textarea>
                        <button type="submit">Submit</button>
                    </form>
                </body>
                </html>
            """;
            sendResponse(exchange, html);
        }
    }

    static class ViewHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder table = new StringBuilder();
            table.append("<table border='1' style='border-collapse: collapse; width: 100%;'>");
            table.append("<tr><th>Name</th><th>ID</th><th>Reason</th></tr>");
            for (LeaveRequest req : requests) {
                table.append("<tr><td>").append(req.employeeName)
                     .append("</td><td>").append(req.employeeId)
                     .append("</td><td>").append(req.reason).append("</td></tr>");
            }
            table.append("</table>");

            String html = """
                <html>
                <head>
                <style>
                    body { font-family: Arial; background: #fff3e6; }
                    table { background: white; margin: 20px auto; }
                    th, td { padding: 8px; }
                </style>
                </head>
                <body>
                    <h2>Leave Requests</h2>
                    """ + table + """
                </body>
                </html>
            """;
            sendResponse(exchange, html);
        }
    }

    private static void sendResponse(HttpExchange exchange, String response) throws IOException {
        exchange.sendResponseHeaders(200, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}
