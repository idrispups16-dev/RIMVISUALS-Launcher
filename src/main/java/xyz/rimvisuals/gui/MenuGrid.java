package xyz.rimvisuals.gui;

/** Pure geometry shared by drawing and hit regions; no render-scale rounding here. */
public final class MenuGrid {
 private MenuGrid() {}
 public static int columns(int width,int requested){return Math.max(1,Math.min(Math.max(1,Math.min(3,requested)),(width+10)/180));}
 public static int width(int available,int columns){return Math.max(1,(available-(columns-1)*10)/columns);}
 public static int shortest(int[] heights){int result=0;for(int i=1;i<heights.length;i++)if(heights[i]<heights[result])result=i;return result;}
}
