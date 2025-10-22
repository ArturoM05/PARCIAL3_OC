import java.io.*;

public class Parser {
    private BufferedReader reader;
    private String currentCommand;
    private String nextCommand; // lookahead

    public Parser(String filePath) throws IOException {
        reader = new BufferedReader(new FileReader(filePath));
        currentCommand = null;
        nextCommand = readNextCommand();
    }

    // true si hay un comando disponible (lookahead)
    public boolean hasMoreCommands() {
        return nextCommand != null;
    }

    // avanza: currentCommand <- nextCommand, y lee el siguiente nextCommand
    public void advance() throws IOException {
        if (nextCommand == null) {
            currentCommand = null;
            return;
        }
        currentCommand = nextCommand;
        nextCommand = readNextCommand();
    }

    // busca y devuelve la siguiente línea útil (sin comentarios ni espacios), o null si EOF
    private String readNextCommand() throws IOException {
        String line = reader.readLine();
        while (line != null) {
            // eliminar comentarios en línea
            int idx = line.indexOf("//");
            if (idx != -1) {
                line = line.substring(0, idx);
            }
            line = line.trim();
            if (!line.isEmpty()) {
                return line;
            }
            line = reader.readLine();
        }
        return null;
    }

    public CommandType commandType() {
        if (currentCommand == null) return null;
        if (currentCommand.startsWith("push")) return CommandType.C_PUSH;
        if (currentCommand.startsWith("pop")) return CommandType.C_POP;
        if (currentCommand.startsWith("label")) return CommandType.C_LABEL;
        if (currentCommand.startsWith("goto")) return CommandType.C_GOTO;
        if (currentCommand.startsWith("if-goto")) return CommandType.C_IF;
        if (currentCommand.startsWith("function")) return CommandType.C_FUNCTION;
        if (currentCommand.startsWith("call")) return CommandType.C_CALL;
        if (currentCommand.startsWith("return")) return CommandType.C_RETURN;
        return CommandType.C_ARITHMETIC;
    }

    // devuelve el primer argumento: para C_ARITHMETIC -> la propia operación
    public String arg1() {
        CommandType type = commandType();
        if (type == CommandType.C_ARITHMETIC) return currentCommand; // e.g. "add"
        if (type == CommandType.C_RETURN) return null; // return no tiene args
        String[] parts = currentCommand.split("\\s+");
        if (parts.length >= 2) return parts[1];
        return null;
    }

    // devuelve el segundo argumento (sólo cuando aplica)
    public int arg2() {
        String[] parts = currentCommand.split("\\s+");
        if (parts.length >= 3) {
            return Integer.parseInt(parts[2]);
        }
        throw new IllegalStateException("arg2 solicitado pero comando no tiene un segundo argumento: " + currentCommand);
    }
}
