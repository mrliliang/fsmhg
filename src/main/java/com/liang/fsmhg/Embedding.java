package com.liang.fsmhg;

import com.liang.fsmhg.graph.LabeledVertex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

public class Embedding {

    private Embedding parent;
    private LabeledVertex v;
    private int size;

    public Embedding(LabeledVertex v, Embedding parent) {
        this.v = v;
        this.parent = parent;
        if (parent == null) {
            size = 1;
        } else {
            size = parent.size + 1;
        }
    }

    public LabeledVertex vertex() {
        return v;
    }

    public List<LabeledVertex> vertices() {
        Vector<LabeledVertex> list = new Vector<>(size);
        list.setSize(size);
//        return vertices(1);
        Embedding em = this;
        for (int i = size - 1; i >= 0; i--) {
            list.set(i, em.v);
            em = em.parent;
        }
        return list;
    }

    private List<LabeledVertex> vertices(int count) {
        if (this.parent == null) {
            List<LabeledVertex> vertices = new ArrayList<>(count);
            vertices.add(v);
            return vertices;
        }
        List<LabeledVertex> vertices = this.parent.vertices(count + 1);
        vertices.add(v);
        return vertices;
    }

    private void vertices(List<LabeledVertex> list) {
        if (this.parent == null) {
            list.add(v);
            return;
        }
        parent.vertices(list);
        list.add(v);
    }

    public int size() {
        return size;
    }

}
