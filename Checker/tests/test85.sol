======= The VC compiler =======

Pass 1: Lexical and syntactic Analysis
Pass 2: Semantic Analysis
ERROR: 25(24)..25(24): *25: too many actual parameters
Compilation was unsuccessful.
/* prime.c  It prompts the user to enter an integer N. It prints out
 *           if it is a prime or not. If not, it prints out a factor of N.
 */

int main() {
  int n;
  int i;
  boolean flag = true;

  putString("Enter value of N: ");
  n = getInt();
 
 for (i=2; (i<(n/2)) && flag; ) {
    if ( ((n / i) * i) == n) 
      flag = false;
    else
      i = i + 1;
  }
 
  if (flag) {
    putInt(n);
    putStringLn(" is prime");
  } else {
    putInt(n);
    putString(" has ", 1);  // error
    putInt(i);
    putStringLn(" as a factor");
  }
  return 0;
}
