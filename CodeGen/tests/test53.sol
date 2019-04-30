=== test program === 
/* fibo.c -- It prints out the first N Fibonacci
 *           numbers.
 */

int main() {
    int n;        /* The number of fibonacci numbers we will print */
    int i;        /* The index of fibonacci number to be printed next */ 
    int current;  /* The value of the (i)th fibonacci number */
    int next;     /* The value of the (i+1)th fibonacci number */
    int twoaway;  /* The value of the (i+2)th fibonacci number */

    putString("How many Fibonacci numbers do you want to compute? ");
    n = 10; // n = getInt();
    if (n<=0)
       putString("The number should be positive.\n");
    else {
      putString("\n\n\tI \t Fibonacci(I) \n\t=====================\n");
      next = current = 1;
      for (i=1; i<=n; i=i+1) {
	putString("\t");
        putInt(i);
	putString("\t");
  	putIntLn(current);
	twoaway = current+next;
	current = next;
	next    = twoaway;
      }
    }
    return 0;
}

/* The output from a run of this program was:

How many Fibonacci numbers do you want to compute? 9

	I 	 Fibonacci(I) 
	=====================
	1 	   1
	2 	   1
	3 	   2
	4 	   3
	5 	   5
	6 	   8
	7 	   13
	8 	   21
	9 	   34

*/


======= The VC compiler =======

Pass 1: Lexical and syntactic Analysis
Pass 2: Semantic Analysis
Pass 3: Code Generation

Compilation was successful.

=== The output of the test program === 
How many Fibonacci numbers do you want to compute? 

	I 	 Fibonacci(I) 
	=====================
	1	1
	2	1
	3	2
	4	3
	5	5
	6	8
	7	13
	8	21
	9	34
	10	55
