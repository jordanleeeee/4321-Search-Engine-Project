package spider;

import org.htmlparser.beans.LinkBean;
import org.htmlparser.beans.StringBean;
import util.Word;

import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.*;
import java.net.*;
import java.util.*;

public class WebInfoSeeker {
    private String url;

    public WebInfoSeeker(String url) {
        this.url = url;
    }

    /**
     * a page can access only if no need to login and the link is alive
     * @return weather the page can be access
     */
    boolean canAccess() {
        try{
            new URL(url).openStream();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    boolean isHtmlPage() {
        try {
            String connectionType = null;
            while (connectionType == null) {
                connectionType = new URL(url).openConnection().getHeaderField("Content-Type");
            }
            return connectionType.contains("html");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * get all children links of the given url
     * @return list of children links
     */
    public List<String> getChildLinks() {
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

    private String getTitleVer2() {
        //todo can have a better way to do it?
        HTMLEditorKit htmlKit = new HTMLEditorKit();
        HTMLDocument htmlDoc = (HTMLDocument) htmlKit.createDefaultDocument();
        HTMLEditorKit.Parser parser = new ParserDelegator();
        try {
            parser.parse(new InputStreamReader(new URL(url).openStream()),
                    htmlDoc.getReader(0), true);
        } catch (IOException e) {
            System.out.println("take too long, try again");
            return getTitleVer2();
        }
        Object title = htmlDoc.getProperty("title");
        if (title == null) {
            return "NoTitle";
        }
        return title.toString();
    }

    public String getTitle() {
        try {
            InputStream response = new URL(url).openStream();
            Scanner scanner = new Scanner(response);
            String responseBody = scanner.useDelimiter("\\A").next();
            String title;
            title = responseBody.substring(responseBody.indexOf("<title>") + 7, responseBody.indexOf("</title>"));
            return title.replace("\n", "");
        } catch (Exception e) {
            return getTitleVer2();
        }
    }

    public String getLastModificationTime() {
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

    public String getPageSize() {
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

    public Vector<String> getKeywords() {
        String contents = getWords();
        Vector<String> words = new Vector<>();
        StringTokenizer st = new StringTokenizer(contents);
        while (st.hasMoreTokens()) {
            words.add(st.nextToken());
        }

        Vector<String> keywords = new Vector<>();
        for (String oneWord : words) {
            String word = oneWord.toLowerCase();
            if (Word.isMeaningfulWord(word)) {
                word = Word.porterAlgorithm(word);
                keywords.add(word);
            }
        }
        return keywords;
    }
}