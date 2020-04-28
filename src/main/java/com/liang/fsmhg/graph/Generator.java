package com.liang.fsmhg.graph;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

import com.liang.fsmhg.Utils;

public class Generator {

    private File dir;
    private int initialEntity;
    private int averageDegree;
    private int snapshotNum;
    private double insertRate;
    private double insertDelRate;

    private int vLabelNum;
    private int eLabelNum;

    private Random random;

    private Map<Integer, StaticVertex> vMap;
    private Map<Integer, AdjEdges> eMap;
    private TreeMap<Integer, TreeSet<Integer>> vGroup;
    private int totalDegree;

    public Generator(File dir, int initialEntity, int averageDegree, int snapshotNum, double insertRate, double insertDelRate, int vLabelNum, int eLabelNum) {
        this.dir = dir;
        this.initialEntity = initialEntity;
//        this.maxEntity = maxEntity;
        this.averageDegree = averageDegree;
        this.snapshotNum = snapshotNum;
        this.insertRate = insertRate;
        this.insertDelRate = insertDelRate;
        this.vLabelNum = vLabelNum;
        this.eLabelNum = eLabelNum;
        this.random = new Random();

        eMap = new TreeMap<>();
        vMap = new HashMap<>();
        vGroup = new TreeMap<>();
    }

    public void generate() {
        Utils.deleteFileDir(dir);
        dir.mkdir();
        boolean append = false;
        firstSnapshot();
        output(0, append);
        for (int i = 1; i < snapshotNum; i++) {
            evolve();
            output(i, append);
        }
        if (append) {
            output(-1, append);
        }
    }

    private void firstSnapshot() {
        int m0 = averageDegree / 2;
        vGroup.put(0, new TreeSet<>());
        for (int i = 0; i < m0; i++) {
            StaticVertex v = new StaticVertex(i, vLabel());
            eMap.put(v.id(), new AdjEdges());
            vMap.put(v.id(), v);
            vGroup.get(0).add(v.id());
        }

        for (int i = m0; i < initialEntity; i++) {
            insertEdges(i, m0);
        }
    }

    private void evolve() {
        int maxV = (int)Math.ceil(vMap.size() + initialEntity * insertRate);
        int insertedEdgeNum = (int)Math.ceil(averageDegree / (2 * (1 - 1 / insertDelRate)));
        for (int i = vMap.size(); i < maxV; i++) {
            insertEdges(i, insertedEdgeNum);
        }

        int removedEdgeNum = (int)Math.floor(insertRate * initialEntity * insertedEdgeNum / insertDelRate);
        for (int i = 0; i < removedEdgeNum; i++) {
            removeEdge();
        }
    }

    private void insertEdges(int vId, int edgeNum) {
        StaticVertex from = new StaticVertex(vId, vLabel());
        eMap.put(from.id(), new AdjEdges());
        for (int j = 0; j < edgeNum; j++) {
            StaticVertex to = vMap.get(selectAttachPoint());
            while (eMap.get(from.id()).edgeTo(to.id()) != null) {
                to = vMap.get(selectAttachPoint());
            }
            vGroup.get(eMap.get(to.id()).size()).remove(to.id());
            int eLabel = eLabel();
            StaticEdge e = new StaticEdge(from, to, eLabel);
            eMap.get(from.id()).add(e);
            e = new StaticEdge(to, from, eLabel);
            eMap.get(to.id()).add(e);
            vGroup.computeIfAbsent(eMap.get(to.id()).size(), integer -> new TreeSet<>()).add(to.id());
            totalDegree += 2;
        }
        vMap.put(from.id(), from);
        TreeSet<Integer> group = vGroup.get(eMap.get(from.id()).size());
        if (group == null) {
            group = new TreeSet<>();
            vGroup.put(eMap.get(from.id()).size(), group);
        }
        group.add(from.id());
    }

    private void removeEdge() {
        int from = random.nextInt(vMap.size());
        int to = random.nextInt(vMap.size());
        while (from == to || eMap.get(from).edgeTo(to) == null) {
            from = random.nextInt(vMap.size());
            to = random.nextInt(vMap.size());
        }
        eMap.get(from).remove(to);
        eMap.get(to).remove(from);
        totalDegree -= 2;
    }

    private void output(int transId, boolean append) {
        System.out.println("TRANS " + transId);
        int vSize = vMap.size();
        int eSize = 0;
        for (AdjEdges adjEdges : eMap.values()) {
            eSize += adjEdges.size();
        }
        eSize /= 2;
        
        String filename = String.format("T%04dV%dE%d.txt", transId, vSize, eSize);
        if (append) {
            filename = "snapshots.txt";
        }
        File file = new File(dir, filename);
        try {
            FileWriter writer = new FileWriter(file, append);
            BufferedWriter bw = new BufferedWriter(writer);
            if (transId != 0 && append) {
                bw.newLine();
            }
            bw.write("t # " + transId);
            if (transId == -1) {
                bw.close();
                writer.close();
                return;
            }
            for (StaticVertex v : vMap.values()) {
                bw.newLine();
                bw.write("v " + v.id() + " " + v.label());
            }
            List<StaticVertex> vertices = new ArrayList<>(vMap.values());
            for (int i = 0; i < vertices.size(); i++) {
                for (int j = i + 1; j < vertices.size(); j++) {
                    StaticVertex from = vertices.get(i);
                    StaticVertex to = vertices.get(j);
                    StaticEdge e = (StaticEdge) eMap.get(from.id()).edgeTo(to.id());
                    if (e == null) {
                        continue;
                    }
                    bw.newLine();
                    bw.write("e " + from.id() + " " + to.id() + " " + e.label());
                }
            }
            bw.close();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int vLabel() {
        return random.nextInt(vLabelNum);
    }

    private int eLabel() {
        return random.nextInt(eLabelNum);
    }

    private int selectAttachPoint() {
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

    private List<Integer> vertices(int degree) {
        int sum = 0;
        TreeSet<Integer> vertices = new TreeSet<>();
        for (Map.Entry<Integer, TreeSet<Integer>> entry : vGroup.entrySet()) {
            int d = entry.getKey();
            vertices = entry.getValue();
            if (vertices == null || vertices.isEmpty()) {
                continue;
            }
            sum += d * vertices.size();
            if (sum >= degree) {
                break;
            }
        }
        return new ArrayList<>(vertices);
    }

    public static void main(String[] args) {
        File dir = new File("synthetic");
        int initialEntity = 300;
        int averageDegree = 4;
        int snapshotNum = 2000;
        double insertRate = 0.005;
        double insertDelRate = 10;

        int vLabelNum = 100;
        int eLabelNum = 100;

        Generator generator = new Generator(dir, initialEntity, averageDegree, snapshotNum, insertRate, insertDelRate, vLabelNum, eLabelNum);
        generator.generate();
    }

}
