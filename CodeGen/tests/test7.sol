=== test program === 
int main() {
int a1[] = {1};
int a2[2] = {22, 33};
int a3[3];
a3[0] = a3[1] = a3[2] = a1[0] + 1;
a3[2] = a1[0] + 1;
a3[1] = a2[0] * a2[1];
a3[0] = a3[2];
putIntLn(a1[0]);
putIntLn(a2[0]);
putIntLn(a2[1]);
putIntLn(a3[0]);
putIntLn(a3[1]);
putIntLn(a3[2]);
return 0;
}

======= The VC compiler =======

Pass 1: Lexical and syntactic Analysis
Pass 2: Semantic Analysis
Pass 3: Code Generation

Compilation was successful.

=== The output of the test program === 
1
22
33
2
726
2
