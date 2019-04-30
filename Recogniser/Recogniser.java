package VC.Recogniser;

import VC.ErrorReporter;
import VC.Scanner.Scanner;
import VC.Scanner.SourcePosition;
import VC.Scanner.Token;

import java.util.Arrays;
import java.util.List;

import static VC.Scanner.Token.*;

public class Recogniser {
    private List<Integer> var_decl = Arrays.asList(VOID, BOOLEAN, INT, FLOAT);
    private List<Integer> stmt = Arrays.asList(LCURLY, IF, FOR, WHILE, BREAK, CONTINUE, RETURN, PLUS,
            MINUS, NOT, ID, LPAREN, INTLITERAL, FLOATLITERAL, BOOLEANLITERAL, STRINGLITERAL, SEMICOLON);
    private Scanner scanner;
    private ErrorReporter errorReporter;
    private Token currentToken;

    public Recogniser(Scanner lexer, ErrorReporter reporter) {
        scanner = lexer;
        errorReporter = reporter;
        currentToken = scanner.getToken();
    }

    public void parseProgram() {
        try {
            while (currentToken.kind != Token.EOF) parseDecl();
        } catch (SyntaxError ignore) {
        }
    }

    private void match(int tokenExpected) throws SyntaxError {
        if (currentToken.kind == tokenExpected) currentToken = scanner.getToken();
        else syntacticError("%s expected", Token.spell(tokenExpected));
    }

    private void syntacticError(String messageTemplate, String tokenQuoted) throws SyntaxError {
        SourcePosition pos = currentToken.position;
        errorReporter.reportError(messageTemplate, tokenQuoted, pos);
        throw (new SyntaxError());
    }

    private void parseDecl() throws SyntaxError {
        parseType();
        parseIdent();
        if (currentToken.kind == Token.LPAREN) {
            parseParameterList();
            parseCompoundStmt();
        } else parseVarDecl(true);
    }

    private void parseParameterList() throws SyntaxError {
        match(Token.LPAREN);
        if (currentToken.kind != Token.RPAREN) {
            parseType();
            parseDeclarator();
            while (currentToken.kind == Token.COMMA) {
                match(Token.COMMA);
                parseType();
                parseDeclarator();
            }
        }
        match(Token.RPAREN);
    }

    private void parseCompoundStmt() throws SyntaxError {
        match(Token.LCURLY);
        while (var_decl.contains(currentToken.kind)) parseVarDecl(false);
        while (stmt.contains(currentToken.kind)) parseStmt();
        match(Token.RCURLY);
    }

    private void parseVarDecl(boolean newDecl) throws SyntaxError {
        if (newDecl) {
            if (currentToken.kind == Token.LBRACKET) {
                match(Token.LBRACKET);
                if (currentToken.kind == Token.INTLITERAL) match(Token.INTLITERAL);
                match(Token.RBRACKET);
            }
            if (currentToken.kind == Token.EQ) {
                match(Token.EQ);
                parseInitialiser();
            }
            if (currentToken.kind == Token.COMMA) {
                match(Token.COMMA);
                parseInitDeclaratorList();
            }
            match(Token.SEMICOLON);
        } else {
            parseType();
            parseInitDeclaratorList();
            match(Token.SEMICOLON);
        }
    }

    private void parseInitDeclarator() throws SyntaxError {
        parseDeclarator();
        if (currentToken.kind == Token.EQ) {
            match(Token.EQ);
            parseInitialiser();
        }
    }

    private void parseInitDeclaratorList() throws SyntaxError {
        parseInitDeclarator();
        while (currentToken.kind == Token.COMMA) {
            match(Token.COMMA);
            parseInitDeclarator();
        }
    }

    private void parseDeclarator() throws SyntaxError {
        parseIdent();
        if (currentToken.kind == Token.LBRACKET) {
            match(Token.LBRACKET);
            if (currentToken.kind == Token.INTLITERAL) match(Token.INTLITERAL);
            match(Token.RBRACKET);
        }
    }

    private void parseInitialiser() throws SyntaxError {
        if (currentToken.kind == Token.LCURLY) {
            match(Token.LCURLY);
            parseAssignExpr();
            while (currentToken.kind == Token.COMMA) {
                match(Token.COMMA);
                parseAssignExpr();
            }
            match(Token.RCURLY);
        } else parseAssignExpr();
    }

    private void parseType() throws SyntaxError {
        switch (currentToken.kind) {
            case Token.VOID:
                match(Token.VOID);
                break;
            case Token.BOOLEAN:
                match(Token.BOOLEAN);
                break;
            case Token.INT:
                match(Token.INT);
                break;
            case Token.FLOAT:
                match(Token.FLOAT);
                break;
            default:
                syntacticError("Type expected", currentToken.spelling);
        }
    }

