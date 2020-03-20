package com.liang.fsmhg.code;

public class DFSEdge implements Comparable<DFSEdge> {
    private int from;
    private int to;
    private int fromLabel;
    private int toLabel;
    private int edgeLabel;


    public DFSEdge(int from, int to, int fromLabel, int toLabel, int eLabel) {
        this.from = from;
        this.to = to;
        this.fromLabel = fromLabel;
        this.toLabel = toLabel;
        this.edgeLabel = eLabel;
    }

    public boolean isForward() {
        return from < to;
    }

    public int from() {
        return from;
    }

    public int to() {
        return to;
    }

    public int fromLabel() {
        return fromLabel;
    }

    public int toLabel() {
        return toLabel;
    }

    public int edgeLabel() {
        return edgeLabel;
    }

    @Override
    public int compareTo(DFSEdge other) {
        if (!this.isForward() && other.isForward()) {
            return -1;
        } else if (this.isForward() && !other.isForward()) {
            return 1;
        }

        if (!this.isForward() && !this.isForward()) {
            if (this.to < other.to) {
                return -1;
            } else if (this.to > other.to) {
                return 1;
            }
            if (this.edgeLabel < other.edgeLabel) {
                return -1;
            } else if (this.edgeLabel > other.edgeLabel) {
                return 1;
            }

            return 0;
        }


        if (this.from > other.from) {
            return -1;
        } else if (this.from < other.from) {
            return 1;
        }

        if (this.fromLabel < other.fromLabel) {
            return -1;
        } else if (this.fromLabel > other.fromLabel) {
            return 1;
        }

        if (this.edgeLabel < other.edgeLabel) {
            return -1;
        } else if (this.edgeLabel > other.edgeLabel) {
            return 1;
        }

        if (this.toLabel < other.toLabel) {
            return -1;
        } else if (this.toLabel > other.toLabel) {
            return 1;
        }

        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof DFSEdge) {
            DFSEdge other = (DFSEdge)obj;
            return from == other.from
                    && to == other.to
                    && fromLabel == other.fromLabel
                    && edgeLabel == other.edgeLabel
                    && toLabel == other.toLabel;
        }

        return false;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('(')
                .append(from)
                .append(',')
                .append(to)
                .append(',')
                .append(fromLabel)
                .append(',')
                .append(edgeLabel)
                .append(',')
                .append(toLabel)
                .append(')');
        return builder.toString();
    }
}
