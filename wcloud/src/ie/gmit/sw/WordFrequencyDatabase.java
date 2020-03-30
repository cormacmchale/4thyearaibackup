package ie.gmit.sw;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import ie.gmit.sw.ai.cloud.WordFrequency;

//have all node searchers populate this data base
public class WordFrequencyDatabase {
	
	//singleton
	private WordFrequencyDatabase() {}
	
	private static WordFrequencyDatabase database = null;
	
	//sort the top words, word frequency can be sorted
	private List<WordFrequency> topWords = new ArrayList<WordFrequency>();
		
	//ignores these words
	private Set<String> ignore =  new ConcurrentSkipListSet<String>();
	//for organizing thread search terms
	public Set<String> getIgnore() {
		return ignore;
	}

	//return this at the end... top 32 words
	private WordFrequency[] wf = new WordFrequency[32];
	//add to the hash map all the words
	private ConcurrentHashMap<String, Integer> wordCount = new ConcurrentHashMap<String, Integer>();
	
	public void addWord(String s)
	{
		//all caps words.
        String first=s.substring(0,1);  
        String afterfirst=s.substring(1);  
        String capitalizeWord = first.toUpperCase()+afterfirst;
        if(capitalizeWord.length()<3)
        {
        	//ignore this word
        }
        else
        {
			if(ignore.contains(capitalizeWord))
			{
				return;
			}
			else
			{
				//add a word and increment the count of it
				if (wordCount.containsKey(capitalizeWord))
				{
					int increment = wordCount.get(capitalizeWord);
					increment++;
					wordCount.put(capitalizeWord, increment);
				}
				else
				{
					wordCount.put(s, 1);
				}
			}
        }
	}
	//where is where the wordFrequency is populate form the hashMap and returned
	public WordFrequency[] getWf() {
		//get top 32 words in hashmap
		 this.wordCount.entrySet().forEach(entry->{
			    //add to a list
			    topWords.add(new WordFrequency(entry.getKey(),entry.getValue())); 
			 });
		//sort the list
		Collections.sort(topWords);
		//add top 31 word to wf and return it from this method
		for(int i =0; i<32; i++)
		{
			//System.out.println("Sorted?: "+topWords.get(i).getWord()+ " : "+ topWords.get(i).getFrequency());
			wf[i] = topWords.get(i);
		}
		return wf;
	}
	//clear all info from previous searches
	public void clearWordInfo()
	{
		this.wordCount.clear();
		this.topWords.clear();
	}
	
	public void printWordCount()
	{
		//print the map for display
		//for debug
		 this.wordCount.entrySet().forEach(entry->{
			    System.out.println(entry.getKey() + " : " + entry.getValue());  
			 });
	}
	//get singleton
    public static WordFrequencyDatabase getInstance() 
    { 
        if (database == null)
        {
        	database = new WordFrequencyDatabase(); 
        }
        return database; 
    }
    //populate the ignore words file
    public void ignoreWordsFromFile(File f)
    {
    	 BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(f));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 	  
    	  String st; 
    	  try {
			while ((st = br.readLine()) != null) 
			  {
					//System.out.println(st);
		          	String first=st.substring(0,1);  
		          	String afterfirst=st.substring(1);  
		          	String capitalizeWord = first.toUpperCase()+afterfirst;
				    ignore.add(capitalizeWord); 
			  }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    //populate the ignore words file
    public void ignoreWordsFromSearch(String s)
    {
		 //System.out.println(st);
          String first=s.substring(0,1);  
          String afterfirst=s.substring(1);  
          String capitalizeWord = first.toUpperCase()+afterfirst;
	      ignore.add(capitalizeWord);
    }
}
