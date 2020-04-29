package retriever;


import indexer.Indexer;
import indexer.InvertedIndex;
import util.Word;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Retrieval {

    private Indexer indexer = Indexer.getInstance();
    private InvertedIndex invertedIndex = InvertedIndex.getInstance();

    public Retrieval(String query) {

        Set<String> afterProcessQuery = processQuery(query);

        if (!afterProcessQuery.isEmpty()) {
            HashMap<Integer, Double> allResultList = cosineSimilarity(afterProcessQuery);
            LinkedHashMap<Integer, Double> Top50Result = RetrievalTop50(allResultList);
            printAll(Top50Result);
        }
    }

    private Set<String> processQuery(String query){
        Set<String> set = new HashSet<>();

        Matcher matcher = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(query);
        while (matcher.find()) {
            String modifiedWord = matcher.group(1).replace("\"", ""); // Add .replace("\"", "") to remove surrounding quotes.
            if (modifiedWord.contains(" "))
                set.add(modifiedWord); //if phrase, directly add to set
            else if (Word.isMeaningfulWord(modifiedWord))
                set.add(Word.porterAlgorithm(modifiedWord));
        }
        System.out.println("The query word: " + set);
        return set;
    }


    private HashMap<Integer, Double> cosineSimilarity(Set<String> afterProcessQuery){
        HashMap<Integer, Double> allResultList = new HashMap<>();

        for (String queryWord : afterProcessQuery) {
            Integer currentWordID = indexer.searchIDByWord(queryWord, false);
            if (currentWordID == -1) {
                continue;
            }

            Set<Integer> pageIDs = invertedIndex.getRelatedPage(currentWordID);

            for (Integer pageID : pageIDs) {
                double partialScore = invertedIndex.getTermWeight(queryWord, pageID);
                allResultList.merge(pageID, partialScore, Double::sum);
            }
        }

        int numOfQueryWord = afterProcessQuery.size();
        double queryLength = Math.sqrt(numOfQueryWord);

        for (Integer pageID: allResultList.keySet()) {
            boolean foundQueryInTitle = false;
            double documentLength = 0;
            String[] keyWords = invertedIndex.getKeyWords(pageID);
            Set<String> titleWord = invertedIndex.getTitleWords(pageID);

            for (String KeyWord : keyWords) {
                double termWeight = invertedIndex.getTermWeight(KeyWord, pageID);
                documentLength += Math.pow(termWeight, 2);
            }
            documentLength = Math.sqrt(documentLength);

            for (String queryWord : afterProcessQuery) {
                if (titleWord.contains(queryWord)) {
                    foundQueryInTitle = true;
                    break;
                }
            }

            double afterNormalized = allResultList.get(pageID)/(documentLength*queryLength);
            if (foundQueryInTitle) {
                allResultList.put(pageID, afterNormalized + 0.7);
            } else {
                allResultList.put(pageID, afterNormalized);
            }
        }
        return allResultList;
    }

    private LinkedHashMap<Integer, Double> RetrievalTop50 (HashMap<Integer, Double> allResultList){
        //sort the related page by value
        LinkedHashMap<Integer, Double> sortedResultList =
                allResultList.entrySet()
                .stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        LinkedHashMap<Integer, Double> Top50Result = new LinkedHashMap<>();
        int num = 0;
        for (Map.Entry<Integer, Double> entry : sortedResultList.entrySet()) {
            if (++num > 50) {
                break;
            }
            Top50Result.put(entry.getKey(), entry.getValue());
        }
        return Top50Result;
    }

    public void printAll(LinkedHashMap<Integer, Double> Top50Result){
        System.out.println("Score" +" "+ "PageID");
        for (Integer docID: Top50Result.keySet()) {
            System.out.println(docID + " " +  Math.round(Top50Result.get(docID)*100000)/100000.00);
        }
    }

    public static void main(String[] args) {
        while (true) {
            Scanner scanner = new Scanner(System.in);  // Create a Scanner object
            System.out.println("Enter query");
            String query = scanner.nextLine();  // Read user input
            retriever.Retrieval newQuery = new Retrieval(query);
        }
    }
}
