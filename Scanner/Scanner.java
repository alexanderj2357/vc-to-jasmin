package VC.Scanner;

import VC.ErrorReporter;

import java.util.Arrays;
import java.util.List;

public final class Scanner {
    private SourceFile sourceFile;
    private ErrorReporter errorReporter;
    private StringBuffer currentSpelling;
    private boolean debug;
    private char currChar;
    private int lineNum = 1;
    private int colStartNum = 1;
    private int colEndNum = 1;

    public Scanner(SourceFile source, ErrorReporter reporter) {
        sourceFile = source;
        errorReporter = reporter;
        debug = false;
        currChar = sourceFile.getNextChar();
    }

    public void enableDebugging() {
        debug = true;
    }

    private void accept() {
        if (currChar == SourceFile.eof) {
            currentSpelling.append(Token.spell(Token.EOF));
        } else if (!Character.isWhitespace(currChar) && currChar != '"') {
            currentSpelling.append(currChar);
        }

        colEndNum += 1;

        if (Character.isWhitespace(currChar)) {
            handleWhiteSpace();
        }

        currChar = sourceFile.getNextChar();
    }

    private void handleWhiteSpace() {
        currentSpelling = new StringBuffer();

        if (currChar == '\t') {
            colEndNum = ((colEndNum - 1) / 8 + 1) * 8 + 1;
        }

        colStartNum = colEndNum;

        if (currChar == '\n' || currChar == '\r') {
            lineNum += 1;
            colEndNum = colStartNum = 1;
        }
    }

    private void handleEscape(char c) {
        switch (c) {
            case 'b':
                currentSpelling.append('\b');
                break;
            case 'f':
                currentSpelling.append('\f');
                break;
            case 'n':
                currentSpelling.append('\n');
                break;
            case 'r':
                currentSpelling.append('\r');
                break;
            case 't':
                currentSpelling.append('\t');
                break;
            case '\'':
                currentSpelling.append('\'');
                break;
            case '\"':
                currentSpelling.append('\"');
                break;
            case '\\':
                currentSpelling.append('\\');
                break;
        }

        colEndNum += 2;
        currChar = sourceFile.getNextChar();
        currChar = sourceFile.getNextChar();
    }

    private int handleSeparators() {
        int retVal = -1;

        switch (currChar) {
            case '{':
                accept();
                retVal = Token.LCURLY;
                break;
            case '}':
                accept();
                retVal = Token.RCURLY;
                break;
            case '(':
                accept();
                retVal = Token.LPAREN;
                break;
            case ')':
                accept();
                retVal = Token.RPAREN;
                break;
            case '[':
                accept();
                retVal = Token.LBRACKET;
                break;
            case ']':
                accept();
                retVal = Token.RBRACKET;
                break;
            case ';':
                accept();
                retVal = Token.SEMICOLON;
                break;
            case ',':
                accept();
                retVal = Token.COMMA;
                break;
        }

        return retVal;
    }

    private int handleOperators() {
        int retVal = -1;

        switch (currChar) {
            case '+':
                accept();
                retVal = Token.PLUS;
                break;
            case '-':
                accept();
                retVal = Token.MINUS;
                break;
            case '*':
                accept();
                retVal = Token.MULT;
                break;
            case '!':
                accept();
                if (currChar == '=') {
                    accept();
                    retVal = Token.NOTEQ;
                    break;
                } else {
                    retVal = Token.NOT;
                    break;
                }
            case '=':
                accept();
                if (currChar == '=') {
                    accept();
                    retVal = Token.EQEQ;
                    break;
                } else {
                    retVal = Token.EQ;
                    break;
                }
            case '>':
                accept();
                if (currChar == '=') {
                    accept();
                    retVal = Token.GTEQ;
                    break;
                } else {
                    retVal = Token.GT;
                    break;
                }
            case '<':
                accept();
                if (currChar == '=') {
                    accept();
                    retVal = Token.LTEQ;
                    break;
                } else {
                    retVal = Token.LT;
                    break;
                }
            case '&':
                accept();
                if (currChar == '&') {
                    accept();
                    retVal = Token.ANDAND;
                    break;
                } else {
                    retVal = Token.ERROR;
                    break;
                }
            case '|':
                accept();
                if (currChar == '|') {
                    accept();
                    retVal = Token.OROR;
                    break;
                } else {
                    retVal = Token.ERROR;
                    break;
                }
            case '/':
                accept();

                if (currChar == '/' || currChar == '*') {
                    return handleComments();
                } else {
                    return Token.DIV;
                }
        }

        return retVal;
    }

    private int handleComments() {
        int result = -1;

        if (currChar == '/') {
            while (currChar != '\n') {
                accept();
            }

            accept();
            colStartNum = colEndNum;
            result = nextToken();
        } else if (currChar == '*') {
            accept();
            while (currChar != '*' || sourceFile.inspectChar(1) != '/') {
                accept();
                if (currChar == SourceFile.eof) {
                    String errorMessage = "" + ": unterminated comment";
                    errorReporter.reportError(errorMessage, Token.spell(Token.ERROR),
                            new SourcePosition(lineNum, colStartNum, colStartNum));
                    return nextToken();
                }
            }
            accept();
            accept();
            currentSpelling = new StringBuffer();
            colStartNum = colEndNum;
            result = nextToken();
        }

        return result;
    }

