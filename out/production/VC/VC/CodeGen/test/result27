=== test program === 
void f() { putIntLn(1); } // called
int g() { putIntLn(2); return 1; }  // not called
int main() {
int i = 1;
if (i == 1)
  f();
else 
  g();
return 0;
}

======= The VC compiler =======

Pass 1: Lexical and syntactic Analysis
Pass 2: Semantic Analysis
Pass 3: Code Generation

Compilation was successful.

=== The output of the test program === 
1
