import java.util.ArrayList;
import java.util.List;

class Todo {
    String title;
    boolean done;

    Todo(String title, boolean done) {
        this.title = title;
        this.done = done;
    }

    String toItem() {
        if (done) {
            return "<li>[急] " + title + "</li>";
        }
        return "<li>" + title + "</li>";
    }
}

public class Main {
    public static void main(String[] args) {
        List<Todo> todos = new ArrayList<>();
        todos.add(new Todo("牛乳を買う", false));
        todos.add(new Todo("ゴミを出す", true));
        todos.add(new Todo("猫をなでる", true));

        for (Todo todo : todos) {
            System.out.println(todo.toItem());
        }
    }
}
