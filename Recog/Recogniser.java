/***
 ***
 *** Recogniser.java            
 *** 13/2/2024
 *** 
 ***/

/* At this stage, this parser accepts a subset of VC defined	by
 * the following grammar. 
 *
 * You need to modify the supplied parsing methods (if necessary) and 
 * add the missing ones to obtain a parser for the VC language.
 *

 

// program changed
	program                 ->  ( func-decl | var-decl )*
		program             -> (newfunc)*
		newfunc             -> type identifier (func-decl|var-decl)
		
// declarations changed
	func-decl               -> type identifier para-list compound-stmt
		func-decl           -> para-list compound-stmt
			
	var-decl                -> type init-declarator-list ";"
		var-decl            -> init-declarator-list ";"
		
	init-declarator-list    -> init-declarator ( "," init-declarator )*
		init-declarator-list-> entry-declarator ( "," init-declarator )*
		
	init-declarator         -> declarator ( "=" initialiser )? 
	entry-declarator        -> start-declarator ( "=" initialiser )? 

		
	declarator              -> identifier |  identifier "[" INTLITERAL? "]"
		declarator         -> identifier ("[" INTLITERAL? "]")?
		start-declarator   -> ("[" INTLITERAL? "]")?
		
	initialiser             -> expr |  "{" expr ( "," expr )* "}"

// primitive types 
	type                    -> void | boolean | int | float

// identifiers 
	identifier              -> ID 

// statements no change
	compound-stmt           -> "{" var-decl* stmt* "}" 
	stmt                    -> compound-stmt
							|  if-stmt 
							|  for-stmt
							|  while-stmt 
							|  break-stmt
							|  continue-stmt
							|  return-stmt
							|  expr-stmt
	if-stmt                 -> if "(" expr ")" stmt ( else stmt )?
	for-stmt                -> for "(" expr? ";" expr? ";" expr? ")" stmt
	while-stmt              -> while "(" expr ")" stmt
	break-stmt              -> break ";"
	continue-stmt           -> continue ";"
	return-stmt             -> return expr? ";"
	expr-stmt               -> expr? ";"
    
// expressions left recursion mostly
	expr                    -> assignment-expr
	assignment-expr         -> ( cond-or-expr "=" )* cond-or-expr
		assignment-expr  -> cond-or-expr ("=" cond-or-expr)*

	cond-or-expr            -> cond-and-expr 
							|  cond-or-expr "||" cond-and-expr
		cond-or-expr        -> cond-and-expr ("||" cond-and-expr)*

	cond-and-expr           -> equality-expr 
							|  cond-and-expr "&&" equality-expr
		cond-and-expr       -> equality-expr ("&&" equality-expr)*

	equality-expr           -> rel-expr
							|  equality-expr "==" rel-expr
							|  equality-expr "!=" rel-expr
		equality-expr       -> rel-expr ("==" rel-expr| "!=" rel-expr)*

	rel-expr                -> additive-expr
							|  rel-expr "<" additive-expr
							|  rel-expr "<=" additive-expr
							|  rel-expr ">" additive-expr
							|  rel-expr ">=" additive-expr
		rel-expr            -> additive-expr ("<" additive-expr | "<=" additive-expr 
							| ">" additive-expr | ">=" additive-expr)*

	additive-expr           -> multiplicative-expr
							|  additive-expr "+" multiplicative-expr
							|  additive-expr "-" multiplicative-expr
		additive-expr           -> multiplicative-expr("-" multiplicative-expr|"+" multiplicative-expr)*

	multiplicative-expr     -> unary-expr
							|  multiplicative-expr "*" unary-expr
							|  multiplicative-expr "/" unary-expr
		multiplicative-expr     -> unary-expr("*" unary-expr|"/" unary-expr)*
					
	unary-expr              -> "+" unary-expr|  "-" unary-expr|  "!" unary-expr |  primary-expr

	primary-expr            -> identifier arg-list?
							| identifier "[" expr "]"
							| "(" expr ")"
							| INTLITERAL
							| FLOATLITERAL
							| BOOLLITERAL
							| STRINGLITERAL
		primary-expr        -> identifier (arg-list?|"[" expr "]")
							| "(" expr ")"
							| INTLITERAL
							| FLOATLITERAL
							| BOOLLITERAL
							| STRINGLITERAL

// parameters no change
para-list           -> "(" proper-para-list? ")"
proper-para-list    -> para-decl ( "," para-decl )*
para-decl           -> type declarator
arg-list            -> "(" proper-arg-list? ")"
proper-arg-list     -> arg ( "," arg )*
arg                 -> expr
*/

package VC.Recogniser;

