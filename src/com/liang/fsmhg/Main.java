package com.liang.fsmhg;

import sun.jvm.hotspot.utilities.BitMap;

import java.util.BitSet;

public class Main {

    public static void main(String[] args) {
	// write your code here

        BitSet bs = new BitSet(1000000);
        bs.set(1);
        bs.get(1);
        System.out.println(1000001 % 64);
        System.out.println(1L << 10000001);

        VP vp = new VP();
        vp.test(0001,"5555","swa",87.845, new String("ssss"), new A());
    }
}

class VP {
    <T> void test(T... pars) {
        for (T e : pars) {
            System.out.println(e);
        }
    }
}

class A {
    @Override
    public String toString() {
        return "A{}";
    }
}