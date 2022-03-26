package util;

import java.util.ArrayList;
import java.util.HashMap;

public class ArgParser {

    private final static char[] flagArgs = {'v', 'i', 'h', 'g', 's', 'f'}; //Flag without an argument
    private final static char[] specArgs = {'a', 'p'}; //Flag with an argument

    public ArrayList<Character> presentFlags;
    public HashMap<Character, String> presentArgs;
    public String inputFilePath;

    public ArgParser(String[] args) {
        this.presentFlags = new ArrayList<>();
        this.presentArgs = new HashMap<>();
        this.parseArgs(args);
    }

    private void parseArgs(String[] args) {
        int curArgIndex = 0;
        Character curSpecArg = null;
        while (args.length != curArgIndex) {
            String curArg = args[curArgIndex];
            if (curSpecArg != null) {
                presentArgs.put(curSpecArg, curArg);
                curSpecArg = null;
            } else if (curArg.length() == 2 && curArg.charAt(0) == '-') {
                char curChar = curArg.charAt(1);
                if (isFlagArg(curChar)) {
                    this.presentFlags.add(curChar);
                } else if (isSpecArg(curChar)) {
                    curSpecArg = curChar;
                } else {
                    throw new IllegalArgumentException("Unknown argument: -" + curChar);
                }
            } else {
                if (inputFilePath == null) {
                    inputFilePath = curArg;
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + curArg + ", input file path was already defined");
                }
            }
            curArgIndex++;
        }
    }

    private static boolean isFlagArg(char c) {
        for (char flagArg : flagArgs) {
            if (flagArg == c) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSpecArg(char c) {
        for (char specArg : specArgs) {
            if (specArg == c) {
                return true;
            }
        }
        return false;
    }

}