    private void parseStmt() throws SyntaxError {
        switch (currentToken.kind) {
            case Token.LCURLY:
                parseCompoundStmt();
                break;
            case Token.IF:
                match(Token.IF);
                match(Token.LPAREN);
                parseAssignExpr();
                match(Token.RPAREN);
                parseStmt();
                if (currentToken.kind == Token.ELSE) {
                    match(Token.ELSE);
                    parseStmt();
                }
                break;
            case Token.FOR:
                match(Token.FOR);
                match(Token.LPAREN);
                if (currentToken.kind != Token.SEMICOLON) parseAssignExpr();
                match(Token.SEMICOLON);
                if (currentToken.kind != Token.SEMICOLON) parseAssignExpr();
                match(Token.SEMICOLON);
                if (currentToken.kind != Token.RPAREN) parseAssignExpr();
                match(Token.RPAREN);
                parseStmt();
                break;
            case Token.WHILE:
                match(Token.WHILE);
                match(Token.LPAREN);
                parseAssignExpr();
                match(Token.RPAREN);
                parseStmt();
                break;
            case Token.CONTINUE:
                match(Token.CONTINUE);
                match(Token.SEMICOLON);
                break;
            case Token.BREAK:
                match(Token.BREAK);
                match(Token.SEMICOLON);
                break;
            case Token.RETURN:
                match(Token.RETURN);
                if (currentToken.kind != Token.SEMICOLON) parseAssignExpr();
                match(Token.SEMICOLON);
                break;
            default:
                if (currentToken.kind != Token.SEMICOLON) parseAssignExpr();
                match(Token.SEMICOLON);
                break;
        }
    }

    private void parseIdent() throws SyntaxError {
        if (currentToken.kind == Token.ID) currentToken = scanner.getToken();
        else syntacticError("Identifier expected", "");
    }

    private void parseAssignExpr() throws SyntaxError {
        parseCondOrExpr();
        while (currentToken.kind == Token.EQ) {
            match(Token.EQ);
            parseCondOrExpr();
        }
    }

    private void parseCondOrExpr() throws SyntaxError {
        parseCondAndExpr();
        while (currentToken.kind == Token.OROR) {
            match(Token.OROR);
            parseCondAndExpr();
        }
    }

    private void parseCondAndExpr() throws SyntaxError {
        parseEqualityExpr();
        while (currentToken.kind == Token.ANDAND) {
            match(Token.ANDAND);
            parseEqualityExpr();
        }
    }

    private void parseEqualityExpr() throws SyntaxError {
        parseRelExpr();
        while (currentToken.kind == Token.EQEQ || currentToken.kind == Token.NOTEQ) {
            if (currentToken.kind == Token.EQEQ) match(Token.EQEQ);
            else match(Token.NOTEQ);
            parseRelExpr();
        }
    }

    private void parseRelExpr() throws SyntaxError {
        parseAdditiveExpr();
        switch (currentToken.kind) {
            case Token.LT:
                match(Token.LT);
                parseAdditiveExpr();
                break;
            case Token.LTEQ:
                match(Token.LTEQ);
                parseAdditiveExpr();
                break;
            case Token.GT:
                match(Token.GT);
                parseAdditiveExpr();
                break;
            case Token.GTEQ:
                match(Token.GTEQ);
                parseAdditiveExpr();
                break;
            default:
                break;
        }
    }

    private void parseAdditiveExpr() throws SyntaxError {
        parseMultiplicativeExpr();
        while (currentToken.kind == Token.PLUS || currentToken.kind == Token.MINUS) {
            currentToken = scanner.getToken();
            parseMultiplicativeExpr();
        }
    }

    private void parseMultiplicativeExpr() throws SyntaxError {
        parseUnaryExpr();
        while (currentToken.kind == Token.MULT || currentToken.kind == Token.DIV) {
            currentToken = scanner.getToken();
            parseUnaryExpr();
        }
    }

    private void parseUnaryExpr() throws SyntaxError {
        switch (currentToken.kind) {
            case Token.PLUS:
            case Token.MINUS:
            case Token.NOT:
                currentToken = scanner.getToken();
                parseUnaryExpr();
                break;
            default:
                parsePrimaryExpr();
                break;
        }
    }

    private void parsePrimaryExpr() throws SyntaxError {
        switch (currentToken.kind) {
            case Token.ID:
                parseIdent();
                switch (currentToken.kind) {
                    case Token.LBRACKET:
                        match(Token.LBRACKET);
                        parseAssignExpr();
                        match(Token.RBRACKET);
                        break;
                    case Token.LPAREN:
                        match(Token.LPAREN);
                        if (currentToken.kind != Token.RPAREN) {
                            parseAssignExpr();
                            while (currentToken.kind == Token.COMMA) {
                                match(Token.COMMA);
                                parseAssignExpr();
                            }
                        }
                        match(Token.RPAREN);
                        break;
                }
                break;
            case Token.LPAREN:
                currentToken = scanner.getToken();
                parseAssignExpr();
                match(Token.RPAREN);
                break;
            case Token.INTLITERAL:
                parseLiteral(Token.INTLITERAL);
                break;
            case Token.FLOATLITERAL:
                parseLiteral(Token.FLOATLITERAL);
                break;
            case Token.BOOLEANLITERAL:
                parseLiteral(Token.BOOLEANLITERAL);
                break;
            case Token.STRINGLITERAL:
                parseLiteral(Token.STRINGLITERAL);
                break;
            default:
                syntacticError("Illegal expression", currentToken.spelling);
        }
    }

    private void parseLiteral(int tokenKind) throws SyntaxError {
        switch (tokenKind) {
            case Token.INTLITERAL:
                currentToken = scanner.getToken();
                break;
            case Token.FLOATLITERAL:
                currentToken = scanner.getToken();
                break;
            case Token.BOOLEANLITERAL:
                currentToken = scanner.getToken();
                break;
            case Token.STRINGLITERAL:
                currentToken = scanner.getToken();
                break;
            default:
                syntacticError("Literal expected", "");
                break;
        }
    }
}
