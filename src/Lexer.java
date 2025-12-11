import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class Lexer {

    private enum States {
        START, IDENTIFIER, NUMBER, DECIMAL, OPERATOR, DELIM, ACCEPT, ERROR,
    }

    private enum Events {
        LETTER, DIGIT, DOT, OPERATOR, DELIM, WHITESPACE, UNKNOWN, END,
    }

    private static Events classifyChar(char c) {
        if (Character.isLetter(c)) return Events.LETTER;
        if (Character.isDigit(c)) return Events.DIGIT;
        if (c == '.') return Events.DOT;
        if ("+-*/=!<>".indexOf(c) != -1) return Events.OPERATOR;
        if ("();|".indexOf(c) != -1) return Events.DELIM;
        if (Character.isWhitespace(c)) return Events.WHITESPACE;
        return Events.UNKNOWN;
    }

    private static void emitToken(List<Token> tokens, String buffer, States state) {
        if (buffer == null || buffer.isEmpty()) return;
        buffer = buffer.trim();
        if (buffer.isEmpty()) return;

        if (state == States.IDENTIFIER) {
            if (buffer.equals("for") || buffer.equals("during") || buffer.equals("if") || buffer.equals("elif") || buffer.equals("else") || buffer.equals("num") || buffer.equals("dec")) {
                tokens.add(new Token("KEYWORD", buffer));
            } else {
                tokens.add(new Token("IDENTIFIER", buffer));
            }
        } else if (state == States.NUMBER || state == States.DECIMAL) {
            tokens.add(new Token("LITERAL", buffer));
        } else if (state == States.OPERATOR) {
            tokens.add(new Token("OPERATOR", buffer));
        } else if (state == States.DELIM) {
            tokens.add(new Token("DELIM", buffer));
        }
    }

    public List<Token> tokenize(File file) throws FileNotFoundException {
        // transition table
        States[][] transition_table = new States[States.values().length][Events.values().length];

        // START
        transition_table[States.START.ordinal()][Events.LETTER.ordinal()] = States.IDENTIFIER;
        transition_table[States.START.ordinal()][Events.DIGIT.ordinal()] = States.NUMBER;
        transition_table[States.START.ordinal()][Events.DOT.ordinal()] = States.ERROR;
        transition_table[States.START.ordinal()][Events.OPERATOR.ordinal()] = States.OPERATOR;
        transition_table[States.START.ordinal()][Events.DELIM.ordinal()] = States.DELIM;
        transition_table[States.START.ordinal()][Events.WHITESPACE.ordinal()] = States.START;
        transition_table[States.START.ordinal()][Events.UNKNOWN.ordinal()] = States.ERROR;
        transition_table[States.START.ordinal()][Events.END.ordinal()] = States.ACCEPT;

        // IDENTIFIER
        transition_table[States.IDENTIFIER.ordinal()][Events.LETTER.ordinal()] = States.IDENTIFIER;
        transition_table[States.IDENTIFIER.ordinal()][Events.DIGIT.ordinal()] = States.IDENTIFIER;
        transition_table[States.IDENTIFIER.ordinal()][Events.WHITESPACE.ordinal()] = States.ACCEPT;
        transition_table[States.IDENTIFIER.ordinal()][Events.OPERATOR.ordinal()] = States.ACCEPT;
        transition_table[States.IDENTIFIER.ordinal()][Events.DELIM.ordinal()] = States.ACCEPT;
        transition_table[States.IDENTIFIER.ordinal()][Events.END.ordinal()] = States.ACCEPT;

        // NUMBER
        transition_table[States.NUMBER.ordinal()][Events.DIGIT.ordinal()] = States.NUMBER;
        transition_table[States.NUMBER.ordinal()][Events.DOT.ordinal()] = States.DECIMAL;
        transition_table[States.NUMBER.ordinal()][Events.WHITESPACE.ordinal()] = States.ACCEPT;
        transition_table[States.NUMBER.ordinal()][Events.OPERATOR.ordinal()] = States.ACCEPT;
        transition_table[States.NUMBER.ordinal()][Events.DELIM.ordinal()] = States.ACCEPT;
        transition_table[States.NUMBER.ordinal()][Events.END.ordinal()] = States.ACCEPT;

        // DECIMAL
        transition_table[States.DECIMAL.ordinal()][Events.DIGIT.ordinal()] = States.DECIMAL;
        transition_table[States.DECIMAL.ordinal()][Events.WHITESPACE.ordinal()] = States.ACCEPT;
        transition_table[States.DECIMAL.ordinal()][Events.OPERATOR.ordinal()] = States.ACCEPT;
        transition_table[States.DECIMAL.ordinal()][Events.DELIM.ordinal()] = States.ACCEPT;
        transition_table[States.DECIMAL.ordinal()][Events.END.ordinal()] = States.ACCEPT;
        transition_table[States.DECIMAL.ordinal()][Events.DOT.ordinal()] = States.ERROR;

        // OPERATOR
        transition_table[States.OPERATOR.ordinal()][Events.OPERATOR.ordinal()] = States.OPERATOR;
        transition_table[States.OPERATOR.ordinal()][Events.WHITESPACE.ordinal()] = States.ACCEPT;
        transition_table[States.OPERATOR.ordinal()][Events.LETTER.ordinal()] = States.ACCEPT;
        transition_table[States.OPERATOR.ordinal()][Events.DIGIT.ordinal()] = States.ACCEPT;
        transition_table[States.OPERATOR.ordinal()][Events.DELIM.ordinal()] = States.ACCEPT;
        transition_table[States.OPERATOR.ordinal()][Events.END.ordinal()] = States.ACCEPT;

        // DELIM
        transition_table[States.DELIM.ordinal()][Events.WHITESPACE.ordinal()] = States.ACCEPT;
        transition_table[States.DELIM.ordinal()][Events.LETTER.ordinal()] = States.ACCEPT;
        transition_table[States.DELIM.ordinal()][Events.DIGIT.ordinal()] = States.ACCEPT;
        transition_table[States.DELIM.ordinal()][Events.OPERATOR.ordinal()] = States.ACCEPT;
        transition_table[States.DELIM.ordinal()][Events.DELIM.ordinal()] = States.ACCEPT;
        transition_table[States.DELIM.ordinal()][Events.END.ordinal()] = States.ACCEPT;

        // ACCEPT
        for (Events e : Events.values()) {
            transition_table[States.ACCEPT.ordinal()][e.ordinal()] = States.START;
        }

        // ERROR
        for (Events e : Events.values()) {
            transition_table[States.ERROR.ordinal()][e.ordinal()] = States.ERROR;
        }

        Scanner reader = new Scanner(file);
        List<Token> tokens = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        States state = States.START;

        while (reader.hasNextLine()) {
            String line = reader.nextLine();

            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);

                // Look ahead for size two operators
                if ("+-*/=!<>".indexOf(c) != -1) {
                    String two = "" + c + ((i + 1 < line.length()) ? line.charAt(i + 1) : '\0');

                    if (two.equals(">=") || two.equals("<=")) {
                        tokens.add(new Token("OPERATOR", two));
                        i++;
                    } else {
                        tokens.add(new Token("OPERATOR", "" + c));
                    }
                    continue;
                }

                Events event = classifyChar(c);
                States nextState = transition_table[state.ordinal()][event.ordinal()];
                if (nextState == null) nextState = States.ERROR;

                if (nextState == States.ACCEPT) { // Character completes the token so emit to token list
                    emitToken(tokens, buffer.toString(), state);
                    buffer.setLength(0);
                    state = States.START;

                    if (event != Events.WHITESPACE) {
                        i--;
                    }
                } else if (nextState == States.ERROR) { // Character is invalid so add error token and clear buffer
                    tokens.add(new Token("ERROR", String.valueOf(c)));
                    buffer.setLength(0);
                    state = States.START;
                } else { // Character is valid so add to buffer for token
                    buffer.append(c);
                    state = nextState;
                }
            }

            States nextState = transition_table[state.ordinal()][Events.END.ordinal()];
            if (nextState == States.ACCEPT) {
                emitToken(tokens, buffer.toString(), state);
                buffer.setLength(0);
                state = States.START;
            }
        }

        reader.close();
        if (!buffer.isEmpty()) emitToken(tokens, buffer.toString(), state);
        tokens.add(new Token("EOF", "EOF"));
        return tokens;
    }
}
