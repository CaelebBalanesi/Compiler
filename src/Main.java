import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

enum States {
    START,
    ASSIGN_LHS,
    ASSIGN_VALUE,
    VAR_DEC,
    VAR_NAME_DEC,
    VAR_INIT,
    FOR_LOOP,
    FOR_DECL,
    FOR_COND,
    FOR_CTRL,
    FOR_BLOCK,
    WHILE_LOOP,
    WHILE_COND,
    WHILE_BLOCK,
    IF_STMT,
    ELIF_STMT,
    ELSE_STMT,
    COND_BLOCK
}

enum Events {
    NUM,
    DEC,
    IDENTIFIER,
    VALUE,
    END,
    FOR,
    DURING,
    IF,
    ELIF,
    ELSE,
    PIPE,
    BLOCK_START,
    BLOCK_END
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

String nextToken(List<Token> tokens, String data) {
    data = data.trim();
    if (data.isEmpty()) return "";

    char c = data.charAt(0);

    // Delimiters
    if (c == '|' || c == '(' || c == ')' || c == ';') {
        tokens.add(new Token("DELIM", String.valueOf(c)));
        return data.substring(1).trim();
    }

    // Operators
    if ("+-*/".indexOf(c) != -1) {
        tokens.add(new Token("OPERATOR", String.valueOf(c)));
        return data.substring(1).trim();
    }

    // Comparison operators
    if (c == '<' || c == '>') {
        if (data.length() > 1 && data.charAt(1) == '=') {
            tokens.add(new Token("OPERATOR", data.substring(0, 2)));
            return data.substring(2).trim();
        } else {
            tokens.add(new Token("OPERATOR", String.valueOf(c)));
            return data.substring(1).trim();
        }
    }
    if (c == '=' || c == '!') {
        tokens.add(new Token("OPERATOR", String.valueOf(c)));
        return data.substring(1).trim();
    }

    // Identifiers or keywords
    if (Character.isLetter(c)) {
        int idx = 0;
        while (idx < data.length() && Character.isLetterOrDigit(data.charAt(idx))) {
            idx++;
        }
        String word = data.substring(0, idx);
        return switch (word) {
            case "num", "dec", "for", "during", "if", "elif", "else" -> {
                tokens.add(new Token("KEYWORD", word));
                yield data.substring(idx).trim();
            }
            default -> {
                tokens.add(new Token("IDENTIFIER", word));
                yield data.substring(idx).trim();
            }
        };
    }

    // Literals
    if (Character.isDigit(c)) {
        int idx = 0;
        while (idx < data.length() && Character.isDigit(data.charAt(idx))) {
            idx++;
        }
        String num = data.substring(0, idx);
        tokens.add(new Token("LITERAL", num));
        return data.substring(idx).trim();
    }

    // Unknown
    tokens.add(new Token("UNKNOWN", String.valueOf(c)));
    return data.substring(1).trim();
}

void main() {

    // transition_table[current_state][event] = next_state
    States[][] transition_table = new States[States.values().length][Events.values().length];

    // START
    transition_table[States.START.ordinal()][Events.NUM.ordinal()] = States.VAR_DEC;        // START -num-> VAR_DEC
    transition_table[States.START.ordinal()][Events.DEC.ordinal()] = States.VAR_DEC;        // START -dec-> VAR_DEC
    transition_table[States.START.ordinal()][Events.IDENTIFIER.ordinal()] = States.ASSIGN_LHS; // START -identifier-> ASSIGN_LHS
    transition_table[States.START.ordinal()][Events.FOR.ordinal()] = States.FOR_LOOP;       // START -for-> FOR_LOOP
    transition_table[States.START.ordinal()][Events.DURING.ordinal()] = States.WHILE_LOOP;  // START -during-> WHILE_LOOP
    transition_table[States.START.ordinal()][Events.IF.ordinal()] = States.IF_STMT;         // START -if-> IF_STMT
    transition_table[States.START.ordinal()][Events.ELIF.ordinal()] = States.ELIF_STMT;     // START -elif-> ELIF_STMT
    transition_table[States.START.ordinal()][Events.ELSE.ordinal()] = States.ELSE_STMT;     // START -else-> ELSE_STMT

    //VAR_DEC
    transition_table[States.VAR_DEC.ordinal()][Events.IDENTIFIER.ordinal()] = States.VAR_NAME_DEC;

    //VAR_NAME_DEC
    transition_table[States.VAR_NAME_DEC.ordinal()][Events.VALUE.ordinal()] = States.VAR_INIT;
    transition_table[States.VAR_NAME_DEC.ordinal()][Events.PIPE.ordinal()] = States.START;

    //ASSIGN_LHS
    transition_table[States.ASSIGN_LHS.ordinal()][Events.VALUE.ordinal()] = States.ASSIGN_VALUE;

    //ASSIGN_VALUE
    transition_table[States.ASSIGN_VALUE.ordinal()][Events.PIPE.ordinal()] = States.START;

    //WHILE_LOOP
    transition_table[States.WHILE_LOOP.ordinal()][Events.PIPE.ordinal()] = States.WHILE_COND;



    try {
        File myFile = new File("main.cj");
        Scanner myReader = new Scanner(myFile);

        States current_state = States.START;

        while (myReader.hasNextLine()) {
            String data = myReader.nextLine();

            boolean eof = false;
            List<Token> tokens = new ArrayList<>();

            while (!eof) {
                IO.println(current_state);
                IO.println(tokens);
                switch (current_state) {

                    if (data.isEmpty()) {
                        eof = true;
                        break;
                    }

                    data = nextToken(tokens, data);
                    Token last_token = tokens.getLast();
                    case START -> {

                    
                        switch (last_token.type) {
                            case "KEYWORD" -> {
                                switch (last_token.value) {
                                    case "num" ->
                                            current_state = transition_table[current_state.ordinal()][Events.NUM.ordinal()];
                                    case "dec" ->
                                            current_state = transition_table[current_state.ordinal()][Events.DEC.ordinal()];
                                    case "for" ->
                                            current_state = transition_table[current_state.ordinal()][Events.FOR.ordinal()];
                                    case "during" ->
                                            current_state = transition_table[current_state.ordinal()][Events.DURING.ordinal()];
                                    case "if" ->
                                            current_state = transition_table[current_state.ordinal()][Events.IF.ordinal()];
                                    case "elif" ->
                                            current_state = transition_table[current_state.ordinal()][Events.ELIF.ordinal()];
                                    case "else" ->
                                            current_state = transition_table[current_state.ordinal()][Events.ELSE.ordinal()];
                                }
                            }
                            case "IDENTIFIER" ->
                                    current_state = transition_table[current_state.ordinal()][Events.IDENTIFIER.ordinal()];
                            case "LITERAL" ->
                                    current_state = transition_table[current_state.ordinal()][Events.VALUE.ordinal()];
                            case "DELIM" ->
                                    current_state = transition_table[current_state.ordinal()][Events.PIPE.ordinal()];
                        }
                    }

                    case VAR_DEC -> {

                        switch (last_token.type){
                            case "IDENTIFIER" ->
                                current_state = transition_table[current_state.ordinal()][Events.IDENTIFIER.ordinal()];
                            default ->
                                IO.println("Compilation Error: after the variable type should come the variable identifer");
                        }

                    }

                    case VAR_NAME_DEC -> {
                        
                        switch(last_token.type){
                            case "VALUE" -> 
                                current_state = transition_table[current_state.ordinal()][Events.VALUE.ordinal()];
                            case "DELIM" -> 
                                if(last_token.value == ';'){
                                    current_state = transition_table[current_state.ordinal()][Events.PIPE.ordinal()];
                                }
                                IO.println("Compilation Error: wrong delimiter used, end statements with \";\"");
                            default ->
                                IO.println("Compilation Error: must follow a name declaration with either a value or ;");
                        }
                    }

                    case VAR_INIT -> {
                        switch (last_token.type){
                            case "DELIM" -> 
                                if(last_token.value == ';'){
                                    current_state = transition_table[current_state.ordinal()][Events.PIPE.ordinal()];
                                }
                                IO.println("Compilation Error: wrong delimiter used, end statements with \";\"");
                            default ->
                                IO.println("Compilation Error: must end an assignment with ;");
                        }
                    }

                    case ASSIGN_LHS -> {
                        switch (last_token.type){
                            case "VALUE" -> 
                                current_state = transition_table[current_state.ordinal()][Events.VALUE.ordinal()];
                            default ->
                                IO.println("Compilation Error: Need to follow a variable identifier with a value to assign it to");
                        }
                    }

                    case ASSIGN_VALUE -> {
                        switch (last_token.type){
                            case "DELIM" -> 
                                if(last_token.value == ';'){
                                    current_state = transition_table[current_state.ordinal()][Events.PIPE.ordinal()];
                                }
                                IO.println("Compilation Error: wrong delimiter used, end statements with \";\"");
                            default ->
                                IO.println("Compilation Error: must end an assignment with ;");
                        }
                    }

                    default -> eof = true;
                }
            }
            for (Token t : tokens) {
                IO.println(t);
            }
        }
        myReader.close();
    } catch (FileNotFoundException e) {
        System.out.println("An error occurred: File not found.");
        e.printStackTrace();
    }
}