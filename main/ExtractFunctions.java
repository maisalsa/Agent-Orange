//@author
//@category Analysis
//@keybinding
//@menupath
//@toolbar
import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionIterator;
import java.util.*;

public class ExtractFunctions extends GhidraScript {
    @Override
    public void run() throws Exception {
        FunctionIterator funcs = currentProgram.getFunctionManager().getFunctions(true);
        List<String> names = new ArrayList<>();
        while (funcs.hasNext()) {
            Function f = funcs.next();
            String name = f.getName()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
            names.add(name);
        }
        // Output as standard JSON array
        println("[" + String.join(",", names.stream().map(n -> "\"" + n.replace("\"", "\\\"") + "\"").toArray(String[]::new)) + "]");
    }
} 