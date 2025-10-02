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
}

enum Events {
    NUM,
    DEC,
    IDENTIFIER,
    VALUE,
    END,
}

void main() {

    // transition_table[current_state][event] = next_state
    States[][] transition_table = new States[States.values().length][Events.values().length];

    // START
    transition_table[States.START.ordinal()][Events.NUM.ordinal()] = States.VAR_DEC; // START -num-> VAR_DEC
    transition_table[States.START.ordinal()][Events.DEC.ordinal()] = States.VAR_DEC; // START -dec-> VAR_DEC
    transition_table[States.START.ordinal()][Events.IDENTIFIER.ordinal()] = States.ASSIGN_LHS; // START -identifier-> ASSIGN_LHS

    // VAR_DEC
    transition_table[States.VAR_DEC.ordinal()][Events.IDENTIFIER.ordinal()] = States.VAR_NAME_DEC; // VAR_DEC -identifier-> VAR_NAME_DEC

    // VAR_NAME_DEC
    transition_table[States.VAR_NAME_DEC.ordinal()][Events.VALUE.ordinal()] = States.VAR_INIT; // VAR_NAME_DEC -value-> VAR_INIT
    transition_table[States.VAR_NAME_DEC.ordinal()][Events.END.ordinal()] = States.START; // VAR_NAME_DEC -end-> START

    // VAR_INIT
    transition_table[States.VAR_INIT.ordinal()][Events.END.ordinal()] = States.START; // VAR_INIT -end-> START

    // ASSIGN_LHS
    transition_table[States.ASSIGN_LHS.ordinal()][Events.VALUE.ordinal()] = States.ASSIGN_VALUE; // ASSIGN_LHS -value-> ASSIGN_VALUE

    // ASSIGN_VALUE
    transition_table[States.ASSIGN_VALUE.ordinal()][Events.END.ordinal()] = States.START; // ASSIGN_VALUE -end-> START

    try {
        File myFile = new File("main.cj");
        Scanner myReader = new Scanner(myFile);
        while (myReader.hasNextLine()) {
            String data = myReader.nextLine();

        }
        myReader.close();
    } catch (FileNotFoundException e) {
        System.out.println("An error occurred: File not found.");
        e.printStackTrace();
    }

}
