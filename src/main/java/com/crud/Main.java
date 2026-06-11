package com.crud;

import com.crud.ui.ConsoleApp;

public class Main {

    private static final String DATA_FILE = "data/items.json";

    public static void main(String[] args) {
        String filePath = args.length > 0 ? args[0] : DATA_FILE;
        new ConsoleApp(filePath).run();
    }
}
