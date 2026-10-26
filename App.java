import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class App {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        List<String> todos = new ArrayList<>();
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            String message;
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            if (path.equals("/add") && method.equals("POST")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String value = body.substring(5);
                String todo = URLDecoder.decode(value, StandardCharsets.UTF_8);
                if (!todo.isEmpty()) {
                    todos.add(todo);
                }
                exchange.getResponseHeaders().set("Location", "/");
                exchange.sendResponseHeaders(303, -1);
                exchange.close();
                return;
            } else if (path.equals("/")) {
                String html = "<form method='post' action='/add'><input name='todo'><button>追加</button></form>";
                html += "<ul>";
                for (String todo : todos) {
                    html += "<li>" + todo + "</li>";
                }
                html += "</ul>";
                message = html;
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            } else if (path.equals("/hello")) {
                String query = exchange.getRequestURI().getRawQuery();
                System.out.println("query = " + query);
                String name = query == null
                        ? "ゲスト"
                        : URLDecoder.decode(query.substring(5), StandardCharsets.UTF_8);
                message = "こんにちは、" + name + "さん！";
            } else if (path.equals("/bye")) {
                message = "さようなら！";
            } else if (path.equals("/moon")) {
                message = "月が綺麗ですね！";
            } else {
                message = "ページが見つかりません";
            }
            byte[] body = message.getBytes("UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.getResponseBody().close();
        });
        server.start();
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）");
    }
}
