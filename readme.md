# Search Engine Project #
<br>Update (26/4): term weighted added, code is refactored, phrase search is still not supported.</br>
<br>Update (27/4): posting list of inverted index will record the position of word in the page.
(similar to slide 14 in lecture notes: implementation issues). It can be useful for for identify phrase</br>
<br>Update(29/4): retriever has basic function(calculate cosine similarity), still not support phrase
search. Fixed some bugs in inverted index. Make code cleaner</br>
<br>possible useful function for retriever:</br>
```
//in inverted index class *very useful*
public Set<Integer> getRelatedPage(int wordID)
public Set<String> getTitleWords(int pageID)
public double getTermWeight(String word, int pageID) 

//in converter class
public static List<String> phraseString(String content)

//in indexer class
public Integer searchIdByWord(String word) 

//any other function in other class
...
```
