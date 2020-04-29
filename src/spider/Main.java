package spider;

import util.Converter;

public class Main {
    public static void main(String[] args) {
        String url = "https://www.cse.ust.hk";
        int numOfPage = 500;
        Spider spider1 = new Spider(url);
        spider1.BFS(numOfPage);
        spider1.printAll("spider_result.txt");

//        continue fetch page base on previous not yet fetched child links
//        can un-comment the following line
//        Spider spider2 = new Spider(Converter.readRemainingQueue("remainingQueue.txt"));
//        spider2.BFS(200);
//        spider2.printAll("spider_result.txt");
    }
}
