public class Item { // Item（プログラムの名前）というクラスを作ります
    public static void main(String[] args) { // プログラムが最初に実行する場所です
        String title = "学校に行く"; // String型（文字を入れる種類）の変数titleを作ります
        String html = "<li>" + title + "</li>"; // +で文字をつなぎ、HTMLの1行を作ります
        System.out.println(html); // 作ったHTMLをターミナル（文字を表示する画面）に出します
        boolean done = false;
        System.out.println(done);
        int count = 3;
        System.out.println("いま" + count + "件");
    } // mainの終わりです
} // Itemクラスの終わりです