import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection; // ★ SQLite保存用に変更
import java.sql.DriverManager; // ★ SQLite保存用に変更
import java.sql.PreparedStatement; // ★ SQLite保存用に変更
import java.sql.ResultSet; // ★ SQLite保存用に変更
import java.sql.SQLException; // ★ SQLite保存用に変更
import java.sql.Statement; // ★ SQLite保存用に変更
import java.util.ArrayList;
import java.util.List;

class SqliteTodo { // ★ 既存のMain.javaにあるTodoクラスとの名前衝突を回避
    private final int id;
    private final String title;
    private final boolean done;

    SqliteTodo(int id, String title, boolean done) { // ★ DBから完了状態も復元
        this.id = id;
        this.title = title;
        this.done = done; // ★
    }

    int getId() {
        return id;
    }

    String getTitle() {
        return title;
    }

    boolean isDone() {
        return done;
    }
}

public class App {
    private static final String DB_URL = "jdbc:sqlite:todos.db"; // ★ CSVからSQLiteへ変更

    private static Connection connect() throws SQLException { // ★
        return DriverManager.getConnection(DB_URL); // ★
    }

    private static void createTable() throws SQLException { // ★
        String sql = "CREATE TABLE IF NOT EXISTS todos (" // ★
                + "id INTEGER PRIMARY KEY, title TEXT, done INTEGER)"; // ★
        try (Connection connection = connect(); // ★
                Statement statement = connection.createStatement()) { // ★
            statement.execute(sql); // ★
        }
    }

    private static List<SqliteTodo> findAll() throws SQLException { // ★ SELECTで一覧を取得
        List<SqliteTodo> todos = new ArrayList<>(); // ★
        String sql = "SELECT id, title, done FROM todos ORDER BY id"; // ★
        try (Connection connection = connect(); // ★
                Statement statement = connection.createStatement(); // ★
                ResultSet results = statement.executeQuery(sql)) { // ★
            while (results.next()) { // ★
                todos.add(new SqliteTodo(results.getInt("id"), results.getString("title"), // ★
                        results.getInt("done") != 0)); // ★
            }
        }
        return todos; // ★
    }

    private static void add(String title) throws SQLException { // ★ INSERTに変更
        String sql = "INSERT INTO todos (title, done) VALUES (?, 0)"; // ★
        try (Connection connection = connect(); // ★
                PreparedStatement statement = connection.prepareStatement(sql)) { // ★
            statement.setString(1, title); // ★
            statement.executeUpdate(); // ★
        }
    }

    private static void toggleDone(int id) throws SQLException { // ★ UPDATEに変更
        String sql = "UPDATE todos SET done = CASE done WHEN 0 THEN 1 ELSE 0 END WHERE id = ?"; // ★
        try (Connection connection = connect(); // ★
                PreparedStatement statement = connection.prepareStatement(sql)) { // ★
            statement.setInt(1, id); // ★
            statement.executeUpdate(); // ★
        }
    }

    private static void delete(int id) throws SQLException { // ★ DELETEに変更
        String sql = "DELETE FROM todos WHERE id = ?"; // ★
        try (Connection connection = connect(); // ★
                PreparedStatement statement = connection.prepareStatement(sql)) { // ★
            statement.setInt(1, id); // ★
            statement.executeUpdate(); // ★
        }
    }

    private static void deleteCompleted() throws SQLException { // ★ 完了済みもDELETE
        String sql = "DELETE FROM todos WHERE done <> 0"; // ★
        try (Connection connection = connect(); // ★
                PreparedStatement statement = connection.prepareStatement(sql)) { // ★
            statement.executeUpdate(); // ★
        }
    }

