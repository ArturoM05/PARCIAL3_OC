import java.io.*;
import java.util.*;

/*
 * Como usarlo:
 * Compilar los archiivos .java: javac *.java
 * Ejecutar el traductor: java VMTranslator <archivo.vm o carpeta>
 * Ejemplo: java VMTranslator example.vm
 */

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

        // Si es un directorio, escribimos el bootstrap (SP=256 y call Sys.init 0)
        if (input.isDirectory()) {
            codeWriter.writeInit();
            // Procesar todos los .vm dentro del directorio
            File[] vmFiles = input.listFiles((d, name) -> name.endsWith(".vm"));
            if (vmFiles != null) {
                // es conveniente procesar Sys.vm primero (si existe), aunque no es estrictamente necesario
                Arrays.sort(vmFiles, Comparator.comparing(File::getName));
                for (File file : vmFiles) {
                    processFile(file, codeWriter);
                }
            }
        } else {
            // archivo individual
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
            } else if (type == CommandType.C_LABEL) {
                codeWriter.writeLabel(parser.arg1());
            } else if (type == CommandType.C_GOTO) {
                codeWriter.writeGoto(parser.arg1());
            } else if (type == CommandType.C_IF) {
                codeWriter.writeIf(parser.arg1());
            } else if (type == CommandType.C_FUNCTION) {
                codeWriter.writeFunction(parser.arg1(), parser.arg2());
            } else if (type == CommandType.C_CALL) {
                codeWriter.writeCall(parser.arg1(), parser.arg2());
            } else if (type == CommandType.C_RETURN) {
                codeWriter.writeReturn();
            }
        }
    }
}
