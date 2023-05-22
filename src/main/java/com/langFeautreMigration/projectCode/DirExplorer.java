package com.langFeautreMigration.projectCode;
import java.io.File;
import java.io.FileNotFoundException;

//This is the code driver.... it recursively checks a directory for java code files and apply edits to every file.
public class DirExplorer {
    //local parameter to keep the engine instance and use it to modify files.
    LoomMigrationEngine engine;

    //This class is what the driver uses to handle files and call the engine.
    public class FileHandler {
        void handle(int level, String path, File file) throws FileNotFoundException {
            System.out.println("Now I am checking: ." + path);
            engine.handle(file);
        }
    }

    //This class just filters to the files we are interested in... in this case the files which have the extension ".java"
    public static class Filter {
        boolean interested(int level, String path, File file){
            return path.endsWith(".java");
        }
    }
    private final FileHandler fileHandler = new FileHandler();
    private final Filter filter = new Filter();

    //constructor to instantiate the engine variable.
    public DirExplorer(LoomMigrationEngine p_engine) {
        engine = p_engine;
    }

    //Explore function takes the root file/folder and starts keeping track of the directory visits this far.
    public void explore(File root) throws FileNotFoundException {
        explore(0, "", root);
    }

    //Recursively explore the files specified
    private void explore(int level, String path, File file) throws FileNotFoundException {
        if (file.isDirectory()) {
            //if it is a directory we recursively visit all its children.
            for (File child : file.listFiles()) {
                explore(level + 1, path + "/" + child.getName(), child);
            }
        } else {
            //if it is a file we check if we are interested
            if (filter.interested(level, path, file)) {
                //if we are we call the filehandler to modify the file.
                fileHandler.handle(level, path, file);
            }
        }
    }
}