    private static Integer queryId(HttpExchange exchange) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null || !query.startsWith("id=") || query.length() <= 3)
            return null;
        try {
            return Integer.parseInt(query.substring(3));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void redirect(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Location", "/");
        exchange.sendResponseHeaders(303, -1);
        exchange.close();
    }

    private static void send(HttpExchange exchange, int status, String type, String message)
            throws IOException {
        byte[] body = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type + "; charset=UTF-8");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException { // JSONレスポンスを送信する
        byte[] body = json.getBytes(StandardCharsets.UTF_8); // JSONをUTF-8のバイト列に変換する
        exchange.getResponseHeaders().set("Content-Type", "application/json"); // charsetなしのJSON形式を指定する
        exchange.sendResponseHeaders(status, body.length); // ステータスと本文サイズを送信する
        exchange.getResponseBody().write(body); // JSON本文を書き込む
        exchange.close(); // 通信を終了する
    } // JSON送信処理を終了する

    private static String todosToJson(List<SqliteTodo> todos) { // Todo一覧をJSON配列へ変換する
        StringBuilder json = new StringBuilder("["); // JSON配列を開始する
        for (int i = 0; i < todos.size(); i++) { // 全Todoを順番に処理する
            if (i > 0) json.append(','); // 二件目以降の前に区切りを追加する
            SqliteTodo todo = todos.get(i); // 現在のTodoを取得する
            json.append("{\"title\":\""); // titleプロパティを開始する
            json.append(escapeJson(todo.getTitle())); // タイトルをエスケープして追加する
            json.append("\",\"done\":"); // doneプロパティを開始する
            json.append(todo.isDone()); // 完了状態を真偽値として追加する
            json.append('}'); // Todoオブジェクトを終了する
        } // 全Todoの処理を終了する
        return json.append(']').toString(); // JSON配列を閉じて返す
    } // Todo一覧の変換処理を終了する

    private static String escapeJson(String value) { // JSON文字列用にタイトルをエスケープする
        StringBuilder escaped = new StringBuilder(); // エスケープ後の文字列を保持する
        for (int i = 0; i < value.length(); i++) { // タイトルを一文字ずつ処理する
            char c = value.charAt(i); // 現在の文字を取得する
            switch (c) { // エスケープ方法を文字ごとに選ぶ
                case '"': escaped.append("\\\""); break; // 二重引用符をエスケープする
                case '\\': escaped.append("\\\\"); break; // バックスラッシュをエスケープする
                case '\b': escaped.append("\\b"); break; // バックスペースをエスケープする
                case '\f': escaped.append("\\f"); break; // フォームフィードをエスケープする
                case '\n': escaped.append("\\n"); break; // 改行をエスケープする
                case '\r': escaped.append("\\r"); break; // 復帰をエスケープする
                case '\t': escaped.append("\\t"); break; // タブをエスケープする
                default: // その他の文字を処理する
                    if (c < 0x20) { // JSONで直接使えない制御文字を判定する
                        escaped.append(String.format("\\u%04x", (int) c)); // Unicodeエスケープへ変換する
                    } else { // 通常の文字を処理する
                        escaped.append(c); // 文字をそのまま追加する
                    } // 制御文字の判定を終了する
            } // 文字ごとの分岐を終了する
        } // 一文字ずつの処理を終了する
        return escaped.toString(); // エスケープ済み文字列を返す
    } // JSON文字列のエスケープ処理を終了する

    private static String renderPage() throws SQLException { // ★ 表示時にDBから読む
        List<SqliteTodo> todos = findAll(); // ★
        int remainingCount = 0;
        boolean hasCompleted = false;
        for (SqliteTodo todo : todos) {
            if (todo.isDone())
                hasCompleted = true;
            else
                remainingCount++;
        }

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang='ja'><head><meta charset='UTF-8'>")
                .append("<title>Todoリスト</title><style>")
                .append("body{max-width:640px;margin:32px auto;padding:0 16px;font-size:16px;line-height:1.6}")
                .append("input,button{font-size:inherit}.status{display:flex;align-items:center;gap:12px}")
                .append("</style></head><body><h1>Todoリスト</h1>")
                .append("<form method='post' action='/add'><input name='todo' required>")
                .append("<button>追加</button></form><div class='status'><p>")
                .append(remainingCount == 0 ? "全部完了" : "あと" + remainingCount + "件")
                .append("</p>");
        if (hasCompleted)
            html.append("<a href='/delete-completed'>完了済みをすべて削除</a>");
        html.append("</div>");
        if (todos.isEmpty()) {
            html.append("<p>なにもありません</p>");
        } else {
            html.append("<ul>");
            for (SqliteTodo todo : todos) {
                String style = todo.isDone() ? " style='color:#999;text-decoration:line-through'" : "";
                html.append("<li><span").append(style).append(">")
                        .append(escapeHtml(todo.getTitle())).append("</span> ")
                        .append("<a href='/done?id=").append(todo.getId()).append("'>完了</a> ")
                        .append("<a href='/delete?id=").append(todo.getId()).append("'>削除</a></li>");
            }
            html.append("</ul>");
        }
        return html.append("</body></html>").toString();
    }

    public static void main(String[] args) throws Exception {
        createTable(); // ★ 起動時にテーブルを作成
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/todos", exchange -> { // Todo一覧APIを登録する
            if (!exchange.getRequestMethod().equals("GET")) { // GET以外のリクエストを拒否する
                exchange.getResponseHeaders().set("Allow", "GET"); // 利用可能なメソッドを通知する
                exchange.sendResponseHeaders(405, -1); // 本文なしで405を返す
                exchange.close(); // 通信を終了する
                return; // 処理を打ち切る
            } // メソッド判定を終了する
            try { // データベース処理の例外を捕捉する
                sendJson(exchange, 200, todosToJson(findAll())); // 全TodoをJSONで返す
            } catch (SQLException e) { // データベースエラーを処理する
                e.printStackTrace(); // エラー内容を標準エラーへ出力する
                sendJson(exchange, 500, "{\"error\":\"データベースエラー\"}"); // JSON形式で500を返す
            } // 例外処理を終了する
        }); // Todo一覧APIの登録を終了する
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            try {
                if (path.equals("/add") && method.equals("POST")) {
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    String value = body.startsWith("todo=") ? body.substring(5) : "";
                    String title = URLDecoder.decode(value, StandardCharsets.UTF_8);
                    if (!title.strip().isEmpty())
                        add(title); // ★ INSERT
                    redirect(exchange);
                } else if (path.equals("/done") && method.equals("GET")) {
                    Integer id = queryId(exchange);
                    if (id != null)
                        toggleDone(id); // ★ UPDATE
                    redirect(exchange);
                } else if (path.equals("/delete") && method.equals("GET")) {
                    Integer id = queryId(exchange);
                    if (id != null)
                        delete(id); // ★ DELETE
                    redirect(exchange);
                } else if (path.equals("/delete-completed") && method.equals("GET")) {
                    deleteCompleted(); // ★ DELETE
                    redirect(exchange);
                } else if (path.equals("/") && method.equals("GET")) {
                    send(exchange, 200, "text/html", renderPage()); // ★ SELECT結果を表示
                } else {
                    send(exchange, 404, "text/plain", "ページが見つかりません");
                }
            } catch (SQLException e) { // ★ DBエラーをHTTP 500として処理
                e.printStackTrace(); // ★
                send(exchange, 500, "text/plain", "データベースエラーが発生しました"); // ★
            }
        });
        server.start();
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）");
    }
}
