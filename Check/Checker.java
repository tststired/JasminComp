/***
 * Checker.java   
 *
 * Sun 10 Mar 2024 12:16:02 AEDT
 *
 ****/

package VC.Checker;

import VC.ASTs.*;
import VC.Scanner.SourcePosition;
import VC.ErrorReporter;
import VC.StdEnvironment;


public final class Checker implements Visitor {
	private String errMesg[] = {
		"*0: main function is missing",
		"*1: return type of main is not int",

		// defined occurrences of identifiers
		// for global, local and parameters
		"*2: identifier redeclared",
		"*3: identifier declared void",
		"*4: identifier declared void[]",

		// applied occurrences of identifiers
		"*5: identifier undeclared",

		// assignments
		"*6: incompatible type for =",
		"*7: invalid lvalue in assignment",

		// types for expressions
		"*8: incompatible type for return",
		"*9: incompatible type for this binary operator",
		"*10: incompatible type for this unary operator",

		// scalars
		"*11: attempt to use an array/function as a scalar",

		// arrays
		"*12: attempt to use a scalar/function as an array",
		"*13: wrong type for element in array initialiser",
		"*14: invalid initialiser: array initialiser for scalar",
		"*15: invalid initialiser: scalar initialiser for array",
		"*16: excess elements in array initialiser",
		"*17: array subscript is not an integer",
		"*18: array size missing",

		// functions
		"*19: attempt to reference a scalar/array as a function",

		// conditional expressions in if, for and while
		"*20: if conditional is not boolean",
		"*21: for conditional is not boolean",
		"*22: while conditional is not boolean",

		// break and continue
		"*23: break must be in a while/for",
		"*24: continue must be in a while/for",

		// parameters
		"*25: too many actual parameters",
		"*26: too few actual parameters",
		"*27: wrong type for actual parameter",

		// reserved for errors that I may have missed (J. Xue)
		"*28: misc 1",
		"*29: misc 2",

		// the following two checks are optional
		"*30: statement(s) not reached",
		"*31: missing return statement",
	};

	private SymbolTable idTable;
	private static SourcePosition dummyPos = new SourcePosition();
	private ErrorReporter reporter;

	// Checks whether the source program, represented by its AST,
	// satisfies the language's scope rules and type rules.
	// Also decorates the AST as follows:
	// (1) Each applied occurrence of an identifier is linked to
	// the corresponding declaration of that identifier.
	// (2) Each expression and variable is decorated by its type.

	public Checker(ErrorReporter reporter) {
		this.reporter = reporter;
		this.idTable = new SymbolTable();
		establishStdEnvironment();
	}
	public void check(AST ast) {
		ast.visit(this, null);
	}

	// Programs
	public Object visitProgram(Program ast, Object o) {
		ast.FL.visit(this, null);

		return null;
	}

