package com.liang.fsmhg;

import com.liang.fsmhg.code.DFSEdge;
import com.liang.fsmhg.graph.LabeledGraph;
import com.liang.fsmhg.graph.Snapshot;

import java.util.*;
import java.util.function.Function;

public class Pattern {

    private DFSEdge edge;
    private Pattern parent;

    private Map<LabeledGraph, List<Embedding>> embeddingMap;
    private Map<Cluster, List<Embedding>> intersectionEmbeddings;
    private Map<Cluster, List<Embedding>> borderEmbeddings;

    private Map<DFSEdge, Pattern> children;

    public Pattern(DFSEdge edge, Pattern parent) {
        this.edge = edge;
        this.parent = parent;

        embeddingMap = new TreeMap<>();
        intersectionEmbeddings = new LinkedHashMap<>();
        borderEmbeddings = new LinkedHashMap<>();

        children = new TreeMap<>();
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

    public List<LabeledGraph> unClusteredGraphs() {
        List<LabeledGraph> graphs = new ArrayList<>(embeddingMap.keySet());
        for (Cluster c : intersectionEmbeddings.keySet()) {
            graphs.removeAll(c.snapshots());
        }

        for (Cluster c : borderEmbeddings.keySet()) {
            if (!intersectionEmbeddings.containsKey(c)) {
                graphs.removeAll(c.snapshots());
            }
        }
        return graphs;
    }

    public List<Embedding> embeddings(long graphId) {
        return embeddingMap.get(graphId);
    }

    public List<Embedding> intersectionEmbeddings(Cluster c) {
        return intersectionEmbeddings.get(c);
    }

    public List<Embedding> borderEmbeddings(Cluster c) {
        return borderEmbeddings.get(c);
    }

    public void addEmbedding(LabeledGraph g, Embedding em) {
        List<Embedding> embeddings = embeddingMap.computeIfAbsent(g, aLong -> new ArrayList<>());
        embeddings.add(em);
    }

    public void addIntersectionEmbedding(Cluster c, Embedding em) {
        List<Embedding> embeddings = intersectionEmbeddings.computeIfAbsent(c, c1 -> {
            for (LabeledGraph g : c1.snapshots()) {
                if (!embeddingMap.containsKey(g)) {
                    embeddingMap.put(g, new ArrayList<>());
                }
            }
            return new ArrayList<>();
        });
        embeddings.add(em);
    }

    public void addBorderEmbedding(Cluster c, Embedding em) {
        List<Embedding> embeddings = borderEmbeddings.computeIfAbsent(c, c1 -> {
            for (LabeledGraph g : c1.snapshots()) {
                if (!embeddingMap.containsKey(g)) {
                    embeddingMap.put(g, new ArrayList<>());
                }
            }
            return new ArrayList<>();
        });
        embeddings.add(em);
    }

    public Pattern child(int from, int to, int fromLabel, int toLabel, int eLabel) {
        DFSEdge e = new DFSEdge(from, to, fromLabel, toLabel, eLabel);
        return children.get(e);
    }

    public Pattern addChild(int from, int to, int fromLabel, int toLabel, int eLabel) {
        DFSEdge e = new DFSEdge(from, to, fromLabel, toLabel, eLabel);
        Pattern child = new Pattern(e, this);
        children.put(e, new Pattern(e, this));
        return child;
    }
}
