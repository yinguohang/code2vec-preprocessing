import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import javax.naming.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;

public class MethodVisitor extends VoidVisitorAdapter<Double> {
    private List<Node> leaves;
    private Set<Node> visited;
    private Node start;
    private FileOutputStream fileOutputStream;
    private Encoder nodeEncoder;
    private Encoder pathEncoder;
    private FileOutputStream countLogFile;
    private int pathCount = 0;
    private List<ContextItem> contexts;

    /**
     * 设置Log文件的前缀
     * @param prefix
     */
    public void setLogPrefix(String prefix) {
        try {
            File file = new File(prefix + "");
            if (file.exists())
                file.delete();
            countLogFile = new FileOutputStream(new File(prefix + "-cnt.log"), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置context输出文件
     * @param name
     */
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

    /**
     * 找到所有的叶子节点
     * @param now
     */
    public void findLeaves(Node now) {
        if (now.getChildNodes().size() == 0) {
            leaves.add(now);
            return;
        }
        for (Node node : now.getChildNodes()) {
            findLeaves(node);
        }
    }
    public String getName(Node node) {
        String name = node.getClass().getName();
        return name.substring(name.lastIndexOf('.') + 1, name.length());
    }

    public int getScore(Node now) {
        if (now instanceof IfStmt) {
            return 2;
        }
        if (now instanceof SwitchStmt) {
            return 2;
        }
        if (now instanceof WhileStmt) {
            return 1;
        }
        if (now instanceof ForStmt) {
            return 1;
        }
        return 0;
    }

    /**
     * 遍历树
     * @param now 当前的节点
     * @param path 当前的路径
     * @param index 在向上的过程中，是父节点的第几个节点；在向下的过程中，index是-1；
     */
    public void surf(Node now, List<String> path, int index, int score) {
        visited.add(now);
        // 如果当前路径已经超过了设定的长度上限，则停止
        if (path.size() >= 2 * Main.LENGTH)
            return;
        // 如果不是MethodDeclaration，则可以向上走
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
                surf(parent, path, index, score + getScore(now));
                path.remove(path.size() - 1);
                path.remove(path.size() - 1);
            }
        }
        int i = 0;
        for (Node node : now.getChildNodes()) {
            // 如果超过宽度限制，则跳过
            if (index != -1 && (i < index - Main.WIDTH || i > index + Main.WIDTH)) {
                i = i + 1;
                continue;
            }
            i = i + 1;
            if (! visited.contains(node)) {
                visited.add(node);
                path.add("!");
                path.add(getName(node));
                surf(node, path, -1, score + getScore(now));
                path.remove(path.size() - 1);
                path.remove(path.size() - 1);
            }
        }
        if (path.size() == 1)
            return;
        // 将context信息输出到文件中/将信息存储在contexts中
        if (leaves.contains(now)) {
            pathCount++;
            Integer startIndex = nodeEncoder.getIndex(URLEncoder.encode(start.toString()));
            Integer pathIndex = pathEncoder.getIndex(String.join("", path));
            Integer endIndex = nodeEncoder.getIndex(URLEncoder.encode(now.toString()));
            int currentScore = score + (path.size() + 1) / 2;
            String text =  startIndex.toString() + "," + pathIndex.toString() + "," + endIndex.toString() + "," + Integer.toString(currentScore) +"\n";
            contexts.add(new ContextItem(currentScore, text));
        }
    }

    public void parse(MethodDeclaration root, Double score) {
        leaves = new ArrayList<>();
        pathCount = 0;
        if (Main.LEVEL == "METHOD") {
            try {
                fileOutputStream.write(("method_name:" + root.getNameAsString() + "\n").getBytes());
                fileOutputStream.write(("score:" + score.toString() + "\n").getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        findLeaves(root);
        for (Node leaf : leaves) {
            int pre = pathCount;
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
            surf(leaf, path, index, 0);
        }
        if (Main.LOGGING) {
            String output = leaves.size() + "," + pathCount + "\n";
            try {
                countLogFile.write(output.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void visit(MethodDeclaration n, Double arg) {
        super.visit(n, arg);
        parse(n, arg);
    }

    /**
     * 将node和path的信息保存下来
     * @param prefix
     */
    public void save(String prefix) {
        nodeEncoder.save(prefix + "-node.txt");
        pathEncoder.save(prefix + "-path.txt");
    }

    /**
     * 在每次访问类之前会调用的
     */
    public boolean classStart(String name, Double score, String features) {
        contexts = new ArrayList<>();
        if (Main.LEVEL == "CLASS") {
            try {
                fileOutputStream.write(("class_name:" + name + "\n").getBytes());
                fileOutputStream.write(("score:" + score.toString() + "\n").getBytes());
                fileOutputStream.write(("features:" + features + "\n").getBytes());
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public void classEnd() {
        Collections.sort(contexts, new Comparator<ContextItem>() {
            @Override
            public int compare(ContextItem o1, ContextItem o2) {
                return o2.getScore() - o1.getScore();
            }
        });
        for (ContextItem contextItem: contexts) {
            try {
                fileOutputStream.write(contextItem.getText().getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public MethodVisitor() {
        super();
        nodeEncoder = new Encoder();
        pathEncoder = new Encoder();
    }

}
