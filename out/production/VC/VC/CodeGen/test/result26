=== test program === 
boolean f() { putIntLn(111); return true; } // called
boolean g() { putIntLn(222); return true; } // called
int main() {
putBoolLn(true && f() && g());
return 0;
}

======= The VC compiler =======

Pass 1: Lexical and syntactic Analysis
Pass 2: Semantic Analysis
Pass 3: Code Generation

Compilation was successful.

=== The output of the test program === 
111
222
true
