package ie.gmit.sw;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
//runnable
//return frequencies to a database

import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.FunctionBlock;
import net.sourceforge.jFuzzyLogic.rule.Variable;
public class NodeSearcher implements Runnable{

	private int linksSearched = 0;
	//for a random search
	Random rand = new Random();
	//the url to do a mini heuristically inform search
	private String url;
	WordFrequencyDatabase wordcount = WordFrequencyDatabase.getInstance(); 
	
	//used for the heuristic	
	private List<String> searchTerms = new ArrayList<String>();
		
	//maintain amount of node to be searched	
	private static int MAX = 20;

	//stops a loop - number of nodes visited.
	private List<String> closed = new ArrayList<String>();
	//place on queue
	private List<Document> q = new ArrayList<Document>();
	//access the fuzzy rule set
	private File fuzzyRules;
	
	public NodeSearcher(String url, String searchTerm, File ruleset)
	{
		//System.out.println("links searched increasing");
		this.url = url;
		this.fuzzyRules = ruleset;
		this.searchTerms = searchTermsProcessing(searchTerm);
	}
	//perform heuristically informed search here and populate word count
	@Override
	public void run() {
		try {
			//get the document at this link
			//System.out.println(this.searchTerm);
			Document doc = Jsoup.connect(this.url).get();
			//double heuristicScore = fuzzyHeuristic(doc);
			//System.out.println(heuristicScore);	
						closed.add(this.url);
						q.add(doc);
						//mini thread search
						search();
			//wait for the search to complete
			//System.out.println("end random search");
			//wordcount.printWordCount();
		  System.out.println(this.getClass().getName()+ " searched "+this.linksSearched+" links");
		} catch (Exception e) {
			System.out.println(e);
		}
	}
	private void search()
	{
		int searchCounter = 0;
		while(!q.isEmpty() && closed.size() <= MAX)
		{
			searchCounter = 0;
			//get document from the List
			//which is the search result form duck duck go
			Document doc = q.remove(rand.nextInt(q.size()));
			wordCount(doc.body().text());
			//System.out.println("Seaching node: "+doc.title());
			Elements links = doc.select("a[href]");
			//System.out.println("hyperlinks= "+links.size());
			for(Element edge: links)
			{
			    String link = edge.absUrl("href");
			    //System.out.println(link+": child link");
			    if(link!=null && closed.size()<= MAX && !closed.contains(link))
			    {
					//Document linkTitle = Jsoup.connect(link).get();
					//get J fuzzy logic score here aswell					
					//beginning of the heuristic is the search term in the link?
			    	//for loop for search terms
			    	for(String searchWord: this.searchTerms)
			    	{
						if(link.contains(searchWord))
						{
							try {
								Document child = Jsoup.connect(link).get();
								//System.out.println(link+" -->to be searched");							
								//fuz77zy logic here, if result of fuzzy logic is good
								double heuristicScore = fuzzyHeuristic(child);
								//then add the word count to the database of words
								if(heuristicScore>5)
								{
									wordCount(child.body().text());								
									closed.add(link);
									q.add(child);		
								}
								else
								{
									//System.out.println("page not relevant enough");
								}
							} catch (IOException e) {
								//TODO Auto-generated catch block
								System.out.println("dud");
								//e.printStackTrace();
							}
							//check all searched nodes
						}
			    	}//end checking links
			    }//end if node is applicable
			    //search the first 50 links
			    searchCounter++;
			    if(searchCounter>=100)
			    {
			    	linksSearched+=searchCounter;
			    	break;
			    }
			    linksSearched+=searchCounter;
			}//end all links of node
	    }//end while loop of search
		//System.out.println("Closed = "+ closed.size());
	}
	private void wordCount(String webPageText)
	{
		System.out.println("Word Count");
		String[] words = webPageText.split("\\W+");
		for (String word : words) {	
			wordcount.addWord(word.trim());
		}
		//print the words
	}
	//remove invalid terms from search
	private List<String> searchTermsProcessing(String initialSearch)
	{
		List<String> returnTerms = new ArrayList<String>();
		String[] words = initialSearch.split("\\W+");
		for (String word : words) {
			//don't ignore the word.. add it to the search terms for heuristic
			if(!wordcount.getIgnore().contains(word))
			{
				returnTerms.add(word.trim());
				wordcount.ignoreWordsFromSearch(word.trim());
			}
		}
		return returnTerms;
	}
	private double fuzzyHeuristic(Document relevanceCheck)
	{		
		int highRelevance = 0;
		int mediumRelevance = 0;
		int lowRelevance = 0;
		//Jfuzzy logic here
			//check Title		
    	for(String searchWord: this.searchTerms)
	   	{
	   		if(relevanceCheck.title().toLowerCase().contains(searchWord))
	   		{
	   			highRelevance++;
	    		//System.out.println("found high relevance");
	    	}
	    }
		//Jfuzzy logic here
		Elements heading1 = relevanceCheck.select("h1,h2,h3");
		//System.out.println("h1");
		for(Element h1: heading1)
		{
			//System.out.println(h1.toString());
	    	for(String searchWord: this.searchTerms)
	    	{
	    		if(h1.toString().contains(searchWord))
	    		{
	    			mediumRelevance++;
	    			//System.out.println("found high relevance");
	    		}
	    	}
		}
		//Jfuzzy logic here
		Elements paragraphs = relevanceCheck.select("p");
		//System.out.println("paragraphs");
		for(Element paragraph: paragraphs)
		{
			//System.out.println(paragraph.toString());
	    	for(String searchWord: this.searchTerms)
	    	{
	    		if(paragraph.toString().contains(searchWord))
	    		{
	    			lowRelevance++;
	    			//System.out.println("found low relevance");
	    		}
	    	}
		}
		//grab function block from file
	    FIS fis = FIS.load(this.fuzzyRules.getAbsolutePath(),true);
	    FunctionBlock fb = fis.getFunctionBlock("relevance");
	    // Set inputs
	    fis.setVariable("title",highRelevance);
	    fis.setVariable("headings",mediumRelevance);
	    fis.setVariable("paragraph",lowRelevance);
	    // Evaluate
	    fis.evaluate();
	    Variable relevance = fb.getVariable("relevance");
		//display page relevancy
		//System.out.println(relevanceCheck.title()+ "- Relevance: "+relevance.getLatestDefuzzifiedValue()+"\n");
		return relevance.getLatestDefuzzifiedValue();
	}
}