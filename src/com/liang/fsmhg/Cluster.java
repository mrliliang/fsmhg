package com.liang.fsmhg;


import com.liang.fsmhg.graph.*;

import java.util.*;

public class Cluster implements Iterable<Snapshot> {

    private double similarity;
    private List<Snapshot> snapshots;

    private List<DynamicVertex> commonVertices;
    private Map<Integer, AdjEdges<DynamicEdge>> commonEdges;


    public Cluster(double similarity) {
        this.similarity = similarity;
        snapshots = new ArrayList<>();
        commonVertices = new ArrayList<>();
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
            commonVertices = s.vertices();
            for (DynamicVertex v : commonVertices) {
                commonEdges.put(v.id(), s.adjEdges(v.id()));
            }
            return true;
        }

        return similarityCheck(s);
    }

    private boolean similarityCheck(Snapshot s) {
        List<DynamicVertex> vCommon = commonVertices(s);
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

    private List<DynamicVertex> commonVertices(Snapshot s) {
        Snapshot last = snapshots.get(snapshots.size() - 1);
        List<DynamicVertex> vCommon = new ArrayList<>();
        for (DynamicVertex v : commonVertices) {
            if (s.vertex(v.id()) != null && last.vLabel(v) == s.vLabel(v)) {
                vCommon.add(v);
            }
        }
        return vCommon;
    }

    private Map<Integer, AdjEdges<DynamicEdge>> commonEdges(List<DynamicVertex> vCommon, Snapshot s) {
        Snapshot last = snapshots.get(snapshots.size() - 1);
        Map<Integer, AdjEdges<DynamicEdge>> eCommon = new HashMap<>();
        for (DynamicVertex v : vCommon) {
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
        Snapshot s = snapshots.get(snapshots.size() - 1);
        List<DynamicEdge> edges = new ArrayList<>();
        for (AdjEdges adjEdges : commonEdges.values()) {
            edges.addAll(adjEdges.edges());
        }
        return new Intersection(s.graphId(), commonVertices, edges);
    }

    public void delta(Snapshot s) {
        // TODO: 2020/3/21 To be implemented for delta graph
    }

    public List<DynamicVertex> border() {
        // TODO: 2020/3/21 To be implemented for border
        return null;
    }

    public boolean isBorderEmbedding() {
        // TODO: 2020/3/21 To be implemented for border embedding checking
        return false;
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
}
