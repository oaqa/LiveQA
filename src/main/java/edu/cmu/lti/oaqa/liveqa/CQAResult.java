package edu.cmu.lti.oaqa.liveqa;

import java.util.ArrayList;

public class CQAResult implements Comparable<CQAResult> {
    public String subject;

    public String content = "";

    public String bestAnswer;

    public ArrayList<String> otherAnswers = null;

    public int bestAnswerAskerRating = -1;

    public String topCategory;

    public String qid;

    public double qq_score;

    public double dd_score;

    public double getScore() {
        return qq_score + dd_score;
    }

//    public double score;

    public String getUrl() {
        if (url == null && qid != null) {
            url = YAnswerSearcher.QID_URL_PREFIX + qid;
        }
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String url;

    @Override
    public String toString() {
        return "CQAResult{" +
                "subject='" + subject + '\'' +
                ", content='" + content + '\'' +
                ", bestAnswer='" + (bestAnswer != null ? bestAnswer.substring(0, Math.min(1000, bestAnswer.length())) : "") + '\'' +
                ", score=" + getScore() +
                ", url='" + url + '\'' +
                '}';
    }

    @Override
    public int compareTo(CQAResult o) {
        return Double.compare(o.getScore(), getScore());
    }

}
