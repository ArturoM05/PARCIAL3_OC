import java.io.*;
import java.util.*;

public class VMTranslator {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Uso: java VMTranslator <archivo.vm | carpeta>");
            System.exit(1);
        }

        File input = new File(args[0]);
        List<File> vmFiles = new ArrayList<File>();
        String outputPath;

        if (input.isDirectory()) {
            File[] files = input.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".vm");
                }
            });
            if (files != null) {
                Arrays.sort(files);
                for (File f : files) vmFiles.add(f);
            }
            outputPath = new File(input, input.getName() + ".asm").getPath();
        } else {
            if (!input.getName().endsWith(".vm")) {
                System.err.println("El archivo de entrada debe ser .vm o una carpeta que contenga .vm");
                return;
            }
            vmFiles.add(input);
            String outName = input.getName().replaceAll("\\.vm$", ".asm");
            outputPath = new File(input.getParent(), outName).getPath();
        }

        CodeWriter writer = new CodeWriter(outputPath);

        // Si hay más de un archivo (o es carpeta) escribimos bootstrap
        if (input.isDirectory()) {
            writer.writeInit();
        }

        for (File vm : vmFiles) {
            Parser parser = new Parser(vm.getPath());
            // set file name para variables static
            String base = vm.getName();
            if (base.endsWith(".vm")) base = base.substring(0, base.length() - 3);
            writer.setFileName(base);

            while (parser.hasMoreCommands()) {
                parser.advance();
                CommandType type = parser.commandType();
                if (type == null) continue;
                if (type == CommandType.C_ARITHMETIC) {
                    writer.writeArithmetic(parser.arg1());
                } else if (type == CommandType.C_PUSH || type == CommandType.C_POP) {
                    writer.writePushPop(type, parser.arg1(), parser.arg2());
                } else if (type == CommandType.C_LABEL) {
                    writer.writeLabel(parser.arg1());
                } else if (type == CommandType.C_GOTO) {
                    writer.writeGoto(parser.arg1());
                } else if (type == CommandType.C_IF) {
                    writer.writeIf(parser.arg1());
                } else if (type == CommandType.C_FUNCTION) {
                    writer.writeFunction(parser.arg1(), parser.arg2());
                } else if (type == CommandType.C_CALL) {
                    writer.writeCall(parser.arg1(), parser.arg2());
                } else if (type == CommandType.C_RETURN) {
                    writer.writeReturn();
                }
            }
        }

        writer.close();
        System.out.println("Generado: " + outputPath);
    }
}
