package com.liang.fsmhg;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import com.liang.fsmhg.code.DFSCode;
import com.liang.fsmhg.code.DFSEdge;
import com.liang.fsmhg.graph.AdjEdges;
import com.liang.fsmhg.graph.LabeledEdge;
import com.liang.fsmhg.graph.LabeledGraph;
import com.liang.fsmhg.graph.LabeledVertex;

public class FSMHGWIN {
    private static final String PATTERN_TREE_FILE = "/tmp/tree";

    private TreeMap<Integer, PointPattern> points;
    private List<LabeledGraph> trans;
    private File out;
    private double minSup;
    private double absSup;
    private int maxEdgeSize = Integer.MAX_VALUE;
    private boolean partition;
    private double similarity;
    private ArrayList<Cluster> clusters;
    private int clusterCounter;
    private int maxVid = 0;
    private LabeledGraph transDelimiter;
    private Cluster clusterDelimiter;
    private int patternCount = 0;
    private int pointCount = 0;
    private PatternWriter pw;
    private PatternTreeWriter ptw;

    private int winCount = -1;

    public FSMHGWIN(double minSupport, int maxEdgeSize, boolean partition, double similarity) {
        this.minSup = minSupport;
        this.maxEdgeSize = maxEdgeSize;
        this.partition = partition;
        this.similarity = similarity;
        this.points = new TreeMap<>();
        this.clusters = new ArrayList<>();
        this.trans = new ArrayList<>();
        this.ptw = new PatternTreeWriter(new File(PATTERN_TREE_FILE));
    }

    public void enumerate(List<LabeledGraph> newTrans) {
        this.winCount++;

        long startTime = System.currentTimeMillis();
        this.patternCount = 0;
        this.pointCount = 0;

        if (this.winCount == 1) {
            loadPatterns();
        }

        List<LabeledGraph> removed;
        List<LabeledGraph> added;
        int index = this.trans.indexOf(newTrans.get(0));
        if (index == -1) {
            removed = this.trans;
            this.trans = newTrans;
            added = this.trans;
        } else {
            removed = this.trans.subList(0, index);
            this.trans = newTrans;
            added = this.trans.subList(this.trans.size() - index, this.trans.size());
        }

        System.out.println("Total trans : " + this.trans.size());
        this.absSup = Math.ceil(this.trans.size() * this.minSup);

        long shinkStart = System.currentTimeMillis();
        shrink(removed);
        long shrinkEnd = System.currentTimeMillis();
        long growStart = System.currentTimeMillis();
        grow(added);
        long growEnd = System.currentTimeMillis();

        if (this.winCount > 0) {
            saveResult();
        }
        this.pw.close();

        long endTime = System.currentTimeMillis();
        System.out.println(pointCount + " point patterns");
        System.out.println((this.patternCount - pointCount) + " connected patterns.");
        System.out.println("Duration = " + (endTime - startTime));
        System.out.println("Shrink time = " + (shrinkEnd - shinkStart));
        System.out.println("Grow time = " + (growEnd - growStart));
    }

    private void shrink(List<LabeledGraph> graphs) {
        if (graphs == null || graphs.isEmpty()) {
            return;
        }
        List<Cluster> emptyClusters = new ArrayList<>();
        if (this.partition) {
            List<LabeledGraph> batch = new ArrayList<>();
            Cluster c = graphs.get(0).getCluster();
            for (LabeledGraph g : graphs) {
                Cluster temp = g.getCluster();
                if (c == temp) {
                    batch.add(g);
                } else {
                    c.remove(batch);
                    if (c.size() == 0) {
                        emptyClusters.add(c);
                    }
                    c = temp;
                    batch = new ArrayList<>();
                    batch.add(g);
                }
            }
            c.remove(batch);
            if (c.size() == 0) {
                emptyClusters.add(c);
            }
            this.clusters.removeAll(emptyClusters);
        }

        Iterator<Entry<Integer, PointPattern>> ppIt = this.points.entrySet().iterator();
        while (ppIt.hasNext()) {
            PointPattern pp = ppIt.next().getValue();
            pp.remove(graphs, emptyClusters);
            if (pp.support() <= 0) {
                ppIt.remove();
            }
        }
    }

    private void grow(List<LabeledGraph> addedTrans) {
        // this.transDelimiter = addedTrans.get(addedTrans.size() - 1);
        List<Cluster> addedClusters;
        Map<Integer, PointPattern> addedPoints;
        Map<DFSEdge, Pattern> addedEdges;
        if (partition) {
            this.transDelimiter = addedTrans.get(addedTrans.size() - 1);
            addedClusters = Cluster.partition(addedTrans, similarity, clusterCounter);
            this.clusterDelimiter = addedClusters.get(addedClusters.size() - 1);
            this.clusterCounter += addedClusters.size();
            this.clusters.addAll(addedClusters);
            addedPoints = pointsByPartition(addedClusters);
            addedEdges = edgesByPartition(addedPoints, addedClusters);

            // TODO process degradation
            // Cluster lastClusterDelimiter = this.clusterDelimiter;
            // List<LabeledGraph> appended = new ArrayList<>();
            // if (lastClusterDelimiter != null) {
            // for (LabeledGraph g : addedTrans) {
            // if (!lastClusterDelimiter.append(g)) {
            // break;
            // }
            // appended.add(g);
            // }
            // }
            // addedClusters = Cluster.partition(addedTrans.subList(appended.size(),
            // addedTrans.size()), similarity, clusterCounter);
            // if (!addedClusters.isEmpty()) {
            // this.clusterDelimiter = addedClusters.get(addedClusters.size() - 1);
            // this.clusterCounter += addedClusters.size();
            // this.clusters.addAll(addedClusters);
            // }
            // addedPoints = pointsByPartition1(lastClusterDelimiter, appended,
            // this.transDelimiter, addedClusters);
            // this.transDelimiter = addedTrans.get(addedTrans.size() - 1);
            // addedEdges = edgesByPartition1(addedPoints);
        } else {
            this.transDelimiter = addedTrans.get(addedTrans.size() - 1);
            addedPoints = pointsNoPartition(addedTrans);
            addedEdges = edgesNoPartition(addedPoints, addedTrans);
        }

        if (this.winCount == 0) {
            for (PointPattern pp : addedPoints.values()) {
                if (!isFrequent(pp)) {
                    continue;
                }
                this.pointCount++;
                this.pw.save(pp, this.patternCount++);
                for (Pattern p : pp.children()) {
                    this.ptw.saveNode(p);
                    pp.removeChild(p);
                    if (!isFrequent(p)) {
                        continue;
                    }

                    subgraphMining(trans, p);
                }
            }
            this.ptw.close();
            this.ptw = null;
            return;
        }

        for (Pattern p : addedEdges.values()) {
            if (!isFrequent(p)) {
                continue;
            }
            subgraphMining(addedTrans, p);
        }
    }