	// Statements
	public Object visitStmtList(StmtList ast, Object o) {
		ast.S.visit(this, o);
		if (ast.S instanceof ReturnStmt && ast.SL instanceof StmtList)
			reporter.reportError(errMesg[30], "", ast.SL.position);
		ast.SL.visit(this, o);
		return null;
	}
	public Object visitIfStmt(IfStmt ast, Object o) {
		// if only we aren't restricted to booleans :3
		// if(1+2) is valid in C xddd
		// jk no more homework plz
		if (!((Type) ast.E.visit(this, o)).isBooleanType()) {
			reporter.reportError(errMesg[20] + " (found: " + ast.E.type.toString() + ")", "", ast.E.position);
		}
		ast.S1.visit(this, o);
		ast. S2.visit(this, o);
		return null;
	}
	public Object visitWhileStmt(WhileStmt ast, Object o) {
		// really should just do a function wrapper since its just copy paste above
		// but im too tired ;(
		if (!((Type) ast.E.visit(this, o)).isBooleanType()) {
			reporter.reportError(errMesg[22] + " (found: " + ast.E.type.toString() + ")", "", ast.E.position);
		}
		ast.S.visit(this, o);
		return null;
	}
	public Object visitForStmt(ForStmt ast, Object o) {
		//if (!ast.E3.isEmptyExpr()) { BUG HALL OF FAME NICE 1 HR DEBUGGING
		// i love being confused with e1 e2 e3 is very nice good bug :))))))))
		if (!ast.E2.isEmptyExpr()) {
			if (!((Type) ast.E2.visit(this, o)).isBooleanType()) {
				reporter.reportError(errMesg[21] + " (found: " + ast.E2.type.toString() + ")", "", ast.E2.position);
			}
		}
		return null;
	}
	public Object visitBreakStmt(BreakStmt ast, Object o) {
		// this one a bit annoying since parent isn't actually scope parent
		// we call parent's parent until we reach an edge of a compound stmt
		// we're prolly under a few stmt lists
		// at the edge of compound stmt we check if that parent is while or for
		if (!(ast.parent instanceof WhileStmt) && !(ast.parent instanceof ForStmt)) {
			while (!(ast.parent instanceof CompoundStmt)) {
				ast.parent = ast.parent.parent;
			}
			if (!(ast.parent.parent instanceof WhileStmt) && !(ast.parent.parent instanceof ForStmt)) {
				reporter.reportError(errMesg[23], "", ast.position);
			}
		}


		return null;
	}
	public Object visitContinueStmt(ContinueStmt ast, Object o) {
		// same as above but code reptition is cool :3
		if (!(ast.parent instanceof WhileStmt) && !(ast.parent instanceof ForStmt)) {
			while (!(ast.parent instanceof CompoundStmt)) {
				ast.parent = ast.parent.parent;
			}
			if (!(ast.parent.parent instanceof WhileStmt) && !(ast.parent.parent instanceof ForStmt)) {
				reporter.reportError(errMesg[24], "", ast.position);
			}
		}

		return null;
	}
	public Object visitReturnStmt(ReturnStmt ast, Object o) {
		//return stmt followed by a statement = err (optional
		//return stmt flow of control for non void (optional)
		// return stmt coercion (optional)

		//no arr ret
		// no return expr when void (i think) might get me later!!!!!!!!!!!!!!!!!!!!
		//return with no expr must be void func
		//if non void must have expr ret valid type (coercion)

		Type eT = (Type) ast.E.visit(this, o);
		if ((ast.E.isEmptyExpr())) {
			if (!(((FuncDecl) o).T.isVoidType())) {
				reporter.reportError(errMesg[8] , "", ast.position);
				return null;
			}
		} else {
			if (!(((FuncDecl) o).T).assignable(eT)) {
				reporter.reportError(errMesg[8] ,"", ast.position);
				return null;
			}
		}

		return null;
	}
	public Object visitCompoundStmt(CompoundStmt ast, Object o) {
		idTable.openScope();
		if (o instanceof FuncDecl) {
			((FuncDecl) o).PL.visit(this, o);
		}
		ast.DL.visit(this, o);
		ast.SL.visit(this, o);
		idTable.closeScope();
		return null;
	}
	public Object visitExprStmt(ExprStmt ast, Object o) {
		ast.E.visit(this, o);
		return null;
	}
	public Object visitEmptyCompStmt(EmptyCompStmt ast, Object o) {
		return null;
	}
	public Object visitEmptyStmt(EmptyStmt ast, Object o) {
		return null;
	}

	// auxiliary methods

	public Integer findArrayLength(ArrayInitExpr ast) {
		Integer c = 0;
		ArrayExprList IL = (ArrayExprList) ast.IL;
		while (IL instanceof ArrayExprList) {
			if (IL.EL instanceof EmptyArrayExprList) {
				break;
			} else {
				IL = (ArrayExprList) IL.EL;
				c++;
			}
		}
		return c;
	}

	public List safeNextArr(List list) {
		if (list instanceof EmptyArrayExprList) {
			return new EmptyArrayExprList(dummyPos);
		}  else {
			return ((ArrayExprList) list).EL;
		}
	}