import VC.Scanner.Scanner;
import VC.Scanner.SourcePosition;
import VC.Scanner.Token;
import VC.ErrorReporter;

public class Recogniser {

	private Scanner scanner;
	private ErrorReporter errorReporter;
	private Token currentToken;

	public Recogniser(Scanner lexer, ErrorReporter reporter) {
		scanner = lexer;
		errorReporter = reporter;

		currentToken = scanner.getToken();
	}

	// match checks to see f the current token matches tokenExpected.
	// If so, fetches the next token.
	// If not, reports a syntactic error.

	void match(int tokenExpected) throws SyntaxError {
		printme("match");
		if (currentToken.kind == tokenExpected) {
			currentToken = scanner.getToken();
		} else {
			//System.out.println("we got this '"+ currentToken.spelling + "'' with kind "+ currentToken.kind);
			syntacticError("\"%\" expected here", Token.spell(tokenExpected));
			
		}
	}

	// accepts the current token and fetches the next
	void accept() {
		printme("accepted");
		currentToken = scanner.getToken();
	}

	void syntacticError(String messageTemplate, String tokenQuoted) throws SyntaxError {
		SourcePosition pos = currentToken.position;
		errorReporter.reportError(messageTemplate, tokenQuoted, pos);
		throw (new SyntaxError());
	}

	void printme(String str) {
		// System.out.println(str + " " +" " + currentToken.spelling);
		// System.out.println(currentToken.kind);
	
	}

	// ========================== PROGRAMS ========================

	//has no left recursion
	//transformation with choice op (delay choice)
	public void parseProgram() {

		try 
		{
			while(currentToken.kind != Token.EOF){
				printme("program");
				parseFuncType();
			}
			if (currentToken.kind != Token.EOF) {
				syntacticError("\"%\" wroaang result type for a function", currentToken.spelling);
			}
		} catch (SyntaxError s) {
		}
	}

	// ========================== DECLA(RATIONS ========================

	void parseFuncType() throws SyntaxError {
		parseType(); 
		parseIdentifier();
		if (currentToken.kind == Token.LPAREN) {
			parseFuncDecl();
		} else {
			EParseVarDecl();
		}
	}

	void parseFuncDecl() throws SyntaxError {
		printme("parseFuncDecl");
		parseParaList();
		parseCompoundStmt();
	}

	void EParseVarDecl() throws SyntaxError {
		printme("EParseVarDecl");
		EparseInitDeclaratorList();
		match(Token.SEMICOLON);
	}

	void EparseInitDeclaratorList() throws SyntaxError {
		printme("EparseInitDeclaratorList");
		EparseInitDeclarator();
		while (currentToken.kind == Token.COMMA) {
			accept();
			parseInitDeclarator();
		}
	}

	void EparseInitDeclarator() throws SyntaxError {
		printme("EparseInitDeclarator");
		EparseDeclarator();
		if (currentToken.kind == Token.EQ) {
			accept();
			parseInitialiser();
		}
	}

	void EparseDeclarator() throws SyntaxError {
		printme("EparseDeclarator");
		if (currentToken.kind == Token.LBRACKET) {
			accept();
			if (currentToken.kind == Token.INTLITERAL) {
				accept();
			}
			match(Token.RBRACKET);
		}
	}

	void parseVarDecl() throws SyntaxError {
		printme("parseVarDecl");
		parseType();
		parseInitDeclaratorList();
		match(Token.SEMICOLON);
	}

	void parseInitDeclaratorList() throws SyntaxError {
		printme("parseInitDeclaratorList");
		parseInitDeclarator();
		while (currentToken.kind == Token.COMMA) {
			accept();
			parseInitDeclarator();
		}
	}

	void parseInitDeclarator() throws SyntaxError {
		printme("parseInitDeclarator");
		parseDeclarator();
		if (currentToken.kind == Token.EQ) {
			accept();
			parseInitialiser();
		}
	}

	void parseDeclarator() throws SyntaxError {
		printme("parseDeclarator");
		parseIdentifier();
		if (currentToken.kind == Token.LBRACKET) {
			accept();
			if (currentToken.kind == Token.INTLITERAL) {
				accept();
			}
			match(Token.RBRACKET);
		}
		
	}

	void parseInitialiser() throws SyntaxError {
		printme("parseInitialiser");
		if (currentToken.kind == Token.LCURLY) {
			accept();
			parseExpr();
			while (currentToken.kind == Token.COMMA) {
				accept();
				parseExpr();
			}
			match(Token.RCURLY);
		} else {
			parseExpr();
		}
	}

	// ======================= PARAMETERS ==============================

