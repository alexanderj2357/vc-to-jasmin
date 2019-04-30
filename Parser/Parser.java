package VC.Parser;

import VC.ASTs.*;
import VC.ErrorReporter;
import VC.Scanner.Scanner;
import VC.Scanner.SourcePosition;
import VC.Scanner.Token;

import java.util.Arrays;

import static VC.Scanner.Token.*;

public class Parser {
    private Scanner scanner;
    private ErrorReporter errorReporter;
    private Token currentToken;
    private SourcePosition prevTokenPosition;
    private SourcePosition dummyPosition = new SourcePosition();

    public Parser(Scanner scanner, ErrorReporter errorReporter) {
        this.scanner = scanner;
        this.errorReporter = errorReporter;
        prevTokenPosition = new SourcePosition();
        currentToken = this.scanner.getToken();
    }

    private void match(int tokenExpected) throws SyntaxError {
        if (currentToken.kind == tokenExpected) {
            prevTokenPosition = currentToken.position;
            currentToken = scanner.getToken();
        } else syntaxError("\"%\" expected here", Token.spell(tokenExpected));
    }

    private void accept() {
        prevTokenPosition = currentToken.position;
        currentToken = scanner.getToken();
    }

    private void syntaxError(String messageTemplate, String tokenQuoted) throws SyntaxError {
        errorReporter.reportError(messageTemplate, tokenQuoted, currentToken.position);
        throw (new SyntaxError());
    }

    private void start(SourcePosition position) {
        position.lineStart = currentToken.position.lineStart;
        position.charStart = currentToken.position.charStart;
    }

    private void finish(SourcePosition position) {
        position.lineFinish = prevTokenPosition.lineFinish;
        position.charFinish = prevTokenPosition.charFinish;
    }

    public Program parseProgram() {
        Program abstractSyntaxTree;
        SourcePosition programPosition = new SourcePosition();
        start(programPosition);
        try {
            finish(programPosition);
            List dlAST = parseDeclList();
            abstractSyntaxTree = new Program(dlAST, programPosition);
            if (currentToken.kind != Token.EOF) syntaxError("\"%\" unknown type", currentToken.spelling);
        } catch (SyntaxError ignore) {
            return null;
        }
        return abstractSyntaxTree;
    }

    private List parseDeclList() throws SyntaxError {
        DeclList declList;
        DeclList mostChildishList;
        if (currentToken.kind == Token.EOF) return new EmptyDeclList(dummyPosition);
        declList = parseDecl();
        mostChildishList = declList;
        while (!(mostChildishList.DL instanceof EmptyDeclList)) mostChildishList = ((DeclList) mostChildishList.DL);
        if (currentToken.kind != Token.EOF) mostChildishList.DL = parseDeclList();
        return declList;
    }

    private DeclList parseDecl() throws SyntaxError {
        SourcePosition declPosition = new SourcePosition();
        start(declPosition);
        Type t = parseType();
        Ident ident = null;
        if (currentToken.kind == Token.ID) {
            prevTokenPosition = currentToken.position;
            String identSpelling = currentToken.spelling;
            ident = new Ident(identSpelling, prevTokenPosition);
            currentToken = scanner.getToken();
        } else syntaxError("identifier expected here", "");
        DeclList declList;
        if (currentToken.kind == Token.LPAREN) {
            List paraList = parseParamList();
            Stmt cmpStmt = parseCompoundStmt();
            Decl fDecl = new FuncDecl(t, ident, paraList, cmpStmt, declPosition);
            finish(declPosition);
            declList = new DeclList(fDecl, new EmptyDeclList(dummyPosition), declPosition);
        } else {
            GlobalVarDecl var;
            Type varType = t;
            Expr varExp = new EmptyExpr(dummyPosition);
            SourcePosition varPos = new SourcePosition();
            start(varPos);
            if (currentToken.kind == Token.LBRACKET) {
                Expr arrayIntExp = new EmptyExpr(dummyPosition);
                match(Token.LBRACKET);
                if (currentToken.kind == Token.INTLITERAL) {
                    String spelling = currentToken.spelling;
                    accept();
                    IntLiteral intLit = new IntLiteral(spelling, prevTokenPosition);
                    arrayIntExp = new IntExpr(intLit, declPosition);
                }
                match(Token.RBRACKET);
                finish(declPosition);
                varType = new ArrayType(t, arrayIntExp, declPosition);
            }
            if (currentToken.kind == Token.EQ) {
                acceptOperator();
                varExp = parseInitialiser();
            }
            finish(varPos);
            var = new GlobalVarDecl(varType, ident, varExp, varPos);
            List commaDeclList = new EmptyDeclList(dummyPosition);
            if (currentToken.kind == Token.COMMA) {
                match(Token.COMMA);
                commaDeclList = parseInitDeclaratorList(t, "global");
            }
            match(Token.SEMICOLON);
            declList = new DeclList(var, commaDeclList, varPos);
        }
        return declList;
    }

