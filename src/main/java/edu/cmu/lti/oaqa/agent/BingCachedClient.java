package edu.cmu.lti.oaqa.agent;

import edu.cmu.lti.oaqa.search.BingSearch;
import edu.cmu.lti.oaqa.search.WebSearchResult;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Di Wang.
 */
public class BingCachedClient extends AbstractCachedFetcher<String> {

    static final String[] accountKeys = { };  //signup and get key from: https://datamarket.azure.com/dataset/bing/searchweb
    protected static final int NUM_RESULTS = 20;

    Random rand = new Random();

    @Override
    public String fetchOnline(String requestUrl) {
        int ki = rand.nextInt(accountKeys.length);
        String output = null;
        try {
            output = BingSearch.queryBingApi(requestUrl, accountKeys[ki]);
        } catch (SAXException | IOException e) {
            e.printStackTrace();
        }
        return output;
    }

    public List<WebSearchResult> query(String query, String sites) {
        try {
            String url = BingSearch.buildRequest(query, sites, NUM_RESULTS);
            String response = fetch(url);
            return BingSearch.parseResponse(response);
        } catch (Exception e) {
            System.err.println("bing query failed: " + query);
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void main(String[] args) {
        BingCachedClient bing = new BingCachedClient();
        try {
            List<WebSearchResult> results = bing.query("Why do I keep losing my mom room's internet connection?", "");
            for (WebSearchResult result : results) {
                System.out.println("result = " + result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
