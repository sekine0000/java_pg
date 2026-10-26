import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

class Todo { // ★変更
    private final int id; // ★変更
    private final String title; // ★変更
    private boolean done; // ★変更

    Todo(int id, String title) { // ★変更
        this.id = id; // ★変更
        this.title = title; // ★変更
        this.done = false; // ★変更
    }

    int getId() { // ★変更
        return id; // ★変更
    }

    String getTitle() { // ★変更
        return title; // ★変更
    }

    boolean isDone() { // ★変更
        return done; // ★変更
    }

    void setDone(boolean done) { // ★変更
        this.done = done; // ★変更
    }
}

public class App {
    static List<Todo> todos = new ArrayList<>(); // ★変更
    static int nextId = 1; // ★変更

    public static void main(String[] args) throws Exception {
        todos.add(new Todo(nextId++, "牛乳を買う")); // ★変更
        Todo egg = new Todo(nextId++, "卵を買う"); // ★変更
        egg.setDone(true); // ★変更
        todos.add(egg); // ★変更
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            String message;
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            if (path.equals("/add") && method.equals("POST")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String value = body.substring(5);
                String title = URLDecoder.decode(value, StandardCharsets.UTF_8); // ★変更
                if (!title.strip().isEmpty()) { // ★変更
                    todos.add(new Todo(nextId, title)); // ★変更
                    nextId++; // ★変更
                }
                exchange.getResponseHeaders().set("Location", "/");
                exchange.sendResponseHeaders(303, -1);
                exchange.close();
                return;
            } else if (path.equals("/done") && method.equals("GET")) { // ★追加
                String query = exchange.getRequestURI().getQuery(); // ★追加
                if (query != null && query.startsWith("id=") && query.length() > 3) { // ★追加
                    try { // ★追加
                        int id = Integer.parseInt(query.substring(3)); // ★追加
                        for (Todo todo : todos) { // ★追加
                            if (todo.getId() == id) { // ★追加
                                todo.setDone(!todo.isDone()); // ★変更
                                break; // ★追加
                            } // ★追加
                        } // ★追加
                    } catch (NumberFormatException e) { // ★追加
                    } // ★追加
                } // ★追加
                exchange.getResponseHeaders().set("Location", "/"); // ★追加
                exchange.sendResponseHeaders(303, -1); // ★追加
                exchange.close(); // ★追加
                return; // ★追加
            } else if (path.equals("/delete") && method.equals("GET")) { // ★追加
                String query = exchange.getRequestURI().getQuery(); // ★追加
                if (query != null && query.startsWith("id=") && query.length() > 3) { // ★追加
                    try { // ★追加
                        int id = Integer.parseInt(query.substring(3)); // ★追加
                        todos.removeIf(todo -> todo.getId() == id); // ★修正
                    } catch (NumberFormatException e) { // ★追加
                    } // ★追加
                } // ★追加
                exchange.getResponseHeaders().set("Location", "/"); // ★追加
                exchange.sendResponseHeaders(303, -1); // ★追加
                exchange.close(); // ★追加
                return; // ★追加
            } else if (path.equals("/delete-completed") && method.equals("GET")) {
                for (int i = todos.size() - 1; i >= 0; i--) {
                    if (todos.get(i).isDone()) {
                        todos.remove(i);
                    }
                }
                exchange.getResponseHeaders().set("Location", "/");
                exchange.sendResponseHeaders(303, -1);
                exchange.close();
                return;
            } else if (path.equals("/")) {
                String html = "<!DOCTYPE html><html lang='ja'><head><meta charset='UTF-8'>"
                        + "<title>やることリスト</title>"
                        + "<style>body { max-width: 640px; margin: 32px auto; padding: 0 16px; font-size: 16px; line-height: 1.6; } input, button { font-size: inherit; } .status { display: flex; align-items: center; gap: 12px; }</style>"
                        + "</head><body><h1>やることリスト</h1>"
                        + "<form method='post' action='/add'><input name='todo'><button>追加</button></form>";
                int remainingCount = 0;
                boolean hasCompleted = false;
                for (Todo todo : todos) {
                    if (todo.isDone() == false) {
                        remainingCount++;
                    } else {
                        hasCompleted = true;
                    }
                }
                html += "<div class='status'><p>";
                if (remainingCount == 0) {
                    html += "全部完了";
                } else {
                    html += "あと" + remainingCount + "件";
                }
                html += "</p>";
                if (hasCompleted) {
                    html += "<a href='/delete-completed'>完了済みをすべて削除</a>";
                }
                html += "</div>";
                if (todos.isEmpty()) {
                    html += "<p>なにもありません</p>";
                } else {
                    html += "<ul>";
                    for (Todo todo : todos) { // ★変更
                        String titleStyle = todo.isDone()
                                ? " style='color: #999; text-decoration: line-through;'"
                                : "";
                        html += "<li><span" + titleStyle + ">" + todo.getTitle() + "</span> <a href='/done?id="
                                + todo.getId()
                                + "'>完了</a> <a href='/delete?id=" + todo.getId() + "'>削除</a></li>"; // ★追加
                    }
                    html += "</ul>";
                }
                html += "</body></html>";
                message = html;
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            } else {
                message = "ページが見つかりません"; // ★変更
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
