package com.liang.fsmhg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.function.Function;

import javax.xml.crypto.dsig.keyinfo.RetrievalMethod;

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
        // if (this.snapshots.size() == 0) {
        //     this.snapshots.add(s);
        //     s.setClusterIndex(this.index);
        //     for (LabeledVertex v : s.vertices()) {
        //         this.commonVertices.put(v.id(), v);
        //         this.commonEdges.put(v.id(), s.adjEdges(v.id()));
        //     }
        //     updateIntersection(s.graphId(), commonVertices, commonEdges);
        //     return true;
        // }

        this.deltaGraphList.add(this.getDeltaGraph(s));

        Map<Integer, LabeledVertex> vDeltaInc = new HashMap<>();
        Map<Integer, AdjEdges> eDeltaInc = new HashMap<>();
        Map<Integer, LabeledVertex> vCommon = commonVertices(s, vDeltaInc);
        Map<Integer, AdjEdges> eCommon = commonEdges(s, vCommon, vDeltaInc, eDeltaInc);
        if (checkSimilarity(s, vCommon, eCommon)) {
            s.setClusterIndex(this.index);
            this.commonVertices = vCommon;
            this.commonEdges = eCommon;
            this.snapshots.add(s);
            updateIntersection(s, vCommon, eCommon);
            updateDeltaGraphs(vDeltaInc, eDeltaInc);
            return true;
        }
        this.deltaGraphList.remove(this.deltaGraphList.size() - 1);
        return false;
    }

    private Map<Integer, LabeledVertex> commonVertices(LabeledGraph s, Map<Integer, LabeledVertex> vDeltaInc) {
        Map<Integer, LabeledVertex> vCommon = new HashMap<>();
        if (this.snapshots.size() == 0) {
            for (LabeledVertex v : s.vertices()) {
                vCommon.put(v.id(), v);
            }
            return vCommon;
        }

        LabeledGraph last = snapshots.get(snapshots.size() - 1);
        // if (commonVertices.size() <= s.vSize()) {
        //     for (LabeledVertex v : commonVertices.values()) {
        //         LabeledVertex v1 = s.vertex(v.id());
        //         if (v1 == null) {
        //             vDelta.putIfAbsent(v.id(), v);
        //         } else if (last.vLabel(v) == s.vLabel(v)) {
        //             vCommon.put(v1.id(), v1);
        //         }
        //     }
        // } else {
        //     for (LabeledVertex v : s.vertices()) {
        //         if (s.vertex(v.id()) != null && last.vLabel(v) == s.vLabel(v)) {
        //             vCommon.put(v.id(), v);
        //         }
        //     }
        // }

        for (LabeledVertex v : this.commonVertices.values()) {
            LabeledVertex v1 = s.vertex(v.id());
            if (v1 == null) {
                vDeltaInc.putIfAbsent(v.id(), v);
            } else if (last.vLabel(v) == s.vLabel(v1)) {
                vCommon.put(v1.id(), v1);
            }
        }
        return vCommon;
    }

    private Map<Integer, AdjEdges> commonEdges(LabeledGraph s, Map<Integer, LabeledVertex> vCommon, Map<Integer, LabeledVertex> vDeltaInc, Map<Integer, AdjEdges> eDeltaInc) {
        Map<Integer, AdjEdges> eCommon = new HashMap<>();
        if (this.snapshots.size() == 0) {
            for (int vId : vCommon.keySet()) {
                eCommon.put(vId, s.adjEdges(vId));
            }
            return eCommon;
        }

        LabeledGraph last = snapshots.get(snapshots.size() - 1);
        // int commonEdgeSize = 0;
        // for (AdjEdges adj : commonEdges.values()) {
        //     commonEdgeSize += adj.size();
        // }
        // commonEdgeSize /= 2;
        // if (commonEdgeSize <= s.eSize()) {
        //     for (LabeledVertex v : vCommon.values()) {
        //         AdjEdges edges = new AdjEdges();
        //         AdjEdges edges1 = s.adjEdges(v.id());
        //         for (LabeledEdge e : commonEdges.get(v.id())) {
        //             LabeledEdge e1 = edges1.edgeTo(e.to().id());
        //             if (e1 == null) {
        //                 continue;
        //             }
        //             if (last.vLabel(e.from()) == s.vLabel(e1.from()) && last.vLabel(e.to()) == s.vLabel(e1.to())
        //                     && last.eLabel(e) == s.eLabel(e1)) {
        //                 edges.add(e1);
        //             }
        //         }
        //         eCommon.put(v.id(), edges);
        //     }
        // } else {
        //     for (LabeledVertex v : s.vertices()) {
        //         AdjEdges edges = new AdjEdges();
        //         AdjEdges edges1 = commonEdges.get(v.id());
        //         for (LabeledEdge e : s.adjEdges(v.id())) {
        //             LabeledEdge e1 = edges1.edgeTo(e.to().id());
        //             if (e1 == null) {
        //                 continue;
        //             }
        //             if (last.vLabel(e.from()) == s.vLabel(e1.from()) && last.vLabel(e.to()) == s.vLabel(e1.to()) 
        //                     && last.eLabel(e) == s.eLabel(e1)) {
        //                 edges.add(e);
        //             }
        //         }
        //         eCommon.put(v.id(), edges);
        //     }
        // }
        
        // for (LabeledVertex v : this.commonVertices.values()) {
        //     AdjEdges edges = new AdjEdges();
        //     AdjEdges edges1 = s.adjEdges(v.id());
        //     for (LabeledEdge e : this.commonEdges.get(v.id())) {
        //         LabeledEdge e1 = edges1.edgeTo(e.to().id());
        //         if (e1 == null) {
        //             if ()
        //             continue;
        //         }
        //         if (last.vLabel(e.from()) == s.vLabel(e1.from()) && last.vLabel(e.to()) == s.vLabel(e1.to())
        //                 && last.eLabel(e) == s.eLabel(e1)) {
        //             edges.add(e1);
        //         }
        //     }
        //     eCommon.put(v.id(), edges);
        // }

        for (Entry<Integer, AdjEdges> entry : this.commonEdges.entrySet()) {
            AdjEdges edges = new AdjEdges();
            for (LabeledEdge e : entry.getValue()) {
                LabeledVertex from = e.from();
                LabeledVertex to = e.to();
                LabeledEdge e1 = s.edge(from.id(), to.id());
                if (e1 == null) {
                    vDeltaInc.putIfAbsent(from.id(), from);
                    vDeltaInc.putIfAbsent(to.id(), to);
                    AdjEdges adj = eDeltaInc.computeIfAbsent(from.id(), new Function<Integer, AdjEdges>() {
                        @Override
                        public AdjEdges apply(Integer vId) {
                            return new AdjEdges();
                        }
                    });
                    adj.add(e);
                    continue;
                }
                if (last.vLabel(e.from()) == s.vLabel(e1.from()) && last.vLabel(e.to()) == s.vLabel(e1.to())
                        && last.eLabel(e) == s.eLabel(e1)) {
                    edges.add(e1);
                }
            }
            eCommon.put(entry.getKey(), edges);
        }
        return eCommon;
    }

    public LabeledGraph intersection() {
        return this.intersection;
    }

    private boolean checkSimilarity(LabeledGraph s, Map<Integer, LabeledVertex> vCommon, Map<Integer, AdjEdges> eCommon) {
        if (this.snapshots.size() == 0) {
            return true;
        }

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
        return sim >= similarity;
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

    private void updateDeltaGraphs(Map<Integer, LabeledVertex> vDelta, Map<Integer, AdjEdges> eDelta) {
        Iterator<Entry<Integer, Map<LabeledGraph, AdjEdges>>> borderAdjIt = this.borderAdjEdges.entrySet().iterator();
        while (borderAdjIt.hasNext()) {
            Entry<Integer, Map<LabeledGraph, AdjEdges>> entry = borderAdjIt.next();
            int borderVid = entry.getKey();
            if (!this.commonVertices.containsKey(borderVid)) {
                borderAdjIt.remove();
            }
        }

        for (LabeledVertex v : vDelta.values()) {
            for (LabeledGraph g : deltaGraphList) {
                g.addVertexIfAbsent(v);
            }
        }

        for (Entry<Integer, AdjEdges> entry : eDelta.entrySet()) {
            for (LabeledGraph g : deltaGraphList) {
                AdjEdges adj = g.adjEdges(entry.getKey());
                for (LabeledEdge e : entry.getValue()) {
                    adj.add(e);
                    LabeledVertex from = e.from();
                    LabeledVertex to = e.to();

                    if (this.commonVertices.containsKey(from.id())) {
                        Map<LabeledGraph, AdjEdges> map =  this.borderAdjEdges.computeIfAbsent(from.id(), new Function<Integer, Map<LabeledGraph, AdjEdges>>() {
                            @Override
                            public Map<LabeledGraph, AdjEdges> apply(Integer t) {
                                return new HashMap<>();
                            }
                        });
                        AdjEdges deltAdj = entry.getValue();
                        AdjEdges edges = map.putIfAbsent(g, deltAdj);
                        if (deltAdj != null && edges != null) {
                            for (LabeledEdge e1 : deltAdj) {
                                edges.add(e1);
                            }
                        }
                    }

                    if (this.commonVertices.containsKey(to.id())) {
                        Map<LabeledGraph, AdjEdges> map = this.borderAdjEdges.computeIfAbsent(to.id(), new Function<Integer, Map<LabeledGraph, AdjEdges>>() {
                            @Override
                            public Map<LabeledGraph, AdjEdges> apply(Integer t) {
                                return new HashMap<>();
                            }
                        });
                        AdjEdges deltAdj = entry.getValue();
                        AdjEdges edges = map.putIfAbsent(g, eDelta.get(to.id()));
                        if (deltAdj != null && edges != null) {
                            for (LabeledEdge e1 : deltAdj) {
                                edges.add(e1);
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateIntersection() {
        LabeledGraph last = snapshots.get(snapshots.size() - 1);
        // List<LabeledEdge> edges = new ArrayList<>();
        // for (AdjEdges adjEdges : commonEdges.values()) {
        //     edges.addAll(adjEdges.edges());
        // }
        // this.intersection = new Intersection(last.graphId(), commonVertices.values(), edges);
        if (this.intersection == null) {
            this.intersection = new StaticGraph(last.graphId());
        } else {
            this.intersection.setId(last.graphId());
        }
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

    private LabeledGraph getDeltaGraph(LabeledGraph s) {
        if (this.snapshots.size() == 0) {
            return new StaticGraph(s.graphId());
        }

        Map<Integer, LabeledVertex> vDelta = new HashMap<>();
        Map<Integer, AdjEdges> eDelta = new HashMap<>();

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
                    // if (commonVertices.containsKey(from.id())) {
                    //     Map<LabeledGraph, AdjEdges> borderAdj = this.borderAdjEdges.computeIfAbsent(from.id(),
                    //             new Function<Integer, Map<LabeledGraph, AdjEdges>>() {
                    //                 @Override
                    //                 public Map<LabeledGraph, AdjEdges> apply(Integer t) {
                    //                     return new HashMap<>();
                    //                 }
                    //             });
                    //     borderAdj.putIfAbsent(s, eDelta.get(from.id()));
                    // }
                    // if (commonVertices.containsKey(to.id())) {
                    //     Map<LabeledGraph, AdjEdges> borderAdj = this.borderAdjEdges.computeIfAbsent(to.id(),
                    //             new Function<Integer, Map<LabeledGraph, AdjEdges>>() {
                    //                 @Override
                    //                 public Map<LabeledGraph, AdjEdges> apply(Integer t) {
                    //                     return new HashMap<>();
                    //                 }
                    //             });
                    //     borderAdj.putIfAbsent(s, eDelta.get(to.id()));
                    // }
                    eDelta.get(from.id()).add(e);
                }
            }
        }

        LabeledGraph dg = new StaticGraph(s.graphId());
        dg.setVertices(vDelta);
        dg.setEdges(eDelta);
        return dg;
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
            // cluster.updateIntersection();
            // cluster.computeDeltas();
            clusters.add(cluster);
            cluster = new Cluster(similarity);
            cluster.setIndex(startIndex++);
            cluster.add(s);
        }
        // cluster.updateIntersection();
        // cluster.computeDeltas();
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