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
import java.net.*;
import java.util.*;

class WebInfoSeeker {
    private String url;
    private static HashSet<String> stopWordsList = new HashSet<>();

    static {
        try (BufferedReader reader = new BufferedReader(new FileReader("stopwords.txt"))){
            String line;
            while ((line = reader.readLine()) != null) {
                stopWordsList.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    WebInfoSeeker(String url) {
        this.url = url;
    }

    /**
     * get all children links of the given url
     * @return list of children links
     */
    List<String> getChildLinks() {
        List<String> links = new LinkedList<>();
        LinkBean bean = new LinkBean();
        bean.setURL(url);
        URL[] urls = bean.getLinks();
        for (URL link : urls) {
            String l = link.toString();
            //ignore not cse web page
            if(!l.contains("cse.ust.hk")){
                continue;
            }
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
            //todo can ignore .pdf .png .......??
            links.add(l);
        }
        return links;
    }

    String getTitle() {
        //todo can have a better way to do it?
        HTMLEditorKit htmlKit = new HTMLEditorKit();
        HTMLDocument htmlDoc = (HTMLDocument) htmlKit.createDefaultDocument();
        HTMLEditorKit.Parser parser = new ParserDelegator();
        try {
            parser.parse(new InputStreamReader(new URL(url).openStream()),
                    htmlDoc.getReader(0), true);
        } catch (IOException e) {
            if (e instanceof ConnectException){
                return getTitle();
            }
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
        String date = uCon.getHeaderField("Last-Modified");
        if (date == null) {
            date = uCon.getHeaderField("Date");
            if (date == null) {
                return getLastModificationTime();
            }
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

        Vector<String> keywords = new Vector<>();
        for (String oneWord : words) {
            String word = oneWord.toLowerCase();
            if (!(stopWordsList.contains(word)) && word.length() >= 2 && word.matches("^[a-zA-Z]*$")) {
                keywords.add(word);
            }
        }
        return keywords;
    }
}