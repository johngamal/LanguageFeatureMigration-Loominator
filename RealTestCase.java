import com.github.javaparser.utils.CodeGenerationUtils;
import com.github.javaparser.utils.SourceRoot;

public class testcase {
    private final void helloTest(){
    }
    private final void main(String[] args) {

        // Create a CompletableFuture that runs a task asynchronously
        ExecutorService realExecutor = Executors.newVirtualThreadPerTaskExecutor();

        // Create a CompletableFuture that runs a task asynchronously
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            // Perform some long-running task here
            System.out.println("Long-running task completed");
        });

        // Register a callback to execute after the long-running task completes
        future.thenRun(() -> {
            System.out.println("Callback executed after long-running task");
        });

        // Wait for the asynchronous task and callback to complete
        future.join();
    }
}
