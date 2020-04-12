package spider;

public class Test {
    public static void main(String[] args) {
        String url = "https://www.cse.ust.hk";
        int numOfPage = 100;
        Spider spider = new Spider(url);
        spider.BFS(numOfPage);
        spider.printAll("spider_result.txt");
    }
}