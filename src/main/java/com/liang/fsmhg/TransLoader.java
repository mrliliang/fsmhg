package com.liang.fsmhg;

import com.liang.fsmhg.graph.LabeledGraph;
import com.liang.fsmhg.graph.StaticGraph;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TransLoader {
    private File data;
    private FileReader fr;
    private BufferedReader br;

    private long currentTransId = -1;

    public TransLoader(File data) {
        this.data = data;
        try {
            fr = new FileReader(data);
            br = new BufferedReader(fr);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public List<LabeledGraph> loadTrans() {
        List<LabeledGraph> trans = new ArrayList<>();
        if (!this.data.isDirectory()) {
            trans.addAll(readTrans(this.data));
        } else {
            for (File f : this.data.listFiles()) {
                trans.addAll(readTrans(f));
            }
        }
        close();
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
            if(!hasNext()) {
                close();
                break;
            }
            trans.add(nextTrans());
        }
        return trans;
    }

    private boolean hasNext() {
        return currentTransId != -1;
    }

    private LabeledGraph nextTrans() {
        try {
            String line;
            LabeledGraph g = null;
            while ((line = br.readLine()) != null) {
                if (currentTransId != -1) {
                    g = new StaticGraph(currentTransId);
                }

                String[] str = line.split(" ");
                if (line.startsWith("t")) {
                    currentTransId = Integer.parseInt(line.split(" ")[2]);
                    if (currentTransId > 0) {
                        break;
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
