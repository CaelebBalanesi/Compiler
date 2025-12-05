import java.util.*;

public class Parser {
    private final List<Token> tokens;
    private int pos = 0;

    public List<Atom> atoms = new ArrayList<>();
    private int tempCounter = 0;
    private int labelCounter = 0;
    private String lastRelOp = null;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    private Token current() {
        if (pos < tokens.size()) return tokens.get(pos);
        return new Token("EOF", "EOF");
    }

    private boolean match(String expected) {
        Token t = current();
        if (t.value.equals(expected) || t.type.equals(expected)) {
            pos++;
            return true;
        }
        return false;
    }

    private void expect(String expected) {
        if (!match(expected)) {
            throw new RuntimeException("Syntax Error: Expected '" + expected + "' but found " + current());
        }
    }

    private boolean isType(Token t) {
        return t.value.equals("num") || t.value.equals("dec");
    }

    private boolean isRelOp(Token t) {
        return Arrays.asList(">", ">=", "<", "<=", "=", "!").contains(t.value);
    }

    private boolean isAddOp(Token t) {
        return Arrays.asList("+", "-").contains(t.value);
    }

    private boolean isMulOp(Token t) {
        return Arrays.asList("*", "/").contains(t.value);
    }

    private String newTemp() {
        return "t" + (tempCounter++);
    }

    private String newLabel() {
        return "L" + (labelCounter++);
    }

    private Integer getCmpCode(String op) {
        return switch (op) {
            case "=" -> 1;
            case "<" -> 2;
            case ">" -> 3;
            case "<=" -> 4;
            case ">=" -> 5;
            case "!=", "!" -> 6;
            default -> 0;
        };
    }

    public void printAtoms() {
        System.out.println("Atom Stream:");
        for (Atom a : atoms) {
            System.out.println(a);
        }
        System.out.println("-----------------------------------");
    }

    public void parseProgram() {
        parseStmtList();
        if (!current().type.equals("EOF") && !current().value.equals("EOF"))
            throw new RuntimeException("Unexpected tokens after end of program: " + current());
        System.out.println("Parsing complete: program is syntactically correct.");
    }

    private void parseStmtList() {
        while (startsStmt(current())) {
            parseStmt();
        }
    }

    private boolean startsStmt(Token t) {
        if (t == null) return false;
        return isType(t) || t.type.equals("IDENTIFIER") || t.value.equals("if") || t.value.equals("for") || t.value.equals("during");
    }

    private void parseStmt() {
        Token t = current();
        if (isType(t)) {
            parseVarDecl();
            expect(";");
        } else if (t.type.equals("IDENTIFIER")) {
            parseAssignment();
            expect(";");
        } else if (t.value.equals("if")) {
            parseIfStmt();
        } else if (t.value.equals("for")) {
            parseForStmt();
        } else if (t.value.equals("during")) {
            parseDuringStmt();
        } else {
            throw new RuntimeException("Invalid statement starting with " + t);
        }
    }

    private void parseVarDecl() {
        match("KEYWORD");
        String var = current().value;
        expect("IDENTIFIER");
        if (startsExpr(current())) {
            String val = parseExpr();
            atoms.add(new Atom(AtomOp.MOV, val, null, var, null, null));
        }
    }

    private void parseAssignment() {
        String var = current().value;
        expect("IDENTIFIER");
        String val = parseExpr();
        atoms.add(new Atom(AtomOp.MOV, val, null, var, null, null));
    }

    private void parseIfStmt() {
        expect("if");
        expect("|");
        String cond = parseExpr();
        expect("|");

        String elseLabel = newLabel();
        String endLabel = newLabel();
        Integer cmpCode = lastRelOp != null ? getCmpCode(lastRelOp) : 0;

        atoms.add(new Atom(AtomOp.TST, cond, "0", null, cmpCode, elseLabel));

        expect("(");
        parseStmtList();
        expect(")");
        atoms.add(new Atom(AtomOp.JMP, null, null, null, null, endLabel));
        atoms.add(new Atom(AtomOp.LBL, null, null, null, null, elseLabel));

        parseElifList();
        parseElseOpt();

        atoms.add(new Atom(AtomOp.LBL, null, null, null, null, endLabel));
    }

    private void parseElifList() {
        while (current().value.equals("elif")) {
            parseElif();
        }
    }

