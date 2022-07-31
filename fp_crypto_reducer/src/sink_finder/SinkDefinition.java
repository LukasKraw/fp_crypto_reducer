package sink_finder;

/**
 * Represents a sink definition. Contains the class name, method name and sensitive argument of the sink.
 * Provides functionality to query the number of arguments of a method.
 * 
 * @author Lukas Krawczyk
 *
 */
public class SinkDefinition{
	private String className;
	private String methodName;
	private int secureArgument;
	private String[] validValues;
	
	public SinkDefinition(String className, String methodName, int secureArgument, String[] validValues){
		this.className = className;
		this.methodName = methodName;
		this.secureArgument = secureArgument;
		this.validValues = validValues;
	}
	
	/**
	 * Gets the full class name, including the package name.
	 * @return
	 */
	public String getClassName(){
		return this.className;
	}
	
	/**
	 * Gets the short class name, without the package name.
	 * @return
	 */
	public String getShortClassName(){
		return this.className.substring(this.className.lastIndexOf('.') + 1);
	}
	
	/**
	 * Gets the full method name, including the list of arguments.
	 * @return
	 */
	public String getMethodName(){
		return this.methodName;
	}
	
	/**
	 * Gets the short method name, without the list of arguments.
	 * @return
	 */
	public String getShortMethodName(){
		return this.methodName.substring(0, this.methodName.indexOf('('));
	}
	
	/**
	 * Gets the index of the secure argument.
	 * @return
	 */
	public int getSecureArgument(){
		return this.secureArgument;
	}
	
	public String[] getValidValues() {
		return this.validValues;
	}
	
	/**
	 * Gets the number of arguments the corresponding method has.
	 * @return
	 */
	public int getNumberOfArguments(){
		char[] arguments = this.methodName.substring(this.methodName.indexOf('(')).toCharArray();
		int count = 1;
		boolean continueCounting = true;
		int numberOfBrackets = 0;
		for(int i = 1; i < arguments.length; i++){
			if(arguments[i] == ',' && continueCounting)
				count++;
			else if(arguments[i] == '('){
				continueCounting = false;
				numberOfBrackets++;
			}
			else if(arguments[i] == ')'){
				if(continueCounting)
					break;
				numberOfBrackets--;
				if(numberOfBrackets == 0)
					continueCounting = true;
			}	
		}
		return count;
	}
	
	/**
	 * Returns the number of arguments of the method it receives as input.
	 * @param fullMethodName
	 * @return
	 */
	public static int getNumberOfArgumentsOfAnyMethod(String fullMethodName){
		char[] arguments = fullMethodName.substring(fullMethodName.indexOf('(')).toCharArray();
		int count = 1;
		boolean continueCounting = true;
		int numberOfBrackets = 0;
		for(int i = 1; i < arguments.length; i++){
			if(arguments[i] == ',' && continueCounting)
				count++;
			else if(arguments[i] == '('){
				continueCounting = false;
				numberOfBrackets++;
			}
			else if(arguments[i] == ')'){
				if(continueCounting)
					break;
				numberOfBrackets--;
				if(numberOfBrackets == 0)
					continueCounting = true;
			}	
		}
		return count;
	}
}
