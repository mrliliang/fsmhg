package com.liang.fsmhg;

public class StaticEdge extends Edge<StaticVertex> {
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
}
