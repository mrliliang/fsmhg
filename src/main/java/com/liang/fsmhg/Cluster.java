package com.liang.fsmhg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import com.liang.fsmhg.graph.AdjEdges;
import com.liang.fsmhg.graph.LabeledEdge;
import com.liang.fsmhg.graph.LabeledGraph;
import com.liang.fsmhg.graph.LabeledVertex;
import com.liang.fsmhg.graph.StaticGraph;

public class Cluster implements Iterable<LabeledGraph>, Comparable<Cluster> {
    private int index;

    private double similarity;
    private ArrayList<LabeledGraph> snapshots;

    private Map<Integer, LabeledVertex> commonVertices;
    private Map<Integer, AdjEdges> commonEdges;

    private Map<Long, DeltaGraph> deltaGraphs;
    private LabeledGraph intersection;
    private Map<Integer, LabeledVertex> border;
    private Map<Integer, Map<LabeledGraph, AdjEdges>> borderAdjEdges;
    private static final Map<LabeledGraph, AdjEdges> EMPTY_BORDER_ADJ = new HashMap<>();

    public Cluster(double similarity) {
        this.similarity = similarity;
        snapshots = new ArrayList<>();
        commonVertices = new HashMap<>();
        commonEdges = new HashMap<>();
        border = new HashMap<>();
        borderAdjEdges = new HashMap<>();
    }

    public Cluster(List<LabeledGraph> trans) {
        this.index = 0;
        this.intersection = trans.get(0);
        snapshots = new ArrayList<>();
        snapshots.addAll(trans);
    }

    public List<LabeledGraph> snapshots() {
        return snapshots;
    }

    public boolean contains(LabeledGraph g) {
        if (g == null) {
            return false;
        }

        LabeledGraph first = snapshots.get(0);
        LabeledGraph last = snapshots.get(snapshots.size() - 1);
        return g.graphId() >= first.graphId() && g.graphId() <= last.graphId();
    }

    public LabeledGraph first() {
        return snapshots.get(0);
    }

    public LabeledGraph last() {
        return snapshots.get(snapshots.size() - 1);
    }

    public int size() {
        return snapshots.size();
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }

    public boolean remove(Collection<LabeledGraph> snapshots) {
        return this.snapshots.removeAll(snapshots);
    }

    public boolean add(LabeledGraph s) {
        if (this.snapshots.size() == 0) {
            this.snapshots.add(s);
            s.setClusterIndex(this.index);
            for (LabeledVertex v : s.vertices()) {
                commonVertices.put(v.id(), v);
                commonEdges.put(v.id(), s.adjEdges(v.id()));
            }
            return true;
        }

        return similarityCheck(s);
    }

    private boolean similarityCheck(LabeledGraph s) {
        Map<Integer, LabeledVertex> vCommon = commonVertices(s);
        Map<Integer, AdjEdges> eCommon = commonEdges(vCommon, s);
        int commonEdgeNum = 0;
        for (AdjEdges adjEdges : eCommon.values()) {
            commonEdgeNum += adjEdges.size();
        }
        commonEdgeNum = commonEdgeNum / 2;

        // int denominator = s.vSize() + s.eSize();
        int totalEdgeNum = s.eSize();
        for (LabeledGraph snapshot : snapshots) {
            // denominator += (snapshot.vSize() + snapshot.eSize());
            totalEdgeNum += (snapshot.eSize());
        }
        // double sim = (double)(snapshots.size() + 1) * (vCommon.size() +
        // commonEdgeNum) / denominator;
        double sim = (double) (snapshots.size() + 1) * (commonEdgeNum) / totalEdgeNum;
        if (sim >= similarity) {
            commonVertices = vCommon;
            commonEdges = eCommon;
            this.snapshots.add(s);
            s.setClusterIndex(this.index);
            return true;
        }
        return false;
    }

    private Map<Integer, LabeledVertex> commonVertices(LabeledGraph s) {
        LabeledGraph last = snapshots.get(snapshots.size() - 1);
        Map<Integer, LabeledVertex> vCommon = new HashMap<>();
        for (LabeledVertex v : commonVertices.values()) {
            if (s.vertex(v.id()) != null && last.vLabel(v) == s.vLabel(v)) {
                // if (s.vertex(v.id()) != null) {
                vCommon.put(v.id(), v);
            }
        }
        return vCommon;
    }

