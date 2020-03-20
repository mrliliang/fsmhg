package com.liang.fsmhg;


import com.liang.fsmhg.graph.DynamicEdge;
import com.liang.fsmhg.graph.DynamicVertex;
import com.liang.fsmhg.graph.LabeledGraph;
import com.liang.fsmhg.graph.Snapshot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Cluster implements Iterable<Snapshot> {

    private double similarity;
    private List<Snapshot> snapshots;

    private List<DynamicVertex> intersectedVertices;
    private List<DynamicEdge> intersectedEdges;


    public Cluster(double similarity) {
        this.similarity = similarity;
        snapshots = new ArrayList<>();
        intersectedVertices = new ArrayList<>();
        intersectedEdges = new ArrayList<>();
    }

    public List<Snapshot> snapshots() {
        return snapshots;
    }

    public void remove(List<Snapshot> snapshots) {
        snapshots.removeAll(snapshots);
    }

    public boolean add(Snapshot s) {
        if (this.snapshots.size() == 0) {
            this.snapshots.add(s);
            intersectedVertices = s.vertices();
            intersectedEdges = s.edges();
            return true;
        }

        return similarityCheck(s);
    }

    private boolean similarityCheck(Snapshot s) {
        Snapshot last = snapshots.get(snapshots.size() - 1);

        List<DynamicVertex> vertices = new ArrayList<>(intersectedVertices);
        vertices.retainAll(s.vertices());
        Iterator<DynamicVertex> vertexIterator = vertices.iterator();
        while (vertexIterator.hasNext()) {
            DynamicVertex v = vertexIterator.next();
            if (last.vLabel(v) != s.vLabel(v)) {
                vertexIterator.remove();
            }
        }

        List<DynamicEdge> edges = new ArrayList<>(intersectedEdges);
        edges.retainAll(s.edges());
        Iterator<DynamicEdge> edgeIterator = edges.iterator();
        while (edgeIterator.hasNext()) {
            DynamicEdge e = edgeIterator.next();
            if (last.eLabel(e) != s.eLabel(e)
                    || last.vLabel(e.from()) != s.vLabel(e.from())
                    || last.vLabel(e.to()) != s.vLabel(e.to())) {
                edgeIterator.remove();
            }
        }


        int denominator = 0;
        for (Snapshot snapshot : snapshots) {
            denominator += (snapshot.vSize() + snapshot.eSize());
        }
        denominator += (s.vSize() + s.eSize());
        double sim = (double)(snapshots.size() + 1) * (vertices.size() + edges.size()) / denominator;
        if (sim >= similarity) {
            intersectedVertices = vertices;
            intersectedEdges = edges;
            return true;
        }
        return false;
    }


    public Intersection intersection() {
        Snapshot s = snapshots.get(snapshots.size() - 1);
        return new Intersection(s.graphId(), intersectedVertices, intersectedEdges);
    }

    @Override
    public Iterator<Snapshot> iterator() {
        return snapshots.iterator();
    }

    public static List<Cluster> partition(List<Snapshot> snapshots, double similarity) {
        List<Cluster> clusters = new ArrayList<>();
        Cluster cluster = new Cluster(similarity);
        for (Snapshot s : snapshots) {
            if (cluster.add(s)) {
                continue;
            }
            clusters.add(cluster);
            cluster = new Cluster(similarity);
            cluster.add(s);
        }
        return clusters;
    }

    public class Intersection extends LabeledGraph<DynamicVertex, DynamicEdge> {

        private Intersection(long id, List<DynamicVertex> vertices, List<DynamicEdge> edges) {
            super(id, vertices, edges);
        }

        @Override
        public int vLabel(DynamicVertex v) {
            return v.label(graphId());
        }

        @Override
        public int eLabel(DynamicEdge e) {
            return e.label(graphId());
        }

        @Override
        public DynamicVertex addVertex(int id, int label) {
            throw new RuntimeException("Not allowed to add vertex to intersection.");
        }

        @Override
        public DynamicEdge addEdge(int from, int to, int eLabel) {
            throw new RuntimeException("Not allowed to add edge to intersection.");
        }
    }
}