    public TreeMap<Integer, PointPattern> pointsByPartition(List<Cluster> clusters) {
        TreeMap<Integer, PointPattern> addedPoints = new TreeMap<>();
        for (Cluster c : clusters) {
            LabeledGraph inter = c.intersection();
            for (LabeledVertex v : inter.vertices()) {
                PointPattern pattern = this.points.get(inter.vLabel(v));
                if (pattern == null) {
                    pattern = new PointPattern(inter.vLabel(v));
                    this.points.put(pattern.label(), pattern);
                }
                Embedding em = new Embedding(v, null);
                pattern.addIntersectionEmbedding(c, em);
                addedPoints.put(pattern.label(), pattern);
                if (v.id() > maxVid) {
                    maxVid = v.id();
                }
            }

            for (LabeledGraph g : c) {
                LabeledGraph dg = c.deltaGraph1(g);
                for (LabeledVertex v : dg.vertices()) {
                    if (inter.vertex(v.id()) != null) {
                        continue;
                    }
                    PointPattern pattern = this.points.get(g.vLabel(v));
                    if (pattern == null) {
                        pattern = new PointPattern(g.vLabel(v));
                        this.points.put(pattern.label(), pattern);
                    }
                    Embedding em = new Embedding(v, null);
                    pattern.addEmbedding(g, c, em);
                    addedPoints.put(pattern.label(), pattern);
                    if (v.id() > maxVid) {
                        maxVid = v.id();
                    }
                }
            }
        }
        return addedPoints;
    }

    public TreeMap<Integer, PointPattern> pointsByPartition1(Cluster lastClusterDelimiter, List<LabeledGraph> appended,
            LabeledGraph lastGraphDelimiter, List<Cluster> clusters) {
        TreeMap<Integer, PointPattern> addedPoints = new TreeMap<>();

        for (PointPattern pp : this.points.values()) {
            if (!pp.containsCluster(lastClusterDelimiter)) {
                continue;
            }
            if (lastGraphDelimiter.graphId() < lastClusterDelimiter.last().graphId()) {
                List<Embedding> interEmbeddings = pp.intersectionEmbeddings(lastClusterDelimiter);
                List<Embedding> nonInterEmbeddings = new ArrayList<>();
                for (Embedding em : interEmbeddings) {
                    if (!containEmbedding(lastClusterDelimiter.intersection(), em, pp)) {
                        nonInterEmbeddings.add(em);
                    }
                }
                interEmbeddings.removeAll(nonInterEmbeddings);
                if (!interEmbeddings.isEmpty()) {
                    addedPoints.put(pp.label(), pp);
                    pp.increaseSupport((int) (lastClusterDelimiter.last().graphId() - lastGraphDelimiter.graphId()));
                } else {
                    pp.removeCluster(lastClusterDelimiter, lastGraphDelimiter);
                }
                for (Embedding em : nonInterEmbeddings) {
                    for (LabeledGraph g : lastClusterDelimiter) {
                        if (g.graphId() > lastGraphDelimiter.graphId()) {
                            break;
                        }
                        pp.addEmbedding(g, lastClusterDelimiter, em);
                    }
                }
                if (pp.hasChild()) {
                    for (LabeledGraph g : lastClusterDelimiter) {
                        if (g.graphId() > lastGraphDelimiter.graphId()) {
                            break;
                        }
                        pp.embeddings(g).clear();
                    }
                }
            }
        }

        for (LabeledGraph g : appended) {
            LabeledGraph dg = lastClusterDelimiter.deltaGraph1(g);
            for (LabeledVertex v : dg.vertices()) {
                if (lastClusterDelimiter.intersection().vertex(v.id()) != null) {
                    continue;
                }
                PointPattern pattern = this.points.get(g.vLabel(v));
                if (pattern == null) {
                    pattern = new PointPattern(g.vLabel(v));
                    this.points.put(pattern.label(), pattern);
                }
                Embedding em = new Embedding(v, null);
                pattern.addEmbedding(g, lastClusterDelimiter, em);
                addedPoints.put(pattern.label(), pattern);
                if (v.id() > maxVid) {
                    maxVid = v.id();
                }
            }
        }

        for (Cluster c : clusters) {
            LabeledGraph inter = c.intersection();
            for (LabeledVertex v : inter.vertices()) {
                PointPattern pattern = this.points.get(inter.vLabel(v));
                if (pattern == null) {
                    pattern = new PointPattern(inter.vLabel(v));
                    this.points.put(pattern.label(), pattern);
                }
                Embedding em = new Embedding(v, null);
                pattern.addIntersectionEmbedding(c, em);
                addedPoints.put(pattern.label(), pattern);
                if (v.id() > maxVid) {
                    maxVid = v.id();
                }
            }

            for (LabeledGraph g : c) {
                LabeledGraph dg = c.deltaGraph1(g);
                for (LabeledVertex v : dg.vertices()) {
                    if (inter.vertex(v.id()) != null) {
                        continue;
                    }
                    PointPattern pattern = this.points.get(g.vLabel(v));
                    if (pattern == null) {
                        pattern = new PointPattern(g.vLabel(v));
                        this.points.put(pattern.label(), pattern);
                    }
                    Embedding em = new Embedding(v, null);
                    pattern.addEmbedding(g, c, em);
                    addedPoints.put(pattern.label(), pattern);
                    if (v.id() > maxVid) {
                        maxVid = v.id();
                    }
                }
            }
        }
        return addedPoints;
    }

