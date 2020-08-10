import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class PatternTreeWriter {
    private FileWriter fw;
    private BufferedWriter bw;

    public PatternTreeWriter(File file) {
        this.fw = new FileWriter(file);
        this.bw = new BufferedWriter(fw);
    }
    
    public void saveNode(Pattern p) {
        bw.write("code:" + p.code());
        bw.write("support:" + p.support());
        bw.write("sequences:");
        bw.write("snapshots:");
        bw.write("sequence delimiter:");
        bw.write("snapshot delimiter:");
        bw.write("check min:");
    }

    public void close() {
        bw.close();
        fw.close();
    }
}