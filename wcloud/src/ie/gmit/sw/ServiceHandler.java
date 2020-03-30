package ie.gmit.sw;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ie.gmit.sw.ai.cloud.LogarithmicSpiralPlacer;
import ie.gmit.sw.ai.cloud.WeightedFont;
import ie.gmit.sw.ai.cloud.WordFrequency;

/*
 * -------------------------------------------------------------------------------------------------------------------
 * PLEASE READ THE FOLLOWING CAREFULLY. MOST OF THE "ISSUES" STUDENTS HAVE WITH DEPLOYMENT ARISE FROM NOT READING
 * AND FOLLOWING THE INSTRUCTIONS BELOW.
 * -------------------------------------------------------------------------------------------------------------------
 *
 * To compile this servlet, open a command prompt in the web application directory and execute the following commands:
 *
 * Linux/Mac													Windows
 * ---------													---------	
 * cd WEB-INF/classes/											cd WEB-INF\classes\
 * javac -cp .:$TOMCAT_HOME/lib/* ie/gmit/sw/*.java				javac -cp .:%TOMCAT_HOME%/lib/* ie/gmit/sw/*.java
 * cd ../../													cd ..\..\
 * jar -cf wcloud.war *											jar -cf wcloud.war *
 * 
 * Drag and drop the file ngrams.war into the webapps directory of Tomcat to deploy the application. It will then be 
 * accessible from http://localhost:8080. The ignore words file at res/ignorewords.txt will be located using the
 * IGNORE_WORDS_FILE_LOCATION mapping in web.xml. This works perfectly, so don't change it unless you know what
 * you are doing...
 * 
*/

public class ServiceHandler extends HttpServlet {
	
	Thread search = null;
	private String ignoreWords = null;
	private String getFuzzyRules = null;
	private File ignore;
	private File relevance;
	ExecutorService searchers = Executors.newFixedThreadPool(10);
	WordFrequencyDatabase wordcount = WordFrequencyDatabase.getInstance(); 
	
