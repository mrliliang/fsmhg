package com.liang.fsmhg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

import com.liang.fsmhg.code.DFSCode;
import com.liang.fsmhg.code.DFSEdge;
import com.liang.fsmhg.graph.LabeledGraph;

public class Pattern {

    private DFSEdge edge;
    private Pattern parent;

    private HashMap<LabeledGraph, List<Embedding>> embeddingMap;
    private HashMap<Cluster, List<Embedding>> intersectionEmbeddings;

    private TreeMap<DFSEdge, Pattern> children;

    private boolean isMinChecked;
    private boolean minCheckResult;

    private Cluster clusterDelimiter;
    private LabeledGraph graphDelimiter;

    private int support = 0;
    private HashMap<Cluster, DeltaCounter> deltaSupportCount;
    private class DeltaCounter {
        int delta = 0;
    }


    public Pattern(DFSEdge edge, Pattern parent) {
        this.edge = edge;
        this.parent = parent;

        embeddingMap = new HashMap<>();
        intersectionEmbeddings = new HashMap<>();

        children = new TreeMap<>();
        deltaSupportCount = new HashMap<>();
    }

    public DFSEdge edge() {
        return edge;
    }

    public Pattern parent() {
        return parent;
    }

    public int support() {
        return this.support;

        // HashSet<LabeledGraph> set = new HashSet<>(embeddingMap.keySet());
        // for (Cluster c : intersectionEmbeddings.keySet()) {
        //     set.addAll(c.snapshots());
        // }
        // return set.size();
    }

    public void increaseSupport(int deltaSup) {
        this.support += deltaSup;
    }

    public boolean containsCluster(Cluster c) {
        return intersectionEmbeddings.containsKey(c);
    }

    public boolean containGraph(LabeledGraph g) {
        return embeddingMap.containsKey(g);
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

    public List<LabeledGraph> graphsAfterDelimiter() {
        if (graphDelimiter == null) {
            return graphs();
        }
        TreeSet<LabeledGraph> graphs = new TreeSet<>(embeddingMap.keySet());
        return new ArrayList<>(graphs.tailSet(graphDelimiter, false));
    }

    public List<Cluster> clusters() {
        return new ArrayList<>(intersectionEmbeddings.keySet());
    }

    public List<Cluster> clustersAfterDelimiter() {
        if (clusterDelimiter == null) {
            return clusters();
        }
        TreeSet<Cluster> clusters = new TreeSet<>(intersectionEmbeddings.keySet());
        //TODO Not sure whether should include clusterDelimiter
        // return new ArrayList<>(clusters.tailSet(clusterDelimiter, false));
        return new ArrayList<>(clusters.tailSet(clusterDelimiter, true));
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

    public void addEmbedding(LabeledGraph g, Embedding em) {
        List<Embedding> embeddings = embeddingMap.computeIfAbsent(g, labeledGraph -> {
            this.support++;
            return new ArrayList<>();
        });
        embeddings.add(em);
    }

    public void addEmbedding(LabeledGraph g, Cluster c, Embedding em) {
        List<Embedding> embeddings = embeddingMap.computeIfAbsent(g, labeledGraph -> {
            if (!intersectionEmbeddings.containsKey(c)) {
                this.support++;
                DeltaCounter counter = deltaSupportCount.computeIfAbsent(c, new Function<Cluster, DeltaCounter>() {
                    @Override
                    public DeltaCounter apply(Cluster t) {
                        return new DeltaCounter();
                    }
                });
                counter.delta++;
            }
            return new ArrayList<>();
        });
        embeddings.add(em);
    }

    public void addIntersectionEmbedding(Cluster c, Embedding em) {
        List<Embedding> embeddings = intersectionEmbeddings.computeIfAbsent(c, key -> {
            DeltaCounter counter = deltaSupportCount.getOrDefault(c, new DeltaCounter());
            this.support += (c.size() - counter.delta);
            deltaSupportCount.remove(c);
            return new ArrayList<>();
        });
        embeddings.add(em);
    }

    public void clearEmbeddings() {
        for (List<Embedding> embeddings : intersectionEmbeddings.values()) {
            embeddings.clear();
        }
        for (List<Embedding> embeddings : embeddingMap.values()) {
            embeddings.clear();
        }
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
                this.support--;
            } 
            else {
                Cluster c = g.getCluster();
                if (c != null && intersectionEmbeddings.containsKey(c)) {
                    removedGraphs.add(g);
                    this.support--;
                }
            }
        }

        if (removedGraphs.isEmpty()) {
            return;
        }

        List<Cluster> removedClusters = new ArrayList<>();
        for (Cluster c : clusters) {
            if (intersectionEmbeddings.remove(c) != null) {
                removedClusters.add(c);
            }
            this.deltaSupportCount.remove(c);
        }

        Iterator<Entry<DFSEdge, Pattern>> it = this.children.entrySet().iterator();
        while (it.hasNext()) {
            Pattern child = it.next().getValue();
            child.remove(removedGraphs, removedClusters);
            if (child.support <= 0) {
                it.remove();
            }
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

    public void removeChildren() {
        children.clear();
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

    public LabeledGraph graphDelimiter() {
        return this.graphDelimiter;
    }

    public long numberOfEmbeddings() {
        long num = 0;
        for (Entry<Cluster, List<Embedding>> entry : intersectionEmbeddings.entrySet()) {
            num += entry.getValue() .size();
        }
        for (List<Embedding> embeddings : embeddingMap.values()) {
            num += embeddings.size();
        }
        return num;
    }

    public long numberOfEmbeddingsNoPartition() {
        long num = 0;
        for (Entry<Cluster, List<Embedding>> entry : intersectionEmbeddings.entrySet()) {
            num += entry.getValue().size() * entry.getKey().size();
        }
        for (List<Embedding> embeddings : embeddingMap.values()) {
            num += embeddings.size();
        }
        return num;
    }
}
