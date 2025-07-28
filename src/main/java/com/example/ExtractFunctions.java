//@author Agent-Orange
//@category Analysis
//@keybinding
//@menupath
//@toolbar

/**
 * ExtractFunctions - Ghidra Analysis Script
 * 
 * This script extracts all function names from the current program and outputs them
 * as a valid JSON array. The output is designed for downstream consumption by
 * Java applications or other tools that need program structure information.
 * 
 * Features:
 * - Extracts all functions (including imported/external functions)
 * - Outputs valid, well-escaped JSON
 * - Handles special characters in function names
 * - Provides consistent output format
 * 
 * Sample Output:
 * [
 *   "main",
 *   "printf",
 *   "malloc",
 *   "free",
 *   "strcpy",
 *   "function_with_underscores",
 *   "function_with\"quotes\"",
 *   "function_with\\backslashes"
 * ]
 * 
 * Usage:
 * 1. Open a binary in Ghidra
 * 2. Run this script (Scripts -> Analysis -> ExtractFunctions)
 * 3. Copy the JSON output from the console
 * 4. Use in downstream applications
 * 
 * Note: This script is designed to work with the Agent-Orange pentesting chatbot
 * and outputs JSON that can be directly consumed by Java applications.
 */

import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionIterator;
import java.util.*;

public class ExtractFunctions extends GhidraScript {
    
    /**
     * Main execution method called by Ghidra.
     * Extracts all function names and outputs them as a JSON array.
     */
    @Override
    public void run() throws Exception {
        // Get all functions from the current program
        FunctionIterator funcs = currentProgram.getFunctionManager().getFunctions(true);
        List<String> names = new ArrayList<>();
        
        // Extract and escape function names
        while (funcs.hasNext()) {
            Function f = funcs.next();
            String name = f.getName();
            
            // Skip null or empty function names
            if (name == null || name.trim().isEmpty()) {
                continue;
            }
            
            // Escape the function name for JSON output
            String escapedName = escapeForJson(name);
            names.add(escapedName);
        }
        
        // Output as valid JSON array
        String jsonOutput = buildJsonArray(names);
        println(jsonOutput);
        
        // Log summary for debugging
        println("// Extracted " + names.size() + " functions from " + currentProgram.getName());
    }
    
    /**
     * Escapes a string for safe JSON output.
     * Handles all special characters that could break JSON formatting.
     * 
     * @param input The string to escape
     * @return The escaped string safe for JSON
     */
    private String escapeForJson(String input) {
        if (input == null) {
            return "";
        }
        
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    // Handle other control characters (ASCII 0-31)
                    if (c < 32) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                    break;
            }
        }
        return escaped.toString();
    }
    
    /**
     * Builds a properly formatted JSON array from a list of strings.
     * 
     * @param items The list of strings to include in the JSON array
     * @return A valid JSON array string
     */
    private String buildJsonArray(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "[]";
        }
        
        StringBuilder json = new StringBuilder();
        json.append("[");
        
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append("\"").append(items.get(i)).append("\"");
        }
        
        json.append("]");
        return json.toString();
    }
} 