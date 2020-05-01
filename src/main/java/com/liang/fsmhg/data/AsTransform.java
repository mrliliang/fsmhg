package com.liang.fsmhg.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyStore.Entry;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.function.Function;

import com.liang.fsmhg.Utils;
import com.liang.fsmhg.graph.AdjEdges;
import com.liang.fsmhg.graph.LabeledEdge;
import com.liang.fsmhg.graph.StaticEdge;
import com.liang.fsmhg.graph.StaticVertex;

public class AsTransform {
    HashMap<Integer, StaticVertex> allVertices = new HashMap<>();

    private static final int LARGE_DEGREE = 20;

    public static void main(String[] args) {
        File data = new File("/home/liliang/data/as-733");
        File outdir = new File("/home/liliang/data/as-733-snapshots");
        Utils.deleteFileDir(outdir);
        outdir.mkdirs();
        new AsTransform().transform(data, outdir);
    }

    private void transform(File data, File outdir) {
        int count = 0;
        for (File file : data.listFiles()) {
            count++;
            System.out.format("transform snapshot %d\r", count);
            HashMap<Integer, StaticVertex> vMap = new HashMap<>();
            HashMap<Integer, AdjEdges> adjLists = new HashMap<>();
            snapshot(file, vMap, adjLists);
            int vSize = vMap.size();
            int eSize = 0;
            int minDegree = vMap.size() - 1;
            int maxDegree = 0;
            int largeDegreeCount = 0;
            for (AdjEdges adj : adjLists.values()) {
                eSize += adj.size();
                if (adj.size() > 50) {
                    largeDegreeCount++;
                }
                if (adj.size() > maxDegree) {
                    maxDegree = adj.size();
                } else {
                    minDegree = adj.size();
                }
            }
            eSize = eSize / 2;

            File out = new File(outdir, file.getName() + "V" + vSize + "E" + eSize + "m" + minDegree + "M" + maxDegree + "L" + largeDegreeCount);
            output(out, vMap, adjLists, count);
        }
        System.out.println();
        System.out.println("Done!");
    }

    private void snapshot(File file, HashMap<Integer, StaticVertex> vMap, HashMap<Integer, AdjEdges> adjLists) {
        try {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            for (int i = 0; i < 4; i++) {
                br.readLine();
            }

            String line;
            String v[];
            while ((line = br.readLine()) != null && !line.isEmpty()) {
                v = line.split("\t");
                int fromId = Integer.parseInt(v[0]);
                int toId = Integer.parseInt(v[1]);
                if (fromId == toId) {
                    continue;
                }

                StaticVertex from = allVertices.computeIfAbsent(fromId, new Function<Integer, StaticVertex>() {
                    @Override
                    public StaticVertex apply(Integer vId) {
                        return new StaticVertex(vId, 1);
                    }
                });
                vMap.putIfAbsent(from.id(), from);
                
                StaticVertex to = allVertices.computeIfAbsent(toId, new Function<Integer, StaticVertex>() {
                    @Override
                    public StaticVertex apply(Integer vId) {
                        return new StaticVertex(vId, 1);
                    }
                });
                vMap.putIfAbsent(to.id(), to);

                AdjEdges edges = adjLists.computeIfAbsent(fromId, new Function<Integer, AdjEdges>() {
                    @Override
                    public AdjEdges apply(Integer fromId) {
                        return new AdjEdges();
                    }
                });
                LabeledEdge e = edges.edgeTo(toId);
                if (e == null) {
                    e = new StaticEdge(from, to, 1);
                    edges.add(e);;
                }
                
                edges = adjLists.computeIfAbsent(toId, new Function<Integer, AdjEdges>() {
                    @Override
                    public AdjEdges apply(Integer toId) {
                        return new AdjEdges();
                    }
                });
                e = edges.edgeTo(fromId);
                if (e == null) {
                    e = new StaticEdge(to, from, 1);
                    edges.add(e);;
                }
            }
            br.close();
            fr.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    private void removeHighDegreeVertices(HashMap<Integer, StaticVertex> vMap, HashMap<Integer, AdjEdges> adjLists) {
        // Iterator<Integer, StaticVertex> it = vMap.entrySet().iterator();
        // for (StaticVertex v : vMap.values()) {
        //     AdjEdges adj = adjLists.get(v.id());
        //     if (adj.size() > LARGE_DEGREE) {

        //     }
        // }
    }

    private void output(File out, HashMap<Integer, StaticVertex> vMap, HashMap<Integer, AdjEdges> adjLists, int transId) {
        TreeMap<Integer, StaticVertex> vMapOrdered = new TreeMap<>(vMap);
        try {
            FileWriter fw = new FileWriter(out);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("t # " + transId);
            for (StaticVertex v : vMapOrdered.values()) {
                bw.newLine();
                bw.write("v " + v.id() + " " + v.label());
            }
            for (StaticVertex v : vMapOrdered.values()) {
                for (LabeledEdge e : adjLists.get(v.id())) {
                    if (e.from().id() >= e.to().id()) {
                        continue;
                    }
                    bw.newLine();
                    bw.write("e " + e.from().id() + " " + e.to().id() + " " + e.label(0));
                }
            }
            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}