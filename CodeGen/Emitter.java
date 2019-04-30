package VC.CodeGen;

import VC.ASTs.*;
import VC.ErrorReporter;

public final class Emitter implements Visitor {
    private ErrorReporter errorReporter;
    private String inputFilename;
    private String classname;
    private Type funcReturnType;

    public Emitter(String inputFilename, ErrorReporter reporter) {
        this.inputFilename = inputFilename;
        errorReporter = reporter;
        int i = inputFilename.lastIndexOf('.');
        if (i > 0)
            classname = inputFilename.substring(0, i);
        else
            classname = inputFilename;
        funcReturnType = null;
    }

    public final void gen(AST ast) {
        ast.visit(this, null);
        JVM.dump(classname + ".j");
    }

    public Object visitProgram(Program ast, Object o) {
        emit(JVM.CLASS, "public", classname);
        emit(JVM.SUPER, "java/lang/Object");
        emit("");
        for (List list = ast.FL; !list.isEmpty(); list = ((DeclList) list).DL) {
            DeclList dlAST = (DeclList) list;
            if (dlAST.D instanceof GlobalVarDecl) {
                GlobalVarDecl vAST = (GlobalVarDecl) dlAST.D;
                emit(JVM.STATIC_FIELD, vAST.I.spelling, VCtoJavaType(vAST.T));
            }
        }
        emit("");
        emit("; standard class static initializer ");
        emit(JVM.METHOD_START, "static <clinit>()V");
        emit("");
        Frame frame = new Frame(false);
        for (List list = ast.FL; !list.isEmpty(); list = ((DeclList) list).DL) {
            DeclList dlAST = (DeclList) list;
            if (dlAST.D instanceof GlobalVarDecl) {
                GlobalVarDecl vAST = (GlobalVarDecl) dlAST.D;
                if (vAST.T.isArrayType()) {
                    ((ArrayType) vAST.T).E.visit(this, frame);
                    emit(JVM.NEWARRAY, ((ArrayType) vAST.T).T.toString());
                }
                if (!vAST.E.isEmptyExpr()) vAST.E.visit(this, frame);
                else if (!vAST.T.isArrayType()) {
                    if (vAST.T.isFloatType()) emit(JVM.FCONST_0);
                    else if (vAST.T.isIntType() || vAST.T.isBooleanType()) emit(JVM.ICONST_0);
                    else System.out.println("[!] Unhandled global var decl type: " + vAST.T.toString());
                    frame.push();
                }
                emit(JVM.PUTSTATIC, classname + "/" + vAST.I.spelling, VCtoJavaType(vAST.T));
                frame.pop();
            }
        }
        emit("");
        emit("; set limits used by this method");
        emit(JVM.LIMIT, "locals", frame.getNewIndex());
        emit(JVM.LIMIT, "stack", frame.getMaximumStackSize());
        emit(JVM.RETURN);
        emit(JVM.METHOD_END, "method");
        emit("");
        emit("; standard constructor initializer ");
        emit(JVM.METHOD_START, "public <init>()V");
        emit(JVM.LIMIT, "stack 1");
        emit(JVM.LIMIT, "locals 1");
        emit(JVM.ALOAD_0);
        emit(JVM.INVOKESPECIAL, "java/lang/Object/<init>()V");
        emit(JVM.RETURN);
        emit(JVM.METHOD_END, "method");
        return ast.FL.visit(this, o);
    }

    public Object visitStmtList(StmtList ast, Object o) {
        ast.S.visit(this, o);
        ast.SL.visit(this, o);
        return null;
    }

    public Object visitCompoundStmt(CompoundStmt ast, Object o) {
        Frame frame = (Frame) o;
        String scopeStart = frame.getNewLabel();
        String scopeEnd = frame.getNewLabel();
        frame.scopeStart.push(scopeStart);
        frame.scopeEnd.push(scopeEnd);
        emit(scopeStart + ":");
        if (ast.parent instanceof FuncDecl) {
            if (((FuncDecl) ast.parent).I.spelling.equals("main")) {
                emit(JVM.VAR, "0 is argv [Ljava/lang/String; from " + frame.scopeStart.peek() + " to " + frame.scopeEnd.peek());
                emit(JVM.VAR, "1 is vc_to_jasmin$ L" + classname + "; from " + frame.scopeStart.peek() + " to " + frame.scopeEnd.peek());
                emit(JVM.NEW, classname);
                emit(JVM.DUP);
                frame.push(2);
                emit("invokenonvirtual", classname + "/<init>()V");
                frame.pop();
                emit(JVM.ASTORE_1);
                frame.pop();
            } else {
                emit(JVM.VAR, "0 is this L" + classname + "; from " + frame.scopeStart.peek() + " to " + frame.scopeEnd.peek());
                ((FuncDecl) ast.parent).PL.visit(this, o);
            }
        }
        ast.DL.visit(this, o);
        ast.SL.visit(this, o);
        emit(scopeEnd + ":");
        frame.scopeStart.pop();
        frame.scopeEnd.pop();
        return null;
    }

