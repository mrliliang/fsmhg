package com.liang.fsmhg;

public abstract class Vertex {
    private int id;

    public Vertex(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }
}
