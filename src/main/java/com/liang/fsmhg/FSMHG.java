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
    private int maxEdgeSize = Integer.MAX_VALUE;
    private boolean partition;
    private double similarity;
    private int clusterCounter;
    private int patternCount;

//    private LabeledGraph transDelimiter;
//    private Cluster clusterDelimiter;

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
                this.patternCount++;
                bw.write("t # " + (pointCount++) + " * " + pp.frequency());
                bw.newLine();
                bw.write("v 0 " + pp.label());
                bw.newLine();
            }
            System.out.println(pointCount + " point patterns");

            for (PointPattern pp : points.values()) {
                if (!isFrequent(pp)) {
                    continue;
                }
                for (Pattern p : pp.children()) {
                    if (!isFrequent(p) || !p.checkMin()) {
                        continue;
                    }
                    save(p, bw);
                }
            }
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
        LabeledGraph g = p.code().toGraph();
        for (int i = 0; i < g.vSize(); i++) {
            LabeledVertex v = g.vertex(i);
            bw.write("v " + i + " " + g.vLabel(v));
            bw.newLine();
        }
        for (int i = 0; i < g.vSize(); i++) {
            for (LabeledEdge e : g.adjEdges(i)) {
                if (e.from().id() >= e.to().id()) {
                    continue;
                }
                bw.write("e " + e.from().id() + " " + e.to().id() + " " + g.eLabel(e));
                bw.newLine();
            }
        }

        for (Pattern child : p.children()) {
            if (!isFrequent(p) || !p.checkMin()) {
                continue;
            }
            save(child, bw);
        }
    }


    // TODO: 2020/3/31 enumeration
    public void enumerate() {
//        transDelimiter = trans.firstEntry().getValue();
        this.trans = loadTrans();
        System.out.println("Total trans: " + trans.size());

        List<Cluster> clusters;
        Map<Integer, PointPattern> points;
        Map<DFSEdge, Pattern> edges;
        if (partition) {
            clusters = Cluster.partition(trans, similarity, 0);
//            clusterDelimiter = clusters.get(0);
//            clusterCounter = clusters.size();
            points = newPointsCluster(clusters);
            edges = newEdges(points, clusters);
        } else {
            points = newPoints(this.trans);
            edges = newEdges(points);
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
            subgraphMining(trans, p, rightSiblings(p, patterns, i + 1));
        }

        saveResult();
    }

    private List<Pattern> rightSiblings(Pattern p, List<Pattern> patterns, int fromIndex) {
        int toIndex = fromIndex;
        while (patterns.get(toIndex).parent() == p.parent()) {
            toIndex++;
        }
        return patterns.subList(fromIndex, toIndex);
    }

    // TODO: 2020/3/31 increment enumeration