	//this is the actual searching object
	//contains all the logic for getting the 	
	public void init() throws ServletException {
		ServletContext ctx = getServletContext(); //Get a handle on the application context
		
		//Reads the value from the <context-param> in web.xml
		ignoreWords = getServletContext().getRealPath(File.separator) + ctx.getInitParameter("IGNORE_WORDS_FILE_LOCATION");
		getFuzzyRules = getServletContext().getRealPath(File.separator) + ctx.getInitParameter("RELEVANCE_FILE_LOCATION"); 
		ignore = new File(ignoreWords); //A file wrapper around the ignore words...
		relevance = new File(getFuzzyRules);
		wordcount.ignoreWordsFromFile(ignore);
	
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/html"); //Output the MIME type
		PrintWriter out = resp.getWriter(); //Write out text. We can write out binary too and change the MIME type...
		
		//Initialize some request variables with the submitted form info. These are local to this method and thread safe...
		String option = req.getParameter("cmbOptions"); //Change options to whatever you think adds value to your assignment...
		String s = req.getParameter("query");
		out.print("<html><head><title>Artificial Intelligence Assignment</title>");		
		out.print("<link rel=\"stylesheet\" href=\"includes/style.css\">");
		out.print("</head>");		
		out.print("<body>");		
		out.print("<div style=\"font-size:48pt; font-family:arial; color:#990000; font-weight:bold\">Web Opinion Visualiser</div>");			
		out.print("<p><h2>Please read the following carefully</h2>");
		out.print("<p>The &quot;ignore words&quot; file is located at <font color=red><b>" + ignore.getAbsolutePath() + "</b></font> and is <b><u>" + ignore.length() + "</u></b> bytes in size.");
		out.print("<p>The &quot;ignore words&quot; file is located at <font color=red><b>" + relevance.getAbsolutePath() + "</b></font> and is <b><u>" + relevance.length() + "</u></b> bytes in size.");
		out.print("You must place any additional files in the <b>res</b> directory and access them in the same way as the set of ignore words.");
		out.print("<p>Place any additional JAR archives in the WEB-INF/lib directory. This will result in Tomcat adding the library of classes ");	
		out.print("to the CLASSPATH for the web application context. Please note that the JAR archives <b>jFuzzyLogic.jar</b>, <b>encog-core-3.4.jar</b> and "); 		
		out.print("<b>jsoup-1.12.1.jar</b> have already been added to the project.");				
		out.print("<p><fieldset><legend><h3>Result</h3></legend>");
		//testing duckduck go search
		try {
			Search(s);
		    //search.join();
			//maximum of 30 seconds	
			searchers.awaitTermination(25, TimeUnit.SECONDS);
			searchers.shutdown();
	        while (!searchers.isTerminated()) {
	        	//wait for search to complete
	        	//System.out.println("await termination");
	        }
	        System.out.println("Finished all Jobs - serach complete");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    //wait max 2 mins for threads to finish
		//this is where you need to return a result from the project
		//array of words
			//wordcount.printWordCount();		
		//possibly get this from database.
		WordFrequency[] words = new WeightedFont().getFontSizes(wordcount.getWf());
		Arrays.sort(words, Comparator.comparing(WordFrequency::getFrequency, Comparator.reverseOrder()));
		//Arrays.stream(words).forEach(System.out::println);
		//Spira Mirabilis
		LogarithmicSpiralPlacer placer = new LogarithmicSpiralPlacer(800, 600);
		for (WordFrequency word : words) {
			placer.place(word); //Place each word on the canvas starting with the largest
		}
		BufferedImage cloud = placer.getImage(); //Get a handle on the word cloud graphic
		out.print("<img src=\"data:image/png;base64," + encodeToString(cloud) + "\" alt=\"Word Cloud\">");	
		out.print("</fieldset>");	
		out.print("<P>Maybe output some search stats here, e.g. max search depth, effective branching factor.....<p>");		
		out.print("<a href=\"./\">Return to Start Page</a>");
		out.print("</body>");	
		out.print("</html>");	
		//clear all accumulated data
		System.out.println("clearing word info");
		wordcount.clearWordInfo();
	}
	//initially testing the duckduckgo search library
	private void Search(String s) throws IOException, InterruptedException {
		//System.out.println(s);
	    int counter = 0;
		//get all the document of duckduckgo searching the word
	    Document doc = Jsoup.connect("https://duckduckgo.com/html/?q=" + s).get();
	    //get all the links on this page
	    Elements res = doc.getElementById("links").getElementsByClass("results_links");
	    //Element title = res.get(0).getElementsByClass("links_main").first().getElementsByTag("a").first();
	    //test heuristic on one node
	    //System.out.println(title.attr("href")+" "+s);
	    for(Element r: res){   
	     Element title = r.getElementsByClass("links_main").first().getElementsByTag("a").first();
	     //for these nodes... perform a mini heuristic search
	     //make use of duckduckgo's heuristic
	     //start thread
	     //System.out.println(title.attr("href"));
	     //NodeSearcher n = new NodeSearcher(title.attr("href"),s);
         //search = new Thread(n);
         //search.start();
	     searchers.execute(new NodeSearcher(title.attr("href"),s, relevance));
	     			//possibly add later
		 	     	counter++;
		 	     	if(counter==5)
		 	     	{
		     			return;
		 	     	}
	     }//end loop first search result
	    //searchers.shutdown();
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
 	}	
	private String encodeToString(BufferedImage image) {
	    String s = null;
	    ByteArrayOutputStream bos = new ByteArrayOutputStream();

	    try {
	        ImageIO.write(image, "png", bos);
	        byte[] bytes = bos.toByteArray();

	        Base64.Encoder encoder = Base64.getEncoder();
	        s = encoder.encodeToString(bytes);
	        bos.close();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	    return s;
	}
	private BufferedImage decodeToImage(String imageString) {
	    BufferedImage image = null;
	    byte[] bytes;
	    try {
	        Base64.Decoder decoder = Base64.getDecoder();
	        bytes = decoder.decode(imageString);
	        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
	        image = ImageIO.read(bis);
	        bis.close();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return image;
	}
}