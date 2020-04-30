package com.liang.fsmhg.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.liang.fsmhg.Utils;

public class Transform {
    private DateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss Z (z)", Locale.ENGLISH);

    private TreeMap<Date, List<Mail>> mailMap = new TreeMap<>(new Comparator<Date>() {
        @Override
        public int compare(Date o1, Date o2) {
            Calendar c1 = Calendar.getInstance();
            c1.setTime(o1);
            Calendar c2 = Calendar.getInstance();
            c2.setTime(o2);

            if (c1.get(Calendar.YEAR) < c2.get(Calendar.YEAR)) {
                return -1;
            }
            if (c1.get(Calendar.YEAR) > c2.get(Calendar.YEAR)) {
                return 1;
            }
            if (c1.get(Calendar.DAY_OF_YEAR) < c2.get(Calendar.DAY_OF_YEAR)) {
                return -1;
            }
            if (c1.get(Calendar.DAY_OF_YEAR) > c2.get(Calendar.DAY_OF_YEAR)) {
                return 1;
            }

            return 0;
        }
    });

    private HashSet<String> address = new HashSet<>();
    private HashMap<String, Integer> userIds = new HashMap<>();

    private int emailCount = 0;

    private List<Mail> readMail(File file) {
        if (!file.isDirectory()) {
            Transform.this.emailCount++;
            System.out.format("Reading email %d\r", Transform.this.emailCount);
            ArrayList<Mail> mails = new ArrayList<>();
            Mail m = new Mail(file);
            mails.add(m);
            return mails;
        }

        ArrayList<Mail> mails = new ArrayList<>();
        for (File f : file.listFiles()) {
            mails.addAll(readMail(f));
        }
        return mails;
    }

    private void transform(File data, File outDir) {
        List<Mail> mails = readMail(data);
        System.out.println();

        for (Mail m : mails) {
            if (m.from == null || m.toList.isEmpty()) {
                continue;
            }
    
            List<Mail> list = mailMap.computeIfAbsent(m.date, new Function<Date, List<Mail>>() {
                @Override
                public List<Mail> apply(Date t) {
                    return new ArrayList<>();
                }
            });
            list.add(m);

            if (m.from.endsWith("@enron.com")) {
                address.add(m.from);
                userIds.computeIfAbsent(m.from, new Function<String, Integer>() {
                    @Override
                    public Integer apply(String t) {
                        return userIds.size();
                    }
                });
            }
            for (String to : m.toList) {
                if (to.endsWith("@enron.com")) {
                    address.add(to);
                    userIds.computeIfAbsent(to, new Function<String, Integer>() {
                        @Override
                        public Integer apply(String t) {
                            return userIds.size();
                        }
                    });
                }
            }
        }
        
        int count = 0;
        for (Entry<Date, List<Mail>> entry : mailMap.entrySet()) {
            HashMap<Integer, Integer> sendCount = new HashMap<>();
            HashMap<Interaction, Integer> interactionCount = new HashMap<>();
            snapshot(entry.getValue(), sendCount, interactionCount);
            if (sendCount.size() > 0 && interactionCount.size() > 0) {
                count++;
                System.out.format("Transform snapshot %d\r", count);
                output(sendCount, interactionCount, outDir, entry.getKey(), count);
            }
        }
        System.out.println();
        System.out.format("Done! %d snapshots in total.\n", count);
        System.out.format("%d users\n", userIds.size());
        System.out.format("%d address\n", address.size());
    }

    private void snapshot(List<Mail> mails, HashMap<Integer, Integer> sendCount, HashMap<Interaction, Integer> interactionCount) {
        for (Mail m : mails) {
            int fromId = userIds.getOrDefault(m.from, -1);
            for (String to : m.toList) {
                int toId = userIds.getOrDefault(to, -1);
                if (fromId == toId) {
                    continue;
                }
                if (fromId >= 0) {
                    sendCount.put(fromId, sendCount.getOrDefault(fromId, 0) + 1);
                }
                if (fromId < 0 || toId < 0) {
                    continue;
                }
                Interaction inter = new Interaction(fromId, toId);
                if (toId < fromId) {
                    inter.from = toId;
                    inter.to = fromId;
                }
                interactionCount.put(inter, interactionCount.getOrDefault(inter, 0) + 1);
            }
        }
    }

    private void output(HashMap<Integer, Integer> sendCount, HashMap<Interaction, Integer> interactionCount, File outDir, Date d, int transId) {
        TreeMap<Integer, Integer> sendCountOrder = new TreeMap<>(sendCount);
        TreeMap<Interaction, Integer> interactionCountOrder = new TreeMap<>(interactionCount);
        File out = getFile(outDir, d, transId);
        try {
            FileWriter fw = new FileWriter(out);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("t # " + transId);
            for (Entry<Integer, Integer> entry : sendCountOrder.entrySet()) {
                bw.newLine();
                bw.write("v " + entry.getKey() + " " + entry.getValue());
            }
            for (Entry<Interaction, Integer> entry : interactionCountOrder.entrySet()) {
                bw.newLine();
                bw.write("e " + entry.getKey().from + " " + entry.getKey().to + " " + entry.getValue());
            }
            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getFile(File dir, Date d, int count) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        String name = String.format("%04d-%d%02d%02d", count, year, month, day);
        return new File(dir, name);
    }


    private class Mail {
        String msgId;
        Date date;
        String from;
        List<String> toList = new ArrayList<>();

        public Mail(File mail) {
            try {
                FileReader fr = new FileReader(mail);
                BufferedReader br = new BufferedReader(fr);
                String line = br.readLine();
                if (line.startsWith("Message-ID")) {
                    msgId = line.substring("Message-ID: <".length(), line.length() - 1);
                }

                line = br.readLine();
                if (line.startsWith("Date")) {
                    String d = line.substring("Date: ".length());
                    date = format.parse(d);
                }

                line = br.readLine();
                if (line.startsWith("From")) {
                    from = line.substring("From: ".length());
                }

                line = br.readLine();
                StringBuilder tos = new StringBuilder();
                if (line.startsWith("To")) {
                    tos.append(line.substring("To: ".length()));
                    line = br.readLine();
                    while(line != null && !line.startsWith("Subject")) {
                        tos.append(line);
                        line = br.readLine();
                    }
                }
                String str = tos.toString();
                Pattern p = Pattern.compile("\\s*|\t|\r|\n");  
                Matcher m = p.matcher(str);  
                str = m.replaceAll(""); 
                toList = Arrays.asList(str.split(","));
                
                br.close();
                fr.close();
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private class Interaction implements Comparable<Interaction> {
        int from;
        int to;

        public Interaction(int from, int to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof Interaction) {
                Interaction other = (Interaction)obj;
                return this.from == other.from && this.to == other.to;
            }
            return false;
        }

        @Override
        public int compareTo(Interaction other) {
            if (this == other) {
                return 0;
            }
            if (this.from < other.from) {
                return -1;
            }
            if (this.from > other.from) {
                return 1;
            }
            if (this.to < other.to) {
                return -1;
            }
            if (this.to > other.to) {
                return 1;
            }
            return 0;
        }
    }

    public static void main(String[] args) {
        File maildir = new File("/home/liliang/data/maildir");
        File outdir = new File("/home/liliang/data/enron_snapshots");
        Utils.deleteFileDir(outdir);
        outdir.mkdirs();
        new Transform().transform(maildir, outdir);
    }

}