    private Map<Integer, AdjEdges> commonEdges(Map<Integer, LabeledVertex> vCommon, LabeledGraph s) {
        LabeledGraph last = snapshots.get(snapshots.size() - 1);
        Map<Integer, AdjEdges> eCommon = new HashMap<>();
        for (LabeledVertex v : vCommon.values()) {
            AdjEdges edges = new AdjEdges();
            AdjEdges edges1 = s.adjEdges(v.id());
            for (LabeledEdge e : commonEdges.get(v.id())) {
                LabeledEdge e1 = edges1.edgeTo(e.to().id());
                if (e1 == null) {
                    continue;
                }
                if (last.vLabel(e.from()) == s.vLabel(e1.from()) && last.vLabel(e.to()) == s.vLabel(e1.to())
                        && last.eLabel(e) == s.eLabel(e1)) {
                    edges.add(e);
                }
                // edges.add(e);
            }
            eCommon.put(v.id(), edges);
        }
        return eCommon;
    }

    public LabeledGraph intersection() {
        return this.intersection;
    }

    private void computeIntersection() {
        LabeledGraph last = snapshots.get(snapshots.size() - 1);
        // List<LabeledEdge> edges = new ArrayList<>();
        // for (AdjEdges adjEdges : commonEdges.values()) {
        //     edges.addAll(adjEdges.edges());
        // }
        // this.intersection = new Intersection(last.graphId(), commonVertices.values(), edges);
        this.intersection = new StaticGraph(last.graphId());
        this.intersection.setVertices(commonVertices);
        this.intersection.setEdges(commonEdges);
    }

    private DeltaGraph computeDelta(LabeledGraph s) {
        Map<Integer, LabeledVertex> vDelta = new HashMap<>();
        Map<Integer, AdjEdges> eDelta = new HashMap<>();
        Map<Integer, LabeledVertex> vBorder = new HashMap<>();

        for (LabeledVertex v : s.vertices()) {
            if (!commonVertices.containsKey(v.id())) {
                vDelta.put(v.id(), v);
            }
            for (LabeledEdge e : s.adjEdges(v.id())) {
                LabeledVertex from = e.from();
                LabeledVertex to = e.to();
                AdjEdges adjInCommon = this.commonEdges.get(from.id());
                if (adjInCommon == null || adjInCommon.edgeTo(to.id()) == null) {
                    vDelta.putIfAbsent(from.id(), from);
                    eDelta.putIfAbsent(from.id(), new AdjEdges());
                    vDelta.putIfAbsent(to.id(), to);
                    eDelta.putIfAbsent(to.id(), new AdjEdges());
                    if (commonVertices.containsKey(from.id())) {
                        vBorder.put(from.id(), from);
                        Map<LabeledGraph, AdjEdges> borderAdj = this.borderAdjEdges.computeIfAbsent(from.id(),
                                new Function<Integer, Map<LabeledGraph, AdjEdges>>() {
                                    @Override
                                    public Map<LabeledGraph, AdjEdges> apply(Integer t) {
                                        return new HashMap<>();
                                    }
                                });
                        borderAdj.putIfAbsent(s, eDelta.get(from.id()));
                    }
                    if (commonVertices.containsKey(to.id())) {
                        vBorder.put(to.id(), to);
                        Map<LabeledGraph, AdjEdges> borderAdj = this.borderAdjEdges.computeIfAbsent(to.id(),
                                new Function<Integer, Map<LabeledGraph, AdjEdges>>() {
                                    @Override
                                    public Map<LabeledGraph, AdjEdges> apply(Integer t) {
                                        return new HashMap<>();
                                    }
                                });
                        borderAdj.putIfAbsent(s, eDelta.get(to.id()));
                    }
                    eDelta.get(from.id()).add(e);
                }
            }
        }
        List<LabeledEdge> edges = new ArrayList<>();
        for (AdjEdges adjEdges : eDelta.values()) {
            edges.addAll(adjEdges.edges());
        }

        this.border.putAll(vBorder);
        return new DeltaGraph(s.graphId(), vDelta.values(), edges, vBorder);
    }

