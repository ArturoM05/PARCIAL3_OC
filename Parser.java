import java.io.*;


public class Parser {
    private BufferedReader reader;
    private String currentCommand;

    public Parser(String filePath) throws IOException {
        reader = new BufferedReader(new FileReader(filePath));
        currentCommand = null;
    }

    public boolean hasMoreCommands() throws IOException {
        return reader.ready();
    }

    public void advance() throws IOException {
        currentCommand = reader.readLine();
        while (currentCommand != null && (currentCommand.trim().isEmpty() || currentCommand.startsWith("//"))) {
            currentCommand = reader.readLine();
        }
        if (currentCommand != null) {
            int commentIndex = currentCommand.indexOf("//");
            if (commentIndex != -1) {
                currentCommand = currentCommand.substring(0, commentIndex);
            }
            currentCommand = currentCommand.trim();
        }
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

    public String arg1() {
        CommandType type = commandType();
        if (type == CommandType.C_ARITHMETIC) return currentCommand;
        if (type == CommandType.C_RETURN) return null; // return no tiene argumentos
        return currentCommand.split(" ")[1];
    }

    public int arg2() {
        return Integer.parseInt(currentCommand.split(" ")[2]);
    }
}