    private List parseVarDeclList() throws SyntaxError {
        SourcePosition declListPosition = new SourcePosition();
        DeclList varDeclList;
        DeclList childDeclList;
        if (!Arrays.asList(VOID, BOOLEAN, INT, FLOAT).contains(currentToken.kind))
            return new EmptyDeclList(dummyPosition);
        start(declListPosition);
        Type varType;
        List declaratorList;
        varType = parseType();
        declaratorList = parseInitDeclaratorList(varType, "local");
        match(Token.SEMICOLON);
        varDeclList = (DeclList) declaratorList;
        childDeclList = varDeclList;
        while (!(childDeclList.DL instanceof EmptyDeclList)) childDeclList = ((DeclList) childDeclList.DL);
        if (Arrays.asList(VOID, BOOLEAN, INT, FLOAT).contains(currentToken.kind)) {
            childDeclList.DL = parseVarDeclList();
            finish(declListPosition);
            varDeclList.position = declListPosition;
        }
        return varDeclList;
    }

    private List parseInitDeclaratorList(Type varType, String declType) throws SyntaxError {
        SourcePosition declListPosition = new SourcePosition();
        DeclList declList;
        Decl var;
        start(declListPosition);
        Expr varExp = new EmptyExpr(dummyPosition);
        var = parseDeclarator(varType, declType);
        if (currentToken.kind == Token.EQ) {
            acceptOperator();
            varExp = parseInitialiser();
        }
        if (var instanceof GlobalVarDecl) ((GlobalVarDecl) var).E = varExp;
        else if (var instanceof LocalVarDecl) ((LocalVarDecl) var).E = varExp;
        else throw new RuntimeException("expected local or global variable");
        if (currentToken.kind != Token.COMMA) {
            finish(declListPosition);
            declList = new DeclList(var, new EmptyDeclList(dummyPosition), declListPosition);
        } else {
            match(Token.COMMA);
            declList = new DeclList(var, parseInitDeclaratorList(varType, declType), dummyPosition);
            finish(declListPosition);
            declList.position = declListPosition;
        }
        return declList;
    }

    private Decl parseDeclarator(Type varType, String declType) throws SyntaxError {
        SourcePosition declPosition = varType.position;
        Decl var;
        Expr arrayIntExp = new EmptyExpr(dummyPosition);
        Ident ident = null;
        if (currentToken.kind == Token.ID) {
            prevTokenPosition = currentToken.position;
            String spelling = currentToken.spelling;
            ident = new Ident(spelling, prevTokenPosition);
            currentToken = scanner.getToken();
        } else syntaxError("identifier expected here", "");
        if (currentToken.kind == Token.LBRACKET) {
            match(Token.LBRACKET);
            if (currentToken.kind == Token.INTLITERAL) {
                String intLiteralSpelling = currentToken.spelling;
                accept();
                IntLiteral intLiteral = new IntLiteral(intLiteralSpelling, prevTokenPosition);
                arrayIntExp = new IntExpr(intLiteral, prevTokenPosition);
            }
            match(Token.RBRACKET);
            finish(declPosition);
            varType = new ArrayType(varType, arrayIntExp, declPosition);
        }
        finish(declPosition);
        if (declType.equals("global"))
            var = new GlobalVarDecl(varType, ident, new EmptyExpr(dummyPosition), declPosition);
        else if (declType.equals("parameter")) var = new ParaDecl(varType, ident, declPosition);
        else var = new LocalVarDecl(varType, ident, new EmptyExpr(dummyPosition), declPosition);
        return var;
    }