	public void parseVar(Decl ast) {
		//check void
		//check array combinations either
		// 1. type_expr with var_expr
		// 2. type_expr with var_emptyexpr
		// 3. type_emptyexpr with var_expr
		// check if i can assign at all assuming its a regular var
		Ident i = ast.I;
		Expr e;
		declareVariable(i, ast);

		if (ast instanceof GlobalVarDecl) {
			e = ((GlobalVarDecl) ast).E;
		} else {
			e = ((LocalVarDecl) ast).E;
		}
		Type vT = ast.T;
		vT.visit(this, ast);

		if (vT.isVoidType()) {
			reporter.reportError(errMesg[3] + ": %", i.spelling, i.position);
		} else if (vT.isArrayType()) {
			if (((ArrayType) vT).T.isVoidType()) {
				reporter.reportError(errMesg[4] + ": %", i.spelling, i.position);
			}
			if (((ArrayType) vT).E.isEmptyExpr() && (e instanceof EmptyExpr)) {
				reporter.reportError(errMesg[18] + ": %", i.spelling, i.position);
			}
		}
		e.visit(this, vT);

		if (vT.isArrayType()) {
			if (e instanceof ArrayInitExpr) {
				// need to find array length
				ArrayType type = (ArrayType) vT;
				if (type.E.isEmptyExpr()) {
					Integer len = findArrayLength((ArrayInitExpr) e);
					type.E = new IntExpr(new IntLiteral(len.toString(), dummyPos), dummyPos);
				} else {
					// check if array length is the same
					// if not, report error
					int initArrLen = findArrayLength((ArrayInitExpr) e);
					int decArrLen = Integer.parseInt(((IntExpr) type.E).IL.spelling);
					if (initArrLen > decArrLen) {
						reporter.reportError(errMesg[16] + ": %", i.spelling, i.position);
					}
					//allgood
				}
			} else {
				if (!(e.isEmptyExpr())) {
					reporter.reportError(errMesg[15] + ": %", i.spelling, i.position);
				} else {
					return;
				}
			}
		} else {
			if (!vT.assignable(e.type)) {
				reporter.reportError(errMesg[6] + ": %", i.spelling, i.position);
			}

		}
	}
	public boolean scalar_c(Expr ast) {
		// only thing that can't be scalar is funcdecl or arraytype
		// check if is instance of VarExpr
		if (ast instanceof VarExpr) {
			Ident i = ((SimpleVar) ((VarExpr) ast).V).I;
			Decl decl = idTable.retrieve(i.spelling);

			if (decl == null) {
				return false;
			}
			if (decl instanceof FuncDecl || decl.T instanceof ArrayType) {
				reporter.reportError(errMesg[11] + ": %", i.spelling, i.position);
				return false;
			}
		}
		return true;
	}

	public boolean array_c(Expr ast) {
		// check if is array type
		// check if is instance of VarExpr
		if (ast instanceof VarExpr) {
			Ident i = ((SimpleVar) ((VarExpr) ast).V).I;
			Decl decl = idTable.retrieve(i.spelling);
			if (decl.T instanceof ArrayType) {
				return true;
			}
			reporter.reportError(errMesg[12] + ": %", i.spelling, i.position);
		}
		return false;
	}

	private void declareVariable(Ident ident, Decl decl) {
		IdEntry entry = idTable.retrieveOneLevel(ident.spelling);

		if (entry == null) {
			; // no problem
		} else
			reporter.reportError(errMesg[2] + ": %", ident.spelling, ident.position);
		idTable.insert(ident.spelling, decl);
		// if a declaration, say, "int i = 1" has also an initialiser (i.e.,
		// "1" here, then i is both a defined occurrence (i.e., definition)
		// of i and an applied occurrence (i.e., use) of i. So do the
		// identification here for i just in case (even it is not used)
		ident.visit(this, null);
	}

