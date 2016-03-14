package edu.cmu.lti.oaqa.index;

import edu.cmu.lti.oaqa.liveqa.CQAResult;
import edu.cmu.lti.oaqa.search.WordNetExpansion;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Di Wang.
 */
public class QaPairIndex {

    public static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();

    public static final String QID = "uri";
    public static final String BEST_ANSWER_FIELD = "best_answer";
    public static final String Q_DESCRIPTION = "Q_DESCRIPTION";

    public static final Analyzer ANALYZER = new EnglishAnalyzer();

    protected static final boolean USE_PROXIMITY_QUERY = false;

    //create index for YA corpus
    //mvn exec:java -Dexec.mainClass=edu.cmu.lti.oaqa.liveqa.index.QaPairIndex /path/to/ya.xml /path/to/index_folder/
    public static void main(String[] args) throws IOException, XMLStreamException {
        assert args.length == 2;
        String filePath = args[0];
        String indexPath = args[1];
        List<CQAResult> qaSetList = YahooAnswerXmlReader.parseMannerXml(filePath);
        System.out.println("Num of QA Sets = " + qaSetList.size());
        Directory dir = FSDirectory.open(new File(indexPath));
        QaPairIndex.createIndexQ(qaSetList, dir);
    }

    public static void createIndexQ(List<CQAResult> QASetList, Directory dir) {
        System.out.println("Creating Questions Index");
        IndexWriterConfig iwc = new IndexWriterConfig(ANALYZER.getVersion(), ANALYZER);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        try {
            IndexWriter writer = new IndexWriter(dir, iwc);
            int id = 0; //XXX seq_id
            for (CQAResult qaSet : QASetList) {
                Document doc = new Document();
                if (qaSet.subject == null) {
                    id++;
                    continue;
                }
                doc.add(new IntField(QID, id++, Field.Store.YES));
                doc.add(new TextField(BEST_ANSWER_FIELD, qaSet.subject, Field.Store.NO));
                doc.add(new TextField(Q_DESCRIPTION, qaSet.content, Field.Store.NO));
                writer.addDocument(doc);
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static BooleanQuery buildQuery(String text, String field) throws IOException {
        BooleanQuery fullQuery = new BooleanQuery();
        TokenStream tokStream = ANALYZER.tokenStream(field, text);
        try {
            tokStream.reset();
            Term lastTerm = null;
            while (tokStream.incrementToken()) {
                String termStr = tokStream.getAttribute(CharTermAttribute.class).toString();

                Term term = new Term(field, termStr);
                TermQuery termQuery = new TermQuery(term);
                termQuery.setBoost(0.8f);
                fullQuery.add(termQuery, BooleanClause.Occur.SHOULD);

                List<String> expansionWords = WordNetExpansion.getStrictExpansion(termStr);
                if (expansionWords != null) {
                    for (String expTermStr : expansionWords) {
                        TermQuery expTermQuery = new TermQuery(new Term(field, expTermStr));
                        expTermQuery.setBoost(0.4f);
                        fullQuery.add(expTermQuery, BooleanClause.Occur.SHOULD);
                    }
                }

                if (lastTerm != null) {
                    PhraseQuery bigram = new PhraseQuery();
                    bigram.add(lastTerm);
                    bigram.add(term);
                    bigram.setBoost(0.1f);
                    fullQuery.add(bigram, BooleanClause.Occur.SHOULD);

                    if (USE_PROXIMITY_QUERY) {
                        SpanNearQuery spanNearQuery = new SpanNearQuery(new SpanQuery[]{
                                new SpanTermQuery(lastTerm),
                                new SpanTermQuery(term)},
                                6,
                                false);
                        spanNearQuery.setBoost(0.1f);
                        fullQuery.add(spanNearQuery, BooleanClause.Occur.SHOULD);
                    }
                }
                lastTerm = term;
            }
            tokStream.end();
        } finally {
            tokStream.close();
        }
        return fullQuery;
    }

}
