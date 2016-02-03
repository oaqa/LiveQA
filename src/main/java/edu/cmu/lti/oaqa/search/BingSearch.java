package edu.cmu.lti.oaqa.search;

import org.apache.commons.codec.binary.Base64;
import org.apache.ws.commons.util.NamespaceContextImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class BingSearch {

    private static boolean VERBOSE = false;

    public static void setVerbose(boolean flag) {
        VERBOSE = flag;
    }

    //TODO: change to JSON when you are not on deadline
    public static String buildRequest(String queryString, String sites, int numResults) throws URISyntaxException, UnsupportedEncodingException {
        // Note that the query should be in single quotes!
        URI QueryURI = new URI("https", null /* user info */,
                "api.datamarket.azure.com", -1 /* port */,
                "/Bing/Search/Web?",
                "Query='" + replaceUnsafeChars(queryString) + " "+ sites + "'&$top=" + numResults + "&$format=atom",
                null /* fragment */);
        return QueryURI.toString();
    }


    public static String replaceUnsafeChars(String input){
//        return input.replaceAll("[%$&+,/:;=?@<>#]+", " ");
        return input.replaceAll("\\W+", " ");
    }

    public static String queryBingApi(String requestURL, String AccountKey) throws IOException, SAXException {
        URL url = new URL(requestURL);
        if (VERBOSE) System.out.println("Connection URL: " + url);
        URLConnection uc = url.openConnection();
        String userpass = AccountKey + ":" + AccountKey;
        String basicAuth = "Basic " + new String(new Base64().encode(userpass.getBytes()));
        uc.setRequestProperty("Authorization", basicAuth);

        BufferedReader br = new BufferedReader(new InputStreamReader(
                uc.getInputStream(), "utf-8"));

        String line;
        StringBuffer sb = new StringBuffer();
        while ((line = br.readLine()) != null) {
            sb.append(line).append('\n');
        }
        br.close();
        String response = sb.toString();

        // When Bing returns an error, it is just a plain string,
        // not an XML starting with tag <feed
        if (!response.substring(0, 10).matches("^\\s*<feed\\s.*")) {
            throw new SAXException("Bing search failed, error: " + response);
        }

        return response;
    }

    public static List<WebSearchResult> parseResponse(String response) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();

        StringReader reader = new StringReader(response);
        InputSource inputSource = new InputSource(reader);
        Document doc = db.parse(inputSource);

        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();

        NamespaceContextImpl ctx = new NamespaceContextImpl();

		/*
         * Prefix mapping for the following XML root tag:
		 *
		 * <feed xmlns:base="https://api.datamarket.azure.com/Data.ashx/Bing/SearchWeb/v1/Web"
		 * xmlns:d="http://schemas.microsoft.com/ado/2007/08/dataservices"
		 * xmlns:m="http://schemas.microsoft.com/ado/2007/08/dataservices/metadata" xmlns="http://www.w3.org/2005/Atom">
		 *
		 * NOTE: the default namespace can use any prefix, not necessarily default.
		 * Yet, exactly the same prefix should also be used in XPATH expressions
		 */
        ctx.startPrefixMapping("base", "https://api.datamarket.azure.com/Data.ashx/Bing/SearchWeb/Web");
        ctx.startPrefixMapping("d", "http://schemas.microsoft.com/ado/2007/08/dataservices");
        ctx.startPrefixMapping("m", "http://schemas.microsoft.com/ado/2007/08/dataservices/metadata");
        ctx.startPrefixMapping("default", "http://www.w3.org/2005/Atom");
        xpath.setNamespaceContext(ctx);

        NodeList nodes = (NodeList) xpath.evaluate("/default:feed/default:entry", doc,
                XPathConstants.NODESET);
        List<WebSearchResult> resultList = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            try {
                Node CurrNode = nodes.item(i);
                String title = (String) xpath.evaluate("default:content/m:properties/d:Title/text()", CurrNode,
                        XPathConstants.STRING);
                String desc = (String) xpath.evaluate("default:content/m:properties/d:Description/text()", CurrNode,
                        XPathConstants.STRING);
                String url = (String) xpath.evaluate("default:content/m:properties/d:Url/text()", CurrNode,
                        XPathConstants.STRING);

                if (!title.isEmpty() || !desc.isEmpty()) {
                    WebSearchResult res = new WebSearchResult(i, url, title, desc);
                    resultList.add(res);
                }
            } catch (XPathExpressionException e) {
                System.err.printf("[ERROR] cannot parse element # %d, ignoring, error: %s\n",
                        i + 1, e.toString());
            }
        }

        if (VERBOSE)
            System.out.println("Bing reply size: " + resultList.size());
        return resultList;
    }

}