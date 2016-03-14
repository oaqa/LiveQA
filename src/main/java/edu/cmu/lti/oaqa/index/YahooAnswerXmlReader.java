package edu.cmu.lti.oaqa.index;

import edu.cmu.lti.oaqa.liveqa.CQAResult;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * @author Di Wang.
 */
public class YahooAnswerXmlReader {

    public static List<CQAResult> parseMannerXml(String filePath)
            throws IOException, XMLStreamException {

        filePath = filePath.replaceFirst("^~", System.getProperty("user.home"));
        System.out.println("Parsing Input: " + filePath);

        List<CQAResult> QASetList = null;
        CQAResult currQASet = null;
        String tagContent = null;

        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_COALESCING, true);

        InputStream fileStream = new FileInputStream(filePath);
        if (filePath.endsWith(".gz")) {
            fileStream = new GZIPInputStream(fileStream, 65536);
        }

        XMLStreamReader reader = factory.createXMLStreamReader(fileStream);

        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {

                case XMLStreamConstants.START_ELEMENT:
                    if ("vespaadd".equals(reader.getLocalName())) {
                        currQASet = new CQAResult();
                    }
                    if ("ystfeed".equals(reader.getLocalName())) {
                        QASetList = new ArrayList<>();
                    }
                    break;

                case XMLStreamConstants.CHARACTERS:
                    tagContent = reader.getText().trim();
                    break;

                case XMLStreamConstants.END_ELEMENT:
                    switch (reader.getLocalName()) {
                        case "vespaadd":
                            QASetList.add(currQASet);
                            break;
                        case "subject":
                            currQASet.subject = tagContent;
                            break;
                        case "content":
                            currQASet.content = tagContent;
                            break;
                        case "bestanswer":
                            currQASet.bestAnswer = tagContent;
                            break;
                        case "uri":
                            currQASet.url = tagContent;
                            break;
                    }
                    break;
            }
        }
        return QASetList;
    }
}

