//  +--------------+
//  + Parser.java  +
//  +--------------+
//  *
//  * PLEASE COMPARE Recogniser.java PROVIDED IN ASSIGNMENT 2 AND Parser.java
//  * PROVIDED BELOW TO UNDERSTAND HOW THE FORMER IS MODIFIED TO OBTAIN THE LATTER.
//  *
//  * This parser for a subset of the VC language is intended to 
//  *  demonstrate how to create the AST nodes, including (among others): 
//  *  (1) a list (of statements)
//  *  (2) a function
//  *  (3) a statement (which is an expression statement), 
//  *  (4) a unary expression
//  *  (5) a binary expression
//  *  (6) terminals (identifiers, integer literals and operators)
//  *
//  * In addition, it also demonstrates how to use the two methods start 
//  * and finish to determine the position information for the start and 
//  * end of a construct (known as a phrase) corresponding an AST node.
//  *
//  * NOTE THAT THE POSITION INFORMATION WILL NOT BE MARKED. HOWEVER, IT CAN BE
//  * USEFUL TO DEBUG YOUR IMPLEMENTATION.
//  *
//  *
//  * --- 24/2/2024 --- 

// program       -> func-decl
// func-decl     -> type identifier "(" ")" compound-stmt
// type          -> void
// identifier    -> ID
// // statements
// compound-stmt -> "{" stmt* "}" 
// stmt          -> expr-stmt
// expr-stmt     -> expr? ";"
// // expressions 
// expr                -> additive-expr
// additive-expr       -> multiplicative-expr
//                     |  additive-expr "+" multiplicative-expr
//                     |  additive-expr "-" multiplicative-expr
// multiplicative-expr -> unary-expr
// 	            |  multiplicative-expr "*" unary-expr
// 	            |  multiplicative-expr "/" unary-expr
// unary-expr          -> "-" unary-expr
// 		    |  primary-expr

// primary-expr        -> identifier
//  		    |  INTLITERAL
// 		    | "(" expr ")"

package VC.Parser;

import VC.Scanner.Scanner;
import VC.Scanner.SourcePosition;
import VC.Scanner.Token;

import VC.ASTs.List;

import VC.ASTs.ArrayType;

import VC.ErrorReporter;
import VC.ASTs.ArrayExpr;
import VC.ASTs.*;

public class Parser {

	private Scanner scanner;
	private ErrorReporter errorReporter;
	private Token currentToken;
	private SourcePosition previousTokenPosition;
	private SourcePosition dummyPos = new SourcePosition();
	private boolean debug = true;

	public Parser(Scanner lexer, ErrorReporter reporter) {
		scanner = lexer;
		errorReporter = reporter;

		previousTokenPosition = new SourcePosition();

		currentToken = scanner.getToken();
	}

	// match checks to see f the current token matches tokenExpected.
	// If so, fetches the next token.
	// If not, reports a syntactic error.

	void match(int tokenExpected) throws SyntaxError {

		if (debug) {
			//System.out.println("Matched " + currentToken.spelling + " with " + Token.spell(tokenExpected));
		}

		if (currentToken.kind == tokenExpected) {
			previousTokenPosition = currentToken.position;
			currentToken = scanner.getToken();
		} else {
			syntacticError("\"%\" expected here", Token.spell(tokenExpected));
		}
	}

	void accept() {
		if (debug)
			//System.out.println("accept " + currentToken.spelling);
		previousTokenPosition = currentToken.position;
		currentToken = scanner.getToken();
	}

	void syntacticError(String messageTemplate, String tokenQuoted) throws SyntaxError {
		SourcePosition pos = currentToken.position;
		errorReporter.reportError(messageTemplate, tokenQuoted, pos);
		throw (new SyntaxError());
	}

	// start records the position of the start of a phrase.
	// This is defined to be the position of the first
	// character of the first token of the phrase.

	void start(SourcePosition position) {
		position.lineStart = currentToken.position.lineStart;
		position.charStart = currentToken.position.charStart;
	}

	// finish records the position of the end of a phrase.
	// This is defined to be the position of the last
	// character of the last token of the phrase.

	void finish(SourcePosition position) {
		position.lineFinish = previousTokenPosition.lineFinish;
		position.charFinish = previousTokenPosition.charFinish;
	}

	void copyStart(SourcePosition from, SourcePosition to) {
		to.lineStart = from.lineStart;
		to.charStart = from.charStart;
	}

	// ========================== PROGRAMS ========================