    public Object visitReturnStmt(ReturnStmt ast, Object o) {
        Frame frame = (Frame) o;
        ast.E.visit(this, o);
        if (frame.isMain()) {
            emit(JVM.RETURN);
            return null;
        }
        if (funcReturnType.isIntType() || funcReturnType.isBooleanType()) {
            emit(JVM.IRETURN);
            frame.pop();
        } else if (funcReturnType.isFloatType()) {
            emit(JVM.FRETURN);
            frame.pop();
        } else if (funcReturnType.isVoidType()) emit(JVM.RETURN);
        else System.out.println("[!] Unhandled return type");
        return null;
    }

    public Object visitEmptyStmtList(EmptyStmtList ast, Object o) {
        return null;
    }

    public Object visitEmptyCompStmt(EmptyCompStmt ast, Object o) {
        Frame frame = (Frame) o;
        String scopeStart = frame.getNewLabel();
        String scopeEnd = frame.getNewLabel();
        frame.scopeStart.push(scopeStart);
        frame.scopeEnd.push(scopeEnd);
        emit(scopeStart + ":");
        if (ast.parent instanceof FuncDecl) {
            if (((FuncDecl) ast.parent).I.spelling.equals("main")) {
                emit(JVM.VAR, "0 is argv [Ljava/lang/String; from " + frame.scopeStart.peek() + " to " + frame.scopeEnd.peek());
                emit(JVM.VAR, "1 is vc_to_jasmin$ L" + classname + "; from " + frame.scopeStart.peek() + " to " + frame.scopeEnd.peek());
                emit(JVM.NEW, classname);
                emit(JVM.DUP);
                frame.push(2);
                emit("invokenonvirtual", classname + "/<init>()V");
                frame.pop();
                emit(JVM.ASTORE_1);
                frame.pop();
            } else {
                emit(JVM.VAR, "0 is this L" + classname + "; from " + frame.scopeStart.peek() + " to " + frame.scopeEnd.peek());
                ((FuncDecl) ast.parent).PL.visit(this, o);
            }
        }
        emit(scopeEnd + ":");
        frame.scopeStart.pop();
        frame.scopeEnd.pop();
        return null;
    }

    public Object visitEmptyStmt(EmptyStmt ast, Object o) {
        return null;
    }

    public Object visitCallExpr(CallExpr ast, Object o) {
        Frame frame = (Frame) o;
        String fname = ast.I.spelling;
        switch (fname) {
            case "getInt":
                ast.AL.visit(this, o);
                emit("invokestatic VC/lang/System.getInt()I");
                frame.push();
                break;
            case "putInt":
                ast.AL.visit(this, o);
                emit("invokestatic VC/lang/System.putInt(I)V");
                frame.pop();
                break;
            case "putIntLn":
                ast.AL.visit(this, o);
                emit("invokestatic VC/lang/System/putIntLn(I)V");
                frame.pop();
                break;
            case "getFloat":
                ast.AL.visit(this, o);
                emit("invokestatic VC/lang/System/getFloat()F");
                frame.push();
                break;
            case "putFloat":
                ast.AL.visit(this, o);
                emit("invokestatic VC/lang/System/putFloat(F)V");
                frame.pop();
                break;
            case "putFloatLn":
                ast.AL.visit(this, o);
                emit("invokestatic VC/lang/System/putFloatLn(F)V");
                frame.pop();
                break;
            case "putBool":
                ast.AL.visit(this, o);
                emit("invokestatic VC/lang/System/putBool(Z)V");
                frame.pop();
                break;
            case "putBoolLn":
                ast.AL.visit(this, o);
                emit("invokestatic VC/lang/System/putBoolLn(Z)V");
                frame.pop();
                break;
            case "putString":
                ast.AL.visit(this, o);
                emit(JVM.INVOKESTATIC, "VC/lang/System/putString(Ljava/lang/String;)V");
                frame.pop();
                break;
            case "putStringLn":
                ast.AL.visit(this, o);
                emit(JVM.INVOKESTATIC, "VC/lang/System/putStringLn(Ljava/lang/String;)V");
                frame.pop();
                break;
            case "putLn":
                ast.AL.visit(this, o);
                emit("invokestatic VC/lang/System/putLn()V");
                break;
            default:
                FuncDecl fAST = (FuncDecl) ast.I.decl;
                if (frame.isMain()) emit("aload_1");
                else emit("aload_0");
                frame.push();
                ast.AL.visit(this, o);
                String retType = VCtoJavaType(fAST.T);
                int argc = 0;
                StringBuilder argsTypes = new StringBuilder();
                for (List fpl = fAST.PL; !fpl.isEmpty(); fpl = ((ParaList) fpl).PL, argc++)
                    argsTypes.append(VCtoJavaType(((ParaList) fpl).P.T));
                emit("invokevirtual", classname + "/" + fname + "(" + argsTypes + ")" + retType);
                frame.pop(argc + 1);
                if (!fAST.T.isVoidType()) frame.push();
                break;
        }

        return null;
    }

