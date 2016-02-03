package edu.cmu.lti.oaqa.liveqa;

import org.apache.lucene.search.TopDocs;

import java.util.ArrayList;

public class CQAResult implements Comparable<CQAResult> {
    public String subject;

    public String content = "";

    public String bestAnswer;

    public ArrayList<String> otherAnswers = null;

    public int bestAnswerAskerRating = -1;

    public String topCategory;

    public String qid;

    public double getScore() {
        return score;
    }

    public double score;

    public String getUrl() {
        if (url == null && qid != null) {
            url = YAnswerSearcher.QID_URL_PREFIX + qid;
        }
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    String url;

    transient TopDocs topDocs;

    @Override
    public String toString() {
        return "CQAResult{" +
                "subject='" + subject + '\'' +
                ", content='" + content + '\'' +
                ", bestAnswer='" + (bestAnswer != null ? bestAnswer.substring(0, Math.min(1000, bestAnswer.length())) : "") + '\'' +
                ", score=" + score +
                ", url='" + url + '\'' +
                '}';
    }

    @Override
    public int compareTo(CQAResult o) {
        return Double.compare(o.getScore(), getScore());
    }

}