	public Program parseProgram() {

		Program programAST = null;

		SourcePosition programPos = new SourcePosition();
		start(programPos);

		try {
			List dlAST = parseDeclList();
			finish(programPos);
			programAST = new Program(dlAST, programPos);
			if (currentToken.kind != Token.EOF) {
				syntacticError("\"%\" unknown type", currentToken.spelling);
			}
		} catch (SyntaxError s) {
			return null;
		}
		return programAST;
	}

	// ========================== DECLARATIONS ========================

	//rewrite this for sure its a recipe for bugs
	
	DeclList child(DeclList list) throws SyntaxError {
		while (!(list.DL instanceof EmptyDeclList)) {
			list = ((DeclList) list.DL);
		}
		return list; 
	}

	List parseDeclList() throws SyntaxError {
		//System.out.println(("parseDeclList: current token: " + currentToken.spelling);		
		SourcePosition declPos = new SourcePosition();
		start(declPos);

		if (currentToken.kind == Token.EOF) {
			return new EmptyDeclList(dummyPos);
		}
		DeclList node = parseFuncType();
		if (currentToken.kind != Token.EOF) {
			DeclList loop = child(node); 
			loop.DL = parseDeclList();
		}
		return node;
	}

	DeclList parseFuncType() throws SyntaxError {
		SourcePosition funcPos = new SourcePosition();
		start(funcPos);

		Type tAST = parseType();
		//System.out.println(("parseFuncType: current token: " + currentToken.spelling);
		Ident iAST = parseIdent();
		//System.out.println(("parseFuncType: current token: " + currentToken.spelling);
		DeclList dlAST = null; 
		
		if (currentToken.kind == Token.LPAREN) {
			dlAST = parseFuncDecl(tAST, iAST, funcPos);
			finish(funcPos);
		} else {
			dlAST = EParseVarDecl(tAST, iAST, funcPos);
			//System.out.println(("print decllist: " + dlAST.D + " " + dlAST.DL);
			finish(funcPos);
		}
		return dlAST;
	}

	DeclList parseFuncDecl(Type tAST, Ident iAST, SourcePosition funcPos) throws SyntaxError {
		
		Decl fAST = null;
		List fplAST = parseParaList();
		//System.out.println("parseFuncDecl: current token: " + currentToken.spelling);
		Stmt cAST = parseCompoundStmt();
		fAST = new FuncDecl(tAST, iAST, fplAST, cAST, funcPos);
		finish(funcPos);
		DeclList dlAST = new DeclList(fAST, new EmptyDeclList(dummyPos), funcPos);
		return dlAST;
	}

	DeclList EParseVarDecl(Type tAST, Ident iAST, SourcePosition funcPos) throws SyntaxError {
		//combine all the estatments why did i even split it in the first place :I
		//System.out.println("hi");
		DeclList declList = null;
		
		SourcePosition declPos = new SourcePosition();
		List dlAST = new EmptyDeclList(dummyPos);
		start(declPos);
		Expr eAST = new EmptyExpr(dummyPos); 
		
		if (currentToken.kind == Token.LBRACKET) {
			match(Token.LBRACKET);
			if (currentToken.kind == Token.INTLITERAL) {
				IntLiteral ilAST = parseIntLiteral();
				eAST = new IntExpr(ilAST, funcPos);
			}
			match(Token.RBRACKET);
			finish (funcPos);
			//maybe wrong
			tAST = new ArrayType(tAST, eAST, funcPos);
		}
		if (currentToken.kind == Token.EQ) {
			//System.out.println("eqeq");
			acceptOperator();
			// HAHAHAH NICE BUG DONT ASSIGN PARSE INITIALISER TO ANYTHING HAHAHAHHAHAHAHAHHAHAHA GOOD TROLL
			//parseInitialiser();
			eAST = parseInitialiser();
		}
		finish(declPos);

		//System.out.println(("current eparse token: " + currentToken.spelling);
		GlobalVarDecl vAST = new GlobalVarDecl(tAST, iAST, eAST, declPos);
		
		if (currentToken.kind == Token.COMMA) {
			//System.out.println(("comma found eparse");
			match(Token.COMMA);
			dlAST = parseInitDeclaratorList(tAST, true);
		}
		match (Token.SEMICOLON);
		declList = new DeclList(vAST, dlAST, funcPos);
		return declList;
	}

	DeclList parseVarDecl() throws SyntaxError{
		Type tAST = parseType();
		DeclList dlAST = parseInitDeclaratorList(tAST, false);
		match(Token.SEMICOLON);
		return dlAST;
	}

