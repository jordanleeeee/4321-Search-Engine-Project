package spider;

import org.htmlparser.beans.LinkBean;
import org.htmlparser.beans.StringBean;

import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.BufferedReader;
import java.io.FileReader;
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
            // remove any junk suffix at the end of url

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

    String getTitle() {
        HTMLEditorKit htmlKit = new HTMLEditorKit();
        HTMLDocument htmlDoc = (HTMLDocument) htmlKit.createDefaultDocument();
        HTMLEditorKit.Parser parser = new ParserDelegator();
        try {
            parser.parse(new InputStreamReader(new URL(url).openStream()),
                    htmlDoc.getReader(0), true);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("this should not happened");
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
            System.out.println("this should not happened");
        }
        URLConnection uCon = null;
        try {
            assert link != null;
            uCon = link.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("this should not happened");
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
            System.out.println("this should not happened");
        }
        HttpURLConnection httpCon = null;
        try {
            assert link != null;
            httpCon = (HttpURLConnection) link.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("this should not happened");
        }

        assert httpCon != null;
        long date = httpCon.getContentLength();
        if (date == -1)
            return getTotalNumOfChar() + " char";
        else
            return date + " bytes";
    }

    private String getWords() {
        StringBean bean = new StringBean();
        bean.setURL(url);
        bean.setLinks(false);
        return bean.getStrings();
    }

    private String getTotalNumOfChar() {
        String contents = getWords();
        contents = contents.replace("\n", "");
        return String.valueOf(contents.length());
    }

    Vector<String> getKeywords() {
        String contents = getWords();
        Vector<String> words = new Vector<>();
        StringTokenizer st = new StringTokenizer(contents);
        while (st.hasMoreTokens()) {
            words.add(st.nextToken());
        }

        Vector<String> Keywords = new Vector<>();
        HashSet<String> stopWords = new HashSet<>();

        try {
            BufferedReader reader = new BufferedReader(new FileReader("stopwords.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                stopWords.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (String oneWord : words) {
            String word = oneWord.toLowerCase();
            if (!(stopWords.contains(word)) && word.length() >= 2 && !Keywords.contains(word)
                    && word.matches("^[a-zA-Z]*$")) {
                Keywords.addElement(word);
            }
        }
        return Keywords;
    }

}