package graph_builder;

import java.util.List;
import soot.Unit;

/**
 * A class that represents the context of a sink containing information about the sinks secure method,
 * sensitive argument, the control flow graph node containing the sink, the line number of the sink, 
 * the surrounding method and the lists of path conditions and informations associated with that sink. 
 * 
 * @author Lukas Krawczyk
 *
 */
public class Context{
	public String secureMethod;
	public String sensitiveArgument;
	public Unit block;
	public int codeLine;
	public String containingMethod;
	public List<Integer> pathInformation;
	public List<String> pathConditions;
	
	public Context(String secureMethod, String sensitiveArgument, Unit block, int codeLine, String containingMethod, List<Integer> pathInformation, List<String> pathConditions){
		this.secureMethod = secureMethod;
		this.sensitiveArgument = sensitiveArgument;
		this.block = block;
		this.codeLine = codeLine;
		this.containingMethod = containingMethod;
		this.pathInformation = pathInformation;
		this.pathConditions = pathConditions;
	}
}
