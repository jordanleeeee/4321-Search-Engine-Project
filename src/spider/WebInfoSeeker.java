package spider;

import org.htmlparser.beans.LinkBean;
import org.htmlparser.beans.StringBean;

import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

class WebInfoSeeker {
    private String url;

    WebInfoSeeker(String url) {
        this.url = url;
    }

    String getTitle() {
        HTMLEditorKit htmlKit = new HTMLEditorKit();
        HTMLDocument htmlDoc = (HTMLDocument) htmlKit.createDefaultDocument();
        HTMLEditorKit.Parser parser = new ParserDelegator();
        try {
            parser.parse(new InputStreamReader(new URL(url).openStream()),
                    htmlDoc.getReader(0), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Object title = htmlDoc.getProperty("title");
        if (title == null) {
            return "NoTitle";
        }
        return title.toString();
    }

    String getLastModificationTime() {
        URL link = null;
        try {
            link = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        URLConnection uCon = null;
        try {
            assert link != null;
            uCon = link.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert uCon != null;
        uCon.setConnectTimeout(5000);   // 5 seconds
        String date = uCon.getHeaderField("Last-Modified");
        if (date == null) {
            date = uCon.getHeaderField("Date");
        }
        return date;
    }

    String getPageSize() {
        URL link = null;
        try {
            link = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        HttpURLConnection httpCon = null;
        try {
            assert link != null;
            httpCon = (HttpURLConnection) link.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert httpCon != null;
        long date = httpCon.getContentLength();
        if (date == -1)
            return getTotalNumOfChar() + " char";
        else
            return date + " bytes";
    }

    String getWords() {
        StringBean bean = new StringBean();
        bean.setURL(url);
        bean.setLinks(false);
        return bean.getStrings();
    }

    private String getTotalNumOfChar() {
        StringBean bean = new StringBean();
        bean.setURL(url);
        bean.setLinks(false);
        String contents = bean.getStrings();
        contents = contents.replace("\n", "");
        return String.valueOf(contents.length());
    }

    /**
     * get all children links of the given url
     * @param url link to the webpage
     * @return list of children links
     */
    static List<String> getChildLinks(String url) {
        List<String> links = new LinkedList<>();
        LinkBean bean = new LinkBean();
        bean.setURL(url);
        URL[] urls = bean.getLinks();
        for (URL link : urls) {
            String l = link.toString();
            while (true) {
                char lastChar = l.charAt(l.length() - 1);
                if( (lastChar >= 'a' && lastChar <= 'z') ||
                        (lastChar >= 'A' && lastChar <= 'Z') ||
                        Character.isDigit(lastChar))   // last char is alphabet or a digit
                    break;
                else
                    l = l.substring(0, l.length()-1);
            }
            links.add(l);
        }
        return links;
    }
}
