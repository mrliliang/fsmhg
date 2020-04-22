package com.liang.fsmhg;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import com.liang.fsmhg.graph.LabeledGraph;

public class Test {

    public static void outputGraphIds(List<LabeledGraph> graphs, File file) {
        try {
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            for (LabeledGraph g : graphs) {
                bw.write(String.valueOf(g.graphId()));
                bw.newLine();
            }
            bw.close();
            fw.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static int compareContent(File f1, File f2) {
        try {
            FileReader fr1 = new FileReader(f1);
            BufferedReader br1 = new BufferedReader(fr1);
            FileReader fr2 = new FileReader(f2);
            BufferedReader br2 = new BufferedReader(fr2);

            String line1;
            String line2;
            int lineCount = 0;
            while ((line1 = br1.readLine()) != null && (line2 = br2.readLine()) != null) {
                lineCount++;
                if (!line1.equals(line2)) {
                    br1.close();
                    fr1.close();
                    fr2.close();
                    br2.close();
                    return lineCount;
                }
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return 0;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Need to input two directories");
            System.exit(1);
        }

        File dir1 = new File(args[0]);
        if (!dir1.isDirectory()) {
            System.out.println(args[0] + " is not a directory");
            return;
        }
        File dir2 = new File(args[1]);
        if (!dir2.isDirectory()) {
            System.out.println(args[1] + " is not a directory");
            return;
        }

        File[] ret1 = dir1.listFiles();
        File[] ret2 = dir2.listFiles();
        if (ret1.length != ret2.length) {
            System.out.println("The number of files is not equal.");
            return;
        }

        for (int i = 0; i < ret1.length; i++) {
            int diff = compareContent(ret1[i], ret2[i]);
            if (diff != 0) {
                System.out.println(ret1[i].getAbsolutePath() + " is different with " + ret2[i].getAbsolutePath() + " in line " + diff);
                return;
            }
        }
        System.out.println("Two results are the same.");
    }
}