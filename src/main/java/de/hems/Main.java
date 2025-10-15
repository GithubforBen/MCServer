package de.hems;

import org.eclipse.aether.util.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    private static Main instance;

    public Main() throws IOException {
        if (true) return;
        if (instance == null) {
            instance = this;
        } else {
            throw new IllegalStateException("Already initialized");
        }
    }

    public static void main(String[] args) throws Exception {
        new File("test.txt").createNewFile();
        new Main();
        new ListenerAdapter();
    }
}