package com.slinkydeveloper.sdp.client;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Console {

    private final PrintStream outStream;

    public Console() {
        this.outStream = System.out;
    }

    public Console header(String str) {
        outStream.println(ConsoleColor.GREEN_BOLD.colorize(str));
        return this;
    }

    public Console error(String str) {
        outStream.println(ConsoleColor.RED.colorize(str));
        return this;
    }

    public Console print(String str) {
        outStream.println(str);
        return this;
    }

    public Console newLine() {
        outStream.println();
        return this;
    }

    @SafeVarargs
    public final Console listChoices(Map.Entry<String, Runnable>... choices) {
        List<Map.Entry<String, Runnable>> choicesList = Arrays.asList(choices);
        print("0) Quit the program");
        for (int i = 0; i < choicesList.size(); i++) {
            print((i + 1) + ") " + choicesList.get(i).getKey());
        }
        Integer choice = numericInput("Insert your choice");
        if (choice == null || choice == 0) {
            return this;
        }

        Runnable r;
        try {
            r = choicesList.get(choice - 1).getValue();
        } catch (IndexOutOfBoundsException e) {
            error("Invalid choice. Accepted choicesList: 1 - " + choicesList.size());
            return this;
        }

        newLine();
        r.run();
        return this;
    }

    public Integer numericInput(String str) {
        outStream.print(str + ": ");
        return readNumber();
    }

    private Integer readNumber() {
        String input = System.console().readLine();
        if (input.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(input.trim().replace("\n", "").replace("\r", ""));
        } catch (NumberFormatException e) {
            error("Error while parsing the choice");
            return null;
        }
    }
}
