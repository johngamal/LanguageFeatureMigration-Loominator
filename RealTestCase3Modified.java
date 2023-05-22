import com.github.javaparser.utils.CodeGenerationUtils;
import com.github.javaparser.utils.SourceRoot;

class Animal {
    void eat() {
        System.out.println("eating...");
    }
}

class Cat extends CompletableFuture {
    void runAsync(int x) {
        ExecutorService __Dummy_Virtual_Executor__ = Executors.newVirtualThreadPerTaskExecutor();
        System.out.println("meowing...");
        super.runAsync(x, __Dummy_Virtual_Executor__);
    }
}

class TestInheritance3 {
    public static void main(String[] args) {
        Cat c = new Cat();
        c.meow();
    }
}