    public Map<DFSEdge, Pattern> edgesByPartition(Map<Integer, PointPattern> points, List<Cluster> clusters) {
        Map<DFSEdge, Pattern> addedEdges = new TreeMap<>();

        for (Cluster c : clusters) {
            LabeledGraph inter = c.intersection();
            for (PointPattern pp : points.values()) {
                List<Embedding> embeddings = pp.intersectionEmbeddings(c);
                if (embeddings != null) {
                    for (Embedding em : embeddings) {
                        for (LabeledEdge e : inter.adjEdges(em.vertex().id())) {
                            if (inter.vLabel(e.from()) > inter.vLabel(e.to())) {
                                continue;
                            }
                            Pattern child = pp.child(0, 1, inter.vLabel(e.from()), inter.vLabel(e.to()),
                                    inter.eLabel(e));
                            child.addIntersectionEmbedding(c, new Embedding(e.to(), em));
                            addedEdges.put(child.edge(), child);
                        }

                        Map<LabeledGraph, AdjEdges> borderAdj = c.borderAdj(em.vertex().id());
                        for (Entry<LabeledGraph, AdjEdges> adjEntry : borderAdj.entrySet()) {
                            LabeledGraph g = adjEntry.getKey();
                            AdjEdges adj = adjEntry.getValue();
                            for (LabeledEdge e : adj) {
                                if (g.vLabel(e.from()) > g.vLabel(e.to())) {
                                    continue;
                                }
                                Pattern child = pp.child(0, 1, g.vLabel(e.from()), g.vLabel(e.to()), g.eLabel(e));
                                child.addEmbedding(g, c, new Embedding(e.to(), em));
                                addedEdges.put(child.edge(), child);
                            }
                        }
                    }
                    embeddings.clear();
                }

                for (LabeledGraph g : c) {
                    embeddings = pp.embeddings(g);
                    if (embeddings == null) {
                        continue;
                    }
                    for (Embedding em : embeddings) {
                        for (LabeledEdge e : g.adjEdges(em.vertex().id())) {
                            if (g.vLabel(e.from()) > g.vLabel(e.to())) {
                                continue;
                            }
                            Pattern child = pp.child(0, 1, g.vLabel(e.from()), g.vLabel(e.to()), g.eLabel(e));
                            child.addEmbedding(g, c, new Embedding(e.to(), em));
                            addedEdges.put(child.edge(), child);
                        }
                    }
                    embeddings.clear();
                }

                pp.setClusterDelimiter(this.clusterDelimiter);
                pp.setGraphDelimiter(this.transDelimiter);
            }
        }

        return addedEdges;
    }

    public Map<DFSEdge, Pattern> edgesByPartition1(Map<Integer, PointPattern> points) {
        Map<DFSEdge, Pattern> addedEdges = new TreeMap<>();

        for (PointPattern pp : points.values()) {
            Cluster clusterDelimiter = pp.clusterDelimiter();
            LabeledGraph graphDelimiter = pp.graphDelimiter();
            if (clusterDelimiter != null) {
                for (Pattern child : pp.children()) {
                    if (!child.containsCluster(clusterDelimiter)) {
                        continue;
                    }
                    if (graphDelimiter.graphId() < clusterDelimiter.last().graphId()) {
                        List<Embedding> interEmbeddings = child.intersectionEmbeddings(clusterDelimiter);
                        List<Embedding> nonInterEmbeddings = new ArrayList<>();
                        for (Embedding em : interEmbeddings) {
                            if (!containEmbedding(clusterDelimiter.intersection(), em, child)) {
                                nonInterEmbeddings.add(em);
                            }
                        }
                        interEmbeddings.removeAll(nonInterEmbeddings);
                        if (!interEmbeddings.isEmpty()) {
                            addedEdges.put(child.edge(), child);
                            child.increaseSupport((int) (clusterDelimiter.last().graphId() - graphDelimiter.graphId()));
                        } else {
                            child.removeCluster(clusterDelimiter, graphDelimiter);
                        }
                        for (Embedding em : nonInterEmbeddings) {
                            for (LabeledGraph g : clusterDelimiter) {
                                if (g.graphId() > pp.graphDelimiter().graphId()) {
                                    break;
                                }
                                child.addEmbedding(g, clusterDelimiter, em);
                            }
                        }
                        if (child.hasChild()) {
                            for (LabeledGraph g : clusterDelimiter) {
                                if (g.graphId() > graphDelimiter.graphId()) {
                                    break;
                                }
                                child.embeddings(g).clear();
                                ;
                            }
                        }
                    }
                }
                List<Embedding> embeddings = pp.intersectionEmbeddings(clusterDelimiter);
                for (Embedding em : embeddings) {
                    Map<LabeledGraph, AdjEdges> borderAdj = clusterDelimiter.borderAdj(em.vertex().id());
                    for (Entry<LabeledGraph, AdjEdges> adjEntry : borderAdj.entrySet()) {
                        LabeledGraph g = adjEntry.getKey();
                        if (graphDelimiter != null && g.graphId() <= graphDelimiter.graphId()) {
                            continue;
                        }
                        AdjEdges adj = adjEntry.getValue();
                        for (LabeledEdge e : adj) {
                            if (g.vLabel(e.from()) > g.vLabel(e.to())) {
                                continue;
                            }
                            Pattern child = pp.child(0, 1, g.vLabel(e.from()), g.vLabel(e.to()), g.eLabel(e));
                            child.addEmbedding(g, clusterDelimiter, new Embedding(e.to(), em));
                            addedEdges.put(child.edge(), child);
                        }
                    }
                }
                if (clusterDelimiter != this.clusterDelimiter) {
                    embeddings.clear();
                }
            }

            List<Cluster> clusters = pp.clustersAfterDelimiter();
            for (Cluster c : clusters) {
                LabeledGraph inter = c.intersection();
                List<Embedding> embeddings = pp.intersectionEmbeddings(c);
                for (Embedding em : embeddings) {
                    for (LabeledEdge e : inter.adjEdges(em.vertex().id())) {
                        if (inter.vLabel(e.from()) > inter.vLabel(e.to())) {
                            continue;
                        }
                        Pattern child = pp.child(0, 1, inter.vLabel(e.from()), inter.vLabel(e.to()), inter.eLabel(e));
                        child.addIntersectionEmbedding(c, new Embedding(e.to(), em));
                        addedEdges.put(child.edge(), child);
                    }

                    Map<LabeledGraph, AdjEdges> borderAdj = c.borderAdj(em.vertex().id());
                    for (Entry<LabeledGraph, AdjEdges> adjEntry : borderAdj.entrySet()) {
                        LabeledGraph g = adjEntry.getKey();
                        AdjEdges adj = adjEntry.getValue();
                        for (LabeledEdge e : adj) {
                            if (g.vLabel(e.from()) > g.vLabel(e.to())) {
                                continue;
                            }
                            Pattern child = pp.child(0, 1, g.vLabel(e.from()), g.vLabel(e.to()), g.eLabel(e));
                            child.addEmbedding(g, c, new Embedding(e.to(), em));
                            addedEdges.put(child.edge(), child);
                        }
                    }
                }
                if (c != this.clusterDelimiter) {
                    embeddings.clear();
                }
            }

            for (LabeledGraph g : pp.graphsAfterDelimiter()) {
                List<Embedding> embeddings = pp.embeddings(g);
                if (embeddings == null) {
                    continue;
                }
                for (Embedding em : embeddings) {
                    for (LabeledEdge e : g.adjEdges(em.vertex().id())) {
                        if (g.vLabel(e.from()) > g.vLabel(e.to())) {
                            continue;
                        }
                        Pattern child = pp.child(0, 1, g.vLabel(e.from()), g.vLabel(e.to()), g.eLabel(e));
                        child.addEmbedding(g, g.getCluster(), new Embedding(e.to(), em));
                        addedEdges.put(child.edge(), child);
                    }
                }
                embeddings.clear();
            }

            pp.setClusterDelimiter(this.clusterDelimiter);
            pp.setGraphDelimiter(this.transDelimiter);
        }

        return addedEdges;
    }