	void parseParaList() throws SyntaxError {
		printme("parseParaList");
		match(Token.LPAREN);
		if (currentToken.kind != Token.RPAREN) {
			parseProperParaList();
		}
		match(Token.RPAREN);
	}

	void parseProperParaList() throws SyntaxError {
		printme("parseProperParaList");
		parseParaDecl();
		while (currentToken.kind == Token.COMMA) {
			accept();
			parseParaDecl();
		}
	}

	void parseParaDecl() throws SyntaxError {
		printme("parseParaDecl");
		parseType();
		parseDeclarator();
	}

	void parseArgList() throws SyntaxError {
		printme("parseArgList");
		match(Token.LPAREN);
		if (currentToken.kind != Token.RPAREN) {
			parseProperArgList();
		}
		match(Token.RPAREN);
	}

	void parseProperArgList() throws SyntaxError {
		printme("parseProperArgList");
		parseArg();
		while (currentToken.kind == Token.COMMA) {
			accept();
			parseArg();
		}
	}

	void parseArg() throws SyntaxError {
		printme("parseArg");
		parseExpr();
	}

	// ======================= STATEMENTS ==============================

	void parseCompoundStmt() throws SyntaxError {
		printme("parseCompoundStmt");
		match(Token.LCURLY);
		if (currentToken.kind != Token.RCURLY) {
			while (currentToken.kind == Token.VOID || currentToken.kind == Token.BOOLEAN || currentToken.kind == Token.INT
					|| currentToken.kind == Token.FLOAT) {
				parseVarDecl();
			}
			parseStmtList();
		}
		match(Token.RCURLY);
	}

	void parseStmtList() throws SyntaxError {
		printme("parseStmtList");
		while (currentToken.kind != Token.RCURLY)
			parseStmt();
	}

	void parseStmt() throws SyntaxError {
		printme("parseStmt");
		switch (currentToken.kind) {
			case Token.LCURLY:
				parseCompoundStmt();
				break;
			case Token.IF:
				parseIfStmt();
				break;
			case Token.FOR:
				parseForStmt();
				break;
			case Token.WHILE:
				parseWhileStmt();
				break;
			case Token.BREAK:
				parseBreakStmt();
				break;
			case Token.CONTINUE:
				parseContinueStmt();
				break;
			case Token.RETURN:
				parseReturnStmt();
				break;
			default:
				parseExprStmt();
				break;
		}
	}

	void parseIfStmt() throws SyntaxError {
		match(Token.IF);
		match(Token.LPAREN);
		parseExpr();
		match(Token.RPAREN);
		parseStmt();
		if (currentToken.kind == Token.ELSE) {
			accept();
			parseStmt();
		}
	}

	void parseForStmt() throws SyntaxError {
		match(Token.FOR);
		match(Token.LPAREN);
		if (currentToken.kind != Token.SEMICOLON) {
			parseExpr();
		}
		match(Token.SEMICOLON);
		if (currentToken.kind != Token.SEMICOLON) {
			parseExpr();
		}
		match(Token.SEMICOLON);
		if (currentToken.kind != Token.RPAREN) {
			parseExpr();
		}
		match(Token.RPAREN);
		parseStmt();
	}

	void parseWhileStmt() throws SyntaxError {
		match(Token.WHILE);
		match(Token.LPAREN);
		parseExpr();
		match(Token.RPAREN);
		parseStmt();
	}

	void parseBreakStmt() throws SyntaxError {
		match(Token.BREAK);
		match(Token.SEMICOLON);
	}

	void parseContinueStmt() throws SyntaxError {
		match(Token.CONTINUE);
		match(Token.SEMICOLON);
	}

	void parseReturnStmt() throws SyntaxError {
		match(Token.RETURN);
		if (currentToken.kind != Token.SEMICOLON) {
			parseExpr();
		}
		match(Token.SEMICOLON);
	}

	void parseExprStmt() throws SyntaxError {
		if (currentToken.kind != Token.SEMICOLON) {
			parseExpr();
		}
		match(Token.SEMICOLON);
	}

	// ======================= IDENTIFIERS+PRIM ======================

	// Call parseIdent rather than match(Token.ID).
	// In Assignment 3, an Identifier node will be constructed in here.

	void parseIdentifier() throws SyntaxError {
		printme("parseIdentifier");
		if (currentToken.kind == Token.ID) {
			accept();
		} else
			syntacticError("identifier expected here", "");
	}

