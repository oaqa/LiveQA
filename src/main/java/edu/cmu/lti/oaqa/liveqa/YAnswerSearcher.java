package edu.cmu.lti.oaqa.liveqa;

import edu.cmu.lti.oaqa.agent.HtmlCachedClient;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Di Wang.
 */
public class YAnswerSearcher {

    private static final String URL_PREFIX = "https://answers.yahoo.com/search/search_result?p=";
    public static final String QID_URL_PREFIX = "https://answers.yahoo.com/question/index?qid=";

    static HtmlCachedClient htmlCachedClient = new HtmlCachedClient();

    public static void main(String[] args) throws Exception {
        String question = "How often should you buy a new mattress?";
        ArrayList<CQAResult> ret_yanswers = new ArrayList<>();
        searchYAnswers(question, ret_yanswers, 1, true);
        for (CQAResult ret_yanswer : ret_yanswers) {
            System.out.println(ret_yanswer);
        }
    }

    public static void searchYAnswers(String query, ArrayList<CQAResult> ret_yanswers, int pages, boolean parsePages) throws UnsupportedEncodingException {

        String ya_search_url = URL_PREFIX + URLEncoder.encode(query, "utf-8").replace("+", "%20");
        ExecutorService executor = Executors.newFixedThreadPool(4); // NUM_THREADS

        HashSet<String> qids = new HashSet<>();
        for (CQAResult ret_yanswer : ret_yanswers) {
            qids.add(ret_yanswer.qid);
        }

        //search within YAnswer
        for (int i = 1; i <= pages; i++) {
            String htmlStr = htmlCachedClient.fetch(ya_search_url + "&s=" + i);

            if (htmlStr.isEmpty()) {
                System.out.println("empty html page: " + ya_search_url + "&s=" + i);
                htmlStr = htmlCachedClient.fetch(ya_search_url + "&s=" + i, true);
            }

            Document doc = Jsoup.parse(htmlStr);
            Element html = doc.child(0);

            Element questions = html.getElementById("ya-search");
            if (questions != null) {
                Elements questionTitles = questions.getElementsByClass("title");
                for (Element qtitleEle : questionTitles) {
                    Elements qLink = qtitleEle.select("a");
                    String href = qLink.attr("href");
                    String qid = extractQid(href);

                    if (qids.contains(qid)) {
                        continue;
                    } else {
                        qids.add(qid);
                    }

                    CQAResult yqa = new CQAResult();
                    yqa.qid = qid;
                    yqa.subject = qLink.text();
                    ret_yanswers.add(yqa);

                    if (parsePages) {
                        executor.execute(new YAPageExtractRunnable(yqa));
                    }
                }
                if (questionTitles.size() < 10) {
                    break;
                }
            }
        }

        executor.shutdown();
        try {
            executor.awaitTermination(50, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static String extractQid(String url) {
        int begin = url.indexOf("qid=") + 4;
        int end = url.indexOf("&", begin);
        return url.substring(begin, end > 0 ? end : url.length());
    }

    static class YAPageExtractRunnable implements Runnable {
        CQAResult yqa;

        public YAPageExtractRunnable(CQAResult yqa) {
            this.yqa = yqa;
        }

        @Override
        public void run() {
            String htmlStr = htmlCachedClient.fetch(yqa.getUrl());
            extractYAnswersPropertiesFromHtml(htmlStr, yqa);
        }
    }

    static int extract_rating(Element element) {
        int rating = -1;
        Elements asker_ratings = element.getElementsByAttributeValueEnding("alt", "out of 5");
        if (asker_ratings != null && asker_ratings.size() == 1) {
            rating = Integer.parseInt(asker_ratings.get(0).attr("alt").substring(0, 1));
        }
        return rating;
    }

    public static void extractYAnswersPropertiesFromHtml(String htmlStr, CQAResult yqa) {
        Document doc = Jsoup.parse(htmlStr);
        Element body = doc.body();

        Element head = doc.head();
        //get title
        Elements titleMeta = head.getElementsByAttributeValue("name", "title");
        if (titleMeta.size() > 0) {
            yqa.subject = titleMeta.get(0).attr("content");
        } else {
            Elements titleOgMeta = head.getElementsByAttributeValue("property", "og:title");
            if (titleOgMeta.size() > 0) {
                yqa.subject = titleOgMeta.get(0).attr("content");
            } else {
                System.out.printf(htmlStr);
                System.err.println("Can't parse question subject: " + yqa.qid);
            }
        }

        Elements full_text = null;
        Element ya_best_answer = body.getElementById("ya-best-answer");
        if (ya_best_answer != null) {
            full_text = ya_best_answer.getElementsByClass("ya-q-full-text");
            yqa.bestAnswerAskerRating = extract_rating(ya_best_answer);
        } else {
            for (Element class_ya_best_answer : body.getElementsByClass("ya-best-answer")) {
                full_text = class_ya_best_answer.getElementsByClass("ya-q-full-text");
                if (full_text != null) {
                    yqa.bestAnswerAskerRating = extract_rating(class_ya_best_answer);
                    break;
                }
            }
        }

        if (full_text != null && full_text.size() > 0) {
            yqa.bestAnswer = full_text.get(0).text();
        }

        Element other_answer = body.getElementById("ya-qn-answers");
        if (other_answer != null) {
            ArrayList<String> otherAnswers = new ArrayList<>();
            for (Element class_other_answer : other_answer.getElementsByClass("ya-other-answer")) {
                class_other_answer.getAllElements();
                for (Element other_a_text : class_other_answer.getElementsByClass("ya-a-text")) {
                    otherAnswers.add(other_a_text.text());
                    break;
                }
            }
            yqa.otherAnswers = otherAnswers;
        }

        Elements topCategory = body.select("div#brdCrb>a.Clr-b:first-child");
        if (topCategory.size() == 1) {
            yqa.topCategory = topCategory.attr("title");
        }

        Element ya_question_detail = body.getElementById("ya-question-detail");
        if (ya_question_detail != null) {
            String qid = ya_question_detail.attr("data-ya-question-id");
            if (qid == null || qid.isEmpty()) {
                Elements linkCanonical = head.getElementsByAttributeValue("rel", "canonical");
                if (linkCanonical.size() > 0) {
                    String href = linkCanonical.get(0).attr("href");
                    int indexOfQid = href.indexOf("qid=");
                    if (indexOfQid > 0) {
                        qid = href.substring(indexOfQid + 4);
                    }
                }
            }
            if (yqa.qid == null) {
                yqa.qid = qid;
            } else if (!yqa.qid.equals(qid)) {
                System.err.println("Provided QID is not consistent: " + yqa.qid + " " + qid);
            }


            Elements ya_q_content = body.getElementsByClass("ya-q-text");
            if (ya_q_content != null && ya_q_content.size() > 0) {
                yqa.content = ya_q_content.get(0).text();
            }
        }

        if (StringUtils.isNotEmpty(yqa.content) && yqa.content.endsWith("...")) {
            Elements descOgMeta = head.getElementsByAttributeValue("property", "og:description");
            if (descOgMeta.size() > 0) {
                String description = descOgMeta.get(0).attr("content");
                if (description.length() > yqa.content.length()) {
                    yqa.content = description;
                }
            }
        }


    }
}
