/*
 *** Emitter.java
 *** Mon 03 Apr 2023 12:42:25 AEST
 */

// A new frame object is created for every function just before the
// function is being translated in visitFuncDecl.
//
// All the information about the translation of a function should be
// placed in this Frame object and passed across the AST nodes as the
// 2nd argument of every visitor method in Emitter.java.

package VC.CodeGen;

import java.lang.reflect.Array;
import java.util.LinkedList;
import java.util.Enumeration;
import java.util.ListIterator;

import VC.ASTs.*;
import VC.ErrorReporter;
import VC.StdEnvironment;

public final class Emitter implements Visitor {

	private ErrorReporter errorReporter;
	private String inputFilename;
	private String classname;
	private String outputFilename;

	public Emitter(String inputFilename, ErrorReporter reporter) {
		this.inputFilename = inputFilename;
		errorReporter = reporter;

		int i = inputFilename.lastIndexOf('.');
		if (i > 0)
			classname = inputFilename.substring(0, i);
		else
			classname = inputFilename;

	}

	// PRE: ast must be a Program node

	public final void gen(AST ast) {
		ast.visit(this, null);
		JVM.dump(classname + ".j");
	}

	// Programs
	public Object visitProgram(Program ast, Object o) {
		/**
		 * This method works for scalar variables only. You need to modify
		 * it to handle all array-related declarations and initialisations.
		 **/

		// Generates the default constructor initialiser
		emit(JVM.CLASS, "public", classname);
		emit(JVM.SUPER, "java/lang/Object");

		emit("");

		// Three subpasses:

		// (1) Generate .field definition statements since
		// these are required to appear before method definitions
		List list = ast.FL;
		while (!list.isEmpty()) {
			DeclList dlAST = (DeclList) list;
			if (dlAST.D instanceof GlobalVarDecl) {
				GlobalVarDecl vAST = (GlobalVarDecl) dlAST.D;
				emit(JVM.STATIC_FIELD, vAST.I.spelling, VCtoJavaType(vAST.T));
			}
			list = dlAST.DL;
		}

		emit("");

		// (2) Generate <clinit> for global variables (assumed to be static)

		emit("; standard class static initializer ");
		emit(JVM.METHOD_START, "static <clinit>()V");
		emit("");

		// create a Frame for <clinit>

		Frame frame = new Frame(false);

		list = ast.FL;
		while (!list.isEmpty()) {
			DeclList dlAST = (DeclList) list;
			if (dlAST.D instanceof GlobalVarDecl vAST) {

				// array program entry
				if (vAST.T.isArrayType()) {
					ArrayType type = (ArrayType) vAST.T;
					type.E.visit(this, frame);

					//we make new thingo in jvm to handle new array bc
					// sol answer requires newarray
					frame.push();
					// why tf is this different >_>
					emit(JVM.NEWARRAY, VCtoJavaArr(type));
					//vAST.E.type = type;
					//nice troll noob
					vAST.E.type = type.T;
				}

				if (!vAST.E.isEmptyExpr()) {
					vAST.E.visit(this, frame);
				} else {
					if (vAST.T.equals(StdEnvironment.floatType))
						emit(JVM.FCONST_0);
					else
						emit(JVM.ICONST_0);
					frame.push();
				}
				emitPUTSTATIC(VCtoJavaType(vAST.T), vAST.I.spelling);
				frame.pop();
			}
			list = dlAST.DL;
		}

		emit("");
		emit("; set limits used by this method");
		emit(JVM.LIMIT, "locals", frame.getNewIndex());

		emit(JVM.LIMIT, "stack", frame.getMaximumStackSize());
		// changed by the marker
		//emit(JVM.LIMIT, "stack", 50);

		emit(JVM.RETURN);
		emit(JVM.METHOD_END, "method");

		emit("");

		// (3) Generate Java bytecode for the VC program

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

	// Lists for denoting the null reference

	public Object visitEmptyDeclList(EmptyDeclList ast, Object o) {
		return null;
	}

	public Object visitEmptyStmtList(EmptyStmtList ast, Object o) {
		return null;
	}

	public Object visitEmptyArrayExprList(EmptyArrayExprList ast, Object o) {
		// should be nothing it's empty anyways
        return null;
    }

	public Object visitEmptyParaList(EmptyParaList ast, Object o) {
		return null;
	}

	public Object visitEmptyArgList(EmptyArgList ast, Object o) {
		return null;
	}

	// Statements

	public Object visitStmtList(StmtList ast, Object o) {
		ast.S.visit(this, o);
		ast.SL.visit(this, o);
		return null;
	}

	public Object visitIfStmt(IfStmt ast, Object o) {
		//toodo -- done jingling code
		Frame frame = (Frame) o;
		String falseLabel = frame.getNewLabel();
		String nextLabel = frame.getNewLabel();

		ast.E.visit(this, o);
		//pop off operator stack since ifeq consumes
		emit(JVM.IFEQ, falseLabel);
		frame.pop();
		ast.S1.visit(this, o);
		// no change with goto
		emit("goto", nextLabel);
		emit(falseLabel + ":");
		ast.S2.visit(this, o);
		emit(nextLabel + ":");

		return null;
    }

	public Object visitWhileStmt(WhileStmt ast, Object o) {
        // toodo -- done jingling code
		Frame frame = (Frame) o;
		String contLabel = frame.getNewLabel();
		String breakLabel = frame.getNewLabel();

		//L1 needs to be cont
		//continue push l1 constack
		//break label l2 brkstack
		frame.conStack.push(contLabel);
		frame.brkStack.push(breakLabel);

		//L1
		emit(contLabel + ":");
		//pop off operator stack since ifeq consumes
		ast.E.visit(this, o);
		emit(JVM.IFEQ, breakLabel);
		frame.pop();
		ast.S.visit(this, o);
		emit("goto", contLabel);

		//L2
		emit(breakLabel + ":");
		frame.conStack.pop();
		frame.brkStack.pop();
		return null;
    }

	public Object visitForStmt(ForStmt ast, Object o) {
		// toodo -- done i think
		// should be similar to while
		// same strategy make stack push labels, might need increment label
		// could also unroll into a while LUL <try this>
		// jk stupid idea ignore yourself

		Frame frame = (Frame) o;
		String initLabel = frame.getNewLabel();
		String incrLabel = frame.getNewLabel();
		String breakLabel = frame.getNewLabel();
		frame.conStack.push(incrLabel);
		frame.brkStack.push(breakLabel);

		//for loop init we never need to visit again so leave outside
		ast.E1.visit(this, o);  //+1


		//start loop check condition rmbr to pop
		//on false go break pop
		emit(initLabel + ":");
		ast.E2.visit(this, o);  //+1
		//pop off operator stack since ifeq consumes
		emit(JVM.IFEQ, breakLabel);  //-1
		frame.pop();

		//stmt body if true
		ast.S.visit(this, o);

		//increment condition need to visit a few times so leave inside
		//go back to start loop
		emit(incrLabel + ":");
		ast.E3.visit(this, o);
		//goto no change
		emit("goto", initLabel);

		//end loop fix cont break
		emit(breakLabel + ":");
		frame.conStack.pop();
		frame.brkStack.pop();
		return null;
	}

	public Object visitBreakStmt(BreakStmt ast, Object o) {
        // toodo -- done
		// goto the label marking the inst following the while
		// so we can just look at parent frame for the label
		// its just a stack of strings so we can use peek meth since its jv utils
		// no pop since goto
		emit("goto", ((Frame) o).brkStack.peek());
        return null;
    }

	public Object visitContinueStmt(ContinueStmt ast, Object o) {
        // toodo -- done
		// goto the label marking the first inst of the while
		// same as above
		emit("goto", ((Frame) o).conStack.peek());
        return null;
    }

	public Object visitReturnStmt(ReturnStmt ast, Object o) {
		// toodo -- done
		/*
		 * int main() { return 0; } must be interpretted as
		 * public static void main(String[] args) { return ; }
		 * Therefore, "return expr", if present in the main of a VC program
		 * must be translated into a RETURN rather than IRETURN instruction.
		 */

		//		• Code Template: return E:int and return E:Boolean
		//      • Code Template: return E:float
		//      im pretty sure boolean is also int return since theres no breturn

		Frame frame = (Frame) o;
		if (frame.isMain()) {
			emit(JVM.RETURN);
			return null;
		} else if (ast.E.type.isIntType() || ast.E.type.isBooleanType()) {
			ast.E.visit(this, o);
			frame.pop(frame.getCurStackSize());
			emit(JVM.IRETURN);
		} else if (ast.E.type.isFloatType()) {
			ast.E.visit(this, o);
			frame.pop(frame.getCurStackSize());
			emit(JVM.FRETURN);
		}
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
				emit(JVM.VAR, "0 is argv [Ljava/lang/String; from " + (String) frame.scopeStart.peek() + " to "
						+ (String) frame.scopeEnd.peek());
				emit(JVM.VAR, "1 is vc$ L" + classname + "; from " + (String) frame.scopeStart.peek() + " to "
						+ (String) frame.scopeEnd.peek());
				// Generate code for the initialiser vc$ = new classname();
				emit(JVM.NEW, classname);
				emit(JVM.DUP);
				frame.push(2);
				emit("invokenonvirtual", classname + "/<init>()V");
				frame.pop();
				emit(JVM.ASTORE_1);
				frame.pop();
			} else {
				emit(JVM.VAR, "0 is this L" + classname + "; from " + (String) frame.scopeStart.peek() + " to "
						+ (String) frame.scopeEnd.peek());
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

	public Object visitExprStmt(ExprStmt ast, Object o) {
        //toodo -- let subexpressions handle the pops
		// off the top of my head only assign, empty don't pop
		ast.E.visit(this, o);
        return null;
    }

	public Object visitEmptyCompStmt(EmptyCompStmt ast, Object o) {
		return null;
	}

	public Object visitEmptyStmt(EmptyStmt ast, Object o) {
		return null;
	}

	// Expressions
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

	public Object visitArrayInitExpr(ArrayInitExpr ast, Object o) {
		// we do the el-classical dont ask me
		ast.IL.visit(this,o);
        return null;
    }

	public Object visitArrayExprList(ArrayExprList ast, Object o) {
		//todo -- aaaaaaaaaaaa
		// ******* whoever calls us already has reference on stack  top just dupe ref
		// first dupe the new array for astore later
		// then cycle through the ast child list and emit [ index, val, ] iastore, then dupe ref
		// surely there's an instruction that doesn't consume the array ref but atlas

		Frame frame = (Frame) o;
		List list = ast;

		for (int i = 0; !(list.isEmptyArrayExprList()); i++) {
			// stupid list casting
			ArrayExprList casted = (ArrayExprList) list;
			emit(JVM.DUP);
			frame.push();
			emit(JVM.BIPUSH, i); // just use this for all since arry maybe big
			frame.push();
			casted.E.visit(this, o);
			//expression returns us to top of stack
			// since all valid programs just look at type of expr
			if (casted.E.type.isFloatType()) {
				emit(JVM.FASTORE);
			} else if (casted.E.type.isIntType()) {
				emit(JVM.IASTORE);
			} else if (casted.E.type.isBooleanType()) {
				emit(JVM.BASTORE);
			} else {
				emit("HHHHHHHHHHHHH");
			}
			frame.pop(3);
			//then loops back and preserves reference on top of stack
			// oi where's your increment for list xddddd
			list = casted.EL;

		}

		return null;
    }

    public Object visitArrayExpr(ArrayExpr ast, Object o) {
        //todo
	    // check if glob or local so we getstatic or not
	    // arrayref, index → value
	    // get arr ref, then load index
	    // do type checking

	    Frame frame = (Frame) o;
		SimpleVar sv = (SimpleVar) ast.V;
		Decl decl = (Decl) sv.I.decl;

	    if (decl.isGlobalVarDecl()) {
		    emitGETSTATIC(VCtoJavaType(ast.type), sv.I.spelling);
	    } else {
			emit(JVM.ALOAD, decl.index);
	    }
	    frame.push();
		ast.E.visit(this, o);

		// here figure out if load or store instruction
	    // look daddy, if we're on rhs just return it we don't need to do anything
	    if(ast.parent instanceof AssignExpr) {
			// fixed the L121 and L170  bug of
		    //	  temp = a[lcv];
		    //	  a[lcv] = a[lcv+1];
		    //	  a[lcv+1] = temp;
		    //	  lastChange = lcv;
		    // you weren't checking before if we were on lhs or rhs
			if (((AssignExpr) ast.parent).E1.equals(ast)) {
				return null;
			}
	    }

		//else just load adn return
	    if (ast.type.isFloatType()) {
		    emit(JVM.FALOAD);
	    } else if (ast.type.isIntType()) {
		    emit(JVM.IALOAD);
	    } else if (ast.type.isBooleanType()) {
		    emit(JVM.BALOAD);
	    } else {
		    emit("YYYYYYYYYY");
	    }
		frame.push(); //consumes one from load

	    return null;
    }

	public Object visitVarExpr(VarExpr ast, Object o) {
		//toodo -- done
		// pawn off expr pop
		ast.V.visit(this, o);
		return null;
	}

	public Object visitEmptyExpr(EmptyExpr ast, Object o) {
		return null;
	}

	public Object visitUnaryExpr(UnaryExpr ast, Object o) {
        //toodo
		// can be i! or f! or i- or f- we ignore f+ and i+
		Frame frame = (Frame) o;
		String op = ast.O.spelling;

		//value returned here (hiopefully stack already calculated correctly riggggggggggggggggggght???)
		ast.E.visit(this, o);

		if (op.equals("i!") || op.equals("f!")) {
			String falseLabel = frame.getNewLabel();
			String nextLabel = frame.getNewLabel();

			//consumes the ast expr
			emit(JVM.IFEQ, falseLabel);
			frame.pop();
			emit(JVM.ICONST_0);
			emit("goto", nextLabel);
			emit(falseLabel + ":");
			emit(JVM.ICONST_1);
			emit(nextLabel + ":");
			//only one push here since both exec paths const_1||const_0 are mutexcl
			//technically can remove both pop n push but we leave it bc you stoopid and will forget later
			frame.push();

		} else if (op.equals("i-")) {
			emit(JVM.INEG);
		} else if (op.equals("f-")) {
			emit(JVM.FNEG);
		}
        return null;
    }

	public Object visitBinaryExpr(BinaryExpr ast, Object o) {
        // toodo
		Frame frame = (Frame) o;


		//above jingling lec code
		//rmbr to do short circuit for and or
		//never evaluate second bit if first gg
		//see emitif_icmpcond for the i!= i== i< i<= i> i>=
		//same for emitfcmp
		// weird that the code of these are written differnetly despite being similar lul
		// shouldnt emitif_icmpcond just be called icmp
		//only need to handle && || and basics

		//check shortcirc
		if (ast.O.spelling.equals("i&&")) {
			String falseLabel = frame.getNewLabel();
			String nextLabel = frame.getNewLabel();

			//straight from jingling xd
			ast.E1.visit(this, o);
			//just to check boolean, if somehow we get a value here smh cry
			emit(JVM.IFEQ, falseLabel);
			ast.E2.visit(this, o);
			emit(JVM.IFEQ, falseLabel);
			//consume both ifeq
			frame.pop(2);

			emit(JVM.ICONST_1);
			emit("goto", nextLabel);

			emit(falseLabel + ":");
			//bubble up result incase of nesting
			emit(JVM.ICONST_0);

			emit(nextLabel + ":");
			frame.push();
			//one push only rmbr since its
		} else if (ast.O.spelling.equals("i||")) {
			String falseLabel = frame.getNewLabel();
			String nextLabel = frame.getNewLabel();

			ast.E1.visit(this, o);
			emit(JVM.IFNE, falseLabel);
			ast.E2.visit(this, o);
			emit(JVM.IFNE, falseLabel);
			frame.pop(2);

			emit(JVM.ICONST_0);
			emit("goto", nextLabel);

			//yay something passed
			emit(falseLabel + ":");
			emit(JVM.ICONST_1);

			//again only one push
			emit(nextLabel + ":");
			frame.push();
		} else {
			//jingling hardcarries this look slide 499
			ast.E1.visit(this, o);
			ast.E2.visit(this, o);
			String op = ast.O.spelling;
			if(op.equals("i+")) {
				//consume 2 add 1 so left pop
				emit(JVM.IADD);
				frame.pop();
			} else if(op.equals("i-")) {
				emit(JVM.ISUB);
				frame.pop();
			} else if(op.equals("f+")) {
				emit(JVM.FADD);
				frame.pop();
			} else if(op.equals("f-")) {
				emit(JVM.FSUB);
				frame.pop();
			} else if(op.equals("i*")) {
				emit(JVM.IMUL);
				frame.pop();
			} else if(op.equals("f*")) {
				emit(JVM.FMUL);
				frame.pop();
			} else if(op.equals("i/")) {
				emit(JVM.IDIV);
				frame.pop();
			} else if(op.equals("f/")) {
				emit(JVM.FDIV);
				frame.pop();
			} else if(op.startsWith("i")) {
				emitIF_ICMPCOND(op, frame);
			} else if(op.startsWith("f")) {
				emitFCMP(op, frame);
			} else {
				emit(JVM.NOP);
			}
		}
        return null;
    }

	public Object visitCallExpr(CallExpr ast, Object o) {
		Frame frame = (Frame) o;
		String fname = ast.I.spelling;

		if (fname.equals("getInt")) {
			ast.AL.visit(this, o); // push args (if any) into the op stack
			emit("invokestatic VC/lang/System.getInt()I");
			frame.push();
		} else if (fname.equals("putInt")) {
			ast.AL.visit(this, o); // push args (if any) into the op stack
			emit("invokestatic VC/lang/System.putInt(I)V");
			frame.pop();
		} else if (fname.equals("putIntLn")) {
			ast.AL.visit(this, o); // push args (if any) into the op stack
			emit("invokestatic VC/lang/System/putIntLn(I)V");
			frame.pop();
		} else if (fname.equals("getFloat")) {
			ast.AL.visit(this, o); // push args (if any) into the op stack
			emit("invokestatic VC/lang/System/getFloat()F");
			frame.push();
		} else if (fname.equals("putFloat")) {
			ast.AL.visit(this, o); // push args (if any) into the op stack
			emit("invokestatic VC/lang/System/putFloat(F)V");
			frame.pop();
		} else if (fname.equals("putFloatLn")) {
			ast.AL.visit(this, o); // push args (if any) into the op stack
			emit("invokestatic VC/lang/System/putFloatLn(F)V");
			frame.pop();
		} else if (fname.equals("putBool")) {
			ast.AL.visit(this, o); // push args (if any) into the op stack
			emit("invokestatic VC/lang/System/putBool(Z)V");
			frame.pop();
		} else if (fname.equals("putBoolLn")) {
			ast.AL.visit(this, o); // push args (if any) into the op stack
			emit("invokestatic VC/lang/System/putBoolLn(Z)V");
			frame.pop();
		} else if (fname.equals("putString")) {
			ast.AL.visit(this, o);
			emit(JVM.INVOKESTATIC, "VC/lang/System/putString(Ljava/lang/String;)V");
			frame.pop();
		} else if (fname.equals("putStringLn")) {
			ast.AL.visit(this, o);
			emit(JVM.INVOKESTATIC, "VC/lang/System/putStringLn(Ljava/lang/String;)V");
			frame.pop();
		} else if (fname.equals("putLn")) {
			ast.AL.visit(this, o); // push args (if any) into the op stack
			emit("invokestatic VC/lang/System/putLn()V");
		} else { // programmer-defined functions

			FuncDecl fAST = (FuncDecl) ast.I.decl;

			// all functions except main are assumed to be instance methods
			if (frame.isMain())
				emit("aload_1"); // vc.funcname(...)
			else
				emit("aload_0"); // this.funcname(...)
			frame.push();

			ast.AL.visit(this, o);

			String retType = VCtoJavaType(fAST.T);

			// The types of the parameters of the called function are not
			// directly available in the FuncDecl node but can be gathered
			// by traversing its field PL.

			Integer sub = 0;
			StringBuffer argsTypes = new StringBuffer("");
			List fpl = fAST.PL;
			while (!fpl.isEmpty()) {
				if (((ParaList) fpl).P.T.equals(StdEnvironment.booleanType))
					argsTypes.append("Z");
				else if (((ParaList) fpl).P.T.equals(StdEnvironment.intType))
					argsTypes.append("I");
				else if (((ParaList) fpl).P.T.equals(StdEnvironment.floatType))
					argsTypes.append("F");
				else if (((ParaList) fpl).P.T.isArrayType()) {
					if (((ArrayType) ((ParaList) fpl).P.T).T.isBooleanType())
						argsTypes.append("[Z");
					else if (((ArrayType) ((ParaList) fpl).P.T).T.isIntType())
						argsTypes.append("[I");
					else if (((ArrayType) ((ParaList) fpl).P.T).T.isFloatType())
						argsTypes.append("[F");
					sub++;
				}


				fpl = ((ParaList) fpl).PL;
			}

			emit("invokevirtual", classname + "/" + fname + "(" + argsTypes + ")" + retType);
			frame.pop(argsTypes.length() + 1 - sub);

			if (!retType.equals("V"))
				frame.push();
		}
		return null;
	}

	public Object visitAssignExpr(AssignExpr ast, Object o) {
		Frame frame = (Frame) o;
		//jingling said dont visit e1 since it might be load or store
		//handle later rmbr case of assignexpr

		if (!(ast.E1 instanceof ArrayExpr)) {
			ast.E2.visit(this, o);
			if (ast.parent instanceof AssignExpr) {
				//have to dupe since we're assignexpr chaining in case granddaddy needs our val
				// i = j = 5;
				// rec visit rhs assignexp -> assignexp -> intliteral
				// dupe intliteral so i can be assigned
				// nawh this is too annoying to debug its litearlly dog
				// L17 line 121 we're different from bubble.jsol
				// just use dup_x2 to push the value to the top
				emit(JVM.DUP);
				frame.push();
			}

			//simple var isnt loaded on yet if lhs
			//if lhs is simple var then load it dont visit since simplevar pushes
			if (ast.E1 instanceof VarExpr) {
				VarExpr ve = ((VarExpr) ast.E1);
				SimpleVar sv = ((SimpleVar) ve.V);
				if (sv.I.decl instanceof GlobalVarDecl) {
					emitPUTSTATIC(VCtoJavaType(sv.type), sv.I.spelling);
				} else if (ast.type.isFloatType()) {
					emitFSTORE(sv.I);
				} else if (ast.type.isIntType() || ast.type.isBooleanType()) {
					emitISTORE(sv.I);
				} else {
					emitPUTSTATIC("bbbbbbbbbbb", "CAN'T BE HERE NOOB");
				}
			}
			return null;
		} else {
			ast.E1.visit(this, o);
			ast.E2.visit(this, o);
			//stack ::   top [some stuff,  index, array ref ] bottom   <-- wiki reads backwards v magical
			// then wejust shove it in ez
			// have to duping trick for cascadin in case multi assign
			// either use dup_x2 (check 3131 discord for moreinfo)
			// or visit it again and reaload vals
			// value3, value2, value1 → value1, value3, value2, value1
			// take -2 to +1
			// seems a bit pointless tho let's just call it again



			if (ast.type.isFloatType()) {
				emit(JVM.FASTORE);
				// no sample test case :(
				// we pray here
			} else if (ast.type.isIntType()) {
				emit(JVM.IASTORE);
			} else if (ast.type.isBooleanType()) {
				emit(JVM.BASTORE);
			}



		}

		return null;

    }




	// Declarations

	public Object visitDeclList(DeclList ast, Object o) {
		ast.D.visit(this, o);
		ast.DL.visit(this, o);
		return null;
	}

	public Object visitFuncDecl(FuncDecl ast, Object o) {

		Frame frame;

		if (ast.I.spelling.equals("main")) {

			frame = new Frame(true);

			// Assume that main has one String parameter and reserve 0 for it
			frame.getNewIndex();

			emit(JVM.METHOD_START, "public static main([Ljava/lang/String;)V");
			// Assume implicitly that
			// classname vc$;
			// appears before all local variable declarations.
			// (1) Reserve 1 for this object reference.

			frame.getNewIndex();

		} else {

			frame = new Frame(false);

			// all other programmer-defined functions are treated as if
			// they were instance methods
			frame.getNewIndex(); // reserve 0 for "this"

			String retType = VCtoJavaType(ast.T);

			// The types of the parameters of the called function are not
			// directly available in the FuncDecl node but can be gathered
			// by traversing its field PL.

			StringBuffer argsTypes = new StringBuffer("");
			List fpl = ast.PL;
			while (!fpl.isEmpty()) {
				if (((ParaList) fpl).P.T.equals(StdEnvironment.booleanType))
					argsTypes.append("Z");
				else if (((ParaList) fpl).P.T.equals(StdEnvironment.intType))
					argsTypes.append("I");
				else if (((ParaList) fpl).P.T.equals(StdEnvironment.floatType))
					argsTypes.append("F");
				else if (((ParaList) fpl).P.T.isArrayType()) {
					ArrayType type = (ArrayType) ((ParaList) fpl).P.T;
					argsTypes.append("[");
					if (type.T.isBooleanType())
						argsTypes.append("Z");
					else if (type.T.isIntType())
						argsTypes.append("I");
					else if (type.T.isFloatType())
						argsTypes.append("F");
				}

				fpl = ((ParaList) fpl).PL;
			}

			emit(JVM.METHOD_START, ast.I.spelling + "(" + argsTypes + ")" + retType);
		}

		ast.S.visit(this, frame);

		// JVM requires an explicit return in every method.
		// In VC, a function returning void may not contain a return, and
		// a function returning int or float is not guaranteed to contain
		// a return. Therefore, we add one at the end just to be sure.

		if (ast.T.equals(StdEnvironment.voidType)) {
			emit("");
			emit("; return may not be present in a VC function returning void");
			emit("; The following return inserted by the VC compiler");
			emit(JVM.RETURN);
		} else if (ast.I.spelling.equals("main")) {
			// In case VC's main does not have a return itself
			emit(JVM.RETURN);
		} else
			emit(JVM.NOP);

		emit("");
		emit("; set limits used by this method");
		emit(JVM.LIMIT, "locals", frame.getNewIndex());

		emit(JVM.LIMIT, "stack", frame.getMaximumStackSize());
		// changed by the marker
		//emit(JVM.LIMIT, "stack", 50);
		emit(".end method");

		return null;
	}

	public Object visitGlobalVarDecl(GlobalVarDecl ast, Object o) {
		// nothing to be done prgoram handles that bit also we added arrs there
		return null;
	}

	public Object visitLocalVarDecl(LocalVarDecl ast, Object o) {
		// todo -- done
		Frame frame = (Frame) o;
		ast.index = frame.getNewIndex();
		String T = VCtoJavaType(ast.T);

		if (ast.T.isArrayType()) {
			ArrayType type = (ArrayType) ast.T;
			T = VCtoJavaType(((ArrayType) ast.T).T);

			//only thing different from below is change T  add [
			emit(JVM.VAR + " " + ast.index + " is " + ast.I.spelling + " [" + T + " from " + (String) frame.scopeStart.peek()
					+ " to " + (String) frame.scopeEnd.peek());

			type.E.visit(this, o);
			emit(JVM.NEWARRAY, VCtoJavaArr(type));
			frame.push();

			if(!(ast.E.isEmptyExpr())) {
				ast.E.type = type.T;
				ast.E.visit(this, o);
			}

			emit(JVM.ASTORE, ast.index);
			//astore works differntly from iastore stuff
			// only consumes one
			frame.pop();
		} else {

			emit(JVM.VAR + " " + ast.index + " is " + ast.I.spelling + " " + T + " from " + (String) frame.scopeStart.peek()
					+ " to " + (String) frame.scopeEnd.peek());

			if (!ast.E.isEmptyExpr()) {
				ast.E.visit(this, o);

				if (ast.T.equals(StdEnvironment.floatType)) {
					emitFSTORE(ast.I);
				} else {
					emitISTORE(ast.I);
				}
				frame.pop();
			}
		}

		return null;
	}

	// Parameters
	public Object visitParaList(ParaList ast, Object o) {
		ast.P.visit(this, o);
		ast.PL.visit(this, o);
		return null;
	}

	public Object visitParaDecl(ParaDecl ast, Object o) {
		Frame frame = (Frame) o;
		ast.index = frame.getNewIndex();
		String T = VCtoJavaType(ast.T);

		if (ast.T.isArrayType()) {
			ArrayType type = (ArrayType) ast.T;
			T = VCtoJavaType(((ArrayType) ast.T).T);

			//only thing different from below is change T  add [
			emit(JVM.VAR + " " + ast.index + " is " + ast.I.spelling + " [" + T + " from " + (String) frame.scopeStart.peek()
					+ " to " + (String) frame.scopeEnd.peek());
		} else {

			emit(JVM.VAR + " " + ast.index + " is " + ast.I.spelling + " " + T + " from " + (String) frame.scopeStart.peek()
					+ " to " + (String) frame.scopeEnd.peek());
		}
		return null;
	}

	// Arguments
	public Object visitArgList(ArgList ast, Object o) {
		ast.A.visit(this, o);
		ast.AL.visit(this, o);
		return null;
	}

	public Object visitArg(Arg ast, Object o) {
		ast.E.visit(this, o);
		return null;
	}

	// Types
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

	public Object visitArrayType(ArrayType ast, Object o) {
		// should be nothing we dc about type
		return null;
	}

	public Object visitStringType(StringType ast, Object o) {
        // should be nothing we dc about type
		return null;
    }

	public Object visitErrorType(ErrorType ast, Object o) {
		return null;
	}

	// Literals, Identifiers and Operators
	public Object visitIntLiteral(IntLiteral ast, Object o) {
		Frame frame = (Frame) o;
		emitICONST(Integer.parseInt(ast.spelling));
		frame.push();
		return null;
	}

	public Object visitFloatLiteral(FloatLiteral ast, Object o) {
		Frame frame = (Frame) o;
		emitFCONST(Float.parseFloat(ast.spelling));
		frame.push();
		return null;
	}

	public Object visitBooleanLiteral(BooleanLiteral ast, Object o) {
		Frame frame = (Frame) o;
		emitBCONST(ast.spelling.equals("true"));
		frame.push();
		return null;
	}

	public Object visitStringLiteral(StringLiteral ast, Object o) {
		Frame frame = (Frame) o;
		emit(JVM.LDC, "\"" + ast.spelling.replace("\"", "\\\"") + "\"");
		frame.push();
		return null;
	}

	public Object visitIdent(Ident ast, Object o) {
		return null;
	}

	public Object visitOperator(Operator ast, Object o) {
		return null;
	}

	// Variables

	public Object visitSimpleVar(SimpleVar ast, Object o) {
		Frame frame = (Frame) o;
		Decl decl = (Decl) ast.I.decl;
		//see if glob
		if (decl.isGlobalVarDecl()) {
			//static default dump on stack
			emitGETSTATIC(VCtoJavaType(ast.type), ast.I.spelling);
		} else {
			// is local also dump on stack check type tho
			if (decl.T.isFloatType()) {
				emitFLOAD(decl.index);
			} else if (decl.T.isIntType() || decl.T.isBooleanType()) {
				//bool still int
				emitILOAD(decl.index);
			} else {
				emit(JVM.ALOAD, decl.index);
			}
		}
		frame.push();
		return null;
	}

	// Auxiliary methods for byte code generation

	// The following method appends an instruction directly into the JVM
	// Code Store. It is called by all other overloaded emit methods.

	private void emit(String s) {
		//		Commented out by the marker for marking purposes
//		System.out.println(s);
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

	private void emitIF_ICMPCOND(String op, Frame frame) {
		String opcode;

		if (op.equals("i!="))
			opcode = JVM.IF_ICMPNE;
		else if (op.equals("i=="))
			opcode = JVM.IF_ICMPEQ;
		else if (op.equals("i<"))
			opcode = JVM.IF_ICMPLT;
		else if (op.equals("i<="))
			opcode = JVM.IF_ICMPLE;
		else if (op.equals("i>"))
			opcode = JVM.IF_ICMPGT;
		else // if (op.equals("i>="))
			opcode = JVM.IF_ICMPGE;

		String falseLabel = frame.getNewLabel();
		String nextLabel = frame.getNewLabel();

		emit(opcode, falseLabel);
		frame.pop(2);
		emit("iconst_0");
		emit("goto", nextLabel);
		emit(falseLabel + ":");
		emit(JVM.ICONST_1);
		frame.push();
		emit(nextLabel + ":");
	}

	private void emitFCMP(String op, Frame frame) {
		String opcode;

		if (op.equals("f!="))
			opcode = JVM.IFNE;
		else if (op.equals("f=="))
			opcode = JVM.IFEQ;
		else if (op.equals("f<"))
			opcode = JVM.IFLT;
		else if (op.equals("f<="))
			opcode = JVM.IFLE;
		else if (op.equals("f>"))
			opcode = JVM.IFGT;
		else // if (op.equals("f>="))
			opcode = JVM.IFGE;

		String falseLabel = frame.getNewLabel();
		String nextLabel = frame.getNewLabel();

		emit(JVM.FCMPG);
		frame.pop(2);
		emit(opcode, falseLabel);
		emit(JVM.ICONST_0);
		emit("goto", nextLabel);
		emit(falseLabel + ":");
		emit(JVM.ICONST_1);
		frame.push();
		emit(nextLabel + ":");

	}

	private void emitILOAD(int index) {
		if (index >= 0 && index <= 3)
			emit(JVM.ILOAD + "_" + index);
		else
			emit(JVM.ILOAD, index);
	}

	private void emitFLOAD(int index) {
		if (index >= 0 && index <= 3)
			emit(JVM.FLOAD + "_" + index);
		else
			emit(JVM.FLOAD, index);
	}

	private void emitGETSTATIC(String T, String I) {
		emit(JVM.GETSTATIC, classname + "/" + I, T);
	}

	private void emitISTORE(Ident ast) {
		int index;
		if (ast.decl instanceof ParaDecl)
			index = ((ParaDecl) ast.decl).index;
		else
			index = ((LocalVarDecl) ast.decl).index;

		if (index >= 0 && index <= 3)
			emit(JVM.ISTORE + "_" + index);
		else
			emit(JVM.ISTORE, index);
	}

	private void emitFSTORE(Ident ast) {
		int index;
		if (ast.decl instanceof ParaDecl)
			index = ((ParaDecl) ast.decl).index;
		else
			index = ((LocalVarDecl) ast.decl).index;
		if (index >= 0 && index <= 3)
			emit(JVM.FSTORE + "_" + index);
		else
			emit(JVM.FSTORE, index);
	}

	private void emitPUTSTATIC(String T, String I) {
		emit(JVM.PUTSTATIC, classname + "/" + I, T);
	}

	private void emitICONST(int value) {
		if (value == -1)
			emit(JVM.ICONST_M1);
		else if (value >= 0 && value <= 5)
			emit(JVM.ICONST + "_" + value);
		else if (value >= -128 && value <= 127)
			emit(JVM.BIPUSH, value);
		else if (value >= -32768 && value <= 32767)
			emit(JVM.SIPUSH, value);
		else
			emit(JVM.LDC, value);
	}

	private void emitFCONST(float value) {
		if (value == 0.0)
			emit(JVM.FCONST_0);
		else if (value == 1.0)
			emit(JVM.FCONST_1);
		else if (value == 2.0)
			emit(JVM.FCONST_2);
		else
			emit(JVM.LDC, value);
	}

	private void emitBCONST(boolean value) {
		if (value)
			emit(JVM.ICONST_1);
		else
			emit(JVM.ICONST_0);
	}

	private String VCtoJavaType(Type t) {
		if (t.equals(StdEnvironment.booleanType))
			return "Z";
		else if (t.equals(StdEnvironment.intType))
			return "I";
		else if (t.equals(StdEnvironment.floatType))
			return "F";
		else // if (t.equals(StdEnvironment.voidType))
			return "V";
	}

	private String VCtoJavaArr(Type t) {
		Type at = ((ArrayType) t).T;
		if (at.equals(StdEnvironment.booleanType))
			return "boolean";
		else if (at.equals(StdEnvironment.intType))
			return "int";
		else if (at.equals(StdEnvironment.floatType))
			return "float";
		else
			throw new AssertionError("should only get boolean int or float for array type");
	}

}
