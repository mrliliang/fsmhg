package com.liang.fsmhg.graph;

import java.util.Map;

public class Generator {

    private int maxEntity;
    private int vLabelNum;
    private int eLabelNum;
    private int snapshotNum;
    private int maxDegree;

    private Map<Integer, LabeledVertex> vertexMap;
    private Map<Integer, AdjEdges> edgesMap;

    private Map<Integer, LabeledVertex> coreVertex;
    private Map<Integer, AdjEdges> coreEdges;

    private Map<Integer, LabeledVertex> marginVertex;
    private Map<Integer, AdjEdges> marginEdges;

    public void evolve() {

    }

}
