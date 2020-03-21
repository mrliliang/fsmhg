package com.liang.fsmhg.graph;


import java.util.*;

public class AdjEdges<E extends AEdge> implements Iterable<E> {

    private Map<Integer, E> edges;

    public AdjEdges() {
        edges = new TreeMap<>();
    }

    public int size() {
        return edges.size();
    }

    public E edgeTo(int to) {
        return edges.get(to);
    }

    public void add(E e) {
        edges.put(e.to().id(), e);
    }

    public List<E> edges() {
        return new ArrayList<>(edges.values());
    }

    @Override
    public Iterator<E> iterator() {
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
