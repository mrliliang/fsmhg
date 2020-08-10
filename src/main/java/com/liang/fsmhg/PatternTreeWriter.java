import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

import com.liang.fsmhg.graph.LabeledGraph;

public class PatternTreeWriter {
    private FileWriter fw;
    private BufferedWriter bw;

    public PatternTreeWriter(File file) {
        this.fw = new FileWriter(file);
        this.bw = new BufferedWriter(fw);
    }
    
    public void saveNode(Pattern p) {
        //code
        // bw.write("code:" + p.code().toString());
        bw.write(p.code().toString() + ",");
        // bw.newLine();

        //support
        // bw.write("support:" + p.support());
        bw.write(p.support() + ",");
        // bw.newLine();

        //sequences
        StringBuilder sb = new StringBuilder();
        List<Cluster> clusters = p.clusters();
        for (int i = 0; i < clusters.size() - 1; i++) {
            sb.append(clusters.get(i).index()).append(" ");
        }
        if (clusters.size() > 0) {
            sb.append(clusters.get(clusters.size() - 1).index());
        }
        // bw.write("sequences:" + sb.toString());
        bw.write(sb.toString() + ",");
        // bw.newLine();

        //graphs
        sb = new StringBuilder();
        List<LabeledGraph> graphs = p.graphs();
        for (int i = 0; i < graphs.size() - 1; i++) {
            sb.append(graphs.get(i).graphId()).append(" ");
        }
        if (graphs.size() > 0) {
            sb.append(graphs.get(graphs.size() - 1).graphId());
        }
        // bw.write("graphs:" + sb.toString());
        bw.write(sb.toString() + ",");
        // bw.newLine();

        //sequence delimiter
        int seqDelimiter = -1;
        Cluster cDelimiter = p.clusterDelimiter();
        if (cDelimiter != null) {
            seqDelimiter = cDelimiter.index();
        }
        // bw.write("sequence delimiter:" + cDelimiter);
        bw.write(cDelimiter + ",");
        bw.newLine();

        //graph delimiter
        long graphDelimiter = -1;
        LabeledGraph g = p.graphDelimiter();
        if (g != null) {
            graphDelimiter = g.graphId();
        }
        // bw.write("graph delimiter:" + graphDelimiter);
        bw.write(graphDelimiter + ",");
        bw.newLine();

        //isMinChecked
        int isMinChecked = p.isMinChecked() ? 1 : 0;
        // bw.write("isMinChecked:" + isMinChecked);
        bw.write(isMinChecked + ",");
        bw.newLine();

        //minCheckResult
        int minCheckResult = p.minCheckResult() ? 1 : 0;
        // bw.write("minCheckResult:" + minCheckResult);
        bw.write(minCheckResult + "");
        bw.newLine();
    }

    public void close() {
        bw.close();
        fw.close();
    }
}