	// Expressions
	// Returns the Type denoting the type of the expression. Does
	// not use the given object.
	public Object visitIntExpr(IntExpr ast, Object o) {
		ast.type = StdEnvironment.intType;
		return ast.type;
	}
	public Object visitFloatExpr(FloatExpr ast, Object o) {
		ast.type = StdEnvironment.floatType;
		return ast.type;
	}
	public Object visitBooleanExpr(BooleanExpr ast, Object o) {
		ast.type = StdEnvironment.booleanType;
		return ast.type;
	}
	public Object visitStringExpr(StringExpr ast, Object o) {
		ast.type = StdEnvironment.stringType;
		return ast.type;
	}
	public Object visitUnaryExpr(UnaryExpr ast, Object o) {
		Type type = (Type) ast.E.visit(this, o);
		if(type.isErrorType()) {
			ast.type = StdEnvironment.errorType;
		} else {
			if (!(type.isVoidType() || type.isStringType() || type.isArrayType())) {
				String op = ast.O.spelling;
				//assuming no error, we need to match intfloatbools
				// check for negation if its ! need to make sure its booleanble
				// if its not  then it shouldnt be

				if (op.equals("!") && !type.isBooleanType()) {
					reporter.reportError(errMesg[10] + ": %", "!", ast.position);
					ast.type = StdEnvironment.errorType;
					return ast.type;
				} else if (!op.equals("!") && type.isBooleanType()) {
					reporter.reportError(errMesg[10] + ": %", "=", ast.position);
					ast.type = StdEnvironment.errorType;
					return ast.type;
				}

				ast.type = type;
				return type;
			}

		}

		reporter.reportError(errMesg[10], "", ast.position);
		ast.type = StdEnvironment.errorType;
		return ast.type;
	}

	public Object visitBinaryExpr(BinaryExpr ast, Object o) {
		Type t1 = (Type) ast.E1.visit(this, o);
		Type t2 = (Type) ast.E2.visit(this, o);

		//theres's bazillion repeated code but my brain too smooth to do it better


		if (t1.isErrorType() || t2.isErrorType()) {
			ast.type = StdEnvironment.errorType;
			return ast.type;
		}

		//check if scaar
		//do the intfloatbool checks on both
		// check assignment


		scalar_c(ast.E1);
		scalar_c(ast.E2);

		if (!(t1.isIntType() || t1.isFloatType() || t1.isBooleanType())) {
			reporter.reportError(errMesg[9] + ": %", ast.O.spelling, ast.position);
			ast.type = StdEnvironment.errorType;
			return ast.type;
		}

		if (!(t2.isIntType() || t2.isFloatType() || t2.isBooleanType())) {
			reporter.reportError(errMesg[9] + ": %", ast.O.spelling, ast.position);
			ast.type = StdEnvironment.errorType;
			return ast.type;
		}


		if (!t1.equals(t2)) {
			reporter.reportError(errMesg[9] + ": %", ast.O.spelling, ast.position);
			ast.type = StdEnvironment.errorType;
			return ast.type;
		}

		String spelling = ast.O.spelling;
		//CGHECK THEM ALL LOOK SPEC
		//make usre to return proper types
		//we only really care about intfloatbool
		if (spelling.equals("+") || spelling.equals("-") || spelling.equals("*") || spelling.equals("/")) {
			if (t1.isIntType()) {
				ast.type = StdEnvironment.intType;
			} else {
				ast.type = StdEnvironment.floatType;
			}
		} else if (spelling.equals("&&") || spelling.equals("||")) {
			ast.type = StdEnvironment.booleanType;
		} else if (spelling.equals("==") || spelling.equals("!=") || spelling.equals("=") || spelling.equals("!")) {
			ast.type = StdEnvironment.booleanType;
		} else if (spelling.equals("<") || spelling.equals("<=") || spelling.equals(">") || spelling.equals(">=")) {
			ast.type = StdEnvironment.booleanType;
		} else {
			reporter.reportError(errMesg[9] + ": %", ast.O.spelling, ast.position);
			ast.type = StdEnvironment.errorType;
		}
		return ast.type;

	}

	public Object visitArrayInitExpr(ArrayInitExpr ast, Object o) {
		Type decltype = (Type) o;
		if (!decltype.isArrayType()) {
			reporter.reportError(this.errMesg[14], "wrong type at left brnch", ast.position);
			ast.type = StdEnvironment.errorType;
			return ast.type;
		} else {
			ast.IL.visit(this, ((ArrayType)decltype).T);
			return (((ArrayType)decltype).T);
		}
	}