    private Expr parseInitialiser() throws SyntaxError {
        SourcePosition initPosition = new SourcePosition();
        List expList;
        Expr initialiser;
        start(initPosition);
        if (currentToken.kind == Token.LCURLY) {
            match(Token.LCURLY);
            expList = parseInitialiserList();
            match(Token.RCURLY);
            finish(initPosition);
            initialiser = new InitExpr(expList, initPosition);
        } else initialiser = parseAssignExpr();
        return initialiser;
    }

    private List parseInitialiserList() throws SyntaxError {
        SourcePosition initListPosition = new SourcePosition();
        List initList;
        Expr initExpr;
        start(initListPosition);
        initExpr = parseAssignExpr();
        if (currentToken.kind == Token.COMMA) {
            match(Token.COMMA);
            initList = new ExprList(initExpr, parseInitialiserList(), dummyPosition);
            finish(initListPosition);
            initList.position = initListPosition;
        } else {
            finish(initListPosition);
            initList = new ExprList(initExpr, new EmptyExprList(dummyPosition), initListPosition);
        }
        return initList;
    }

    private Type parseType() throws SyntaxError {
        Type typeAST = null;
        SourcePosition typePosition = new SourcePosition();
        start(typePosition);
        switch (currentToken.kind) {
            case Token.VOID:
                match(Token.VOID);
                typeAST = new VoidType(dummyPosition);
                finish(typePosition);
                typeAST.position = typePosition;
                break;
            case Token.BOOLEAN:
                match(Token.BOOLEAN);
                typeAST = new BooleanType(dummyPosition);
                finish(typePosition);
                typeAST.position = typePosition;
                break;
            case Token.INT:
                match(Token.INT);
                typeAST = new IntType(dummyPosition);
                finish(typePosition);
                typeAST.position = typePosition;
                break;
            case Token.FLOAT:
                match(Token.FLOAT);
                typeAST = new FloatType(dummyPosition);
                finish(typePosition);
                typeAST.position = typePosition;
                break;
            default:
                syntaxError("expecting a type", currentToken.spelling);
        }
        return typeAST;
    }

    private Stmt parseCompoundStmt() throws SyntaxError {
        SourcePosition compoundStmtPosition = new SourcePosition();
        Stmt compoundStmt;
        List varDeclList;
        List stmtList;
        start(compoundStmtPosition);
        match(Token.LCURLY);
        if (currentToken.kind == Token.RCURLY) {
            match(Token.RCURLY);
            return new EmptyCompStmt(dummyPosition);
        }
        varDeclList = parseVarDeclList();
        stmtList = parseStmtList();
        match(Token.RCURLY);
        finish(compoundStmtPosition);
        compoundStmt = new CompoundStmt(varDeclList, stmtList, compoundStmtPosition);
        return compoundStmt;
    }

    private List parseStmtList() throws SyntaxError {
        List slAST;
        SourcePosition stmtPosition = new SourcePosition();
        start(stmtPosition);
        if (currentToken.kind != Token.RCURLY) {
            Stmt sAST = parseStmt();
            {
                if (currentToken.kind != Token.RCURLY) {
                    slAST = parseStmtList();
                    finish(stmtPosition);
                    slAST = new StmtList(sAST, slAST, stmtPosition);
                } else {
                    finish(stmtPosition);
                    slAST = new StmtList(sAST, new EmptyStmtList(dummyPosition), stmtPosition);
                }
            }
        } else slAST = new EmptyStmtList(dummyPosition);
        return slAST;
    }

