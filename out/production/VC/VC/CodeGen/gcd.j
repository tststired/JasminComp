.class public CodeGen/gcd
.super java/lang/Object
	
.field static i I
.field static j I
	
	; standard class static initializer 
.method static <clinit>()V
	
	iconst_0
	putstatic CodeGen/gcd/i I
	iconst_0
	putstatic CodeGen/gcd/j I
	
	; set limits used by this method
.limit locals 0
.limit stack 1
	return
.end method
	
	; standard constructor initializer 
.method public <init>()V
.limit stack 1
.limit locals 1
	aload_0
	invokespecial java/lang/Object/<init>()V
	return
.end method
.method gcd(II)I
L0:
.var 0 is this LCodeGen/gcd; from L0 to L1
.var 1 is a I from L0 to L1
.var 2 is b I from L0 to L1
	iload_2
	iconst_0
	if_icmpeq L4
	iconst_0
	goto L5
L4:
	iconst_1
L5:
	ifeq L2
	iload_1
	ireturn
	goto L3
L2:
	aload_0
	iload_2
	iload_1
	iload_1
	iload_2
	idiv
	iload_2
	imul
	isub
	invokevirtual CodeGen/gcd/gcd(II)I
	ireturn
L3:
L1:
	nop
	
	; set limits used by this method
.limit locals 3
.limit stack 5
.end method
.method public static main([Ljava/lang/String;)V
L0:
.var 0 is argv [Ljava/lang/String; from L0 to L1
.var 1 is vc$ LCodeGen/gcd; from L0 to L1
	new CodeGen/gcd
	dup
	invokenonvirtual CodeGen/gcd/<init>()V
	astore_1
	invokestatic VC/lang/System.getInt()I
	putstatic CodeGen/gcd/i I
	invokestatic VC/lang/System.getInt()I
	putstatic CodeGen/gcd/j I
	aload_1
	getstatic CodeGen/gcd/i I
	getstatic CodeGen/gcd/j I
	invokevirtual CodeGen/gcd/gcd(II)I
	invokestatic VC/lang/System/putIntLn(I)V
	return
L1:
	return
	
	; set limits used by this method
.limit locals 2
.limit stack 5
.end method
