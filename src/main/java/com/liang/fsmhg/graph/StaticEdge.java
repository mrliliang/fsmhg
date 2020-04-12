package com.liang.fsmhg.graph;

public class StaticEdge extends LabeledEdge {
    private int eLabel;

    public StaticEdge(StaticVertex from, StaticVertex to, int eLabel) {
        super(from, to);
        this.eLabel = eLabel;
    }

    public int label() {
        return eLabel;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("e ")
                .append(from().id())
                .append(' ')
                .append(to().id())
                .append(' ')
                .append(eLabel);
        return builder.toString();
    }

    @Override
    public int label(long time) {
        return eLabel;
    }
}
