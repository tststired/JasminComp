=== test program === 
int h() { return 1; }
void g(int i, float f, boolean b) { 
  putIntLn(i); 
  putFloatLn(f); 
  putBoolLn(b); 
  }
void f(int i, float f) { 
  putIntLn(i); 
  putFloatLn(f); 
  g( i = i + 1, f + 1 + h(), true);
  }
int main() {
  int i = 1;
  int j = 2;
  f(i, j);
  return 0;
}

======= The VC compiler =======

Pass 1: Lexical and syntactic Analysis
Pass 2: Semantic Analysis
Pass 3: Code Generation

Compilation was successful.

=== The output of the test program === 
1
2.0
2
4.0
true
