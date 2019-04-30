package VC.CodeGen;

import java.io.PrintWriter;

public class Instruction {
    public String inst;

    public Instruction(String inst) {
        this.inst = inst;
    }

    public void write(PrintWriter writer) {
        if (!(inst.startsWith(".") || inst.endsWith(":")))
            writer.print("\t");
        writer.println(inst);
    }
}  
