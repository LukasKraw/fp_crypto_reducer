package sast_parser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * The parser of the output that the SAST tool (currently CogniCrypt) produces.
 * Modifications to this class need to be made if a different SAST tool is used.
 * 
 * @author Lukas Krawczyk
 *
 */
public class Parser{
	
	/**
	 * A method that parses the CogniCrypt output, copied from the Eclipse console window and pasted to a txt file.
     * It saves the information as a list of Vulnerability objects.	 
	 *
	 * @param filePath		The path to the txt file containing the output of CogniCrypt
	 * @return  			A list of vulnerabilities.
	 */
	public static List<Vulnerability> getSastOutputCogniCrypt(String filePath){
		List<Vulnerability> vulnerabilities = new ArrayList<Vulnerability>();
		FileInputStream stream = null;
		try{
			stream = new FileInputStream(filePath);
		}
		catch(Exception e){
			System.out.println("Could not open SAST output file.");
			e.printStackTrace();
		}
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line;
        try{
            while((line = reader.readLine()) != null){
            	String[] lineArray = line.split("\t");
            	String text = lineArray[0];
            	String className = lineArray[1].split("[.]")[0];
            	String fullPath = "";
            	if(lineArray[2].contains("/src/"))
            		fullPath = (lineArray[2].split("/src/")[1]).replace("/", "\\");
            	int lineNumber = Integer.valueOf(lineArray[3].split(" ")[1]);
            	Vulnerability vulnerability = new Vulnerability(text, className, fullPath, lineNumber, line);
            	vulnerabilities.add(vulnerability);
            }
        }
        catch(Exception e){
        	System.out.println("SAST output file is not correctly formatted.");
            e.printStackTrace();
        }
        try{
            reader.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }
		
		return vulnerabilities;
	}
}
