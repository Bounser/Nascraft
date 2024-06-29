package me.bounser.nascraft.managers;


public class GraphManager {

    private final int width = 363, height = 126;
    private final int offsetX = 10, offsetY = 54;


    private static GraphManager instance;

    public static GraphManager getInstance() { return instance == null ? instance = new GraphManager() : instance; }

    public int[] getSize() { return new int[]{width, height}; }
    public int[] getOffset() { return new int[]{offsetX, offsetY}; }

}