    public TreeMap<Integer, PointPattern> pointsNoPartition(List<LabeledGraph> trans) {
        TreeMap<Integer, PointPattern> addedPoints = new TreeMap<>();
        for (LabeledGraph g : trans) {
            for (LabeledVertex v : g.vertices()) {
                PointPattern pp = this.points.get(g.vLabel(v));
                if (pp == null) {
                    pp = new PointPattern(g.vLabel(v));
                    this.points.put(pp.label(), pp);
                }
                pp.addEmbedding(g, new Embedding(v, null));
                addedPoints.put(pp.label(), pp);
                if (v.id() > maxVid) {
                    maxVid = v.id();
                }
            }
        }
        return addedPoints;
    }

    public Map<DFSEdge, Pattern> edgesNoPartition(Map<Integer, PointPattern> points, List<LabeledGraph> trans) {
        Map<DFSEdge, Pattern> addedEdges = new TreeMap<>();

        for (PointPattern pp : points.values()) {
            for (LabeledGraph g : trans) {
                List<Embedding> embeddings = pp.embeddings(g);
                for (Embedding em : embeddings) {
                    LabeledVertex from = em.vertex();
                    for (LabeledEdge e : g.adjEdges(from.id())) {
                        LabeledVertex to = e.to();
                        if (g.vLabel(from) > g.vLabel(to)) {
                            continue;
                        }
                        Pattern child = pp.child(0, 1, g.vLabel(from), g.vLabel(to), g.eLabel(e));
                        child.addEmbedding(g, new Embedding(to, em));
                        addedEdges.put(child.edge(), child);
                    }
                }
                embeddings.clear();
            }
            pp.setGraphDelimiter(this.transDelimiter);
        }

        return addedEdges;
    }

    public void subgraphMining(List<LabeledGraph> trans, Pattern parent) {
        if (!parent.checkMin()) {
            // parent.clearEmbeddings(this.clusterDelimiter);
            parent.clearEmbeddings();
            return;
        }
        if (this.winCount == 0) {
            this.pw.save(parent, this.patternCount++);
        }
        if (parent.code().edgeSize() >= maxEdgeSize) {
            // parent.clearEmbeddings(this.clusterDelimiter);
            parent.clearEmbeddings();
            return;
        }

        List<Pattern> children = enumerateChildren(parent);
        parent.setClusterDelimiter(this.clusterDelimiter);
        parent.setGraphDelimiter(this.transDelimiter);
        for (Pattern child : children) {
            if (this.winCount == 0) {
                this.ptw.saveNode(child);
                parent.removeChild(child);
            }
            if (!isFrequent(child)) {
                continue;
            }

            subgraphMining(trans, child);
        }
    }

