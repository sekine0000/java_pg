public class Items {
    public static void main(String[] args) {
        String[] todos = { "牛乳を買う", "", "パンを買う", "掃除をする" };
        boolean[] done = { true, false, false, false };
        for (int i = 0; i < todos.length; i++) {
            if (!todos[i].isEmpty()) {
                System.out.println("<li>" + (done[i] ? "[済] " : "") + todos[i] + "</li>");
            }
        }
    }
}
