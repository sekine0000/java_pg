import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

public class App {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);// 【1】
        server.createContext("/", exchange -> {// 【毎】☆
            // アクセスされたパスを取り出す
            String path = exchange.getRequestURI().getPath();
            String message;
            // パスが /hello と同じか比べる
            if (path.equals("/hello")) {
                message = "こんにちは！";
            } else if (path.equals("/bye")) {
                message = "さようなら！";
            } else {
                message = "ページが見つかりません";
            }
            exchange.getResponseHeaders().set("Content -Type", "text/plain; charset=UTF-8");// 【毎】
            byte[] body = message.getBytes("UTF-8");// 【毎】
            exchange.sendResponseHeaders(200, body.length);// 【毎】
            exchange.getResponseBody().write(body);// 【毎】
            exchange.getResponseBody().close();// 【毎】
        });
        server.start(); // 【 毎】☆
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）");// 【毎】☆
    }
}