//    public void incrementEnum(TreeMap<Long, LabeledGraph> newTrans) {
////        transDelimiter = newTrans.firstEntry().getValue();
//        this.trans.putAll(newTrans);
//        List<Cluster> clusters;
//        Map<Integer, PointPattern> points;
//        Map<DFSEdge, Pattern> edges;
//        if (partition) {
//            clusters = Cluster.partition(new ArrayList<>(newTrans.values()), similarity, clusterCounter);
////            clusterDelimiter = clusters.get(0);
//            clusterCounter += clusters.size();
//            points = newPoints(clusters);
//            edges = newEdges(points, clusters);
//        } else {
//            points = newPoints(this.trans);
//            edges = newEdges(points);
//        }
//
//        List<Pattern> patterns = new ArrayList<>(edges.values());
//        for (int i = 0; i < patterns.size(); i++) {
//            Pattern p = patterns.get(i);
//            if (!isFrequent(p) || p.code().edgeSize() < maxEdgeSize) {
//                continue;
//            }
//            subgraphMining(newTrans, p, rightSiblings(p, patterns, i + 1));
//        }
//    }


    public Map<Integer, PointPattern> newPointsCluster(List<Cluster> clusters) {
        Map<Integer, PointPattern> newPoints = new TreeMap<>();
        for (Cluster c : clusters) {
            intersectionPoints(c, newPoints);
            otherPoints(c, newPoints);
        }
        return newPoints;
    }

    private void intersectionPoints(Cluster c, Map<Integer, PointPattern> newPoints) {
        Cluster.Intersection inter = c.intersection();
        Map<Integer, LabeledVertex> border = c.border();
        for (LabeledVertex v : inter.vertices()) {
            PointPattern pattern = this.points.get(inter.vLabel(v));
            if (pattern == null) {
                pattern = new PointPattern(inter.vLabel(v));
                this.points.put(pattern.label(), pattern);
            }
            newPoints.put(pattern.label(), pattern);
            Embedding em = new Embedding(v, null);
            if (isBorderEmbedding(em, border)) {
                pattern.addBorderEmbedding(c, em);
            } else {
                pattern.addIntersectionEmbedding(c, em);
            }
        }
    }

    private void otherPoints(Cluster c, Map<Integer, PointPattern> newPoints) {
        for (LabeledGraph g : c.snapshots()) {
            Cluster.DeltaGraph dg = c.deltaGraph(g);
            Map<Integer, LabeledVertex> border = dg.border();
            for (LabeledVertex v : g.vertices()) {
                if (border.containsKey(v.id())) {
                    continue;
                }
                PointPattern pattern = this.points.get(g.vLabel(v));
                if (pattern == null) {
                    pattern = new PointPattern(g.vLabel(v));
                    this.points.put(pattern.label(), pattern);
                }
                newPoints.put(pattern.label(), pattern);
                Embedding em = new Embedding(v, null);
                pattern.addEmbedding(g, em);
            }
        }
    }

    public Map<DFSEdge, Pattern> newEdges(Map<Integer, PointPattern> newPoints, List<Cluster> clusters) {
        Map<DFSEdge, Pattern> newEdges = new TreeMap<>();
        for (Cluster c : clusters) {
            intersectionEdges(c, newPoints, newEdges);
            otherEdges(c, newPoints, newEdges);
        }
        return newEdges;
    }

    private void intersectionEdges(Cluster c, Map<Integer, PointPattern> newPoints, Map<DFSEdge, Pattern> newEdges) {
        Cluster.Intersection inter = c.intersection();

        for (PointPattern p : newPoints.values()) {
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
                    newEdges.put(child.edge(), child);
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
                    newEdges.put(child.edge(), child);
                }
            }
        }
    }

    private void otherEdges (Cluster c, Map<Integer, PointPattern> newPoints, Map<DFSEdge, Pattern> newEdges) {
        for (LabeledGraph g : c) {
            Cluster.DeltaGraph delta = c.deltaGraph(g);
            for (PointPattern p : newPoints.values()) {
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
                        newEdges.put(child.edge(), child);
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
                        newEdges.put(child.edge(), child);
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

    public Map<Integer, PointPattern> newPoints(List<LabeledGraph> trans) {
        TreeMap<Integer, PointPattern> newPoints = new TreeMap<>();
        for (LabeledGraph g : trans) {
            for (LabeledVertex v : g.vertices()) {
                PointPattern pp = this.points.get(g.vLabel(v));
                if (pp == null) {
                    pp = new PointPattern(g.vLabel(v));
                    this.points.put(pp.label(), pp);
                }
                pp.addEmbedding(g, new Embedding(v, null));
                newPoints.put(pp.label(), pp);
            }
        }
        return newPoints;
    }

    public Map<DFSEdge, Pattern> newEdges(Map<Integer, PointPattern> newPoints) {
        Map<DFSEdge, Pattern> eMap = new TreeMap<>();
        for (PointPattern pp : newPoints.values()) {
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


    public void subgraphMining(List<LabeledGraph> trans, Pattern parent, List<Pattern> siblings) {
        if (!parent.checkMin()) {
            return;
        }

        List<Pattern> children = enumerateChildren(parent, siblings);
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
            subgraphMining(trans, p, children.subList(i + 1, children.size()));
        }
    }

    private List<Pattern> enumerateChildren(Pattern p, List<Pattern> siblings) {
        TreeMap<DFSEdge, Pattern> newChildren = new TreeMap<>();

        join(p, siblings, newChildren);
        if (p.edge().isForward()) {
            extend(p, newChildren);
        }

        return new ArrayList<>(newChildren.values());

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
        // TODO: 2020/3/31 join
        TreeMap<Integer, TreeSet<DFSEdge>> backCand = new TreeMap<>();
        TreeMap<Integer, TreeSet<DFSEdge>> forCand = new TreeMap<>();

        DFSEdge e1 = p.edge();
        for (Pattern sib : siblings) {
            if (!isFrequent(sib)) {
                continue;
            }
            DFSEdge e2 = sib.edge();
            if (e1.compareTo(e2) >= 0) {
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

        TreeSet<Cluster> commonCluster = new TreeSet<>();
        TreeSet<LabeledGraph> commonTrans = new TreeSet<>();
        for (Pattern sib : siblings) {
            if (partition) {
                commonCluster.addAll(sib.clusters());
            }
            commonTrans.addAll(sib.unClusteredGraphs());
        }
        if (partition) {
            commonCluster.retainAll(p.clusters());
        }
        commonTrans.retainAll(p.unClusteredGraphs());

        if (partition) {
            for (Cluster c : commonCluster) {
                joinInterEmbeddings(c, p, backCand, forCand, children);
                joinDeltaEmbeddings(c, p, backCand, forCand, children);
            }
        }
        for (LabeledGraph g : commonTrans) {
            joinOtherEmbeddings(g, p, backCand, forCand, children);
        }
    }

    private TreeMap<DFSEdge, Pattern> extend(Pattern p, TreeMap<DFSEdge, Pattern> children) {
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

        TreeSet<Cluster> commonCluster = new TreeSet<>();
        TreeSet<LabeledGraph> commonTrans = new TreeSet<>();
        for (Pattern ep : candEdges.values()) {
            if (partition) {
                commonCluster.addAll(ep.clusters());
            }
            commonTrans.addAll(ep.unClusteredGraphs());
        }
        if (partition) {
            commonCluster.retainAll(p.clusters());
        }
        commonTrans.retainAll(p.unClusteredGraphs());

        if (partition) {
            for (Cluster c : commonCluster) {
                extendInterEmbeddings(c, p, new TreeSet<>(candEdges.keySet()), children);
                extendDeltaEmbeddings(c, p, new TreeSet<>(candEdges.keySet()), children);
            }
        }
        for (LabeledGraph g : commonTrans) {
            extendOtherEmbeddings(g, p, new TreeSet<>(candEdges.keySet()), children);
        }

        return children;
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
            List<LabeledVertex> emVertics = em.vertices();
            //join backward edges
            for (Map.Entry<Integer, TreeSet<DFSEdge>> entry : backCand.entrySet()) {
                LabeledVertex from = emVertics.get(emVertics.size() - 1);
                LabeledVertex to = emVertics.get(entry.getKey());
                LabeledEdge back = g.edge(from.id(), to.id());
                if (back == null) {
                    continue;
                }
                TreeSet<DFSEdge> cands = entry.getValue();
                DFSEdge dfsEdge = new DFSEdge(emVertics.size() - 1, entry.getKey(), g.vLabel(from), g.vLabel(to), g.eLabel(back));
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
                for (LabeledEdge e : g.adjEdges(from.id())) {
                    if (emBits.get(e.to().id())) {
                        continue;
                    }
                    DFSEdge dfsEdge = new DFSEdge(entry.getKey(), emVertics.size(), g.vLabel(from), g.vLabel(e.to()), g.eLabel(e));
                    TreeSet<DFSEdge> cands = entry.getValue();
                    if (cands.contains(dfsEdge)) {
                        Pattern child = updateOtherExpansion(g, dfsEdge.from(), dfsEdge.to(), dfsEdge.fromLabel(), dfsEdge.toLabel(), dfsEdge.edgeLabel(), new Embedding(e.to(), em), p);
                        children.put(child.edge(), child);
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
            for (int j = 0; j < rmPathIds.size() - 1; j++) {
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
                for (int j = 0; j < rmPathIds.size() - 1; j++) {
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
            List<LabeledVertex> emVertices = em.vertices();
            BitSet emBits = new BitSet();
            for (LabeledVertex v : emVertices) {
                emBits.set(v.id());
            }

            int fromId = rmPathIds.get(rmPathIds.size() - 1);
            LabeledVertex from = emVertices.get(rmPathIds.get(rmPathIds.size() - 1));

            //extend backward edges
            for (int j = 0; j < rmPathIds.size() - 1; j++) {
                int toId = rmPathIds.get(j);
                LabeledVertex to = emVertices.get(toId);
                LabeledEdge back = g.edge(from.id(), to.id());
                if (back == null) {
                    continue;
                }
                DFSEdge dfsEdge1 = new DFSEdge(0, 1, g.vLabel(from), g.vLabel(to), g.eLabel(back));
                DFSEdge dfsEdge2 = new DFSEdge(0, 1, g.vLabel(to), g.vLabel(from), g.eLabel(back));
                if (cand.contains(dfsEdge1) || cand.contains(dfsEdge2)) {
                    Pattern child = updateOtherExpansion(g, fromId, toId, g.vLabel(from), g.vLabel(to), g.eLabel(back), em, p);;
                    children.put(child.edge(), child);
                }
            }

            //extend forward edges
            for (LabeledEdge e : g.adjEdges(from.id())) {
                LabeledVertex to = e.to();
                if (emBits.get(to.id())) {
                    continue;
                }
                DFSEdge dfsEdge1 = new DFSEdge(0, 1, g.vLabel(from), g.vLabel(to), g.eLabel(e));
                DFSEdge dfsEdge2 = new DFSEdge(0, 1, g.vLabel(to), g.vLabel(from), g.eLabel(e));
                if (cand.contains(dfsEdge1) || cand.contains(dfsEdge2)) {
                    Pattern child = updateOtherExpansion(g, fromId, fromId + 1, g.vLabel(from), g.vLabel(to), g.eLabel(e), new Embedding(to, em), p);
                    children.put(child.edge(), child);
                }
            }
        }

    }

    private boolean isFrequent(Pattern p) {
        return p.frequency() >= this.trans.size() * minSup;
    }

}