	DeclList parseInitDeclaratorList(Type tAST, boolean global) throws SyntaxError{
		//System.out.println(("parseInitDeclaratorList: current token: " + currentToken.spelling);
		SourcePosition declPos = new SourcePosition();
		DeclList dlAST = null;
		Decl dAST = null;

		start(declPos);
		dAST = parseInitDeclarator(tAST, global);
		if (currentToken.kind == Token.COMMA) {
			//System.out.println(("comma found init");
			accept();
			DeclList ddlAST = parseInitDeclaratorList(tAST, global);
			finish(declPos);
			dlAST = new DeclList(dAST, ddlAST, declPos);
		} else {
			finish(declPos);
			dlAST = new DeclList(dAST, new EmptyDeclList(dummyPos), declPos);
		}
		
		//System.out.println(("dlAST: " + dlAST.D + " " + dlAST.DL);
		// print the out fields of dlAST
		return dlAST;
	}

	Decl parseInitDeclarator(Type tAST, boolean global) throws SyntaxError {
		//System.out.println(("parseInitDeclarator: current token: " + currentToken.spelling);
		Expr eAST = new EmptyExpr(dummyPos);
		Decl dAST = parseDeclarator(tAST, global, false);

		if (currentToken.kind == Token.EQ) {
			acceptOperator();
			eAST = parseInitialiser();
		}

		if (dAST instanceof GlobalVarDecl) {
			((GlobalVarDecl) dAST).E = eAST;
		} else if (dAST instanceof LocalVarDecl) {
			((LocalVarDecl) dAST).E = eAST;
		}

		return dAST;
	}

	Decl parseDeclarator(Type tAST, boolean global, boolean para) throws SyntaxError {
		//System.out.println("parseDeclarator");
		Expr eAST = new EmptyExpr(dummyPos);
		SourcePosition tPos = new SourcePosition();
		Decl dAST = null;
		Ident iAST = parseIdent();
		start(tPos);
		if (currentToken.kind == Token.LBRACKET) {
			match(Token.LBRACKET);
			if (currentToken.kind == Token.INTLITERAL) {
				IntLiteral ilAST = parseIntLiteral();
				eAST = new IntExpr(ilAST, tPos);
			}
			match(Token.RBRACKET);
			finish(tPos);
			tAST = new ArrayType(tAST, eAST, tPos);
		}
		finish(tPos);

		if (global) {
			dAST = new GlobalVarDecl(tAST, iAST, eAST, tPos);
		} else if (para) {
			dAST = new ParaDecl(tAST, iAST, tPos);
		} else {
			dAST = new LocalVarDecl(tAST, iAST, eAST, tPos);
		}
		return dAST;
	}

	// ArrayExprList parseInitialiser() throws SyntaxError {
	// 	// ArrayExprList elAST = null;
	// 	// Expr eAST = null; 
	// 	// Expr e2AST = null;

	// 	// if (currentToken.kind == Token.LCURLY) {
	// 	// 	match(Token.LCURLY);
	// 	// 	eAST = parseExpr();
	// 	// 	elAST = new ArrayExprList(eAST, new EmptyArrayExprList(dummyPos), eAST.position);
	// 	// 	while (currentToken.kind == Token.COMMA) {
	// 	// 		match(Token.COMMA);
	// 	// 		eAST = parseExpr();
	// 	// 		elAST = new ArrayExprList(eAST, elAST, eAST.position);
	// 	// 	}
	// 	// 	match(Token.RCURLY);
	// 	// } else {
	// 	// 	eAST = parseExpr();
	// 	// 	elAST = new ArrayExprList(eAST, new EmptyArrayExprList(dummyPos), eAST.position);
	// 	// }
	// 	// return elAST;
	// 	return null;
	// }

	//todo fix this doesnt recurse properly
	Expr parseInitialiser() throws SyntaxError {
		//System.out.println("parseInitialiser");
		Expr eAST = null;
		if (currentToken.kind == Token.LCURLY) {
			match(Token.LCURLY);
			eAST = parseExpr();
			while (currentToken.kind == Token.COMMA) {
				match(Token.COMMA);
				eAST = parseExpr();
			}
			match(Token.RCURLY);
		} else {
			//System.out.println("current token: " + currentToken.spelling);
			eAST = parseExpr();
		}
		return eAST;
	}



	// ======================== TYPES ==========================

