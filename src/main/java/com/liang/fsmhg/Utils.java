package com.liang.fsmhg;

import java.io.File;

public class Utils {
    public static void deleteFileDir(File dir) {
        if (!dir.isDirectory()) {
            dir.delete();
            return;
        }
        for (File file : dir.listFiles()) {
            deleteFileDir(file);
        }
    }
}