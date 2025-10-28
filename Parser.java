import java.io.*;
import java.util.*;

public class Parser {
    private List<String> commands = new ArrayList<String>();
    private int current = -1;
    private String currentCommand = null;

    public Parser(String path) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line;
        while ((line = br.readLine()) != null) {
            int idx = line.indexOf("//");
            if (idx != -1) line = line.substring(0, idx);
            line = line.trim();
            if (!line.isEmpty()) {
                commands.add(line);
            }
        }
        br.close();
    }

    public boolean hasMoreCommands() {
        return current + 1 < commands.size();
    }

    public void advance() {
        if (hasMoreCommands()) {
            current++;
            currentCommand = commands.get(current);
        } else {
            currentCommand = null;
        }
    }

    public CommandType commandType() {
        if (currentCommand == null) return null;
        String[] parts = currentCommand.split("\\s+");
        String cmd = parts[0];
        if (cmd.equals("push")) return CommandType.C_PUSH;
        if (cmd.equals("pop")) return CommandType.C_POP;
        if (cmd.equals("label")) return CommandType.C_LABEL;
        if (cmd.equals("goto")) return CommandType.C_GOTO;
        if (cmd.equals("if-goto")) return CommandType.C_IF;
        if (cmd.equals("function")) return CommandType.C_FUNCTION;
        if (cmd.equals("call")) return CommandType.C_CALL;
        if (cmd.equals("return")) return CommandType.C_RETURN;
        // otherwise arithmetic
        return CommandType.C_ARITHMETIC;
    }

    // For arithmetic commands returns the command itself (e.g., "add")
    // For other commands returns the first argument (segment or label or function name)
    public String arg1() {
        CommandType type = commandType();
        if (type == CommandType.C_ARITHMETIC) {
            return currentCommand.split("\\s+")[0];
        }
        String[] parts = currentCommand.split("\\s+");
        if (parts.length >= 2) return parts[1];
        return null;
    }

    public int arg2() {
        String[] parts = currentCommand.split("\\s+");
        if (parts.length >= 3) {
            try {
                return Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }
}