	Type parseType() throws SyntaxError {
		Type typeAST = null;
		SourcePosition typePos = new SourcePosition();
		start(typePos);

		switch (currentToken.kind) {
			case Token.VOID:
				match(Token.VOID);
				typeAST = new VoidType(typePos);
				break;
			case Token.BOOLEAN:
				match(Token.BOOLEAN);
				typeAST = new BooleanType(typePos);
				break;
			case Token.INT:
				match(Token.INT);
				typeAST = new IntType(typePos);
				break;
			case Token.FLOAT:
				match(Token.FLOAT);
				typeAST = new FloatType(typePos);
				break;
			default:
				syntacticError("\"%\" wrong result type for a function", currentToken.spelling);
		}

		finish(typePos);
		typeAST.position = typePos;
		return typeAST;
	}

	// ======================= STATEMENTS ==============================
	// + Stmt
    // = CompoundStmt(List dlAST,  List slAST)
    // = IfStmt(Expr eAST, Stmt s1AST (, Stmt s2AST)?)
    // = ForStmt(Expr e1AST, Expr e2AST, Expr e3AST, Stmt sAST)
    // = WhileStmt(Expr eAST, Stmt sAST)
    // = ExprStmt(Expr eAST)
    // = ContinueStmt()
    // = BreakStmt()
    // = ReturnStmt(Expr eAST)
    // = EmptyCompStmt()
    // = EmptyStmt()
	// + List 
	// = StmtList(Stmt sAST, List slList)

	Stmt parseCompoundStmt() throws SyntaxError {
		//System.out.println("parseCompoundStmt");
		SourcePosition stmtPos = new SourcePosition();
		Stmt cmpStmt = null;
		List dlAST = new EmptyDeclList(dummyPos);
		List slAST = new EmptyStmtList(dummyPos);
			
		start(stmtPos);
		match(Token.LCURLY);
		if (currentToken.kind != Token.RCURLY) {
			//System.out.println("hi");
			dlAST = parseVarDeclList();		
			//System.out.println("bye");
			slAST = parseStmtList();
		} else {
			//System.out.println("empty");
			match(Token.RCURLY);
			return new EmptyCompStmt(dummyPos);
		}
		match(Token.RCURLY);
		finish(stmtPos);

		cmpStmt = new CompoundStmt(dlAST, slAST, stmtPos);
		return cmpStmt;
	}

	List parseVarDeclList() throws SyntaxError {
		List dlAST = null;
		SourcePosition lPos = new SourcePosition();
		start(lPos);

		if (currentToken.kind != Token.RCURLY) {
			//System.out.println("current token: " + currentToken.spelling);
			
			{
				if (currentToken.kind == Token.VOID || currentToken.kind == Token.BOOLEAN || currentToken.kind == Token.INT
				|| currentToken.kind == Token.FLOAT) {
					Decl dAST = parseVarDecl().D;
					dlAST = parseVarDeclList();
					finish(lPos);
					dlAST = new DeclList(dAST, dlAST, lPos);
				} else {
					finish(lPos);
					dlAST = new EmptyDeclList(dummyPos);
					//dlAST = new DeclList(dAST, new EmptyDeclList(dummyPos), lPos);
				}
			}
		} else
		dlAST = new EmptyDeclList(dummyPos);

		return dlAST;
	}

	List parseStmtList() throws SyntaxError {
		List slAST = null;
		SourcePosition stmtPos = new SourcePosition();

		start(stmtPos);
		if (currentToken.kind != Token.RCURLY) {
			Stmt sAST = parseStmt();
			//System.out.println("byebye");
			{
				if (currentToken.kind != Token.RCURLY) {
					slAST = parseStmtList();
					finish(stmtPos);
					slAST = new StmtList(sAST, slAST, stmtPos);
				} else {
					finish(stmtPos);
					slAST = new StmtList(sAST, new EmptyStmtList(dummyPos), stmtPos);
				}
			}
		} else
		slAST = new EmptyStmtList(dummyPos);

		return slAST;
	}

	Stmt parseStmt() throws SyntaxError {
		Stmt sAST = null;

		switch (currentToken.kind) {
			case Token.LCURLY:
				sAST = parseCompoundStmt();
				break;
			case Token.IF:
				sAST = parseIfStmt();
				break;
			case Token.FOR:
				sAST = parseForStmt();
				break;
			case Token.WHILE:
				sAST = parseWhileStmt();
				break;
			case Token.BREAK:
				sAST = parseBreakStmt();
				break;
			case Token.CONTINUE:
				sAST = parseContinueStmt();
				break;
			case Token.RETURN:
				sAST = parseReturnStmt();
				break;
			default:
				//System.out.println("byebyebye");
				sAST = parseExprStmt();
				break;
		}
		return sAST;
	}

