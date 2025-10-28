import java.io.*;

public class CodeWriter {
    private BufferedWriter out;
    private String currentFileName = "Sys"; // para variables static: FileName.index
    private int labelCounter = 0;
    private String currentFunction = ""; // nombre de la función actual

    public CodeWriter(String outputPath) throws IOException {
        out = new BufferedWriter(new FileWriter(outputPath));
    }

    public void setFileName(String fileName) {
        // fileName sin .vm
        this.currentFileName = fileName;
    }

    // Escribe código de inicialización (bootstrap)
    public void writeInit() throws IOException {
        // SP = 256
        out.write("// bootstrap\n");
        out.write("@256\n");
        out.write("D=A\n");
        out.write("@SP\n");
        out.write("M=D\n");
        // llamar a Sys.init
        writeCall("Sys.init", 0);
    }

    public void writeArithmetic(String command) throws IOException {
        out.write("// " + command + "\n");
        if (command.equals("add")) {
            writeBinary("M=M+D");
        } else if (command.equals("sub")) {
            writeBinary("M=M-D");
        } else if (command.equals("and")) {
            writeBinary("M=M&D");
        } else if (command.equals("or")) {
            writeBinary("M=M|D");
        } else if (command.equals("eq")) {
            writeComparison("JEQ");
        } else if (command.equals("gt")) {
            writeComparison("JGT");
        } else if (command.equals("lt")) {
            writeComparison("JLT");
        } else if (command.equals("neg")) {
            writeUnary("M=-M");
        } else if (command.equals("not")) {
            writeUnary("M=!M");
        } else {
            // desconocido -> ignorar
        }
    }

    private void writeBinary(String op) throws IOException {
        // SP-- ; D = *SP ; A = SP-1 ; M = M op D
        out.write("@SP\n");
        out.write("AM=M-1\n");
        out.write("D=M\n");
        out.write("A=A-1\n");
        out.write(op + "\n");
    }

    private void writeUnary(String op) throws IOException {
        // A = SP-1 ; M = op
        out.write("@SP\n");
        out.write("A=M-1\n");
        out.write(op + "\n");
    }

    private void writeComparison(String jump) throws IOException {
        String labelTrue = "TRUE$" + labelCounter;
        String labelEnd = "END$" + labelCounter;
        labelCounter++;

        out.write("@SP\n");
        out.write("AM=M-1\n"); // SP-- ; A = SP
        out.write("D=M\n");    // D = y
        out.write("A=A-1\n");  // A = x
        out.write("D=M-D\n");  // D = x - y
        out.write("@" + labelTrue + "\n");
        out.write("D;" + jump + "\n");
        // false
        out.write("@SP\n");
        out.write("A=M-1\n");
        out.write("M=0\n");
        out.write("@" + labelEnd + "\n");
        out.write("0;JMP\n");
        // true
        out.write("(" + labelTrue + ")\n");
        out.write("@SP\n");
        out.write("A=M-1\n");
        out.write("M=-1\n");
        // end
        out.write("(" + labelEnd + ")\n");
    }

    // push/pop
    public void writePushPop(CommandType type, String segment, int index) throws IOException {
        out.write("// " + (type == CommandType.C_PUSH ? "push " : "pop ") + segment + " " + index + "\n");
        if (type == CommandType.C_PUSH) {
            if (segment.equals("constant")) {
                out.write("@" + index + "\n");
                out.write("D=A\n");
                pushD();
            } else if (segment.equals("local") || segment.equals("argument") ||
                       segment.equals("this") || segment.equals("that")) {
                String base = segmentToBase(segment);
                // D = *(base + index)
                out.write("@" + base + "\n");
                out.write("D=M\n");
                out.write("@" + index + "\n");
                out.write("A=D+A\n");
                out.write("D=M\n");
                pushD();
            } else if (segment.equals("temp")) {
                int addr = 5 + index;
                out.write("@" + addr + "\n");
                out.write("D=M\n");
                pushD();
            } else if (segment.equals("pointer")) {
                String which = (index == 0 ? "THIS" : "THAT");
                out.write("@" + which + "\n");
                out.write("D=M\n");
                pushD();
            } else if (segment.equals("static")) {
                out.write("@" + currentFileName + "." + index + "\n");
                out.write("D=M\n");
                pushD();
            } else {
                // segmento desconocido
            }
        } else if (type == CommandType.C_POP) {
            if (segment.equals("local") || segment.equals("argument") ||
                segment.equals("this") || segment.equals("that")) {
                String base = segmentToBase(segment);
                // R13 = base + index
                out.write("@" + base + "\n");
                out.write("D=M\n");
                out.write("@" + index + "\n");
                out.write("D=D+A\n");
                out.write("@R13\n");
                out.write("M=D\n");
                // pop -> D
                popToD();
                // *R13 = D
                out.write("@R13\n");
                out.write("A=M\n");
                out.write("M=D\n");
            } else if (segment.equals("temp")) {
                int addr = 5 + index;
                popToD();
                out.write("@" + addr + "\n");
                out.write("M=D\n");
            } else if (segment.equals("pointer")) {
                String which = (index == 0 ? "THIS" : "THAT");
                popToD();
                out.write("@" + which + "\n");
                out.write("M=D\n");
            } else if (segment.equals("static")) {
                popToD();
                out.write("@" + currentFileName + "." + index + "\n");
                out.write("M=D\n");
            } else {
                // segmento desconocido
            }
        }
    }

