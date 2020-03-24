package com.liang.fsmhg.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Graph {
//    private long graphId;
//
//    private Map<Integer, Vertex> vertices;
//    private Map<Integer, AdjEdges<Edge>> adjLists;
//
//    public Graph() {
//        this(0);
//    }
//
//    public Graph(long id) {
//        this(id, null, null);
//    }
//
//    public Graph(long id, List<? extends Vertex> vertices, List<? extends Edge> edges) {
//        this.graphId = id;
//        this.vertices = new HashMap<>();
//        this.adjLists = new HashMap<>();
//
//        if (vertices != null) {
//            for (Vertex v : vertices) {
//                addVertex(v);
//            }
//        }
//
//        if (edges != null) {
//            for (Edge e : edges) {
//                addEdge(e);
//            }
//        }
//    }
//
//    public Vertex addVertex(int id) {
//        Vertex v = new Vertex(id);
//        addVertex(v);
//        return v;
//    }
//
//    public Edge addEdge(int from, int to) {
//        Edge e = new Edge(vertex(from), vertex(to));
//        addEdge(e);
//        return e;
//    }
//
//    public long graphId() {
//        return graphId;
//    }
//
//    protected void addVertex(Vertex v) {
//        vertices.put(v.id(), v);
//        adjLists.put(v.id(), new AdjEdges<>());
//    }
//
//    protected void addEdge(Edge e) {
//        adjLists.get(e.from().id()).add(e);
//    }
//
//    public List<? extends Vertex> vertices() {
//        return new ArrayList<>(vertices.values());
//    }
//
//    public List<? extends Edge> edges() {
//        List<Edge> edges = new ArrayList<>();
//        for (AdjEdges<Edge> adjEdges : adjLists.values()) {
//            edges.addAll(adjEdges.edges());
//        }
//        return edges;
//    }
//
//    public Vertex vertex(int id) {
//        return vertices.get(id);
//    }
//
//    public Edge edge(int from, int to) {
//        return adjLists.get(from).edgeTo(to);
//    }
//
//    public AdjEdges<? extends Edge> adjEdges(int vertexId) {
//        return adjLists.get(vertexId);
//    }
//
//    public int vSize() {
//        return vertices.size();
//    }
//
//    public int eSize() {
//        int size = 0;
//        for (AdjEdges<Edge> adjEdges : adjLists.values()) {
//            size += adjEdges.size();
//        }
//        return size / 2;
//    }
//
//    @Override
//    public String toString() {
//        StringBuilder builder = new StringBuilder();
//        for (AdjEdges<Edge> adj : adjLists.values()) {
//            for (Edge e : adj) {
//                builder.append(e.from().id())
//                        .append(" ")
//                        .append(e.to().id())
//                        .append('\n');
//            }
//        }
//        return builder.toString();
//    }
}
