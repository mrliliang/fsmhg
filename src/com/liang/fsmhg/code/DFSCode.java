package com.liang.fsmhg.code;


import com.liang.fsmhg.Embedding;
import com.liang.fsmhg.Pattern;
import com.liang.fsmhg.graph.*;

import java.util.*;

public class DFSCode implements Comparable<DFSCode> {

    private int nodeCount;

    private ArrayList<DFSEdge> edges;

    public DFSCode() {
        edges = new ArrayList<>();
    }

    public DFSCode(DFSCode parent, DFSEdge expandedEdge) {
        edges = new ArrayList<>();
        if (parent != null) {
            edges.addAll(parent.edges);
        }
        edges.add(expandedEdge);

        if (parent == null) {
            nodeCount = 2;
        } else {
            nodeCount = Math.max(parent.nodeCount(), Math.max(expandedEdge.from(), expandedEdge.to()) + 1);
        }
    }

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

        List<Embedding> embeddings = new ArrayList<>(edges.size());
        Embedding em;
        for (LabeledVertex v : pg.vertices()) {
            if (pg.vLabel(v) != edge.fromLabel()) {
                continue;
            }
            em = new Embedding(v, null);
            for (LabeledEdge e : pg.adjEdges(v.id())) {
                if (pg.eLabel(e) == edge.edgeLabel() && pg.vLabel(e.to()) == edge.toLabel()) {
                    embeddings.add(new Embedding(e.to(), em));
                }
            }
        }

        DFSCode subCode = new DFSCode();
        subCode.add(edge);
        LabeledGraph subPatternGraph = subCode.toGraph();
        TreeMap<DFSEdge, List<Embedding>> map;
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

        map = forwardEdge(patternGraph, subPatternGraph, embeddings, rmPath);
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

