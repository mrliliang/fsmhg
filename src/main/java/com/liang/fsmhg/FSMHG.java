package com.liang.fsmhg;

import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.TreeMap;
import java.util.TreeSet;

import com.liang.fsmhg.code.DFSCode;
import com.liang.fsmhg.code.DFSEdge;
import com.liang.fsmhg.graph.AdjEdges;
import com.liang.fsmhg.graph.LabeledEdge;
import com.liang.fsmhg.graph.LabeledGraph;
import com.liang.fsmhg.graph.LabeledVertex;

public class FSMHG {

    private TreeMap<Integer, PointPattern> points;
    private List<LabeledGraph> trans;
    List<Cluster> clusters;
    private double minSup;
    private double absSup;
    private int maxEdgeSize;
    private boolean partition;
    private double similarity;
    private boolean optimize = false;
    private int patternCount = 0;
    private int pointCount = 0;

    private int maxVid = 0;

    private PatternWriter pw;

    private long partitionTime = 0;
    private long numOfEmbedding = 0;
    private long numOfEmbeddingNoPartition = 0;

    public FSMHG(File out, double minSupport, int maxEdgeSize, boolean partition, double similarity) {
        this.minSup = minSupport;
        this.maxEdgeSize = maxEdgeSize;
        this.partition = partition;
        this.similarity = similarity;
        this.points = new TreeMap<>();
        this.pw = new PatternWriter(out);
    }

    // public void optimize(boolean opt) {
    //     this.optimize = opt;
    // }

    public void enumerate(List<LabeledGraph> trans) {
        long startTime = System.currentTimeMillis();
        this.trans = trans;
        System.out.println("Total trans: " + this.trans.size());
        this.absSup = Math.ceil(this.trans.size() * this.minSup);

        if (partition) {
            long partitionBegin = System.currentTimeMillis();
            this.clusters = Cluster.partition(trans, similarity, 0);
            long partitionEnd = System.currentTimeMillis();
            this.partitionTime += (partitionEnd - partitionBegin);
            System.out.println(trans.size() + " snapshots in " + clusters.size() + " clusters");
            this.points = pointsCluster(this.clusters);
            edges(this.points, this.clusters);
        } else {
            this.points = points(this.trans);
            edges(points);
        }

        for (PointPattern pp : this.points.values()) {
            if (!isFrequent(pp)) {
                continue;
            }
            this.pointCount++;
            pw.save(pp, this.patternCount++);
            for (Pattern p : pp.children()) {
                this.numOfEmbedding += p.numberOfEmbeddings();
                this.numOfEmbeddingNoPartition += p.numberOfEmbeddingsNoPartition();
                if (!isFrequent(p)) {
                    continue;
                }
                subgraphMining(trans, p);
            }
        }
        pw.close();
        System.out.println(this.pointCount + " point patterns");
        System.out.println((this.patternCount - this.pointCount) + " connected patterns.");

        System.out.println("support = " + this.minSup);
        if (this.partition) {
            System.out.println("similarity = " + this.similarity);
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Duration = " + (endTime - startTime));
        System.out.println("Partition time = " + partitionTime);
        System.out.println("Number of embeddings partition = " + this.numOfEmbedding);
        System.out.println("Number of embeddings no partition = " + this.numOfEmbeddingNoPartition);

    }

    public TreeMap<Integer, PointPattern> pointsCluster(List<Cluster> clusters) {
        TreeMap<Integer, PointPattern> points = new TreeMap<>();
        for (Cluster c : clusters) {
            LabeledGraph inter = c.intersection();
            for (LabeledVertex v : inter.vertices()) {
                PointPattern pattern = points.get(inter.vLabel(v));
                if (pattern == null) {
                    pattern = new PointPattern(inter.vLabel(v));
                }
                Embedding em = new Embedding(v, null);
                pattern.addIntersectionEmbedding(c, em);
                points.putIfAbsent(pattern.label(), pattern);
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
                    PointPattern pattern = points.get(g.vLabel(v));
                    if (pattern == null) {
                        pattern = new PointPattern(g.vLabel(v));
                    }
                    Embedding em = new Embedding(v, null);
                    pattern.addEmbedding(g, c, em);
                    points.putIfAbsent(pattern.label(), pattern);
                    if (v.id() > maxVid) {
                        maxVid = v.id();
                    }
                }
            }
        }
        return points;
    }

