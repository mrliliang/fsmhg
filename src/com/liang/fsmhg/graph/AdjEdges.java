package com.liang.fsmhg.graph;


import java.util.*;

public class AdjEdges implements Iterable<LabeledEdge> {

    private Map<Integer, LabeledEdge> edges;

    public AdjEdges() {
        edges = new TreeMap<>();
    }

    public int size() {
        return edges.size();
    }

    public LabeledEdge edgeTo(int to) {
        return edges.get(to);
    }

    public void add(LabeledEdge e) {
        edges.put(e.to().id(), e);
    }

    public List<LabeledEdge> edges() {
        return new ArrayList<>(edges.values());
    }

    @Override
    public Iterator<LabeledEdge> iterator() {
        return edges.values().iterator();
//        return new Iterator<E>() {
//            Iterator<Map.Entry<Integer, E>> it = edges.entrySet().iterator();
//            @Override
//            public boolean hasNext() {
//                return it.hasNext();
//            }
//
//            @Override
//            public E next() {
//                return it.next().getValue();
//            }
//        };
    }
}
