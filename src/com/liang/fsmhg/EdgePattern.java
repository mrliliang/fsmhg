package com.liang.fsmhg;

import com.liang.fsmhg.code.DFSEdge;

public class EdgePattern extends Pattern {

    private DFSEdge edge;
    private PointPattern pointParent;
    private EdgePattern edgeParent;

    public EdgePattern(DFSEdge edge, PointPattern parent) {
        this.edge = edge;
        this.pointParent = parent;
    }

    public EdgePattern(DFSEdge edge, EdgePattern pattern) {
        this.edgeParent = pattern;
    }

    public PointPattern pointParent() {
        return pointParent;
    }

    public EdgePattern edgeParent() {
        return edgeParent;
    }
}
