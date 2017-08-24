package com.example.alan.a0811whacamole;

/**
 * Created by Alan on 2017/8/23.
 */
public class Hole {
    private int holeId;
    private int holeX;
    private int holeY;

    public Hole(int hole) {
        this.holeId = hole;
    }

    public int getHoleId() {
        return holeId;
    }

    public void setHole(int hole) {
        this.holeId = hole;
    }

    public int getHoleX() {
        return holeX;
    }

    public void setHoleX(int holeX) {
        this.holeX = holeX;
    }

    public int getHoleY() {
        return holeY;
    }

    public void setHoleY(int holeY) {
        this.holeY = holeY;
    }
}