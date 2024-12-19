=== test program === 
boolean b[2];
int f() { return 1; }
int g() { return 2; }
int main() {
int a[] = {f(), g()};
float f[] = {a[0], 2.0};
b[0] = a[0] <= f[0] && b[1] || (f[1] + a[1] >= 100);
putBoolLn(b[0]);
putBoolLn(b[1]);
return 0;
}

======= The VC compiler =======

Pass 1: Lexical and syntactic Analysis
Pass 2: Semantic Analysis
Pass 3: Code Generation

Compilation was successful.

=== The output of the test program === 
false
false