	public Object visitArrayExprList(ArrayExprList ast, Object o) {
		//type here is already extracted tyo prinmitives no longer arraytype
		Type decltype = (Type) o;
		ast.E.visit(this, o);

		if (!decltype.assignable(ast.E.type)) {
			reporter.reportError(errMesg[13] + ": %", String.valueOf(ast.index), ast.position);
		}

		if (ast.EL instanceof ArrayExprList) {
			((ArrayExprList)ast.EL).index = ast.index++;
			return ast.EL.visit(this, o);
		} else {
			return o;
		}
	}

	public Object visitArrayExpr(ArrayExpr ast, Object o) {

		Type vT = (Type)ast.V.visit(this, o);
		Type eT = (Type)ast.E.visit(this, o);

		if (vT.isArrayType()) {
			vT = ((ArrayType)vT).T;
		} else if (!vT.isErrorType()) {
			reporter.reportError(this.errMesg[12], "", ast.position);
			vT = StdEnvironment.errorType;
		}
		if (!eT.isErrorType() && !eT.isIntType()) {
			this.reporter.reportError(this.errMesg[17], "", ast.position);
		}

		ast.type = vT;
		return vT;
	}
	public Object visitVarExpr(VarExpr ast, Object o) {
		ast.type = (Type) ast.V.visit(this, o);
		if (ast.type == null) {
			ast.type = StdEnvironment.errorType;
		}
		return ast.type;
	}



	public Object visitCallExpr(CallExpr ast, Object o) {
		// only take in funcdecl since we're callign an expression
		// check argc vs argv count
		// then be annoyed at checking types of those args
		// checking args recursively bump up the list of args in funcdecl
		Ident i = ast.I;
		Decl idecl = ((Decl) i.visit(this, null));

		// it'll return null from retrieve if it doesn't exixt
		if (idecl == null) {
//			reporter.reportError(errMesg[5] + ": %", i.spelling, i.position);
			ast.type = StdEnvironment.errorType;
		} else if (!(idecl instanceof FuncDecl)) {
			if (idecl != null) {
				reporter.reportError(errMesg[19] + ": %", i.spelling, i.position);
			}
			ast.type = StdEnvironment.errorType;
		}  else {
			ast.AL.visit(this, ((FuncDecl) i.decl).PL);
			ast.type = ((FuncDecl) i.decl).T;

			int funcArgc = 0;
			int callArgc = 0;
			List funcPL = ((FuncDecl) i.decl).PL;
			List callAL = ast.AL;
			//yeah this for sure can be moved somewhere else
			// waste of code tbh
			while (!(funcPL instanceof EmptyParaList)) {
				funcArgc++;
				funcPL = ((ParaList) funcPL).PL;
			}
			while (!(callAL instanceof EmptyArgList)) {
				callArgc++;
				callAL = ((ArgList) callAL).AL;
			}

			if (funcArgc < callArgc) {
				reporter.reportError(errMesg[25], "", ast.position);
			} else if (funcArgc > callArgc) {
				reporter.reportError(errMesg[26], "", ast.position);
			}
		}



		return ast.type;


//		int abs_num = (callArgc <= funcArgc) ? callArgc : funcArgc;

		// this is so scuffed
//		for (int j = 0; j < abs_num; j++) {
//			Arg a = ((ArgList) callALRec).A;
//			ParaDecl p = ((ParaList) funcPLRec).P;
//
//			//undo || for && later
//			if (a.type.isArrayType() || p.T.isArrayType()) {
//				Expr argExpArray;
//				if (((ArrayType) a.type).parent instanceof LocalVarDecl) {
//					argExpArray = ((LocalVarDecl) ((ArrayType) a.type).parent).E;
//				} else {
//					argExpArray = ((GlobalVarDecl) ((ArrayType) a.type).parent).E;
				//				}
				//
				//			} else if (a.type.assignable(p.T)) {
				//				reporter.reportError(errMesg[27] + ": ", i.spelling, ast.position);
				//			}
				//			callALRec = ((ArgList) callALRec).AL;
				//			funcPLRec = ((ParaList) funcPLRec).PL;
				//		}





	}



