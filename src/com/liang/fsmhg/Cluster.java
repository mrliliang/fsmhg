package com.liang.fsmhg;


import com.liang.fsmhg.graph.*;

import javax.swing.*;
import java.util.*;

public class Cluster implements Iterable<LabeledGraph>, Comparable<Cluster>{
    private int index;

    private double similarity;
    private List<LabeledGraph> snapshots;

    private Map<Integer, LabeledVertex> commonVertices;
    private Map<Integer, AdjEdges> commonEdges;

    private Map<Long, DeltaGraph> deltaGraphs;

    public Cluster(double similarity) {
        this.similarity = similarity;
        snapshots = new ArrayList<>();
        commonVertices = new HashMap<>();
        commonEdges = new HashMap<>();
    }

    public List<LabeledGraph> snapshots() {
        return snapshots;
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

    public void remove(List<LabeledGraph> snapshots) {
        snapshots.removeAll(snapshots);
    }

    public boolean add(LabeledGraph s) {
        if (this.snapshots.size() == 0) {
            this.snapshots.add(s);
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
        int edgeNum = 0;
        for (AdjEdges adjEdges : eCommon.values()) {
            edgeNum += adjEdges.size();
        }
        edgeNum /= edgeNum / 2;


        int denominator = 0;
        for (LabeledGraph snapshot : snapshots) {
            denominator += (snapshot.vSize() + snapshot.eSize());
        }
        denominator += (s.vSize() + s.eSize());
        double sim = (double)(snapshots.size() + 1) * (vCommon.size() + edgeNum) / denominator;
        if (sim >= similarity) {
            commonVertices = vCommon;
            commonEdges = eCommon;
            return true;
        }
        return false;
    }

    private Map<Integer, LabeledVertex> commonVertices(LabeledGraph s) {
        LabeledGraph last = snapshots.get(snapshots.size() - 1);
        Map<Integer, LabeledVertex> vCommon = new HashMap<>();
        for (LabeledVertex v : commonVertices.values()) {
            if (s.vertex(v.id()) != null && last.vLabel(v) == s.vLabel(v)) {
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
                if (last.vLabel(e.from()) == s.vLabel(e1.from())
                        && last.vLabel(e.to()) == s.vLabel(e1.to())
                        && last.eLabel(e) == s.eLabel(e1)) {
                    edges.add(e);
                }
            }
            eCommon.put(v.id(), edges);
        }
        return eCommon;
    }

    public Intersection intersection() {
        LabeledGraph last = snapshots.get(snapshots.size() - 1);
        List<LabeledEdge> edges = new ArrayList<>();
        for (AdjEdges adjEdges : commonEdges.values()) {
            edges.addAll(adjEdges.edges());
        }
        return new Intersection(last.graphId(), new ArrayList<>(commonVertices.values()), edges);
    }

    private DeltaGraph computeDelta(LabeledGraph s) {
        Map<Integer, LabeledVertex> vDelta = new HashMap<>();
        Map<Integer, AdjEdges> eDelta = new HashMap<>();
        Map<Integer, LabeledVertex> vBorder = new HashMap<>();
        for (LabeledVertex v : s.vertices()) {
            if (!commonVertices.containsKey(v.id())) {
                vDelta.put(v.id(), v);
                eDelta.put(v.id(), s.adjEdges(v.id()));
            }
        }

        // TODO: 2020/4/2 delta graph may not be correct
        for (AdjEdges adjEdges : new ArrayList<>(eDelta.values())) {
            for (LabeledEdge e : adjEdges) {
                LabeledVertex from = e.from();
                LabeledVertex to = e.to();
                if (commonVertices.containsKey(to.id())) {
                    vBorder.put(to.id(), to);
                    AdjEdges adjEdges1 = eDelta.get(to.id());
                    if (adjEdges1 == null) {
                        adjEdges1 = new AdjEdges();
                        eDelta.put(to.id(), adjEdges1);
                    }
                    adjEdges1.add(s.edge(to.id(), from.id()));

                    // TODO: 2020/4/2 从与to连接的所有边中找出border points和delta edges

                }
            }
        }

        vDelta.putAll(vBorder);
        List<LabeledVertex> vertices = new ArrayList<>(vBorder.values());
        for (int i = 0; i < vertices.size(); i++) {
            for (int j = i + 1; j < vertices.size(); j++) {
                LabeledVertex v1 = vertices.get(i);
                LabeledVertex v2 = vertices.get(j);
                LabeledEdge e1 = s.edge(v1.id(), v2.id());
                LabeledEdge e2 = commonEdges.get(v1.id()).edgeTo(v2.id());
                if (e1 != null && e2 == null) {
                    eDelta.get(v1.id()).add(e1);
                    eDelta.get(v2.id()).add(s.edge(v2.id(), v1.id()));
                }
            }
        }

        List<LabeledEdge> edges = new ArrayList<>();
        for (AdjEdges adjEdges : eDelta.values()) {
            edges.addAll(adjEdges.edges());
        }
        return new DeltaGraph(s.graphId(), new ArrayList<>(vDelta.values()), new ArrayList<>(edges), vBorder);
    }

    private void computeDeltas() {
        this.deltaGraphs = new TreeMap<>();
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
        Map<Integer, LabeledVertex> map = new HashMap<>();
        for (DeltaGraph delta : deltaGraphs.values()) {
            map.putAll(delta.border());
        }
        return map;
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
            cluster.computeDeltas();
            clusters.add(cluster);
            cluster = new Cluster(similarity);
            cluster.setIndex(startIndex++);
        }
        cluster.computeDeltas();
        clusters.add(cluster);
        return clusters;
    }

    @Override
    public int compareTo(Cluster other) {
        if (other == null) {
            return 1;
        }
        return this.index - other.index;
    }

    public class Intersection extends LabeledGraph {

        private Intersection(long id, List<? extends LabeledVertex> vertices, List<? extends LabeledEdge> edges) {
            super(id, vertices, edges);
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

        private DeltaGraph(long id, List<? extends LabeledVertex> vertices, List<? extends LabeledEdge> edges, Map<Integer, LabeledVertex> border) {
            super(id, vertices, edges);
            this.border = border;
        }

        public Map<Integer, LabeledVertex> border() {
            return border;
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
}
