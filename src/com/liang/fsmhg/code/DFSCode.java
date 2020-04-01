package com.liang.fsmhg.code;


import com.liang.fsmhg.Embedding;
import com.liang.fsmhg.graph.*;

import java.util.*;

public class DFSCode implements Comparable<DFSCode> {

    private int nodeCount;

    private ArrayList<DFSEdge> edges;

    public DFSCode() {
        edges = new ArrayList<>();
    }

//    public DFSCode(DFSCode parent, DFSEdge expandedEdge) {
//        edges = new ArrayList<>();
//        if (parent != null) {
//            edges.addAll(parent.edges);
//        }
//        edges.add(expandedEdge);
//
//        if (parent == null) {
//            nodeCount = 2;
//        } else {
//            nodeCount = Math.max(parent.nodeCount(), Math.max(expandedEdge.from(), expandedEdge.to()) + 1);
//        }
//    }

    public void add(DFSEdge edge) {
        edges.add(edge);
        nodeCount = Math.max(nodeCount, Math.max(edge.from(), edge.to()) + 1);
    }

    public ArrayList<DFSEdge> edges() {
        return edges;
    }

    public DFSEdge get(int i) {
        return edges.get(i);
    }

    public int edgeSize() {
        return edges.size();
    }

    public DFSEdge lastEdge() {
        return edges.get(edges.size() - 1);
    }

    public int nodeCount() {
        return nodeCount;
    }

    public LabeledGraph toGraph() {
        // TODO: 2020/3/29 Convert DFS code to graph
        LabeledGraph g = new StaticGraph();

        for (DFSEdge edge : edges) {
            int id = edge.from();
            LabeledVertex from = g.vertex(id);
            if (from == null) {
                from = g.addVertex(id, edge.fromLabel());
            }

            id = edge.to();
            LabeledVertex to = g.vertex(id);
            if (to == null) {
                to = g.addVertex(id, edge.toLabel());
            }

            g.addEdge(from.id(), to.id(), edge.edgeLabel());
            g.addEdge(to.id(), from.id(), edge.edgeLabel());
        }

        return g;
    }

    // TODO: 2020/3/28 Need correct right most path
    public ArrayList<Integer> rightMostPath() {
        ArrayList<Integer> rmPath = new ArrayList<>();
        long oldFrom = -1;

        for (int i = edges.size() - 1; i >= 0; i--) {
            DFSEdge edge = edges.get(i);
            if (edge.isForward() && (rmPath.isEmpty() || oldFrom == edge.to())) {
                rmPath.add(edge.to());
                oldFrom = edge.from();
            }
        }
        rmPath.add(0);

        Collections.reverse(rmPath);

        return rmPath;
    }

    @Override
    public int compareTo(DFSCode other) {
        if (this == other) {
            return 0;
        }
        int min = Math.min(edges.size(), other.edges.size());
        for (int i = 0; i < min; i++) {
            DFSEdge e1 = this.edges.get(i);
            DFSEdge e2 = other.edges.get(i);
            int result = e1.compareTo(e2);
            if (result == 0) {
                continue;
            }
            return result;
        }
        return this.edges.size() - other.edges.size();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof DFSCode) {
            DFSCode other = (DFSCode)obj;
            int min = Math.min(edges.size(), other.edges.size());
            for (int i = 0; i < min; i++) {
                DFSEdge e1 = this.edges.get(i);
                DFSEdge e2 = other.edges.get(i);
                if (!e1.equals(e2)) {
                    return false;
                }
            }

            return this.edges.size() == other.edges.size();
        }

