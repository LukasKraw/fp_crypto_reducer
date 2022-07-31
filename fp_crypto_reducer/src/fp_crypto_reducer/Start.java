package fp_crypto_reducer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import graph_builder.CallGraphGenerator;
import graph_builder.CallGraphWithSinks;
import graph_builder.Context;
import graph_builder.ControlFlowGraphGenerator;
import graph_builder.MainMethodFinder;
import path_searching.Searcher;
import sast_parser.Parser;
import sast_parser.Vulnerability;
import sink_finder.Sink;
import sink_finder.SinkDefinition;
import sink_finder.SinkFinder;
import soot.MethodOrMethodContext;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Targets;
import soot.jimple.toolkits.callgraph.Units;
import soot.tagkit.Tag;
import soot.toolkits.graph.ExceptionalUnitGraph;

/**
 * The main class and therefore starting point of the Crypto-FP-Reducer.
 * See the associated bachelor thesis for the exact sequence of functions performed.
 * 
 * @author Lukas Krawczyk
 *
 */
public class Start{
	
	/**
	 * Starting method of the Crypto-FP-Reducer. 
	 *
	 * @param args		The array that contains the path to the source files, the path to the class files, the path to the output file of the SAST tool
	 *              	and the path to the file containing the sink definitions in that order
	 */
	public static void main(String[] args){
		////////////////////////////////////////////////////////////////////////////////////
		//Read input parameters (path to sources, path to classes, sast output file etc.)
		String pathToSources = "C:\\Users\\salva\\eclipse-workspace\\crypto_benchmarks\\src";
		String pathToClasses = "C:\\Users\\salva\\eclipse-workspace\\crypto_benchmarks\\bin";
		String pathToSASTOutput = "C:\\Users\\salva\\Documents\\Bachelorarbeit\\CogniCrypt output.txt";
		String pathToSinkDefinitions = "C:\\Users\\salva\\Documents\\Bachelorarbeit\\sinks.txt";
		if(args.length == 4) {
			pathToSources = args[0];
			pathToClasses = args[1];
			pathToSASTOutput = args[2];
			pathToSinkDefinitions = args[3];
		}

		//int x = SinkDefinition.getNumberOfArgumentsOfAnyMethod("xdf(aaa.aaa(fgh, rrr, 66), dddd, 7, a.x(ff, gg), 68.9)676767, 7676767, 767676");
		
		//Parse SAST output
		List<Vulnerability> vulnerabilitiesDetectedBySAST = Parser.getSastOutputCogniCrypt(pathToSASTOutput);
		
		//Read sink definitions and find possible sinks
		List<SinkDefinition> sinkDefinitions = SinkFinder.GetSinkDefinitions(pathToSinkDefinitions);
		List<Sink> actualSinks = SinkFinder.FindSinks(pathToSources, sinkDefinitions, vulnerabilitiesDetectedBySAST, false);
		
		//Get all main classes
		MainMethodFinder mmf = new MainMethodFinder(pathToSources);
		List<String> mainClasses = mmf.getMainClasses();
		List<String> pathsToMainClasses = mmf.getPathsToMainClasses();
		
		//List<Context> falsePositives = new ArrayList<Context>();
		List<Sink> falsePositives = new ArrayList<Sink>();
		
		//iterate through main classes create CGs and get all Sinks that can be reached from this CG (use class1:method1 may call class2:method2 and contains checks on that) -> create structure that represents that
		for(int i = 0; i < mainClasses.size(); i++){
			CallGraphWithSinks cg = new CallGraphWithSinks(CallGraphGenerator.generateGraph(pathsToMainClasses.get(i), mainClasses.get(i)), actualSinks, mainClasses.get(i));
			//outputCG(cg.callGraph);
			
			//iterate through all sinks of a cg
			for(Sink sink: cg.sinks){
				//create cfg for sink method
				ControlFlowGraphGenerator cfgg = new ControlFlowGraphGenerator(pathToClasses, sink.getFullPath(), cg.className, sink.getSurroundingMethod());
				ExceptionalUnitGraph cfg = cfgg.generateGraph();
				//outputCFG(cfg);
				Iterator<Unit> cfgUnits = cfg.getBody().getUnits().snapshotIterator();
				Context sinkContext;
				//iterate through all nodes of the cfg
				boolean sinkInDeadCode = true;
			    while(cfgUnits.hasNext()){
			    	Unit unit = cfgUnits.next();
			    	Tag lineNumberTag = unit.getTag("LineNumberTag");
			    	
			    	if(lineNumberTag != null) {
			    		//if node has same line number and method signature as sink -> sink found in cfg
				    	if(Integer.parseInt(lineNumberTag.toString()) == sink.getLineNumber() && unit.toString().contains(sink.getMethodDefinition())) {
				    		sinkInDeadCode = false;
				    		String variable = Searcher.getSensitiveArgument(unit.toString(), sink.getSecureArgument());
				    		sinkContext = new Context(sink.getMethodDefinition(), variable, unit, sink.getLineNumber(), sink.getSurroundingMethod(), new ArrayList<Integer>(), new ArrayList<String>());
				    		//check if input is valid or not -> if valid, add sink to false positives else continue
				    		if(Searcher.isLiteral(variable)) {
				    			if(sink.isValidValue(variable.substring(1, variable.length() - 1))) {
				    				//falsePositives.add(sinkContext);
				    				falsePositives.add(sink);
				    				break;
				    			}
				    		}
				    		
				    		//check if return value is assigned to a variable -> if not, add sink to false positives
				    		if(unit.toString().split("[=]").length > 1) {						    		
					    		List<Context> contexts = new ArrayList<Context>();
					    		Searcher.pathSearch(contexts, sinkContext, cfg, cg.callGraph, pathToClasses, pathsToMainClasses.get(i).replace(pathToClasses + "\\", ""), cg.className);
					    		
					    		//check if there is a path from start to sink -> if not, add sink to fale positives
					    		if(contexts.isEmpty()) {
					    			//falsePositives.add(sinkContext);
					    			falsePositives.add(sink);
					    		}
					    		else {
						    		//System.out.println(cfg.getBody().getMethod().toString());
					    			boolean addToFalsePositives = true;
						    		for(Context context: contexts) {				    				
						    			if(Searcher.isValidButInsecurePath(context.pathConditions, sink)) {
						    				addToFalsePositives = false;
						    			}
						    		}
						    		if(addToFalsePositives) {
						    			//falsePositives.add(sinkContext);
						    			falsePositives.add(sink);
						    		}
					    		}
				    		}
				    		else {
				    			//falsePositives.add(sinkContext);
				    			falsePositives.add(sink);
				    			break;
				    		}
				    	}
			    	}
			    }
			    if(sinkInDeadCode)
			    	falsePositives.add(sink);
			}
		}
		
		//Return edited SAST output
		/*for(Vulnerability v: vulnerabilitiesDetectedBySAST){
			v.printVulnerability();
		}*/
		System.out.println("False positives:  ");
		System.out.println();
		for(Sink sink: falsePositives) {
			for(Vulnerability vulnerability: vulnerabilitiesDetectedBySAST) {
				if(sink.getFileName().equals(vulnerability.getClassName()) && sink.getLineNumber() == vulnerability.getLineNumber()) {
					System.out.println(vulnerability.getAssociatedSASTOutput());
				}
			}
		}
		System.exit(0);
	}
	
