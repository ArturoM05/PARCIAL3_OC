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
        return CommandType.C_ARITHMETIC;
    }

    public String arg1() {
        if (commandType() == CommandType.C_ARITHMETIC) return currentCommand;
        return currentCommand.split(" ")[1];
    }

    public int arg2() {
        return Integer.parseInt(currentCommand.split(" ")[2]);
    }
}
