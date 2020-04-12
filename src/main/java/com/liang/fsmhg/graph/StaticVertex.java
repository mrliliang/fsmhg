package com.liang.fsmhg.graph;

public class StaticVertex extends LabeledVertex {
    private int label;

    public StaticVertex(int id) {
        super(id);
    }

    @Override
    public int label(long time) {
        return label;
    }

    public StaticVertex(int id, int label) {
        super(id);
        this.label = label;
    }

    public int label() {
        return label;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("v ")
                .append(id())
                .append(' ')
                .append(label);
        return builder.toString();
    }
}