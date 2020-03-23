package com.liang.fsmhg.enumerator;

import com.liang.fsmhg.Pattern;
import com.liang.fsmhg.PatternTree;
import com.liang.fsmhg.graph.LabeledGraph;

import java.util.List;

public abstract class EnumDFS extends Enumerator {

    public EnumDFS(Operation op) {
        super(op);
    }

    @Override
    public PatternTree enumerate(List<? extends LabeledGraph> graphs, double minSupport, double similarity, int maxEdgeNum) {

        return null;
    }

}