    private List<Pattern> enumerateChildren(Pattern p) {
        TreeMap<DFSEdge, Pattern> addedChildren = new TreeMap<>();

        TreeMap<Integer, TreeSet<DFSEdge>> joinBackCands = new TreeMap<>();
        TreeMap<Integer, TreeSet<DFSEdge>> joinForCands = new TreeMap<>();
        joinCands(p, joinBackCands, joinForCands);

        TreeSet<DFSEdge> extendCands = new TreeSet<>();
        extendCands(p, extendCands);

        Cluster clusterDelimiter = p.clusterDelimiter();
        // if (clusterDelimiter != null) {
        // joinExtendIntersectionInClusterDelimiter(clusterDelimiter, p, joinBackCands,
        // joinForCands, extendCands, addedChildren);
        // }

        // for (Cluster c : p.clustersAfterDelimiter()) {
        for (Cluster c : p.clusters()) {
            if (clusterDelimiter != null && c.index() <= clusterDelimiter.index()) {
                continue;
            }
            joinExtendIntersection(c, p, joinBackCands, joinForCands, extendCands, addedChildren);
        }

        LabeledGraph graphDelimiter = p.graphDelimiter();
        // for (LabeledGraph g : p.graphsAfterDelimiter()) {
        for (LabeledGraph g : p.graphs()) {
            if (graphDelimiter != null && g.graphId() <= graphDelimiter.graphId()) {
                continue;
            }
            joinExtendOther(g, p, joinBackCands, joinForCands, extendCands, addedChildren);
        }

        return new ArrayList<>(addedChildren.values());
    }

    private void joinExtendIntersection(Cluster c, Pattern p, TreeMap<Integer, TreeSet<DFSEdge>> backCand,
            TreeMap<Integer, TreeSet<DFSEdge>> forCand, TreeSet<DFSEdge> extendCands,
            TreeMap<DFSEdge, Pattern> addedChildren) {
        List<Embedding> embeddings = p.intersectionEmbeddings(c);
        if (embeddings == null || embeddings.isEmpty()) {
            return;
        }
        LabeledGraph inter = c.intersection();
        DFSCode code = p.code();
        List<Integer> rmPathIds = code.rightMostPath();
        int rmDfsId = rmPathIds.get(rmPathIds.size() - 1);

        for (Embedding em : embeddings) {
            List<LabeledVertex> emVertices = em.vertices();
            // join backward edges
            for (Map.Entry<Integer, TreeSet<DFSEdge>> entry : backCand.entrySet()) {
                LabeledVertex from = emVertices.get(emVertices.size() - 1);
                LabeledVertex to = emVertices.get(entry.getKey());
                TreeSet<DFSEdge> cands = entry.getValue();
                LabeledEdge back = inter.edge(from.id(), to.id());
                if (back != null) {
                    DFSEdge dfsEdge = new DFSEdge(rmDfsId, entry.getKey(), inter.vLabel(from), inter.vLabel(to),
                            inter.eLabel(back));
                    if (cands.contains(dfsEdge)) {
                        Pattern child = p.child(dfsEdge);
                        child.addIntersectionEmbedding(c, em);
                        addedChildren.put(child.edge(), child);
                    }
                }

                Map<LabeledGraph, AdjEdges> borderAdj = c.borderAdj(from.id());
                for (Entry<LabeledGraph, AdjEdges> adjEntry : borderAdj.entrySet()) {
                    LabeledGraph g = adjEntry.getKey();
                    AdjEdges adj = adjEntry.getValue();
                    back = adj.edgeTo(to.id());
                    if (back == null) {
                        continue;
                    }
                    DFSEdge dfsEdge = new DFSEdge(rmDfsId, entry.getKey(), g.vLabel(from), g.vLabel(to),
                            g.eLabel(back));
                    if (cands.contains(dfsEdge)) {
                        Pattern child = p.child(dfsEdge);
                        child.addEmbedding(g, c, em);
                        addedChildren.put(child.edge(), child);
                    }
                }
            }

            // join forward edge
            BitSet emBits = new BitSet(maxVid + 1);
            for (LabeledVertex v : emVertices) {
                emBits.set(v.id());
            }

            for (Map.Entry<Integer, TreeSet<DFSEdge>> entry : forCand.entrySet()) {
                LabeledVertex from = emVertices.get(entry.getKey());
                TreeSet<DFSEdge> cands = entry.getValue();
                for (LabeledEdge e : inter.adjEdges(from.id())) {
                    if (emBits.get(e.to().id())) {
                        continue;
                    }
                    DFSEdge dfsEdge = new DFSEdge(entry.getKey(), emVertices.size(), inter.vLabel(from),
                            inter.vLabel(e.to()), inter.eLabel(e));
                    if (cands.contains(dfsEdge)) {
                        Pattern child = p.child(dfsEdge);
                        child.addIntersectionEmbedding(c, new Embedding(e.to(), em));
                        addedChildren.put(child.edge(), child);
                    }
                }

                Map<LabeledGraph, AdjEdges> borderAdj = c.borderAdj(from.id());
                for (Entry<LabeledGraph, AdjEdges> adjEntry : borderAdj.entrySet()) {
                    LabeledGraph g = adjEntry.getKey();
                    AdjEdges adj = adjEntry.getValue();
                    for (LabeledEdge e : adj) {
                        if (emBits.get(e.to().id())) {
                            continue;
                        }
                        DFSEdge dfsEdge = new DFSEdge(entry.getKey(), emVertices.size(), g.vLabel(from),
                                g.vLabel(e.to()), g.eLabel(e));
                        if (cands.contains(dfsEdge)) {
                            Pattern child = p.child(dfsEdge);
                            child.addEmbedding(g, c, new Embedding(e.to(), em));
                            addedChildren.put(child.edge(), child);
                        }
                    }
                }
            }

            // extend backward edges
            LabeledVertex from = emVertices.get(rmDfsId);
            for (int j = 0; j < rmPathIds.size() - 2; j++) {
                int toId = rmPathIds.get(j);
                LabeledVertex to = emVertices.get(toId);
                LabeledEdge back = inter.edge(from.id(), to.id());
                LabeledVertex nextTo = emVertices.get(rmPathIds.get(j + 1));
                LabeledEdge pathEdge = inter.edge(to.id(), nextTo.id());
                if (back != null) {
                    if (inter.eLabel(pathEdge) > inter.eLabel(back) || (inter.eLabel(pathEdge) == inter.eLabel(back)
                            && inter.vLabel(nextTo) > inter.vLabel(back.from()))) {
                        continue;
                    }

                    DFSEdge dfsEdge;
                    if (inter.vLabel(from) <= inter.vLabel(to)) {
                        dfsEdge = new DFSEdge(0, 1, inter.vLabel(from), inter.vLabel(to), inter.eLabel(back));
                    } else {
                        dfsEdge = new DFSEdge(0, 1, inter.vLabel(to), inter.vLabel(from), inter.eLabel(back));
                    }
                    if (extendCands.contains(dfsEdge)) {
                        Pattern child = p.child(rmDfsId, toId, inter.vLabel(from), inter.vLabel(to),
                                inter.eLabel(back));
                        child.addIntersectionEmbedding(c, em);
                        addedChildren.put(child.edge(), child);
                    }
                }

                Map<LabeledGraph, AdjEdges> borderAdj = c.borderAdj(from.id());
                for (Entry<LabeledGraph, AdjEdges> adjEntry : borderAdj.entrySet()) {
                    LabeledGraph g = adjEntry.getKey();
                    AdjEdges adj = adjEntry.getValue();
                    back = adj.edgeTo(to.id());
                    if (back == null) {
                        continue;
                    }
                    if (g.eLabel(pathEdge) > g.eLabel(back)
                            || (g.eLabel(pathEdge) == g.eLabel(back) && g.vLabel(nextTo) > g.vLabel(back.from()))) {
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
                        child.addEmbedding(g, c, em);
                        addedChildren.put(child.edge(), child);
                    }
                }
            }

            // extend forward edges
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
                    Pattern child = p.child(rmDfsId, emVertices.size(), inter.vLabel(from), inter.vLabel(to),
                            inter.eLabel(e));
                    child.addIntersectionEmbedding(c, new Embedding(e.to(), em));
                    addedChildren.put(child.edge(), child);
                }
            }

