package com.liang.fsmhg.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.function.Function;

import com.liang.fsmhg.Utils;
import com.liang.fsmhg.graph.AdjEdges;
import com.liang.fsmhg.graph.LabeledEdge;
import com.liang.fsmhg.graph.LabeledVertex;
import com.liang.fsmhg.graph.StaticEdge;
import com.liang.fsmhg.graph.StaticVertex;

public class AsTransform {
    HashMap<Integer, StaticVertex> allVertices = new HashMap<>();
    HashMap<Integer, AdjEdges> allEdges = new HashMap<>();

    private static final int LARGE_DEGREE = 20;
    private static final int VERTEX_LABEL_NUM = 100;
    private static final int EDGE_LABEL_NUM = 100;

    int unconnectedCount = 0;
    private int maxV = 0;
    private int maxE = 0;
    private int minV = Integer.MAX_VALUE;
    private int minE = Integer.MAX_VALUE;

    public static void main(String[] args) {
        File indir = new File("/home/liliang/data/as-733");
        File outdir = new File("/home/liliang/data/as-733-snapshots-connected");
        Utils.deleteFileDir(outdir);
        outdir.mkdirs();
        List<File> data = Arrays.asList(indir.listFiles());
        Collections.sort(data, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return f1.getName().compareTo(f2.getName());
            }
        });
        new AsTransform().transform(data, outdir);
    }

    private void transform(List<File> data, File outdir) {
        int count = 0;
        for (File file : data) {
            count++;
            System.out.format("transform snapshot %d\r", count);
            HashMap<Integer, StaticVertex> vMap = new HashMap<>();
            HashMap<Integer, AdjEdges> adjLists = new HashMap<>();
            snapshot(file, vMap, adjLists);
            removeHighDegreeVertices(vMap, adjLists);
            int vSize = vMap.size();
            int eSize = 0;
            int minDegree = vMap.size() - 1;
            int maxDegree = 0;
            int largeDegreeCount = 0;
            for (AdjEdges adj : adjLists.values()) {
                eSize += adj.size();
                if (adj.size() > 5) {
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
            
            if (eSize < vSize - 1) {
                unconnectedCount++;
            }
            if (vSize > maxV) {
                maxV = vSize;
                maxE = eSize;
            }
            if (vSize < minV) {
                minV = vSize;
                minE = eSize;
            }
        }
        System.out.println();
        System.out.println("Done!");
        System.out.println(unconnectedCount + " unconnected graphs");
        System.out.format("Min graph %d vertices %d edges.\n", minV, minE);
        System.out.format("Max graph %d vertices %d edges.\n", maxV, maxE);
    }

    private void transform1(List<File> data, File outdir) {
        File f = data.get(0);
        HashMap<Integer, StaticVertex> vMap = new HashMap<>();
        HashMap<Integer, AdjEdges> adjLists = new HashMap<>();
        snapshot(f, vMap, adjLists);
        removeHighDegreeVertices(vMap, adjLists);
        int vSize = vMap.size();
        int eSize = 0;
        int minDegree = vMap.size() - 1;
        int maxDegree = 0;
        int largeDegreeCount = 0;
        for (AdjEdges adj : adjLists.values()) {
            eSize += adj.size();
            if (adj.size() > 5) {
                largeDegreeCount++;
            }
            if (adj.size() > maxDegree) {
                maxDegree = adj.size();
            } else {
                minDegree = adj.size();
            }
        }
        eSize = eSize / 2;

        for (int count = 1; count <= 10000; count++) {
            System.out.format("transform snapshot %d\r", count);
            String name = String.format("T%04dV%dE%dm%dM%dL%d", count, vSize, eSize, minDegree, maxDegree, largeDegreeCount);
            File out = new File(outdir, name);
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
                        return new StaticVertex(vId, vLabel());
                    }
                });
                vMap.putIfAbsent(from.id(), from);
                
                StaticVertex to = allVertices.computeIfAbsent(toId, new Function<Integer, StaticVertex>() {
                    @Override
                    public StaticVertex apply(Integer vId) {
                        return new StaticVertex(vId, vLabel());
                    }
                });
                vMap.putIfAbsent(to.id(), to);

                AdjEdges edges = allEdges.computeIfAbsent(fromId, new Function<Integer, AdjEdges>() {
                    @Override
                    public AdjEdges apply(Integer fromId) {
                        return new AdjEdges();
                    }
                });
                LabeledEdge e = edges.edgeTo(toId);
                if (e == null) {
                    e = new StaticEdge(from, to, eLabel());
                    edges.add(e);;
                }
                adjLists.computeIfAbsent(fromId, new Function<Integer, AdjEdges>() {
                    @Override
                    public AdjEdges apply(Integer fromId) {
                        return new AdjEdges();
                    }
                }).add(e);
                
                edges = allEdges.computeIfAbsent(toId, new Function<Integer, AdjEdges>() {
                    @Override
                    public AdjEdges apply(Integer toId) {
                        return new AdjEdges();
                    }
                });
                e = edges.edgeTo(fromId);
                if (e == null) {
                    e = new StaticEdge(to, from, eLabel());
                    edges.add(e);;
                }
                adjLists.computeIfAbsent(toId, new Function<Integer, AdjEdges>() {
                    @Override
                    public AdjEdges apply(Integer toId) {
                        return new AdjEdges();
                    }
                }).add(e);
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

    private int vLabel() {
        Random r = new Random();
        return r.nextInt(VERTEX_LABEL_NUM);
    }

    private int eLabel() {
        Random r = new Random();
        return r.nextInt(EDGE_LABEL_NUM);
    }


    private void removeHighDegreeVertices(HashMap<Integer, StaticVertex> vMap, HashMap<Integer, AdjEdges> adjLists) {
        Iterator<Entry<Integer, StaticVertex>> it = vMap.entrySet().iterator();
        List<LabeledVertex> isolatedVertices = new ArrayList<>();
        while (it.hasNext()) {
            Entry<Integer, StaticVertex> entry = it.next();
            AdjEdges adj = adjLists.get(entry.getKey());
            if (adj.size() <= LARGE_DEGREE) {
                continue;
            }
            it.remove();
            adjLists.remove(entry.getKey());
            for (LabeledEdge e : adj) {
                AdjEdges edges = adjLists.get(e.to().id());
                edges.remove(e.from().id());
                if (edges.size() == 0) {
                    isolatedVertices.add(e.to());
                }
            }
        }
        for (LabeledVertex v : isolatedVertices) {
            vMap.remove(v.id());
            adjLists.remove(v.id());
        }
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