# fsmhg

Historical graph is an emerging data model for representing the evolving process of a dynamic graph. A historical graph can be regarded as a sequence of 
snapshots within a time interval. This project implements two algorithms for mining frequent subgraph patterns on historical graph data. 
The existing frequent subgraph mining algorithms do not perform efﬁciently on historical graphs because they overlook the similarity among snapshots in 
consecutive time points. One algorithm is designed for mining frequent subgraph subgraph patterns on the static historical snapshot sequence. It speeds up 
frequent subgraph mining by leveraging the similarity among consecutive snapshots. Experiments on real-world data show that this algorithm outperforms the 
baseline algorithm by more than 40x. Another algorithm is designed for mining frequent subgraph patterns on the stream of historical snapshots in a 
sliding window. It is an incremental mining approach which don’t have to search all subgraph patterns from scratch when the window moves.
