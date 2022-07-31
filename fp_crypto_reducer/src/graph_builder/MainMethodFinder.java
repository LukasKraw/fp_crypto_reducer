package graph_builder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;

/**
 * A class whose purpose it is to find all executable classes, i.e. classes that have a main method definition, 
 * within a directory containing the associated source files. 
 * 
 * @author Lukas Krawczyk
 *
 */
public class MainMethodFinder{
	private List<String> mainClasses;
	private List<String> pathsToMainClasses;
	
	/**
	 * A constructor that finds all main classes for a given source path.
	 *
	 * @param srcPath		The path to the source files.
	 */
	public MainMethodFinder(String srcPath){
		getMainClasses(getAllClasses(srcPath));
	}
	
	/**
	 *
	 * @param mainClasses			A list of names of found main classes.
	 * @param pathsToMainClasses	A list of paths to those classes.
	 */
	public MainMethodFinder(List<String> mainClasses, List<String> pathsToMainClasses){
		this.mainClasses = mainClasses;
		this.pathsToMainClasses = pathsToMainClasses;
	}
	
	/**
	 * A method that returns the file paths of all java files for a given source path.
	 *
	 * @param srcPath  	The path to the source files.
	 * @return  		A list of paths to all java files in the source directory.
	 */
	private List<String> getAllClasses(String srcPath){
		ArrayList<String> allClasses = new ArrayList<String>();
        File root = new File(srcPath);
        try{
            Collection<File> files = FileUtils.listFiles(root, null, true);

            for (Iterator<File> iterator = files.iterator(); iterator.hasNext();){
                File file = (File) iterator.next();
                if (file.getName().contains(".java"))
                    allClasses.add(file.getAbsolutePath());
            }
        } 
		catch (Exception e){
            e.printStackTrace();
        }
		return allClasses;
	}
	
	/**
	 * A method that finds all main classes of a list of java classes.
	 * Assigns the results to the corresponding fields of this class.
	 *
	 * @param allClasses		A list of paths to all java classes within a source directory.
	 */
	private void getMainClasses(List<String> allClasses){
		this.mainClasses = new ArrayList<String>();
		this.pathsToMainClasses = new ArrayList<String>();
		for(String classFile: allClasses){
			if(isClassMainClass(classFile)){
				String[] splitFile = classFile.split("\\\\");
				this.mainClasses.add(splitFile[splitFile.length - 1].substring(0, splitFile[splitFile.length - 1].length() - 5));
				this.pathsToMainClasses.add(classFile.replace("\\src\\", "\\bin\\").substring(0, classFile.length() - splitFile[splitFile.length - 1].length() - 1));
			}
		}
	}
	
	/**
	 * A method that decides whether a given java source file contains a main method definition.
	 *
	 * @param filePath		The path to the java source file.
	 * @return				Whether or not that source file represents a valid main class.
	 */
	private boolean isClassMainClass(String filePath){
		FileInputStream stream = null;
		boolean mainClass = false;
		try{
			stream = new FileInputStream(filePath);
		}
		catch(Exception e){
			e.printStackTrace();
		}
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line;
        try{
            while ((line = reader.readLine()) != null){
				//TODO better matching of main classes
				if(line.contains("public static void main(") || line.contains("public static void main (")){
				//if(line.matches(".*public\s+static\s+void\s+main\s+\(.*") && !(line.matches("//.*") || (line.matches("/\*.*"))));
					mainClass = true;
					break;
				}
            }
        } 
		catch (Exception e){
            e.printStackTrace();
        }
        try{
            reader.close();
        } 
		catch (Exception e){
            e.printStackTrace();
        }
		return mainClass;
	}
	
	public List<String> getMainClasses(){
		return this.mainClasses;
	}
	
	public List<String> getPathsToMainClasses(){
		return this.pathsToMainClasses;
	}
}


