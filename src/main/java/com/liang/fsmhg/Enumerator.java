package com.liang.fsmhg;

import com.liang.fsmhg.graph.LabeledGraph;

import java.io.File;
import java.util.List;

public interface Enumerator {
    void enumerate(List<LabeledGraph> trans);

    void setOutput(File out);
}
