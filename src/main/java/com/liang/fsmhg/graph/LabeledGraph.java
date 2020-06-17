package com.liang.fsmhg.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import com.liang.fsmhg.Cluster;

public abstract class LabeledGraph implements Comparable<LabeledGraph> {
    private long graphId;
    private Cluster cluster;

    private Map<Integer, LabeledVertex> vertices;
    private Map<Integer, AdjEdges> adjLists;


    public LabeledGraph(long id) {
        this(id, null, null);
    }

    public LabeledGraph(long id, Collection<? extends LabeledVertex> vertices, Collection<? extends LabeledEdge> edges) {
        this.graphId = id;
        this.vertices = new HashMap<>();
        this.adjLists = new HashMap<>();

        if (vertices != null) {
            for (LabeledVertex v : vertices) {
                addVertex(v);
            }
        }

        if (edges != null) {
            for (LabeledEdge e : edges) {
                addEdge(e);
            }
        }
    }

    // public LabeledGraph(long id, Map<Integer, LabeledVertex> vertices, Map<Integer, AdjEdges> adjLists) {
    //     this.graphId = id;
    //     this.vertices = vertices;
    //     this.adjLists = adjLists;
    // }

    public void setId(long graphId) {
        this.graphId = graphId;
    }

    public void setCluster(Cluster c) {
        this.cluster = c;
    }

    public Cluster getCluster() {
        return this.cluster;
    }

    public void setVertices(Map<Integer, LabeledVertex> vertices) {
        this.vertices = vertices;
    }

    public void setEdges(Map<Integer, AdjEdges> adjLists) {
        this.adjLists = adjLists;
    }

    public long graphId() {
        return this.graphId;
    }

    public abstract int vLabel(LabeledVertex v);

    public abstract int eLabel(LabeledEdge e);

    public abstract LabeledVertex addVertex(int id, int label);

    public abstract LabeledEdge addEdge(int from, int to, int eLabel);

    public void addVertex(LabeledVertex v) {
        vertices.put(v.id(), v);
        adjLists.put(v.id(), new AdjEdges());
    }

    public void addVertexIfAbsent(LabeledVertex v) {
        vertices.computeIfAbsent(v.id(), new Function<Integer, LabeledVertex>() {
            @Override
            public LabeledVertex apply(Integer vId) {
                adjLists.put(vId, new AdjEdges());
                return v;
            }
        });
    }

    public void addEdge(LabeledEdge e) {
        adjLists.get(e.from().id()).add(e);
    }

    public Collection<LabeledVertex> vertices() {
        return vertices.values();
    }

    public LabeledVertex vertex(int id) {
        return vertices.get(id);
    }

    public LabeledEdge edge(int from, int to) {
        AdjEdges adj = adjLists.get(from);
        if (adj == null) {
            return null;
        }
        return adj.edgeTo(to);
    }

    public AdjEdges adjEdges(int vertexId) {
        return adjLists.get(vertexId);
    }

    public int vSize() {
        return vertices.size();
    }

    public int eSize() {
        int size = 0;
        for (AdjEdges adjEdges : adjLists.values()) {
            size += adjEdges.size();
        }
        return size / 2;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        Collection<LabeledVertex> vList = vertices();
        for (LabeledVertex v : vList) {
            builder.append(v.id()).append('\n');
        }

        for (LabeledVertex v : vList) {
            for (LabeledEdge e : adjEdges(v.id())) {
                builder.append(e.from().id())
                        .append(" ")
                        .append(e.to().id())
                        .append('\n');
            }
        }
        return builder.toString();
    }

    @Override
    public int compareTo(LabeledGraph other) {
        if (this.graphId < other.graphId) {
            return -1;
        }
        if (this.graphId > other.graphId) {
            return 1;
        }
        return 0;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(graphId);
    }
}