    public Object visitExprStmt(ExprStmt ast, Object o) {
        Frame frame = (Frame) o;
        ast.E.visit(this, o);
        if (frame.getCurStackSize() != 0) {
            emit(JVM.POP);
            frame.pop();
        }
        if (frame.getCurStackSize() != 0) System.out.println("[!] ExprStmt has a stack size greater than 1 left");
        return null;
    }

    public Object visitContinueStmt(ContinueStmt ast, Object o) {
        Frame frame = (Frame) o;
        emit(JVM.GOTO, frame.conStack.peek());
        return null;
    }

    public Object visitBreakStmt(BreakStmt ast, Object o) {
        Frame frame = (Frame) o;
        emit(JVM.GOTO, frame.brkStack.peek());
        return null;
    }

    public Object visitForStmt(ForStmt ast, Object o) {
        Frame frame = (Frame) o;
        String conLabel = frame.getNewLabel();
        String brkLabel = frame.getNewLabel();
        frame.conStack.push(conLabel);
        frame.brkStack.push(brkLabel);
        String firstTimeLabel = frame.getNewLabel();
        ast.E1.visit(this, o);
        if (frame.getCurStackSize() != 0) {
            emit(JVM.POP);
            frame.pop();
        }
        if (frame.getCurStackSize() != 0) System.out.println("[!] ForExpr1 has a stack size greater than 1 left");
        emit(JVM.GOTO, firstTimeLabel);
        emit(conLabel + ":");
        ast.E3.visit(this, o);
        if (frame.getCurStackSize() != 0) {
            emit(JVM.POP);
            frame.pop();
        }
        if (frame.getCurStackSize() != 0) System.out.println("[!] ForExpr3 has a stack size greater than 1 left");
        emit(firstTimeLabel + ":");
        ast.E2.visit(this, o);
        if (!ast.E2.isEmptyExpr()) {
            emit(JVM.IFEQ, brkLabel);
            frame.pop();
        }
        ast.S.visit(this, o);
        emit(JVM.GOTO, conLabel);
        emit(brkLabel + ":");
        frame.conStack.pop();
        frame.brkStack.pop();
        return null;
    }

    public Object visitWhileStmt(WhileStmt ast, Object o) {
        Frame frame = (Frame) o;
        String conLabel = frame.getNewLabel();
        String brkLabel = frame.getNewLabel();
        frame.conStack.push(conLabel);
        frame.brkStack.push(brkLabel);
        emit(conLabel + ":");
        ast.E.visit(this, o);
        emit(JVM.IFEQ, brkLabel);
        frame.pop();
        ast.S.visit(this, o);
        emit(JVM.GOTO, conLabel);
        emit(brkLabel + ":");
        frame.conStack.pop();
        frame.brkStack.pop();
        return null;
    }

    public Object visitIfStmt(IfStmt ast, Object o) {
        Frame frame = (Frame) o;
        String l1 = frame.getNewLabel();
        String l2 = frame.getNewLabel();
        ast.E.visit(this, o);
        emit(JVM.IFEQ, l1);
        frame.pop();
        ast.S1.visit(this, o);
        emit(JVM.GOTO, l2);
        emit(l1 + ":");
        ast.S2.visit(this, o);
        emit(l2 + ":");
        return null;
    }

