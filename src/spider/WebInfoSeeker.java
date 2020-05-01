package spider;

import org.htmlparser.beans.LinkBean;
import org.htmlparser.beans.StringBean;
import util.Word;

import java.io.*;
import java.net.*;
import java.util.*;

public class WebInfoSeeker {
    private String url;
    private String words = null;

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
        if (url.contains(".bib") || url.contains(".pdf") || url.contains(".doc")) {
            return false;
        }
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

    public String getTitle() {
        try {
            InputStream response = new URL(url).openStream();
            Scanner scanner = new Scanner(response);
            String responseBody = scanner.useDelimiter("\\A").next();
            responseBody = responseBody.toLowerCase(Locale.ROOT);
            if (!responseBody.contains("<title>")) {
                return "NoTitle";
            }
            String title;
            title = responseBody.substring(responseBody.indexOf("<title>") + 7, responseBody.indexOf("</title>"));
            return title.replace("\n", "");
        } catch (Exception e) {
            e.printStackTrace();
            return "NoTitle";
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

    private void getWords() {
        if (words == null) {
            StringBean bean = new StringBean();
            bean.setURL(url);
            bean.setLinks(false);
            words = bean.getStrings();
        }
    }

    private String getTotalNumOfChar() {
        getWords();
        String contents = words;
        contents = contents.replace("\n", "");
        return String.valueOf(contents.length());
    }

    public Vector<String> getKeywords() {
        getWords();
        String contents = words;
        Vector<String> words = new Vector<>();
        StringTokenizer st = new StringTokenizer(contents);
        while (st.hasMoreTokens()) {
            words.add(st.nextToken());
        }

        Vector<String> keywords = new Vector<>();
        for (String oneWord : words) {
            if (Word.isMeaningfulWord(oneWord)) {
                oneWord = Word.porterAlgorithm(oneWord);
                keywords.add(oneWord);
            }
        }
        return keywords;
    }
}