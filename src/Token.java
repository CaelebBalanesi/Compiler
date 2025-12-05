public class Token {
    public final String type;
    public final String value;

    public Token(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public String toString() {
        return "<" + type + ", " + value + ">";
    }
}