    public Object visitEmptyExpr(EmptyExpr ast, Object o) {
        return null;
    }

    public Object visitIntExpr(IntExpr ast, Object o) {
        ast.IL.visit(this, o);
        return null;
    }

    public Object visitFloatExpr(FloatExpr ast, Object o) {
        ast.FL.visit(this, o);
        return null;
    }

    public Object visitBooleanExpr(BooleanExpr ast, Object o) {
        ast.BL.visit(this, o);
        return null;
    }

    public Object visitStringExpr(StringExpr ast, Object o) {
        ast.SL.visit(this, o);
        return null;
    }

    public Object visitAssignExpr(AssignExpr ast, Object o) {
        Frame frame = (Frame) o;

        if (ast.E1 instanceof ArrayExpr) {
            ((ArrayExpr) ast.E1).V.visit(this, frame);
            ((ArrayExpr) ast.E1).E.visit(this, frame);
            ast.E2.visit(this, o);
            if (ast.parent != null) {
                emit(JVM.DUP_X2);
                frame.push();
            }
            if (ast.type.isIntType()) emit(JVM.IASTORE);
            else if (ast.type.isFloatType()) emit(JVM.FASTORE);
            else if (ast.type.isBooleanType()) emit(JVM.BASTORE);
            else System.out.println("[!] Unhandled Assignment LHS ArrayExpr type");
            frame.pop(3);
        } else if (ast.E1 instanceof VarExpr) {
            ast.E2.visit(this, o);
            if (ast.parent != null) {
                emit(JVM.DUP);
                frame.push();
            }
            visitStoreVarExpr((VarExpr) ast.E1, frame);
        } else System.out.println("[!] Unhandled Assignment LHS type");
        return null;
    }

    private void visitStoreVarExpr(VarExpr ast, Frame frame) {
        visitStoreSimpleVar((SimpleVar) ast.V, frame);
    }

    private void visitStoreSimpleVar(SimpleVar ast, Frame frame) {
        Decl varDecl = (Decl) ast.I.decl;
        if (varDecl instanceof GlobalVarDecl) {
            emit(JVM.PUTSTATIC, classname + "/" + varDecl.I.spelling, VCtoJavaType(varDecl.T));
            frame.pop();
        } else if (varDecl instanceof LocalVarDecl || varDecl instanceof ParaDecl) {
            if (varDecl.T.isFloatType()) {
                int index;
                if (varDecl.I.decl instanceof ParaDecl) index = ((ParaDecl) varDecl.I.decl).index;
                else index = ((LocalVarDecl) varDecl.I.decl).index;
                if (index >= 0 && index <= 3) emit(JVM.FSTORE + "_" + index);
                else emit(JVM.FSTORE, index);
                frame.pop();
            } else if (varDecl.T.isIntType() || varDecl.T.isBooleanType()) {
                int index;
                if (varDecl.I.decl instanceof ParaDecl) index = ((ParaDecl) varDecl.I.decl).index;
                else index = ((LocalVarDecl) varDecl.I.decl).index;
                if (index >= 0 && index <= 3) emit(JVM.ISTORE + "_" + index);
                else emit(JVM.ISTORE, index);
                frame.pop();
            } else System.out.println("[!] Unhandled Assignment LHS local var/param var decl type");
        } else System.out.println("[!] Unhandled Assignment LHS SimpleVar decl type");
    }

    public Object visitVarExpr(VarExpr ast, Object o) {
        ast.V.visit(this, o);
        return null;
    }

    public Object visitArrayExpr(ArrayExpr ast, Object o) {
        Frame frame = (Frame) o;
        ast.V.visit(this, frame);
        ast.E.visit(this, frame);
        if (ast.type.isIntType()) emit(JVM.IALOAD);
        else if (ast.type.isFloatType()) emit(JVM.FALOAD);
        else if (ast.type.isBooleanType()) emit(JVM.BALOAD);
        else System.out.println("[!] Unhandled ArrayExpr type");
        frame.pop(2);
        frame.push();
        return null;
    }