	public Object visitAssignExpr(AssignExpr ast, Object o) {
		//!!!!!! we don't handle initialisation of anytype someoone will call us on assignment
		//arrays can't be in expressions
		// check LHS and RHS type
		// check type coercion for int to float
		// check scalars (anything not a function or array)
		// check if LHS is an identifier (can't assign float to float etc)
		// check assignable  and equals
		// only expressions that can hold vars are varexp && arrayexpr
		// no other things can be on the lvalue


		//Todo
		//fix the stupid err 7 &6 bug



		Boolean f1 = true;
		Expr e1 = ast.E1;
		Expr e2 = ast.E2;
		if (scalar_c(e1) || scalar_c(e2)) {
			ast.type = StdEnvironment.errorType;
			f1 = false;
		}

		Type t1 = (Type) e1.visit(this, o);
		Type t2 = (Type) e2.visit(this, o);

		//decide what types to assign this obj


		//check if spurious errors
		if (t1.isErrorType() || t2.isErrorType()) {
			ast.type = StdEnvironment.errorType;
		}

		if (!(ast.E1 instanceof VarExpr || ast.E1 instanceof ArrayExpr)) {
			reporter.reportError(errMesg[7] + "", "",  ast.position);
			ast.type = StdEnvironment.errorType;
			return StdEnvironment.errorType;
		}

		SimpleVar v;
		if (e1 instanceof VarExpr) {
			v = (SimpleVar) ((VarExpr) e1).V;
		} else {
			v = (SimpleVar) ((ArrayExpr) e1).V;
		}
		Decl varDecl = (Decl) idTable.retrieve(v.I.spelling);

		if (varDecl == null || varDecl instanceof FuncDecl) {
			reporter.reportError(errMesg[7] + "", "",  ast.position);
			ast.type = StdEnvironment.errorType;
		}


		// don't know why this stupid bug is so impossible to fix
		// i cbbbbbbbbbbbbbbbbbbbbbbbaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
		if (e1.type.assignable(e2.type)) {
			ast.type = ast.E1.type;
		} else {
			reporter.reportError(errMesg[6], "",  ast.position);
			ast.type = StdEnvironment.errorType;
		}

		return ast.type;

	}

	public Object visitEmptyExpr(EmptyExpr ast, Object o) {
		ast.type = StdEnvironment.errorType;
		return ast.type;
	}

	// Declarations
	// Always returns null. Does not use the given object.
	public Object visitFuncDecl(FuncDecl ast, Object o) {
		// HINT
		// Pass ast as the 2nd argument (as done below) so that the
		// formal parameters of the function an be extracted from ast when the
		// function body is later visited

		IdEntry ret = idTable.retrieveOneLevel(ast.I.spelling);
		if (ret != null) {
			reporter.reportError(errMesg[2] + ": %", ast.I.spelling, ast.I.position);
		}
		idTable.insert(ast.I.spelling, ast);

		if (ast.S.isEmptyCompStmt() && !ast.T.isVoidType()) {
			reporter.reportError(errMesg[31] + ": %", ast.I.spelling, ast.I.position);
		}
		ast.S.visit(this, ast);
		return null;
	}
	public Object visitDeclList(DeclList ast, Object o) {
		ast.D.visit(this, o);
		ast.DL.visit(this, o);
		return null;
	}
	public Object visitGlobalVarDecl(GlobalVarDecl ast, Object o) {
		parseVar(ast);
		return null;
	}
	public Object visitLocalVarDecl(LocalVarDecl ast, Object o) {
		// don't worry the funcdecl already scoped for us so we bingchilling
		parseVar(ast);
		return null;
	}