        return false;
    }


    public boolean isMin() {
        // TODO: 2020/3/27 need to check min DFS code
        DFSEdge edge = edges.get(0);
        if (edges.size() == 1) {
            return edge.from() == 0 && edge.to() == 1 && edge.fromLabel() <= edge.toLabel();
        }

        LabeledGraph pg = toGraph();

        TreeMap<DFSEdge, List<Embedding>> map = firstEdge(pg);
        edge = map.firstKey();
        if (!edge.equals(edges.get(0))) {
            return false;
        }

        List<Embedding> embeddings = map.get(edge);
        DFSCode subCode = new DFSCode();
        subCode.add(edge);
        LabeledGraph subPatternGraph = subCode.toGraph();
        for (int i = 1; i < edges.size(); i++) {
            List<Integer> rmpath = rmPath(i);
            map = nextEdge(pg, subPatternGraph, embeddings, rmpath);
            Map.Entry<DFSEdge, List<Embedding>> entry = map.firstEntry();
            edge = entry.getKey();
            embeddings = entry.getValue();
            if (edge != null && !edges.get(i).equals(edge)) {
                return false;
            }
            if (subPatternGraph.vertex(edge.from()) == null) {
                subPatternGraph.addVertex(edge.from(), edge.fromLabel());
            }
            if (subPatternGraph.vertex(edge.to()) == null) {
                subPatternGraph.addVertex(edge.to(), edge.toLabel());
            }
            subPatternGraph.addEdge(edge.from(), edge.to(), edge.edgeLabel());
            subPatternGraph.addEdge(edge.to(), edge.from(), edge.edgeLabel());
        }

        return true;
    }


    private TreeMap<DFSEdge, List<Embedding>> firstEdge(LabeledGraph pg) {
        TreeMap<Integer, List<Embedding>> vMap = new TreeMap<>();
        for (LabeledVertex v : pg.vertices()) {
            List<Embedding> ems = vMap.get(pg.vLabel(v));
            if (ems == null) {
                ems = new ArrayList<>();
                vMap.put(pg.vLabel(v), ems);
            }
            ems.add(new Embedding(v, null));
        }

        TreeMap<DFSEdge, List<Embedding>> eMap = new TreeMap<>();
        for (Embedding em : vMap.firstEntry().getValue()) {
            LabeledVertex v = em.vertex();
            LabeledEdge minEdge = null;
            for (LabeledEdge e : pg.adjEdges(v.id())) {
                if (pg.vLabel(e.from()) < pg.vLabel(e.to())) {
                    continue;
                }
                if (minEdge == null) {
                    minEdge = e;
                } else if (pg.eLabel(e) < pg.eLabel(minEdge) ||
                        (pg.eLabel(e) == pg.eLabel(minEdge) &&
                                pg.vLabel(e.to()) <= pg.vLabel(minEdge.to()))) {
                    minEdge = e;
                }
            }
            if (minEdge != null) {
                DFSEdge dfsEdge = new DFSEdge(0, 1, pg.vLabel(minEdge.from()), pg.vLabel(minEdge.to()), pg.eLabel(minEdge));
                List<Embedding> embeddingList = eMap.get(dfsEdge);
                if (embeddingList == null) {
                    embeddingList = new ArrayList<>();
                    eMap.put(dfsEdge, embeddingList);
                }
                embeddingList.add(new Embedding(minEdge.to(), em));
            }
        }

        return eMap;
    }

    private List<Integer> rmPath(int index) {
        DFSCode code = new DFSCode();
        for (int i = 0; i <= index; i++) {
            code.add(edges.get(i));
        }
        return code.rightMostPath();
    }

    private TreeMap<DFSEdge, List<Embedding>> nextEdge(LabeledGraph patternGraph, LabeledGraph subPatternGraph, List<Embedding> embeddings, List<Integer> rmPath) {
        // TODO: 2020/3/29 next edge
        TreeMap<DFSEdge, List<Embedding>> map = backwardEdge(patternGraph, subPatternGraph, embeddings, rmPath);
        if (map != null && !map.isEmpty()) {
            return map;
        }

        map = forwardEdge(patternGraph, embeddings, rmPath);
        return map;
    }

    private TreeMap<DFSEdge, List<Embedding>> backwardEdge(LabeledGraph patternGraph, LabeledGraph subPatternGraph, List<Embedding> embeddings, List<Integer> rmPath) {
        TreeMap<DFSEdge, List<Embedding>> map = new TreeMap<>();
        for (Embedding em : embeddings) {
            List<LabeledVertex> emVertices = em.vertices();
            LabeledVertex rmVertex = emVertices.get(rmPath.get(rmPath.size() - 1));
            for (int i : rmPath) {
                LabeledVertex pathVertex = emVertices.get(rmPath.get(i));
                if (subPatternGraph.edge(rmVertex.id(), pathVertex.id()) != null) {
                    continue;
                }

                LabeledEdge back = patternGraph.edge(rmVertex.id(), pathVertex.id());
                if (back == null) {
                    continue;
                }
                DFSEdge dfsEdge = new DFSEdge(rmVertex.id(), pathVertex.id(), patternGraph.vLabel(rmVertex), patternGraph.vLabel(pathVertex), patternGraph.eLabel(back));
                List<Embedding> embeddingList = map.get(dfsEdge);
                if (embeddingList == null) {
                    embeddingList = new ArrayList<>();
                    map.put(dfsEdge, embeddingList);
                }
                embeddingList.add(em);
                break;
            }
        }
        return map;
    }

    private TreeMap<DFSEdge, List<Embedding>> forwardEdge(LabeledGraph patternGraph, List<Embedding> embeddings, List<Integer> rmPath) {
        TreeMap<DFSEdge, List<Embedding>> map = new TreeMap<>();
        for (Embedding em : embeddings) {
            List<LabeledVertex> emVertices = em.vertices();
            BitSet emBits = new BitSet();
            for (LabeledVertex v : emVertices) {
                emBits.set(v.id());
            }
            LabeledVertex rmVertex = emVertices.get(rmPath.get(rmPath.size() - 1));

            //Forward edge on right most vertex
            LabeledEdge minRmForward = null;
            for (LabeledEdge e : patternGraph.adjEdges(rmVertex.id())) {
                if (emBits.get(e.to().id())) {
                    continue;
                }
                if (minRmForward == null) {
                    minRmForward = e;
                } else if (patternGraph.eLabel(e) < patternGraph.eLabel(minRmForward) ||
                        (patternGraph.eLabel(e) == patternGraph.eLabel(minRmForward) &&
                                patternGraph.vLabel(e.to()) <= patternGraph.vLabel(minRmForward.to()))) {
                    minRmForward = e;
                }
            }
            if (minRmForward != null) {
                DFSEdge dfsEdge = new DFSEdge(rmVertex.id(), rmVertex.id() + 1, patternGraph.vLabel(rmVertex), patternGraph.vLabel(minRmForward.to()), patternGraph.eLabel(minRmForward));
                List<Embedding> embeddingList = map.get(dfsEdge);
                if (embeddingList == null) {
                    embeddingList = new ArrayList<>();
                    map.put(dfsEdge, embeddingList);
                }
                embeddingList.add(new Embedding(minRmForward.to(), em));
                continue;
            }


            //Forward edge on right most path
            for (int i = rmPath.get(rmPath.size() - 2); i >= 0; i--) {
                LabeledVertex pathVertex = emVertices.get(rmPath.get(i));

                LabeledEdge minPathForward = null;
                for (LabeledEdge e : patternGraph.adjEdges(pathVertex.id())) {
                    if (emBits.get(e.to().id())) {
                        continue;
                    }
                    if (minPathForward == null) {
                        minPathForward = e;
                    }
                    if (patternGraph.eLabel(e) < patternGraph.eLabel(minRmForward) ||
                            (patternGraph.eLabel(e) == patternGraph.eLabel(minRmForward) &&
                                    patternGraph.vLabel(e.to()) <= patternGraph.vLabel(minRmForward.to()))) {
                        minRmForward = e;
                    }
                }
                if (minRmForward != null) {
                    DFSEdge dfsEdge = new DFSEdge(rmVertex.id(), rmVertex.id() + 1, patternGraph.vLabel(rmVertex), patternGraph.vLabel(minRmForward.to()), patternGraph.eLabel(minRmForward));
                    List<Embedding> embeddingList = map.get(dfsEdge);
                    if (embeddingList == null) {
                        embeddingList = new ArrayList<>();
                        map.put(dfsEdge, embeddingList);
                    }
                    embeddingList.add(new Embedding(minRmForward.to(), em));
                    continue;
                }
            }
        }
        return map;
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (DFSEdge e : edges) {
            builder.append(e.toString());
        }
        return builder.toString();
    }

}
