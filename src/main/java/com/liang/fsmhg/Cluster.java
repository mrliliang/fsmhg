package com.liang.fsmhg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

    private ArrayList<LabeledGraph> deltaGraphList;
    private Map<Long, DeltaGraph> deltaGraphs;
    private LabeledGraph intersection;
    private Map<Integer, LabeledVertex> border;
    private Map<Integer, Map<LabeledGraph, AdjEdges>> borderAdjEdges;
    private static final Map<LabeledGraph, AdjEdges> EMPTY_BORDER_ADJ = new HashMap<>();
    private int totalEdgeNum = 0;

    private Cluster(double similarity) {
        this.similarity = similarity;
        snapshots = new ArrayList<>();
        commonVertices = new HashMap<>();
        commonEdges = new HashMap<>();
        border = new HashMap<>();
        borderAdjEdges = new HashMap<>();
        deltaGraphList = new ArrayList<>();
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
        Map<Integer, LabeledVertex> vCommon = new HashMap<>();
        Map<Integer, AdjEdges> eCommon = new HashMap<>();
        Map<Integer, LabeledVertex> vDeltaInc = new HashMap<>();
        Map<Integer, AdjEdges> eDeltaInc = new HashMap<>();
        commonVerticesEdges(s, vCommon, eCommon, vDeltaInc, eDeltaInc);

        boolean isSim = checkSimilarity(s, vCommon, eCommon);
        if (isSim) {
            s.setClusterIndex(this.index);
            this.commonVertices = vCommon;
            this.commonEdges = eCommon;
            updateIntersection(s, vCommon, eCommon);
            updateDeltaGraphs(vDeltaInc, eDeltaInc);
            this.snapshots.add(s);
            this.totalEdgeNum += s.eSize();
            this.deltaGraphList.add(this.getDeltaGraph(s));
            return true;
        }
        return false;
    }

    private void commonVerticesEdges(LabeledGraph s, Map<Integer, LabeledVertex> vCommon, Map<Integer, AdjEdges> eCommon, Map<Integer, LabeledVertex> vDeltaInc, Map<Integer, AdjEdges> eDeltaInc) {
        if (this.snapshots.size() == 0) {
            for (LabeledVertex v : s.vertices()) {
                vCommon.put(v.id(), v);
                eCommon.put(v.id(), s.adjEdges(v.id()));
            }
            return;
        }

        LabeledGraph last = snapshots.get(snapshots.size() - 1);
        for (LabeledVertex v : this.commonVertices.values()) {
            LabeledVertex from = s.vertex(v.id());
            if (from == null || last.vLabel(v) != s.vLabel(from)) {
                vDeltaInc.putIfAbsent(v.id(), v);
                AdjEdges deltaAdj = eDeltaInc.computeIfAbsent(v.id(), new Function<Integer, AdjEdges>() {
                    @Override
                    public AdjEdges apply(Integer t) {
                        return new AdjEdges();
                    }
                });
                for (LabeledEdge e : this.commonEdges.get(v.id())) {
                    deltaAdj.add(e);
                    LabeledVertex to = e.to();
                    vDeltaInc.putIfAbsent(to.id(), to);
                    eDeltaInc.putIfAbsent(to.id(), new AdjEdges());
                }
                continue;
            }

            vCommon.put(from.id(), from);
            AdjEdges edges = new AdjEdges();
            eCommon.put(from.id(), edges);
            for (LabeledEdge e : this.commonEdges.get(from.id())) {
                LabeledVertex to = e.to();
                LabeledEdge e1 = s.edge(from.id(), to.id());
                if (e1 != null && last.vLabel(e.to()) == s.vLabel(e1.to()) && last.eLabel(e) == s.eLabel(e1)) {
                    edges.add(e1);
                    continue;
                }
                
                vDeltaInc.putIfAbsent(from.id(), from);
                AdjEdges deltaAdj = eDeltaInc.computeIfAbsent(from.id(), new Function<Integer, AdjEdges>() {
                    @Override
                    public AdjEdges apply(Integer t) {
                        return new AdjEdges();
                    }
                });
                deltaAdj.add(e);
                vDeltaInc.putIfAbsent(to.id(), to);
                eDeltaInc.putIfAbsent(to.id(), new AdjEdges());
            }
        }
    }

    private boolean checkSimilarity(LabeledGraph s, Map<Integer, LabeledVertex> vCommon, Map<Integer, AdjEdges> eCommon) {
        if (this.snapshots.size() == 0) {
            return true;
        }

        int commonEdgeNum = 0;
        HashMap<Integer, AdjEdges> map = new HashMap<>();
        map.values();
        for (AdjEdges adjEdges : eCommon.values()) {
            commonEdgeNum += adjEdges.size();
        }
        commonEdgeNum = commonEdgeNum / 2;
        double sim = (double) (snapshots.size() + 1) * (commonEdgeNum) / (this.totalEdgeNum + s.eSize());
        return sim >= similarity;
    }

    public LabeledGraph intersection() {
        return this.intersection;
    }

    private void updateIntersection(LabeledGraph s, Map<Integer, LabeledVertex> vCommon, Map<Integer, AdjEdges> eCommon) {
        if (this.intersection == null) {
            this.intersection = new StaticGraph(s.graphId());
        } else {
            this.intersection.setId(s.graphId());
        }
        this.intersection.setVertices(vCommon);
        this.intersection.setEdges(eCommon);
    }

    private void updateDeltaGraphs(Map<Integer, LabeledVertex> vDeltaInc, Map<Integer, AdjEdges> eDeltaInc) {
        if (vDeltaInc.isEmpty()) {
            return;
        }

        Iterator<Entry<Integer, Map<LabeledGraph, AdjEdges>>> borderAdjIt = this.borderAdjEdges.entrySet().iterator();
        while (borderAdjIt.hasNext()) {
            Entry<Integer, Map<LabeledGraph, AdjEdges>> entry = borderAdjIt.next();
            int borderVid = entry.getKey();
            if (!this.commonVertices.containsKey(borderVid)) {
                borderAdjIt.remove();
            }
        }

        for (LabeledVertex v : vDeltaInc.values()) {
            for (LabeledGraph dg : deltaGraphList) {
                dg.addVertexIfAbsent(v);
            }
        }

        for (Entry<Integer, AdjEdges> entry : eDeltaInc.entrySet()) {
            boolean isBorderVertex = this.commonVertices.containsKey(entry.getKey());
            for (int i = 0; i < this.snapshots.size(); i++) {
                LabeledGraph g = this.snapshots.get(i);
                LabeledGraph dg = this.deltaGraphList.get(i);
                AdjEdges adj = dg.adjEdges(entry.getKey());
                for (LabeledEdge e : entry.getValue()) {
                    adj.add(e);
                }
                if (adj.size() > 0 && isBorderVertex) {
                    Map<LabeledGraph, AdjEdges> map =  this.borderAdjEdges.computeIfAbsent(entry.getKey(), new Function<Integer, Map<LabeledGraph, AdjEdges>>() {
                        @Override
                        public Map<LabeledGraph, AdjEdges> apply(Integer t) {
                            return new HashMap<>();
                        }
                    });
                    map.put(g, adj);
                }
            }
        }
    }

    private LabeledGraph getDeltaGraph(LabeledGraph s) {
        if (this.snapshots.size() == 0) {
            return new StaticGraph(s.graphId());
        }

        Map<Integer, LabeledVertex> vDelta = new HashMap<>();
        Map<Integer, AdjEdges> eDelta = new HashMap<>();

        for (LabeledVertex v : s.vertices()) {
            boolean isCommonVertex = commonVertices.containsKey(v.id());
            if (!isCommonVertex) {
                vDelta.putIfAbsent(v.id(), v);
                AdjEdges deltaAdj = eDelta.computeIfAbsent(v.id(), new Function<Integer, AdjEdges>() {
                    @Override
                    public AdjEdges apply(Integer t) {
                        return new AdjEdges();
                    }
                });
                for (LabeledEdge e : s.adjEdges(v.id())) {
                    deltaAdj.add(e);
                    LabeledVertex to = e.to();
                    vDelta.put(to.id(), to);
                    eDelta.putIfAbsent(to.id(), new AdjEdges());
                }
                continue;
            }
            
            for (LabeledEdge e : s.adjEdges(v.id())) {
                LabeledVertex from = e.from();
                LabeledVertex to = e.to();
                AdjEdges adjInCommon = this.commonEdges.get(from.id());
                if (adjInCommon == null || adjInCommon.edgeTo(to.id()) == null) {
                    vDelta.putIfAbsent(from.id(), from);
                    AdjEdges adj = eDelta.computeIfAbsent(from.id(), new Function<Integer, AdjEdges>() {
                        @Override
                        public AdjEdges apply(Integer t) {
                            return new AdjEdges();
                        }
                    });
                    adj.add(e);
                    vDelta.putIfAbsent(to.id(), to);
                    eDelta.putIfAbsent(to.id(), new AdjEdges());
                }
            }
            
            AdjEdges deltaAdj = eDelta.get(v.id());
            if (deltaAdj == null || deltaAdj.size() == 0) {
                continue;
            }
            Map<LabeledGraph, AdjEdges> borderAdj = this.borderAdjEdges.computeIfAbsent(v.id(),
                    new Function<Integer, Map<LabeledGraph, AdjEdges>>() {
                        @Override
                        public Map<LabeledGraph, AdjEdges> apply(Integer t) {
                            return new HashMap<>();
                        }
                    });
            borderAdj.putIfAbsent(s, deltaAdj);
        }

        LabeledGraph dg = new StaticGraph(s.graphId());
        dg.setVertices(vDelta);
        dg.setEdges(eDelta);
        return dg;
    }

    public DeltaGraph deltaGraph(LabeledGraph graph) {
        return deltaGraphs.get(graph.graphId());
    }

    public LabeledGraph deltaGraph1(LabeledGraph g) {
        int index = (int)(g.graphId() - this.snapshots.get(0).graphId());
        return this.deltaGraphList.get(index);
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
            clusters.add(cluster);
            cluster = new Cluster(similarity);
            cluster.setIndex(startIndex++);
            cluster.add(s);
        }
        clusters.add(cluster);
        return clusters;
    }

    @Override
    public int compareTo(Cluster other) {
        return this.index - other.index;
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