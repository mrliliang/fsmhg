package com.liang.fsmhg;

import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import com.liang.fsmhg.Cluster.DeltaGraph;
import com.liang.fsmhg.code.DFSCode;
import com.liang.fsmhg.code.DFSEdge;
import com.liang.fsmhg.graph.LabeledEdge;
import com.liang.fsmhg.graph.LabeledGraph;
import com.liang.fsmhg.graph.LabeledVertex;

public class FSMHG {

    private TreeMap<Integer, PointPattern> points;
    private List<LabeledGraph> trans;
    private double minSup;
    private double absSup;
    private int maxEdgeSize;
    private boolean partition;
    private double similarity;
    private int patternCount = 0;
    private int pointCount = 0;

    private int maxVid = 0;

    private PatternWriter pw;

    private long interTime = 0;
    private long borderCheckTime = 0;
    private long getClusterTime = 0;

    public FSMHG(File out, double minSupport, int maxEdgeSize, boolean partition, double similarity) {
        this.minSup = minSupport;
        this.maxEdgeSize = maxEdgeSize;
        this.partition = partition;
        this.similarity = similarity;
        this.points = new TreeMap<>();
        this.pw = new PatternWriter(out);
    }

    public void enumerate(List<LabeledGraph> trans) {
        long startTime = System.currentTimeMillis();
        this.trans = trans;
        System.out.println("Total trans: " + this.trans.size());
        this.absSup = Math.ceil(this.trans.size() * this.minSup);

        List<Cluster> clusters;
        Map<DFSEdge, Pattern> edges;
        if (partition) {
            clusters = Cluster.partition(trans, similarity, 0);
            startTime = System.currentTimeMillis();
            int gCount = 0;
            for (Cluster c : clusters) {
                gCount += c.size();
            }
            System.out.println(gCount + " snapshots in " + clusters.size() + " clusters");
            this.points = pointsCluster(clusters);
            edges = edges(this.points, clusters);
        } else {
            this.points = points(this.trans);
            edges = edges(points);
        }
        // System.out.println(this.points.size() + " points");
        // int num = 0;
        // for (PointPattern pp : this.points.values()) {
        //     num += pp.numberOfEmbeddings();
        // }
        // System.out.println(num + " point embeddings");
        // System.out.println(edges.size() + " edges");
        // num = 0;
        // for (Pattern p : edges.values()) {
        //     num += p.numberOfEmbeddings();
        // }
        // System.out.println(num + " edge embeddings");

        for (PointPattern pp : this.points.values()) {
            if (!isFrequent(pp)) {
                continue;
            }
            this.pointCount++;
            pw.save(pp, this.patternCount++);
            for (Pattern p : pp.children()) {
                if (!isFrequent(p)) {
                    continue;
                }
                subgraphMining(trans, p);
            }
        }
        pw.close();
        System.out.println(this.pointCount + " point patterns");
        System.out.println((this.patternCount - this.pointCount) + " connected patterns.");

        long endTime = System.currentTimeMillis();
        System.out.println("Duration = " + (endTime - startTime));
        System.out.println("Intersection time = " + interTime);
        System.out.println("Border check time = " + borderCheckTime);
        System.out.println("Get cluster time = " + getClusterTime);
    }

    public TreeMap<Integer, PointPattern> pointsCluster(List<Cluster> clusters) {
        TreeMap<Integer, PointPattern> points = new TreeMap<>();
        for (Cluster c : clusters) {
            // intersectionPoints(c, points);
            // otherPoints(c, points);
            LabeledGraph inter = c.intersection();
            Map<Integer, LabeledVertex> border = c.border();
            for (LabeledVertex v : inter.vertices()) {
                PointPattern pattern = points.get(inter.vLabel(v));
                if (pattern == null) {
                    pattern = new PointPattern(inter.vLabel(v));
                }
                Embedding em = new Embedding(v, null);
                if (border.containsKey(v.id())) {
                    pattern.addBorderEmbedding(c, em);
                } else {
                    pattern.addIntersectionEmbedding(c, em);
                }
                points.putIfAbsent(pattern.label(), pattern);
                if (v.id() > maxVid) {
                    maxVid = v.id();
                }
            }

            for (LabeledGraph g : c) {
                DeltaGraph dg = c.deltaGraph(g);
                for (LabeledVertex v : dg.vertices()) {
                    long borderCheckBegin = System.currentTimeMillis();
                    if (inter.vertex(v.id()) != null) {
                        long borderCheckEnd = System.currentTimeMillis();
                        this.borderCheckTime += (borderCheckEnd - borderCheckBegin);
                        continue;
                    } else {
                        long borderCheckEnd = System.currentTimeMillis();
                        this.borderCheckTime += (borderCheckEnd - borderCheckBegin);
                    }
                    PointPattern pattern = points.get(g.vLabel(v));
                    if (pattern == null) {
                        pattern = new PointPattern(g.vLabel(v));
                    }
                    Embedding em = new Embedding(v, null);
                    pattern.addEmbedding(g, em);
                    points.putIfAbsent(pattern.label(), pattern);
                    if (v.id() > maxVid) {
                        maxVid = v.id();
                    }
                }
            }
        }
        return points;
    }