    public void edges(Map<Integer, PointPattern> points, List<Cluster> clusters) {
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
                            // child.setMin(true);
                            child.addIntersectionEmbedding(c, new Embedding(e.to(), em));
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
                                // child.setMin(true);
                                child.addEmbedding(g, c, new Embedding(e.to(), em));
                            }
                        }
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
                            // child.setMin(true);
                            child.addEmbedding(g, c, new Embedding(e.to(), em));
                        }
                    }
                }
            }
        }
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

    public void edges(Map<Integer, PointPattern> points) {
        for (PointPattern pp : points.values()) {
            for (LabeledGraph g : pp.unClusteredGraphs()) {
                List<Embedding> embeddings = pp.embeddings(g);
                for (Embedding em : embeddings) {
                    LabeledVertex from = em.vertex();
                    for (LabeledEdge e : g.adjEdges(from.id())) {
                        LabeledVertex to = e.to();
                        if (g.vLabel(from) > g.vLabel(to)) {
                            continue;
                        }
                        Pattern child = pp.child(0, 1, g.vLabel(from), g.vLabel(to), g.eLabel(e));
                        // child.setMin(true);
                        child.addEmbedding(g, new Embedding(to, em));
                    }
                }
            }
        }
    }

    public void subgraphMining(List<LabeledGraph> trans, Pattern parent) {
        if (!parent.checkMin()) {
            return;
        }
        pw.save(parent, this.patternCount++);
        if (parent.code().edgeSize() >= maxEdgeSize) {
            return;
        }
        enumerateChildren(parent);
        // checkChildrenMinCode(parent.children(), parent.edge());
        if (!parent.hasChild()) {
            return;
        }
        // parent.code().nodeCount();
        for (Pattern child : parent.children()) {
            this.numOfEmbedding += child.numberOfEmbeddings();
            this.numOfEmbeddingNoPartition += child.numberOfEmbeddingsNoPartition();
            if (!isFrequent(child)) {
                parent.removeChild(child);
                continue;
            }
            subgraphMining(trans, child);
            parent.removeChild(child);
        }
    }

    // private void checkChildrenMinCode(List<Pattern> children, DFSEdge parentLastEdge) {
    //     if (!parentLastEdge.isForward()) {
    //         return;
    //     }
    //     HashMap<Integer, List<Pattern>> backwardGroups = new HashMap<>();
    //     List<Pattern> forwardGroup = new ArrayList<>();
    //     for (Pattern p : children) {
    //         if (!isFrequent(p)) {
    //             continue;
    //         }
    //         DFSEdge dfsEdge = p.edge();
    //         if (dfsEdge.from() != parentLastEdge.to()) {
    //             continue;
    //         }
    //         if (dfsEdge.isForward()) {
    //             forwardGroup.add(p);
    //         } else {
    //             List<Pattern> group = backwardGroups.computeIfAbsent(dfsEdge.to(), new Function<Integer, List<Pattern>>() {
    //                 @Override
    //                 public List<Pattern> apply(Integer t) {
    //                     return new ArrayList<>();
    //                 }
    //             });
    //             group.add(p);
    //         }
    //     }
    //     for (List<Pattern> group : backwardGroups.values()) {
    //         binaryCheck(group);
    //     }
    //     binaryCheck(forwardGroup);
    // }

    // private void binaryCheck(List<Pattern> patterns) {
    //     if (patterns.size() == 0) {
    //         return;
    //     }
    //     if (patterns.size() == 1) {
    //         patterns.get(0).checkMin();
    //         return;
    //     }
    //     int left = 0;
    //     int right = patterns.size() - 1;
    //     int nonMinBound = left + (right - left) / 2;
    //     int minBound = nonMinBound + 1;
    //     boolean noMin = patterns.get(nonMinBound).checkMin();;
    //     boolean min= patterns.get(minBound).checkMin();
    //     while (left < right) {
    //         if (noMin && min) {
    //             right = nonMinBound;
    //         } else if (!noMin && !min) {
    //             left = minBound;
    //         } else if (!noMin && min) {
    //             break;
    //         }
    //         nonMinBound = left + (right - left) / 2;
    //         minBound = nonMinBound + 1;
    //         noMin = patterns.get(nonMinBound).checkMin();
    //         min = patterns.get(minBound).checkMin();
    //     }

    //     for (int i = 0; i < nonMinBound; i++) {
    //         patterns.get(i).setMin(false);
    //     }
    //     for (int i = minBound + 1; i < patterns.size(); i++) {
    //         patterns.get(i).setMin(true);
    //     }
    // }

    private List<Pattern> enumerateChildren(Pattern p) {
        TreeMap<DFSEdge, Pattern> children = new TreeMap<>();

        if (!optimize) {
            TreeMap<Integer, TreeSet<DFSEdge>> joinBackCands = new TreeMap<>();
            TreeMap<Integer, TreeSet<DFSEdge>> joinForCands = new TreeMap<>();
            joinCands(p, joinBackCands, joinForCands);
    
            TreeSet<DFSEdge> extendCands = new TreeSet<>();
            extendCands(p, extendCands);
    
            for (Cluster c : p.clusters()) {
                joinExtendIntersection(c, p, joinBackCands, joinForCands, extendCands);
            }
            for (LabeledGraph g : p.graphs()) {
                joinExtendOther(g, p, joinBackCands, joinForCands, extendCands);
            }
        } else {
            // TreeMap<Integer, TreeMap<DFSEdge, Boolean>> joinBackCands = new TreeMap<>();
            // TreeMap<Integer, TreeMap<DFSEdge, Boolean>> joinForCands = new TreeMap<>();
            // joinCands1(p, joinBackCands, joinForCands);
    
            // TreeSet<DFSEdge> extendCands = new TreeSet<>();
            // extendCands(p, extendCands);
    
            // for (Cluster c : p.clusters()) {
            //     joinExtendIntersection1(c, p, joinBackCands, joinForCands, extendCands);
            // }
            // for (LabeledGraph g : p.graphs()) {
            //     joinExtendOther1(g, p, joinBackCands, joinForCands, extendCands);
            // }
        }

        return new ArrayList<>(children.values());
    }

    private void joinExtendIntersection(Cluster c, Pattern p, TreeMap<Integer, TreeSet<DFSEdge>> backCand, TreeMap<Integer, TreeSet<DFSEdge>> forCand, TreeSet<DFSEdge> extendCands) {
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
                    }
                }
            }
        }
    }

    private void joinExtendOther(LabeledGraph g, Pattern p, TreeMap<Integer, TreeSet<DFSEdge>> backCand, TreeMap<Integer, TreeSet<DFSEdge>> forCand, TreeSet<DFSEdge> extendCands) {
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
                        child.addEmbedding(g, this.clusters.get(g.clusterIndex()), em);
                    } else {
                        child.addEmbedding(g, em);
                    }
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
                            child.addEmbedding(g, this.clusters.get(g.clusterIndex()), new Embedding(e.to(), em));
                        } else {
                            child.addEmbedding(g, new Embedding(e.to(), em));
                        }
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
                        child.addEmbedding(g, this.clusters.get(g.clusterIndex()), em);
                    } else {
                        child.addEmbedding(g, em);
                    }
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
                        child.addEmbedding(g, this.clusters.get(g.clusterIndex()), new Embedding(to, em));
                    } else {
                        child.addEmbedding(g, new Embedding(to, em));
                    }
                }
            }
        }
    }

    // private void joinExtendIntersection1(Cluster c, Pattern p, TreeMap<Integer, TreeMap<DFSEdge, Boolean>> backCand, TreeMap<Integer, TreeMap<DFSEdge, Boolean>> forCand, TreeSet<DFSEdge> extendCands) {
    //     List<Embedding> embeddings = p.intersectionEmbeddings(c);
    //     if (embeddings == null || embeddings.isEmpty()) {
    //         return;
    //     }
    //     LabeledGraph inter = c.intersection();
    //     DFSCode code = p.code();
    //     List<Integer> rmPathIds = code.rightMostPath();
    //     int rmDfsId = rmPathIds.get(rmPathIds.size() - 1);
        
    //     for (Embedding em : embeddings) {
    //         List<LabeledVertex> emVertices = em.vertices();
    //         //join backward edges
    //         for (Map.Entry<Integer, TreeMap<DFSEdge, Boolean>> entry : backCand.entrySet()) {
    //             LabeledVertex from = emVertices.get(emVertices.size() - 1);
    //             LabeledVertex to = emVertices.get(entry.getKey());
    //             TreeMap<DFSEdge, Boolean> cands = entry.getValue();
    //             LabeledEdge back = inter.edge(from.id(), to.id());
    //             if (back != null) {
    //                 DFSEdge dfsEdge = new DFSEdge(rmDfsId, entry.getKey(), inter.vLabel(from), inter.vLabel(to), inter.eLabel(back));
    //                 Boolean isMin = cands.get(dfsEdge);
    //                 if (isMin != null) {
    //                     Pattern child = p.child(dfsEdge);
    //                     // if (isMin && code.edgeSize() >= 2) {
    //                     if (code.edgeSize() >= 2) {
    //                         child.setMin(isMin);
    //                     }
    //                     child.addIntersectionEmbedding(c, em);
    //                 }
    //             }

    //             Map<LabeledGraph, AdjEdges> borderAdj = c.borderAdj(from.id());
    //             for (Entry<LabeledGraph, AdjEdges> adjEntry : borderAdj.entrySet()) {
    //                 LabeledGraph g = adjEntry.getKey();
    //                 AdjEdges adj = adjEntry.getValue();
    //                 back = adj.edgeTo(to.id());
    //                 if (back == null) {
    //                     continue;
    //                 }
    //                 DFSEdge dfsEdge = new DFSEdge(rmDfsId, entry.getKey(), g.vLabel(from), g.vLabel(to), g.eLabel(back));
    //                 Boolean isMin = cands.get(dfsEdge);
    //                 if (isMin != null) {
    //                     Pattern child = p.child(dfsEdge);
    //                     // if (isMin && code.edgeSize() >= 2) {
    //                     if (code.edgeSize() >= 2) {
    //                         child.setMin(isMin);
    //                     }
    //                     child.addEmbedding(g, c, em);
    //                 }
    //             }
    //         }


    //         //join forward edge
    //         BitSet emBits = new BitSet(maxVid + 1);
    //         for (LabeledVertex v : emVertices) {
    //             emBits.set(v.id());
    //         }

    //         for (Map.Entry<Integer, TreeMap<DFSEdge, Boolean>> entry : forCand.entrySet()) {
    //             LabeledVertex from = emVertices.get(entry.getKey());
    //             TreeMap<DFSEdge, Boolean> cands = entry.getValue();
    //             for (LabeledEdge e : inter.adjEdges(from.id())) {
    //                 if (emBits.get(e.to().id())) {
    //                     continue;
    //                 }
    //                 DFSEdge dfsEdge = new DFSEdge(entry.getKey(), emVertices.size(), inter.vLabel(from), inter.vLabel(e.to()), inter.eLabel(e));
    //                 Boolean isMin = cands.get(dfsEdge);
    //                 if (isMin != null) {
    //                     Pattern child = p.child(dfsEdge);
    //                     // if (isMin && code.edgeSize() >= 2) {
    //                     if (code.edgeSize() >= 2) {
    //                         child.setMin(isMin);
    //                     }
    //                     child.addIntersectionEmbedding(c, new Embedding(e.to(), em));
    //                 }
    //             }

    //             Map<LabeledGraph, AdjEdges> borderAdj = c.borderAdj(from.id());
    //             for (Entry<LabeledGraph, AdjEdges> adjEntry : borderAdj.entrySet()) {
    //                 LabeledGraph g = adjEntry.getKey();
    //                 AdjEdges adj = adjEntry.getValue();
    //                 for (LabeledEdge e : adj) {
    //                     if (emBits.get(e.to().id())) {
    //                         continue;
    //                     }
    //                     DFSEdge dfsEdge = new DFSEdge(entry.getKey(), emVertices.size(), g.vLabel(from), g.vLabel(e.to()), g.eLabel(e));
    //                     Boolean isMin = cands.get(dfsEdge);
    //                     if (isMin != null) {
    //                         Pattern child = p.child(dfsEdge);
    //                         // if (isMin && code.edgeSize() >= 2) {
    //                         if (code.edgeSize() >= 2) {
    //                             child.setMin(isMin);
    //                         }
    //                         child.addEmbedding(g, c, new Embedding(e.to(), em));
    //                     }
    //                 }
    //             }
    //         }

    //         //extend backward edges
    //         LabeledVertex from = emVertices.get(rmDfsId);
    //         for (int j = 0; j < rmPathIds.size() - 2; j++) {
    //             int toId = rmPathIds.get(j);
    //             LabeledVertex to = emVertices.get(toId);
    //             LabeledEdge back = inter.edge(from.id(), to.id());
    //             LabeledVertex nextTo = emVertices.get(rmPathIds.get(j + 1));
    //             LabeledEdge pathEdge = inter.edge(to.id(), nextTo.id());
    //             if (back != null) {
    //                 if (inter.eLabel(pathEdge) > inter.eLabel(back) || (inter.eLabel(pathEdge) == inter.eLabel(back) && inter.vLabel(nextTo) > inter.vLabel(back.from()))) {
    //                     continue;
    //                 }

    //                 DFSEdge dfsEdge;
    //                 if (inter.vLabel(from) <= inter.vLabel(to)) {
    //                     dfsEdge = new DFSEdge(0, 1, inter.vLabel(from), inter.vLabel(to), inter.eLabel(back));
    //                 } else {
    //                     dfsEdge = new DFSEdge(0, 1, inter.vLabel(to), inter.vLabel(from), inter.eLabel(back));
    //                 }
    //                 if (extendCands.contains(dfsEdge)) {
    //                     Pattern child = p.child(rmDfsId, toId, inter.vLabel(from), inter.vLabel(to), inter.eLabel(back));
    //                     child.addIntersectionEmbedding(c, em);
    //                 }
    //             }

    //             Map<LabeledGraph, AdjEdges> borderAdj = c.borderAdj(from.id());
    //             for (Entry<LabeledGraph, AdjEdges> adjEntry : borderAdj.entrySet()) {
    //                 LabeledGraph g = adjEntry.getKey();
    //                 AdjEdges adj = adjEntry.getValue();
    //                 back = adj.edgeTo(to.id());
    //                 if (back == null) {
    //                     continue;
    //                 }
    //                 if (g.eLabel(pathEdge) > g.eLabel(back) || (g.eLabel(pathEdge) == g.eLabel(back) && g.vLabel(nextTo) > g.vLabel(back.from()))) {
    //                     continue;
    //                 }
    
    //                 DFSEdge dfsEdge;
    //                 if (g.vLabel(from) <= g.vLabel(to)) {
    //                     dfsEdge = new DFSEdge(0, 1, g.vLabel(from), g.vLabel(to), g.eLabel(back));
    //                 } else {
    //                     dfsEdge = new DFSEdge(0, 1, g.vLabel(to), g.vLabel(from), g.eLabel(back));
    //                 }
    //                 if (extendCands.contains(dfsEdge)) {
    //                     Pattern child = p.child(rmDfsId, toId, g.vLabel(from), g.vLabel(to), g.eLabel(back));
    //                     child.addEmbedding(g, c, em);
    //                 }
    //             }
    //         }

    //         //extend forward edges
    //         for (LabeledEdge e : inter.adjEdges(from.id())) {
    //             LabeledVertex to = e.to();
    //             if (emBits.get(to.id())) {
    //                 continue;
    //             }
    //             DFSEdge dfsEdge;
    //             if (inter.vLabel(from) <= inter.vLabel(to)) {
    //                 dfsEdge = new DFSEdge(0, 1, inter.vLabel(from), inter.vLabel(to), inter.eLabel(e));
    //             } else {
    //                 dfsEdge = new DFSEdge(0, 1, inter.vLabel(to), inter.vLabel(from), inter.eLabel(e));
    //             }
    //             if (extendCands.contains(dfsEdge)) {
    //                 Pattern child = p.child(rmDfsId, emVertices.size(), inter.vLabel(from), inter.vLabel(to), inter.eLabel(e));
    //                 child.addIntersectionEmbedding(c, new Embedding(e.to(), em));
    //             }
    //         }

    //         Map<LabeledGraph, AdjEdges> borderAdj = c.borderAdj(from.id());
    //         for (Entry<LabeledGraph, AdjEdges> adjEntry : borderAdj.entrySet()) {
    //             LabeledGraph g = adjEntry.getKey();
    //             AdjEdges adj = adjEntry.getValue();
    //             for (LabeledEdge e : adj) {
    //                 LabeledVertex to = e.to();
    //                 if (emBits.get(to.id())) {
    //                     continue;
    //                 }
    //                 DFSEdge dfsEdge;
    //                 if (g.vLabel(from) <= g.vLabel(to)) {
    //                     dfsEdge = new DFSEdge(0, 1, g.vLabel(from), g.vLabel(to), g.eLabel(e));
    //                 } else {
    //                     dfsEdge = new DFSEdge(0, 1, g.vLabel(to), g.vLabel(from), g.eLabel(e));
    //                 }
    //                 if (extendCands.contains(dfsEdge)) {
    //                     Pattern child = p.child(rmDfsId, emVertices.size(), g.vLabel(from), g.vLabel(to), g.eLabel(e));
    //                     child.addEmbedding(g, c, new Embedding(e.to(), em));
    //                 }
    //             }
    //         }
    //     }
    // }

    // private void joinExtendOther1(LabeledGraph g, Pattern p, TreeMap<Integer, TreeMap<DFSEdge, Boolean>> backCand, TreeMap<Integer, TreeMap<DFSEdge, Boolean>> forCand, TreeSet<DFSEdge> extendCands) {
    //     List<Embedding> embeddings = p.embeddings(g);
    //     if (embeddings == null || embeddings.isEmpty()) {
    //         return;
    //     }
    //     DFSCode code = p.code();
    //     List<Integer> rmPathIds = code.rightMostPath();
    //     int rmDfsId = rmPathIds.get(rmPathIds.size() - 1);
    //     for (Embedding em : embeddings) {
    //         List<LabeledVertex> emVertices = em.vertices();

    //         //join backward edges
    //         for (Map.Entry<Integer, TreeMap<DFSEdge, Boolean>> entry : backCand.entrySet()) {
    //             LabeledVertex from = emVertices.get(emVertices.size() - 1);
    //             LabeledVertex to = emVertices.get(entry.getKey());
    //             LabeledEdge back = g.edge(from.id(), to.id());
    //             if (back == null) {
    //                 continue;
    //             }

    //             TreeMap<DFSEdge, Boolean> cands = entry.getValue();
    //             DFSEdge dfsEdge = new DFSEdge(rmDfsId, entry.getKey(), g.vLabel(from), g.vLabel(to), g.eLabel(back));
    //             Boolean isMin = cands.get(dfsEdge);
    //             if (isMin != null) {
    //                 Pattern child = p.child(dfsEdge);
    //                 // if (isMin && code.edgeSize() >= 2) {
    //                 if (code.edgeSize() >= 2) {
    //                     child.setMin(isMin);
    //                 }
    //                 if (this.partition) {
    //                     child.addEmbedding(g, this.clusters.get(g.clusterIndex()), em);
    //                 } else {
    //                     child.addEmbedding(g, em);
    //                 }
    //             }
    //         }

    //         //join forward edges
    //         BitSet emBits = new BitSet(maxVid + 1);
    //         for (LabeledVertex v : emVertices) {
    //             emBits.set(v.id());
    //         }
    //         for (Map.Entry<Integer, TreeMap<DFSEdge, Boolean>> entry : forCand.entrySet()) {
    //             LabeledVertex from = emVertices.get(entry.getKey());
    //             for (LabeledEdge e : g.adjEdges(from.id())) {
    //                 if (emBits.get(e.to().id())) {
    //                     continue;
    //                 }

    //                 DFSEdge dfsEdge = new DFSEdge(entry.getKey(), emVertices.size(), g.vLabel(from), g.vLabel(e.to()), g.eLabel(e));
    //                 TreeMap<DFSEdge, Boolean> cands = entry.getValue();
    //                 Boolean isMin = cands.get(dfsEdge);
    //                 if (isMin != null) {
    //                     Pattern child = p.child(dfsEdge);
    //                     // if (isMin && code.edgeSize() >= 2) {
    //                     if (code.edgeSize() >= 2) {
    //                         child.setMin(isMin);
    //                     }
    //                     if (this.partition) {
    //                         child.addEmbedding(g, this.clusters.get(g.clusterIndex()), new Embedding(e.to(), em));
    //                     } else {
    //                         child.addEmbedding(g, new Embedding(e.to(), em));
    //                     }
    //                 }
    //             }
    //         }

    //         //extend
    //         //extend backward edges
    //         LabeledVertex from = emVertices.get(rmDfsId);
    //         for (int j = 0; j < rmPathIds.size() - 2; j++) {
    //             int toId = rmPathIds.get(j);
    //             LabeledVertex to = emVertices.get(toId);
    //             LabeledEdge back = g.edge(from.id(), to.id());
    //             if (back == null) {
    //                 continue;
    //             }
    //             LabeledVertex nextTo = emVertices.get(rmPathIds.get(j + 1));
    //             LabeledEdge pathEdge = g.edge(to.id(), nextTo.id());
    //             if (g.eLabel(pathEdge) > g.eLabel(back) || (g.eLabel(pathEdge) == g.eLabel(back) && g.vLabel(nextTo) > g.vLabel(back.from()))) {
    //                 continue;
    //             }

    //             DFSEdge dfsEdge;
    //             if (g.vLabel(from) <= g.vLabel(to)) {
    //                 dfsEdge = new DFSEdge(0, 1, g.vLabel(from), g.vLabel(to), g.eLabel(back));
    //             } else {
    //                 dfsEdge = new DFSEdge(0, 1, g.vLabel(to), g.vLabel(from), g.eLabel(back));
    //             }
    //             if (extendCands.contains(dfsEdge)) {
    //                 Pattern child = p.child(rmDfsId, toId, g.vLabel(from), g.vLabel(to), g.eLabel(back));
    //                 if (this.partition) {
    //                     child.addEmbedding(g, this.clusters.get(g.clusterIndex()), em);
    //                 } else {
    //                     child.addEmbedding(g, em);
    //                 }
    //             }
    //         }

    //         //extend rm forward edges
    //         for (LabeledEdge e : g.adjEdges(from.id())) {
    //             LabeledVertex to = e.to();
    //             if (emBits.get(to.id())) {
    //                 continue;
    //             }

    //             DFSEdge dfsEdge;
    //             if (g.vLabel(from) <= g.vLabel(to)) {
    //                 dfsEdge = new DFSEdge(0, 1, g.vLabel(from), g.vLabel(to), g.eLabel(e));
    //             } else {
    //                 dfsEdge = new DFSEdge(0, 1, g.vLabel(to), g.vLabel(from), g.eLabel(e));
    //             }
    //             if (extendCands.contains(dfsEdge)) {
    //                 Pattern child = p.child(rmDfsId, emVertices.size(), g.vLabel(from), g.vLabel(to), g.eLabel(e));
    //                 if (this.partition) {
    //                     child.addEmbedding(g, this.clusters.get(g.clusterIndex()), new Embedding(to, em));
    //                 } else {
    //                     child.addEmbedding(g, new Embedding(to, em));
    //                 }
    //             }
    //         }
    //     }
    // }

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

    // private void joinCands1(Pattern p, TreeMap<Integer, TreeMap<DFSEdge, Boolean>> backCand, TreeMap<Integer, TreeMap<DFSEdge, Boolean>> forCand) {
    //     DFSEdge e1 = p.edge();
    //     for (Pattern sib : p.rightSiblings()) {
    //         if (!isFrequent(sib)) {
    //             continue;
    //         }
    //         if (!sib.checkMin()) {
    //             continue;
    //         }
    //         DFSEdge e2 = sib.edge();
    //         if (e1.compareTo(e2) > 0) {
    //             continue;
    //         }

    //         if (!e1.isForward() && !e2.isForward() && e1.to() == e2.to()) {
    //             continue;
    //         }

    //         TreeMap<DFSEdge, Boolean> candidates;
    //         if (!e2.isForward()) {
    //             candidates = backCand.computeIfAbsent(e2.to(), vIndex -> new TreeMap<>());
    //             candidates.put(e2, sib.checkMin());
    //         } else {
    //             candidates = forCand.computeIfAbsent(e2.from(), vIndex -> new TreeMap<>());
    //             DFSEdge dfsEdge = new DFSEdge(e2.from(), p.code().nodeCount(), e2.fromLabel(), e2.toLabel(), e2.edgeLabel());
    //             candidates.put(dfsEdge, sib.checkMin());
    //         }
    //     }
    // }

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
        return p.support() >= this.absSup;
    }

    private void searchEmbeddings(Pattern p) {
        
    }
}