	void parseType() throws SyntaxError {
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
			syntacticError("\"%\" wrong result type for a function", currentToken.spelling);
		}
	
	}

	// ======================= OPERATORS ======================

	// Call acceptOperator rather than accept().
	// In Assignment 3, an Operator Node will be constructed in here.

	void acceptOperator() throws SyntaxError {
		printme("acceptOperator");
		currentToken = scanner.getToken();
	}

	
	// ======================= EXPRESSIONS ======================

	void parseExpr() throws SyntaxError {
		printme("parseExpr");
		parseAssignExpr();
	}

	void parseAssignExpr() throws SyntaxError {
		printme("parseAssignExpr");
		parseCondOrExpr();
		while (currentToken.kind == Token.EQ) {
			acceptOperator();
			parseCondOrExpr();
		}
	}

	void parseCondOrExpr() throws SyntaxError {
		printme("parseCondOrExpr");
		parseCondAndExpr();
		while (currentToken.kind == Token.OROR) {
			acceptOperator();
			parseCondAndExpr();
		}
	}

	void parseCondAndExpr() throws SyntaxError {
		printme("parseCondAndExpr");
		parseEqualityExpr();
		while (currentToken.kind == Token.ANDAND) {
			acceptOperator();
			parseEqualityExpr();
		}
	}

	void parseEqualityExpr() throws SyntaxError {
		printme("parseEqualityExpr");
		parseRelExpr();
		while (currentToken.kind == Token.EQEQ || currentToken.kind == Token.NOTEQ) {
			acceptOperator();
			parseRelExpr();
		}
	}

	void parseRelExpr() throws SyntaxError {
		printme("parseRelExpr");
		parseAdditiveExpr();
		while (currentToken.kind == Token.LT || currentToken.kind == Token.LTEQ || currentToken.kind == Token.GT
				|| currentToken.kind == Token.GTEQ) {
			acceptOperator();
			parseAdditiveExpr();
		}
	}

	void parseAdditiveExpr() throws SyntaxError {
		printme("parseAdditiveExpr");
		parseMultiplicativeExpr();
		while (currentToken.kind == Token.PLUS || currentToken.kind == Token.MINUS) {
			acceptOperator();
			parseMultiplicativeExpr();
		}
	}

	void parseMultiplicativeExpr() throws SyntaxError {
		printme("parseMultiplicativeExpr");
		parseUnaryExpr();
		while (currentToken.kind == Token.MULT || currentToken.kind == Token.DIV) {
			acceptOperator();
			parseUnaryExpr();
		}
	}

	void parseUnaryExpr() throws SyntaxError {
		printme("parseUnaryExpr");
		switch (currentToken.kind) {
			case Token.MINUS: 
				acceptOperator();
				parseUnaryExpr();
				break;
			case Token.PLUS:
				acceptOperator();
				parseUnaryExpr();
				break;
			case Token.NOT:
				acceptOperator();
				parseUnaryExpr();
				break;
			default:
				parsePrimaryExpr();
				break;
		}
	}

	void parsePrimaryExpr() throws SyntaxError {
		printme("parsePrimaryExpr");
		switch (currentToken.kind) {
			case Token.ID:
				parseIdentifier();
				if (currentToken.kind == Token.LBRACKET) {
					accept();
					parseExpr();
					match(Token.RBRACKET);
				} else if (currentToken.kind == Token.LPAREN) {
					parseArgList();
				}
				break;

			case Token.LPAREN:
				accept();
				parseExpr();
				match(Token.RPAREN);
				break;
			case Token.INTLITERAL:
				parseIntLiteral();
				break;
			case Token.FLOATLITERAL:
				parseFloatLiteral();
				break;
			case Token.BOOLEANLITERAL:
				parseBooleanLiteral();
				break;
			case Token.STRINGLITERAL:
				parseStringLiteral();
				break;

			default:
				syntacticError("illegal parimary expression", currentToken.spelling);

		}
	}

	// ========================== LITERALS ========================

	// Call these methods rather than accept(). In Assignment 3,
	// literal AST nodes will be constructed inside these methods.

	void parseIntLiteral() throws SyntaxError {

		if (currentToken.kind == Token.INTLITERAL) {
			accept();
		} else
			syntacticError("integer literal expected here", "");
	}

	void parseFloatLiteral() throws SyntaxError {

		if (currentToken.kind == Token.FLOATLITERAL) {
			accept();
		} else
			syntacticError("float literal expected here", "");
	}

	void parseBooleanLiteral() throws SyntaxError {

		if (currentToken.kind == Token.BOOLEANLITERAL) {
			accept();
		} else
			syntacticError("boolean literal expected here", "");
	}

	void parseStringLiteral() throws SyntaxError {

		if (currentToken.kind == Token.STRINGLITERAL) {
			accept();
		} else
			syntacticError("string literal expected here", "");
	}

}