	Stmt parseIfStmt() throws SyntaxError {
		Stmt sAST = null;
		Expr eAST = null;
		Stmt s1AST = null;
		Stmt s2AST = null;
		SourcePosition stmtPos = new SourcePosition();

		start(stmtPos);
		match(Token.IF);
		match(Token.LPAREN);
		eAST = parseExpr();
		match(Token.RPAREN);
		s1AST = parseStmt();
		if (currentToken.kind == Token.ELSE) {
			match(Token.ELSE);
			s2AST = parseStmt();
			finish(stmtPos);
			sAST = new IfStmt(eAST, s1AST, s2AST, stmtPos);
		} else {
			finish(stmtPos);
			sAST = new IfStmt(eAST, s1AST, stmtPos);
		}
		return sAST;
	}

	Stmt parseForStmt() throws SyntaxError {
		Stmt sAST = null;
		Expr e1AST = new EmptyExpr(dummyPos);
		Expr e2AST = new EmptyExpr(dummyPos);
		Expr e3AST = new EmptyExpr(dummyPos);
		Stmt s1AST = null;
		SourcePosition stmtPos = new SourcePosition();

		start(stmtPos);
		match(Token.FOR);
		match(Token.LPAREN);
		if (currentToken.kind != Token.SEMICOLON) {
			e1AST = parseExpr();
		}
		match(Token.SEMICOLON);
		if (currentToken.kind != Token.SEMICOLON) {
			e2AST = parseExpr();
		}
		match(Token.SEMICOLON);
		if (currentToken.kind != Token.RPAREN) {
			e3AST = parseExpr();
		}
		match(Token.RPAREN);
		s1AST = parseStmt();
		finish(stmtPos);
		sAST = new ForStmt(e1AST, e2AST, e3AST, s1AST, stmtPos);
		return sAST;
	}

	Stmt parseWhileStmt() throws SyntaxError {
		Stmt sAST = null;
		Expr eAST = null;
		Stmt s1AST = null;
		SourcePosition stmtPos = new SourcePosition();

		start(stmtPos);
		match(Token.WHILE);
		match(Token.LPAREN);
		eAST = parseExpr();
		match(Token.RPAREN);
		s1AST = parseStmt();
		finish(stmtPos);
		sAST = new WhileStmt(eAST, s1AST, stmtPos);
		return sAST;
	}

	Stmt parseBreakStmt() throws SyntaxError {
		Stmt sAST = null;
		SourcePosition stmtPos = new SourcePosition();

		start(stmtPos);
		match(Token.BREAK);
		match(Token.SEMICOLON);
		finish(stmtPos);
		sAST = new BreakStmt(stmtPos);
		return sAST;
	}

	Stmt parseContinueStmt() throws SyntaxError {
		Stmt sAST = null;
		SourcePosition stmtPos = new SourcePosition();

		start(stmtPos);
		match(Token.CONTINUE);
		match(Token.SEMICOLON);
		finish(stmtPos);
		sAST = new ContinueStmt(stmtPos);
		return sAST;
	}

	Stmt parseReturnStmt() throws SyntaxError {
		Stmt sAST = null;
		Expr eAST = new EmptyExpr(dummyPos);
		SourcePosition stmtPos = new SourcePosition();

		start(stmtPos);
		match(Token.RETURN);
		if (currentToken.kind != Token.SEMICOLON) {
			eAST = parseExpr();
		}
		match(Token.SEMICOLON);
		finish(stmtPos);
		sAST = new ReturnStmt(eAST, stmtPos);
		return sAST;
	}

	Stmt parseExprStmt() throws SyntaxError {
		Stmt sAST = null;
		SourcePosition stmtPos = new SourcePosition();

		start(stmtPos);
		if (currentToken.kind != Token.SEMICOLON) {
			Expr eAST = parseExpr();
			match(Token.SEMICOLON);
			finish(stmtPos);
			sAST = new ExprStmt(eAST, stmtPos);
		} else {
			match(Token.SEMICOLON);
			finish(stmtPos);
			sAST = new ExprStmt(new EmptyExpr(dummyPos), stmtPos);
		}
		return sAST;
	}

	// ======================= PARAMETERS =======================
	// + List 
	// = ParaList(ParaDecl pAST, List plList)
    // = EmptyParaList()
	// = ArgList(Arg aAST, List alList)
	// = EmptyArgList()
	// + Decl
	// = ParaDecl(Type tAST, Ident idAST)
	// + Expr 
	// = Arg(Expr eAST)