    private int handleStrings() {
        char startChar = currChar;
        accept();

        while (currChar != startChar) {
            if (currChar == '\n') {
                String errorMessage = currentSpelling.toString() + ": unterminated string";
                errorReporter.reportError(errorMessage, Token.spell(Token.STRINGLITERAL),
                        new SourcePosition(lineNum, colStartNum, colStartNum));
                return Token.STRINGLITERAL;
            }

            if (currChar == '\\') {
                char nextChar = sourceFile.inspectChar(1);

                List<Character> escapeChars = Arrays.asList('b', 'f', 'n', 'r', 't', '\'', '\"', '\\');

                if (!escapeChars.contains(nextChar)) {
                    accept();
                    accept();
                    String errorMessage = "\\" + nextChar + ": illegal escape character";
                    errorReporter.reportError(errorMessage, Token.spell(Token.STRINGLITERAL),
                            new SourcePosition(lineNum, colStartNum, colEndNum - 1));
                } else {
                    handleEscape(nextChar);
                    continue;
                }
            }

            currentSpelling.append(currChar);
            colEndNum += 1;
            currChar = sourceFile.getNextChar();
        }

        accept();

        return Token.STRINGLITERAL;
    }

    private int handleNumbers() {
        int retVal = -1;

        if (currChar >= '0' && currChar <= '9') {
            retVal = Token.INTLITERAL;
            accept();

            while (currChar >= '0' && currChar <= '9') {
                accept();
            }
        }

        if (currChar == '.') {
            if (sourceFile.inspectChar(1) >= '0' && sourceFile.inspectChar(1) <= '9') {
                accept();
                retVal = Token.FLOATLITERAL;

                while (currChar >= '0' && currChar <= '9') {
                    accept();
                }
            } else if (retVal == Token.INTLITERAL) {
                accept();
                retVal = Token.FLOATLITERAL;
            }
        }

        if (currChar == 'e' || currChar == 'E') {
            if (sourceFile.inspectChar(2) >= '0'
                    && sourceFile.inspectChar(2) <= '9'
                    && (sourceFile.inspectChar(1) == '+'
                    || sourceFile.inspectChar(1) == '-')) {
                accept();
            }

            if (sourceFile.inspectChar(1) >= '0' && sourceFile.inspectChar(1) <= '9') {
                if (retVal == Token.INTLITERAL || retVal == Token.FLOATLITERAL) {
                    accept();

                    while (currChar >= '0'
                            && currChar <= '9'
                            || currChar == '+'
                            || currChar == '-') {
                        accept();
                        retVal = Token.FLOATLITERAL;
                    }
                }
            }
        }

        return retVal;
    }

    private int nextToken() {
        List<Character> separators = Arrays.asList('{', '}', '(', ')', '[', ']', ';', ',');
        List<Character> operators = Arrays.asList('+', '-', '*', '!', '=', '>', '<', '&', '|', '/');

        if (separators.contains(currChar)) {
            return handleSeparators();
        } else if (operators.contains(currChar)) {
            return handleOperators();
        } else {
            switch (currChar) {
                case ' ':
                    accept();
                    return nextToken();
                case '"':
                    return handleStrings();
                case '\'':
                case '\f':
                case '\b':
                case '\t':
                case '\r':
                case '\n':
                    accept();
                    return nextToken();
                case SourceFile.eof:
                    accept();
                    return Token.EOF;
                case '.':
                    if (!Character.isDigit(sourceFile.inspectChar(1))) {
                        accept();
                        return Token.ERROR;
                    } else {
                        return handleNumbers();
                    }
                default:
                    if (Character.isDigit(currChar)) {
                        return handleNumbers();
                    } else if (Character.isAlphabetic(currChar) || currChar == '_') {
                        while (Character.isLetterOrDigit(currChar) || currChar == '_') {
                            accept();
                        }

                        switch (currentSpelling.toString()) {
                            case "boolean":
                                return Token.BOOLEAN;
                            case "break":
                                return Token.BREAK;
                            case "continue":
                                return Token.CONTINUE;
                            case "else":
                                return Token.ELSE;
                            case "float":
                                return Token.FLOAT;
                            case "for":
                                return Token.FOR;
                            case "if":
                                return Token.IF;
                            case "int":
                                return Token.INT;
                            case "return":
                                return Token.RETURN;
                            case "void":
                                return Token.VOID;
                            case "while":
                                return Token.WHILE;
                            case "true":
                                return Token.BOOLEANLITERAL;
                            case "false":
                                return Token.BOOLEANLITERAL;
                            default:
                                return Token.ID;
                        }
                    }
            }
        }

        accept();
        return Token.ERROR;
    }

    public Token getToken() {
        Token tok;
        int kind;
        currentSpelling = new StringBuffer();
        kind = nextToken();
        SourcePosition sourcePos = new SourcePosition(lineNum, colStartNum, colEndNum - 1);
        tok = new Token(kind, currentSpelling.toString(), sourcePos);
        colStartNum = colEndNum;

        if (debug) {
            System.out.println(tok);
        }

        return tok;
    }
}