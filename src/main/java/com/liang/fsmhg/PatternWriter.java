package com.liang.fsmhg;

import com.liang.fsmhg.code.DFSCode;
import com.liang.fsmhg.code.DFSEdge;
import com.liang.fsmhg.graph.LabeledGraph;
import com.liang.fsmhg.graph.LabeledVertex;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class PatternWriter {
    private FileWriter fw;
    private BufferedWriter bw;

    public PatternWriter(File output) {
        try {
            fw = new FileWriter(output);
            bw = new BufferedWriter(fw);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save(PointPattern p, int count) {
        try {
            bw.write("t # " + count + " * " + p.support());
            bw.newLine();
            bw.write("v 0 " + p.label());
            bw.newLine();
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save(Pattern p, int count) {
        try {
            bw.write("t # " + count + " * " + p.support());
            bw.newLine();
            DFSCode code = p.code();
            LabeledGraph g = code.toGraph();
            for (int i = 0; i < g.vSize(); i++) {
                LabeledVertex v = g.vertex(i);
                bw.write("v " + i + " " + g.vLabel(v));
                bw.newLine();
            }
            for (DFSEdge e : code.edges()) {
                bw.write("e " + e.from() + " " + e.to() + " " + e.edgeLabel());
                bw.newLine();
            }
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
