public class ContextItem {
    private int score;
    private String text;
    ContextItem(int score, String text) {
        this.score = score;
        this.text = text;
    }

    public int getScore() {
        return score;
    }

    public String getText() {
        return text;
    }
}