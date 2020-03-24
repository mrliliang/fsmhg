package com.liang.fsmhg;

import java.util.*;

public class Pattern {

    private Map<Long, List<Embedding>> embeddingMap;
    private Map<Long, List<Embedding>> intersectionEmbeddings;
    private Map<Long, List<Embedding>> borderEmbeddings;

    public Pattern() {
        embeddingMap = new TreeMap<>();
        intersectionEmbeddings = new TreeMap<>();
        borderEmbeddings = new TreeMap<>();
    }

    public int support() {
        HashSet<Long> graphIds = new HashSet<>();
        graphIds.addAll(embeddingMap.keySet());
        graphIds.addAll(intersectionEmbeddings.keySet());
        graphIds.addAll(borderEmbeddings.keySet());
        return graphIds.size();
    }

    public List<Embedding> embeddings(long graphId) {
        return embeddingMap.get(graphId);
    }

    public void addEmbedding(long graphId, Embedding em) {
        List<Embedding> embeddings = embeddingMap.computeIfAbsent(graphId, aLong -> new ArrayList<>());
        embeddings.add(em);
    }

    public void addIntersectionEmbedding(long graphId, Embedding em) {
        List<Embedding> embeddings = intersectionEmbeddings.computeIfAbsent(graphId, aLong -> new ArrayList<>());
        embeddings.add(em);
    }

    public void addBorderEmbedding(long graphId, Embedding em) {
        List<Embedding> embeddings = borderEmbeddings.computeIfAbsent(graphId, aLong -> new ArrayList<>());
        embeddings.add(em);
    }
}