	List parseParaList() throws SyntaxError {
		List plAST = new EmptyParaList(dummyPos);
		SourcePosition paraPos = new SourcePosition();
		
		start(paraPos);
		match(Token.LPAREN);
		if (currentToken.kind != Token.RPAREN) {
			plAST = parseProperParaList();
		}
		match(Token.RPAREN);
		finish(paraPos);

		return plAST;
	}

	//copy parsestmtlist structure shd be almost same
	// pretty sure can write not recursive but idk why stmtlist did it like this
	// so lets just follow

	// List parseStmtList() throws SyntaxError {
	// 	List slAST = null;
	// 	SourcePosition stmtPos = new SourcePosition();

	// 	start(stmtPos);
	// 	if (currentToken.kind != Token.RCURLY) {
	// 		Stmt sAST = parseStmt();
	// 		{
	// 			if (currentToken.kind != Token.RCURLY) {
	// 				slAST = parseStmtList();
	// 				finish(stmtPos);
	// 				slAST = new StmtList(sAST, slAST, stmtPos);
	// 			} else {
	// 				finish(stmtPos);
	// 				slAST = new StmtList(sAST, new EmptyStmtList(dummyPos), stmtPos);
	// 			}
	// 		}
	// 	} else
	// 	slAST = new EmptyStmtList(dummyPos);

	// 	return slAST;
	// }

	List parseProperParaList() throws SyntaxError {
		List plAST = null;
		SourcePosition paraPos = new SourcePosition();

		start(paraPos);
		ParaDecl pAST = parseParaDecl();
		if (currentToken.kind == Token.COMMA) {
			{
				//might be wrong pos here
				match(Token.COMMA);
				List tmp = parseProperParaList();
				finish(paraPos);
				plAST = new ParaList(pAST, tmp, paraPos);
			}
		} else {
			finish(paraPos);
			plAST = new ParaList(pAST, new EmptyParaList(dummyPos), paraPos);
		}
		return plAST;
	}

	ParaDecl parseParaDecl() throws SyntaxError {
		SourcePosition paraPos = new SourcePosition();
		
		start(paraPos);
		Type tAST = parseType();
		//casting >>______________>>
		ParaDecl pAST = ((ParaDecl)parseDeclarator(tAST, false, true));
		finish(paraPos);

		return pAST;
	}

	List parseArgList() throws SyntaxError {
		List alAST = new EmptyArgList(dummyPos);
		SourcePosition argPos = new SourcePosition();

		start(argPos);
		match(Token.LPAREN);
		if (currentToken.kind != Token.RPAREN) {
			alAST = parseProperArgList();
		}
		match(Token.RPAREN);
		finish(argPos);

		return alAST;
	}

	//copy above structure
	List parseProperArgList() throws SyntaxError {
		List alAST = null;
		SourcePosition argPos = new SourcePosition();

		start(argPos);
		Arg aAST = parseArg();
		if (currentToken.kind == Token.COMMA) {
			{
				match(Token.COMMA);
				List tmp = parseProperArgList();
				finish(argPos);
				alAST = new ArgList(aAST, tmp, argPos);
			}
		} else {
			finish(argPos);
			alAST = new ArgList(aAST, new EmptyArgList(dummyPos), argPos);
		}
		return alAST;
	}

	Arg parseArg() throws SyntaxError {
		Arg aAST = null;
		SourcePosition argPos = new SourcePosition();

		start(argPos);
		Expr eAST = parseExpr();
		finish(argPos);
		aAST = new Arg(eAST, argPos);
		return aAST;
	}

	// ======================= EXPRESSIONS ======================

	Expr parseExpr() throws SyntaxError {
		//System.out.println("parseexpr");
		Expr exprAST = null;
		exprAST = parseAssignExpr();
		return exprAST;
	}

	Expr parseAssignExpr() throws SyntaxError { 
		//System.out.println("parseassignexpr");
		SourcePosition exprPos = new SourcePosition();
		start(exprPos);
		Expr eAST =  parseCondOrExpr(); 
		if (currentToken.kind == Token.EQ) {
			acceptOperator();
			Expr e2AST = parseAssignExpr();
			finish(exprPos);
			eAST = new AssignExpr(eAST, e2AST, exprPos);

		}
		finish(exprPos);
		return eAST;
	}

