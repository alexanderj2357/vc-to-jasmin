package VC.Scanner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.LineNumberReader;

public class SourceFile {

    static final char eof = '\u0000';
    private LineNumberReader reader;

    public SourceFile(String filename) {
        try {
            reader = new LineNumberReader(new BufferedReader(new FileReader(filename)));
        } catch (java.io.FileNotFoundException e) {
            System.out.println("[# vc_to_jasmin #]: can't read: " + filename);
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Caught IOException: " + e.getMessage());
            System.exit(1);
        }
    }

    char getNextChar() {
        try {
            int c = reader.read();
            if (c == -1) c = eof;
            return (char) c;
        } catch (java.io.IOException e) {
            System.out.println("Caught IOException: " + e.getMessage());
            return eof;
        }
    }

    char inspectChar(int nthChar) {
        int c;

        try {
            reader.mark(nthChar);
            do {
                c = reader.read();
                nthChar--;
            } while (nthChar != 0);
            reader.reset();
            if (c == -1) c = eof;
            return (char) c;
        } catch (java.io.IOException e) {
            System.out.println("Caught IOException: " + e.getMessage());
            return eof;
        }
    }

}