    public Object visitSimpleVar(SimpleVar ast, Object o) {
        Frame frame = (Frame) o;
        Decl varDecl = (Decl) ast.I.decl;
        if (varDecl instanceof GlobalVarDecl)
            emit(JVM.GETSTATIC, classname + "/" + varDecl.I.spelling, VCtoJavaType(varDecl.T));
        else if (varDecl instanceof LocalVarDecl || varDecl instanceof ParaDecl) {
            if (varDecl.T.isFloatType()) {
                if (varDecl.index >= 0 && varDecl.index <= 3) emit(JVM.FLOAD + "_" + varDecl.index);
                else emit(JVM.FLOAD, varDecl.index);
            } else if (varDecl.T.isIntType() || varDecl.T.isBooleanType()) {
                if (varDecl.index >= 0 && varDecl.index <= 3) emit(JVM.ILOAD + "_" + varDecl.index);
                else emit(JVM.ILOAD, varDecl.index);
            } else if (varDecl.T.isArrayType()) {
                if (varDecl.index >= 0 && varDecl.index <= 3) emit(JVM.ALOAD + "_" + varDecl.index);
                else emit(JVM.ALOAD, varDecl.index);
            } else System.out.println("[!] Unhandled SimpleVar decl type");
        } else System.out.println("[!] Unhandled SimpleVar decl type");
        frame.push();
        return null;
    }

    public Object visitInitExpr(InitExpr ast, Object o) {
        Frame frame = (Frame) o;
        int arrIndex = 0;
        for (List el = ast.IL; !el.isEmpty(); el = ((ExprList) el).EL, arrIndex++) {
            emit(JVM.DUP);
            frame.push();
            if (arrIndex == -1) emit(JVM.ICONST_M1);
            else if (arrIndex >= 0 && arrIndex <= 5) emit(JVM.ICONST + "_" + arrIndex);
            else if (arrIndex >= -128 && arrIndex <= 127) emit(JVM.BIPUSH, arrIndex);
            else if (arrIndex >= -32768 && arrIndex <= 32767) emit(JVM.SIPUSH, arrIndex);
            else emit(JVM.LDC, arrIndex);
            frame.push();
            Expr e = ((ExprList) el).E;
            e.visit(this, o);
            if (e.type.isFloatType()) emit(JVM.FASTORE);
            else if (e.type.isIntType()) emit(JVM.IASTORE);
            else if (e.type.isBooleanType()) emit(JVM.BASTORE);
            else System.out.println("[!] Unhandled InitExpr member type (index): " + arrIndex);
            frame.pop(3);
        }
        return null;
    }

    public Object visitExprList(ExprList ast, Object o) {
        return null;
    }

    public Object visitEmptyExprList(EmptyExprList ast, Object o) {
        return null;
    }

