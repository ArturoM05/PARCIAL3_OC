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
        this.currentFileName = fileName;
    }

    // Escribe código de inicialización (bootstrap)
    public void writeInit() throws IOException {
        out.write("// bootstrap\n");
        out.write("@256\n");
        out.write("D=A\n");
        out.write("@SP\n");
        out.write("M=D\n");
        writeCall("Sys.init", 0);
    }

    public void writeArithmetic(String command) throws IOException {
        out.write("// " + command + "\n");
        if (command.equals("add")) writeBinary("M=M+D");
        else if (command.equals("sub")) writeBinary("M=M-D");
        else if (command.equals("and")) writeBinary("M=M&D");
        else if (command.equals("or")) writeBinary("M=M|D");
        else if (command.equals("eq")) writeComparison("JEQ");
        else if (command.equals("gt")) writeComparison("JGT");
        else if (command.equals("lt")) writeComparison("JLT");
        else if (command.equals("neg")) writeUnary("M=-M");
        else if (command.equals("not")) writeUnary("M=!M");
    }

    private void writeBinary(String op) throws IOException {
        out.write("@SP\nAM=M-1\nD=M\nA=A-1\n" + op + "\n");
    }

    private void writeUnary(String op) throws IOException {
        out.write("@SP\nA=M-1\n" + op + "\n");
    }

    private void writeComparison(String jump) throws IOException {
        String labelTrue = "TRUE$" + labelCounter;
        String labelEnd = "END$" + labelCounter;
        labelCounter++;

        out.write("@SP\nAM=M-1\nD=M\nA=A-1\nD=M-D\n");
        out.write("@" + labelTrue + "\nD;" + jump + "\n");
        out.write("@SP\nA=M-1\nM=0\n@" + labelEnd + "\n0;JMP\n");
        out.write("(" + labelTrue + ")\n@SP\nA=M-1\nM=-1\n(" + labelEnd + ")\n");
    }

    // push/pop
    public void writePushPop(CommandType type, String segment, int index) throws IOException {
        out.write("// " + (type == CommandType.C_PUSH ? "push " : "pop ") + segment + " " + index + "\n");

        if (type == CommandType.C_PUSH) {
            if (segment.equals("constant")) {
                out.write("@" + index + "\nD=A\n");
                pushD();
            } else if (segment.equals("local") || segment.equals("argument") ||
                       segment.equals("this") || segment.equals("that")) {
                String base = segmentToBase(segment);
                out.write("@" + base + "\nD=M\n@" + index + "\nA=D+A\nD=M\n");
                pushD();
            } else if (segment.equals("temp")) {
                out.write("@" + (5 + index) + "\nD=M\n");
                pushD();
            } else if (segment.equals("pointer")) {
                out.write("@" + (index == 0 ? "THIS" : "THAT") + "\nD=M\n");
                pushD();
            } else if (segment.equals("static")) {
                out.write("@" + currentFileName + "." + index + "\nD=M\n");
                pushD();
            }
        } else if (type == CommandType.C_POP) {
            if (segment.equals("local") || segment.equals("argument") ||
                segment.equals("this") || segment.equals("that")) {
                String base = segmentToBase(segment);
                out.write("@" + base + "\nD=M\n@" + index + "\nD=D+A\n@R13\nM=D\n");
                popToD();
                out.write("@R13\nA=M\nM=D\n");
            } else if (segment.equals("temp")) {
                popToD();
                out.write("@" + (5 + index) + "\nM=D\n");
            } else if (segment.equals("pointer")) {
                popToD();
                out.write("@" + (index == 0 ? "THIS" : "THAT") + "\nM=D\n");
            } else if (segment.equals("static")) {
                popToD();
                out.write("@" + currentFileName + "." + index + "\nM=D\n");
            }
        }
    }

    private String segmentToBase(String seg) {
        switch (seg) {
            case "local": return "LCL";
            case "argument": return "ARG";
            case "this": return "THIS";
            case "that": return "THAT";
        }
        return "";
    }

    private void pushD() throws IOException {
        out.write("@SP\nA=M\nM=D\n@SP\nM=M+1\n");
    }

    private void popToD() throws IOException {
        out.write("@SP\nAM=M-1\nD=M\n");
    }

    // Flow control
    public void writeLabel(String label) throws IOException {
        String full = functionScopedLabel(label);
        out.write("(" + full + ")\n");
    }

    public void writeGoto(String label) throws IOException {
        String full = functionScopedLabel(label);
        out.write("@" + full + "\n0;JMP\n");
    }

    public void writeIf(String label) throws IOException {
        String full = functionScopedLabel(label);
        popToD();
        out.write("@" + full + "\nD;JNE\n");
    }

    private String functionScopedLabel(String label) {
        return (currentFunction == null || currentFunction.isEmpty()) ? label : currentFunction + "$" + label;
    }

    // functions
    public void writeFunction(String functionName, int nVars) throws IOException {
        currentFunction = functionName;
        out.write("(" + functionName + ")\n");
        for (int i = 0; i < nVars; i++) {
            out.write("@0\nD=A\n");
            pushD();
        }
    }

    // call f nArgs
    public void writeCall(String functionName, int nArgs) throws IOException {
        String returnLabel = "RET$" + functionName + "$" + labelCounter++;
        out.write("// call " + functionName + " " + nArgs + "\n");

        // push return address
        out.write("@" + returnLabel + "\nD=A\n");
        pushD();

        // push LCL, ARG, THIS, THAT
        for (String seg : new String[]{"LCL", "ARG", "THIS", "THAT"}) {
            out.write("@" + seg + "\nD=M\n");
            pushD();
        }

        // ARG = SP - nArgs - 5
        out.write("@SP\nD=M\n@" + (nArgs + 5) + "\nD=D-A\n@ARG\nM=D\n");

        // LCL = SP
        out.write("@SP\nD=M\n@LCL\nM=D\n");

        // goto function
        out.write("@" + functionName + "\n0;JMP\n");

        // (return label)
        out.write("(" + returnLabel + ")\n");
    }

    // return
    public void writeReturn() throws IOException {
        out.write("// return\n");

        // FRAME = LCL
        out.write("@LCL\nD=M\n@R13\nM=D\n");

        // RET = *(FRAME - 5)
        out.write("@5\nA=D-A\nD=M\n@R14\nM=D\n");

        // *ARG = pop()
        popToD();
        out.write("@ARG\nA=M\nM=D\n");

        // SP = ARG + 1
        out.write("@ARG\nD=M+1\n@SP\nM=D\n");

        // THAT = *(FRAME - 1)
        out.write("@R13\nAM=M-1\nD=M\n@THAT\nM=D\n");

        // THIS = *(FRAME - 2)
        out.write("@R13\nAM=M-1\nD=M\n@THIS\nM=D\n");

        // ARG = *(FRAME - 3)
        out.write("@R13\nAM=M-1\nD=M\n@ARG\nM=D\n");

        // LCL = *(FRAME - 4)
        out.write("@R13\nAM=M-1\nD=M\n@LCL\nM=D\n");

        // goto RET
        out.write("@R14\nA=M\n0;JMP\n");
    }

    public void close() throws IOException {
        out.close();
    }
}
