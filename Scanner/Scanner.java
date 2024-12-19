/***
 ***
 *** 	 Scanner.java rewrite xd 12 hrs speed run  	                         
 ***
 */

package VC.Scanner;

import VC.ErrorReporter;

public final class Scanner {

    private SourceFile sourceFile;
    private boolean debug;
    static private final int NOMATCH = 69;
    private ErrorReporter errorReporter;
    private StringBuffer currentSpelling;
    private char currentChar;
    private SourcePosition sourcePos;
    private SourcePosition tokenPos;
    private int tabCount;

    // =========================================================

    public Scanner(SourceFile source, ErrorReporter reporter) {
        sourceFile = source;
        errorReporter = reporter;
        currentChar = sourceFile.getNextChar();
        sourcePos = new SourcePosition(1, 1, 0);
        tokenPos = new SourcePosition(1, 1, 0);
        debug = false;
        tabCount = 0;

    }

    private char inspectChar(int nthChar) {
        return sourceFile.inspectChar(nthChar);
    }

    public void enableDebugging() {
        debug = true;
    }

    private void accept() {
        // System.out.println("Current Char:~" + currentChar+"~");
        if (currentChar == '\n') {
            sourcePos.lineStart++;
            sourcePos.lineFinish = sourcePos.lineStart;
            sourcePos.charStart = 1;
            sourcePos.charFinish = 0;
            tabCount = 0;
        } else if (currentChar == '\t') {
            sourcePos.charFinish += 8 - tabCount % 8;
        } else {
            sourcePos.charFinish++;
            tokenPos.charFinish++;
            tabCount++;
        }
        currentChar = sourceFile.getNextChar();
        // System.out.println("Next Char:~" + currentChar+"~");

    }

    private int spellRet(int Token, boolean... rep) {
        for (int i = 0; i <= rep.length; i++) {
            currentSpelling.append(currentChar);
            accept();
        }
        return Token;
    }

    private int checkOP() {
        switch (currentChar) {
            case '+':
                return spellRet(Token.PLUS);
            case '-':
                return spellRet(Token.MINUS);
            case '*':
                return spellRet(Token.MULT);
            case '/':
                return spellRet(Token.DIV);
            case '!':
                return (inspectChar(1) == '=') ? spellRet(Token.NOTEQ, true) : spellRet(Token.NOT);
            case '=':
                return (inspectChar(1) == '=') ? spellRet(Token.EQEQ, true) : spellRet(Token.EQ);
            case '<':
                return (inspectChar(1) == '=') ? spellRet(Token.LTEQ, true) : spellRet(Token.LT);
            case '>':
                return (inspectChar(1) == '=') ? spellRet(Token.GTEQ, true) : spellRet(Token.GT);
            case '&':
                return (inspectChar(1) == '&') ? spellRet(Token.ANDAND, true) : NOMATCH;
            case '|':
                return (inspectChar(1) == '|') ? spellRet(Token.OROR, true) : NOMATCH;
            default:
                return NOMATCH;
        }
    }

    private int checkSEP() {
        switch (currentChar) {
            case '{':
                return spellRet(Token.LCURLY);
            case '}':
                return spellRet(Token.RCURLY);
            case '(':
                return spellRet(Token.LPAREN);
            case ')':
                return spellRet(Token.RPAREN);
            case '[':
                return spellRet(Token.LBRACKET);
            case ']':
                return spellRet(Token.RBRACKET);
            case ';':
                return spellRet(Token.SEMICOLON);
            case ',':
                return spellRet(Token.COMMA);
            default:
                return NOMATCH;
        }
    }

    private int escapes() {
        switch (inspectChar(1)) {
            case 'b':
                return '\b';
            case 'f':
                return '\f';
            case 'n':
                return '\n';
            case 'r':
                return '\r';
            case 't':
                return '\t';
            case '\'':
                return '\'';
            case '"':
                return '\"';
            case '\\':
                return '\\';
            default:
                return NOMATCH;
        }
    }

    private int procStr() {
        accept();
        while (currentChar != '"') {
            boolean flag = false; 
            if (currentChar == '\n' || currentChar == SourceFile.eof) {
                SourcePosition strToken = new SourcePosition(tokenPos.lineStart, tokenPos.charStart,
                        tokenPos.charStart);
                errorReporter.reportError("%: unterminated string", currentSpelling.toString(), strToken);
                accept();
                flag = true;
                return Token.STRINGLITERAL;
            }
            if (currentChar == '\\') {
                flag = true; 
                int esc = escapes();
                accept();
                if (esc == NOMATCH) {
                    String cur = "\\" + currentChar;
                    if (currentChar == '\n') {
                        cur = "\\";
                        currentSpelling.append(cur);
                        errorReporter.reportError("%: unterminated string", currentSpelling.toString(), tokenPos);
                        errorReporter.reportError("%: illegal escape character", cur, tokenPos);
                        accept();
                        return Token.STRINGLITERAL;

                    } else {
                        currentSpelling.append(cur);
                        errorReporter.reportError("%: illegal escape character", cur, tokenPos);
                    }
                } else {
                    currentSpelling.append((char) esc);
                }
                accept();
            } 
            if (!flag) {
                currentSpelling.append(currentChar);
                accept();
            }
        }
        accept();
        return Token.STRINGLITERAL;
    }

