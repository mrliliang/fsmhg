package com.liang.fsmhg;

import com.liang.fsmhg.code.DFSCode;
import com.liang.fsmhg.code.DFSEdge;
import com.liang.fsmhg.graph.*;

import java.io.*;
import java.util.*;

public class FSMHG {

    private File data;
    private File output;

    private TreeMap<Integer, PointPattern> points;
    private List<LabeledGraph> trans;
    private double minSup;
    private int absSup;
    private int maxEdgeSize;
    private boolean partition;
    private double similarity;
    private int patternCount = 0;
    private int pointCount = 0;

    private int maxVid = 0;

    private PatternWriter pw;

    private long joinTime = 0;
    private long extendTime = 0;
    private long joinCandTime = 0;
    private long extendCandTime = 0;
    private long joinCommonGraphTime = 0;
    private long extendCommonGraphTime = 0;
    private long actualJoinTime = 0;
    private long actualExtendTime = 0;
    private long pointTime = 0;
    private long edgeTime = 0;
    private long emVerticesTime = 0;
    private long patternToGraphTime = 0;
    private long emBitsTime = 0;
    private long emBitsCheckTime = 0;
    private long candCheckTime = 0;
    private long insertChildTime = 0;
    private long insertEmbeddingTime = 0;
    private long transTravelTime = 0;


    public FSMHG(File data, File output, double minSupport, int maxEdgeSize, boolean partition, double similarity) {
        this.data = data;
        this.output = output;
        this.minSup = minSupport;
        this.maxEdgeSize = maxEdgeSize;
        this.partition = partition;
        this.similarity = similarity;
        this.points = new TreeMap<>();
        this.pw = new PatternWriter(output);
    }

