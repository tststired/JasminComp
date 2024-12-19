.class public /home/z/Storage/compilers/VC/CodeGen/test/test2
.super java/lang/Object
	
.field static f V
	
	; standard class static initializer 
.method static <clinit>()V
	
	iconst_2
	newarray V
	iconst_0
	putstatic /home/z/Storage/compilers/VC/CodeGen/test/test2/f V
	
	; set limits used by this method
.limit locals 0
.limit stack 3
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
.var 1 is vc$ L/home/z/Storage/compilers/VC/CodeGen/test/test2; from L0 to L1
	new /home/z/Storage/compilers/VC/CodeGen/test/test2
	dup
	invokenonvirtual /home/z/Storage/compilers/VC/CodeGen/test/test2/<init>()V
	astore_1
	getstatic /home/z/Storage/compilers/VC/CodeGen/test/test2/f F
	iconst_0
	faload
	invokestatic VC/lang/System/putFloatLn(F)V
	getstatic /home/z/Storage/compilers/VC/CodeGen/test/test2/f F
	iconst_1
	faload
	invokestatic VC/lang/System/putFloatLn(F)V
	return
L1:
	return
	
	; set limits used by this method
.limit locals 2
.limit stack 5
.end method