    private int checkLIT() {
        int type = NOMATCH;
        boolean flag = false; 
        if (currentChar == '"') {
            return procStr();
        } else if (Character.isDigit(currentChar) || currentChar == '.') {
            while (Character.isDigit(currentChar)) {
                type = Token.INTLITERAL;
                flag = true;
                currentSpelling.append(currentChar);
                accept();
            }
            if (currentChar == '.' && (flag == true || Character.isDigit(inspectChar(1)))) {
                type = Token.FLOATLITERAL;
                currentSpelling.append(currentChar);
                accept();
                while (Character.isDigit(currentChar)) {
                    currentSpelling.append(currentChar);
                    accept();
                }
            } 
            if (currentChar == 'e' || currentChar == 'E') {
                
                if ((inspectChar(1) == '+' || inspectChar(1) == '-') && Character.isDigit(inspectChar(2))) {
                    //System.out.println("exp");
                    type = Token.FLOATLITERAL;
                    currentSpelling.append(currentChar);
                    accept();
                    currentSpelling.append(currentChar);
                    accept();
                    while (Character.isDigit(currentChar)) {
                        currentSpelling.append(currentChar);
                        accept();
                    }
                } else if (Character.isDigit(inspectChar(1))) {
                    //System.out.println("exppy");
                    currentSpelling.append(currentChar);
                    accept();
                    type = Token.FLOATLITERAL;
                    while (Character.isDigit(currentChar)) {
                        currentSpelling.append(currentChar);
                        accept();
                    }
                }
            }
            return type; 
        }
        
        
        return NOMATCH;
    }

    private int checkID() {
        if (currentChar >= 'a' && currentChar <= 'z' || currentChar >= 'A'
                && currentChar <= 'Z' || currentChar == '_') {
            while (currentChar >= 'a' && currentChar <= 'z' || currentChar >= 'A'
                    && currentChar <= 'Z' || currentChar == '_' || Character.isDigit(currentChar)) {
                currentSpelling.append(currentChar);
                accept();
            }
            return (currentSpelling.toString().equals("true")
                    || currentSpelling.toString().equals("false")) ? Token.BOOLEANLITERAL : Token.ID;
        }
        return NOMATCH;
    }

    private int checkSPEC() {
        if (currentChar == SourceFile.eof) {
            currentSpelling.append(Token.spell(Token.EOF));
            accept();
            return Token.EOF;
        }
        return NOMATCH;
    }

    private enum Tokens {
        OP, SEP, ID, LIT, SPEC
    }

    private int tokenCheck(Tokens type) {
        switch (type) {
            case OP:
                return checkOP();
            case SEP:
                return checkSEP();
            case LIT:
                return checkLIT();
            case ID:
                return checkID();
            case SPEC:
                return checkSPEC();
            default:
                return NOMATCH;
        }
    }

    private int nextToken() {

        for (Tokens type : Tokens.values()) {
            int ret = tokenCheck(type);
            if (ret != NOMATCH) {
                return ret;
            }
        }
        return spellRet(Token.ERROR);
    }

    void skipSpaceAndComments() {
        if (Character.isWhitespace(currentChar)) {
            accept();
            skipSpaceAndComments();
        } else if (currentChar == '/') {
            if (inspectChar(1) == '/') {
                while (currentChar != '\n') {
                    accept();
                }
                accept();
                skipSpaceAndComments();
            } else if (inspectChar(1) == '*') {
                SourcePosition commentStart = new SourcePosition(sourcePos.lineStart, sourcePos.charFinish + 1,
                        sourcePos.charFinish + 1);
                while (currentChar != '*' || inspectChar(1) != '/') {
                    if (currentChar == SourceFile.eof) {

                        errorReporter.reportError("%: unterminated comment", "", commentStart);
                        return;
                    }
                    accept();
                }
                accept();
                accept();
                skipSpaceAndComments();
            }
        }
    }

    public Token getToken() {
        currentSpelling = new StringBuffer("");
        skipSpaceAndComments();
        tokenPos = new SourcePosition(sourcePos.lineStart, sourcePos.charFinish + 1, sourcePos.charFinish);
        int kind = nextToken();
        Token tok = new Token(kind, currentSpelling.toString(), tokenPos);

        // * do not remove these three lines
        if (debug)
            System.out.println(tok);
        return tok;
    }
}
