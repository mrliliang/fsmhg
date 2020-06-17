package com.liang.fsmhg;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.liang.fsmhg.graph.LabeledGraph;
import com.liang.fsmhg.graph.StaticGraph;

public class TransLoader {
    private File data;
    private FileReader fr;
    private BufferedReader br;

    private long currentTransId = -1;

    private List<File> snapshots;
    private int loadCounter = 0;

    public TransLoader(File data) {
        this.data = data;
        try {
            if (!data.isDirectory()) {
                fr = new FileReader(data);
                br = new BufferedReader(fr);
            } else {
                snapshots = Arrays.asList(this.data.listFiles());
                Collections.sort(snapshots, new Comparator<File>() {
                    @Override
                    public int compare(File f1, File f2) {
                        return f1.getName().compareTo(f2.getName());
                    }
                });
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public List<LabeledGraph> loadTrans() {
        List<LabeledGraph> trans = new ArrayList<>();
        if (!this.data.isDirectory()) {
            trans.addAll(readTrans(this.data));
        } else {
            // List<File> files = Arrays.asList(this.data.listFiles());
            // Collections.sort(files, new Comparator<File>() {
            //     @Override
            //     public int compare(File f1, File f2) {
            //         return f1.getName().compareTo(f2.getName());
            //     }
            // });
            for (File f : snapshots) {
                trans.addAll(readTrans(f));
            }
        }
        return trans;
    }

    private List<LabeledGraph> readTrans(File file) {
        List<LabeledGraph> trans = new ArrayList<>();
        FileReader fr = null;
        BufferedReader br = null;
        try {
            fr = new FileReader(file);
            br = new BufferedReader(fr);
            String line;
            LabeledGraph g = null;
            while ((line = br.readLine()) != null) {
                String[] str = line.split(" ");
                if (line.startsWith("t")) {
                    long id = Long.parseLong(line.split(" ")[2]);
                    if (id >= 0) {
                        g = new StaticGraph(id);
                        trans.add(g);
                    }
                } else if (line.startsWith("v")) {
                    g.addVertex(Integer.parseInt(str[1]), Integer.parseInt(str[2]));
                } else if (line.startsWith("e")) {
                    int from = Integer.parseInt(str[1]);
                    int to = Integer.parseInt(str[2]);
                    int eLabel = Integer.parseInt(str[3]);
                    g.addEdge(from, to, eLabel);
                    g.addEdge(to, from, eLabel);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
                fr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return trans;
    }

    public List<LabeledGraph> loadTrans(int num) {
        List<LabeledGraph> trans = new ArrayList<>(num);
        for (int i = 0; i < num; i++) {
            if (!data.isDirectory()) {
                LabeledGraph g = nextTrans();
                if(g == null) {
                    close();
                    break;
                }
                trans.add(g);
            } else {
                if (this.currentTransId < this.snapshots.size()) {
                    File f = this.snapshots.get(this.loadCounter++);
                    trans.addAll(readTrans(f));
                } else {
                    break;
                }
            }
        }
        return trans;
    }

    private LabeledGraph nextTrans() {
        try {
            String line;
            LabeledGraph g = null;
            if (currentTransId != -1) {
                g = new StaticGraph(currentTransId);
            }
            while ((line = br.readLine()) != null) {
                String[] str = line.split(" ");
                if (line.startsWith("t")) {
                    currentTransId = Long.parseLong(line.split(" ")[2]);
                    if (currentTransId == -1 || currentTransId > 0) {
                        break;
                    }
                    if (currentTransId == 0) {
                        g = new StaticGraph(currentTransId);
                    }
                } else if (line.startsWith("v")) {
                    g.addVertex(Integer.parseInt(str[1]), Integer.parseInt(str[2]));
                } else if (line.startsWith("e")) {
                    int from = Integer.parseInt(str[1]);
                    int to = Integer.parseInt(str[2]);
                    int eLabel = Integer.parseInt(str[3]);
                    g.addEdge(from, to, eLabel);
                    g.addEdge(to, from, eLabel);
                }
            }

            if (line == null) {
                currentTransId = -1;
            }
            return g;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void close() {
        try {
            br.close();
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