	// Parameters
	// Always returns null. Does not use the given object.
	public Object visitParaList(ParaList ast, Object o) {
		ast.P.visit(this, o);
		ast.PL.visit(this, o);
		return null;
	}
	public Object visitParaDecl(ParaDecl ast, Object o) {
		declareVariable(ast.I, ast);

		if (ast.T.isVoidType()) {
			reporter.reportError(errMesg[3] + ": %", ast.I.spelling, ast.I.position);
		} else if (ast.T.isArrayType()) {
			if (((ArrayType) ast.T).T.isVoidType())
				reporter.reportError(errMesg[4] + ": %", ast.I.spelling, ast.I.position);
		}
		return null;
	}

	// Arguments
	public Object visitArgList(ArgList ast, Object o) {
		// still don't get why i can't cast emptypara <=> para list kinda xddd
		if (((List) o).isEmptyParaList()) {
			reporter.reportError(errMesg[25], "", ast.position);
		} else {
			ast.A.visit(this, ((ParaList) o).P);
			ast.AL.visit(this, ((ParaList) o).PL);
		}
		return null;
	}
	public Object visitArg(Arg ast, Object o) {
		ast.type = (Type) ast.E.visit(this, o);

		if (ast.type == null) {
			ast.type = StdEnvironment.errorType;
		}

		ParaDecl param = ((ParaDecl) o);

		if(param.T.isArrayType()) {
			if(!ast.type.isArrayType()) {
				reporter.reportError(errMesg[27] + ": %", param.I.spelling , ast.E.position);
			} else {
				if (!((ArrayType) param.T).T.assignable(((ArrayType) ast.type).T)) {
					reporter.reportError(errMesg[27] + ": %", param.I.spelling , ast.E.position);
				}
			}
		} else {
			if (!param.T.assignable(ast.type)) {
				reporter.reportError(errMesg[27] + ": %", param.I.spelling , ast.E.position);
			}
		}
		return ast.type;
	}

	// Variables
	public Object visitSimpleVar(SimpleVar ast, Object o) {
		Decl binding = (Decl) visitIdent(ast.I, o);

		if (binding == null) {
			ast.type = StdEnvironment.errorType;
			return ast.type;
		}
		ast.type = binding.T;
		return ast.type;
	}

	// Literals, Identifiers and Operators
	public Object visitIdent(Ident I, Object o) {
		Decl binding = idTable.retrieve(I.spelling);
		if (binding != null) {
			I.decl = binding;
		} else {
			reporter.reportError(errMesg[5] + ": %", I.spelling, I.position);
		}
		return binding;
	}
	public Object visitIntLiteral(IntLiteral IL, Object o) {
		return StdEnvironment.intType;
	}
	public Object visitFloatLiteral(FloatLiteral IL, Object o) {
		return StdEnvironment.floatType;
	}
	public Object visitBooleanLiteral(BooleanLiteral SL, Object o) {
		return StdEnvironment.booleanType;
	}
	public Object visitStringLiteral(StringLiteral IL, Object o) {
		return StdEnvironment.stringType;
	}
	public Object visitOperator(Operator O, Object o) {
		return null;
	}

	// Creates a small AST to represent the "declaration" of each built-in
	// function, and enters it in the symbol table.
	private FuncDecl declareStdFunc(Type resultType, String id, List pl) {

		FuncDecl binding;

		binding = new FuncDecl(resultType, new Ident(id, dummyPos), pl,
				new EmptyStmt(dummyPos), dummyPos);
		idTable.insert(id, binding);
		return binding;
	}

