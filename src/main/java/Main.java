import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {
    public static final int WIDTH = 2;
    public static final int LENGTH = 6;
    private static VoidVisitor<Double> visitor;
    public static class MethodVisitor extends VoidVisitorAdapter<Double> {
        private List<Node> leaves;
        private Set<Node> visited;
        private Node start;
        private FileOutputStream fileOutputStream;
        private Encoder nodeEncoder;
        private Encoder pathEncoder;
        public void setOutputFileName(String name) {
            try {
                File file = new File(name);
                if (file.exists())
                    file.delete();
                fileOutputStream = new FileOutputStream(new File(name), true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        public void find_leaves(Node now) {
            if (now.getChildNodes().size() == 0) {
                leaves.add(now);
                return;
            }
            for (Node node : now.getChildNodes()) {
                find_leaves(node);
            }
        }
        public String getName(Node node) {
            String name = node.getClass().getName();
            return name.substring(name.lastIndexOf('.') + 1, name.length());
        }
        public void surf(Node now, List<String> path, int index) {
            visited.add(now);
            if (path.size() >= 2 * LENGTH)
                return;
            if (!(now instanceof MethodDeclaration)) {
                if (now.getParentNode().isPresent() && !visited.contains(now.getParentNode().get()))  {
                    Node parent = now.getParentNode().get();
                    visited.add(parent);
                    path.add("?");
                    path.add(getName(parent));
                    for (int i = 0; i < parent.getChildNodes().size(); i++) {
                        if (parent.getChildNodes().get(i) == now) {
                            index = i;
                            break;
                        }
                    }
                    surf(parent, path, index);
                    path.remove(path.size() - 1);
                    path.remove(path.size() - 1);
                }
            }
            int i = 0;
            for (Node node : now.getChildNodes()) {
                if (index != -1 && (i < index - WIDTH || i > index + WIDTH)) {
                    i = i + 1;
                    continue;
                }
                i = i + 1;
                if (! visited.contains(node)) {
                    visited.add(node);
                    path.add("!");
                    path.add(getName(node));
                    surf(node, path, -1);
                    path.remove(path.size() - 1);
                    path.remove(path.size() - 1);
                }
            }
            if (path.size() == 1)
                return;
            if (leaves.contains(now)) {
                Integer startIndex = nodeEncoder.getIndex(URLEncoder.encode(start.toString()));
                Integer pathIndex = pathEncoder.getIndex(String.join("", path));
                Integer endIndex = nodeEncoder.getIndex(URLEncoder.encode(now.toString()));
                String text =  startIndex.toString() + "," + pathIndex.toString() + "," + endIndex.toString() + "\n";
                try {
                    fileOutputStream.write(text.getBytes());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        public void parse(MethodDeclaration root, Double score) {
            leaves = new ArrayList<>();
            try {
                fileOutputStream.write(("method_name:" + root.getNameAsString() + "\n").getBytes());
                fileOutputStream.write(("score:" + score.toString() + "\n").getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
            find_leaves(root);
//            contexts = new JSONArray();
            for (Node leaf : leaves) {
                visited = new HashSet<>();
                start = leaf;
                List<String> path = new ArrayList<>();
                path.add(getName(leaf));
                int index = 0;
                for (int i = 0; i < leaf.getParentNode().get().getChildNodes().size(); i++) {
                    if (leaf.getParentNode().get().getChildNodes().get(i) == leaf) {
                        index = i;
                        break;
                    }
                }
                surf(leaf, path, index);
            }
            System.out.println(leaves.size());
        }
        @Override
        public void visit(MethodDeclaration n, Double arg) {
            super.visit(n, arg);
            parse(n, arg);
        }

        public MethodVisitor() {
            super();
            nodeEncoder = new Encoder();
            pathEncoder = new Encoder();
        }

        public void save(String prefix) {
            nodeEncoder.save(prefix + "-node.txt");
            pathEncoder.save(prefix + "-path.txt");
        }
    }
    public static void parseFiles(File file, Double score) {
        if (!file.exists())
            return;
        if (file.isFile()) {
            CompilationUnit cu;
            try {
                System.out.println("Parsing " + file.getName() + "...");
                cu = JavaParser.parse(file);
                visitor.visit(cu, score);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            return;
        }
        for (File child : file.listFiles()) {
            parseFiles(child, score);
        }
    }
    public static void main(String[] args) {
        /*
        File file = new File("D:\\method_name_dataset\\train\\cassandra\\");
        visitor = new MethodVisitor();
        parseFiles(file);
        try {
            FileUtils.writeStringToFile(new File("test.json"), visitor.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        */
        String inputName = "paths-18728";
        File file = new File(inputName + ".txt");
        visitor = new MethodVisitor();
        ((MethodVisitor) visitor).setOutputFileName(inputName + "-context.txt");
        try {
            List<String> lines = FileUtils.readLines(file);
            int i = 0;
            for (String line : lines) {
                System.out.println("[" + i + "/" + lines.size() + "]");
                i = i + 1;
                int index = line.indexOf(",");
                String number = line.substring(0, index);
                String path = line.substring(index + 1);
                parseFiles(new File(path), Double.parseDouble(number));
            }
            ((MethodVisitor) visitor).save(inputName);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
}
