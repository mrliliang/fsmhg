package com.liang.fsmhg;

import com.liang.fsmhg.code.DFSCode;

public class PointPattern extends Pattern {

    private int label;

    public PointPattern(int label) {
        super(null, null);
        this.label = label;
    }

    public int label() {
        return label;
    }

    public DFSCode code() {
        return null;
    }

    @Override
    public boolean checkMin() {
        return true;
    }

    

}
