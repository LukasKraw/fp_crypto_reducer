package sink_finder;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import sast_parser.Vulnerability;

/**
 * Class that provides functionality for parsing sink definitions and finding actual sinks in class files.
 * 
 * @author Lukas Krawczyk
 *
 */
public class SinkFinder {
	
	/**
	 * Gets the list of sink definitions from the file specified in the filePath parameter.
	 * 
	 * @param filePath		Path to the file containing the sink definitions.
	 */
	public static List<SinkDefinition> GetSinkDefinitions(String filePath){	
		FileInputStream stream = null;
		try{
			stream = new FileInputStream(filePath);
		}
		catch(Exception e){
			e.printStackTrace();
		}
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line;
        List<SinkDefinition> sinkDefinitions = new ArrayList<SinkDefinition>();
        try{
            while((line = reader.readLine()) != null){
            	String[] validValues = line.split("[:]")[3].split("[,]");
            	SinkDefinition definition = new SinkDefinition(line.split("[:]")[0], line.split("[:]")[1], Integer.parseInt(line.split("[:]")[2]), validValues);
            	sinkDefinitions.add(definition);
            }
        } 
        catch(Exception e){
            e.printStackTrace();
        }
        try{
            reader.close();
        } 
        catch(Exception e){
            e.printStackTrace();
        }
        
        return sinkDefinitions;
	}
	
	/**
	 * Sorts the list of vulnerabilities found by a SAST tool by class name.
	 * Currently not implemented.
	 * 
	 * @param vulnerabilities
	 * @return
	 */
	private static List<Vulnerability> sortVulnerabilitiesByClass(List<Vulnerability> vulnerabilities){
		List<String> classes = new ArrayList<String>();
		for(Vulnerability vulnerability: vulnerabilities){
			if(!classes.contains(vulnerability.getClassName())){
				classes.add(vulnerability.getClassName());
			}
		}
		List<Vulnerability> vulnerabilitiesNew = new ArrayList<Vulnerability>();
		for(String className: classes){
			for(Vulnerability vulnerability: vulnerabilities) {
				if(vulnerability.getClassName() == className)
					vulnerabilitiesNew.add(vulnerability);
			}
		}
		return vulnerabilitiesNew;
	}
	
	/**
	 * Searches through all the classes in the class path using the list of found vulnerabilities 
	 * and the definition of possible sinks to find all actual sinks.
	 *  
	 * @param pathToSources		File path to the source-files to be analyzed.
	 * @param sinkDefinitions	List of sink definitions to be checked against.
	 * @param vulnerabilities	List of vulnerabilities found by a SAST tool.
	 * @param sorted			Specifies whether the list of vulnerabilities is sorted by class name.
	 * @return
	 */
	public static List<Sink> FindSinks(String pathToSources, List<SinkDefinition> sinkDefinitions, List<Vulnerability> vulnerabilities, boolean sorted){
		List<Sink> potentialSinks = new ArrayList<Sink>();
		List<Sink> foundSinks = new ArrayList<Sink>();
		String className = "";
		FileInputStream stream = null;
		BufferedReader reader = null;
		String line = null;
		if(!sorted ){
			vulnerabilities = sortVulnerabilitiesByClass(vulnerabilities);
		}
		//search through all vulnerabilities to find all classes with vulnerabilities in them
		for(Vulnerability v: vulnerabilities){
			//don't open the file again after searching through it once
			if(!className.equals(v.getClassName())){
				className = v.getClassName();
				try{
					stream = new FileInputStream(pathToSources + "\\" + v.getFullPath() + "\\" +  v.getClassName() + ".java"); //error -> need full path to file
				}
				catch(Exception e){
					System.out.println("Could not open src file " + v.getClassName() + ".");
					e.printStackTrace();
				}
				reader = new BufferedReader(new InputStreamReader(stream));
				List<String> readFile = new ArrayList<String>();
				int counter = 0;
				
				try{
					while((line = reader.readLine()) != null){
						counter++;
						readFile.add(line);
						boolean foundSink = false;
						//String[] lineArray = line.split(" ");
						for(SinkDefinition definition: sinkDefinitions){
							if(line.contains("." + definition.getShortMethodName()) && !line.startsWith("//")){
								if(line.contains(definition.getShortClassName() + "." + definition.getShortMethodName())){
									String arguments = line.substring(line.indexOf(definition.getShortMethodName()));
									int numberOfArguments = SinkDefinition.getNumberOfArgumentsOfAnyMethod(arguments);
									if(numberOfArguments == definition.getNumberOfArguments()) {
										foundSinks.add(new Sink(v.getClassName(), v.getFullPath() ,definition.getClassName(), definition.getShortClassName(), definition.getMethodName(), counter, definition.getSecureArgument(), definition.getValidValues())); 
										foundSink = true;
										break;
									}
								}
							}
						}
						if(!foundSink) {
							for(SinkDefinition definition: sinkDefinitions){
								if(line.contains("." + definition.getShortMethodName()) && !line.startsWith("//")){
									if(line.contains("." + definition.getShortMethodName())){
										String arguments = line.substring(line.indexOf(definition.getShortMethodName()));
										int numberOfArguments = SinkDefinition.getNumberOfArgumentsOfAnyMethod(arguments);
										if(numberOfArguments == definition.getNumberOfArguments()) {
											String objectName = line.substring(0, line.indexOf("." + definition.getShortMethodName()));
											String[] lineArray = objectName.split(" ");
											objectName = lineArray[lineArray.length - 1];
											potentialSinks.add(new Sink(v.getClassName(), v.getFullPath(), definition.getClassName(), objectName, definition.getMethodName(), counter, definition.getSecureArgument(), definition.getValidValues()));
											break;
										}
									}
								}
							}
						}
					}
					for(String entry: readFile){
						for(Sink sink: potentialSinks){
							if(entry.contains((sink.getClassName().substring(sink.getClassName().lastIndexOf('.') + 1)) + " " + sink.getObjectName()))
								foundSinks.add(sink);
						}
					}
				} 
				catch(IOException e){
					System.out.println("Could not read src file " + v.getClassName() + ".");
					e.printStackTrace();
				}
				try{
					reader.close();
					stream.close();
				} 
				catch(IOException e){
					System.out.println("Could not close src file.");
					e.printStackTrace();
				}
			}
		}
		
		return foundSinks;
	}
}
