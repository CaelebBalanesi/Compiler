void main(String[] args) {
    String filename = "main.cj";
    if (args.length > 0) filename = args[0];

    Lexer lexer = new Lexer();
    try {
        List<Token> tokens = lexer.tokenize(new File(filename));

        IO.println("Tokens:\n\n");
        for (Token t : tokens) {
            IO.println(t);
        }
        IO.println("-----------------------------------");
        IO.println("Atoms:\n\n");

        Parser parser = new Parser(tokens);
        parser.parseProgram();
        parser.printAtoms();

        IO.println("-----------------------------------");
        IO.println("Machine Code:\n\n");
        CodeGenerator codeGen = new CodeGenerator();
        List<Integer> words = codeGen.generate(parser.atoms);
        for (int w : words) {
            System.out.println(Instruction.toBinary32(w));
        }
    } catch (FileNotFoundException e) {
        System.err.println("Error: file not found: " + filename);
        IO.println("Create source code in a file called: " + filename);
    } catch (RuntimeException e) {
        System.err.println("Parse error: " + e.getMessage());
    }
}
