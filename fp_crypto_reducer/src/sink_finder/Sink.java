package sink_finder;

/**
 * Represents a sink. Contains the name of the class the sink is contained in, the path to that class,
 * the class that defines the secure method of the sink, the object of that class, the secure method definition,
 * the line number of the sink and the method this sink is contained in (in a short and extended form).
 * 
 * @author Lukas Krawczyk
 *
 */
public class Sink{
	private String fileName;
	private String fullPath;
	private String className;
	private String objectName;
	private String methodDefinition;
	private int lineNumber;
	private String surroundingMethod;
	private String shortSurroundingMethod;
	private int secureArgument;
	private String[] validValues;
	
	public Sink(String fileName, String fullPath, String className, String objectName, String methodDefinition, int lineNumber, int secureArgument, String[] validValues){
		this.fileName = fileName;
		this.fullPath = fullPath;
		this.className = className;
		this.objectName = objectName;
		this.methodDefinition = methodDefinition;
		this.lineNumber = lineNumber;
		this.secureArgument = secureArgument;
		this.validValues = validValues;
	}
	
	/**
	 * The file name of the class this sink is contained in.
	 * @return
	 */
	public String getFileName(){
		return this.fileName;
	}
	
	/***
	 * The full path to the class this sink is contained in (minus the file name).
	 * @return
	 */
	public String getFullPath(){
		return this.fullPath;
	}
	
	/**
	 * The class that defines and calls the sink method.
	 * @return
	 */
	public String getClassName(){
		return this.className;
	}
	
	/**
	 * The instance of the class that defines and calls the sink method
	 * @return
	 */
	public String getObjectName(){
		return this.objectName;
	}
	
	/**
	 * The method that poses the security risk.
	 * @return
	 */
	public String getMethodDefinition(){
		return this.methodDefinition;
	}
	
	/**
	 * The line in the source code this sink occurs in
	 * @return
	 */
	public int getLineNumber(){
		return this.lineNumber;
	}
	
	public int getSecureArgument(){
		return this.secureArgument;
	}
	
	public String[] getValidValues(){
		return this.validValues;
	}
	
	/**
	 * Set the surrounding method in which this sink occurs in.
	 * @param surroundingMethod
	 */
	public void setSurroundingMethod(String surroundingMethod){
		this.surroundingMethod = surroundingMethod;
		String buffer = surroundingMethod.split("[:]")[1].trim();
		this.shortSurroundingMethod = buffer.substring(buffer.indexOf(' ') + 1, buffer.indexOf('('));
	}
	
	/**
	 * Get the surrounding method in which this sink occurs in.
	 * @return
	 */
	public String getSurroundingMethod(){
		return this.surroundingMethod;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getShortSurroundingMethod(){
		return this.shortSurroundingMethod;
	}
	
	/**
	 * Checks if the input argument is secure.
	 * 
	 * @param value
	 * @return
	 */
	public boolean isValidValue(String value){
		for(String validValue: this.validValues){
			if(validValue.toLowerCase().equals(value.toLowerCase()))
				return true;
		}
		return false;
	}
}
