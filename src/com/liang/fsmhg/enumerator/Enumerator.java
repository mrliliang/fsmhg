package com.liang.fsmhg.enumerator;

import com.liang.fsmhg.Cluster;
import com.liang.fsmhg.Embedding;
import com.liang.fsmhg.Pattern;
import com.liang.fsmhg.PointPattern;
import com.liang.fsmhg.code.DFSEdge;
import com.liang.fsmhg.graph.LabeledVertex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Enumerator {

    public List<PointPattern> points(Map<Integer, PointPattern> oldPoints, List<Cluster> clusters) {
        List<PointPattern> newPoints = new ArrayList<>();
        for (Cluster c : clusters) {
            intersectionPoints(c, oldPoints, newPoints);
            otherPoints(c, oldPoints, newPoints);
        }
        return newPoints;
    }

    private void intersectionPoints(Cluster c, Map<Integer, PointPattern> oldPoints, List<PointPattern> newPoints) {
        Cluster.Intersection inter = c.intersection();
        Map<Integer, LabeledVertex> border = c.border();
        for (LabeledVertex v : inter.vertices()) {
            PointPattern pattern = oldPoints.get(v.id());
            if (pattern == null) {
                pattern = new PointPattern(inter.vLabel(v));
                pattern.addCluster(c);
                oldPoints.put(pattern.label(), pattern);
            }
            newPoints.add(pattern);
            Embedding em = new Embedding(v, null);
            if (isBorderEmbedding(em, border)) {
                pattern.addBorderEmbedding(c, em);
            } else {
                pattern.addIntersectionEmbedding(c, em);
            }
        }
    }


    private void otherPoints(Cluster c, Map<Integer, PointPattern> oldPoints, List<PointPattern> newPoints) {
        for (Cluster.DeltaGraph g : c.deltaGraphs()) {
            Map<Integer, LabeledVertex> border = g.border();
            for (LabeledVertex v : g.vertices()) {
                if (border.containsKey(v.id())) {
                    continue;
                }
                PointPattern pattern = oldPoints.get(v.id());
                if (pattern == null) {
                    pattern = new PointPattern(g.vLabel(v));
                    oldPoints.put(pattern.label(), pattern);
                }
                newPoints.add(pattern);
                Embedding em = new Embedding(v, null);
                pattern.addEmbedding(g.graphId(), em);
            }
        }
    }

    public List<Pattern> edges(List<PointPattern> newPoints, Map<DFSEdge, Pattern> oldEdges, List<Cluster> clusters) {
        List<Pattern> newEdges = new ArrayList<>();
        for (Cluster c : clusters) {
            intersectionEdges(c, oldEdges, newPoints, newEdges);
            otherEdges(c, oldEdges, newPoints, newEdges);
        }
        return newEdges;
    }

    private void intersectionEdges(Cluster c, Map<DFSEdge, Pattern> oldEdges, List<PointPattern> newPoints, List<Pattern> newEdges) {

    }

    private void otherEdges(Cluster c, Map<DFSEdge, Pattern> oldEdges, List<PointPattern> newPoints, List<Pattern> newEdges) {

    }

    private boolean isBorderEmbedding(Embedding em, Map<Integer, LabeledVertex> border) {
        for (LabeledVertex v : em.vertices()) {
            if (border.containsKey(v.id())) {
                return true;
            }
        }
        return false;
    }

}
