#Search Engine Project
<br>Update (27/4): term weighted added, code is refactored, phrase search is still not supported</br>
<br></br>
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
