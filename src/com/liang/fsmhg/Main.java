package com.liang.fsmhg;

import com.liang.fsmhg.graph.Graph;


import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

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

        ReturnList<A> rl = new ReturnList<>();
        Graph graph = new Graph(1);
//        StaticGraph sg = new StaticGraph(1);

        BitSet bitSet = new BitSet();
        bitSet.set(1);
        bitSet.set(100);
        System.out.println(bitSet.size());
        System.out.println(bitSet.get(1));
        System.out.println(bitSet.get(100));
        System.out.println(bitSet.get(129));
        bitSet.set(200);
        System.out.println(bitSet.size());
        System.out.println(bitSet.get(200));

        bitSet.set(1000000000);
        bitSet.set(51332);
        System.out.println(bitSet.size());
        System.out.println(bitSet.get(51332));

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

class ReturnList<T extends A> {
    private List<T> list = new ArrayList<>();

    List<T> list() {
        return new ArrayList<>();
    }

    T item() {
        return list.get(0);
    }
}