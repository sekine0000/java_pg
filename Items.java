// Itemsという名前のクラス（プログラムの入れ物）を定義します。
public class Items {
    // プログラムを実行したとき、最初に動く部分です。
    public static void main(String[] args) {
        // 3件のTodo（やること）を、todosという配列（複数の値をまとめる箱）に入れます。
        String[] todos = { "牛乳を買う", "卵を買う", "パンを買う", "掃除をする" };
        // iを0から1ずつ増やし、配列の件数分だけ処理を繰り返します。
        for (int i = 0; i < todos.length; i++) {
            // i番目のTodoを<li>と</li>で囲み、ターミナルに1行で出力します。
            System.out.println("<li>" + todos[i] + "</li>");
            // for文（処理を繰り返す仕組み）の範囲をここで閉じます。
        }
        // mainメソッド（最初に動く部分）の範囲をここで閉じます。
    }
    // Itemsクラス（プログラムの入れ物）の範囲をここで閉じます。
}