    private Stmt parseStmt() throws SyntaxError {
        Stmt stmt;
        switch (currentToken.kind) {
            case Token.IF:
                SourcePosition ifPosition = new SourcePosition();
                Expr ifExpr;
                Stmt ifBody;
                Stmt elseBody;
                start(ifPosition);
                match(Token.IF);
                match(Token.LPAREN);
                ifExpr = parseAssignExpr();
                match(Token.RPAREN);
                ifBody = parseStmt();
                if (currentToken.kind == Token.ELSE) {
                    match(Token.ELSE);
                    elseBody = parseStmt();
                    finish(ifPosition);
                    stmt = new IfStmt(ifExpr, ifBody, elseBody, ifPosition);
                } else {
                    finish(ifPosition);
                    stmt = new IfStmt(ifExpr, ifBody, ifPosition);
                }
                break;
            case Token.FOR:
                SourcePosition forPosition = new SourcePosition();
                Expr expr1 = new EmptyExpr(dummyPosition);
                Expr expr2 = new EmptyExpr(dummyPosition);
                Expr expr3 = new EmptyExpr(dummyPosition);
                Stmt forBody;
                start(forPosition);
                match(Token.FOR);
                match(Token.LPAREN);
                if (currentToken.kind != Token.SEMICOLON) expr1 = parseAssignExpr();
                match(Token.SEMICOLON);
                if (currentToken.kind != Token.SEMICOLON) expr2 = parseAssignExpr();
                match(Token.SEMICOLON);
                if (currentToken.kind != Token.RPAREN) expr3 = parseAssignExpr();
                match(Token.RPAREN);
                forBody = parseStmt();
                finish(forPosition);
                stmt = new ForStmt(expr1, expr2, expr3, forBody, forPosition);
                break;
            case Token.WHILE:
                SourcePosition whilePosition = new SourcePosition();
                Expr whileExpr;
                Stmt whileBody;
                start(whilePosition);
                match(Token.WHILE);
                match(Token.LPAREN);
                whileExpr = parseAssignExpr();
                match(Token.RPAREN);
                whileBody = parseStmt();
                finish(whilePosition);
                stmt = new WhileStmt(whileExpr, whileBody, whilePosition);
                break;
            case Token.CONTINUE:
                SourcePosition continuePosition = new SourcePosition();
                start(continuePosition);
                match(Token.CONTINUE);
                match(Token.SEMICOLON);
                finish(continuePosition);
                stmt = new ContinueStmt(continuePosition);
                break;
            case Token.BREAK:
                SourcePosition breakPosition = new SourcePosition();
                start(breakPosition);
                match(Token.BREAK);
                match(Token.SEMICOLON);
                finish(breakPosition);
                stmt = new BreakStmt(breakPosition);
                break;
            case Token.RETURN:
                SourcePosition retPosition = new SourcePosition();
                Expr retExpr = new EmptyExpr(dummyPosition);
                start(retPosition);
                match(Token.RETURN);
                if (currentToken.kind != Token.SEMICOLON) retExpr = parseAssignExpr();
                match(Token.SEMICOLON);
                finish(retPosition);
                stmt = new ReturnStmt(retExpr, retPosition);
                break;
            case Token.LCURLY:
                stmt = parseCompoundStmt();
                break;
            default:
                SourcePosition stmtPosition = new SourcePosition();
                start(stmtPosition);
                if (currentToken.kind != Token.SEMICOLON) {
                    Expr eAST = parseAssignExpr();
                    match(Token.SEMICOLON);
                    finish(stmtPosition);
                    stmt = new ExprStmt(eAST, stmtPosition);
                } else {
                    match(Token.SEMICOLON);
                    finish(stmtPosition);
                    stmt = new ExprStmt(new EmptyExpr(dummyPosition), stmtPosition);
                }
                break;
        }
        return stmt;
    }

    private List parseParamList() throws SyntaxError {
        List paramList = new EmptyParaList(dummyPosition);
        SourcePosition formalsPosition = new SourcePosition();
        start(formalsPosition);
        match(Token.LPAREN);
        if (currentToken.kind != Token.RPAREN) paramList = parseProperParaList();
        match(Token.RPAREN);
        finish(formalsPosition);
        return paramList;
    }

    private List parseProperParaList() throws SyntaxError {
        SourcePosition paraListPosition = new SourcePosition();
        List paraList;
        ParaDecl para;
        start(paraListPosition);
        para = parseParaDecl();
        if (currentToken.kind == Token.COMMA) {
            match(Token.COMMA);
            paraList = new ParaList(para, parseProperParaList(), dummyPosition);
            finish(paraListPosition);
            paraList.position = paraListPosition;
        } else {
            finish(paraListPosition);
            paraList = new ParaList(para, new EmptyParaList(dummyPosition), paraListPosition);
        }
        return paraList;
    }

    private ParaDecl parseParaDecl() throws SyntaxError {
        SourcePosition paraPosition = new SourcePosition();
        ParaDecl para;
        Type paraType;
        start(paraPosition);
        paraType = parseType();
        para = (ParaDecl) parseDeclarator(paraType, "parameter");
        finish(paraPosition);
        return para;
    }

    private List parseArgList() throws SyntaxError {
        SourcePosition argListPosition = new SourcePosition();
        List argList = new EmptyArgList(dummyPosition);
        start(argListPosition);
        match(Token.LPAREN);
        if (currentToken.kind != Token.RPAREN) argList = parseProperArgList();
        match(Token.RPAREN);
        return argList;
    }

