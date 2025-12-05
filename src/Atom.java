enum AtomOp {
    MOV,
    ADD,
    SUB,
    MUL,
    DIV,
    TST,
    JMP,
    LBL,
    HLT
}

public class Atom {
    public final AtomOp op;
    public final String src1;
    public final String src2;
    public final String dest;
    public final Integer cmp;
    public final String label;

    public Atom(AtomOp op, String src1, String src2,
                String dest, Integer cmp, String label) {
        this.op = op;
        this.src1 = src1;
        this.src2 = src2;
        this.dest = dest;
        this.cmp  = cmp;
        this.label = label;
    }

    @Override
    public String toString() {
        return "Atom(" + op + ", " + src1 + ", " + src2 + ", " +
                dest + ", " + cmp + ", " + label + ")";
    }
}
