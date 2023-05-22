package com.langFeautreMigration.projectCode;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class main {
    public static void main(String[] args) throws FileNotFoundException {

        //basic UI asking for the project directory and the operation mode
        String DirPath;
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please input the directory path: ");
        DirPath = scanner.nextLine();
        System.out.println("Please select how you want how to apply\n(1) For the entire project (Default)\n(2) Ask for every file");
        int mode = scanner.nextInt();

        //instantiating an object of type LoomMigrationEngine and passing it to the dirExplorer
        LoomMigrationEngine engine = new LoomMigrationEngine(mode);
        DirExplorer dirExplorer = new DirExplorer(engine);

        //dirExplorer is the driver class of this software... it recursively visits all files in the given directory
        // and applies edits only to "*.java" files.
        dirExplorer.explore(new File(DirPath));

        //last but not least we ask the software to print the statistics it collected.
        engine.printStats();
    }
}
