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

    public void close() throws IOException {
        out.close();
    }
}