    public Object visitBinaryExpr(BinaryExpr ast, Object o) {
        String opcode, falseLabel, nextLabel;
        Frame frame = (Frame) o;
        String op = ast.O.spelling;
        if (op.equals("i&&") || op.equals("i||")) return visitShortCircuitExpr(ast, o);
        ast.E1.visit(this, o);
        ast.E2.visit(this, o);

        switch (op) {
            case "i!=":
            case "i==":
            case "i<":
            case "i<=":
            case "i>":
            case "i>=":
                opcode = "";
                switch (op) {
                    case "i!=":
                        opcode = JVM.IF_ICMPNE;
                        break;
                    case "i==":
                        opcode = JVM.IF_ICMPEQ;
                        break;
                    case "i<":
                        opcode = JVM.IF_ICMPLT;
                        break;
                    case "i<=":
                        opcode = JVM.IF_ICMPLE;
                        break;
                    case "i>":
                        opcode = JVM.IF_ICMPGT;
                        break;
                    case "i>=":
                        opcode = JVM.IF_ICMPGE;
                        break;
                    default:
                        System.out.println("[!] Unhandled integer comparison operator: " + op);
                        break;
                }
                falseLabel = frame.getNewLabel();
                nextLabel = frame.getNewLabel();
                emit(opcode, falseLabel);
                frame.pop(2);
                emit(JVM.ICONST_0);
                emit(JVM.GOTO, nextLabel);
                emit(falseLabel + ":");
                emit(JVM.ICONST_1);
                frame.push();
                emit(nextLabel + ":");
                break;
            case "f!=":
            case "f==":
            case "f<":
            case "f<=":
            case "f>":
            case "f>=":
                opcode = "";
                switch (op) {
                    case "f!=":
                        opcode = JVM.IFNE;
                        break;
                    case "f==":
                        opcode = JVM.IFEQ;
                        break;
                    case "f<":
                        opcode = JVM.IFLT;
                        break;
                    case "f<=":
                        opcode = JVM.IFLE;
                        break;
                    case "f>":
                        opcode = JVM.IFGT;
                        break;
                    case "f>=":
                        opcode = JVM.IFGE;
                        break;
                    default:
                        System.out.println("[!] Unhandled float comparison operator: " + op);
                        break;
                }
                falseLabel = frame.getNewLabel();
                nextLabel = frame.getNewLabel();
                emit(JVM.FCMPG);
                frame.pop(2);
                emit(opcode, falseLabel);
                emit(JVM.ICONST_0);
                emit(JVM.GOTO, nextLabel);
                emit(falseLabel + ":");
                emit(JVM.ICONST_1);
                frame.push();
                emit(nextLabel + ":");
                break;
            case "i+":
            case "i-":
            case "i*":
            case "i/":
                opcode = "";
                switch (op) {
                    case "i+":
                        opcode = JVM.IADD;
                        break;
                    case "i-":
                        opcode = JVM.ISUB;
                        break;
                    case "i*":
                        opcode = JVM.IMUL;
                        break;
                    case "i/":
                        opcode = JVM.IDIV;
                        break;
                    default:
                        System.out.println("[!] Unhandled integer arirthmetic operator: " + op);
                        break;
                }
                emit(opcode);
                frame.pop(2);
                frame.push();
                break;
            case "f+":
            case "f-":
            case "f*":
            case "f/":
                opcode = "";
                switch (op) {
                    case "f+":
                        opcode = JVM.FADD;
                        break;
                    case "f-":
                        opcode = JVM.FSUB;
                        break;
                    case "f*":
                        opcode = JVM.FMUL;
                        break;
                    case "f/":
                        opcode = JVM.FDIV;
                        break;
                    default:
                        System.out.println("[!] Unhandled float arirthmetic operator: " + op);
                        break;
                }
                emit(opcode);
                frame.pop(2);
                frame.push();
                break;
            default:
                System.out.println("[!] Unhandled binary operator: " + op);
                break;
        }
        return null;
    }

    public Object visitShortCircuitExpr(BinaryExpr ast, Object o) {
        Frame frame = (Frame) o;
        String op = ast.O.spelling;
        String l1 = frame.getNewLabel();
        String l2 = frame.getNewLabel();
        switch (op) {
            case "i&&":
                ast.E1.visit(this, o);
                emit(JVM.IFEQ, l1);
                frame.pop();
                ast.E2.visit(this, o);
                emit(JVM.IFEQ, l1);
                frame.pop();
                emit(JVM.ICONST_1);
                emit(JVM.GOTO, l2);
                emit(l1 + ":");
                emit(JVM.ICONST_0);
                frame.push();
                emit(l2 + ":");
                break;
            case "i||":
                ast.E1.visit(this, o);
                emit(JVM.IFNE, l1);
                frame.pop();
                ast.E2.visit(this, o);
                emit(JVM.IFNE, l1);
                frame.pop();
                emit(JVM.ICONST_0);
                emit(JVM.GOTO, l2);
                emit(l1 + ":");
                emit(JVM.ICONST_1);
                frame.push();
                emit(l2 + ":");
                break;
            default:
                System.out.println("[!] Unhandled short circuit operator: " + op);
                break;
        }
        return null;
    }

    public Object visitUnaryExpr(UnaryExpr ast, Object o) {
        ast.E.visit(this, o);
        String op = ast.O.spelling;
        switch (op) {
            case "i+":
            case "f+":
                emit("");
                break;
            case "i-":
                emit(JVM.INEG);
                break;
            case "f-":
                emit(JVM.FNEG);
                break;
            case "i2f":
                emit(JVM.I2F);
                break;
            case "i!":
                Frame frame = (Frame) o;
                String falseLabel = frame.getNewLabel();
                String nextLabel = frame.getNewLabel();
                emit(JVM.IFEQ, falseLabel);
                emit(JVM.ICONST_0);
                emit(JVM.GOTO, nextLabel);
                emit(falseLabel + ":");
                emit(JVM.ICONST_1);
                emit(nextLabel + ":");
                break;
            default:
                System.out.println("[!] Unhandled unary operator " + op);
                break;
        }
        return null;
    }

