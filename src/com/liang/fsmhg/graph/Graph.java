package com.liang.fsmhg.graph;

import java.util.List;

public class Graph extends AGraph<Vertex, Edge> {
    public Graph(long id) {
        super(id);
    }

    public Graph(long id, List<Vertex> vertices, List<Edge> edges) {
        super(id, vertices, edges);
    }
}
