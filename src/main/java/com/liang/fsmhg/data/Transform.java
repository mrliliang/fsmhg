package com.liang.fsmhg.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Map.Entry;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import javax.xml.soap.MessageFactory;

import com.opencsv.CSVReaderHeaderAware;

public class Transform {
    private static int count = 0;
    private static HashSet<String> messageIds = new HashSet<>();
    private static DateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss Z (z)", Locale.ENGLISH);

    public static void main(String[] args) {
        File maildir = new File("/home/liliang/data/maildir");
        readMail(maildir);
        System.out.println("Total number of mails " + count);
        System.out.println("Total unique mails " + messageIds.size());

        String d = "Wed, 13 Dec 2000 06:17:00 -0800 (PST)";
        DateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss Z (z)", Locale.ENGLISH);
        try {
            Date date = format.parse(d);
            System.out.println(format.format(date));
            System.out.println(d);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void readMail(File file) {
        if (!file.isDirectory()) {
            count++;
            Mail m = new Mail(file);
            messageIds.add(m.msgId);
            return;
        }

        for (File f : file.listFiles()) {
            readMail(f);
        }
    }

    private static class Mail {
        String msgId;
        Date date;
        String from;
        String to;

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
                if (line.startsWith("To")) {
                    to = line.substring("To: ".length());
                }
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
}