package com.liang.fsmhg;

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
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Need to input two directories");
            System.exit(1);
        }

        File file1 = new File(args[0]);
        File file2 = new File(args[1]);
        if (!file1.isDirectory() && !file1.isDirectory()) {
            int diff = compareContent(file1, file2);
            if (diff != 0) {
                System.out.println(file1.getAbsolutePath() + " is different with " + file2.getAbsolutePath() + " in line " + diff);
                return;
            }
            System.out.println("Two results are the same.");
            return;
        }
        if (!file1.isDirectory()) {
            System.out.println(args[0] + " is not a directory");
            return;
        }
        if (!file2.isDirectory()) {
            System.out.println(args[1] + " is not a directory");
            return;
        }

        List<File> ret1 = Arrays.asList(file1.listFiles());
        List<File> ret2 = Arrays.asList(file2.listFiles());
        if (ret1.size() != ret2.size()) {
            System.out.println("The number of files is not equal.");
            return;
        }
        Comparator<File> c = new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o1.getName().compareTo(o2.getName());
            }
        };
        Collections.sort(ret1, c);
        Collections.sort(ret2, c);

        for (int i = 0; i < ret1.size(); i++) {
            File f1 = ret1.get(i);
            File f2 = ret2.get(i);
            System.out.println("Comparing " + f1.getName() + " and " + f2.getName());
            int diff = compareContent(f1, f2);
            if (diff != 0) {
                System.out.println(f1.getAbsolutePath() + " is different with " + f2.getAbsolutePath() + " in line " + diff);
                return;
            }
        }
        System.out.println("Two results are the same.");
    }
}