import com.github.javaparser.utils.CodeGenerationUtils;
import com.github.javaparser.utils.SourceRoot;

public class testcase {

    private final void main(String[] args) {
        Thread myThread = Thread.ofVirtual().unstarted(() -> {
            System.out.println("HelloWorld!");
        });
    }
}

