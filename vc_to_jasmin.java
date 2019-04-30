package VC;

import VC.ASTs.AST;
import VC.Checker.Checker;
import VC.CodeGen.Emitter;
import VC.Parser.Parser;
import VC.Scanner.Scanner;
import VC.Scanner.SourceFile;
import VC.TreeDrawer.Drawer;

public class vc_to_jasmin {

    private static Scanner scanner;
    private static ErrorReporter reporter;
    private static Parser parser;
    private static Drawer drawer;
    private static Printer printer;
    private static UnParser unparser;
    private static Checker checker;
    private static Emitter emitter;

    private static int drawingAST = 0;
    private static boolean printingAST = false;
    private static boolean unparsingAST = false;
    private static String inputFilename;
    private static String astFilename = "";
    private static String unparsingFilename = "";

    private static AST theAST;

    private static void cmdLineOptions() {
        System.out.println("\nUsage: java VC.vc_to_jasmin [-options] filename");
        System.out.println();
        System.out.println("where options include:");
        System.out.println("	-d [1234]           display the AST (without SourcePosition)");
        System.out.println("	                    1:  the AST from the parser (without SourcePosition)");
        System.out.println("	                    2:  the AST from the parser (with SourcePosition)");
        System.out.println("	                    3:  the AST from the checker (without SourcePosition)");
        System.out.println("	                    4:  the AST from the checker (with SourcePosition)");
        System.out.println("	-t [file]           print the (non-annotated) AST into <file>");
        System.out.println("	                    (or filename + \"t\" if <file> is unspecified)");
        System.out.println("	-u [file]  	    unparse the (non-annotated) AST into <file>");
        System.out.println("	                    (or filename + \"u\" if <file> is unspecified)");
        System.exit(1);
    }

    public static void main(String[] args) {
        int i = 0;
        String arg;

        System.out.println("======= The VC compiler =======\n");

        while (i < args.length && args[i].startsWith("-")) {
            arg = args[i++];

            if (arg.startsWith("-d") && !arg.equals("-d")) {
                int n = 0;
                try {
                    n = Integer.parseInt(arg.substring(2));
                } catch (NumberFormatException e) {
                    System.out.println("[# vc_to_jasmin #]: invalid option " + arg);
                    cmdLineOptions();
                }
                if (1 <= n && n <= 4)
                    drawingAST = n;
                else {
                    System.out.println("[# vc_to_jasmin #]: invalid option " + arg);
                    cmdLineOptions();
                }
            } else if (arg.equals("-d")) {
                if (i < args.length) {
                    if (args[i].equals("1")) {
                        drawingAST = 1;
                        i++;
                    } else if (args[i].equals("2")) {
                        drawingAST = 2;
                        i++;
                    } else if (args[i].equals("3")) {
                        drawingAST = 3;
                        i++;
                    } else if (args[i].equals("4")) {
                        drawingAST = 4;
                        i++;
                    } else {
                        System.out.println("[# vc_to_jasmin #]: invalid option -d " + args[i]);
                        cmdLineOptions();
                    }
                }
            } else if (arg.startsWith("-t")) {
                printingAST = true;
                if (!arg.equals("-t"))
                    astFilename = arg.substring(2);
                else if (i < args.length && !args[i].startsWith("-"))
                    astFilename = args[i++];
            } else if (arg.startsWith("-u")) {
                unparsingAST = true;
                if (!arg.equals("-u"))
                    astFilename = arg.substring(2);
                if (i < args.length && !args[i].startsWith("-"))
                    unparsingFilename = args[i++];
            } else {
                System.out.println("[# vc_to_jasmin #]: invalid option " + arg);
                cmdLineOptions();
            }
        }
        if (i == args.length) {
            System.out.println("[# vc_to_jasmin #]: no input file");
            cmdLineOptions();
        } else
            inputFilename = args[i];

        SourceFile source = new SourceFile(inputFilename);

        reporter = new ErrorReporter();

        System.out.println("Pass 1: Lexical and syntactic Analysis");
        scanner = new Scanner(source, reporter);
        parser = new Parser(scanner, reporter);

        theAST = parser.parseProgram();

        if (reporter.numErrors == 0) {
            if (unparsingAST) {
                if (unparsingFilename.equals(""))
                    unparsingFilename = inputFilename + "u";
                unparser = new UnParser(unparsingFilename);
                unparser.unparse(theAST);
                System.out.println("[# vc_to_jasmin #]: The unparsed VC program printed to " + unparsingFilename);
            }
            if (printingAST) {
                if (astFilename.equals(""))
                    astFilename = inputFilename + "p";
                printer = new Printer(astFilename);
                printer.print(theAST);
                System.out.println("[# vc_to_jasmin #]: The linearised AST printed to " + astFilename);
            }
            if (1 <= drawingAST && drawingAST <= 2) {
                drawer = new Drawer();
                if (drawingAST == 2)
                    drawer.enableDebugging();
                drawer.draw(theAST);
            }

            System.out.println("Pass 2: Semantic Analysis");
            checker = new Checker(reporter);
            checker.check(theAST);

            if (reporter.numErrors == 0) {
                System.out.println("Pass 3: Code Generation");
                System.out.println();
                emitter = new Emitter(inputFilename, reporter);
                emitter.gen(theAST);
                if (reporter.numErrors == 0)
                    System.out.println("Compilation was successful.");
                else
                    System.out.println("Compilation was unsuccessful.");
            } else
                System.out.println("Compilation was unsuccessful.");

            if (drawingAST >= 3) {
                drawer = new Drawer();
                if (drawingAST == 4)
                    drawer.enableDebugging();
                drawer.draw(theAST);
            }
        } else
            System.out.println("Compilation was unsuccessful.");
    }
}

