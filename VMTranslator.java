import java.io.*;
import java.util.*;

public class VMTranslator {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Uso: java VMTranslator <archivo.vm o carpeta>");
            return;
        }

        File input = new File(args[0]);
        File outputFile;

        if (input.isDirectory()) {
            String dirName = input.getName();
            outputFile = new File(input, dirName + ".asm");
        } else {
            outputFile = new File(input.getParent(), input.getName().replace(".vm", ".asm"));
        }

        CodeWriter codeWriter = new CodeWriter(outputFile.getPath());

        if (input.isDirectory()) {
            for (File file : Objects.requireNonNull(input.listFiles((d, name) -> name.endsWith(".vm")))) {
                processFile(file, codeWriter);
            }
        } else {
            processFile(input, codeWriter);
        }

        codeWriter.close();
        System.out.println("Traducción completada: " + outputFile.getPath());
    }

    private static void processFile(File file, CodeWriter codeWriter) throws IOException {
        Parser parser = new Parser(file.getPath());
        codeWriter.setFileName(file.getName().replace(".vm", ""));
        while (parser.hasMoreCommands()) {
            parser.advance();
            if (parser.commandType() == null) continue;
            CommandType type = parser.commandType();
            if (type == CommandType.C_ARITHMETIC) {
                codeWriter.writeArithmetic(parser.arg1());
            } else if (type == CommandType.C_PUSH || type == CommandType.C_POP) {
                codeWriter.writePushPop(type, parser.arg1(), parser.arg2());
            }
        }
    }
}
