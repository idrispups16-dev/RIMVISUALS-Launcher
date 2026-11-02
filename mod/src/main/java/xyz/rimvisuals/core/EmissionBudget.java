package xyz.rimvisuals.core;
/** Allocation-free shared per-tick cap for optional particles. */
public final class EmissionBudget {
    private int used;
    public int remaining(int cap){return Math.max(0,cap-used);}
    public void reset(){used=0;}
    public int take(int requested,int cap){int n=Math.max(0,Math.min(requested,cap-used));used+=n;return n;}
    public void refund(int count){used=Math.max(0,used-Math.max(0,count));}
}
