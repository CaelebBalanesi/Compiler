public class Instruction {
    public static final int OPC_CLR = 0;
    public static final int OPC_ADD = 1;
    public static final int OPC_SUB = 2;
    public static final int OPC_MUL = 3;
    public static final int OPC_DIV = 4;
    public static final int OPC_JMP = 5;
    public static final int OPC_CMP = 6;
    public static final int OPC_LOD = 7;
    public static final int OPC_STO = 8;
    public static final int OPC_HLT = 9;

    private Instruction() {}

    /*
     * bits 0-3   : opcode (0-9)
     * bit 4      : 0
     * bits 5-7   : cmp (0-6)
     * bits 8-11  : r (0-15)
     * bits 12-31 : a (0 .. 2^20-1)
     */
    public static int encode(int opcode, int cmp, int r, int address) {
        int word = 0;
        word |= (opcode & 0xF);
        word |= (0 << 4);
        word |= (cmp & 0x7) << 5;
        word |= (r   & 0xF) << 8;
        word |= (address & 0xFFFFF) << 12;
        return word;
    }
    public static int clr(int r) {
        return encode(OPC_CLR, 0, r, 0);
    }

    public static int add(int r, int address) {
        return encode(OPC_ADD, 0, r, address);
    }

    public static int sub(int r, int address) {
        return encode(OPC_SUB, 0, r, address);
    }

    public static int mul(int r, int address) {
        return encode(OPC_MUL, 0, r, address);
    }

    public static int div(int r, int address) {
        return encode(OPC_DIV, 0, r, address);
    }

    public static int lod(int r, int address) {
        return encode(OPC_LOD, 0, r, address);
    }

    public static int sto(int r, int address) {
        return encode(OPC_STO, 0, r, address);
    }

    public static int cmp(int r, int cmpCode, int address) {
        return encode(OPC_CMP, cmpCode, r, address);
    }

    public static int jmp(int address) {
        return encode(OPC_JMP, 0, 0, address);
    }

    public static int hlt() {
        return encode(OPC_HLT, 0, 0, 0);
    }

    public static String toBinary32(int word) {
        String s = Integer.toBinaryString(word);
        if (s.length() < 32) {
            s = "0".repeat(32 - s.length()) + s;
        }
        return s;
    }
}
