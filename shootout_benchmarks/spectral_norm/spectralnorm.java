package shootout_benchmarks.spectral_norm;
/* The Computer Language Benchmarks Game
   https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

   Naive transliteration from Sebastien Loisel's C program
   contributed by Isaac Gouy
*/

public final class spectralnorm
{
  double eval_A(int i, int j) { return 1.0/((i+j)*(i+j+1)/2+i+1); }   
   
  void eval_A_times_u(int N, final double u[], double Au[])
  {
    int i,j;
    for(i=0;i<N;i++)
      {
        Au[i]=0;
        for(j=0;j<N;j++) Au[i]+=eval_A(i,j)*u[j];
      }
  }    
  
  void eval_At_times_u(int N, final double u[], double Au[])
  {
    int i,j;
    for(i=0;i<N;i++)
      {
        Au[i]=0;
        for(j=0;j<N;j++) Au[i]+=eval_A(j,i)*u[j];
      }
  }  
  
  void eval_AtA_times_u(int N, final double u[], double AtAu[])
  { var v = new double[N]; eval_A_times_u(N,u,v); eval_At_times_u(N,v,AtAu); }  
  
  public static void main(String[] args) 
  {
    int i;    
    final int N = args.length > 0 ? Integer.parseInt(args[0]) : 100; 
    var nonStatic = new spectralnorm();
    var u = new double[N];      
    var v = new double[N];  
    double vBv, vv;        
    for (i=0; i<N; i++) u[i] = 1;    
    for(i=0; i<10; i++)
    {
      nonStatic.eval_AtA_times_u(N,u,v);
      nonStatic.eval_AtA_times_u(N,v,u);
    }        
    vBv = vv = 0;
    for(i=0; i<N; i++) { vBv += u[i]*v[i]; vv += v[i]*v[i]; }          
    System.out.printf("%.9f\n", Math.sqrt(vBv/vv));  
  }
}  