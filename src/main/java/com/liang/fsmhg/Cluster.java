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

    static long commonVerticesTime = 0;
    static long commonEdgesTime = 0;
    static long updateInterTime = 0;
    static long updateDeltaTime = 0;
    static long getDeltaTime = 0;
    static long similarityTime = 0;
    static int count = 1;
    public boolean add(LabeledGraph s) {
        Map<Integer, LabeledVertex> vDeltaInc = new HashMap<>();
        Map<Integer, AdjEdges> eDeltaInc = new HashMap<>();
        long vCommonBegin = System.currentTimeMillis();
        Map<Integer, LabeledVertex> vCommon = commonVertices(s, vDeltaInc);
        long vCommonEnd = System.currentTimeMillis();
        commonVerticesTime += (vCommonEnd - vCommonBegin);
        long eCommonBegin = System.currentTimeMillis();
        Map<Integer, AdjEdges> eCommon = commonEdges(s, vCommon, vDeltaInc, eDeltaInc);
        long eCommonEnd = System.currentTimeMillis();
        commonEdgesTime += (eCommonEnd - eCommonBegin);

        long similarityBegin = System.currentTimeMillis();
        boolean isSim = checkSimilarity(s, vCommon, eCommon);
        long similarityEnd = System.currentTimeMillis();
        similarityTime += (similarityEnd - similarityBegin);
        if (isSim) {
            // System.out.println(count++);
            s.setClusterIndex(this.index);
            this.commonVertices = vCommon;
            this.commonEdges = eCommon;
            long updateInterBegin = System.currentTimeMillis();
            updateIntersection(s, vCommon, eCommon);
            long updateInterEnd = System.currentTimeMillis();
            updateInterTime += (updateInterEnd - updateInterBegin);
            long updateDeltaBegin = System.currentTimeMillis();
            updateDeltaGraphs(vDeltaInc, eDeltaInc);
            long updateDeltaEnd = System.currentTimeMillis();
            updateDeltaTime += (updateDeltaEnd - updateDeltaBegin);
            this.snapshots.add(s);
            long getDeltaBegin = System.currentTimeMillis();
            this.totalEdgeNum += s.eSize();
            this.deltaGraphList.add(this.getDeltaGraph(s));
            long getDeltaEnd = System.currentTimeMillis();
            getDeltaTime += (getDeltaEnd - getDeltaBegin);
            return true;
        }
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

    static long edgeSizeTime = 0;
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

        // int denominator = s.vSize() + s.eSize();
        long sizeBegin = System.currentTimeMillis();
        // int totalEdgeNum = s.eSize();
        long sizeEnd = System.currentTimeMillis();
        edgeSizeTime += (sizeEnd - sizeBegin);
        // for (LabeledGraph snapshot : snapshots) {
        //     // denominator += (snapshot.vSize() + snapshot.eSize());
        //     sizeBegin = System.currentTimeMillis();
        //     int eSize = snapshot.eSize();
        //     sizeEnd = System.currentTimeMillis();
        //     edgeSizeTime += (sizeEnd - sizeBegin);
        //     totalEdgeNum += (eSize);
        // }
        // double sim = (double)(snapshots.size() + 1) * (vCommon.size() +
        // commonEdgeNum) / denominator;
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
            for (LabeledGraph dg : deltaGraphList) {
                dg.addVertexIfAbsent(v);
            }
        }

        for (Entry<Integer, AdjEdges> entry : eDelta.entrySet()) {
            boolean contains = this.commonVertices.containsKey(entry.getKey());
            for (int i = 0; i < this.snapshots.size(); i++) {
                LabeledGraph g = this.snapshots.get(i);
                LabeledGraph dg = this.deltaGraphList.get(i);
                AdjEdges adj = dg.adjEdges(entry.getKey());
                for (LabeledEdge e : entry.getValue()) {
                    adj.add(e);
                }
                if (adj.size() > 0 && contains) {
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
            boolean contains = commonVertices.containsKey(v.id());
            if (!contains) {
                vDelta.put(v.id(), v);
                eDelta.put(v.id(), new AdjEdges());
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
            if (contains) {
                Map<LabeledGraph, AdjEdges> borderAdj = this.borderAdjEdges.computeIfAbsent(v.id(),
                        new Function<Integer, Map<LabeledGraph, AdjEdges>>() {
                            @Override
                            public Map<LabeledGraph, AdjEdges> apply(Integer t) {
                                return new HashMap<>();
                            }
                        });
                borderAdj.putIfAbsent(s, eDelta.get(v.id()));
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

        // long commonVerticesTime = 0;
        // long commonEdgesTime = 0;
        // long updateInterTime = 0;
        // long updateDeltaTime = 0;
        // long getDeltaTime = 0;
        // System.out.println("Common vertices time = " + commonVerticesTime);
        // System.out.println("Common edges time = " + commonEdgesTime);
        // System.out.println("Update inter time = " + updateInterTime);
        // System.out.println("Update delta time = " + updateDeltaTime);
        // System.out.println("Get delta time = " + getDeltaTime);
        cluster.printTime();
        return clusters;
    }

    private void printTime() {
        System.out.println("Common vertices time = " + commonVerticesTime);
        System.out.println("Common edges time = " + commonEdgesTime);
        System.out.println("Update inter time = " + updateInterTime);
        System.out.println("Update delta time = " + updateDeltaTime);
        System.out.println("Get delta time = " + getDeltaTime);
        System.out.println("Similarity time = " + similarityTime);
        System.out.println("Edge size time = " + edgeSizeTime);
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