package com.liang.fsmhg;

import com.liang.fsmhg.code.DFSCode;
import com.liang.fsmhg.code.DFSEdge;
import com.liang.fsmhg.graph.Graph;
import com.liang.fsmhg.graph.LabeledGraph;
import com.liang.fsmhg.graph.Snapshot;

import java.util.*;
import java.util.function.Function;

public class Pattern {

    private DFSEdge edge;
    private Pattern parent;

    private TreeMap<LabeledGraph, List<Embedding>> embeddingMap;
    private TreeMap<Cluster, List<Embedding>> intersectionEmbeddings;
    private TreeMap<Cluster, List<Embedding>> borderEmbeddings;

    private TreeMap<DFSEdge, Pattern> children;

    private boolean isMinChecked;
    private boolean minCheckResult;

    public Pattern(DFSEdge edge, Pattern parent) {
        this.edge = edge;
        this.parent = parent;

        embeddingMap = new TreeMap<>();
        intersectionEmbeddings = new TreeMap<>();
        borderEmbeddings = new TreeMap<>();

        children = new TreeMap<>();
    }

    public DFSEdge edge() {
        return edge;
    }

    public Pattern parent() {
        return parent;
    }

    public int frequency() {
        return embeddingMap.size();
    }

    public DFSCode code() {
        if (parent instanceof PointPattern) {
            DFSCode code = new DFSCode();
            code.add(edge);
            return code;
        }

        DFSCode code = parent.code();
        code.add(edge);
        return code;
    }

    public boolean checkMin() {
        if (!isMinChecked) {
            minCheckResult = code().isMin();
            isMinChecked = true;
        }
        return minCheckResult;
    }

    public List<LabeledGraph> unClusteredGraphs() {
        TreeSet<LabeledGraph> graphs = new TreeSet<>(embeddingMap.keySet());
        TreeSet<Cluster> clusters = new TreeSet<>(intersectionEmbeddings.keySet());
        clusters.addAll(borderEmbeddings.keySet());
        for (Cluster c : clusters) {
            graphs.removeAll(c.snapshots());
        }

        return new ArrayList<>(graphs);
    }

    public List<LabeledGraph> unClusteredGraphs(LabeledGraph graphDelimiter) {
        TreeSet<LabeledGraph> graphs = new TreeSet<>(embeddingMap.tailMap(graphDelimiter).keySet());
        TreeSet<Cluster> clusters = new TreeSet<>(intersectionEmbeddings.keySet());
        clusters.addAll(borderEmbeddings.keySet());
        for (Cluster c : clusters) {
            graphs.removeAll(c.snapshots());
        }

        return new ArrayList<>(graphs);
    }

    public List<Cluster> clusters() {
        TreeSet<Cluster> clusters = new TreeSet<>(intersectionEmbeddings.keySet());
        clusters.addAll(borderEmbeddings.keySet());
        return new ArrayList<>(clusters);
    }

    public List<Cluster> clusters(Cluster clusterDelimiter) {
        TreeSet<Cluster> clusters = new TreeSet<>(intersectionEmbeddings.tailMap(clusterDelimiter).keySet());
        clusters.addAll(borderEmbeddings.tailMap(clusterDelimiter).keySet());
        return new ArrayList<>(clusters);
    }

    public List<Embedding> embeddings(LabeledGraph graph) {
        List<Embedding> embeddings = this.embeddingMap.get(graph);
        if (embeddings == null) {
            embeddings = new ArrayList<>(0);
        }
        return embeddings;
    }

    public List<Embedding> intersectionEmbeddings(Cluster c) {
        List<Embedding> embeddings = intersectionEmbeddings.get(c);
        if (embeddings == null) {
            embeddings = new ArrayList<>(0);
        }
        return embeddings;
    }

    public List<Embedding> borderEmbeddings(Cluster c) {
        List<Embedding> embeddings = borderEmbeddings.get(c);
        if (embeddings == null) {
            embeddings = new ArrayList<>(0);
        }
        return embeddings;
    }

    public void addEmbedding(LabeledGraph g, Embedding em) {
        List<Embedding> embeddings = embeddingMap.computeIfAbsent(g, aLong -> new ArrayList<>());
        embeddings.add(em);
    }

    public void addIntersectionEmbedding(Cluster c, Embedding em) {
        List<Embedding> embeddings = intersectionEmbeddings.computeIfAbsent(c, key -> {
            for (LabeledGraph g : key.snapshots()) {
                if (!embeddingMap.containsKey(g)) {
                    embeddingMap.put(g, new ArrayList<>());
                }
            }
            return new ArrayList<>();
        });
        embeddings.add(em);
    }

    public void addBorderEmbedding(Cluster c, Embedding em) {
        List<Embedding> embeddings = borderEmbeddings.computeIfAbsent(c, key -> {
            for (LabeledGraph g : key.snapshots()) {
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

    public List<Pattern> children() {
        List<Pattern> patterns = new ArrayList<>();
        patterns.addAll(this.children.values());
        return patterns;
    }

    public void remove(Collection<LabeledGraph> graphs, Collection<Cluster> clusters) {
        List<LabeledGraph> removedGraphs = new ArrayList<>();
        for (LabeledGraph g : graphs) {
            if (embeddingMap.remove(g) != null) {
                removedGraphs.add(g);
            }
        }
        if (removedGraphs.isEmpty()) {
            return;
        }

        List<Cluster> removedClusters = new ArrayList<>();
        for (Cluster c : clusters) {
            if (intersectionEmbeddings.remove(c) != null || borderEmbeddings.remove(c) != null) {
                removedClusters.add(c);
            }
        }

        for (Pattern p : children()) {
            remove(removedGraphs, removedClusters);
        }
    }

}