    private void parseElif() {
        expect("elif");
        expect("|");
        String cond = parseExpr();
        expect("|");

        String elseLabel = newLabel();
        Integer cmpCode = lastRelOp != null ? getCmpCode(lastRelOp) : 0;

        atoms.add(new Atom(AtomOp.TST, cond, "0", null, cmpCode, elseLabel));

        expect("(");
        parseStmtList();
        expect(")");
        atoms.add(new Atom(AtomOp.LBL, null, null, null, null, elseLabel));
    }

    private void parseElseOpt() {
        if (current().value.equals("else")) {
            expect("else");
            expect("(");
            parseStmtList();
            expect(")");
        }
    }

    private void parseForStmt() {
        expect("for");
        expect("|");
        parseForInit();
        expect("|");
        String condTemp = parseExpr();
        expect("|");
        String updateLabel = newLabel();
        parseForUpdate();
        expect("|");

        String loopStart = newLabel();
        String loopEnd = newLabel();
        Integer cmpCode = lastRelOp != null ? getCmpCode(lastRelOp) : 0;

        atoms.add(new Atom(AtomOp.LBL, null, null, null, null, loopStart));
        atoms.add(new Atom(AtomOp.TST, condTemp, "0", null, cmpCode, loopEnd));

        expect("(");
        parseStmtList();
        expect(")");

        atoms.add(new Atom(AtomOp.LBL, null, null, null, null, updateLabel));
        parseForUpdate();
        atoms.add(new Atom(AtomOp.JMP, null, null, null, null, loopStart));
        atoms.add(new Atom(AtomOp.LBL, null, null, null, null, loopEnd));
    }

    private void parseForInit() {
        if (isType(current())) parseVarDecl();
        else if (current().type.equals("IDENTIFIER")) parseAssignment();
    }

    private void parseForUpdate() {
        if (current().type.equals("IDENTIFIER")) parseAssignment();
    }

    private void parseDuringStmt() {
        expect("during");
        expect("|");
        String condTemp = parseExpr();
        expect("|");

        String loopStart = newLabel();
        String loopEnd = newLabel();
        Integer cmpCode = lastRelOp != null ? getCmpCode(lastRelOp) : 0;

        atoms.add(new Atom(AtomOp.LBL, null, null, null, null, loopStart));
        atoms.add(new Atom(AtomOp.TST, condTemp, "0", null, cmpCode, loopEnd));

        expect("(");
        parseStmtList();
        expect(")");

        atoms.add(new Atom(AtomOp.JMP, null, null, null, null, loopStart));
        atoms.add(new Atom(AtomOp.LBL, null, null, null, null, loopEnd));
    }

    private boolean startsExpr(Token t) {
        return t != null && (t.type.equals("LITERAL") || t.type.equals("IDENTIFIER") || t.value.equals("|"));
    }

    private String parseExpr() {
        return parseRelExpr();
    }

    private String parseRelExpr() {
        String left = parseAddExpr();
        if (isRelOp(current())) {
            String op = current().value;
            match(op);
            String right = parseAddExpr();
            lastRelOp = op;
            String result = newTemp();
            atoms.add(new Atom(AtomOp.SUB, left, right, result, null, null));
            return result;
        }
        lastRelOp = null;
        return left;
    }

    private String parseAddExpr() {
        String left = parseMulExpr();
        while (isAddOp(current())) {
            String op = current().value;
            match(op);
            String right = parseMulExpr();
            String result = newTemp();
            if (op.equals("+")) atoms.add(new Atom(AtomOp.ADD, left, right, result, null, null));
            else if (op.equals("-")) atoms.add(new Atom(AtomOp.SUB, left, right, result, null, null));
            left = result;
        }
        return left;
    }

    private String parseMulExpr() {
        String left = parsePrimary();
        while (isMulOp(current())) {
            String op = current().value;
            match(op);
            String right = parsePrimary();
            String result = newTemp();
            if (op.equals("*")) atoms.add(new Atom(AtomOp.MUL, left, right, result, null, null));
            else if (op.equals("/")) atoms.add(new Atom(AtomOp.DIV, left, right, result, null, null));
            left = result;
        }
        return left;
    }

    private String parsePrimary() {
        Token t = current();
        if (t.type.equals("LITERAL") || t.type.equals("IDENTIFIER")) {
            match(t.type);
            return t.value;
        } else if (t.value.equals("|")) {
            expect("|");
            String val = parseExpr();
            expect("|");
            return val;
        } else {
            throw new RuntimeException("Unexpected token in expression: " + t);
        }
    }
}
