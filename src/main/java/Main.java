import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;

public class Main {
    public static final int WIDTH = 2;
    public static final int LENGTH = 6;
    public static final boolean LOGGING = false;
    public static final String LEVEL = "CLASS";
    private static MethodVisitor visitor;

    /**
     * 递归的找到当前目录(文件)下所有的文件
     * @param path
     * @param score
     * @param features
     */
    public static void parseFiles(String path, Double score, String features) {
        /*
        if (!file.exists())
            return;
        if (file.isFile()) {
            CompilationUnit cu;
            try {
                System.out.println("Parsing " + file.getName() + "...");
                cu = JavaParser.parse(file);
                if (!visitor.classStart(cu, score, features))
                    return;
                visitor.visit(cu, score);
                visitor.classEnd();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            return;
        }
        */
        CompilationUnit cu;
        try {
            System.out.println("Parsing " + path + "...");
            String[] elements = path.split("\\$");
            cu = JavaParser.parse(new File(elements[0] + ".java"));
            if (!visitor.classStart(path, score, features))
                return;
            if (elements.length > 1) {
                List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);
                for (ClassOrInterfaceDeclaration c : classes) {
                    if (c.getNameAsString().equals(elements[elements.length - 1])) {
                        c.accept(visitor, score);
                    }
                }
            } else {
                visitor.visit(cu, score);
            }
            visitor.classEnd();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
    public static void main(String[] args) {
        // 文件名前缀
        String inputName = "paths-18728";
        File file = new File(inputName + ".txt");
        visitor = new MethodVisitor();
        // 设定输出的context的文件名
        visitor.setOutputFileName(inputName + "-context.txt");
        if (LOGGING) {
            visitor.setLogPrefix(inputName);
        }
        try {
            List<String> lines = FileUtils.readLines(file);
            int i = 0;
            for (String line : lines) {
                // 输出当前进度
                System.out.println("[" + (i + 1) + "/" + lines.size() + "]");
                i = i + 1;
                int index1 = line.indexOf(",");
                String number = line.substring(0, index1);
                int index2 = line.indexOf(",", index1 + 1);
                String path = line.substring(index1 + 1, index2);
                String features = line.substring(index2 + 1);
                parseFiles(path, Double.parseDouble(number), features);
            }
            // 将Map存储到硬盘上
            visitor.save(inputName);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
}