    public Map<DFSEdge, Pattern> edges(Map<Integer, PointPattern> points, List<Cluster> clusters) {
        Map<DFSEdge, Pattern> edges = new TreeMap<>();
        for (Cluster c : clusters) {
            LabeledGraph inter = c.intersection();
            Map<Integer, LabeledVertex> border = c.border();
            for (PointPattern pp : points.values()) {
                List<Embedding> embeddings = pp.intersectionEmbeddings(c);
                if (embeddings != null) {
                    for (Embedding em : embeddings) {
                        for (LabeledEdge e : inter.adjEdges(em.vertex().id())) {
                            if (inter.vLabel(e.from()) > inter.vLabel(e.to())) {
                                continue;
                            }
                            Pattern child = pp.child(0, 1, inter.vLabel(e.from()), inter.vLabel(e.to()), inter.eLabel(e));
                            if (border.containsKey(e.to().id())) {
                                child.addBorderEmbedding(c, new Embedding(e.to(), em));
                            } else {
                                child.addIntersectionEmbedding(c, new Embedding(e.to(), em));
                            }
                            edges.putIfAbsent(child.edge(), child);
                        }
                    }
                }

                embeddings = pp.borderEmbeddings(c);
                if (embeddings != null) {
                    for (Embedding em : embeddings) {
                        for (LabeledEdge e : inter.adjEdges(em.vertex().id())) {
                            if (inter.vLabel(e.from()) > inter.vLabel(e.to())) {
                                continue;
                            }
                            Pattern child = pp.child(0, 1, inter.vLabel(e.from()), inter.vLabel(e.to()), inter.eLabel(e));
                            child.addBorderEmbedding(c, new Embedding(e.to(), em));
                            edges.putIfAbsent(child.edge(), child);
                        }

                        for (LabeledGraph g : c) {
                            DeltaGraph dg = c.deltaGraph(g);
                            long borderCheckBegin = System.currentTimeMillis();
                            if (dg.vertex(em.vertex().id()) == null) {
                                long borderCheckEnd = System.currentTimeMillis();
                                this.borderCheckTime += (borderCheckEnd - borderCheckBegin);
                                continue;
                            } else {
                                long borderCheckEnd = System.currentTimeMillis();
                                this.borderCheckTime += (borderCheckEnd - borderCheckBegin);
                            }
                            for (LabeledEdge e : dg.adjEdges(em.vertex().id())) {
                                if (g.vLabel(e.from()) > g.vLabel(e.to())) {
                                    continue;
                                }
                                Pattern child = pp.child(0, 1, g.vLabel(e.from()), g.vLabel(e.to()), g.eLabel(e));
                                child.addEmbedding(g, new Embedding(e.to(), em));
                                edges.putIfAbsent(child.edge(), child);
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
                            child.addEmbedding(g, new Embedding(e.to(), em));
                            edges.putIfAbsent(child.edge(), child);
                        }
                    }
                }
            }
        }
        return edges;
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
                        eMap.putIfAbsent(child.edge(), child);
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
        pw.save(parent, this.patternCount++);
        if (parent.code().edgeSize() >= maxEdgeSize) {
            return;
        }

        enumerateChildren(parent);
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

        long clusterBegin = System.currentTimeMillis();
        List<Cluster> clusters = p.clusters();
        long clusterEnd = System.currentTimeMillis();
        this.getClusterTime += (clusterEnd - clusterBegin);
        for (Cluster c : clusters) {
            joinExtendInter(c, p, joinBackCands, joinForCands, extendCands);
            joinExtendDelta(c, p, joinBackCands, joinForCands, extendCands);
        }
        // for (LabeledGraph g : p.unClusteredGraphs()) {
        for (LabeledGraph g : p.graphs()) {
            joinExtendOther(g, p, joinBackCands, joinForCands, extendCands);
        }

        return new ArrayList<>(children.values());
    }

    private void joinExtendInter(Cluster c, Pattern p, TreeMap<Integer, TreeSet<DFSEdge>> backCand, TreeMap<Integer, TreeSet<DFSEdge>> forCand, TreeSet<DFSEdge> extendCands) {
        LabeledGraph inter = c.intersection();
        List<Embedding> interEmbeddings = p.intersectionEmbeddings(c);
        // List<Embedding> borderEmbeddings = p.borderEmbeddings(c);
        Map<Integer, LabeledVertex> border = c.border();
        DFSCode code = p.code();
        List<Integer> rmPathIds = code.rightMostPath();
        int rmDfsId = rmPathIds.get(rmPathIds.size() - 1);

        // for (int interEmCount = 0, borderEmCount = 0 - interEmbeddings.size(); interEmCount < interEmbeddings.size() || borderEmCount < borderEmbeddings.size(); interEmCount++, borderEmCount++) {
        for (int interEmCount = 0; interEmCount < interEmbeddings.size(); interEmCount++) {
            Embedding em = interEmbeddings.get(interEmCount);
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

                // for (LabeledGraph g : c) {
                //     DeltaGraph dg = c.deltaGraph(g);
                //     if (dg.vertex(from.id()) == null || dg.vertex(to.id()) == null) {
                //         continue;
                //     }
                //     back = dg.edge(from.id(), to.id());
                //     if (back == null) {
                //         continue;
                //     }
                //     DFSEdge dfsEdge = new DFSEdge(rmDfsId, entry.getKey(), g.vLabel(from), g.vLabel(to), g.eLabel(back));
                //     if (cands.contains(dfsEdge)) {
                //         Pattern child = p.child(dfsEdge);
                //         child.addEmbedding(g, em);
                //     }
                // }
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
                        if (border.containsKey(e.to().id())) {
                            child.addBorderEmbedding(c, new Embedding(e.to(), em));
                        } else {
                            child.addIntersectionEmbedding(c, new Embedding(e.to(), em));
                        }
                    }
                }

                // for (LabeledGraph g : c) {
                //     DeltaGraph dg = c.deltaGraph(g);
                //     if (dg.vertex(from.id()) == null) {
                //         continue;
                //     }
                //     for (LabeledEdge e : dg.adjEdges(from.id())) {
                //         if (emBits.get(e.to().id())) {
                //             continue;
                //         }
                //         DFSEdge dfsEdge = new DFSEdge(entry.getKey(), emVertices.size(), g.vLabel(from), g.vLabel(e.to()), g.eLabel(e));
                //         if (cands.contains(dfsEdge)) {
                //             Pattern child = p.child(dfsEdge);
                //             child.addEmbedding(g, new Embedding(e.to(), em));
                //         }
                //     }
                // }
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

                // for (LabeledGraph g : c) {
                //     DeltaGraph dg = c.deltaGraph(g);
                //     if (dg.vertex(from.id()) == null || dg.vertex(to.id()) == null) {
                //         continue;
                //     }
                //     back = dg.edge(from.id(), to.id());
                //     if (back == null) {
                //         continue;
                //     }
                //     if (g.eLabel(pathEdge) > g.eLabel(back) || (g.eLabel(pathEdge) == g.eLabel(back) && g.vLabel(nextTo) > g.vLabel(back.from()))) {
                //         continue;
                //     }
    
                //     DFSEdge dfsEdge;
                //     if (g.vLabel(from) <= g.vLabel(to)) {
                //         dfsEdge = new DFSEdge(0, 1, g.vLabel(from), g.vLabel(to), g.eLabel(back));
                //     } else {
                //         dfsEdge = new DFSEdge(0, 1, g.vLabel(to), g.vLabel(from), g.eLabel(back));
                //     }
                //     if (extendCands.contains(dfsEdge)) {
                //         Pattern child = p.child(rmDfsId, toId, g.vLabel(from), g.vLabel(to), g.eLabel(back));
                //         child.addEmbedding(g, em);
                //     }
                // }
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
                    if (border.containsKey(to.id())) {
                        child.addBorderEmbedding(c, new Embedding(e.to(), em));
                    } else {
                        child.addIntersectionEmbedding(c, new Embedding(e.to(), em));
                    }
                }
            }

            // for (LabeledGraph g : c) {
            //     DeltaGraph dg = c.deltaGraph(g);
            //     if (dg.vertex(from.id()) == null) {
            //         continue;
            //     }
            //     for (LabeledEdge e : dg.adjEdges(from.id())) {
            //         LabeledVertex to = e.to();
            //         if (emBits.get(to.id())) {
            //             continue;
            //         }
            //         DFSEdge dfsEdge;
            //         if (g.vLabel(from) <= g.vLabel(to)) {
            //             dfsEdge = new DFSEdge(0, 1, g.vLabel(from), g.vLabel(to), g.eLabel(e));
            //         } else {
            //             dfsEdge = new DFSEdge(0, 1, g.vLabel(to), g.vLabel(from), g.eLabel(e));
            //         }
            //         if (extendCands.contains(dfsEdge)) {
            //             Pattern child = p.child(rmDfsId, emVertices.size(), g.vLabel(from), g.vLabel(to), g.eLabel(e));;
            //             child.addEmbedding(g, new Embedding(e.to(), em));
            //         }
            //     }
            // }
        }
    }

    private void joinExtendDelta(Cluster c, Pattern p, TreeMap<Integer, TreeSet<DFSEdge>> backCand, TreeMap<Integer, TreeSet<DFSEdge>> forCand, TreeSet<DFSEdge> extendCands) {
        List<Embedding> embeddings = p.borderEmbeddings(c);
        if (embeddings == null) {
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
                        child.addBorderEmbedding(c, em);
                    }
                }

                for (LabeledGraph g : c) {
                    DeltaGraph dg = c.deltaGraph(g);
                    long borderCheckBegin = System.currentTimeMillis();
                    if (dg.vertex(from.id()) == null || dg.vertex(to.id()) == null) {
                        long borderCheckEnd = System.currentTimeMillis();
                        this.borderCheckTime += (borderCheckEnd - borderCheckBegin);
                        continue;
                    } else {
                        long borderCheckEnd = System.currentTimeMillis();
                        this.borderCheckTime += (borderCheckEnd - borderCheckBegin);
                    }
                    back = dg.edge(from.id(), to.id());
                    if (back == null) {
                        continue;
                    }
                    DFSEdge dfsEdge = new DFSEdge(rmDfsId, entry.getKey(), g.vLabel(from), g.vLabel(to), g.eLabel(back));
                    if (cands.contains(dfsEdge)) {
                        Pattern child = p.child(dfsEdge);
                        child.addEmbedding(g, em);
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
                        child.addBorderEmbedding(c, new Embedding(e.to(), em));
                    }
                }

                for (LabeledGraph g : c) {
                    DeltaGraph dg = c.deltaGraph(g);
                    long borderCheckBegin = System.currentTimeMillis();
                    if (dg.vertex(from.id()) == null) {
                        long borderCheckEnd = System.currentTimeMillis();
                        this.borderCheckTime += (borderCheckEnd - borderCheckBegin);
                        continue;
                    } else {
                        long borderCheckEnd = System.currentTimeMillis();
                        this.borderCheckTime += (borderCheckEnd - borderCheckBegin);
                    }
                    for (LabeledEdge e : dg.adjEdges(from.id())) {
                        if (emBits.get(e.to().id())) {
                            continue;
                        }
                        DFSEdge dfsEdge = new DFSEdge(entry.getKey(), emVertices.size(), g.vLabel(from), g.vLabel(e.to()), g.eLabel(e));
                        if (cands.contains(dfsEdge)) {
                            Pattern child = p.child(dfsEdge);
                            child.addEmbedding(g, new Embedding(e.to(), em));
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
                        child.addBorderEmbedding(c, em);
                    }
                }

                for (LabeledGraph g : c) {
                    DeltaGraph dg = c.deltaGraph(g);
                    long borderCheckBegin = System.currentTimeMillis();
                    if (dg.vertex(from.id()) == null || dg.vertex(to.id()) == null) {
                        long borderCheckEnd = System.currentTimeMillis();
                        this.borderCheckTime += (borderCheckEnd - borderCheckBegin);
                        continue;
                    } else {
                        long borderCheckEnd = System.currentTimeMillis();
                        this.borderCheckTime += (borderCheckEnd - borderCheckBegin);
                    }
                    back = dg.edge(from.id(), to.id());
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
                        child.addEmbedding(g, em);
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
                    child.addBorderEmbedding(c, new Embedding(e.to(), em));
                }
            }

            for (LabeledGraph g : c) {
                DeltaGraph dg = c.deltaGraph(g);
                long borderCheckBegin = System.currentTimeMillis();
                if (dg.vertex(from.id()) == null) {
                    long borderCheckEnd = System.currentTimeMillis();
                    this.borderCheckTime += (borderCheckEnd - borderCheckBegin);
                    continue;
                } else {
                    long borderCheckEnd = System.currentTimeMillis();
                    this.borderCheckTime += (borderCheckEnd - borderCheckBegin);
                }
                for (LabeledEdge e : dg.adjEdges(from.id())) {
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
                        child.addEmbedding(g, new Embedding(e.to(), em));
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