    private String segmentToBase(String seg) {
        if (seg.equals("local")) return "LCL";
        if (seg.equals("argument")) return "ARG";
        if (seg.equals("this")) return "THIS";
        if (seg.equals("that")) return "THAT";
        return "";
    }

    private void pushD() throws IOException {
        out.write("@SP\n");
        out.write("A=M\n");
        out.write("M=D\n");
        out.write("@SP\n");
        out.write("M=M+1\n");
    }

    private void popToD() throws IOException {
        out.write("@SP\n");
        out.write("AM=M-1\n");
        out.write("D=M\n");
    }

    // flow control: label, goto, if-goto
    public void writeLabel(String label) throws IOException {
        String full = functionScopedLabel(label);
        out.write("// label " + full + "\n");
        out.write("(" + full + ")\n");
    }

    public void writeGoto(String label) throws IOException {
        String full = functionScopedLabel(label);
        out.write("// goto " + full + "\n");
        out.write("@" + full + "\n");
        out.write("0;JMP\n");
    }

    public void writeIf(String label) throws IOException {
        String full = functionScopedLabel(label);
        out.write("// if-goto " + full + "\n");
        popToD();
        out.write("@" + full + "\n");
        out.write("D;JNE\n");
    }

    private String functionScopedLabel(String label) {
        if (currentFunction == null || currentFunction.isEmpty()) {
            return label;
        } else {
            return currentFunction + "$" + label;
        }
    }

    // functions: function, call, return

    // function f n: declara n variables locales (push 0 n veces) y setea currentFunction
    public void writeFunction(String functionName, int nVars) throws IOException {
        this.currentFunction = functionName;
        out.write("// function " + functionName + " " + nVars + "\n");
        out.write("(" + functionName + ")\n");
        // inicializar nVars a 0 en la pila
        for (int i = 0; i < nVars; i++) {
            out.write("@0\n");
            out.write("D=A\n");
            pushD();
        }
    }

    // call f nArgs
    public void writeCall(String functionName, int nArgs) throws IOException {
        String returnLabel = "RET$" + functionName + "$" + labelCounter;
        labelCounter++;

        out.write("// call " + functionName + " " + nArgs + "\n");
        // push return-address
        out.write("@" + returnLabel + "\n");
        out.write("D=A\n");
        pushD();
        // push LCL
        out.write("@LCL\nD=M\n");
        pushD();
        // push ARG
        out.write("@ARG\nD=M\n");
        pushD();
        // push THIS
        out.write("@THIS\nD=M\n");
        pushD();
        // push THAT
        out.write("@THAT\nD=M\n");
        pushD();

        // ARG = SP - nArgs - 5
        out.write("@SP\n");
        out.write("D=M\n");
        out.write("@" + (nArgs + 5) + "\n");
        out.write("D=D-A\n");
        out.write("@ARG\n");
        out.write("M=D\n");

        // LCL = SP
        out.write("@SP\n");
        out.write("D=M\n");
        out.write("@LCL\n");
        out.write("M=D\n");

        // goto functionName
        out.write("@" + functionName + "\n");
        out.write("0;JMP\n");

        // (returnLabel)
        out.write("(" + returnLabel + ")\n");
    }

    public void writeReturn() throws IOException {
        out.write("// return\n");
        // FRAME = LCL (R13)
        out.write("@LCL\n");
        out.write("D=M\n");
        out.write("@R13\n");
        out.write("M=D\n");
        // RET = *(FRAME - 5) (R14)
        out.write("@5\n");
        out.write("A=D-A\n");
        out.write("D=M\n");
        out.write("@R14\n");
        out.write("M=D\n");
        // *ARG = pop()
        popToD();
        out.write("@ARG\n");
        out.write("A=M\n");
        out.write("M=D\n");
        // SP = ARG + 1
        out.write("@ARG\n");
        out.write("D=M+1\n");
        out.write("@SP\n");
        out.write("M=D\n");
        // THAT = *(FRAME - 1)
        out.write("@R13\n");
        out.write("AM=M-1\n"); // R13 = FRAME-1 ; A = R13
        out.write("D=M\n");
        out.write("@THAT\n");
        out.write("M=D\n");
        // THIS = *(FRAME - 2)
        out.write("@R13\n");
        out.write("AM=M-1\n");
        out.write("D=M\n");
        out.write("@THIS\n");
        out.write("M=D\n");
        // ARG = *(FRAME - 3)
        out.write("@R13\n");
        out.write("AM=M-1\n");
        out.write("D=M\n");
        out.write("@ARG\n");
        out.write("M=D\n");
        // LCL = *(FRAME - 4)
        out.write("@R13\n");
        out.write("AM=M-1\n");
        out.write("D=M\n");
        out.write("@LCL\n");
        out.write("M=D\n");
        // goto RET
        out.write("@R14\n");
        out.write("A=M\n");
        out.write("0;JMP\n");
    }

    public void close() throws IOException {
        out.close();
    }
}
