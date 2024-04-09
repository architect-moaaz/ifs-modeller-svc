package io.intelliflow.utils;

import io.intelliflow.service.FileOperations;

import javax.enterprise.context.ApplicationScoped;
import javax.tools.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class CreateClassOnTheGo {

    public Class<?> createClass(String className) throws IOException, ClassNotFoundException {

	System.out.println("Class Name Rcvd :"+className);
        File sourceFile = new File(className.replaceAll("\\s", ""),".java");

        String classname = sourceFile.getName().split("\\.")[0];

        String sourceCode = "public class " + classname + "{ }";

 String path = sourceFile.getPath();
        System.out.println("sourceFile path : "+path);

        try (FileWriter writer = new FileWriter(sourceFile)) {
            writer.write(sourceCode);
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        File parentDirectory = sourceFile.getParentFile();
        fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singletonList(parentDirectory));
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(List.of(sourceFile));
        compiler.getTask(null, fileManager, null, null, null, compilationUnits).call();
        fileManager.close();

        FileOperations.deleteFiles(sourceFile.toPath());
        try (URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{parentDirectory.toURI().toURL()})) {
            return classLoader.loadClass(classname);
        }
    }
}