    public Object visitDeclList(DeclList ast, Object o) {
        ast.D.visit(this, o);
        ast.DL.visit(this, o);
        return null;
    }

    public Object visitEmptyDeclList(EmptyDeclList ast, Object o) {
        return null;
    }

    public Object visitFuncDecl(FuncDecl ast, Object o) {
        Frame frame;
        funcReturnType = ast.T;
        if (ast.I.spelling.equals("main")) {
            frame = new Frame(true);
            frame.getNewIndex();
            emit(JVM.METHOD_START, "public static main([Ljava/lang/String;)V");
            frame.getNewIndex();
        } else {
            frame = new Frame(false);
            frame.getNewIndex();
            String retType = VCtoJavaType(ast.T);
            StringBuilder argsTypes = new StringBuilder();
            List fpl = ast.PL;
            while (!fpl.isEmpty()) {
                argsTypes.append(VCtoJavaType(((ParaList) fpl).P.T));
                fpl = ((ParaList) fpl).PL;
            }
            emit(JVM.METHOD_START, ast.I.spelling + "(" + argsTypes + ")" + retType);
        }
        ast.S.visit(this, frame);
        if (ast.T.isVoidType()) emit(JVM.RETURN);
        else if (ast.I.spelling.equals("main")) emit(JVM.RETURN);
        else emit(JVM.NOP);
        emit("");
        emit("; set limits used by this method");
        emit(JVM.LIMIT, "locals", frame.getNewIndex());
        emit(JVM.LIMIT, "stack", frame.getMaximumStackSize());
        emit(".end method");
        return null;
    }

    public Object visitGlobalVarDecl(GlobalVarDecl ast, Object o) {
        return null;
    }

    public Object visitLocalVarDecl(LocalVarDecl ast, Object o) {
        Frame frame = (Frame) o;
        ast.index = frame.getNewIndex();
        String T = VCtoJavaType(ast.T);
        emit(JVM.VAR + " " + ast.index + " is " + ast.I.spelling + " " + T + " from " + frame.scopeStart.peek() + " to " + frame.scopeEnd.peek());
        if (ast.T.isArrayType()) {
            ((ArrayType) ast.T).E.visit(this, o);
            emit(JVM.NEWARRAY, ((ArrayType) ast.T).T.toString());
        }
        ast.I.decl = ast;
        if (!ast.E.isEmptyExpr()) {
            ast.E.visit(this, o);
            if (ast.T.isFloatType()) {
                int index;
                if (ast.I.decl instanceof ParaDecl) index = ((ParaDecl) ast.I.decl).index;
                else index = ((LocalVarDecl) ast.I.decl).index;
                if (index >= 0 && index <= 3) emit(JVM.FSTORE + "_" + index);
                else emit(JVM.FSTORE, index);
            } else if (ast.T.isIntType() || ast.T.isBooleanType()) {
                int index;
                if (ast.I.decl instanceof ParaDecl) index = ((ParaDecl) ast.I.decl).index;
                else index = ((LocalVarDecl) ast.I.decl).index;
                if (index >= 0 && index <= 3) emit(JVM.ISTORE + "_" + index);
                else emit(JVM.ISTORE, index);
            } else if (ast.T.isArrayType()) {
                int index;
                if (ast.I.decl instanceof ParaDecl) index = ((ParaDecl) ast.I.decl).index;
                else index = ((LocalVarDecl) ast.I.decl).index;
                if (index >= 0 && index <= 3) emit(JVM.ASTORE + "_" + index);
                else emit(JVM.ASTORE, index);
            } else System.out.println("[!] Unhandled LocalVarDecl rhs expr type: " + ast.T.toString());
            frame.pop();
        } else if (ast.T.isArrayType()) {
            int index;
            if (ast.I.decl instanceof ParaDecl) index = ((ParaDecl) ast.I.decl).index;
            else index = ((LocalVarDecl) ast.I.decl).index;
            if (index >= 0 && index <= 3) emit(JVM.ASTORE + "_" + index);
            else emit(JVM.ASTORE, index);
            frame.pop();
        }
        return null;
    }

    public Object visitParaList(ParaList ast, Object o) {
        ast.P.visit(this, o);
        ast.PL.visit(this, o);
        return null;
    }

