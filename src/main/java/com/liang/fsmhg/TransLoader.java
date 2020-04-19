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
        return trans;
    }

    private List<LabeledGraph> readTrans(File file) {
        List<LabeledGraph> trans = new ArrayList<>();
        try {
            fr = new FileReader(file);
            br = new BufferedReader(fr);
            String line;
            LabeledGraph g = null;
            while ((line = br.readLine()) != null) {
                String[] str = line.split(" ");
                if (line.startsWith("t")) {
                    int id = Integer.parseInt(line.split(" ")[2]);
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

    private void close() {
        try {
            br.close();
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
