package edu.cmu.lti.oaqa.search;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Web Search Result Bean
 *
 * @author Di Wang
 */

public class WebSearchResult implements Serializable {

    private static final long serialVersionUID = 1L;
    String url;
    int rank;

    public String getCorrectedQuery() {
        return correctedQuery;
    }

    public void setCorrectedQuery(String correctedQuery) {
        this.correctedQuery = correctedQuery;
    }

    String correctedQuery;

    private String desc;
    private String title;

    public Map<String, List<Map<String, String>>> getPageMap() {
        return pageMap;
    }

    public void setPageMap(Map<String, List<Map<String, String>>> pageMap) {
        this.pageMap = pageMap;
    }

    Map<String, List<Map<String, String>>> pageMap;

    public WebSearchResult(int rank, String url, String title, String desc) {
        this.title = title;
        this.url = url;
        this.rank = rank;
        this.desc = desc;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getURL() {
        return url;
    }

    @Override
    public String toString() {
        return "WebSearchResult{" +
                "rank=" + rank +
                ", url='" + url + '\'' +
                ", title='" + title + '\'' +
                ", desc='" + desc + '\'' +
                ", pageMap=" + pageMap +
                '}';
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

}
