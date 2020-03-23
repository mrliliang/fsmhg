package com.liang.fsmhg.enumerator;

import com.liang.fsmhg.Pattern;

import java.util.List;

public interface Operation {
    List<Pattern> join(Pattern p1, Pattern p2);

    List<Pattern> extend(Pattern parent);

    String canonicalCode(Pattern p);
}
