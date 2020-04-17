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
    private int maxEdgeSize;
    private boolean partition;
    private double similarity;
    private int clusterCounter;
    private int patternCount = 0;

//    private LabeledGraph transDelimiter;
//    private Cluster clusterDelimiter;

    private int maxVid = 0;

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


    public FSMHG(File data, File output, double minSupport, int maxEdgeSize, boolean partition, double similarity) {
        this.data = data;
        this.output = output;
        this.minSup = minSupport;
        this.maxEdgeSize = maxEdgeSize;
        this.partition = partition;
        this.similarity = similarity;
        this.points = new TreeMap<>();
    }

    private List<LabeledGraph> loadTrans() {
        List<LabeledGraph> trans = new ArrayList<>();
        if (!this.data.isDirectory()) {
            trans.addAll(readTrans(this.data));
        } else {
            for (File f : this.data.listFiles()) {
                trans.addAll(readTrans(f));
            }
        }
        return trans;
    }

    private List<LabeledGraph> readTrans(File file) {
        List<LabeledGraph> trans = new ArrayList<>();
        FileReader fr = null;
        BufferedReader br = null;
        try {
            fr = new FileReader(file);
            br = new BufferedReader(fr);
            String line;
            LabeledGraph g = null;
            while ((line = br.readLine()) != null) {
                String[] str = line.split(" ");
                if (line.startsWith("t")) {
                    int id = Integer.parseInt(line.split(" ")[2]);
                    if (id >= 0) {
                        g = new StaticGraph(id);
                        trans.add(g);
                    }
                } else if (line.startsWith("v")) {
                    g.addVertex(Integer.parseInt(str[1]), Integer.parseInt(str[2]));
                } else if (line.startsWith("e")) {
                    int from = Integer.parseInt(str[1]);
                    int to = Integer.parseInt(str[2]);
                    int eLabel = Integer.parseInt(str[3]);
                    g.addEdge(from, to, eLabel);
                    g.addEdge(to, from, eLabel);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
                fr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return trans;
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
        this.trans = loadTrans();
        System.out.println("Total trans: " + this.trans.size());

        long startTime = System.currentTimeMillis();
        List<Cluster> clusters;
//        Map<Integer, PointPattern> points;
        Map<DFSEdge, Pattern> edges;
        if (partition) {
            clusters = Cluster.partition(trans, similarity, 0);
            this.points = pointsCluster(clusters);
            edges = edges(points, clusters);
        } else {
            long pointBegin = System.currentTimeMillis();
            this.points = points(this.trans);
            long pointEnd = System.currentTimeMillis();
            pointTime = pointEnd - pointBegin;

            long edgesBegin = System.currentTimeMillis();
            edges = edges(points);
            long edgesEnd = System.currentTimeMillis();
            edgeTime = edgesEnd - edgesBegin;
        }

        List<Pattern> patterns = new ArrayList<>(edges.values());
        for (int i = 0; i < patterns.size(); i++) {
            Pattern p = patterns.get(i);
            if (!isFrequent(p)) {
                continue;
            }
            if (p.code().edgeSize() >= maxEdgeSize) {
                break;
            }
//            long begin = System.currentTimeMillis();
            subgraphMining(trans, p);
//            long end = System.currentTimeMillis();
//            System.out.println(i + " subgraphMining time " + (end - begin));
        }

        long saveTime = System.currentTimeMillis();
        saveResult();
        long endTime = System.currentTimeMillis();
        System.out.println("Duration = " + (endTime - startTime));
        System.out.println("Save time = " + (endTime - saveTime));

        minCodeCheckTimeTest();
        System.out.println("Total join time = " + joinTime);
        System.out.println("Total extend time = " + extendTime);
        System.out.println("Join cand time = " + joinCandTime);
        System.out.println("Extend cand time = " + extendCandTime);
        System.out.println("Join common grpah time = " + joinCommonGraphTime);
        System.out.println("Extend common graph time = " + extendCommonGraphTime);
        System.out.println("Actual join time = " + actualJoinTime);
        System.out.println("Actual extend time = " + actualExtendTime);
        System.out.println("Points time = " + pointTime);
        System.out.println("Edges time = " + edgeTime);
        System.out.println("Embedding vertices time = " + emVerticesTime);
        System.out.println("Embedding bits time = " + emBitsTime);
        System.out.println("Embedding bits check time " + emBitsCheckTime);
        System.out.println("Candidates check time = " + candCheckTime);
        System.out.println("Child insert time = " + insertChildTime);
    }

    private void minCodeCheckTimeTest() {
        long begin = System.currentTimeMillis();
        List<Pattern> patterns = new ArrayList<>();
        for (PointPattern pp : this.points.values()) {
            collectPatterns(pp, patterns);
        }
        System.out.println("Total number of connected patterns " + patterns.size());

        for (Pattern p : patterns) {
            p.code().isMin();
        }
        long end = System.currentTimeMillis();
        System.out.println("Min code check time = " + (end - begin));
    }

    private void collectPatterns(Pattern p, List<Pattern> patterns) {
        for (Pattern child : p.children()) {
            patterns.add(child);
            collectPatterns(child, patterns);
        }
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
//            points.put(pattern.label(), pattern);
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
//                points.put(pattern.label(), pattern);
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
                    if (child == null) {
                        child = p.addChild(0, 1, inter.vLabel(e.from()), inter.vLabel(e.to()), inter.eLabel(e));
                    }
                    child.addBorderEmbedding(c, new Embedding(e.to(), em));
                    edges.put(child.edge(), child);
                }
            }

            for (Embedding em : p.intersectionEmbeddings(c)) {
                for (LabeledEdge e : inter.adjEdges(em.vertex().id())) {
                    if (inter.vLabel(e.from()) > inter.vLabel(e.to())) {
                        continue;
                    }
                    Pattern child = p.child(0, 1, inter.vLabel(e.from()), inter.vLabel(e.to()), inter.eLabel(e));
                    if (child == null) {
                        child = p.addChild(0, 1, inter.vLabel(e.from()), inter.vLabel(e.to()), inter.eLabel(e));
                    }
                    child.addIntersectionEmbedding(c, new Embedding(e.to(), em));
                    edges.put(child.edge(), child);
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
                        if (child == null) {
                            child = p.addChild(0, 1, g.vLabel(e.from()), g.vLabel(e.to()), g.eLabel(e));
                        }
                        child.addEmbedding(g, new Embedding(e.to(), em));
                        edges.put(child.edge(), child);
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
                        if (child == null) {
                            child = p.addChild(0, 1, g.vLabel(e.from()), g.vLabel(e.to()), g.eLabel(e));
                        }
                        child.addEmbedding(g, new Embedding(e.to(), em));
                        edges.put(child.edge(), child);
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
//                points.put(pp.label(), pp);
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
                        Pattern child = updateOtherExpansion(g, 0, 1, g.vLabel(from), g.vLabel(to), g.eLabel(e), new Embedding(to, em), pp);
                        eMap.put(child.edge(), child);
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

        List<Pattern> children = enumerateChildren(parent);
        if (children == null || children.size() == 0) {
            return;
        }
        for (int i = 0; i < children.size(); i++) {
            Pattern p = children.get(i);
            if (p.code().edgeSize() >= maxEdgeSize) {
                return;
            }
            if (!isFrequent(p)) {
                continue;
            }
            subgraphMining(trans, p);
        }
    }

    private List<Pattern> enumerateChildren(Pattern p) {
        TreeMap<DFSEdge, Pattern> children = new TreeMap<>();

        long joinBegin = System.currentTimeMillis();
        join(p, p.rightSiblings(), children);
        long joinEnd = System.currentTimeMillis();
        joinTime += (joinEnd - joinBegin);
        if (p.edge().isForward()) {
            long extendBegin = System.currentTimeMillis();
            extend(p, children);
            long extendEnd = System.currentTimeMillis();
            extendTime += (extendEnd - extendBegin);
        }

        return new ArrayList<>(children.values());

//        for (Cluster c : p.clusters(clusterDemimiter)) {
//            expandInterEmbeddings(p, c, newChildren);
//            expandBorderEmbeddings(p, c, newChildren);
//        }
//        for (LabeledGraph g : p.unClusteredGraphs(graphDelimiter)) {
//            expandOtherEmbeddings(p, g, newChildren);
//        }
//        return new ArrayList<>(newChildren.values());
    }

    private void expandInterEmbeddings(Pattern p, Cluster c, Map<DFSEdge, Pattern> newChildren) {
        LabeledGraph inter = c.intersection();
        List<Integer> rmPath = p.code().rightMostPath();
        LabeledGraph pg = p.code().toGraph();

        for (Embedding em : p.intersectionEmbeddings(c)) {
            List<LabeledVertex> emVertices = em.vertices();
            BitSet emBit = new BitSet();
            for (LabeledVertex v : emVertices) {
                emBit.set(v.id());
            }
            for (int i = 0; i < rmPath.size() - 1; i++) {
                // TODO: 2020/3/28 backward edge, use join operation
                LabeledEdge backEdge = getBackwardEdge(inter, pg, emVertices, rmPath.get(i), rmPath.get(rmPath.size() - 1));
                if (backEdge == null) {
                    Pattern child = updateInterExpansion(c, rmPath.get(rmPath.size() - 1), rmPath.get(i), inter.vLabel(backEdge.to()), inter.vLabel(backEdge.from()), inter.eLabel(backEdge), em, p);
                    newChildren.put(child.edge(), child);
                }

                // TODO: 2020/3/28 forward edges on right most paht vertex, use join operation
                List<LabeledEdge> forwardEdges = getRmPathForward(inter, emVertices, emBit, i);
                for (LabeledEdge e : forwardEdges) {
                    Pattern child = updateInterExpansion(c, rmPath.get(i), emVertices.size(), inter.vLabel(e.from()), inter.vLabel(backEdge.to()), inter.eLabel(e), new Embedding(e.to(), em), p);
                    newChildren.put(child.edge(), child);
                }
            }
            // TODO: 2020/3/28 forward edges on right most path, use join operation
            List<LabeledEdge> rmForwardEdges = getRmVertexForward(inter, emVertices, emBit, rmPath.get(rmPath.size() - 1));
            for (LabeledEdge e : rmForwardEdges) {
                Pattern child = updateInterExpansion(c, rmPath.get(rmPath.size() - 1), emVertices.size(), inter.vLabel(e.from()), inter.vLabel(e.to()), inter.eLabel(e), new Embedding(e.to(), em), p);
                newChildren.put(child.edge(), child);
            }
        }
    }

    private void expandBorderEmbeddings(Pattern p, Cluster c, Map<DFSEdge, Pattern> newChildren) {
        LabeledGraph inter = c.intersection();
        List<Integer> rmPath = p.code().rightMostPath();
        LabeledGraph pg = p.code().toGraph();

        for (Embedding em : p.borderEmbeddings(c)) {
            List<LabeledVertex> emVertices = em.vertices();
            BitSet emBit = new BitSet();
            for (LabeledVertex v : emVertices) {
                emBit.set(v.id());
            }
            for (int i = 0; i < rmPath.size() - 1; i++) {
                // TODO: 2020/3/28 backward edge, use join operation
                LabeledEdge backEdge = getBackwardEdge(inter, pg, emVertices, rmPath.get(i), rmPath.get(rmPath.size() - 1));
                if (backEdge == null) {
                    Pattern child = updateBorderExpansion(c, rmPath.get(rmPath.size() - 1), emVertices.size(), inter.vLabel(backEdge.to()), inter.vLabel(backEdge.from()), inter.eLabel(backEdge), em, p);
                    newChildren.put(child.edge(), child);
                }

                // TODO: 2020/3/28 forward edges on right most path vertex, use join operation
                List<LabeledEdge> forwardEdges = getRmPathForward(inter, emVertices, emBit, rmPath.get(i));
                for (LabeledEdge e : forwardEdges) {
                    Pattern child = updateBorderExpansion(c, rmPath.get(i), emVertices.size(), inter.vLabel(e.from()), inter.vLabel(backEdge.to()), inter.eLabel(e), new Embedding(e.to(), em), p);
                    newChildren.put(child.edge(), child);
                }
            }
            // TODO: 2020/3/28 forward edges on right most path, use join operation
            List<LabeledEdge> rmForwardEdges = getRmVertexForward(inter, emVertices, emBit, rmPath.get(rmPath.size() - 1));
            for (LabeledEdge e : rmForwardEdges) {
                Pattern child = updateBorderExpansion(c, rmPath.get(rmPath.size() - 1), emVertices.size(), inter.vLabel(e.from()), inter.vLabel(e.to()), inter.eLabel(e), new Embedding(e.to(), em), p);
                newChildren.put(child.edge(), child);
            }


            for (LabeledGraph g : c) {
                for (int i = 0; i < rmPath.size() - 1; i++) {
                    LabeledEdge backEdge = getDeltaBackwardEdge(g, pg, c.deltaGraph(g), emVertices, rmPath.get(i), rmPath.get(rmPath.size() - 1));
                    if (backEdge != null) {
                        Pattern child = updateOtherExpansion(g, rmPath.get(rmPath.size() - 1), emVertices.size(), inter.vLabel(backEdge.to()), inter.vLabel(backEdge.from()), inter.eLabel(backEdge), em, p);
                        newChildren.put(child.edge(), child);
                    }

                    List<LabeledEdge> forwardEdges = getDeltaRmPathForward(g, c.deltaGraph(g), emVertices, emBit, rmPath.get(i));
                    for (LabeledEdge e : forwardEdges) {
                        Pattern child = updateOtherExpansion(g, rmPath.get(i), emVertices.size(), inter.vLabel(e.from()), inter.vLabel(backEdge.to()), inter.eLabel(e), new Embedding(e.to(), em), p);
                        newChildren.put(child.edge(), child);
                    }
                }

                rmForwardEdges = getDeltaRmVertexForward(g, c.deltaGraph(g), emVertices, emBit, rmPath.get(rmPath.size() - 1));
                for (LabeledEdge e : rmForwardEdges) {
                    Pattern child = updateOtherExpansion(g, rmPath.get(rmPath.size() - 1), emVertices.size(), inter.vLabel(e.from()), inter.vLabel(e.to()), inter.eLabel(e), new Embedding(e.to(), em), p);
                    newChildren.put(child.edge(), child);
                }
            }
        }
    }

    private void expandOtherEmbeddings(Pattern p, LabeledGraph g, Map<DFSEdge, Pattern> newChildren) {
        List<Integer> rmPath = p.code().rightMostPath();
        LabeledGraph pg = p.code().toGraph();

        for (Embedding em : p.embeddings(g)) {
            List<LabeledVertex> emVertices = em.vertices();
            BitSet emBit = new BitSet();
            for (LabeledVertex v : emVertices) {
                emBit.set(v.id());
            }
            for (int i = 0; i < rmPath.size() - 1; i++) {
                // TODO: 2020/3/28 backward edge, use join operation
                LabeledEdge backEdge = getBackwardEdge(g, pg, emVertices, rmPath.get(i), rmPath.get(rmPath.size() - 1));
                if (backEdge == null) {
                    updateOtherExpansion(g, rmPath.get(rmPath.size() - 1), rmPath.get(i), g.vLabel(backEdge.to()), g.vLabel(backEdge.from()), g.eLabel(backEdge), em, p);
                }

                // TODO: 2020/3/28 forward edges on right most path vertex, use join operation
                List<LabeledEdge> forwardEdges = getRmPathForward(g, emVertices, emBit, i);
                for (LabeledEdge e : forwardEdges) {
                    updateOtherExpansion(g, rmPath.get(i), emVertices.size(), g.vLabel(e.from()), g.vLabel(backEdge.to()), g.eLabel(e), new Embedding(e.to(), em), p);
                }
            }
            // TODO: 2020/3/28 forward edges on right most path, use join operation
            List<LabeledEdge> rmForwardEdges = getRmVertexForward(g, emVertices, emBit, rmPath.get(rmPath.size() - 1));
            for (LabeledEdge e : rmForwardEdges) {
                updateOtherExpansion(g, rmPath.get(rmPath.size() - 1), emVertices.size(), g.vLabel(e.from()), g.vLabel(e.to()), g.eLabel(e), new Embedding(e.to(), em), p);
            }
        }
    }

    private LabeledEdge getBackwardEdge(LabeledGraph g, LabeledGraph pg, List<LabeledVertex> vertices, int pathVertexIndex, int rmVertexIndex) {
        if (pg.edge(rmVertexIndex, pathVertexIndex) != null) {
            return null;
        }
        LabeledEdge pathEdge = g.edge(vertices.get(pathVertexIndex).id(), vertices.get(pathVertexIndex + 1).id());
        LabeledEdge backEdge = g.edge(vertices.get(rmVertexIndex).id(), vertices.get(pathVertexIndex).id());
        if (backEdge == null) {
            return null;
        }
        if (g.eLabel(pathEdge) < g.eLabel(backEdge)
                || (g.eLabel(pathEdge) == g.eLabel(backEdge) && g.vLabel(pathEdge.to()) <= g.vLabel(backEdge.from()))) {
            return backEdge;
        }
        return null;
    }

    private List<LabeledEdge> getRmVertexForward(LabeledGraph g, List<LabeledVertex> vertices, BitSet emBit, int rmVertexIndex) {
        List<LabeledEdge> forwardEdges = new ArrayList<>();
        LabeledEdge firstEdge = g.edge(vertices.get(0).id(), vertices.get(1).id());
        for (LabeledEdge e : g.adjEdges(vertices.get(rmVertexIndex).id())) {
            if (emBit.get(e.to().id())) {
                continue;
            }
            if (g.vLabel(firstEdge.from()) < g.vLabel(e.to())
                    || (g.vLabel(firstEdge.from()) == g.vLabel(e.to()) && g.eLabel(firstEdge) <= g.eLabel(e))) {
                forwardEdges.add(e);
            }
        }
        return forwardEdges;
    }

    private List<LabeledEdge> getRmPathForward(LabeledGraph g, List<LabeledVertex> vertices, BitSet emBit, int pathVertexIndex) {
        List<LabeledEdge> forwardEdges = new ArrayList<>();
        LabeledEdge pathEdge = g.edge(vertices.get(pathVertexIndex).id(), vertices.get(pathVertexIndex + 1).id());
        for (LabeledEdge e : g.adjEdges(vertices.get(pathVertexIndex).id())) {
            if (emBit.get(e.to().id())) {
                continue;
            }
            if (g.eLabel(pathEdge) < g.eLabel(e)
                    || (g.eLabel(pathEdge) == g.eLabel(e) && g.vLabel(pathEdge.to()) <= g.vLabel(e.to()))) {
                forwardEdges.add(e);
            }
        }
        return forwardEdges;
    }


    private LabeledEdge getDeltaBackwardEdge(LabeledGraph g, LabeledGraph pg, LabeledGraph delta, List<LabeledVertex> emVertices, int pathVertexIndex, int rmVertexIndex) {
        if (pg.edge(rmVertexIndex, pathVertexIndex) != null) {
            return null;
        }
        LabeledEdge pathEdge = g.edge(emVertices.get(pathVertexIndex).id(), emVertices.get(pathVertexIndex + 1).id());
        LabeledEdge backEdge = delta.edge(emVertices.get(rmVertexIndex).id(), emVertices.get(pathVertexIndex).id());
        if (backEdge == null) {
            return null;
        }

        if (g.eLabel(pathEdge) < g.eLabel(backEdge)
                || (g.eLabel(pathEdge) == g.eLabel(backEdge) && g.vLabel(pathEdge.to()) <= g.vLabel(backEdge.from()))) {
            return backEdge;
        }
        return null;
    }

    private List<LabeledEdge> getDeltaRmVertexForward(LabeledGraph g, LabeledGraph delta, List<LabeledVertex> vertices, BitSet emBit, int rmVertexIndex) {
        List<LabeledEdge> forwardEdges = new ArrayList<>();
        LabeledEdge firstEdge = g.edge(vertices.get(0).id(), vertices.get(1).id());
        for (LabeledEdge e : delta.adjEdges(vertices.get(rmVertexIndex).id())) {
            if (emBit.get(e.to().id())) {
                continue;
            }
            if (g.vLabel(firstEdge.from()) < g.vLabel(e.to())
                    || (g.vLabel(firstEdge.from()) == g.vLabel(e.to()) && g.eLabel(firstEdge) <= g.eLabel(e))) {
                forwardEdges.add(e);
            }
        }
        return forwardEdges;
    }

    private List<LabeledEdge> getDeltaRmPathForward(LabeledGraph g, LabeledGraph delta, List<LabeledVertex> vertices, BitSet emBit, int pathVertexIndex) {
        List<LabeledEdge> forwardEdges = new ArrayList<>();
        LabeledEdge pathEdge = g.edge(vertices.get(pathVertexIndex).id(), vertices.get(pathVertexIndex + 1).id());
        for (LabeledEdge e : delta.adjEdges(vertices.get(pathVertexIndex).id())) {
            if (emBit.get(e.to().id())) {
                continue;
            }
            if (g.eLabel(pathEdge) < g.eLabel(e)
                    || (g.eLabel(pathEdge) == g.eLabel(e) && g.vLabel(pathEdge.to()) <= g.vLabel(e.to()))) {
                forwardEdges.add(e);
            }
        }
        return forwardEdges;
    }


    private Pattern updateInterExpansion(Cluster c, int dfsFrom, int dfsTo, int fromLabel, int toLabel, int eLabel, Embedding em, Pattern parent) {
        Pattern child = parent.child(dfsFrom, dfsTo, fromLabel, toLabel, eLabel);
        if (child == null) {
            child = parent.addChild(dfsFrom, dfsTo, fromLabel, toLabel, eLabel);
        }
        child.addIntersectionEmbedding(c, em);
        return child;
    }

    private Pattern updateBorderExpansion(Cluster c, int dfsFrom, int dfsTo, int fromLabel, int toLabel, int eLabel, Embedding em, Pattern parent) {
        Pattern child = parent.child(dfsFrom, dfsTo, fromLabel, toLabel, eLabel);
        if (child == null) {
            child = parent.addChild(dfsFrom, dfsTo, fromLabel, toLabel, eLabel);
        }
        child.addBorderEmbedding(c, em);
        return child;
    }

    private Pattern updateOtherExpansion(LabeledGraph g, int dfsFrom, int dfsTo, int fromLabel, int toLabel, int eLabel, Embedding em, Pattern parent) {
        Pattern child = parent.child(dfsFrom, dfsTo, fromLabel, toLabel, eLabel);
        if (child == null) {
            child = parent.addChild(dfsFrom, dfsTo, fromLabel, toLabel, eLabel);
        }
        child.addEmbedding(g, em);
        return child;
    }

    private void join(Pattern p, List<Pattern> siblings, TreeMap<DFSEdge, Pattern> children) {
        long joinCandBegin = System.currentTimeMillis();
        TreeMap<Integer, TreeSet<DFSEdge>> backCand = new TreeMap<>();
        TreeMap<Integer, TreeSet<DFSEdge>> forCand = new TreeMap<>();

        DFSEdge e1 = p.edge();
        for (Pattern sib : siblings) {
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
        long joinCandEnd = System.currentTimeMillis();
        joinCandTime += (joinCandEnd - joinCandBegin);

        long commonBegin = System.currentTimeMillis();
        TreeSet<Cluster> commonCluster = new TreeSet<>();
        TreeSet<LabeledGraph> commonTrans = new TreeSet<>();
//        for (Pattern sib : siblings) {
//            if (partition) {
//                commonCluster.addAll(sib.clusters());
//            }
//            commonTrans.addAll(sib.unClusteredGraphs());
//        }
//        if (partition) {
//            commonCluster.retainAll(p.clusters());
//        }
        commonTrans.retainAll(p.unClusteredGraphs());
        long commonEnd = System.currentTimeMillis();
        joinCommonGraphTime += (commonEnd - commonBegin);


        if (partition) {
            for (Cluster c : commonCluster) {
                joinInterEmbeddings(c, p, backCand, forCand, children);
                joinDeltaEmbeddings(c, p, backCand, forCand, children);
            }
        }

        // TODO: 2020/4/16 support counting is incorrect
//        commonTrans.addAll(p.unClusteredGraphs());
        long actualJoinBegin = System.currentTimeMillis();
        for (LabeledGraph g : p.unClusteredGraphs()) {
            joinOtherEmbeddings(g, p, backCand, forCand, children);
        }
        long actualJoinEnd = System.currentTimeMillis();
        actualJoinTime += (actualJoinEnd - actualJoinBegin);
    }

    private void extend(Pattern p, TreeMap<DFSEdge, Pattern> children) {
        long extendCandBegin = System.currentTimeMillis();
        DFSEdge lastEdge = p.edge();
        DFSEdge firstEdge = p.code().get(0);
        TreeMap<DFSEdge, Pattern> candEdges = new TreeMap<>();
        for (Pattern ep : this.points.get(firstEdge.fromLabel()).children()) {
            if (!isFrequent(ep)) {
                continue;
            }
            DFSEdge e = ep.edge();
            if (e.toLabel() == lastEdge.toLabel() && e.edgeLabel() >= firstEdge.edgeLabel()) {
                candEdges.put(e, ep);
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
                    candEdges.put(e, ep);
                }
            }
        }
        PointPattern rmPoint = this.points.get(lastEdge.toLabel());
        if (rmPoint != null && isFrequent(rmPoint)) {
            for (Pattern ep : rmPoint.children()) {
                if (!isFrequent(ep)) {
                    continue;
                }
                candEdges.put(ep.edge(), ep);
            }
        }
        long extendCandEnd = System.currentTimeMillis();
        extendCandTime += (extendCandEnd - extendCandBegin);

        long commonBegin = System.currentTimeMillis();
        TreeSet<Cluster> commonCluster = new TreeSet<>();
        TreeSet<LabeledGraph> commonTrans = new TreeSet<>();
//        for (Pattern ep : candEdges.values()) {
//            if (partition) {
//                commonCluster.addAll(ep.clusters());
//            }
//            commonTrans.addAll(ep.unClusteredGraphs());
//        }
//        if (partition) {
//            commonCluster.retainAll(p.clusters());
//        }
//        commonTrans.retainAll(p.unClusteredGraphs());
        long commonEnd = System.currentTimeMillis();
        extendCommonGraphTime += (commonEnd - commonBegin);


        if (partition) {
            for (Cluster c : commonCluster) {
                extendInterEmbeddings(c, p, new TreeSet<>(candEdges.keySet()), children);
                extendDeltaEmbeddings(c, p, new TreeSet<>(candEdges.keySet()), children);
            }
        }
        // TODO: 2020/4/16 support counting is incorrect
//        commonTrans.addAll(p.unClusteredGraphs());
        long actualExtendBegin = System.currentTimeMillis();
        for (LabeledGraph g : p.unClusteredGraphs()) {
            extendOtherEmbeddings(g, p, new TreeSet<>(candEdges.keySet()), children);
        }
        long actualExtendEnd = System.currentTimeMillis();
        actualExtendTime += (actualExtendEnd - actualExtendBegin);
    }

    private void joinInterEmbeddings(Cluster c, Pattern p, TreeMap<Integer, TreeSet<DFSEdge>> backCand, TreeMap<Integer, TreeSet<DFSEdge>> forCand, TreeMap<DFSEdge, Pattern> children) {
        LabeledGraph inter = c.intersection();
        List<Embedding> embeddings = p.intersectionEmbeddings(c);
        int borderEmbeddingDelimiter = embeddings.size();
        embeddings.addAll(p.borderEmbeddings(c));

        for (int i = 0; i < embeddings.size(); i++) {
            Embedding em = embeddings.get(i);
            List<LabeledVertex> emVertics = em.vertices();
            //join backward edges
            for (Map.Entry<Integer, TreeSet<DFSEdge>> entry : backCand.entrySet()) {
                LabeledVertex from = emVertics.get(emVertics.size() - 1);
                LabeledVertex to = emVertics.get(entry.getKey());
                LabeledEdge back = inter.edge(from.id(), to.id());
                if (back == null) {
                    continue;
                }
                TreeSet<DFSEdge> cands = entry.getValue();
                DFSEdge dfsEdge = new DFSEdge(emVertics.size() - 1, entry.getKey(), inter.vLabel(from), inter.vLabel(to), inter.eLabel(back));
                if (cands.contains(dfsEdge)) {
                    Pattern child;
                    if (i < borderEmbeddingDelimiter) {
                        child = updateInterExpansion(c, dfsEdge.from(), dfsEdge.to(), dfsEdge.fromLabel(), dfsEdge.toLabel(), dfsEdge.edgeLabel(), em, p);
                    } else {
                        child = updateBorderExpansion(c, dfsEdge.from(), dfsEdge.to(), dfsEdge.fromLabel(), dfsEdge.toLabel(), dfsEdge.edgeLabel(), em, p);
                    }
                    children.put(child.edge(), child);
                }
            }

            //join forward edge
            BitSet emBits = new BitSet();
            for (LabeledVertex v : emVertics) {
                emBits.set(v.id());
            }
            for (Map.Entry<Integer, TreeSet<DFSEdge>> entry : forCand.entrySet()) {
                LabeledVertex from = emVertics.get(entry.getKey());
                for (LabeledEdge e : inter.adjEdges(from.id())) {
                    if (emBits.get(e.to().id())) {
                        continue;
                    }
                    DFSEdge dfsEdge = new DFSEdge(entry.getKey(), emVertics.size(), inter.vLabel(from), inter.vLabel(e.to()), inter.eLabel(e));
                    TreeSet<DFSEdge> cands = entry.getValue();
                    if (cands.contains(dfsEdge)) {
                        Pattern child;
                        if (i < borderEmbeddingDelimiter) {
                            child = updateInterExpansion(c, dfsEdge.from(), dfsEdge.to(), dfsEdge.fromLabel(), dfsEdge.toLabel(), dfsEdge.edgeLabel(), new Embedding(e.to(), em), p);
                        } else {
                            child = updateBorderExpansion(c, dfsEdge.from(), dfsEdge.to(), dfsEdge.fromLabel(), dfsEdge.toLabel(), dfsEdge.edgeLabel(), new Embedding(e.to(), em), p);
                        }
                        children.put(child.edge(), child);
                    }
                }
            }
        }
    }

    private void joinDeltaEmbeddings(Cluster c, Pattern p, TreeMap<Integer, TreeSet<DFSEdge>> backCand, TreeMap<Integer, TreeSet<DFSEdge>> forCand, TreeMap<DFSEdge, Pattern> children) {
        for (LabeledGraph g : c) {
            Cluster.DeltaGraph dg = c.deltaGraph(g);

            for (Embedding em : p.borderEmbeddings(c)) {
                if (!isBorderEmbedding(em, dg.border())) {
                    continue;
                }
                List<LabeledVertex> emVertics = em.vertices();
                //join backward edges
                for (Map.Entry<Integer, TreeSet<DFSEdge>> entry : backCand.entrySet()) {
                    LabeledVertex from = emVertics.get(emVertics.size() - 1);
                    LabeledVertex to = emVertics.get(entry.getKey());
                    LabeledEdge back = dg.edge(from.id(), to.id());
                    if (back == null) {
                        continue;
                    }
                    TreeSet<DFSEdge> cands = entry.getValue();
                    DFSEdge dfsEdge = new DFSEdge(emVertics.size() - 1, entry.getKey(), dg.vLabel(from), dg.vLabel(to), dg.eLabel(back));
                    if (cands.contains(dfsEdge)) {
                        Pattern child = updateOtherExpansion(g, dfsEdge.from(), dfsEdge.to(), dfsEdge.fromLabel(), dfsEdge.toLabel(), dfsEdge.edgeLabel(), em, p);
                        children.put(child.edge(), child);
                    }
                }

                //join forward edge
                BitSet emBits = new BitSet();
                for (LabeledVertex v : emVertics) {
                    emBits.set(v.id());
                }
                for (Map.Entry<Integer, TreeSet<DFSEdge>> entry : forCand.entrySet()) {
                    LabeledVertex from = emVertics.get(entry.getKey());
                    for (LabeledEdge e : dg.adjEdges(from.id())) {
                        if (emBits.get(e.to().id())) {
                            continue;
                        }
                        DFSEdge dfsEdge = new DFSEdge(entry.getKey(), emVertics.size(), dg.vLabel(from), dg.vLabel(e.to()), dg.eLabel(e));
                        TreeSet<DFSEdge> cands = entry.getValue();
                        if (cands.contains(dfsEdge)) {
                            Pattern child = updateOtherExpansion(g, dfsEdge.from(), dfsEdge.to(), dfsEdge.fromLabel(), dfsEdge.toLabel(), dfsEdge.edgeLabel(), new Embedding(e.to(), em), p);
                            children.put(child.edge(), child);
                        }
                    }
                }
            }
        }
    }

    private void joinOtherEmbeddings(LabeledGraph g, Pattern p, TreeMap<Integer, TreeSet<DFSEdge>> backCand, TreeMap<Integer, TreeSet<DFSEdge>> forCand, TreeMap<DFSEdge, Pattern> children) {
        List<Embedding> embeddings = p.embeddings(g);
        if (embeddings == null) {
            return;
        }
        for (Embedding em : embeddings) {
            long emVerticesBegin = System.currentTimeMillis();
            List<LabeledVertex> emVertics = em.vertices();
            long emVerticesEnd = System.currentTimeMillis();
            emVerticesTime += (emVerticesEnd - emVerticesBegin);

            //join backward edges
            for (Map.Entry<Integer, TreeSet<DFSEdge>> entry : backCand.entrySet()) {
                LabeledVertex from = emVertics.get(emVertics.size() - 1);
                LabeledVertex to = emVertics.get(entry.getKey());
                LabeledEdge back = g.edge(from.id(), to.id());
                if (back == null) {
                    continue;
                }

                long candCheckBegin = System.currentTimeMillis();
                TreeSet<DFSEdge> cands = entry.getValue();
                DFSEdge dfsEdge = new DFSEdge(emVertics.size() - 1, entry.getKey(), g.vLabel(from), g.vLabel(to), g.eLabel(back));
                if (cands.contains(dfsEdge)) {
                    long candCheckEnd = System.currentTimeMillis();
                    candCheckTime += (candCheckEnd - candCheckBegin);

                    long insertChildBegin = System.currentTimeMillis();
                    Pattern child = updateOtherExpansion(g, dfsEdge.from(), dfsEdge.to(), dfsEdge.fromLabel(), dfsEdge.toLabel(), dfsEdge.edgeLabel(), em, p);
                    children.put(child.edge(), child);
                    long insertChildEnd = System.currentTimeMillis();
                    insertChildTime += (insertChildEnd - insertChildBegin);
                } else {
                    long candCheckEnd = System.currentTimeMillis();
                    candCheckTime += (candCheckEnd - candCheckBegin);
                }
            }

            //join forward edge
            Vector<Boolean> emBits = new Vector<>(maxVid + 1);
            emBits.setSize(maxVid + 1);
            long emBitsBegin = System.currentTimeMillis();
            for (LabeledVertex v : emVertics) {
                emBits.set(v.id(), true);
            }
            long emBitsEnd = System.currentTimeMillis();
            emBitsTime += (emBitsEnd - emBitsBegin);
            for (Map.Entry<Integer, TreeSet<DFSEdge>> entry : forCand.entrySet()) {
                LabeledVertex from = emVertics.get(entry.getKey());
                for (LabeledEdge e : g.adjEdges(from.id())) {
                    long emBitsCheckBegin = System.currentTimeMillis();
                    if (emBits.get(e.to().id())) {
                        long emBitsCheckEnd = System.currentTimeMillis();
                        emBitsCheckTime += (emBitsCheckEnd - emBitsCheckBegin);
                        continue;
                    }
                    long emBitsCheckEnd = System.currentTimeMillis();
                    emBitsCheckTime += (emBitsCheckEnd - emBitsCheckBegin);

                    long candCheckBegin = System.currentTimeMillis();
                    DFSEdge dfsEdge = new DFSEdge(entry.getKey(), emVertics.size(), g.vLabel(from), g.vLabel(e.to()), g.eLabel(e));
                    TreeSet<DFSEdge> cands = entry.getValue();
                    if (cands.contains(dfsEdge)) {
                        long candCheckEnd = System.currentTimeMillis();
                        candCheckTime += (candCheckEnd - candCheckBegin);

                        long insertChildBegin = System.currentTimeMillis();
                        Pattern child = updateOtherExpansion(g, dfsEdge.from(), dfsEdge.to(), dfsEdge.fromLabel(), dfsEdge.toLabel(), dfsEdge.edgeLabel(), new Embedding(e.to(), em), p);
                        children.put(child.edge(), child);
                        long insertChildEnd = System.currentTimeMillis();
                        insertChildTime += (insertChildEnd - insertChildBegin);
                    } else {
                        long candCheckEnd = System.currentTimeMillis();
                        candCheckTime += (candCheckEnd - candCheckBegin);
                    }
                }
            }
        }
    }

    private void extendInterEmbeddings(Cluster c, Pattern p, TreeSet<DFSEdge> cand, TreeMap<DFSEdge, Pattern> children) {
        DFSCode code = p.code();
        List<Integer> rmPathIds = code.rightMostPath();
        List<Embedding> embeddings = p.intersectionEmbeddings(c);
        int borderEmbeddingDelimiter = embeddings.size();
        embeddings.addAll(p.borderEmbeddings(c));
        LabeledGraph inter = c.intersection();

        for (int i = 0; i < embeddings.size(); i++) {
            Embedding em = embeddings.get(i);
            List<LabeledVertex> emVertices = em.vertices();
            BitSet emBits = new BitSet();
            for (LabeledVertex v : emVertices) {
                emBits.set(v.id());
            }

            int fromId = rmPathIds.get(rmPathIds.size() - 1);
            LabeledVertex from = emVertices.get(rmPathIds.get(rmPathIds.size() - 1));

            //extend backward edges
            for (int j = 0; j < rmPathIds.size() - 2; j++) {
                int toId = rmPathIds.get(j);
                LabeledVertex to = emVertices.get(toId);
                LabeledEdge back = inter.edge(from.id(), to.id());
                if (back == null) {
                    continue;
                }
                DFSEdge dfsEdge1 = new DFSEdge(0, 1, inter.vLabel(from), inter.vLabel(to), inter.eLabel(back));
                DFSEdge dfsEdge2 = new DFSEdge(0, 1, inter.vLabel(to), inter.vLabel(from), inter.eLabel(back));
                if (cand.contains(dfsEdge1) || cand.contains(dfsEdge2)) {
                    Pattern child;
                    if (i < borderEmbeddingDelimiter) {
                        child = updateInterExpansion(c, fromId, toId, inter.vLabel(from), inter.vLabel(to), inter.eLabel(back), em, p);
                    } else {
                        child = updateBorderExpansion(c, fromId, toId, inter.vLabel(from), inter.vLabel(to), inter.eLabel(back), em, p);
                    }
                    children.put(child.edge(), child);
                }
            }

            //extend forward edges
            for (LabeledEdge e : inter.adjEdges(from.id())) {
                LabeledVertex to = e.to();
                if (emBits.get(to.id())) {
                    continue;
                }
                DFSEdge dfsEdge1 = new DFSEdge(0, 1, inter.vLabel(from), inter.vLabel(to), inter.eLabel(e));
                DFSEdge dfsEdge2 = new DFSEdge(0, 1, inter.vLabel(to), inter.vLabel(from), inter.eLabel(e));
                if (cand.contains(dfsEdge1) || cand.contains(dfsEdge2)) {
                    Pattern child;
                    if (i < borderEmbeddingDelimiter) {
                        child = updateInterExpansion(c, fromId, fromId + 1, inter.vLabel(from), inter.vLabel(to), inter.eLabel(e), new Embedding(to, em), p);
                    } else {
                        child = updateBorderExpansion(c, fromId, fromId + 1, inter.vLabel(from), inter.vLabel(to), inter.eLabel(e), new Embedding(to, em), p);
                    }
                    children.put(child.edge(), child);
                }
            }

        }
    }

    private void extendDeltaEmbeddings(Cluster c, Pattern p, TreeSet<DFSEdge> cand, TreeMap<DFSEdge, Pattern> children) {
        DFSCode code = p.code();
        List<Integer> rmPathIds = code.rightMostPath();

        for (LabeledGraph g : c) {
            Cluster.DeltaGraph dg = c.deltaGraph(g);

            for (Embedding em : p.borderEmbeddings(c)) {
                if (!isBorderEmbedding(em, dg.border())) {
                    continue;
                }

                List<LabeledVertex> emVertices = em.vertices();
                BitSet emBits = new BitSet();
                for (LabeledVertex v : emVertices) {
                    emBits.set(v.id());
                }

                int fromId = rmPathIds.get(rmPathIds.size() - 1);
                LabeledVertex from = emVertices.get(rmPathIds.get(rmPathIds.size() - 1));

                //extend backward edges
                for (int j = 0; j < rmPathIds.size() - 2; j++) {
                    int toId = rmPathIds.get(j);
                    LabeledVertex to = emVertices.get(toId);
                    LabeledEdge back = dg.edge(from.id(), to.id());
                    if (back == null) {
                        continue;
                    }
                    DFSEdge dfsEdge1 = new DFSEdge(0, 1, dg.vLabel(from), dg.vLabel(to), dg.eLabel(back));
                    DFSEdge dfsEdge2 = new DFSEdge(0, 1, dg.vLabel(to), dg.vLabel(from), dg.eLabel(back));
                    if (cand.contains(dfsEdge1) || cand.contains(dfsEdge2)) {
                        Pattern child = updateOtherExpansion(g, fromId, toId, dg.vLabel(from), dg.vLabel(to), dg.eLabel(back), em, p);
                        children.put(child.edge(), child);
                    }
                }

                //extend forward edges
                for (LabeledEdge e : dg.adjEdges(from.id())) {
                    LabeledVertex to = e.to();
                    if (emBits.get(to.id())) {
                        continue;
                    }
                    DFSEdge dfsEdge1 = new DFSEdge(0, 1, dg.vLabel(from), dg.vLabel(to), dg.eLabel(e));
                    DFSEdge dfsEdge2 = new DFSEdge(0, 1, dg.vLabel(to), dg.vLabel(from), dg.eLabel(e));
                    if (cand.contains(dfsEdge1) || cand.contains(dfsEdge2)) {
                        Pattern child = updateOtherExpansion(g, fromId, fromId + 1, dg.vLabel(from), dg.vLabel(to), dg.eLabel(e), new Embedding(to, em), p);
                        children.put(child.edge(), child);
                    }
                }
            }
        }
    }

    private void extendOtherEmbeddings(LabeledGraph g, Pattern p, TreeSet<DFSEdge> cand, TreeMap<DFSEdge, Pattern> children) {
        DFSCode code = p.code();
        List<Integer> rmPathIds = code.rightMostPath();

        for (Embedding em : p.embeddings(g)) {
            long emVerticesBegin = System.currentTimeMillis();
            List<LabeledVertex> emVertices = em.vertices();
            long emVerticesEnd = System.currentTimeMillis();
            emVerticesTime += (emVerticesEnd - emVerticesBegin);

            long emBitsBegin = System.currentTimeMillis();
            Vector<Boolean> emBits = new Vector<>(maxVid + 1);
            emBits.setSize(maxVid + 1);
            for (LabeledVertex v : emVertices) {
                emBits.set(v.id(), true);
            }
            long emBitsEnd = System.currentTimeMillis();
            emBitsTime += (emBitsEnd - emBitsBegin);

            int fromId = rmPathIds.get(rmPathIds.size() - 1);
            LabeledVertex from = emVertices.get(fromId);

            //extend backward edges
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

                long candCheckBegin = System.currentTimeMillis();
                DFSEdge dfsEdge1 = new DFSEdge(0, 1, g.vLabel(from), g.vLabel(to), g.eLabel(back));
                DFSEdge dfsEdge2 = new DFSEdge(0, 1, g.vLabel(to), g.vLabel(from), g.eLabel(back));
                if (cand.contains(dfsEdge1) || cand.contains(dfsEdge2)) {
                    long candCheckEnd = System.currentTimeMillis();
                    candCheckTime += (candCheckEnd - candCheckBegin);
                    Pattern child = updateOtherExpansion(g, fromId, toId, g.vLabel(from), g.vLabel(to), g.eLabel(back), em, p);;
                    children.put(child.edge(), child);
                } else {
                    long candCheckEnd = System.currentTimeMillis();
                    candCheckTime += (candCheckEnd - candCheckBegin);
                }
            }

            //extend rm forward edges
            for (LabeledEdge e : g.adjEdges(from.id())) {
                LabeledVertex to = e.to();
                long emBitsCheckBegin = System.currentTimeMillis();
                if (emBits.get(to.id())) {
                    long emBitsCheckEnd = System.currentTimeMillis();
                    emBitsCheckTime += (emBitsCheckEnd - emBitsCheckBegin);
                    continue;
                }
                long emBitsCheckEnd = System.currentTimeMillis();
                emBitsCheckTime += (emBitsCheckEnd - emBitsCheckBegin);

                long candCheckBegin = System.currentTimeMillis();
                DFSEdge dfsEdge1 = new DFSEdge(0, 1, g.vLabel(from), g.vLabel(to), g.eLabel(e));
                DFSEdge dfsEdge2 = new DFSEdge(0, 1, g.vLabel(to), g.vLabel(from), g.eLabel(e));
                if (cand.contains(dfsEdge1) || cand.contains(dfsEdge2)) {
                    long candCheckEnd = System.currentTimeMillis();
                    candCheckTime += (candCheckEnd - candCheckBegin);
                    Pattern child = updateOtherExpansion(g, fromId, emVertices.size(), g.vLabel(from), g.vLabel(to), g.eLabel(e), new Embedding(to, em), p);
                    children.put(child.edge(), child);
                } else {
                    long candCheckEnd = System.currentTimeMillis();
                    candCheckTime += (candCheckEnd - candCheckBegin);
                }
            }
        }

    }

    private boolean isFrequent(Pattern p) {
        return p.frequency() >= this.trans.size() * minSup;
    }

    private static DFSCode code(String code) {
        DFSCode dfsCode = new DFSCode();
        String[] edges = code.split("\\)");
        for (String edge : edges) {
            DFSEdge dfsEdge = edge(edge);
            dfsCode.add(dfsEdge);
        }
        return dfsCode;
    }

    private static DFSEdge edge(String edge) {
        String[] item = edge.substring(1).split(",");
        int from = Integer.parseInt(item[0]);
        int to = Integer.parseInt(item[1]);
//        int fromLabel = item[2].charAt(0);
//        int toLabel = item[4].charAt(0);
//        int eLabel = item[3].charAt(0);
        int fromLabel = Integer.parseInt(item[2]);
        int toLabel = Integer.parseInt(item[4]);
        int eLabel = Integer.parseInt(item[3]);

        return new DFSEdge(from, to, fromLabel, toLabel, eLabel);
    }
}
