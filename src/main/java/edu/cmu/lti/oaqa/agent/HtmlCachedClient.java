package edu.cmu.lti.oaqa.agent;

import edu.cmu.lti.oaqa.cache.NoCache;
import org.jsoup.Jsoup;

import java.io.IOException;

/**
 * Download html page
 *
 * @author Di Wang
 */
public class HtmlCachedClient extends AbstractCachedFetcher<String> {

    public String fetchOnline(String url) {
        String html = "";
        try {
            html = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.bing.com")
                    .timeout(10000).get().html();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return html;
    }

    public static void main(String[] args) throws IOException {
        HtmlCachedClient client = new HtmlCachedClient();
        System.out.println(client
                .fetchOnline("http://www.tripadvisor.com/ShowTopic-g1-i26969-k6135086-Why_can_t_we_buy_an_airline_seat_for_our_dogs-Traveling_with_Pets.html"));
    }


}
