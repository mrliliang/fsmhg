import com.liang.fsmhg.Cluster;
import com.liang.fsmhg.TransLoader;
import com.liang.fsmhg.code.DFSCode;
import com.liang.fsmhg.code.DFSEdge;
import com.liang.fsmhg.graph.LabeledGraph;

import java.io.File;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;


public class Test {

    public static void main(String[] args) {
//         System.out.format("%.20f", 10000 * 0.5);
//         System.out.println();


//         String code;
// //        code = "(0,1,X,a,Y)(1,2,Y,b,X)(2,0,X,a,X)(2,3,X,c,Z)(3,1,Z,b,Y)(1,4,Y,d,Z)";//false
// //        code = "(0,1,Y,a,X)(1,2,X,a,X)(2,0,X,b,Y)(2,3,X,c,Z)(3,0,Z,b,Y)(0,4,Y,d,Z)";//false
//         code = "(0,1,X,a,X)(1,2,X,a,Y)(2,0,Y,b,X)(2,3,Y,b,Z)(3,0,Z,c,X)(2,4,Y,d,Z)";//true
//         code = "(0,1,2,2,2)(1,2,2,2,2)(0,3,2,2,2)";//false
// //        code = "(0,1,2,2,2)(1,2,2,5,2)(1,3,2,5,2)(3,4,2,5,2)";//false
//         code = "(0,1,2,2,2)(1,2,2,5,2)(2,3,2,5,2)(3,4,2,5,2)(4,5,2,5,2)";//true
//         code = "(0,1,2,2,2)(1,2,2,5,2)(2,3,2,5,2)(3,4,2,5,2)(4,5,2,5,2)(1,6,2,5,2)";//true
//         code = "(0,1,2,2,2)(1,2,2,5,2)(2,3,2,5,2)(3,4,2,5,2)(1,5,2,5,2)";//true
//         code = "(0,1,2,2,2)(1,2,2,5,2)(2,3,2,5,2)(3,4,2,5,2)(1,5,2,5,2)(5,6,2,5,2)";//true
//         code = "(0,1,2,2,2)(1,2,2,5,2)(2,3,2,5,2)(1,4,2,5,2)";//true
//         code = "(0,1,2,2,2)(1,2,2,5,2)(2,3,2,5,2)(1,4,2,5,2)(4,5,2,5,2)";//true
//         code = "(0,1,2,5,2)(1,2,2,5,2)(2,3,2,5,2)(3,4,2,5,2)(3,5,2,5,2)(2,6,2,5,5)(6,7,5,5,2)";//true
//         code = "(0,1,2,2,2)(1,2,2,5,2)(2,3,2,5,2)(3,4,2,2,2)(3,5,2,5,2)(5,6,2,5,2)(1,7,2,5,2)";//true
//         DFSCode dfsCode = code(code);
//         System.out.println("min = " + dfsCode.isMin());
//         System.out.println(dfsCode);
//         System.out.println(dfsCode.minCode());

        // TransLoader loader = new TransLoader(new File("/home/liliang/data/as-733-snapshots-repeat"));
        // List<LabeledGraph> graphs =  loader.loadTrans();
        // long begin = System.currentTimeMillis();
        // List<Cluster> clusters = Cluster.partition(graphs, 0.4, 0);
        // long end = System.currentTimeMillis();
        // System.out.println("partition time " + (end - begin));
        // System.out.println(clusters.size() + " clusters");

        HashMap<Integer, Integer> map = new HashMap<>();
        Integer i = map.putIfAbsent(1, 1);
        System.out.println(i);
        i = map.putIfAbsent(1, 2);
        System.out.println(i);
        System.out.println(map.get(1));
    }


    private static DFSCode code(String code) {
        DFSCode dfsCode = new DFSCode();
        String[] edges = code.split("\\)");
        for (String edge : edges) {
            DFSEdge dfsEdge = edge(edge);
            dfsCode.add(dfsEdge);
        }
        return dfsCode;
    }

    private static DFSEdge edge(String edge) {
        String[] item = edge.substring(1).split(",");
        int from = Integer.parseInt(item[0]);
        int to = Integer.parseInt(item[1]);
//        int fromLabel = item[2].charAt(0);
//        int toLabel = item[4].charAt(0);
//        int eLabel = item[3].charAt(0);
        int fromLabel = Integer.parseInt(item[2]);
        int toLabel = Integer.parseInt(item[4]);
        int eLabel = Integer.parseInt(item[3]);

        return new DFSEdge(from, to, fromLabel, toLabel, eLabel);
    }

}
