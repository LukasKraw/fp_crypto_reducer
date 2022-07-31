package graph_builder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import sink_finder.Sink;
import soot.MethodOrMethodContext;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Targets;
import soot.jimple.toolkits.callgraph.Units;

/**
 * A wrapper class for the actual call graph.
 * Contains the name of the class used as the entry point for the graph
 * and a list of sinks that are in methods who are reachable within this call graph.
 * 
 * @author Lukas Krawczyk
 *
 */
public class CallGraphWithSinks{
	
	public CallGraph callGraph;
	public String className;
	public List<Sink> sinks;
	
	/**
	 * A constructor that assigns the call graph and className to the relevant properties
	 * and calls the associateSinksWithCallGraph method to calculate what sinks are assigned to this call graph.
	 *
	 * @param cg  			The call graph.
	 * @param sinks  		A list of sinks.
	 * @param className  	The class name of the main class serving as entry point for the call graph.
	 */
	public CallGraphWithSinks(CallGraph cg, List<Sink> sinks, String className){
		this.callGraph = cg;
		this.className = className;
		this.sinks = associateSinksWithCallGraph(cg, sinks);
	}
	
	/**
	 * A method that iterates through all pairs of caller/callee methods in the call graph and checks if the	 
	 * callee method matches a sinks class, method and line number. The sink then gets associated with that call graph.
	 *
	 * @param cg  					The call graph.
	 * @param potentialSinks  		A list of sinks.
	 * @return  					A list of sinks associated with the call graph.
	 */
	private List<Sink> associateSinksWithCallGraph(CallGraph cg, List<Sink> potentialSinks){
		List<Sink> actualSinks = new ArrayList<Sink>();
		Iterator<MethodOrMethodContext> sourceMethods = cg.sourceMethods();
		
		while(sourceMethods.hasNext()){
			MethodOrMethodContext sourceMethod = (MethodOrMethodContext) sourceMethods.next();
			
			if(!sourceMethod.toString().contains("<jdk.internal") && !sourceMethod.toString().contains("<java.lang")){
				Iterator<MethodOrMethodContext> targetMethods = new Targets(cg.edgesOutOf(sourceMethod));
				Iterator<Unit> targetUnits = new Units(cg.edgesOutOf(sourceMethod));
				
				while(targetMethods.hasNext()){
					SootMethod targetMethod = (SootMethod) targetMethods.next();
					Unit unit = (Unit) targetUnits.next();
					int lineNumber = Integer.parseInt(unit.getTag("LineNumberTag").toString());
					
					for(Sink sink: potentialSinks) {	
						if(sourceMethod.toString().contains(sink.getFileName() + ":") && targetMethod.toString().contains(sink.getClassName()) && targetMethod.toString().contains(sink.getMethodDefinition()) && sink.getLineNumber() == lineNumber){
							sink.setSurroundingMethod(sourceMethod.toString());
							if(!actualSinks.contains(sink))
								actualSinks.add(sink);
						}
					}
				}
			}
		}
		return actualSinks;
	}
}