	Expr parseCondOrExpr() throws SyntaxError {
		//System.out.println("parsecondorexpr");
		SourcePosition exprPos = new SourcePosition();
		start(exprPos);
		Expr eAST = parseCondAndExpr();
		while (currentToken.kind == Token.OROR) {
			Operator opAST = acceptOperator();
			Expr e2AST = parseCondAndExpr();
			finish(exprPos);
			eAST = new BinaryExpr(eAST, opAST, e2AST, exprPos);
		}
		finish(exprPos);
		return eAST;
	}

	Expr parseCondAndExpr() throws SyntaxError {
		//System.out.println("parsecondandexpr");
		SourcePosition exprPos = new SourcePosition();
		start(exprPos);
		Expr eAST = parseEqualityExpr();
		while (currentToken.kind == Token.ANDAND) {
			Operator opAST = acceptOperator();
			Expr e2AST = parseEqualityExpr();
			finish(exprPos);
			eAST = new BinaryExpr(eAST, opAST, e2AST, exprPos);
		}
		finish(exprPos);
		return eAST;
	}

	Expr parseEqualityExpr() throws SyntaxError {
		//System.out.println("parseequalityexpr");
		SourcePosition exprPos = new SourcePosition();
		start(exprPos);
		Expr eAST = parseRelExpr();
		while (currentToken.kind == Token.EQEQ || currentToken.kind == Token.NOTEQ) {
			Operator opAST = acceptOperator();
			Expr e2AST = parseRelExpr();
			finish(exprPos);
			eAST = new BinaryExpr(eAST, opAST, e2AST, exprPos);
		}
		finish(exprPos);
		return eAST;
	}

	Expr parseRelExpr() throws SyntaxError {
		//System.out.println("parseRelExpr");
		SourcePosition exprPos = new SourcePosition();
		start(exprPos);
		Expr eAST = parseAdditiveExpr();
		while (currentToken.kind == Token.LT || currentToken.kind == Token.LTEQ
				|| currentToken.kind == Token.GT || currentToken.kind == Token.GTEQ) {
			Operator opAST = acceptOperator();
			Expr e2AST = parseAdditiveExpr();
			finish(exprPos);
			eAST = new BinaryExpr(eAST, opAST, e2AST, exprPos);
		}
		finish(exprPos);
		return eAST;
	}

	

	Expr parseAdditiveExpr() throws SyntaxError {
		//System.out.println("parseAdditiveExpr");
		Expr exprAST = null;

		SourcePosition addStartPos = new SourcePosition();
		start(addStartPos);

		exprAST = parseMultiplicativeExpr();
		while (currentToken.kind == Token.PLUS
				|| currentToken.kind == Token.MINUS) {
			Operator opAST = acceptOperator();
			Expr e2AST = parseMultiplicativeExpr();

			SourcePosition addPos = new SourcePosition();
			copyStart(addStartPos, addPos);
			finish(addPos);
			exprAST = new BinaryExpr(exprAST, opAST, e2AST, addPos);
		}
		return exprAST;
	}

	Expr parseMultiplicativeExpr() throws SyntaxError {
		//System.out.println("parseMultiplicativeExpr");
		Expr exprAST = null;

		SourcePosition multStartPos = new SourcePosition();
		start(multStartPos);

		exprAST = parseUnaryExpr();
		while (currentToken.kind == Token.MULT
				|| currentToken.kind == Token.DIV) {
			Operator opAST = acceptOperator();
			Expr e2AST = parseUnaryExpr();
			SourcePosition multPos = new SourcePosition();
			copyStart(multStartPos, multPos);
			finish(multPos);
			exprAST = new BinaryExpr(exprAST, opAST, e2AST, multPos);
		}
		return exprAST;
	}

	Expr parseUnaryExpr() throws SyntaxError {
		//System.out.println("parseUnaryExpr");
		Expr exprAST = null;

		SourcePosition unaryPos = new SourcePosition();
		start(unaryPos);

		switch (currentToken.kind) {
			case Token.MINUS: {
				Operator opAST = acceptOperator();
				Expr e2AST = parseUnaryExpr();
				finish(unaryPos);
				exprAST = new UnaryExpr(opAST, e2AST, unaryPos);
				break;
			}
			case Token.PLUS: {
				Operator opAST = acceptOperator();
				Expr e2AST = parseUnaryExpr();
				finish(unaryPos);
				exprAST = new UnaryExpr(opAST, e2AST, unaryPos);
				break;
			}
			case Token.NOT: {
				Operator opAST = acceptOperator();
				Expr e2AST = parseUnaryExpr();
				finish(unaryPos);
				exprAST = new UnaryExpr(opAST, e2AST, unaryPos);
				break;
			}
			default:
				exprAST = parsePrimaryExpr();
				break;

		}
		return exprAST;
	}

