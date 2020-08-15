package com.liang.fsmhg;

import java.io.File;
import java.util.List;

import com.liang.fsmhg.graph.LabeledGraph;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Main {
    public static void main(String[] args) {
        Arguments arguments = Arguments.parse(args);

        File output = new File(arguments.output);
        Utils.deleteFileDir(output);
        File data = new File(arguments.data);
        TransLoader loader = new TransLoader(data);
        if (arguments.window <= 0) {
            FSMHG fsmhg = new FSMHG(output, arguments.support, arguments.maxEdgeNum, arguments.partition, arguments.similarity);
            // fsmhg.optimize(arguments.optimize);
            fsmhg.enumerate(loader.loadTrans());
            return;
        }

        output.mkdir();
        long startTime = System.currentTimeMillis();
        long winStart = startTime;
        long initialWinTime = 0;
        long maxWinTime = 0;
        long minWinTime = 0;
        FSMHGWIN fsmhgwin = new FSMHGWIN(arguments.support, arguments.maxEdgeNum, arguments.partition, arguments.similarity);
        List<LabeledGraph> trans = loader.loadTrans(arguments.window);
        int winCount = -1;
        while (trans.size() == arguments.window) {
            winCount++;
            System.out.println("Window " + winCount);
            File outfile = new File(output, String.format("WIN%03d", winCount));
            if (arguments.enumerator == Arguments.ENUM_FSMHG_WIN) {
                fsmhgwin.setOutput(outfile);
                fsmhgwin.enumerate(trans);
            } else {
                FSMHG fsmhg = new FSMHG(outfile, arguments.support, arguments.maxEdgeNum, arguments.partition, arguments.similarity);
                fsmhg.setWinCount(winCount);
                fsmhg.enumerate(trans);
            }

            long winEnd = System.currentTimeMillis();
            long winTime = winEnd - winStart;
            if (winCount == 0) {
                initialWinTime = winTime;
            } else if (winCount == 1) {
                maxWinTime = winTime;
                minWinTime = winTime;
            } else {
                if (winTime > maxWinTime) {
                    maxWinTime = winTime;
                }
                if (winTime < minWinTime) {
                    minWinTime = winTime;
                }
            }

            winStart = System.currentTimeMillis();
            trans = trans.subList(arguments.sliding, trans.size());
            trans.addAll(loader.loadTrans(arguments.sliding));
        }
        long totalTime = System.currentTimeMillis() - startTime;

        for (int i = 0; i < 50; i++) {
            System.out.print("*");
        }
        System.out.println();
        System.out.println("Initial window time = " + initialWinTime);
        long slidingTime = totalTime - initialWinTime;
        System.out.println("Total sliding time = " + slidingTime);
        System.out.println("Max window time = " + maxWinTime);
        System.out.println("Min window time = " + minWinTime);
        System.out.println("Window count = " + winCount);
        double averageWinTime = (double)slidingTime / winCount;
        System.out.println("Average window time = " + averageWinTime);
        for (int i = 0; i < 50; i++) {
            System.out.print("*");
        }
        System.out.println();
    }

    private static class Arguments {
        public static final int ENUM_FSMHG_WIN = 1;
        public static final int ENUM_FSMHG = 2;
        public String data;
        public String output;
        public double support;
        public int maxEdgeNum = Integer.MAX_VALUE;
        public boolean partition = false;
        public double similarity;
        public int window;
        public int sliding;
        public int enumerator = ENUM_FSMHG_WIN;
        public boolean optimize = true;


        private Arguments() {

        }

        public static Arguments parse(String[] args) {
            Options ops = new Options();
            ops.addOption("d", "data", true, "The path of data");
            ops.addOption("o", "output", true, "The file name of the result");
            ops.addOption("s", "support",true, "Support threshold");
            ops.addOption("m", "max-edge", true, "The maximal number of edges of a pattern");
            ops.addOption("p", "partition", false, "Partition transaction into clusters against specified similarity value");
            ops.addOption("sim", "similarity", true, "Similarity value for partition (0 - 1.0)");
            ops.addOption("w", "window size", true, "Open sliding window mode and specify the window size (>= 1)");
            ops.addOption("ss", "sliding speed", true, "Window sliding speed (>0 1)");
            ops.addOption("e", "enumerator", true, "1(FSMHG-WIN)/2(FSMHG)");
            ops.addOption("opt", "optimization", true, "Use min code optimization (YES / NO)");
            ops.addOption("h", "Help");

            HelpFormatter formatter = new HelpFormatter();
            if (args == null || args.length == 0) {
                formatter.printHelp("java -jar fsmhg.jar", ops);
                System.exit(1);
            }

            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = null;
            try {
                cmd = parser.parse(ops, args);
                if (cmd.hasOption("h")) {
                    formatter.printHelp("java -jar fsmhg.jar", ops);
                    System.exit(1);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }

            Arguments arguments = new Arguments();
            arguments.data = cmd.getOptionValue("d");
            arguments.output = cmd.getOptionValue("o");
            if (!cmd.hasOption("o")) {
                arguments.output = arguments.data + "_result";
            }

            if (!cmd.hasOption("s")) {
                System.out.println("support value is required.");
                System.exit(1);
            }
            arguments.support = Double.parseDouble(cmd.getOptionValue("s"));

            if (cmd.hasOption("m")) {
                arguments.maxEdgeNum = Integer.parseInt(cmd.getOptionValue("m"));
            }

            if (cmd.hasOption("p")) {
                arguments.partition = true;

                if (!cmd.hasOption("sim")) {
                    System.out.println("Must input a similarity value for partition");
                    System.exit(1);
                }
                arguments.similarity = Double.parseDouble(cmd.getOptionValue("sim"));
            }

            if (!cmd.hasOption("w") && cmd.hasOption("ss")) {
                System.out.println("Must specify window size");
                System.exit(1);
            }

            if (cmd.hasOption("w")) {
                arguments.window = Integer.parseInt(cmd.getOptionValue("w"));
                if (arguments.window <= 0) {
                    System.out.println("Window size must be >= 1");
                    System.exit(1);
                }

                if (!cmd.hasOption("ss")) {
                    System.out.println("Must specify window sliding speed");
                    System.exit(1);
                }

                arguments.sliding = Integer.parseInt(cmd.getOptionValue("ss"));
                if (arguments.sliding <= 0) {
                    System.out.println("Window sliding speed must be >= 1");
                    System.exit(1);
                }
            }

            if (cmd.hasOption("e")) {
                int e = Integer.parseInt(cmd.getOptionValue("e"));
                if (e != ENUM_FSMHG && e != ENUM_FSMHG_WIN) {
                    System.out.println("Invalid enumerator");
                    System.exit(1);
                }
                arguments.enumerator = e;
            }

            // if (cmd.hasOption("opt")) {
            //     String opt = cmd.getOptionValue("opt");
            //     if ("YES".equalsIgnoreCase(opt)) {
            //         arguments.optimize = true;
            //     } else if ("NO".equalsIgnoreCase(opt)) {
            //         arguments.optimize = false;
            //     } else {
            //         System.out.println("opt must be YES / NO");
            //         System.exit(1);
            //     }
            // }

            return arguments;
        }
    }
}