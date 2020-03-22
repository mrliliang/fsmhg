package com.liang.fsmhg;


import com.liang.fsmhg.graph.*;

import java.util.*;

public class Cluster implements Iterable<Snapshot> {

    private double similarity;
    private List<Snapshot> snapshots;

    private Map<Integer, DynamicVertex> commonVertices;
    private Map<Integer, AdjEdges<DynamicEdge>> commonEdges;


    public Cluster(double similarity) {
        this.similarity = similarity;
        snapshots = new ArrayList<>();
        commonVertices = new HashMap<>();
        commonEdges = new HashMap<>();
    }

    public List<Snapshot> snapshots() {
        return snapshots;
    }

    public void remove(List<Snapshot> snapshots) {
        snapshots.removeAll(snapshots);
    }

    public boolean add(Snapshot s) {
        if (this.snapshots.size() == 0) {
            this.snapshots.add(s);
            for (DynamicVertex v : s.vertices()) {
                commonVertices.put(v.id(), v);
                commonEdges.put(v.id(), s.adjEdges(v.id()));
            }
            return true;
        }

        return similarityCheck(s);
    }

    private boolean similarityCheck(Snapshot s) {
        Map<Integer, DynamicVertex> vCommon = commonVertices(s);
        Map<Integer, AdjEdges<DynamicEdge>> eCommon = commonEdges(vCommon, s);
        int edgeNum = 0;
        for (AdjEdges adjEdges : eCommon.values()) {
            edgeNum += adjEdges.size();
        }
        edgeNum /= edgeNum / 2;


        int denominator = 0;
        for (Snapshot snapshot : snapshots) {
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

    private Map<Integer, DynamicVertex> commonVertices(Snapshot s) {
        Snapshot last = snapshots.get(snapshots.size() - 1);
        Map<Integer, DynamicVertex> vCommon = new HashMap<>();
        for (DynamicVertex v : commonVertices.values()) {
            if (s.vertex(v.id()) != null && last.vLabel(v) == s.vLabel(v)) {
                vCommon.put(v.id(), v);
            }
        }
        return vCommon;
    }

    private Map<Integer, AdjEdges<DynamicEdge>> commonEdges(Map<Integer, DynamicVertex> vCommon, Snapshot s) {
        Snapshot last = snapshots.get(snapshots.size() - 1);
        Map<Integer, AdjEdges<DynamicEdge>> eCommon = new HashMap<>();
        for (DynamicVertex v : vCommon.values()) {
            AdjEdges<DynamicEdge> edges = new AdjEdges<>();
            AdjEdges<DynamicEdge> edges1 = s.adjEdges(v.id());
            for (DynamicEdge e : commonEdges.get(v.id())) {
                DynamicEdge e1 = edges1.edgeTo(e.to().id());
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
        Snapshot last = snapshots.get(snapshots.size() - 1);
        List<DynamicEdge> edges = new ArrayList<>();
        for (AdjEdges adjEdges : commonEdges.values()) {
            edges.addAll(adjEdges.edges());
        }
        return new Intersection(last.graphId(), new ArrayList<>(commonVertices.values()), edges);
    }

    public DeltaGraph delta(Snapshot s) {
        Map<Integer, DynamicVertex> vDelta = new HashMap<>();
        Map<Integer, AdjEdges<DynamicEdge>> eDelta = new HashMap<>();
        Map<Integer, DynamicVertex> vBorder = new HashMap<>();
        Map<Integer, AdjEdges<DynamicEdge>> eBorder = new HashMap<>();
        for (DynamicVertex v : s.vertices()) {
            if (!commonVertices.containsKey(v.id())) {
                vDelta.put(v.id(), v);
                eDelta.put(v.id(), s.adjEdges(v.id()));
            }
        }

        for (AdjEdges<DynamicEdge> adjEdges : eDelta.values()) {
            for (DynamicEdge e : adjEdges) {
                DynamicVertex from = e.from();
                DynamicVertex to = e.to();
                if (commonVertices.containsKey(to.id())) {
                    vDelta.put(to.id(), to);
                    AdjEdges<DynamicEdge> edges = eBorder.get(to.id());
                    if (edges == null) {
                        edges = new AdjEdges<>();
                        eBorder.put(to.id(), edges);
                    }
                    edges.add(s.edge(to.id(), from.id()));
                }
            }
        }

        vDelta.putAll(vBorder);
        eDelta.putAll(eBorder);

        Snapshot last = snapshots.get(snapshots.size() - 1);
        List<DynamicEdge> edges = new ArrayList<>();
        for (AdjEdges adjEdges : eDelta.values()) {
            edges.addAll(adjEdges.edges());
        }
        return new DeltaGraph(last.graphId(), new ArrayList<>(vDelta.values()), new ArrayList<>(edges), vBorder);
    }

    @Override
    public Iterator<Snapshot> iterator() {
        return snapshots.iterator();
    }

    public static List<Cluster> partition(List<Snapshot> snapshots, double similarity) {
        List<Cluster> clusters = new ArrayList<>();
        Cluster cluster = new Cluster(similarity);
        for (Snapshot s : snapshots) {
            if (cluster.add(s)) {
                continue;
            }
            clusters.add(cluster);
            cluster = new Cluster(similarity);
            cluster.add(s);
        }
        return clusters;
    }

    public class Intersection extends LabeledGraph<DynamicVertex, DynamicEdge> {

        private Intersection(long id, List<DynamicVertex> vertices, List<DynamicEdge> edges) {
            super(id, vertices, edges);
        }

        @Override
        public int vLabel(DynamicVertex v) {
            return v.label(graphId());
        }

        @Override
        public int eLabel(DynamicEdge e) {
            return e.label(graphId());
        }

        @Override
        public DynamicVertex addVertex(int id, int label) {
            throw new RuntimeException("Not allowed to add vertex to intersection.");
        }

        @Override
        public DynamicEdge addEdge(int from, int to, int eLabel) {
            throw new RuntimeException("Not allowed to add edge to intersection.");
        }
    }

    public class DeltaGraph extends LabeledGraph<DynamicVertex, DynamicEdge> {
        private Map<Integer, DynamicVertex> border;

        private DeltaGraph(long id, List<DynamicVertex> vertices, List<DynamicEdge> edges, Map<Integer, DynamicVertex> border) {
            super(id, vertices, edges);
            this.border = border;
        }

        public Map<Integer, DynamicVertex> border() {
            return border;
        }

        @Override
        public int vLabel(DynamicVertex v) {
            return v.label(graphId());
        }

        @Override
        public int eLabel(DynamicEdge e) {
            return e.label(graphId());
        }

        @Override
        public DynamicVertex addVertex(int id, int label) {
            throw new RuntimeException("Not allowed to add vertex to delta graph.");
        }

        @Override
        public DynamicEdge addEdge(int from, int to, int eLabel) {
            throw new RuntimeException("Not allowed to add edge to delta graph.");
        }
    }
}