    private void saveResult() {
        FileWriter fw = null;
        BufferedWriter bw = null;
        try {
            int pointCount = 0;
            fw = new FileWriter(this.output);
            bw = new BufferedWriter(fw);
            for (PointPattern pp : points.values()) {
                if (!isFrequent(pp)) {
                    continue;
                }
                pointCount++;
                bw.write("t # " + (this.patternCount++) + " * " + pp.frequency());
                bw.newLine();
                bw.write("v 0 " + pp.label());
                bw.newLine();
                bw.newLine();

                for (Pattern child : pp.children()) {
                    if (!isFrequent(child) || !child.checkMin()) {
                        continue;
                    }
                    save(child, bw);
                }
            }
            System.out.println(pointCount + " point patterns");
            System.out.println((this.patternCount - pointCount) + " connected patterns.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bw.close();
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private void save(Pattern p, BufferedWriter bw) throws IOException {
        bw.write("t # " + (this.patternCount++) + " * " + p.frequency());
        bw.newLine();
        DFSCode code = p.code();
        LabeledGraph g = code.toGraph();
        for (int i = 0; i < g.vSize(); i++) {
            LabeledVertex v = g.vertex(i);
            bw.write("v " + i + " " + g.vLabel(v));
            bw.newLine();
        }
        for (DFSEdge e : code.edges()) {
            bw.write("e " + e.from() + " " + e.to() + " " + e.edgeLabel());
            bw.newLine();
        }
        bw.newLine();

        for (Pattern child : p.children()) {
            if (!isFrequent(child) || !child.checkMin()) {
                continue;
            }
            save(child, bw);
        }
    }



    // TODO: 2020/3/31 enumeration
    public void enumerate() {
        long startTime = System.currentTimeMillis();
        this.trans = new TransLoader(this.data).loadTrans();
        System.out.println("Total trans: " + this.trans.size());
        this.absSup = (int) Math.ceil(this.trans.size() * this.minSup);

        List<Cluster> clusters;
        Map<DFSEdge, Pattern> edges;
        if (partition) {
            clusters = Cluster.partition(trans, similarity, 0);
            this.points = pointsCluster(clusters);
            edges = edges(points, clusters);
        } else {
            this.points = points(this.trans);
            edges = edges(points);
        }

        List<Pattern> patterns = new ArrayList<>(edges.values());
        for (PointPattern pp : this.points.values()) {
            if (!isFrequent(pp)) {
                continue;
            }
            this.pointCount++;
            this.patternCount++;
            pw.save(pp, this.patternCount);
            for (Pattern p : pp.children()) {
                if (!isFrequent(p)) {
                    continue;
                }
                if (p.code().edgeSize() >= maxEdgeSize) {
                    break;
                }
                subgraphMining(trans, p);
            }
        }
        pw.close();
        System.out.println(this.pointCount + " point patterns");
        System.out.println((this.patternCount - this.pointCount) + " connected patterns.");

        long endTime = System.currentTimeMillis();
        System.out.println("Duration = " + (endTime - startTime));
    }

    public TreeMap<Integer, PointPattern> pointsCluster(List<Cluster> clusters) {
        TreeMap<Integer, PointPattern> points = new TreeMap<>();
        for (Cluster c : clusters) {
            intersectionPoints(c, points);
            otherPoints(c, points);
        }
        return points;
    }

    private void intersectionPoints(Cluster c, Map<Integer, PointPattern> points) {
        Cluster.Intersection inter = c.intersection();
        Map<Integer, LabeledVertex> border = c.border();
        for (LabeledVertex v : inter.vertices()) {
            PointPattern pattern = points.get(inter.vLabel(v));
            if (pattern == null) {
                pattern = new PointPattern(inter.vLabel(v));
                points.put(pattern.label(), pattern);
            }
            Embedding em = new Embedding(v, null);
            if (isBorderEmbedding(em, border)) {
                pattern.addBorderEmbedding(c, em);
            } else {
                pattern.addIntersectionEmbedding(c, em);
            }
            if (v.id() > maxVid) {
                maxVid = v.id();
            }
        }
    }

    private void otherPoints(Cluster c, Map<Integer, PointPattern> points) {
        for (LabeledGraph g : c.snapshots()) {
            Cluster.DeltaGraph dg = c.deltaGraph(g);
            Map<Integer, LabeledVertex> border = dg.border();
            for (LabeledVertex v : g.vertices()) {
                if (border.containsKey(v.id())) {
                    continue;
                }
                PointPattern pattern = points.get(g.vLabel(v));
                if (pattern == null) {
                    pattern = new PointPattern(g.vLabel(v));
                    points.put(pattern.label(), pattern);
                }
                Embedding em = new Embedding(v, null);
                pattern.addEmbedding(g, em);
                if (v.id() > maxVid) {
                    maxVid = v.id();
                }
            }
        }
    }

    public Map<DFSEdge, Pattern> edges(Map<Integer, PointPattern> points, List<Cluster> clusters) {
        Map<DFSEdge, Pattern> edges = new TreeMap<>();
        for (Cluster c : clusters) {
            intersectionEdges(c, points, edges);
            otherEdges(c, points, edges);
        }
        return edges;
    }

    private void intersectionEdges(Cluster c, Map<Integer, PointPattern> points, Map<DFSEdge, Pattern> edges) {
        Cluster.Intersection inter = c.intersection();

        for (PointPattern p : points.values()) {
            for (Embedding em : p.borderEmbeddings(c)) {
                for (LabeledEdge e : inter.adjEdges(em.vertex().id())) {
                    if (inter.vLabel(e.from()) > inter.vLabel(e.to())) {
                        continue;
                    }
                    Pattern child = p.child(0, 1, inter.vLabel(e.from()), inter.vLabel(e.to()), inter.eLabel(e));
                    child.addBorderEmbedding(c, new Embedding(e.to(), em));
//                    edges.put(child.edge(), child);
                }
            }

            for (Embedding em : p.intersectionEmbeddings(c)) {
                for (LabeledEdge e : inter.adjEdges(em.vertex().id())) {
                    if (inter.vLabel(e.from()) > inter.vLabel(e.to())) {
                        continue;
                    }
                    Pattern child = p.child(0, 1, inter.vLabel(e.from()), inter.vLabel(e.to()), inter.eLabel(e));
                    child.addIntersectionEmbedding(c, new Embedding(e.to(), em));
//                    edges.put(child.edge(), child);
                }
            }
        }
    }

    private void otherEdges (Cluster c, Map<Integer, PointPattern> points, Map<DFSEdge, Pattern> edges) {
        for (LabeledGraph g : c) {
            Cluster.DeltaGraph delta = c.deltaGraph(g);
            for (PointPattern p : points.values()) {
                for (Embedding em : p.embeddings(g)) {
                    for (LabeledEdge e : delta.adjEdges(em.vertex().id())) {
                        if (g.vLabel(e.from()) > g.vLabel(e.to())) {
                            continue;
                        }
                        Pattern child = p.child(0, 1, g.vLabel(e.from()), g.vLabel(e.to()), g.eLabel(e));
                        child.addEmbedding(g, new Embedding(e.to(), em));
//                        edges.put(child.edge(), child);
                    }
                }

                for (Embedding em : p.borderEmbeddings(c)) {
                    if (!delta.border().containsKey(em.vertex().id())) {
                        continue;
                    }
                    for (LabeledEdge e : delta.adjEdges(em.vertex().id())) {
                        if (g.vLabel(e.from()) > g.vLabel(e.to())) {
                            continue;
                        }
                        Pattern child = p.child(0, 1, g.vLabel(e.from()), g.vLabel(e.to()), g.eLabel(e));
                        child.addEmbedding(g, new Embedding(e.to(), em));
//                        edges.put(child.edge(), child);
                    }
                }
            }
        }
    }

    private boolean isBorderEmbedding(Embedding em, Map<Integer, LabeledVertex> border) {
        for (LabeledVertex v : em.vertices()) {
            if (border.containsKey(v.id())) {
                return true;
            }
        }
        return false;
    }

    public TreeMap<Integer, PointPattern> points(List<LabeledGraph> trans) {
        TreeMap<Integer, PointPattern> points = new TreeMap<>();
        for (LabeledGraph g : trans) {
            for (LabeledVertex v : g.vertices()) {
                PointPattern pp = points.get(g.vLabel(v));
                if (pp == null) {
                    pp = new PointPattern(g.vLabel(v));
                    points.put(pp.label(), pp);
                }
                pp.addEmbedding(g, new Embedding(v, null));
                if (v.id() > maxVid) {
                    maxVid = v.id();
                }
            }
        }
        return points;
    }

    public Map<DFSEdge, Pattern> edges(Map<Integer, PointPattern> points) {
        TreeMap<DFSEdge, Pattern> eMap = new TreeMap<>();
        for (PointPattern pp : points.values()) {
            for (LabeledGraph g : pp.unClusteredGraphs()) {
                for (Embedding em : pp.embeddings(g)) {
                    LabeledVertex from = em.vertex();
                    for (LabeledEdge e : g.adjEdges(from.id())) {
                        LabeledVertex to = e.to();
                        if (g.vLabel(from) > g.vLabel(to)) {
                            continue;
                        }
                        Pattern child = pp.child(0, 1, g.vLabel(from), g.vLabel(to), g.eLabel(e));
                        child.addEmbedding(g, new Embedding(to, em));
//                        eMap.put(child.edge(), child);
                    }
                }
            }
        }
        return eMap;
    }


    public void subgraphMining(List<LabeledGraph> trans, Pattern parent) {
        if (!parent.checkMin()) {
            return;
        }
        this.patternCount++;
        pw.save(parent, this.patternCount);
        if (parent.code().edgeSize() >= maxEdgeSize) {
            return;
        }

        List<Pattern> children = enumerateChildren(parent);
        if (!parent.hasChild()) {
            return;
        }
        for (Pattern child : parent.children()) {
            if (!isFrequent(child)) {
                parent.removeChild(child);
                continue;
            }
            subgraphMining(trans, child);
            parent.removeChild(child);
        }
    }

    private List<Pattern> enumerateChildren(Pattern p) {
        TreeMap<DFSEdge, Pattern> children = new TreeMap<>();

        TreeMap<Integer, TreeSet<DFSEdge>> joinBackCands = new TreeMap<>();
        TreeMap<Integer, TreeSet<DFSEdge>> joinForCands = new TreeMap<>();
        joinCands(p, joinBackCands, joinForCands);

        TreeSet<DFSEdge> extendCands = new TreeSet<>();
        extendCands(p, extendCands);

        for (Cluster c : p.clusters()) {
            joinExtendInter(c, p, joinBackCands, joinForCands, extendCands);
            joinExtendDelta(c, p, joinBackCands, joinForCands, extendCands);
        }
        for (LabeledGraph g : p.unClusteredGraphs()) {
            joinExtendOther(g, p, joinBackCands, joinForCands, extendCands);
        }

        return new ArrayList<>(children.values());
    }

    private Pattern updateInterExpansion(Cluster c, int dfsFrom, int dfsTo, int fromLabel, int toLabel, int eLabel, Embedding em, Pattern parent) {
        Pattern child = parent.child(dfsFrom, dfsTo, fromLabel, toLabel, eLabel);
        child.addIntersectionEmbedding(c, em);
        return child;
    }

    private Pattern updateBorderExpansion(Cluster c, int dfsFrom, int dfsTo, int fromLabel, int toLabel, int eLabel, Embedding em, Pattern parent) {
        Pattern child = parent.child(dfsFrom, dfsTo, fromLabel, toLabel, eLabel);
        child.addBorderEmbedding(c, em);
        return child;
    }

    private Pattern updateOtherExpansion(LabeledGraph g, int dfsFrom, int dfsTo, int fromLabel, int toLabel, int eLabel, Embedding em, Pattern parent) {
        Pattern child = parent.child(dfsFrom, dfsTo, fromLabel, toLabel, eLabel);
        child.addEmbedding(g, em);
        return child;
    }

    private void joinExtendInter(Cluster c, Pattern p, TreeMap<Integer, TreeSet<DFSEdge>> backCand, TreeMap<Integer, TreeSet<DFSEdge>> forCand, TreeSet<DFSEdge> extendCands) {
        LabeledGraph inter = c.intersection();
        List<Embedding> interEmbeddings = p.intersectionEmbeddings(c);
        List<Embedding> borderEmbeddings = p.borderEmbeddings(c);
        DFSCode code = p.code();
        List<Integer> rmPathIds = code.rightMostPath();
        int rmDfsId = rmPathIds.get(rmPathIds.size() - 1);

        for (int interEmCount = 0, borderEmCount = 0 - interEmbeddings.size(); interEmCount < interEmbeddings.size() || borderEmCount < borderEmbeddings.size(); interEmCount++, borderEmCount++) {
            Embedding em;
            if (borderEmCount < 0) {
                em = interEmbeddings.get(interEmCount);
            } else {
                em = borderEmbeddings.get(borderEmCount);
            }
            List<LabeledVertex> emVertices = em.vertices();
            //join backward edges
            for (Map.Entry<Integer, TreeSet<DFSEdge>> entry : backCand.entrySet()) {
                LabeledVertex from = emVertices.get(emVertices.size() - 1);
                LabeledVertex to = emVertices.get(entry.getKey());
                LabeledEdge back = inter.edge(from.id(), to.id());
                if (back == null) {
                    continue;
                }
                TreeSet<DFSEdge> cands = entry.getValue();
                DFSEdge dfsEdge = new DFSEdge(rmDfsId, entry.getKey(), inter.vLabel(from), inter.vLabel(to), inter.eLabel(back));
                if (cands.contains(dfsEdge)) {
                    Pattern child = p.child(dfsEdge);
                    if (borderEmCount < 0) {
                        child.addIntersectionEmbedding(c, em);
                    } else {
                        child.addBorderEmbedding(c, em);
                    }
                }
            }


            //join forward edge
            BitSet emBits = new BitSet(maxVid + 1);
            for (LabeledVertex v : emVertices) {
                emBits.set(v.id());
            }

            for (Map.Entry<Integer, TreeSet<DFSEdge>> entry : forCand.entrySet()) {
                LabeledVertex from = emVertices.get(entry.getKey());
                for (LabeledEdge e : inter.adjEdges(from.id())) {
                    if (emBits.get(e.to().id())) {
                        continue;
                    }
                    DFSEdge dfsEdge = new DFSEdge(entry.getKey(), emVertices.size(), inter.vLabel(from), inter.vLabel(e.to()), inter.eLabel(e));
                    TreeSet<DFSEdge> cands = entry.getValue();
                    if (cands.contains(dfsEdge)) {
                        Pattern child = p.child(dfsEdge);;
                        if (borderEmCount < 0) {
                            child.addIntersectionEmbedding(c, new Embedding(e.to(), em));
                        } else {
                            child.addBorderEmbedding(c, new Embedding(e.to(), em));
                        }
                    }
                }
            }

            //extend backward edges
            LabeledVertex from = emVertices.get(rmDfsId);
            for (int j = 0; j < rmPathIds.size() - 2; j++) {
                int toId = rmPathIds.get(j);
                LabeledVertex to = emVertices.get(toId);
                LabeledEdge back = inter.edge(from.id(), to.id());
                if (back == null) {
                    continue;
                }

                DFSEdge dfsEdge;
                if (inter.vLabel(from) <= inter.vLabel(to)) {
                    dfsEdge = new DFSEdge(0, 1, inter.vLabel(from), inter.vLabel(to), inter.eLabel(back));
                } else {
                    dfsEdge = new DFSEdge(0, 1, inter.vLabel(to), inter.vLabel(from), inter.eLabel(back));
                }
                if (extendCands.contains(dfsEdge)) {
                    Pattern child = p.child(rmDfsId, toId, inter.vLabel(from), inter.vLabel(to), inter.eLabel(back));
                    if (borderEmCount < 0) {
                        child.addIntersectionEmbedding(c, em);
                    } else {
                        child.addBorderEmbedding(c, em);
                    }
                }
            }

            //extend forward edges
            for (LabeledEdge e : inter.adjEdges(from.id())) {
                LabeledVertex to = e.to();
                if (emBits.get(to.id())) {
                    continue;
                }
                DFSEdge dfsEdge;
                if (inter.vLabel(from) <= inter.vLabel(to)) {
                    dfsEdge = new DFSEdge(0, 1, inter.vLabel(from), inter.vLabel(to), inter.eLabel(e));
                } else {
                    dfsEdge = new DFSEdge(0, 1, inter.vLabel(to), inter.vLabel(from), inter.eLabel(e));
                }
                if (extendCands.contains(dfsEdge)) {
                    Pattern child = p.child(rmDfsId, emVertices.size(), inter.vLabel(from), inter.vLabel(to), inter.eLabel(e));;
                    if (borderEmCount < 0) {
                        child.addIntersectionEmbedding(c, new Embedding(e.to(), em));
                    } else {
                        child.addBorderEmbedding(c, new Embedding(e.to(), em));
                    }
                }
            }

        }
    }

    private void joinExtendDelta(Cluster c, Pattern p, TreeMap<Integer, TreeSet<DFSEdge>> backCand, TreeMap<Integer, TreeSet<DFSEdge>> forCand, TreeSet<DFSEdge> extendCands) {
        DFSCode code = p.code();
        List<Integer> rmPathIds = code.rightMostPath();
        int rmDfsId = rmPathIds.get(rmPathIds.size() - 1);
        for (LabeledGraph g : c) {
            Cluster.DeltaGraph dg = c.deltaGraph(g);

            for (Embedding em : p.borderEmbeddings(c)) {
                if (!isBorderEmbedding(em, dg.border())) {
                    continue;
                }
                List<LabeledVertex> emVertices = em.vertices();
                //join backward edges
                for (Map.Entry<Integer, TreeSet<DFSEdge>> entry : backCand.entrySet()) {
                    LabeledVertex from = emVertices.get(emVertices.size() - 1);
                    LabeledVertex to = emVertices.get(entry.getKey());
                    LabeledEdge back = dg.edge(from.id(), to.id());
                    if (back == null) {
                        continue;
                    }
                    TreeSet<DFSEdge> cands = entry.getValue();
                    DFSEdge dfsEdge = new DFSEdge(rmDfsId, entry.getKey(), dg.vLabel(from), dg.vLabel(to), dg.eLabel(back));
                    if (cands.contains(dfsEdge)) {
                        Pattern child = p.child(dfsEdge);
                        child.addEmbedding(g, em);
                    }
                }

                //join forward edge
                BitSet emBits = new BitSet(maxVid + 1);
                for (LabeledVertex v : emVertices) {
                    emBits.set(v.id());
                }

                for (Map.Entry<Integer, TreeSet<DFSEdge>> entry : forCand.entrySet()) {
                    LabeledVertex from = emVertices.get(entry.getKey());
                    for (LabeledEdge e : dg.adjEdges(from.id())) {
                        if (emBits.get(e.to().id())) {
                            continue;
                        }
                        DFSEdge dfsEdge = new DFSEdge(entry.getKey(), emVertices.size(), dg.vLabel(from), dg.vLabel(e.to()), dg.eLabel(e));
                        TreeSet<DFSEdge> cands = entry.getValue();
                        if (cands.contains(dfsEdge)) {
                            Pattern child = p.child(dfsEdge);
                            child.addEmbedding(g, new Embedding(e.to(), em));
                        }
                    }
                }

                //extend
                //extend backward edges
                LabeledVertex from = emVertices.get(rmDfsId);
                for (int j = 0; j < rmPathIds.size() - 2; j++) {
                    int toId = rmPathIds.get(j);
                    LabeledVertex to = emVertices.get(toId);
                    LabeledEdge back = dg.edge(from.id(), to.id());
                    if (back == null) {
                        continue;
                    }

                    DFSEdge dfsEdge;
                    if (dg.vLabel(from) <= dg.vLabel(to)) {
                        dfsEdge = new DFSEdge(0, 1, dg.vLabel(from), dg.vLabel(to), dg.eLabel(back));
                    } else {
                        dfsEdge = new DFSEdge(0, 1, dg.vLabel(to), dg.vLabel(from), dg.eLabel(back));
                    }
                    if (extendCands.contains(dfsEdge)) {
                        Pattern child = p.child(rmDfsId, toId, dg.vLabel(from), dg.vLabel(to), dg.eLabel(back));
                        child.addEmbedding(g, em);
                    }
                }

                //extend forward edges
                for (LabeledEdge e : dg.adjEdges(from.id())) {
                    LabeledVertex to = e.to();
                    if (emBits.get(to.id())) {
                        continue;
                    }
                    DFSEdge dfsEdge;
                    if (dg.vLabel(from) <= dg.vLabel(to)) {
                        dfsEdge = new DFSEdge(0, 1, dg.vLabel(from), dg.vLabel(to), dg.eLabel(e));
                    } else {
                        dfsEdge = new DFSEdge(0, 1, dg.vLabel(to), dg.vLabel(from), dg.eLabel(e));
                    }
                    if (extendCands.contains(dfsEdge)) {
                        Pattern child = p.child(rmDfsId, emVertices.size(), dg.vLabel(from), dg.vLabel(to), dg.eLabel(e));
                        child.addEmbedding(g, new Embedding(to, em));
                    }
                }
            }
        }
    }

    private void joinExtendOther(LabeledGraph g, Pattern p, TreeMap<Integer, TreeSet<DFSEdge>> backCand, TreeMap<Integer, TreeSet<DFSEdge>> forCand, TreeSet<DFSEdge> extendCands) {
        List<Embedding> embeddings = p.embeddings(g);
        if (embeddings == null) {
            return;
        }
        DFSCode code = p.code();
        List<Integer> rmPathIds = code.rightMostPath();
        int rmDfsId = rmPathIds.get(rmPathIds.size() - 1);
        for (Embedding em : embeddings) {
            List<LabeledVertex> emVertices = em.vertices();

            //join backward edges
            for (Map.Entry<Integer, TreeSet<DFSEdge>> entry : backCand.entrySet()) {
                LabeledVertex from = emVertices.get(emVertices.size() - 1);
                LabeledVertex to = emVertices.get(entry.getKey());
                LabeledEdge back = g.edge(from.id(), to.id());
                if (back == null) {
                    continue;
                }

                TreeSet<DFSEdge> cands = entry.getValue();
                DFSEdge dfsEdge = new DFSEdge(rmDfsId, entry.getKey(), g.vLabel(from), g.vLabel(to), g.eLabel(back));
                if (cands.contains(dfsEdge)) {
                    Pattern child = p.child(dfsEdge);
                    child.addEmbedding(g, em);
                }
            }

            //join forward edges
            BitSet emBits = new BitSet(maxVid + 1);
            for (LabeledVertex v : emVertices) {
                emBits.set(v.id());
            }
            for (Map.Entry<Integer, TreeSet<DFSEdge>> entry : forCand.entrySet()) {
                LabeledVertex from = emVertices.get(entry.getKey());
                for (LabeledEdge e : g.adjEdges(from.id())) {
                    if (emBits.get(e.to().id())) {
                        continue;
                    }

                    DFSEdge dfsEdge = new DFSEdge(entry.getKey(), emVertices.size(), g.vLabel(from), g.vLabel(e.to()), g.eLabel(e));
                    TreeSet<DFSEdge> cands = entry.getValue();
                    if (cands.contains(dfsEdge)) {
                        Pattern child = p.child(dfsEdge);
                        child.addEmbedding(g, new Embedding(e.to(), em));
                    }
                }
            }

            //extend
            //extend backward edges
            LabeledVertex from = emVertices.get(rmDfsId);
            for (int j = 0; j < rmPathIds.size() - 2; j++) {
                int toId = rmPathIds.get(j);
                LabeledVertex to = emVertices.get(toId);
                LabeledEdge back = g.edge(from.id(), to.id());
                if (back == null) {
                    continue;
                }
                LabeledVertex nextTo = emVertices.get(rmPathIds.get(j + 1));
                LabeledEdge pathEdge = g.edge(to.id(), nextTo.id());
                if (g.eLabel(pathEdge) > g.eLabel(back) || (g.eLabel(pathEdge) == g.eLabel(back) && g.vLabel(nextTo) > g.vLabel(back.from()))) {
                    continue;
                }

                DFSEdge dfsEdge;
                if (g.vLabel(from) <= g.vLabel(to)) {
                    dfsEdge = new DFSEdge(0, 1, g.vLabel(from), g.vLabel(to), g.eLabel(back));
                } else {
                    dfsEdge = new DFSEdge(0, 1, g.vLabel(to), g.vLabel(from), g.eLabel(back));
                }
                if (extendCands.contains(dfsEdge)) {
                    Pattern child = p.child(rmDfsId, toId, g.vLabel(from), g.vLabel(to), g.eLabel(back));
                    child.addEmbedding(g, em);
                }
            }

            //extend rm forward edges
            for (LabeledEdge e : g.adjEdges(from.id())) {
                LabeledVertex to = e.to();
                if (emBits.get(to.id())) {
                    continue;
                }

                DFSEdge dfsEdge;
                if (g.vLabel(from) <= g.vLabel(to)) {
                    dfsEdge = new DFSEdge(0, 1, g.vLabel(from), g.vLabel(to), g.eLabel(e));
                } else {
                    dfsEdge = new DFSEdge(0, 1, g.vLabel(to), g.vLabel(from), g.eLabel(e));
                }
                if (extendCands.contains(dfsEdge)) {
                    Pattern child = p.child(rmDfsId, emVertices.size(), g.vLabel(from), g.vLabel(to), g.eLabel(e));
                    child.addEmbedding(g, new Embedding(to, em));
                }
            }

        }
    }

    private void joinCands(Pattern p, TreeMap<Integer, TreeSet<DFSEdge>> backCand, TreeMap<Integer, TreeSet<DFSEdge>> forCand) {
        DFSEdge e1 = p.edge();
        for (Pattern sib : p.rightSiblings()) {
            if (!isFrequent(sib)) {
                continue;
            }
            DFSEdge e2 = sib.edge();
            if (e1.compareTo(e2) > 0) {
                continue;
            }

            if (!e1.isForward() && !e2.isForward() && e1.to() == e2.to()) {
                continue;
            }

            TreeSet<DFSEdge> candidates;
            if (!e2.isForward()) {
                candidates = backCand.computeIfAbsent(e2.to(), vIndex -> new TreeSet<>());
                candidates.add(e2);
            } else {
                candidates = forCand.computeIfAbsent(e2.from(), vIndex -> new TreeSet<>());
                candidates.add(new DFSEdge(e2.from(), p.code().nodeCount(), e2.fromLabel(), e2.toLabel(), e2.edgeLabel()));
            }
        }
    }

    private void extendCands(Pattern p, TreeSet<DFSEdge> extendCands) {
        DFSEdge lastEdge = p.edge();
        if (!lastEdge.isForward()) {
            return;
        }
        DFSEdge firstEdge = p.code().get(0);
        for (Pattern ep : this.points.get(firstEdge.fromLabel()).children()) {
            if (!isFrequent(ep)) {
                continue;
            }
            DFSEdge e = ep.edge();
            if (e.toLabel() == lastEdge.toLabel() && e.edgeLabel() >= firstEdge.edgeLabel()) {
                extendCands.add(e);
            }
        }
        for (PointPattern pp : this.points.tailMap(firstEdge.fromLabel(), false).headMap(lastEdge.toLabel()).values()) {
            if (!isFrequent(pp)) {
                continue;
            }
            for (Pattern ep : pp.children()) {
                if (!isFrequent(ep)) {
                    continue;
                }
                DFSEdge e = ep.edge();
                if (e.toLabel() == lastEdge.toLabel()) {
                    extendCands.add(e);
                }
            }
        }
        PointPattern rmPoint = this.points.get(lastEdge.toLabel());
        if (rmPoint != null && isFrequent(rmPoint)) {
            for (Pattern ep : rmPoint.children()) {
                if (!isFrequent(ep)) {
                    continue;
                }
                extendCands.add(ep.edge());
            }
        }
    }

    private boolean isFrequent(Pattern p) {
        return p.frequency() >= this.absSup;
    }

}