            Map<LabeledGraph, AdjEdges> borderAdj = c.borderAdj(from.id());
            for (Entry<LabeledGraph, AdjEdges> adjEntry : borderAdj.entrySet()) {
                LabeledGraph g = adjEntry.getKey();
                AdjEdges adj = adjEntry.getValue();
                for (LabeledEdge e : adj) {
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
                        child.addEmbedding(g, c, new Embedding(e.to(), em));
                        addedChildren.put(child.edge(), child);
                    }
                }
            }
        }
        if (c != this.clusterDelimiter) {
            embeddings.clear();
        }
    }

    private void joinExtendIntersectionInClusterDelimiter(Cluster clusterDelimiter, Pattern p,
            TreeMap<Integer, TreeSet<DFSEdge>> backCand, TreeMap<Integer, TreeSet<DFSEdge>> forCand,
            TreeSet<DFSEdge> extendCands, TreeMap<DFSEdge, Pattern> addedChildren) {
        LabeledGraph graphDelimiter = p.graphDelimiter();
        if (graphDelimiter.graphId() < clusterDelimiter.last().graphId()) {
            for (Pattern child : p.children()) {
                if (!child.containsCluster(clusterDelimiter)) {
                    continue;
                }
                List<Embedding> interEmbeddings = child.intersectionEmbeddings(clusterDelimiter);
                List<Embedding> nonInterEmbeddings = new ArrayList<>();
                for (Embedding em : interEmbeddings) {
                    if (!containEmbedding(clusterDelimiter.intersection(), em, child)) {
                        nonInterEmbeddings.add(em);
                    }
                }
                interEmbeddings.removeAll(nonInterEmbeddings);
                if (!interEmbeddings.isEmpty()) {
                    addedChildren.put(child.edge(), child);
                    child.increaseSupport((int) (clusterDelimiter.last().graphId() - graphDelimiter.graphId()));
                } else {
                    child.removeCluster(clusterDelimiter, graphDelimiter);
                }
                for (Embedding em : nonInterEmbeddings) {
                    for (LabeledGraph g : clusterDelimiter) {
                        if (g.graphId() > graphDelimiter.graphId()) {
                            break;
                        }
                        child.addEmbedding(g, clusterDelimiter, em);
                    }
                }
                if (child.hasChild()) {
                    for (LabeledGraph g : clusterDelimiter) {
                        if (g.graphId() > graphDelimiter.graphId()) {
                            break;
                        }
                        child.embeddings(g).clear();
                        ;
                    }
                }
            }
        }

        List<Embedding> embeddings = p.intersectionEmbeddings(clusterDelimiter);
        if (embeddings == null || embeddings.isEmpty()) {
            return;
        }

        LabeledGraph inter = clusterDelimiter.intersection();
        DFSCode code = p.code();
        List<Integer> rmPathIds = code.rightMostPath();
        int rmDfsId = rmPathIds.get(rmPathIds.size() - 1);

        for (Embedding em : embeddings) {
            List<LabeledVertex> emVertices = em.vertices();
            // join backward edges
            for (Map.Entry<Integer, TreeSet<DFSEdge>> entry : backCand.entrySet()) {
                LabeledVertex from = emVertices.get(emVertices.size() - 1);
                LabeledVertex to = emVertices.get(entry.getKey());
                TreeSet<DFSEdge> cands = entry.getValue();

                Map<LabeledGraph, AdjEdges> borderAdj = clusterDelimiter.borderAdj(from.id());
                for (Entry<LabeledGraph, AdjEdges> adjEntry : borderAdj.entrySet()) {
                    LabeledGraph g = adjEntry.getKey();
                    if (graphDelimiter != null && g.graphId() <= graphDelimiter.graphId()) {
                        continue;
                    }
                    AdjEdges adj = adjEntry.getValue();
                    LabeledEdge back = adj.edgeTo(to.id());
                    if (back == null) {
                        continue;
                    }
                    DFSEdge dfsEdge = new DFSEdge(rmDfsId, entry.getKey(), g.vLabel(from), g.vLabel(to),
                            g.eLabel(back));
                    if (cands.contains(dfsEdge)) {
                        Pattern child = p.child(dfsEdge);
                        child.addEmbedding(g, clusterDelimiter, em);
                        addedChildren.put(child.edge(), child);
                    }
                }
            }

            // join forward edge
            BitSet emBits = new BitSet(maxVid + 1);
            for (LabeledVertex v : emVertices) {
                emBits.set(v.id());
            }

            for (Map.Entry<Integer, TreeSet<DFSEdge>> entry : forCand.entrySet()) {
                LabeledVertex from = emVertices.get(entry.getKey());
                TreeSet<DFSEdge> cands = entry.getValue();

                Map<LabeledGraph, AdjEdges> borderAdj = clusterDelimiter.borderAdj(from.id());
                for (Entry<LabeledGraph, AdjEdges> adjEntry : borderAdj.entrySet()) {
                    LabeledGraph g = adjEntry.getKey();
                    if (graphDelimiter != null && g.graphId() <= graphDelimiter.graphId()) {
                        continue;
                    }
                    AdjEdges adj = adjEntry.getValue();
                    for (LabeledEdge e : adj) {
                        if (emBits.get(e.to().id())) {
                            continue;
                        }
                        DFSEdge dfsEdge = new DFSEdge(entry.getKey(), emVertices.size(), g.vLabel(from),
                                g.vLabel(e.to()), g.eLabel(e));
                        if (cands.contains(dfsEdge)) {
                            Pattern child = p.child(dfsEdge);
                            child.addEmbedding(g, clusterDelimiter, new Embedding(e.to(), em));
                            addedChildren.put(child.edge(), child);
                        }
                    }
                }
            }

            // extend backward edges
            LabeledVertex from = emVertices.get(rmDfsId);
            for (int j = 0; j < rmPathIds.size() - 2; j++) {
                int toId = rmPathIds.get(j);
                LabeledVertex to = emVertices.get(toId);
                LabeledVertex nextTo = emVertices.get(rmPathIds.get(j + 1));
                LabeledEdge pathEdge = inter.edge(to.id(), nextTo.id());

                Map<LabeledGraph, AdjEdges> borderAdj = clusterDelimiter.borderAdj(from.id());
                for (Entry<LabeledGraph, AdjEdges> adjEntry : borderAdj.entrySet()) {
                    LabeledGraph g = adjEntry.getKey();
                    if (graphDelimiter != null && g.graphId() <= graphDelimiter.graphId()) {
                        continue;
                    }
                    AdjEdges adj = adjEntry.getValue();
                    LabeledEdge back = adj.edgeTo(to.id());
                    if (back == null) {
                        continue;
                    }
                    if (g.eLabel(pathEdge) > g.eLabel(back)
                            || (g.eLabel(pathEdge) == g.eLabel(back) && g.vLabel(nextTo) > g.vLabel(back.from()))) {
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
                        child.addEmbedding(g, clusterDelimiter, em);
                        addedChildren.put(child.edge(), child);
                    }
                }
            }

            // extend forward edges
            Map<LabeledGraph, AdjEdges> borderAdj = clusterDelimiter.borderAdj(from.id());
            for (Entry<LabeledGraph, AdjEdges> adjEntry : borderAdj.entrySet()) {
                LabeledGraph g = adjEntry.getKey();
                if (graphDelimiter != null && g.graphId() <= graphDelimiter.graphId()) {
                    continue;
                }
                AdjEdges adj = adjEntry.getValue();
                for (LabeledEdge e : adj) {
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
                        child.addEmbedding(g, clusterDelimiter, new Embedding(e.to(), em));
                        addedChildren.put(child.edge(), child);
                    }
                }
            }
        }
        if (clusterDelimiter != this.clusterDelimiter) {
            embeddings.clear();
        }
    }

    private void joinExtendOther(LabeledGraph g, Pattern p, TreeMap<Integer, TreeSet<DFSEdge>> backCand,
            TreeMap<Integer, TreeSet<DFSEdge>> forCand, TreeSet<DFSEdge> extendCands,
            TreeMap<DFSEdge, Pattern> addedChildren) {
        List<Embedding> embeddings = p.embeddings(g);
        if (embeddings == null || embeddings.isEmpty()) {
            return;
        }
        DFSCode code = p.code();
        List<Integer> rmPathIds = code.rightMostPath();
        int rmDfsId = rmPathIds.get(rmPathIds.size() - 1);
        for (Embedding em : embeddings) {
            List<LabeledVertex> emVertices = em.vertices();

            // join backward edges
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
                    if (this.partition) {
                        child.addEmbedding(g, g.getCluster(), em);
                    } else {
                        child.addEmbedding(g, em);
                    }
                    addedChildren.put(child.edge(), child);
                }
            }

            // join forward edges
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

                    DFSEdge dfsEdge = new DFSEdge(entry.getKey(), emVertices.size(), g.vLabel(from), g.vLabel(e.to()),
                            g.eLabel(e));
                    TreeSet<DFSEdge> cands = entry.getValue();
                    if (cands.contains(dfsEdge)) {
                        Pattern child = p.child(dfsEdge);
                        if (this.partition) {
                            child.addEmbedding(g, g.getCluster(), new Embedding(e.to(), em));
                        } else {
                            child.addEmbedding(g, new Embedding(e.to(), em));
                        }
                        addedChildren.put(child.edge(), child);
                    }
                }
            }

            // extend
            // extend backward edges
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
                if (g.eLabel(pathEdge) > g.eLabel(back)
                        || (g.eLabel(pathEdge) == g.eLabel(back) && g.vLabel(nextTo) > g.vLabel(back.from()))) {
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
                    if (this.partition) {
                        child.addEmbedding(g, g.getCluster(), em);
                    } else {
                        child.addEmbedding(g, em);
                    }
                    addedChildren.put(child.edge(), child);
                }
            }

            // extend rm forward edges
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
                    if (this.partition) {
                        child.addEmbedding(g, g.getCluster(), new Embedding(to, em));
                    } else {
                        child.addEmbedding(g, new Embedding(to, em));
                    }
                    addedChildren.put(child.edge(), child);
                }
            }
        }

        embeddings.clear();
    }

    private void joinCands(Pattern p, TreeMap<Integer, TreeSet<DFSEdge>> backCand,
            TreeMap<Integer, TreeSet<DFSEdge>> forCand) {
        DFSEdge e1 = p.edge();
        for (Pattern sib : p.rightSiblings()) {
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
                candidates.add(
                        new DFSEdge(e2.from(), p.code().nodeCount(), e2.fromLabel(), e2.toLabel(), e2.edgeLabel()));
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
            DFSEdge e = ep.edge();
            if (e.toLabel() == lastEdge.toLabel() && e.edgeLabel() >= firstEdge.edgeLabel()) {
                extendCands.add(e);
            }
        }
        for (PointPattern pp : this.points.tailMap(firstEdge.fromLabel(), false).headMap(lastEdge.toLabel()).values()) {
            for (Pattern ep : pp.children()) {
                DFSEdge e = ep.edge();
                if (e.toLabel() == lastEdge.toLabel()) {
                    extendCands.add(e);
                }
            }
        }
        PointPattern rmPoint = this.points.get(lastEdge.toLabel());
        if (rmPoint != null) {
            for (Pattern ep : rmPoint.children()) {
                extendCands.add(ep.edge());
            }
        }
    }

    private boolean isFrequent(Pattern p) {
        return p.support() >= this.absSup;
    }

    private void saveResult() {
        // FileWriter fw = null;
        // BufferedWriter bw = null;
        try {
            // fw = new FileWriter(out);
            // bw = new BufferedWriter(fw);
            for (PointPattern pp : points.values()) {
                if (!isFrequent(pp)) {
                    continue;
                }
                pointCount++;
                // bw.write("t # " + (this.patternCount++) + " * " + pp.support());
                // bw.newLine();
                // bw.write("v 0 " + pp.label());
                // bw.newLine();
                // bw.newLine();
                this.pw.save(pp, this.patternCount++);

                for (Pattern child : pp.children()) {
                    if (!isFrequent(child) || !child.checkMin()) {
                        continue;
                    }
                    save(child);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // try {
            // bw.close();
            // fw.close();
            // } catch (IOException e) {
            // e.printStackTrace();
            // }

        }
    }

    // private void save(Pattern p, BufferedWriter bw) throws IOException {
    private void save(Pattern p) throws IOException {
        // bw.write("t # " + (this.patternCount++) + " * " + p.support());
        // bw.newLine();
        // DFSCode code = p.code();
        // LabeledGraph g = code.toGraph();
        // for (int i = 0; i < g.vSize(); i++) {
        // LabeledVertex v = g.vertex(i);
        // bw.write("v " + i + " " + g.vLabel(v));
        // bw.newLine();
        // }
        // for (DFSEdge e : code.edges()) {
        // bw.write("e " + e.from() + " " + e.to() + " " + e.edgeLabel());
        // bw.newLine();
        // }
        // bw.newLine();
        this.pw.save(p, this.patternCount++);

        for (Pattern child : p.children()) {
            if (!isFrequent(child) || !child.checkMin()) {
                continue;
            }
            // save(child, bw);
            save(child);
        }
    }

    public void setOutput(File out) {
        this.out = out;
        this.pw = new PatternWriter(this.out);
    }

    private boolean containEmbedding(LabeledGraph g, Embedding em, Pattern p) {
        if (p instanceof PointPattern) {
            return g.vertex(em.vertex().id()) != null;
        }
        List<LabeledVertex> vertices = em.vertices();
        DFSCode code = p.code();
        for (DFSEdge e : code.edges()) {
            LabeledVertex from = vertices.get(e.from());
            LabeledVertex to = vertices.get(e.to());
            if (g.edge(from.id(), to.id()) == null) {
                return false;
            }
        }
        return true;
    }

    private void outputGraphIds(long[] graphIds, File out) {
        try {
            FileWriter fw = new FileWriter(out);
            BufferedWriter bw = new BufferedWriter(fw);
            for (long id : graphIds) {
                bw.write(String.valueOf(id));
                bw.newLine();
            }
            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPatterns() {
        FileReader fr = null;
        BufferedReader br = null;

        try {
            fr = new FileReader(new File(PATTERN_TREE_FILE));
            br = new BufferedReader(fr);

            String line = null;
            while ((line = br.readLine()) != null) {
                int index = line.indexOf("),");
                String code = line.substring(0, index + 1);
                Pattern p = getPattern(code);

                String[] items = line.substring(index + 2).split(",");
                int support = Integer.parseInt(items[0]);
                p.setSupport(support);

                //sequences
                if (!" ".equals(items[1])) {
                    String[] indices = items[1].split(" ");
                    for (String str : indices) {
                        p.addCluster(this.clusters.get(Integer.parseInt(str)));
                    }
                }

                //graphs
                if (!" ".equals(items[2])) {
                    String[] graphIds = items[2].split(" ");
                    for (String id : graphIds) {
                        p.addGraph(this.trans.get(Integer.parseInt(id)));
                    }
                }

                //sequence delimiter
                int clusterDelimiter = Integer.parseInt(items[3]);
                if (clusterDelimiter != -1) {
                    p.setClusterDelimiter(this.clusters.get(clusterDelimiter));
                }

                //graph delimiter
                int graphDelimiter = Integer.parseInt(items[4]);
                if (graphDelimiter != -1) {
                    p.setGraphDelimiter(this.trans.get(graphDelimiter));
                }

                //isMinChecked
                boolean isMinChecked = "1".equals(items[5]);
                p.setChecked(isMinChecked);

                //minCheckResult
                boolean minCheckResult = "1".equals(items[6]);
                p.setMinCheckResult(minCheckResult);
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

    }

    private Pattern getPattern(String code) {
        DFSCode dfsCode = DFSCode.parse(code);
        int label = dfsCode.get(0).fromLabel();
        Pattern p = this.points.get(label);
        for (int i = 0; i < dfsCode.edgeSize(); i++) {
            p = p.child(dfsCode.get(i));
        }
        return p;
    }
}
