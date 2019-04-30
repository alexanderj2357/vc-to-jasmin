package VC.CodeGen;

import java.util.Stack;

public class Frame {
    private final boolean _main;
    public Stack<String> conStack;
    public Stack<String> brkStack;
    public Stack<String> scopeStart;
    public Stack<String> scopeEnd;
    private int label;
    private int localVarIndex;
    private int currentStackSize;
    private int maximumStackSize;

    public Frame(boolean _main) {
        this._main = _main;
        label = 0;
        localVarIndex = 0;
        currentStackSize = 0;
        maximumStackSize = 0;
        conStack = new Stack<>();
        brkStack = new Stack<>();
        scopeStart = new Stack<>();
        scopeEnd = new Stack<>();
    }

    public boolean isMain() {
        return _main;
    }

    public int getNewIndex() {
        if (localVarIndex >= JVM.MAX_LOCALVARINDEX) {
            System.out.println("The maximum local variable index (" + JVM.MAX_LOCALVARINDEX + ") reached.");
            System.exit(1);
        }
        return localVarIndex++;
    }

    public String getNewLabel() {
        return "L" + label++;
    }

    public void push() {
        push(1);
    }

    public void push(int i) {
        currentStackSize += i;
        if (currentStackSize < 0 || currentStackSize > JVM.MAX_OPSTACK) {
            System.out.println("Invalid operand stack size.");
            System.out.println("Current operand stack size is " + currentStackSize + ".");
            System.out.println("You wanted to push " + i + ((i == 1) ? " operand" : " operands") + " to the stack.");
            System.out.println("The size of the operand stack is limited to the range 0 .. " + JVM.MAX_OPSTACK + ".");
            System.out.println("Good luck with debugging your code generator.");
            System.exit(1);
        }

        if (currentStackSize > maximumStackSize)
            maximumStackSize = currentStackSize;
    }

    public void pop() {
        pop(1);
    }

    public void pop(int i) {
        currentStackSize -= i;

        if (currentStackSize < 0) {
            System.out.println("Invalid operand stack size.");
            System.out.println("Current operand stack size is " + currentStackSize + ".");
            System.out.println("You wanted to pop " + i + ((i == 1) ? " operand" : " operands") + " to the stack.");
            System.out.println("The size of the operand stack is limited to the range 0 .. " + JVM.MAX_OPSTACK + ".");
            System.out.println("Good luck with debugging your code generator.");
            System.exit(1);
        }
    }

    public int getMaximumStackSize() {
        return maximumStackSize;
    }

    public int getCurStackSize() {
        return currentStackSize;
    }
}
