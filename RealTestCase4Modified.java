import com.github.javaparser.utils.CodeGenerationUtils;
import com.github.javaparser.utils.SourceRoot;

class TestInheritance3 {

    public class fac extends ThreadFactory {
    }

    public static void main(String[] args) {
        //        ThreadFactory factory = new fac();
        ThreadFactory factory = Thread.ofVirtual().factory();
        Executors.newVirtualThreadPerTaskExecutor();
        // Same as newVirtualThreadPerTaskExecutor
        Executors.newThreadPerTaskExecutor(factory);
        Executors.newSingleThreadExecutor();
        Executors.newCachedThreadPool();
        Executors.newFixedThreadPool(1);
        Executors.newScheduledThreadPool(1);
        Executors.newSingleThreadScheduledExecutor();
    }
}

