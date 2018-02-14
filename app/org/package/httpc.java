import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.StringJoiner;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class httpc {
	
	// ----- Annotations to handle the command line parameters ------
	@Argument (required = false, index = 0, usage = "Type of Operation: get | post")
	String operationType;
	
	@Option (name = "-v", usage = "Prints the detail of the response such as protocol, status, and headers.")
	Boolean verboseOutput = false;
	
	@Option(name = "-h", usage = "Associates headers to HTTP Request with the format 'key:value.")
	Map <String, String> headerParams = new <String, String> HashMap(); // key : value
	
	@Option(name = "-d", forbids = {"-f"}, usage = "Associates an inline data to the body HTTP POST.") // inline data
	Map <String, String> inlineDataParam = new <String, String> HashMap();
	
	@Option(name = "-f", forbids = {"-d"}, usage = "Associates the content of a file to the body HTTP POST request")
	File fileName;
	
	@Argument (required = false, index = 1, usage = "URL")
	String urlParam;

	@Option (name = "-help", help = true, usage = "Display help information.")
	boolean help;
	
	public static void main(String[] args) throws Exception {
		//Gets the entered arguments, parses them with args4j
		new httpc().doMain(args); 
	}
	
	public void doMain(String[] args) throws Exception {
		
		httpc http = new httpc();
		CmdLineParser parser = new CmdLineParser(this);
		
		// The user can enter key : value instead of key = value
		for(int i = 0; i < args.length; i++) {
			args[i] = args[i].replaceAll(":", "="); 
		}

        try {
            parser.parseArgument(args);
                       
            if (help == true) {
            	System.out.println("httpc is a curl-like application but supports HTTP protocol only.");
            	System.out.println("Usage:");
            	System.out.println("	httpc command [arguments]");
            	System.out.println("The commands are:");
            	System.out.println("		get	executes a HTTP GET request and prints the response.");
				System.out.println("		post	executes a HTTP POST request and prints the response.");
				System.out.println("		help	prints this screen.");
            	
            	System.out.println();
            	parser.printUsage(System.out);
            	return;          	
            }
            
            if (!(operationType.equals("get") || (operationType.equals("post")))) {
            	throw new CmdLineException(parser,"Missing or error HTTP protocol (make sure its lowercase as either 'get' or 'post').");
            }
            
            if (urlParam == null) {
            	throw new CmdLineException(parser,"URL is missing.");
            }     
        } 
        catch( CmdLineException e ) {
            System.err.println(e.getMessage());
            System.err.println("httpc (get|post) [-v] (-h \"k:v\")* [-d inline-data] [-f file] URL");
            return;
        }
                     
        if (operationType.equals("get")) {
        	String getParam = "";
        	http.get(urlParam, verboseOutput, headerParams, getParam);   	
        } else if (operationType.equals("post")) {           	
        	if (inlineDataParam != null) {
        		http.post(urlParam, verboseOutput, headerParams, inlineDataParam); 
        	} 	
        }
    }

	public void setverboseOutput(Boolean verboseOutputBool) {
			verboseOutput = verboseOutputBool;
	}
	
	private void get(String urlString, Boolean verbose, Map<String, String> header, String getParam) throws Exception {
		
		// Set URL	
		String url = urlString.concat(getParam);
		url = urlString.replaceAll("=", ":");		
		URL urlObject = new URL(url);
		
		// Open HTTP connection
		HttpURLConnection con = (HttpURLConnection) urlObject.openConnection();
			
		//Add header type details
		con.setRequestMethod("GET"); 
		if (header == null) {
			con.setRequestProperty("User-Agent", "Mozilla/5.0");
			con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		} else {			
			for(String key : header.keySet()) {
	        	con.setRequestProperty(key, header.get(key));
	        }		
		}
		verbose(con, verbose); 
		//Display output
		copy(con.getInputStream(), System.out);
	}
	

	private void post(String urlString, Boolean verbose, Map<String, String> header, Map<String, String> inlineDataParam) throws Exception {
		// Set URL	
		String url = urlString.replaceAll("=", ":");
		URL urlObject = new URL(url);

		// Open HTTP connection
		HttpURLConnection con = (HttpURLConnection) urlObject.openConnection();
		
		//Add Header details
		con.setRequestMethod("POST");
		
		//Add header type details
		con.setRequestMethod("POST");
		
		if(header == null) {
			con.setRequestProperty("User-Agent", "Mozilla/5.0");
			con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		} else {			
			for(String key : header.keySet()) {
	        	con.setRequestProperty(key, header.get(key));
	        }		
		}
		
		String urlParameter = ""; // empty if null
		StringJoiner joiner = new StringJoiner("&");
		
		if(inlineDataParam == null) {
			urlParameter = "";		
		} else {
			for(String key : inlineDataParam.keySet()) {
	        	con.setRequestProperty(key, inlineDataParam.get(key));
	        	urlParameter = key + "=" + inlineDataParam.get(key); //Format key = value
	        	joiner.add(urlParameter); //Joins key : value pairs with '&' in between
	        }	
			urlParameter = joiner.toString();
		}
			
		//Send request (POST)
		con.setDoOutput(true); // For using the output data of the URL connection
			
		ByteArrayInputStream inputStream = new ByteArrayInputStream(urlParameter.getBytes("UTF-8"));
		copy(inputStream, con.getOutputStream());
		inputStream.close();
	
		//Displays output
		verbose(con, verbose);
		copy(con.getInputStream(), System.out);
	}
	
	private void verbose(HttpURLConnection connection, Boolean verbose) {
		//For verbose option
		Map <String, List<String>> headers = connection.getHeaderFields();
		Set <Map.Entry<String, List<String>>> entries = headers.entrySet();
		
		if(verbose == false){
			return;
		}
		
		//Display header details
		if(verbose == true){
			System.out.println();					
			for (Map.Entry<String, List<String>> entry : entries) {
				String name = entry.getKey();
				List<String> values = entry.getValue();
				
		        for (String value : values) {
		        	System.out.println(name + " : " + value);
		        }
			}
		}	
	}
		// Copy method, to prevent other threads to read from the input or write to the output when copying. From "Java I/O" textbook files 
	public static void copy(InputStream in, OutputStream out) throws IOException 
	{
		// do not allow other threads to read from the input or write to the output when copying
	    synchronized (in) {
	    	synchronized (out){
	    		byte[] buffer = new byte[256];
	    		int bytesRead = 0;
	            
	    		while (true) {
	            	bytesRead = in.read(buffer);
	            	if (bytesRead == -1)
	            		break;
	            	out.write(buffer, 0, bytesRead);     	
	            }
	    		out.close();
	        }
	    }
	 } 	  
}