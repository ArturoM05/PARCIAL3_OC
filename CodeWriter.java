import java.io.*;

public class CodeWriter {
    private BufferedWriter out;
    private String fileName;
    private int labelCounter = 0;

    public CodeWriter(String outputFile) throws IOException {
        out = new BufferedWriter(new FileWriter(outputFile));
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void writeArithmetic(String command) throws IOException {
        switch (command) {
            case "add":
                writeBinaryOp("M=M+D");
                break;
            case "sub":
                writeBinaryOp("M=M-D");
                break;
            case "neg":
                writeUnaryOp("M=-M");
                break;
            case "eq":
                writeComparison("JEQ");
                break;
            case "gt":
                writeComparison("JGT");
                break;
            case "lt":
                writeComparison("JLT");
                break;
            case "and":
                writeBinaryOp("M=M&D");
                break;
            case "or":
                writeBinaryOp("M=M|D");
                break;
            case "not":
                writeUnaryOp("M=!M");
                break;
        }
    }

    private void writeBinaryOp(String operation) throws IOException {
        out.write("@SP\nAM=M-1\nD=M\nA=A-1\n" + operation + "\n");
    }

    private void writeUnaryOp(String operation) throws IOException {
        out.write("@SP\nA=M-1\n" + operation + "\n");
    }

    private void writeComparison(String jump) throws IOException {
        String labelTrue = "TRUE_" + labelCounter;
        String labelEnd = "END_" + labelCounter;
        labelCounter++;
        out.write(
            "@SP\nAM=M-1\nD=M\nA=A-1\nD=M-D\n" +
            "@" + labelTrue + "\nD;" + jump + "\n" +
            "@SP\nA=M-1\nM=0\n" +
            "@" + labelEnd + "\n0;JMP\n" +
            "(" + labelTrue + ")\n@SP\nA=M-1\nM=-1\n" +
            "(" + labelEnd + ")\n"
        );
    }

    public void writePushPop(CommandType type, String segment, int index) throws IOException {
        if (type == CommandType.C_PUSH) {
            if (segment.equals("constant")) {
                out.write("@" + index + "\nD=A\n");
            } else if (segment.equals("local")) {
                writeSegmentPush("LCL", index);
            } else if (segment.equals("argument")) {
                writeSegmentPush("ARG", index);
            } else if (segment.equals("this")) {
                writeSegmentPush("THIS", index);
            } else if (segment.equals("that")) {
                writeSegmentPush("THAT", index);
            } else if (segment.equals("temp")) {
                out.write("@" + (5 + index) + "\nD=M\n");
            } else if (segment.equals("pointer")) {
                out.write("@" + (index == 0 ? "THIS" : "THAT") + "\nD=M\n");
            } else if (segment.equals("static")) {
                out.write("@" + fileName + "." + index + "\nD=M\n");
            }
            out.write("@SP\nA=M\nM=D\n@SP\nM=M+1\n");
        } else if (type == CommandType.C_POP) {
            if (segment.equals("local") || segment.equals("argument") ||
                segment.equals("this") || segment.equals("that")) {
                writeSegmentPop(segment, index);
            } else if (segment.equals("temp")) {
                writeDirectPop(5 + index);
            } else if (segment.equals("pointer")) {
                out.write("@SP\nAM=M-1\nD=M\n@" + (index == 0 ? "THIS" : "THAT") + "\nM=D\n");
            } else if (segment.equals("static")) {
                out.write("@SP\nAM=M-1\nD=M\n@" + fileName + "." + index + "\nM=D\n");
            }
        }
    }

    private void writeSegmentPush(String base, int index) throws IOException {
        out.write("@" + base + "\nD=M\n@" + index + "\nA=D+A\nD=M\n");
    }

    private void writeSegmentPop(String segment, int index) throws IOException {
        String base = "";
        if (segment.equals("local")) base = "LCL";
        else if (segment.equals("argument")) base = "ARG";
        else if (segment.equals("this")) base = "THIS";
        else if (segment.equals("that")) base = "THAT";

        out.write("@" + base + "\nD=M\n@" + index + "\nD=D+A\n@R13\nM=D\n");
        out.write("@SP\nAM=M-1\nD=M\n@R13\nA=M\nM=D\n");
    }

    private void writeDirectPop(int address) throws IOException {
        out.write("@SP\nAM=M-1\nD=M\n@" + address + "\nM=D\n");
    }

    public void writeLabel(String label) throws IOException {
        out.write("(" + label + ")\n");
    }

    public void writeGoto(String label) throws IOException {
        out.write("@" + label + "\n0;JMP\n");
    }

    public void writeIf(String label) throws IOException {
        out.write("@SP\nAM=M-1\nD=M\n@" + label + "\nD;JNE\n");
    }

    public void writeCall(String functionName, int nArgs) throws IOException {
        String returnLabel = "RET_" + (labelCounter++);

        // push return-address
        out.write("@" + returnLabel + "\nD=A\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
        // push LCL
        out.write("@LCL\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
        // push ARG
        out.write("@ARG\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
        // push THIS
        out.write("@THIS\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");
        // push THAT
        out.write("@THAT\nD=M\n@SP\nA=M\nM=D\n@SP\nM=M+1\n");

        // ARG = SP - 5 - nArgs
        out.write("@SP\nD=M\n@5\nD=D-A\n@" + nArgs + "\nD=D-A\n@ARG\nM=D\n");
        // LCL = SP
        out.write("@SP\nD=M\n@LCL\nM=D\n");
        // goto functionName
        out.write("@" + functionName + "\n0;JMP\n");
        // (return-address)
        out.write("(" + returnLabel + ")\n");
    }

    public void writeFunction(String functionName, int nVars) throws IOException {
        out.write("(" + functionName + ")\n");
        for (int i = 0; i < nVars; i++) {
            out.write("@SP\nA=M\nM=0\n@SP\nM=M+1\n");
        }
    }

        public void writeReturn() throws IOException {
            // FRAME = LCL (guarda el frame en R13)
            out.write("@LCL\nD=M\n@R13\nM=D\n");
        
            // RET = *(FRAME-5) (guarda la dirección de retorno en R14)
            out.write("@5\nA=D-A\nD=M\n@R14\nM=D\n");
        
            // *ARG = pop() (coloca el valor de retorno para el llamador)
            out.write("@SP\nAM=M-1\nD=M\n@ARG\nA=M\nM=D\n");
        
            // SP = ARG+1 (reposiciona SP)
            out.write("@ARG\nD=M+1\n@SP\nM=D\n");
        
            // THAT = *(FRAME-1) (restaura THAT del llamador)
            out.write("@R13\nAM=M-1\nD=M\n@THAT\nM=D\n");
        
            // THIS = *(FRAME-2) (restaura THIS del llamador)
            out.write("@R13\nAM=M-1\nD=M\n@THIS\nM=D\n");
        
            // ARG = *(FRAME-3) (restaura ARG del llamador)
            out.write("@R13\nAM=M-1\nD=M\n@ARG\nM=D\n");
        
            // LCL = *(FRAME-4) (restaura LCL del llamador)
            out.write("@R13\nAM=M-1\nD=M\n@LCL\nM=D\n");
        
            // goto RET (salta a la dirección de retorno)
            out.write("@R14\nA=M\n0;JMP\n");
        }

    public void close() throws IOException {
        out.close();
    }
}