	// Creates small ASTs to represent "declarations" of all
	// build-in functions.
	// Inserts these "declarations" into the symbol table.
	private final static Ident dummyI = new Ident("x", dummyPos);
	private void establishStdEnvironment() {

		// Define four primitive types
		// errorType is assigned to ill-typed expressions

		StdEnvironment.booleanType = new BooleanType(dummyPos);
		StdEnvironment.intType = new IntType(dummyPos);
		StdEnvironment.floatType = new FloatType(dummyPos);
		StdEnvironment.stringType = new StringType(dummyPos);
		StdEnvironment.voidType = new VoidType(dummyPos);
		StdEnvironment.errorType = new ErrorType(dummyPos);

		// enter into the declarations for built-in functions into the table

		StdEnvironment.getIntDecl = declareStdFunc(StdEnvironment.intType,
				"getInt", new EmptyParaList(dummyPos));
		StdEnvironment.putIntDecl = declareStdFunc(StdEnvironment.voidType,
				"putInt", new ParaList(
						new ParaDecl(StdEnvironment.intType, dummyI, dummyPos),
						new EmptyParaList(dummyPos), dummyPos));
		StdEnvironment.putIntLnDecl = declareStdFunc(StdEnvironment.voidType,
				"putIntLn", new ParaList(
						new ParaDecl(StdEnvironment.intType, dummyI, dummyPos),
						new EmptyParaList(dummyPos), dummyPos));
		StdEnvironment.getFloatDecl = declareStdFunc(StdEnvironment.floatType,
				"getFloat", new EmptyParaList(dummyPos));
		StdEnvironment.putFloatDecl = declareStdFunc(StdEnvironment.voidType,
				"putFloat", new ParaList(
						new ParaDecl(StdEnvironment.floatType, dummyI, dummyPos),
						new EmptyParaList(dummyPos), dummyPos));
		StdEnvironment.putFloatLnDecl = declareStdFunc(StdEnvironment.voidType,
				"putFloatLn", new ParaList(
						new ParaDecl(StdEnvironment.floatType, dummyI, dummyPos),
						new EmptyParaList(dummyPos), dummyPos));
		StdEnvironment.putBoolDecl = declareStdFunc(StdEnvironment.voidType,
				"putBool", new ParaList(
						new ParaDecl(StdEnvironment.booleanType, dummyI, dummyPos),
						new EmptyParaList(dummyPos), dummyPos));
		StdEnvironment.putBoolLnDecl = declareStdFunc(StdEnvironment.voidType,
				"putBoolLn", new ParaList(
						new ParaDecl(StdEnvironment.booleanType, dummyI, dummyPos),
						new EmptyParaList(dummyPos), dummyPos));

		StdEnvironment.putStringLnDecl = declareStdFunc(StdEnvironment.voidType,
				"putStringLn", new ParaList(
						new ParaDecl(StdEnvironment.stringType, dummyI, dummyPos),
						new EmptyParaList(dummyPos), dummyPos));

		StdEnvironment.putStringDecl = declareStdFunc(StdEnvironment.voidType,
				"putString", new ParaList(
						new ParaDecl(StdEnvironment.stringType, dummyI, dummyPos),
						new EmptyParaList(dummyPos), dummyPos));

		StdEnvironment.putLnDecl = declareStdFunc(StdEnvironment.voidType,
				"putLn", new EmptyParaList(dummyPos));

	}


	// Types
	// Returns the type predefined in the standard environment.
	public Object visitVoidType(VoidType ast, Object o) {
		return StdEnvironment.voidType;
	}
	public Object visitBooleanType(BooleanType ast, Object o) {
		return StdEnvironment.booleanType;
	}
	public Object visitIntType(IntType ast, Object o) {
		return StdEnvironment.intType;
	}
	public Object visitFloatType(FloatType ast, Object o) {
		return StdEnvironment.floatType;
	}
	public Object visitStringType(StringType ast, Object o) {
		return StdEnvironment.stringType;
	}
	public Object visitArrayType(ArrayType ast, Object o) {
		Type type = (Type) ast.T.visit(this, o);
		ast.E.visit(this, o);
		return type;
	}
	public Object visitErrorType(ErrorType ast, Object o) {
		return StdEnvironment.errorType;
	}

	//EMPTY STATEMENT
	public Object visitEmptyDeclList(EmptyDeclList ast, Object o) {
		return null;
	}
	public Object visitEmptyStmtList(EmptyStmtList ast, Object o) {
		return null;
	}
	public Object visitEmptyArrayExprList(EmptyArrayExprList ast, Object o) {
		return null;
	}
	public Object visitEmptyParaList(EmptyParaList ast, Object o) {
		return null;
	}
	public Object visitEmptyArgList(EmptyArgList ast, Object o) {
		return null;
	}


}
