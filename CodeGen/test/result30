=== test program === 
void f(float a[]) {
  float tmp;
  tmp = a[1];
  a[1] = a[0];
  a[0] = tmp;
  }
int main() {
  float a[] = {1.0, 2.0};
  f(a);
  putFloatLn(a[0]);
  putFloatLn(a[1]);
  return 0;
}

======= The VC compiler =======

Pass 1: Lexical and syntactic Analysis
Pass 2: Semantic Analysis
Pass 3: Code Generation

Compilation was successful.

=== The output of the test program === 
2.0
1.0