	/**
	 * A method that outputs the associated control flow graph in an easy to read way. 
	 *
	 * @param cfg		The control flow graph to be printed.
	 */
	public static void outputCFG(ExceptionalUnitGraph cfg){
		System.out.println("-----" + cfg.getBody().getMethod().toString() + "-----");
	    Iterator<Unit> units = cfg.getBody().getUnits().snapshotIterator();
		
	    while(units.hasNext()){
	    	Unit unit = units.next();
	    	Tag tag = unit.getTag("LineNumberTag");
	    	
	    	System.out.println(unit);
	    	System.out.println(tag);
	    }
	    System.out.println("--------------------------------------------------------------");
	}
	
	/**
	 * A method that outputs the associated call graph in an easy to read way. 
	 *
	 * @param cg		The call graph to be printed.
	 */
	public static void outputCG(CallGraph cg){
		Iterator<MethodOrMethodContext> sources = cg.sourceMethods();
		while(sources.hasNext()){
			MethodOrMethodContext source = sources.next();
			if(!source.toString().contains("<jdk.internal") && !source.toString().contains("<java.lang")){
				Iterator<MethodOrMethodContext> targets = new Targets(cg.edgesOutOf(source));
				Iterator<Unit> targetUnits = new Units(cg.edgesOutOf(source));
				
				while (targets.hasNext()){
					SootMethod targetMethod = (SootMethod) targets.next();
					Unit unit = (Unit) targetUnits.next();
					Tag lineNumber = unit.getTag("LineNumberTag");
					System.out.println(source + " may call " + targetMethod + " line: " + lineNumber);
				}
			}
		}
		System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
	}
}