    public Object visitParaDecl(ParaDecl ast, Object o) {
        Frame frame = (Frame) o;
        ast.index = frame.getNewIndex();
        String T = VCtoJavaType(ast.T);
        emit(JVM.VAR + " " + ast.index + " is " + ast.I.spelling + " " + T + " from " + frame.scopeStart.peek() + " to " + frame.scopeEnd.peek());
        return null;
    }

    public Object visitEmptyParaList(EmptyParaList ast, Object o) {
        return null;
    }

    public Object visitArgList(ArgList ast, Object o) {
        ast.A.visit(this, o);
        ast.AL.visit(this, o);
        return null;
    }

    public Object visitArg(Arg ast, Object o) {
        ast.E.visit(this, o);
        return null;
    }

    public Object visitEmptyArgList(EmptyArgList ast, Object o) {
        return null;
    }

    public Object visitIntType(IntType ast, Object o) {
        return null;
    }

    public Object visitFloatType(FloatType ast, Object o) {
        return null;
    }

    public Object visitBooleanType(BooleanType ast, Object o) {
        return null;
    }

    public Object visitVoidType(VoidType ast, Object o) {
        return null;
    }

    public Object visitErrorType(ErrorType ast, Object o) {
        return null;
    }

    public Object visitStringType(StringType ast, Object o) {
        return null;
    }

    public Object visitArrayType(ArrayType ast, Object o) {
        return null;
    }

    public Object visitIdent(Ident ast, Object o) {
        return null;
    }

    public Object visitIntLiteral(IntLiteral ast, Object o) {
        Frame frame = (Frame) o;
        if (Integer.parseInt(ast.spelling) == -1) emit(JVM.ICONST_M1);
        else if (Integer.parseInt(ast.spelling) >= 0 && Integer.parseInt(ast.spelling) <= 5)
            emit(JVM.ICONST + "_" + Integer.parseInt(ast.spelling));
        else if (Integer.parseInt(ast.spelling) >= -128 && Integer.parseInt(ast.spelling) <= 127)
            emit(JVM.BIPUSH, Integer.parseInt(ast.spelling));
        else if (Integer.parseInt(ast.spelling) >= -32768 && Integer.parseInt(ast.spelling) <= 32767)
            emit(JVM.SIPUSH, Integer.parseInt(ast.spelling));
        else emit(JVM.LDC, Integer.parseInt(ast.spelling));
        frame.push();
        return null;
    }

    public Object visitFloatLiteral(FloatLiteral ast, Object o) {
        Frame frame = (Frame) o;
        if (Float.parseFloat(ast.spelling) == 0.0) emit(JVM.FCONST_0);
        else if (Float.parseFloat(ast.spelling) == 1.0) emit(JVM.FCONST_1);
        else if (Float.parseFloat(ast.spelling) == 2.0) emit(JVM.FCONST_2);
        else emit(JVM.LDC, Float.parseFloat(ast.spelling));
        frame.push();
        return null;
    }

    public Object visitBooleanLiteral(BooleanLiteral ast, Object o) {
        Frame frame = (Frame) o;
        if (ast.spelling.equals("true")) emit(JVM.ICONST_1);
        else emit(JVM.ICONST_0);
        frame.push();
        return null;
    }

    public Object visitStringLiteral(StringLiteral ast, Object o) {
        Frame frame = (Frame) o;
        emit(JVM.LDC, "\"" + ast.spelling + "\"");
        frame.push();
        return null;
    }

    public Object visitOperator(Operator ast, Object o) {
        return null;
    }

    private void emit(String s) {
        JVM.append(new Instruction(s));
    }

    private void emit(String s1, String s2) {
        emit(s1 + " " + s2);
    }

    private void emit(String s1, int i) {
        emit(s1 + " " + i);
    }

    private void emit(String s1, float f) {
        emit(s1 + " " + f);
    }

    private void emit(String s1, String s2, int i) {
        emit(s1 + " " + s2 + " " + i);
    }

    private void emit(String s1, String s2, String s3) {
        emit(s1 + " " + s2 + " " + s3);
    }

    private String VCtoJavaType(Type t) {
        String type = "";
        if (t.isArrayType()) {
            type += "[";
            t = ((ArrayType) t).T;
        }
        if (t.isBooleanType()) type += "Z";
        else if (t.isIntType()) type += "I";
        else if (t.isFloatType()) type += "F";
        else if (t.isVoidType()) type += "V";
        else System.out.println("[!] Unhandled VC Type in VCtoJavaType: " + t.toString());
        return type;
    }
}