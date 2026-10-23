
// Webサーバー機能を使えるようにします。
import com.sun.net.httpserver.HttpServer;
// IPアドレスとポート番号を指定できるようにします。
import java.net.InetSocketAddress;

// Appという名前のクラスを定義します。
public class App {
    // プログラムを開始するmainメソッドです。
    public static void main(String[] args) throws Exception {
        // 8080番ポートで待ち受けるWebサーバーを用意します。
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        // "/"へのアクセスを処理します。
        server.createContext("/", exchange -> {
            // ブラウザへ返す文字を用意します。
            String message = "こんにちは";
            // 文字コードがUTF-8であることをブラウザへ伝えます。
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            // 文字をUTF-8のバイト列（送信用のデータ）へ変換します。
            byte[] body = message.getBytes("UTF-8");
            // 正常終了を表す200と、データの長さを送ります。
            exchange.sendResponseHeaders(200, body.length);
            // ブラウザへデータを書き込みます。
            exchange.getResponseBody().write(body);
            // データの送り返しを終了します。
            exchange.getResponseBody().close();
            // "/"へのアクセス処理を閉じます。
        });
        // Webサーバーを起動します。
        server.start();
        // 起動したことと、開くアドレスを表示します。
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）");
        // mainメソッドを閉じます。
    }
    // Appクラスを閉じます。
}
