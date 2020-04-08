package com.liang.fsmhg.graph;

import java.util.*;

public class Generator {

    private int initialEntity;
    private int maxEntity;
    private int averageDegree;
    private int snapshotNum;
    private double insertRate;
    private double addDelRate;

    private int vLabelNum;
    private int eLabelNum;

    private Random random;

    private TreeMap<Integer, StaticVertex> vMap;
    private Map<Integer, AdjEdges> eMap;
    private TreeMap<Integer, Integer> degreeInterval;
    private int totalDegree;

    public Generator(int initialEntity, int maxEntity, int averageDegree, int snapshotNum, double insertRate, double addDelRate, int vLabelNum, int eLabelNum) {
        this.initialEntity = initialEntity;
        this.maxEntity = maxEntity;
        this.averageDegree = averageDegree;
        this.snapshotNum = snapshotNum;
        this.insertRate = insertRate;
        this.addDelRate = addDelRate;
        this.vLabelNum = vLabelNum;
        this.eLabelNum = eLabelNum;
        this.random = new Random();

        eMap = new HashMap<>();
        vMap = new TreeMap<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer v1, Integer v2) {
                AdjEdges adj1 = eMap.get(v1);
                AdjEdges adj2 = eMap.get(v2);
                return adj1.size() - adj2.size();
            }
        });

        degreeInterval = new TreeMap<>();
    }

    public void generate() {
        firstSnapshot();
        output();
        for (int i = 0; i < snapshotNum; i++) {
            evolve();
            output();
        }
    }

    private void firstSnapshot() {
        int m0 = averageDegree / 2;
        for (int i = 0; i < m0; i++) {
            StaticVertex v = new StaticVertex(i, vLabel());
            eMap.put(v.id(), new AdjEdges());
            vMap.put(v.id(), v);
        }

        for (int i = m0; i < initialEntity; i++) {
            StaticVertex from = new StaticVertex(i, vLabel());
            eMap.put(i, new AdjEdges());
            for (int j = 0; j < m0; j++) {
                StaticVertex to = vMap.remove(select());
                while (eMap.get(from.id()).edgeTo(to.id()) != null) {
                    to = vMap.remove(select());
                }
                StaticEdge e1 = new StaticEdge(from, to, eLabel());
                eMap.get(from.id()).add(e1);
                StaticEdge e2 = new StaticEdge(to, from, eLabel());
                eMap.get(to.id()).add(e2);
                vMap.put(from.id(), from);
                vMap.put(to.id(), to);
                totalDegree++;
            }
        }

        int v;
        int degree = -1;
        int sum = degree;
        for (Integer id : vMap.keySet()) {
            AdjEdges adj = eMap.get(id);
            if (adj.size() != degree) {
                v = id;
                degree = adj.size();
                sum = degree;
                degreeInterval.put(sum, v);
            }
            sum += adj.size();
        }
    }

    private void evolve() {

    }

    private void output() {

    }

    private int select() {
        if (totalDegree == 0) {
            return random.nextInt(vMap.size());
        }
        int d = random.nextInt(totalDegree + 1);
//        if (d == 0) {
//            return random.nextInt(vMap.size());
//        }
        List<Integer> vertices = vertices(d);
        int index = random.nextInt(vertices.size());
        return vertices.get(index);
    }

    private int totalDegrees() {
        int degrees = 0;
        for (AdjEdges adjEdges : eMap.values()) {
            degrees += adjEdges.size();
        }
        return degrees;
    }

    private int vLabel() {
        return random.nextInt(vLabelNum);
    }

    private int eLabel() {
        return random.nextInt(eLabelNum);
    }

    private List<Integer> vertices(int degree) {
        Map.Entry<Integer, Integer> entry = degreeInterval.floorEntry(degree);
        int v = entry.getKey();
        Map<Integer, StaticVertex> map = vMap.tailMap(v, true).headMap(v, true);
        return new ArrayList<>(map.keySet());
    }

}
