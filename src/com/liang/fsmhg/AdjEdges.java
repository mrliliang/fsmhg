package com.liang.fsmhg;


import java.util.*;

public class AdjEdges<E extends Edge> implements Iterable<E> {

    private Map<Integer, E> edges;

    public AdjEdges() {
        edges = new HashMap<>();
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
        return new Iterator<E>() {
            Iterator<Map.Entry<Integer, E>> it = edges.entrySet().iterator();
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public E next() {
                return it.next().getValue();
            }
        };
    }
}