    private List parseProperArgList() throws SyntaxError {
        SourcePosition argListPosition = new SourcePosition();
        List argList;
        Arg arg;
        start(argListPosition);
        SourcePosition argPosition = new SourcePosition();
        start(argPosition);
        Expr argExpr = parseAssignExpr();
        finish(argPosition);
        arg = new Arg(argExpr, argPosition);
        if (currentToken.kind == Token.COMMA) {
            match(Token.COMMA);
            argList = new ArgList(arg, parseProperArgList(), dummyPosition);
            finish(argListPosition);
            argList.position = argListPosition;
        } else {
            finish(argListPosition);
            argList = new ArgList(arg, new EmptyArgList(dummyPosition), argListPosition);
        }
        return argList;
    }

    private Expr parseAssignExpr() throws SyntaxError {
        SourcePosition assignExpPosition = new SourcePosition();
        Expr assignExpr;
        start(assignExpPosition);
        assignExpr = parseCondOrExpr();
        finish(assignExpPosition);
        if (currentToken.kind == Token.EQ) {
            acceptOperator();
            assignExpr = new AssignExpr(assignExpr, parseAssignExpr(), dummyPosition);
            finish(assignExpPosition);
            assignExpr.position = assignExpPosition;
        }
        return assignExpr;
    }

    private Expr parseCondOrExpr() throws SyntaxError {
        SourcePosition condOrExprPosition = new SourcePosition();
        Expr condOrExpr;
        Operator or;
        start(condOrExprPosition);
        condOrExpr = parseCondAndExpr();
        if (currentToken.kind == Token.OROR) {
            or = acceptOperator();
            condOrExpr = new BinaryExpr(condOrExpr, or, parseCondAndExpr(), dummyPosition);
            finish(condOrExprPosition);
            condOrExpr.position = condOrExprPosition;
        }
        return condOrExpr;
    }

    private Expr parseCondAndExpr() throws SyntaxError {
        SourcePosition condAndExprPosition = new SourcePosition();
        Expr condAndExpr;
        Operator and;
        start(condAndExprPosition);
        condAndExpr = parseEqualityExpr();
        if (currentToken.kind == Token.ANDAND) {
            and = acceptOperator();
            condAndExpr = new BinaryExpr(condAndExpr, and, parseEqualityExpr(), dummyPosition);
            finish(condAndExprPosition);
            condAndExpr.position = condAndExprPosition;
        }
        return condAndExpr;
    }

    private Expr parseEqualityExpr() throws SyntaxError {
        SourcePosition equalityPosition = new SourcePosition();
        Expr equalityExpr;
        Operator eqeq;
        start(equalityPosition);
        equalityExpr = parseRelExpr();
        if (currentToken.kind == Token.EQEQ) {
            eqeq = acceptOperator();
            equalityExpr = new BinaryExpr(equalityExpr, eqeq, parseEqualityExpr(), dummyPosition);
            finish(equalityPosition);
            equalityExpr.position = equalityPosition;
        }
        return equalityExpr;
    }

    private Expr parseRelExpr() throws SyntaxError {
        SourcePosition relPosition = new SourcePosition();
        Expr relExpr;
        Operator angleOp;
        start(relPosition);
        relExpr = parseAdditiveExpr();
        switch (currentToken.kind) {
            case Token.LT:
            case Token.LTEQ:
            case Token.GT:
            case Token.GTEQ:
                angleOp = acceptOperator();
                relExpr = new BinaryExpr(relExpr, angleOp, parseRelExpr(), dummyPosition);
                finish(relPosition);
                relExpr.position = relPosition;
            default:
                break;
        }
        return relExpr;
    }

    private Expr parseAdditiveExpr() throws SyntaxError {
        Expr exprAST;
        SourcePosition addStartPosition = new SourcePosition();
        start(addStartPosition);
        exprAST = parseMultiplicativeExpr();
        while (currentToken.kind == Token.PLUS || currentToken.kind == Token.MINUS) {
            Operator opAST = acceptOperator();
            Expr e2AST = parseMultiplicativeExpr();
            SourcePosition addPos = new SourcePosition();
            addPos.lineStart = addStartPosition.lineStart;
            addPos.charStart = addStartPosition.charStart;
            finish(addPos);
            exprAST = new BinaryExpr(exprAST, opAST, e2AST, addPos);
        }
        return exprAST;
    }