	Expr parsePrimaryExpr() throws SyntaxError {
		//System.out.println("parsePrimaryExpr");

		Expr exprAST = null;

		SourcePosition primPos = new SourcePosition();
		start(primPos);

		switch (currentToken.kind) {

			case Token.ID:
				Ident iAST = parseIdent();
				finish(primPos);
				Var simVAST = new SimpleVar(iAST, primPos);
				exprAST = new VarExpr(simVAST, primPos);
				if (currentToken.kind == Token.LBRACKET) {
					match(Token.LBRACKET);
					Expr e2AST = parseExpr();
					match(Token.RBRACKET);
					finish(primPos);
					exprAST = new ArrayExpr(simVAST, e2AST, primPos);
				} else if (currentToken.kind == Token.LPAREN) {
					List alAST = parseArgList();
					finish(primPos);
					exprAST = new CallExpr(iAST, alAST, primPos);
				}
				break;
			case Token.LPAREN: {
				accept();
				exprAST = parseExpr();
				match(Token.RPAREN);
			}
				break;
			case Token.INTLITERAL:
				IntLiteral ilAST = parseIntLiteral();
				finish(primPos);
				exprAST = new IntExpr(ilAST, primPos);
				break;
			case Token.FLOATLITERAL:
				//System.out.println("floatlit");
				FloatLiteral flAST = parseFloatLiteral();
				finish(primPos);
				exprAST = new FloatExpr(flAST, primPos);
				break;
			case Token.BOOLEANLITERAL:
				BooleanLiteral blAST = parseBooleanLiteral();
				finish(primPos);
				exprAST = new BooleanExpr(blAST, primPos);
				break;
			case Token.STRINGLITERAL:
				StringLiteral slAST = parseStringLiteral();
				finish(primPos);
				exprAST = new StringExpr(slAST, primPos);
				break;

			default:
				syntacticError("illegal primary expression", currentToken.spelling);

		}
		return exprAST;
	}

	// ========================== ID, OPERATOR and LITERALS ========================

	Ident parseIdent() throws SyntaxError {
		//System.out.println(("parseIdent: current token: " + currentToken.spelling);
		Ident I = null;

		if (currentToken.kind == Token.ID) {
			previousTokenPosition = currentToken.position;
			String spelling = currentToken.spelling;
			I = new Ident(spelling, previousTokenPosition);
			currentToken = scanner.getToken();
		} else
			syntacticError("identifier expected here", "");
		return I;
	}

	// acceptOperator parses an operator, and constructs a leaf AST for it

	Operator acceptOperator() throws SyntaxError {
		Operator O = null;

		if (debug) {
			//System.out.println("acceptOperator: " + currentToken.spelling);
		}

		previousTokenPosition = currentToken.position;
		String spelling = currentToken.spelling;
		O = new Operator(spelling, previousTokenPosition);
		currentToken = scanner.getToken();
		return O;
	}

	IntLiteral parseIntLiteral() throws SyntaxError {
		IntLiteral IL = null;

		if (currentToken.kind == Token.INTLITERAL) {
			String spelling = currentToken.spelling;
			accept();
			IL = new IntLiteral(spelling, previousTokenPosition);
		} else
			syntacticError("integer literal expected here", "");
		return IL;
	}

	FloatLiteral parseFloatLiteral() throws SyntaxError {
		FloatLiteral FL = null;

		if (currentToken.kind == Token.FLOATLITERAL) {
			String spelling = currentToken.spelling;
			accept();
			FL = new FloatLiteral(spelling, previousTokenPosition);
		} else
			syntacticError("float literal expected here", "");
		return FL;
	}

	BooleanLiteral parseBooleanLiteral() throws SyntaxError {
		BooleanLiteral BL = null;

		if (currentToken.kind == Token.BOOLEANLITERAL) {
			String spelling = currentToken.spelling;
			accept();
			BL = new BooleanLiteral(spelling, previousTokenPosition);
		} else
			syntacticError("boolean literal expected here", "");
		return BL;
	}

	StringLiteral parseStringLiteral() throws SyntaxError {
		StringLiteral SL = null;

		if (currentToken.kind == Token.STRINGLITERAL) {
			String spelling = currentToken.spelling;
			accept();
			SL = new StringLiteral(spelling, previousTokenPosition);
		} else
			syntacticError("string literal expected here", "");
		return SL;
	}
}