    private void computeDeltas() {
        this.deltaGraphs = new HashMap<>();
        for (LabeledGraph s : snapshots) {
            deltaGraphs.put(s.graphId(), computeDelta(s));
        }
    }

    public List<DeltaGraph> deltaGraphs() {
        return new ArrayList<>(deltaGraphs.values());
    }

    public DeltaGraph deltaGraph(LabeledGraph graph) {
        return deltaGraphs.get(graph.graphId());
    }

    public Map<Integer, LabeledVertex> border() {
        // Map<Integer, LabeledVertex> map = new HashMap<>();
        // for (DeltaGraph delta : deltaGraphs.values()) {
        // map.putAll(delta.border());
        // }
        // return map;
        return this.border;
    }

    public Map<LabeledGraph, AdjEdges> borderAdj(int vId) {
        return this.borderAdjEdges.getOrDefault(vId, EMPTY_BORDER_ADJ);
    }

    @Override
    public Iterator<LabeledGraph> iterator() {
        return snapshots.iterator();
    }

    public static List<Cluster> partition(List<? extends LabeledGraph> snapshots, double similarity, int startIndex) {
        List<Cluster> clusters = new ArrayList<>();
        Cluster cluster = new Cluster(similarity);
        cluster.setIndex(startIndex++);
        for (LabeledGraph s : snapshots) {
            if (cluster.add(s)) {
                continue;
            }
            cluster.computeIntersection();
            cluster.computeDeltas();
            clusters.add(cluster);
            cluster = new Cluster(similarity);
            cluster.setIndex(startIndex++);
            cluster.add(s);
        }
        cluster.computeIntersection();
        cluster.computeDeltas();
        clusters.add(cluster);

        return clusters;
    }

    public boolean isBorder(int vId) {
        for (DeltaGraph dg : deltaGraphs.values()) {
            if (dg.vertex(vId) != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int compareTo(Cluster other) {
        return this.index - other.index;
    }

    public class Intersection extends LabeledGraph {

        private Intersection(long id, Collection<? extends LabeledVertex> vertices,
                Collection<? extends LabeledEdge> edges) {
            super(id, vertices, edges);
        }

        private Intersection(long id, Map<Integer, LabeledVertex> vertices, Map<Integer, AdjEdges> adjLists) {
            super(id);
            setVertices(vertices);
            setEdges(adjLists);
        }

        @Override
        public int vLabel(LabeledVertex v) {
            return v.label(graphId());
        }

        @Override
        public int eLabel(LabeledEdge e) {
            return e.label(graphId());
        }

        @Override
        public LabeledVertex addVertex(int id, int label) {
            throw new RuntimeException("Not allowed to add vertex to intersection.");
        }

        @Override
        public LabeledEdge addEdge(int from, int to, int eLabel) {
            throw new RuntimeException("Not allowed to add edge to intersection.");
        }
    }

    public class DeltaGraph extends LabeledGraph {
        private Map<Integer, LabeledVertex> border;

        private DeltaGraph(long id, Collection<? extends LabeledVertex> vertices,
                Collection<? extends LabeledEdge> edges, Map<Integer, LabeledVertex> border) {
            super(id, vertices, edges);
            this.border = border;
        }

        public Map<Integer, LabeledVertex> border() {
            return border;
        }

        public boolean isBorder(int vId) {
            return border.containsKey(vId);
        }

        @Override
        public int vLabel(LabeledVertex v) {
            return v.label(graphId());
        }

        @Override
        public int eLabel(LabeledEdge e) {
            return e.label(graphId());
        }

        @Override
        public LabeledVertex addVertex(int id, int label) {
            throw new RuntimeException("Not allowed to add vertex to delta graph.");
        }

        @Override
        public LabeledEdge addEdge(int from, int to, int eLabel) {
            throw new RuntimeException("Not allowed to add edge to delta graph.");
        }
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(index);
    }
}