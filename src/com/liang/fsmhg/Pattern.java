package com.liang.fsmhg;

import com.liang.fsmhg.code.DFSEdge;
import com.liang.fsmhg.graph.LabeledGraph;
import com.liang.fsmhg.graph.Snapshot;

import java.util.*;

public class Pattern {

    private DFSEdge edge;
    private Pattern parent;

    private List<Cluster> clusters;

    private Map<Long, List<Embedding>> embeddingMap;
    private Map<Cluster, List<Embedding>> intersectionEmbeddings;
    private Map<Cluster, List<Embedding>> borderEmbeddings;

    public Pattern(DFSEdge edge, Pattern parent) {
        this.edge = edge;
        this.parent = parent;
        this.clusters = new ArrayList<>();

        embeddingMap = new TreeMap<>();
        intersectionEmbeddings = new TreeMap<>();
        borderEmbeddings = new TreeMap<>();
    }

    public DFSEdge edge() {
        return edge;
    }

    public Pattern parent() {
        return parent;
    }

    public int support() {
        return embeddingMap.size();
    }

    public void addCluster(Cluster c) {
        clusters.add(c);
        for (LabeledGraph g : c.snapshots()) {
            if (!embeddingMap.containsKey(g.graphId())) {
                embeddingMap.put(g.graphId(), new ArrayList<>());
            }
        }
    }

    public List<Embedding> embeddings(long graphId) {
        return embeddingMap.get(graphId);
    }

    public void addEmbedding(long graphId, Embedding em) {
        List<Embedding> embeddings = embeddingMap.computeIfAbsent(graphId, aLong -> new ArrayList<>());
        embeddings.add(em);
    }

    public void addIntersectionEmbedding(Cluster c, Embedding em) {
        List<Embedding> embeddings = intersectionEmbeddings.computeIfAbsent(c, aLong -> new ArrayList<>());
        embeddings.add(em);
    }

    public void addBorderEmbedding(Cluster c, Embedding em) {
        List<Embedding> embeddings = borderEmbeddings.computeIfAbsent(c, aLong -> new ArrayList<>());
        embeddings.add(em);
    }
}
