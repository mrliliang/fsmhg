package com.liang.fsmhg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import com.liang.fsmhg.code.DFSCode;
import com.liang.fsmhg.code.DFSEdge;
import com.liang.fsmhg.graph.LabeledGraph;

public class Pattern {

    private DFSEdge edge;
    private Pattern parent;

    private HashMap<LabeledGraph, List<Embedding>> embeddingMap;
    private HashMap<Cluster, List<Embedding>> intersectionEmbeddings;
    private HashMap<Cluster, List<Embedding>> borderEmbeddings;

    private TreeMap<DFSEdge, Pattern> children;

    private boolean isMinChecked;
    private boolean minCheckResult;

    private Cluster clusterDelimiter;
    private LabeledGraph graphDelimiter;

    private int support = 0;


    public Pattern(DFSEdge edge, Pattern parent) {
        this.edge = edge;
        this.parent = parent;

        embeddingMap = new HashMap<>();
        intersectionEmbeddings = new HashMap<>();
        borderEmbeddings = new HashMap<>();

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
        // return this.support;
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
        HashSet<LabeledGraph> graphs = new HashSet<>(embeddingMap.keySet());
        HashSet<Cluster> clusters = new HashSet<>(intersectionEmbeddings.keySet());
        clusters.addAll(borderEmbeddings.keySet());
        for (Cluster c : clusters) {
            graphs.removeAll(c.snapshots());
        }

        return new ArrayList<>(graphs);
    }

    public List<LabeledGraph> unClusteredGraphsAfterDelimiter() {
        if (graphDelimiter == null) {
            return unClusteredGraphs();
        }
        HashSet<LabeledGraph> graphs = new HashSet<>(embeddingMap.keySet());
        HashSet<Cluster> clusters = new HashSet<>(intersectionEmbeddings.keySet());
        clusters.addAll(borderEmbeddings.keySet());
        for (Cluster c : clusters) {
            graphs.removeAll(c.snapshots());
        }

        return new ArrayList<>(new TreeSet<>(graphs).tailSet(graphDelimiter, false));
    }

    public List<Cluster> clusters() {
        HashSet<Cluster> clusters = new HashSet<>(intersectionEmbeddings.keySet());
        clusters.addAll(borderEmbeddings.keySet());
        return new ArrayList<>(clusters);
    }

    public List<Cluster> clustersAfterDelimiter() {
        // if (clusterDelimiter == null) {
        // return new ArrayList<>();
        // }
        if (clusterDelimiter == null) {
            return clusters();
        }
        HashSet<Cluster> clusters = new HashSet<>(intersectionEmbeddings.keySet());
        clusters.addAll(borderEmbeddings.keySet());
        return new ArrayList<>(new TreeSet<>(clusters).tailSet(clusterDelimiter, false));
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
        List<Embedding> embeddings = embeddingMap.computeIfAbsent(g, labeledGraph -> {
            // this.support++;
            return new ArrayList<>();
        });
        embeddings.add(em);
    }

    public void addEmbedding(LabeledGraph g, Cluster c, Embedding em) {
        List<Embedding> embeddings = embeddingMap.computeIfAbsent(g, labeledGraph -> {
            // if (!intersectionEmbeddings.containsKey(c)) {
            //     this.support++;
            // }
            return new ArrayList<>();
        });
        embeddings.add(em);
    }

    public void addIntersectionEmbedding(Cluster c, Embedding em) {
        List<Embedding> embeddings = intersectionEmbeddings.computeIfAbsent(c, key -> {
            for (LabeledGraph g : key.snapshots()) {
                embeddingMap.putIfAbsent(g, new ArrayList<>());
            }
            // this.support += c.size();
            return new ArrayList<>();
        });
        embeddings.add(em);
    }

    public void addBorderEmbedding(Cluster c, Embedding em) {
        List<Embedding> embeddings = borderEmbeddings.computeIfAbsent(c, key -> {
            for (LabeledGraph g : key.snapshots()) {
                embeddingMap.putIfAbsent(g, new ArrayList<>());
            }
            return new ArrayList<>();
        });
        embeddings.add(em);
    }

    public Pattern child(DFSEdge e) {
        Pattern child = children.get(e);
        if (child == null) {
            child = new Pattern(e, this);
            children.put(e, child);
        }
        return child;
    }

    public Pattern child(int from, int to, int fromLabel, int toLabel, int eLabel) {
        DFSEdge e = new DFSEdge(from, to, fromLabel, toLabel, eLabel);
        Pattern child = children.get(e);
        if (child == null) {
            child = new Pattern(e, this);
            children.put(e, child);
        }
        return child;
    }

    public Pattern addChild(int from, int to, int fromLabel, int toLabel, int eLabel) {
        DFSEdge e = new DFSEdge(from, to, fromLabel, toLabel, eLabel);
        Pattern child = new Pattern(e, this);
        children.put(e, child);
        return child;
    }

    public boolean hasChild() {
        return !this.children.isEmpty();
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

        for (Pattern child : children()) {
            child.remove(removedGraphs, removedClusters);
        }
    }

    public List<Pattern> rightSiblings() {
        List<Pattern> list = new ArrayList<>();
        if (parent != null) {
            list.addAll(parent.children.tailMap(this.edge).values());
        }
        return list;
    }

    public void removeChild(Pattern child) {
        children.remove(child.edge);
    }

    public List<LabeledGraph> graphs() {
        return new ArrayList<>(embeddingMap.keySet());
    }

    public long[] graphIds() {
        TreeSet<Long> set = new TreeSet<>();
        for (LabeledGraph g : embeddingMap.keySet()) {
            set.add(g.graphId());
        }

        long[] ids = new long[set.size()];
        int index = 0;
        for (Long i : set) {
            ids[index++] = i;
        }
        return ids;
    }

    public void setClusterDelimiter(Cluster c) {
        this.clusterDelimiter = c;
    }

    public void setGraphDelimiter(LabeledGraph g) {
        this.graphDelimiter = g;
    }

    public int numberOfEmbeddings() {
        int num = 0;
        for (Entry<Cluster, List<Embedding>> entry : intersectionEmbeddings.entrySet()) {
            num += entry.getValue() .size() * entry.getKey().size();
        }
        for (Entry<Cluster, List<Embedding>> entry : borderEmbeddings.entrySet()) {
            num += entry.getValue() .size() * entry.getKey().size();
        }
        for (List<Embedding> embeddings : embeddingMap.values()) {
            num += embeddings.size();
        }
        return num;
    }
}
