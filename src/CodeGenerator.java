import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CodeGenerator {
    private static class PendingJump {
        int instrIndex;
        String label;

        PendingJump(int instrIndex, String label) {
            this.instrIndex = instrIndex;
            this.label = label;
        }
    }

    private static final int ACCUMULATOR = 0;

    private final Map<String, Integer> dataTable = new LinkedHashMap<>();
    private final Map<String, Integer> labelTable = new LinkedHashMap<>();
    private final List<Integer> instructions = new ArrayList<>();
    private final List<PendingJump> pendingJumps = new ArrayList<>();

    private int nextDataAddr = 0;

    private int addrOf(String name) {
        if (name == null) return 0;

        Integer addr = dataTable.get(name);
        if (addr != null) {
            return addr;
        }

        int newAddr = nextDataAddr++;
        dataTable.put(name, newAddr);
        return newAddr;
    }

    public List<Integer> generate(List<Atom> atoms) {
        // First Pass
        for (Atom a : atoms) {
            switch (a.op) {
                case LBL -> handleLabel(a);
                case MOV -> handleMov(a);
                case ADD -> handleAdd(a);
                case SUB -> handleSub(a);
                case MUL -> handleMul(a);
                case DIV -> handleDiv(a);
                case TST -> handleTst(a);
                case JMP -> handleJmp(a);
                case HLT -> instructions.add(Instruction.hlt());
            }
        }

        // Second pass
        patchJumps();

        return instructions;
    }

    private void patchJumps() {
        for (PendingJump pj : pendingJumps) {
            Integer targetAddr = labelTable.get(pj.label);
            if (targetAddr == null) {
                throw new IllegalStateException("Undefined label: " + pj.label);
            }
            int word = instructions.get(pj.instrIndex);

            word &= ~(0xFFFFF << 12);

            word |= (targetAddr & 0xFFFFF) << 12;
            instructions.set(pj.instrIndex, word);
        }
    }

    private void handleLabel(Atom a) {
        if (a.label == null) {
            throw new IllegalArgumentException("LBL atom with null label: " + a);
        }
        labelTable.put(a.label, instructions.size());
    }

    private void handleMov(Atom a) {
        if (a.src1 == null || a.dest == null) {
            throw new IllegalArgumentException("MOV needs src1 and dest: " + a);
        }

        int srcAddr  = addrOf(a.src1);
        int destAddr = addrOf(a.dest);

        instructions.add(Instruction.lod(ACCUMULATOR, srcAddr));
        instructions.add(Instruction.sto(ACCUMULATOR, destAddr));
    }

    private void handleAdd(Atom a) {
        if (a.src1 == null || a.src2 == null || a.dest == null) {
            throw new IllegalArgumentException("ADD needs src1, src2, dest: " + a);
        }

        int addr1 = addrOf(a.src1);
        int addr2 = addrOf(a.src2);
        int destAddr = addrOf(a.dest);

        instructions.add(Instruction.lod(ACCUMULATOR, addr1));
        instructions.add(Instruction.add(ACCUMULATOR, addr2));
        instructions.add(Instruction.sto(ACCUMULATOR, destAddr));
    }

    private void handleSub(Atom a) {
        if (a.src1 == null || a.src2 == null || a.dest == null) {
            throw new IllegalArgumentException("SUB needs src1, src2, dest: " + a);
        }

        int addr1 = addrOf(a.src1);
        int addr2 = addrOf(a.src2);
        int destAddr = addrOf(a.dest);

        instructions.add(Instruction.lod(ACCUMULATOR, addr1));
        instructions.add(Instruction.sub(ACCUMULATOR, addr2));
        instructions.add(Instruction.sto(ACCUMULATOR, destAddr));
    }

    private void handleMul(Atom a) {
        if (a.src1 == null || a.src2 == null || a.dest == null) {
            throw new IllegalArgumentException("MUL needs src1, src2, dest: " + a);
        }

        int addr1 = addrOf(a.src1);
        int addr2 = addrOf(a.src2);
        int destAddr = addrOf(a.dest);

        instructions.add(Instruction.lod(ACCUMULATOR, addr1));
        instructions.add(Instruction.mul(ACCUMULATOR, addr2));
        instructions.add(Instruction.sto(ACCUMULATOR, destAddr));
    }

    private void handleDiv(Atom a) {
        if (a.src1 == null || a.src2 == null || a.dest == null) {
            throw new IllegalArgumentException("DIV needs src1, src2, dest: " + a);
        }

        int addr1 = addrOf(a.src1);
        int addr2 = addrOf(a.src2);
        int destAddr = addrOf(a.dest);

        instructions.add(Instruction.lod(ACCUMULATOR, addr1));
        instructions.add(Instruction.div(ACCUMULATOR, addr2));
        instructions.add(Instruction.sto(ACCUMULATOR, destAddr));
    }

    private void handleTst(Atom a) {
        if (a.src1 == null || a.src2 == null || a.cmp == null || a.label == null) {
            throw new IllegalArgumentException("TST needs src1, src2, cmp, label: " + a);
        }

        int addr1 = addrOf(a.src1);
        int addr2 = addrOf(a.src2);

        instructions.add(Instruction.lod(ACCUMULATOR, addr1));
        instructions.add(Instruction.cmp(ACCUMULATOR, a.cmp, addr2));

        int jmpIndex = instructions.size();
        instructions.add(Instruction.jmp(0));
        pendingJumps.add(new PendingJump(jmpIndex, a.label));
    }

    private void handleJmp(Atom a) {
        if (a.label == null) {
            throw new IllegalArgumentException("JMP needs target label: " + a);
        }
        int jmpIndex = instructions.size();
        instructions.add(Instruction.jmp(0));
        pendingJumps.add(new PendingJump(jmpIndex, a.label));
    }
}
