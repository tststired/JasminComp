.class public /home/z/Storage/compilers/VC/CodeGen/test/test1
.super java/lang/Object
	
.field static i I
.field static b V
	
	; standard class static initializer 
.method static <clinit>()V
	
	bipush 100
	putstatic /home/z/Storage/compilers/VC/CodeGen/test/test1/i I
	iconst_2
	newarray BOOLEAN
	dup
	bipush 0
	iconst_1
	bastore
	dup
	bipush 1
	iconst_0
	bastore
	putstatic /home/z/Storage/compilers/VC/CodeGen/test/test1/b V
	
	; set limits used by this method
.limit locals 0
.limit stack 5
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
.method public static main([Ljava/lang/String;)V
L0:
.var 0 is argv [Ljava/lang/String; from L0 to L1
.var 1 is vc$ L/home/z/Storage/compilers/VC/CodeGen/test/test1; from L0 to L1
	new /home/z/Storage/compilers/VC/CodeGen/test/test1
	dup
	invokenonvirtual /home/z/Storage/compilers/VC/CodeGen/test/test1/<init>()V
	astore_1
	getstatic /home/z/Storage/compilers/VC/CodeGen/test/test1/i I
	invokestatic VC/lang/System/putIntLn(I)V
	getstatic /home/z/Storage/compilers/VC/CodeGen/test/test1/b Z
	iconst_0
	baload
	invokestatic VC/lang/System/putBoolLn(Z)V
	getstatic /home/z/Storage/compilers/VC/CodeGen/test/test1/b Z
	iconst_1
	baload
	invokestatic VC/lang/System/putBoolLn(Z)V
	return
L1:
	return
	
	; set limits used by this method
.limit locals 2
.limit stack 5
.end method