    private TreeMap<DFSEdge, List<Embedding>> forwardEdge(LabeledGraph patternGraph, LabeledGraph subPatternGraph, List<Embedding> embeddings, List<Integer> rmPath) {
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


    private boolean isNextEdgeMin(Graph g, Pattern p, int count) {
        if (count == edges.size()) {
            return true;
        }

//        ArrayList<Integer> rmPath = p.getDfsCode().rightMostPath();

//        TreeMap<DFSEdge, Pattern> patternMap = new TreeMap<>();
//
//        p.expandBackward(0, g, p.embeddings(0), rmPath, patternMap);
//        Map.Entry<DFSEdge, Pattern> entry = patternMap.firstEntry();
//        DFSEdge edge = entry != null ? patternMap.firstKey() : null;
//        if (edge != null) {
//            if (edges.get(count).equals(edge)) {
//                return isNextEdgeMin(g, patternMap.firstEntry().getValue(), count + 1);
//            } else {
//                return false;
//            }
//        }
//
//        patternMap.clear();
//        p.expandRmVertexForward(0, g, p.embeddings(0), rmPath, patternMap);
//        entry = patternMap.firstEntry();
//        edge = entry != null ? patternMap.firstKey() : null;
//        if (edge != null) {
//            if (edges.get(count).equals(edge)) {
//                return isNextEdgeMin(g, patternMap.firstEntry().getValue(), count + 1);
//            } else {
//                return false;
//            }
//        }
//
//        patternMap.clear();
//        p.expandRmPathForward(0, g, p.embeddings(0), rmPath, patternMap);
//        entry = patternMap.firstEntry();
//        edge = entry != null ? patternMap.firstKey() : null;
//        if (edge != null && edges.get(count).equals(edge)) {
//            if (edges.get(count).equals(edge)) {
//                return isNextEdgeMin(g, patternMap.firstEntry().getValue(), count + 1);
//            } else {
//                return false;
//            }
//        }



//        Pattern expand = p.minBackwardPattern(0, g, p.embeddings(0), rmPath);
//        if (expand != null) {
//            DFSEdge dfsEdge = expand.getDfsCode().lastEdge();
//            if (edges.get(count).equals(dfsEdge)) {
//                return isNextEdgeMin(g, expand, count + 1);
//            } else {
//                return false;
//            }
//        }
//
//        expand = p.minRmVertextForward(0, g, p.embeddings(0), rmPath);
//        if (expand != null) {
//            DFSEdge dfsEdge = expand.getDfsCode().lastEdge();
//            if (edges.get(count).equals(dfsEdge)) {
//                return isNextEdgeMin(g, expand, count + 1);
//            } else {
//                return false;
//            }
//        }
//
//        expand = p.minRmPathForward(0, g, p.embeddings(0), rmPath);
//        if (expand != null) {
//            DFSEdge dfsEdge = expand.getDfsCode().lastEdge();
//            if (edges.get(count).equals(dfsEdge)) {
//                return isNextEdgeMin(g, expand, count + 1);
//            } else {
//                return false;
//            }
//        }

        return false;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (DFSEdge e : edges) {
            builder.append(e.toString());
        }
        return builder.toString();
    }

//    public boolean isMinNew() {
        // TODO: 2019/11/1 最小编码验证需要修改
//        if (edges.size() == 1) {
//            DFSEdge edge = edges.get(0);
//            return edge.from() == 0 && edge.to() == 1 && edge.fromLabel() <= edge.toLabel();
//        }
//
//        Graph g = toGraph();
//
//        DFSEdge minDfsEdge = null;
//        Node minNode = null;
//        for (Vertex v : g.vertices()) {
//            for (Edge e : g.adjEdges(v.id())) {
//                Vertex from = e.from();
//                Vertex to = e.to();
//                if (from.label() > to.label()) {
//                    continue;
//                }
//
//                DFSEdge dfsEdge = new DFSEdge(0, 1, from.label(), to.label(), e.label());
//                if (dfsEdge.equals(minDfsEdge)) {
//                    minNode.addEmbedding(0, e, null);
//                } else if (minDfsEdge == null || minDfsEdge.compareTo(dfsEdge) > 0) {
//                    minDfsEdge = dfsEdge;
//                    minNode = new Node(minDfsEdge, null);
//                    minNode.addEmbedding(0, e, null);
//                }
//            }
//        }
//
//        if (!edges.get(0).equals(minDfsEdge)) {
//            return false;
//        }
//
//        return isNextEdgeMinNew(g, minNode, 1);
//    }

//    private boolean isNextEdgeMinNew(Graph g, Node node, int count) {
//        if (count == edges.size()) {
//            return true;
//        }
//
//        ArrayList<Integer> rmPath = node.dfsCode().rightMostPath();
//
//        Node expand = minBackwardPattern(node, g, node.embeddings(0), rmPath);
//        if (expand != null) {
//            DFSEdge dfsEdge = expand.dfsCode().lastEdge();
//            if (edges.get(count).equals(dfsEdge)) {
//                return isNextEdgeMinNew(g, expand, count + 1);
//            } else {
//                return false;
//            }
//        }
//
//        expand = minRmVertextForward(node, g, node.embeddings(0), rmPath);
//        if (expand != null) {
//            DFSEdge dfsEdge = expand.dfsCode().lastEdge();
//            if (edges.get(count).equals(dfsEdge)) {
//                return isNextEdgeMinNew(g, expand, count + 1);
//            } else {
//                return false;
//            }
//        }
//
//        expand = minRmPathForward(node, g, node.embeddings(0), rmPath);
//        if (expand != null) {
//            DFSEdge dfsEdge = expand.dfsCode().lastEdge();
//            if (edges.get(count).equals(dfsEdge)) {
//                return isNextEdgeMinNew(g, expand, count + 1);
//            } else {
//                return false;
//            }
//        }
//
//        return false;
//    }

//    private Node minBackwardPattern(Node parent, Graph graph, List<Embedding> embeddings, List<Integer> rmPath) {
//        Node minNode = null;
//        DFSCode dfsCode = parent.dfsCode();
//        int dfsFrom = dfsCode.nodeCount() - 1;
//        for (int i : rmPath) {
//            boolean flag = false;
//
//            int dfsTo = dfsCode.get(i).from();
//
//            for (Embedding embedding : embeddings) {
//                Embedding.Visitor visitor = embedding.visitor();
//                Edge rmEdge = visitor.getEdge(rmPath.get(rmPath.size() - 1));
//
//                Edge backEdge = parent.getBackward(graph, visitor.getEdge(i), rmEdge, visitor);
//                if (backEdge == null) {
//                    continue;
//                }
//
//                flag = true;
//
//                minNode = updateMinPattern(dfsFrom, dfsTo, backEdge, embedding, parent, minNode);
//            }
//
//            if (flag) {
//                break;
//            }
//        }
//
//        return minNode;
//    }

//    public Node minRmVertextForward(Node parent, Graph graph, List<Embedding> embeddings, List<Integer> rmPath) {
//        Node minNode = null;
//
//        DFSCode dfsCode = parent.dfsCode();
//        int dfsFrom = dfsCode.nodeCount() - 1;
//        for (Embedding embedding : embeddings) {
//            Embedding.Visitor visitor = embedding.visitor();
//            Edge rmEdge = visitor.getEdge(rmPath.get(rmPath.size() - 1));
//
//            List<Edge> forwardEdges = parent.getRmVertexForward(graph, visitor.getEdge(0), rmEdge, visitor);
//            for (Edge e : forwardEdges) {
//                minNode = updateMinPattern(dfsFrom, dfsFrom + 1, e, embedding, parent, minNode);
//            }
//        }
//
//        return minNode;
//    }


//    public Node minRmPathForward(Node parent, Graph graph, List<Embedding> embeddings, List<Integer> rmPath) {
//        Node minNode = null;
//        Comparator comparator = new RmPathForwardComparator();
//        DFSCode dfsCode = parent.dfsCode();
//        int dfsTo = dfsCode.nodeCount();
//        for (int j = rmPath.size() - 1; j >= 0; j--) {
//            boolean flag = false;
//
//            int i = rmPath.get(j);
//            int dfsFrom = dfsCode.get(i).from();
//
//            for (Embedding embedding : embeddings) {
//                Embedding.Visitor visitor = embedding.visitor();
//                List<Edge> forwardEdges = parent.getRmPathForward(graph, visitor.getEdge(0), visitor.getEdge(i), visitor);
//                if (forwardEdges != null && !forwardEdges.isEmpty()) {
//                    flag = true;
//                }
//                for (Edge e : forwardEdges) {
//                    minNode = updateMinPattern(dfsFrom, dfsTo, e, embedding, parent, minNode);
//                }
//            }
//
//            if (flag) {
//                break;
//            }
//        }
//
//        return minNode;
//    }

//    private Node updateMinPattern(int dfsFrom, int dfsTo, Edge e, Embedding embedding, Node parent, Node minNode) {
//        DFSEdge minDfsEdge = minNode == null ? null : minNode.dfsEdge();
//        DFSEdge dfsEdge = new DFSEdge(dfsFrom, dfsTo, e.from().label(), e.to().label(), e.label());
//        if (dfsEdge.equals(minDfsEdge)) {
//            minNode.addEmbedding(0, e, embedding);
//        } else if (minDfsEdge == null || minDfsEdge.compareTo(dfsEdge) > 0) {
//            minNode = new Node(dfsEdge, parent);
//            minNode.addEmbedding(0, e, embedding);
//        }
//
//        return minNode;
//    }

}
