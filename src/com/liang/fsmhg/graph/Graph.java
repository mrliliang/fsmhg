package com.liang.fsmhg.graph;

import java.util.List;

public class Graph extends AGraph<Vertex, Edge> {
    public Graph(long id) {
        super(id);
    }

    public Graph(long id, List<Vertex> vertices, List<Edge> edges) {
        super(id, vertices, edges);
    }

    public Vertex addVertex(int id) {
        Vertex v = new Vertex(id);
        addVertex(v);
        return null;
    }

    public Edge addEdge(int from, int to) {
        Edge e = new Edge(vertex(from), vertex(to));
        addEdge(e);
        return e;
    }
}
