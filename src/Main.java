import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

enum States {
    START,
    IDENTIFIER,
    NUMBER,
    DECIMAL,
    OPERATOR,
    DELIM,
    ACCEPT,
    ERROR
}

enum Events {
    LETTER,
    DIGIT,
    DOT,
    OPERATOR,
    DELIM,
    WHITESPACE,
    UNKNOWN,
    END
}

static class Token {
    String type;
    String value;

    Token(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public String toString() {
        return "<" + type + ", " + value + ">";
    }
}

// Return what EVENT a character fits into
static Events classifyChar(char c) {
    if (Character.isLetter(c)) return Events.LETTER;
    if (Character.isDigit(c)) return Events.DIGIT;
    if (c == '.') return Events.DOT;
    if ("+-*/=!<>".indexOf(c) != -1) return Events.OPERATOR;
    if ("();|".indexOf(c) != -1) return Events.DELIM;
    if (Character.isWhitespace(c)) return Events.WHITESPACE;
    return Events.UNKNOWN;
}

// Add Token to token list with buffer data once accepted
static void emitToken(List<Token> tokens, String buffer, States state) {
    if (buffer.isEmpty()) return;

    if (state == States.IDENTIFIER) {
        if (buffer.equals("for") || buffer.equals("during") || buffer.equals("if") ||
                buffer.equals("elif") || buffer.equals("else") ||
                buffer.equals("num") || buffer.equals("dec")) {
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

void main() {
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

    // Read source from main.cj file
    try {
        File myFile = new File("main.cj");
        Scanner reader = new Scanner(myFile);
        List<Token> tokens = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();

        // Starting state
        States state = States.START;

        while (reader.hasNextLine()) {
            String line = reader.nextLine();

            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
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

            // End of the line so if final token is valid add to token list.
            States nextState = transition_table[state.ordinal()][Events.END.ordinal()];
            if (nextState == States.ACCEPT) {
                emitToken(tokens, buffer.toString(), state);
                buffer.setLength(0);
                state = States.START;
            }
        }
        reader.close();

        if (!buffer.isEmpty()) {
            emitToken(tokens, buffer.toString(), state);
        }

        for (Token t : tokens) {
            System.out.println(t);
        }

    } catch (FileNotFoundException e) {
        System.out.println("Error: File not found.");
    }
}