    private Expr parseMultiplicativeExpr() throws SyntaxError {
        Expr exprAST;
        SourcePosition multStartPosition = new SourcePosition();
        start(multStartPosition);
        exprAST = parseUnaryExpr();
        while (currentToken.kind == Token.MULT || currentToken.kind == Token.DIV) {
            Operator opAST = acceptOperator();
            Expr e2AST = parseUnaryExpr();
            SourcePosition multPos = new SourcePosition();
            multPos.lineStart = multStartPosition.lineStart;
            multPos.charStart = multStartPosition.charStart;
            finish(multPos);
            exprAST = new BinaryExpr(exprAST, opAST, e2AST, multPos);
        }
        return exprAST;
    }

    private Expr parseUnaryExpr() throws SyntaxError {
        Expr exprAST;
        SourcePosition unaryPosition = new SourcePosition();
        start(unaryPosition);
        switch (currentToken.kind) {
            case Token.PLUS:
            case Token.MINUS:
            case Token.NOT: {
                Operator opAST = acceptOperator();
                Expr e2AST = parseUnaryExpr();
                finish(unaryPosition);
                exprAST = new UnaryExpr(opAST, e2AST, unaryPosition);
            }
            break;
            default:
                exprAST = parsePrimaryExpr();
                break;
        }
        return exprAST;
    }

    private Expr parsePrimaryExpr() throws SyntaxError {
        Expr exprAST = null;
        SourcePosition primPosition = new SourcePosition();
        start(primPosition);
        switch (currentToken.kind) {
            case Token.ID:
                prevTokenPosition = currentToken.position;
                String spelling = currentToken.spelling;
                Ident iAST = new Ident(spelling, prevTokenPosition);
                currentToken = scanner.getToken();
                finish(primPosition);
                Var simVAST = new SimpleVar(iAST, primPosition);
                exprAST = new VarExpr(simVAST, primPosition);
                if (currentToken.kind == Token.LBRACKET) {
                    match(Token.LBRACKET);
                    Expr arrayIndexExp = parseAssignExpr();
                    match(Token.RBRACKET);
                    finish(primPosition);
                    exprAST = new ArrayExpr(simVAST, arrayIndexExp, primPosition);
                } else if (currentToken.kind == Token.LPAREN) {
                    List argList = parseArgList();
                    finish(primPosition);
                    exprAST = new CallExpr(iAST, argList, primPosition);
                }
                break;
            case Token.LPAREN:
                accept();
                exprAST = parseAssignExpr();
                match(Token.RPAREN);
                break;
            case Token.INTLITERAL:
                String intLiteralSpelling = currentToken.spelling;
                accept();
                IntLiteral intLiteral = new IntLiteral(intLiteralSpelling, prevTokenPosition);
                finish(primPosition);
                exprAST = new IntExpr(intLiteral, primPosition);
                break;
            case Token.FLOATLITERAL:
                String floatLiteralSpelling = currentToken.spelling;
                accept();
                FloatLiteral floatLiteral = new FloatLiteral(floatLiteralSpelling, prevTokenPosition);
                finish(primPosition);
                exprAST = new FloatExpr(floatLiteral, primPosition);
                break;
            case Token.BOOLEANLITERAL:
                String boolLiteralSpelling = currentToken.spelling;
                accept();
                BooleanLiteral boolLiteral = new BooleanLiteral(boolLiteralSpelling, prevTokenPosition);
                finish(primPosition);
                exprAST = new BooleanExpr(boolLiteral, primPosition);
                break;
            case Token.STRINGLITERAL:
                String stringLiteralSpelling = currentToken.spelling;
                accept();
                StringLiteral stringLiteral = new StringLiteral(stringLiteralSpelling, prevTokenPosition);
                finish(primPosition);
                exprAST = new StringExpr(stringLiteral, primPosition);
                break;
            default:
                syntaxError("illegal primary expression", currentToken.spelling);
        }

        return exprAST;
    }

    private Operator acceptOperator() {
        Operator operator;
        prevTokenPosition = currentToken.position;
        String spelling = currentToken.spelling;
        operator = new Operator(spelling, prevTokenPosition);
        currentToken = scanner.getToken();
        return operator;
    }
}