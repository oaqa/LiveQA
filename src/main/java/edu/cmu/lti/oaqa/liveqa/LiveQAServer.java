package edu.cmu.lti.oaqa.liveqa;

import edu.cmu.lti.oaqa.agent.HtmlCachedClient;
import org.apache.commons.lang.StringUtils;
import org.trec.liveqa.TrecLiveQaDemoServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Di Wang.
 */
public class LiveQAServer extends TrecLiveQaDemoServer {

    public static final String PARTICIPANT_ID = "CMU-OAQA";

    protected String participantId() {
        return PARTICIPANT_ID;
    }

    public LiveQAServer(int port) {
        super(port);
    }

    public LiveQAServer() {
        this(DEFAULT_PORT);
    }

    HtmlCachedClient localHtmlClient = new HtmlCachedClient();

    static int TITLE_YA_PAGES = 1;
    static int TITLE_BODY_YA_PAGES = 1;
    static int KEYWORDS_BODY_YA_PAGES = 1;
    static int MAX_ANSWER_LENGTH = 1000;
    static boolean USE_OPINION_HEADER = false;

    @Override
    /**
     * Server's algorithmic payload.
     *
     * @param qid unique question id
     * @param title question title (roughly 10 words)
     * @param body question body (could be empty, could be lengthy)
     * @param category (verbal description)
     * @return server's answer and a list of resources
     * @throws InterruptedException
     */
    protected AnswerAndResources getAnswerAndResources(String qid, String title, String body, String category)
            throws InterruptedException {

        String retAnswer = null;
        String retResources = "";
        try {
            title = removeUrl(title);
            body = removeUrl(body);

            System.out.println("\n<>title = " + title + "   <> category = " + category);
            System.out.println("<>body = " + body + "\n");

            ArrayList<CQAResult> ret_yanswers = new ArrayList<>();

            YAnswerSearcher.searchYAnswers(title, ret_yanswers, TITLE_YA_PAGES, true);
            if (body != null && !body.trim().isEmpty()) {
                YAnswerSearcher.searchYAnswers(title + " " + body, ret_yanswers, TITLE_BODY_YA_PAGES, true);
            }

            //ret_yanswers = rerank(title, body, ret_yanswers);
            //System.out.println(ret_yanswers);

            int optionNum = 1;
            double retScore = -1;
            for (CQAResult ret_yanswer : ret_yanswers) {
                String aarUrl = ret_yanswer.getUrl();
                if (aarUrl != null && !ret_yanswer.getUrl().contains("answers.yahoo.com")
                        && (ret_yanswer.bestAnswer == null || ret_yanswer.bestAnswer.length() <= MAX_ANSWER_LENGTH)) {
                    try {
                        //findAnswerTextFromUrl(ret_yanswer, aarUrl);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (StringUtils.isNotEmpty(ret_yanswer.bestAnswer) && !StringUtils.equals(qid, ret_yanswer.qid)) {
                    if (StringUtils.isBlank(retAnswer)) {
                        retScore = ret_yanswer.getScore();
                        retAnswer = ret_yanswer.bestAnswer;
                        retResources = ret_yanswer.getUrl();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println(String.format("Error for input: %s, %s, %s, %s", qid, title, body, category));
            e.printStackTrace();
        }

        if (retAnswer == null) {
            return null;
        } else {
            retAnswer = retAnswer.replaceAll("\n", " \n");
            AnswerAndResources aar = new AnswerAndResources(retAnswer.substring(0, Math.min(MAX_ANSWER_LENGTH, retAnswer.length())), retResources);
            return aar;
        }

    }

    // Pattern for recognizing a URL, based off RFC 3986
    private static final Pattern urlPattern = Pattern.compile(
            "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)"
                    + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
                    + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    static String removeUrl(String text) {
        if (text == null) return null;

        try {
            Matcher matcher = urlPattern.matcher(text);
            while (matcher.find()) {
                int matchStart = matcher.start(1);
                int matchEnd = matcher.end();
                text = text.substring(0, matchStart) + " " + text.substring(matchEnd);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return text;
    }

    public static void main(String[] args) throws IOException {
        TrecLiveQaDemoServer server =
                new LiveQAServer(args.length == 0 ? DEFAULT_PORT : Integer.parseInt(args[0]));
        server.start();
        while (System.in.read() != 'q') {
        }
        server.stop();
    }
}
