import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Encoder {
    private int cnt;
    private List<String> names;
    private Map<String, Integer> mapping;
    public Encoder() {
        names = new ArrayList<>();
        mapping = new HashMap<>();
    }
    int getIndex(String name) {
        if (mapping.containsKey(name)) {
            return mapping.get(name);
        }
        names.add(name);
        mapping.put(name, cnt + 1);
        cnt = cnt + 1;
        return cnt;
    }
    public void save(String name) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(name);
            for (int i = 0; i < names.size(); i++) {
                String s = Integer.toString(i + 1) + "," + names.get(i) + "\n";
                fileOutputStream.write(s.getBytes());
            }
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
