package com.liang.fsmhg;

import java.io.BufferedWriter;
import java.io.File;
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
    private List<LabeledGraph> filler;
    private int patternCount = 0;
    private int pointCount = 0;

    private int winCount = -1;

    public FSMHGWIN(double minSupport, int maxEdgeSize, boolean partition, double similarity) {
        this.minSup = minSupport;
        this.maxEdgeSize = maxEdgeSize;
        this.partition = partition;
        this.similarity = similarity;
        this.points = new TreeMap<>();
        this.clusters = new ArrayList<>();
        this.trans = new ArrayList<>();
    }

    public void enumerate(List<LabeledGraph> newTrans) {
        this.winCount++;

        long startTime = System.currentTimeMillis();
        this.patternCount = 0;
        this.pointCount = 0;

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

        shrink(removed);
        grow(added);

        saveResult();
        long endTime = System.currentTimeMillis();
        System.out.println(pointCount + " point patterns");
        System.out.println((this.patternCount - pointCount) + " connected patterns.");
        System.out.println("Duration = " + (endTime - startTime));
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
        this.transDelimiter = addedTrans.get(addedTrans.size() - 1);
        List<Cluster> addedClusters;
        Map<Integer, PointPattern> addedPoints;
        Map<DFSEdge, Pattern> addedEdges;
        if (partition) {
            // addedClusters = Cluster.partition(addedTrans, similarity, clusterCounter);
            // this.clusterDelimiter = addedClusters.get(addedClusters.size() - 1);
            // this.clusterCounter += addedClusters.size();
            // this.clusters.addAll(addedClusters);
            // addedPoints = pointsByPartition(addedClusters);
            // addedEdges = edgesByPartition(addedPoints, addedClusters);

            //TODO process unfull cluster
            Cluster lastClusterDelimiter = this.clusterDelimiter;
            List<LabeledGraph> appended = new ArrayList<>();
            if (lastClusterDelimiter != null) {
                for (LabeledGraph g : addedTrans) {
                    if (!lastClusterDelimiter.append(g)) {
                        break;
                    }
                    appended.add(g);
                }
                addedTrans.removeAll(appended);
            }
            addedClusters = Cluster.partition(addedTrans, similarity, clusterCounter);
            this.clusterDelimiter = addedClusters.get(addedClusters.size() - 1);
            this.clusterCounter += addedClusters.size();
            this.clusters.addAll(addedClusters);
            addedPoints = pointsByPartition1(lastClusterDelimiter, appended, addedClusters);
            addedEdges = edgesByPartition1(addedPoints);
        } else {
            addedPoints = pointsNoPartition(addedTrans);
            addedEdges = edgesNoPartition(addedPoints, addedTrans);
        }

        for (Pattern p : addedEdges.values()) {
            if (!isFrequent(p)) {
                //TODO for children of infrequent pattern, search their embeddings in unfull cluster
                continue;
            }
            subgraphMining(addedTrans, p);
            // p.setClusterDelimiter(this.clusterDelimiter);
            // p.setGraphDelimiter(this.transDelimiter);
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

    public TreeMap<Integer, PointPattern> pointsByPartition1(Cluster lastClusterDelimiter, List<LabeledGraph> appended, List<Cluster> clusters) {
        TreeMap<Integer, PointPattern> addedPoints = new TreeMap<>();
        
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
            
            List<Cluster> clusters = pp.clustersAfterDelimiter();
            for (Cluster c : clusters) {
                
                LabeledGraph graphDelimiter = pp.graphDelimiter();
                // LabeledGraph first = c.first();
                // boolean extendInInter = graphDelimiter == null || graphDelimiter.graphId() < first.graphId();
                List<Embedding> nonInterEmbeddings = new ArrayList<>();
                LabeledGraph inter = c.intersection();
                List<Embedding> embeddings = pp.intersectionEmbeddings(c);
                if (embeddings != null) {
                    for (Embedding em : embeddings) {
                        if (c == pp.clusterDelimiter() && !containEmbedding(inter, em, pp)) {
                            if (!pp.hasChild()) {
                                for (LabeledGraph g : c) {
                                    if (g.graphId() <= graphDelimiter.graphId()) {
                                        pp.addEmbedding(g, c, em);
                                        nonInterEmbeddings.add(em);
                                    }
                                }
                            }
                            continue;
                        }
                        if (c != pp.clusterDelimiter()) {
                            for (LabeledEdge e : inter.adjEdges(em.vertex().id())) {
                                if (inter.vLabel(e.from()) > inter.vLabel(e.to())) {
                                    continue;
                                }
                                Pattern child = pp.child(0, 1, inter.vLabel(e.from()), inter.vLabel(e.to()), inter.eLabel(e));
                                child.addIntersectionEmbedding(c, new Embedding(e.to(), em));
                                addedEdges.put(child.edge(), child);
                            }
                        }

                        Map<LabeledGraph, AdjEdges> borderAdj = c.borderAdj(em.vertex().id());
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
                                child.addEmbedding(g, c, new Embedding(e.to(), em));
                                addedEdges.put(child.edge(), child);
                            }
                        }
                    }
                    if (c != this.clusterDelimiter) {
                        embeddings.clear();
                    } else {
                        embeddings.removeAll(nonInterEmbeddings);
                    }
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
        }

        return addedEdges;
    }

    public void subgraphMining(List<LabeledGraph> trans, Pattern parent) {
        if (!parent.checkMin()) {
            parent.clearEmbeddings();
            return;
        }
        if (parent.code().edgeSize() >= maxEdgeSize) {
            parent.clearEmbeddings();
            return;
        }

        List<Pattern> children = enumerateChildren(parent);
        parent.setClusterDelimiter(this.clusterDelimiter);
        parent.setGraphDelimiter(this.transDelimiter);
        for (Pattern child : children) {
            if (!isFrequent(child)) {
                //TODO for children of infrequent pattern, search their embeddings in unfull cluster
                continue;
            }
            subgraphMining(trans, child);
            // child.setClusterDelimiter(this.clusterDelimiter);
            // child.setGraphDelimiter(this.transDelimiter);
        }
    }

    private List<Pattern> enumerateChildren(Pattern p) {
        TreeMap<DFSEdge, Pattern> addedChildren = new TreeMap<>();

        TreeMap<Integer, TreeSet<DFSEdge>> joinBackCands = new TreeMap<>();
        TreeMap<Integer, TreeSet<DFSEdge>> joinForCands = new TreeMap<>();
        joinCands(p, joinBackCands, joinForCands);

        TreeSet<DFSEdge> extendCands = new TreeSet<>();
        extendCands(p, extendCands);
        
        for (Cluster c : p.clustersAfterDelimiter()) {
            // joinExtendIntersection(c, p, joinBackCands, joinForCands, extendCands, addedChildren);
            joinExtendIntersection1(c, p, joinBackCands, joinForCands, extendCands, addedChildren);
        }
        for (LabeledGraph g : p.graphsAfterDelimiter()) {
            joinExtendOther(g, p, joinBackCands, joinForCands, extendCands, addedChildren);
        }

        return new ArrayList<>(addedChildren.values());
    }

    private void joinExtendIntersection(Cluster c, Pattern p, TreeMap<Integer, TreeSet<DFSEdge>> backCand, TreeMap<Integer, TreeSet<DFSEdge>> forCand, TreeSet<DFSEdge> extendCands, TreeMap<DFSEdge, Pattern> addedChildren) {
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
            //join backward edges
            for (Map.Entry<Integer, TreeSet<DFSEdge>> entry : backCand.entrySet()) {
                LabeledVertex from = emVertices.get(emVertices.size() - 1);
                LabeledVertex to = emVertices.get(entry.getKey());
                TreeSet<DFSEdge> cands = entry.getValue();
                LabeledEdge back = inter.edge(from.id(), to.id());
                if (back != null) {
                    DFSEdge dfsEdge = new DFSEdge(rmDfsId, entry.getKey(), inter.vLabel(from), inter.vLabel(to), inter.eLabel(back));
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
                    DFSEdge dfsEdge = new DFSEdge(rmDfsId, entry.getKey(), g.vLabel(from), g.vLabel(to), g.eLabel(back));
                    if (cands.contains(dfsEdge)) {
                        Pattern child = p.child(dfsEdge);
                        child.addEmbedding(g, c, em);
                        addedChildren.put(child.edge(), child);
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
                TreeSet<DFSEdge> cands = entry.getValue();
                for (LabeledEdge e : inter.adjEdges(from.id())) {
                    if (emBits.get(e.to().id())) {
                        continue;
                    }
                    DFSEdge dfsEdge = new DFSEdge(entry.getKey(), emVertices.size(), inter.vLabel(from), inter.vLabel(e.to()), inter.eLabel(e));
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
                        DFSEdge dfsEdge = new DFSEdge(entry.getKey(), emVertices.size(), g.vLabel(from), g.vLabel(e.to()), g.eLabel(e));
                        if (cands.contains(dfsEdge)) {
                            Pattern child = p.child(dfsEdge);
                            child.addEmbedding(g, c, new Embedding(e.to(), em));
                            addedChildren.put(child.edge(), child);
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
                LabeledVertex nextTo = emVertices.get(rmPathIds.get(j + 1));
                LabeledEdge pathEdge = inter.edge(to.id(), nextTo.id());
                if (back != null) {
                    if (inter.eLabel(pathEdge) > inter.eLabel(back) || (inter.eLabel(pathEdge) == inter.eLabel(back) && inter.vLabel(nextTo) > inter.vLabel(back.from()))) {
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
                        child.addEmbedding(g, c, em);
                        addedChildren.put(child.edge(), child);
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
                    Pattern child = p.child(rmDfsId, emVertices.size(), inter.vLabel(from), inter.vLabel(to), inter.eLabel(e));
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

    private void joinExtendIntersection1(Cluster c, Pattern p, TreeMap<Integer, TreeSet<DFSEdge>> backCand, TreeMap<Integer, TreeSet<DFSEdge>> forCand, TreeSet<DFSEdge> extendCands, TreeMap<DFSEdge, Pattern> addedChildren) {
        // if (c == p.clusterDelimiter()) {
        //     p.increaseSupport((int)(c.last().graphId() - p.graphDelimiter().graphId()));
        // }
        List<Embedding> embeddings = p.intersectionEmbeddings(c);
        if (embeddings == null || embeddings.isEmpty()) {
            return;
        }
        LabeledGraph graphDelimiter = p.graphDelimiter();
        // LabeledGraph first = c.first();
        // boolean extendInInter = graphDelimiter == null || graphDelimiter.graphId() < first.graphId();
        List<Embedding> nonInterEmbeddings = new ArrayList<>();
        LabeledGraph inter = c.intersection();
        DFSCode code = p.code();
        List<Integer> rmPathIds = code.rightMostPath();
        int rmDfsId = rmPathIds.get(rmPathIds.size() - 1);
        
        int emCounter = -1;
        for (Embedding em : embeddings) {
            emCounter++;
            if (this.winCount == 1 && "(0,1,1,73,58)(1,2,58,13,18)(2,3,18,0,63)(3,4,63,33,36)(2,5,18,9,61)(5,6,61,49,31)(2,7,18,80,65)(1,8,58,46,74)(8,9,74,68,57)".equals(code.toString())) {
                // System.out.println();
            }

            if (c == p.clusterDelimiter() && !containEmbedding(inter, em, p)) {
                if (!p.hasChild()) {
                    for (LabeledGraph g : c) {
                        if (g.graphId() <= graphDelimiter.graphId()) {
                            p.addEmbedding(g, c, em);
                            nonInterEmbeddings.add(em);
                        }
                    }
                }
                continue;
            }

            List<LabeledVertex> emVertices = em.vertices();
            //join backward edges
            for (Map.Entry<Integer, TreeSet<DFSEdge>> entry : backCand.entrySet()) {
                LabeledVertex from = emVertices.get(emVertices.size() - 1);
                LabeledVertex to = emVertices.get(entry.getKey());
                TreeSet<DFSEdge> cands = entry.getValue();
                if (c != p.clusterDelimiter()) {
                    LabeledEdge back = inter.edge(from.id(), to.id());
                    if (back != null) {
                        DFSEdge dfsEdge = new DFSEdge(rmDfsId, entry.getKey(), inter.vLabel(from), inter.vLabel(to), inter.eLabel(back));
                        if (cands.contains(dfsEdge)) {
                            Pattern child = p.child(dfsEdge);
                            child.addIntersectionEmbedding(c, em);
                            addedChildren.put(child.edge(), child);
                        }
                    }
                }

                Map<LabeledGraph, AdjEdges> borderAdj = c.borderAdj(from.id());
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
                    DFSEdge dfsEdge = new DFSEdge(rmDfsId, entry.getKey(), g.vLabel(from), g.vLabel(to), g.eLabel(back));
                    if (cands.contains(dfsEdge)) {
                        Pattern child = p.child(dfsEdge);
                        child.addEmbedding(g, c, em);
                        addedChildren.put(child.edge(), child);
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
                TreeSet<DFSEdge> cands = entry.getValue();
                if (c != p.clusterDelimiter()) {
                    for (LabeledEdge e : inter.adjEdges(from.id())) {
                        if (emBits.get(e.to().id())) {
                            continue;
                        }
                        DFSEdge dfsEdge = new DFSEdge(entry.getKey(), emVertices.size(), inter.vLabel(from), inter.vLabel(e.to()), inter.eLabel(e));
                        if (cands.contains(dfsEdge)) {
                            Pattern child = p.child(dfsEdge);
                            child.addIntersectionEmbedding(c, new Embedding(e.to(), em));
                            addedChildren.put(child.edge(), child);
                        }
                    }
                }

                Map<LabeledGraph, AdjEdges> borderAdj = c.borderAdj(from.id());
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
                        DFSEdge dfsEdge = new DFSEdge(entry.getKey(), emVertices.size(), g.vLabel(from), g.vLabel(e.to()), g.eLabel(e));
                        if (cands.contains(dfsEdge)) {
                            Pattern child = p.child(dfsEdge);
                            child.addEmbedding(g, c, new Embedding(e.to(), em));
                            addedChildren.put(child.edge(), child);
                        }
                    }
                }
            }

            //extend backward edges
            LabeledVertex from = emVertices.get(rmDfsId);
            for (int j = 0; j < rmPathIds.size() - 2; j++) {
                int toId = rmPathIds.get(j);
                LabeledVertex to = emVertices.get(toId);
                LabeledVertex nextTo = emVertices.get(rmPathIds.get(j + 1));
                LabeledEdge pathEdge = inter.edge(to.id(), nextTo.id());
                if (c != p.clusterDelimiter()) {
                    LabeledEdge back = inter.edge(from.id(), to.id());
                    if (back != null) {
                        if (inter.eLabel(pathEdge) > inter.eLabel(back) || (inter.eLabel(pathEdge) == inter.eLabel(back) && inter.vLabel(nextTo) > inter.vLabel(back.from()))) {
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
                            child.addIntersectionEmbedding(c, em);
                            addedChildren.put(child.edge(), child);
                        }
                    }
                }

                Map<LabeledGraph, AdjEdges> borderAdj = c.borderAdj(from.id());
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
                        child.addEmbedding(g, c, em);
                        addedChildren.put(child.edge(), child);
                    }
                }
            }

            //extend forward edges
            if (c != p.clusterDelimiter()) {
                if (inter == null) {
                    System.out.println("inter == null");
                }
                if (inter.adjEdges(from.id()) == null) {
                    System.out.println("wincount = " + this.winCount + ", emcount = " + emCounter);
                    System.out.print("code = " + code);
                    System.out.println("adjEdges == null");
                    boolean containV = inter.vertex(from.id()) != null;
                    System.out.println("Contains from " + containV);
                    if (!containEmbedding(inter, em, p)) {
                        System.out.println("Contains embedddings false");
                    }
                }

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
                        Pattern child = p.child(rmDfsId, emVertices.size(), inter.vLabel(from), inter.vLabel(to), inter.eLabel(e));
                        child.addIntersectionEmbedding(c, new Embedding(e.to(), em));
                        addedChildren.put(child.edge(), child);
                    }
                }
            }

            Map<LabeledGraph, AdjEdges> borderAdj = c.borderAdj(from.id());
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
                        child.addEmbedding(g, c, new Embedding(e.to(), em));
                        addedChildren.put(child.edge(), child);
                    }
                }
            }
        }
        if (c != this.clusterDelimiter) {
            embeddings.clear();
        } else {
            embeddings.removeAll(nonInterEmbeddings);
        }
    }

    private void joinExtendOther(LabeledGraph g, Pattern p, TreeMap<Integer, TreeSet<DFSEdge>> backCand, TreeMap<Integer, TreeSet<DFSEdge>> forCand, TreeSet<DFSEdge> extendCands, TreeMap<DFSEdge, Pattern> addedChildren) {
        List<Embedding> embeddings = p.embeddings(g);
        if (embeddings == null || embeddings.isEmpty()) {
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
                    if (this.partition) {
                        child.addEmbedding(g, g.getCluster(), em);
                    } else {
                        child.addEmbedding(g, em);
                    }
                    addedChildren.put(child.edge(), child);
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
                        if (this.partition) {
                            child.addEmbedding(g, g.getCluster(), new Embedding(e.to(), em));
                        } else {
                            child.addEmbedding(g, new Embedding(e.to(), em));
                        }
                        addedChildren.put(child.edge(), child);
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
                    if (this.partition) {
                        child.addEmbedding(g, g.getCluster(), em);
                    } else {
                        child.addEmbedding(g, em);
                    }
                    addedChildren.put(child.edge(), child);
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

    private void joinCands(Pattern p, TreeMap<Integer, TreeSet<DFSEdge>> backCand, TreeMap<Integer, TreeSet<DFSEdge>> forCand) {
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
        FileWriter fw = null;
        BufferedWriter bw = null;
        try {
            fw = new FileWriter(out);
            bw = new BufferedWriter(fw);
            for (PointPattern pp : points.values()) {
                if (!isFrequent(pp)) {
                    continue;
                }
                pointCount++;
                bw.write("t # " + (this.patternCount++) + " * " + pp.support());
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
        bw.write("t # " + (this.patternCount++) + " * " + p.support());
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

    public void setOutput(File out) {
        this.out = out;
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
}
