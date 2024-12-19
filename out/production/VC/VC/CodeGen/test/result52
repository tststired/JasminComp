=== test program === 
boolean even(int n) {
  return (n/2) * 2 == 2;
}
int pow(int x, int n) {
  if (n == 0) 
    return 1;
  else if (n == 1)
    return x;
  else if (even(n))
    return pow( x * x, n/2);
  else
    return pow( x * x, n/2) * x;
}
int main() {  
  putIntLn(pow(2, 10));
  return 0;
}

======= The VC compiler =======

Pass 1: Lexical and syntactic Analysis
Pass 2: Semantic Analysis
Pass 3: Code Generation

Compilation was successful.

=== The output of the test program === 
2048
