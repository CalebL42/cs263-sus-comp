package tests.shootout_binary_trees;
/* The Computer Language Benchmarks Game
   https://salsa.debian.org/benchmarksgame-team/benchmarksgame/
 
   contributed by Isaac Gouy
*/

final class Tree {
    Tree left, right;
       
    Tree(Tree left, Tree right){
       this.left = left;
       this.right = right;
    }      
       
    static Tree with(int depth){
       return (depth == 0) 
          ? new Tree(null, null)
          : new Tree(with(depth-1), with(depth-1));
    }
             
    int nodeCount(){
       return (left == null)
          ? 1 
          : 1 + left.nodeCount() + right.nodeCount();
    }
    
    void clear(){
       if (left != null) {
          left.clear(); 
          left = null;
          right.clear();
          right = null;         
       }
    }   
 }
 
 final class shootout_binary_trees {   
    public static void main(String[] args){
       int n = 10;
       if (args.length > 0) n = Integer.parseInt(args[0]);
 
       int minDepth = 4;
       int maxDepth = (minDepth + 2 > n) ? minDepth + 2 : n;
       int stretchDepth = maxDepth + 1;
       
       stretch(stretchDepth);
       var longLivedTree = Tree.with(maxDepth);
       
       for (int depth = minDepth; depth <=maxDepth; depth += 2) {
          int iterations = 1 << (maxDepth - depth + minDepth);
          int sum = 0;
          for (int i = 1; i <= iterations; i++){
             sum += count(depth);          
          }
          System.out.println(iterations + "\t trees of depth " 
                + depth + "\t check: " + sum);
       }   
       int count = longLivedTree.nodeCount();    
       longLivedTree.clear();       
       System.out.println("long lived tree of depth " 
             + maxDepth + "\t check: "+ count);              
    }
    
    static void stretch(int depth){         
       System.out.println("stretch tree of depth " 
             + depth +"\t check: " + count(depth));   
    }
    
    static int count(int depth){
       var t = Tree.with(depth);         
       int c = t.nodeCount();
       t.clear(); 
       return c;
    }   
 }   