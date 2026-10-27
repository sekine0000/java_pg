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
