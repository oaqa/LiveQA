package edu.cmu.lti.oaqa.rank;

import edu.cmu.lti.oaqa.index.QaPairIndex;
import edu.cmu.lti.oaqa.liveqa.CQAResult;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;

/**
 * @author Di Wang.
 */
public class LuceneReRank {

    private static final Analyzer ANALYZER = QaPairIndex.ANALYZER;
    private static final String Q_TITLE = QaPairIndex.BEST_ANSWER_FIELD;
    protected static final String Q_DESCRIPTION = QaPairIndex.Q_DESCRIPTION;
    protected static final String RE_RANK_ID = "ReRankID";
    protected static final String RE_RANK_OFFSET = "RE_RANK_OFFSET";

    protected static final String DEFAULT_INDEX_LOCATION = "/tmp/index";

    private Directory dir;
    private IndexWriter writer;
    Similarity similarity;

    HashSet<String> fieldToLoad = new HashSet<>();

    public LuceneReRank() throws IOException {
        this(DEFAULT_INDEX_LOCATION);
    }

    public LuceneReRank(String indexLocation) throws IOException {
        dir = FSDirectory.open(new File(indexLocation));
        IndexWriterConfig iwc = new IndexWriterConfig(ANALYZER.getVersion(), ANALYZER);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
        writer = new IndexWriter(dir, iwc);
        float K1 = (float) 1.0;
        float B = (float) 0.75;
        //NOTE: Leo mentioned that lucene's bm25 calculation could be not accurate
        similarity = new BM25Similarity(K1, B);
        fieldToLoad.add(RE_RANK_OFFSET);
    }

    public ArrayList<CQAResult> rerank(String title, String body, ArrayList<CQAResult> candidates) throws IOException {
        if (candidates == null || candidates.size() == 0) {
            return candidates;
        }

        //1. add candidate to existing index
        String reRankID = DigestUtils.sha256Hex(title) + (new Random()).nextDouble();
        for (int i = 0; i < candidates.size(); i++) {
            CQAResult candidateCQA = candidates.get(i);
            if (candidateCQA.subject == null) {
                System.err.println("null subject: " + title);
                continue;
            }
            Document doc = new Document();
            doc.add(new StringField(RE_RANK_ID, reRankID, Field.Store.YES));
            doc.add(new IntField(RE_RANK_OFFSET, i, Field.Store.YES));
            doc.add(new TextField(Q_TITLE, candidateCQA.subject, Field.Store.NO));
            doc.add(new TextField(Q_DESCRIPTION, candidateCQA.subject + " " + candidateCQA.content, Field.Store.NO));
            writer.addDocument(doc);
        }
        writer.commit();

        //2. rank candidate within existing index
        DirectoryReader ireader = DirectoryReader.open(dir);
        IndexSearcher searcher = new IndexSearcher(ireader);
        searcher.setSimilarity(similarity);

        Filter rrIdFilter = new FieldCacheTermsFilter(RE_RANK_ID, reRankID);

        int inputSize = candidates.size();

        BooleanQuery qqQuery = QaPairIndex.buildQuery(title, Q_TITLE);
        BooleanQuery ddQuery = QaPairIndex.buildQuery(title + " " + body, Q_DESCRIPTION);

        for (ScoreDoc scoreDoc : searcher.search(qqQuery, rrIdFilter, inputSize).scoreDocs) {
            String originalRank = searcher.doc(scoreDoc.doc, fieldToLoad).get(RE_RANK_OFFSET);
            if (originalRank != null) {
                CQAResult retQA = candidates.get(Integer.parseInt(originalRank));
                retQA.qq_score = scoreDoc.score;
            }
        }

        for (ScoreDoc scoreDoc : searcher.search(ddQuery, rrIdFilter, inputSize).scoreDocs) {
            String originalRank = searcher.doc(scoreDoc.doc, fieldToLoad).get(RE_RANK_OFFSET);
            if (originalRank != null) {
                CQAResult retQA = candidates.get(Integer.parseInt(originalRank));
                retQA.dd_score = scoreDoc.score;
            }
        }

        Collections.sort(candidates);

        //3. remove rank candidate from index
        writer.deleteDocuments(new Term(RE_RANK_ID, reRankID));
        ireader.close();

        return candidates;
    }

}
