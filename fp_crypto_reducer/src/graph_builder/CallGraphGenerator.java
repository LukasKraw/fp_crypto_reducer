package graph_builder;

import java.io.File;
//import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


import soot.G;
//import soot.PhaseOptions;
import soot.Scene;
import soot.SootClass;
//import soot.jimple.spark.SparkTransformer;
import soot.jimple.toolkits.callgraph.CHATransformer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;

/**
 * Class that generates a CallGraph object. 
 * Call graphs can only be constructed from classes that contain a main method.
 * 
 * @author Lukas Krawczyk
 *
 */
public class CallGraphGenerator{
	
	/**
	 * Generates a call graph using the main class provided as the entry point.
	 * The main class needs to be executable.
	 *
	 * @param classPath  	The path to the directory of the main class.
	 * @param mainClass  	The main class, that is used as the entry point of the graph.
	 * @return  			A call graph.
	 */
	public static CallGraph generateGraph(String classPath, String mainClass){
		G.reset();
		
	    String javaPath = System.getProperty("java.class.path");
	    if(classPath.contains("\\bin\\"))
	    	javaPath = classPath.substring(0, classPath.indexOf("\\bin\\") + 4) + javaPath.substring(javaPath.indexOf(";"));
	    else
	    	javaPath = classPath.substring(0, classPath.indexOf("\\bin") + 4) + javaPath.substring(javaPath.indexOf(";"));
	    String jrePath = System.getProperty("java.home")+"/lib/rt.jar";
	    String finalPath = javaPath+File.pathSeparator+jrePath+File.pathSeparator+classPath;
		
	    Scene.v().setSootClassPath(finalPath);

	    exclude();
	    //include();
	    
	    Options.v().set_whole_program(true);
	    Options.v().set_app(true);
	    Options.v().set_keep_line_number(true);
	    
	    SootClass sootMainClass = Scene.v().loadClassAndSupport(mainClass);
	    Scene.v().setMainClass(sootMainClass);
	    Scene.v().loadNecessaryClasses();
	    
		CHATransformer.v().transform();

	    //PackManager.v().runPacks();
	    
	    return Scene.v().getCallGraph();
	}
	
	/*private static void include(){
		  List<String> includeList = new LinkedList<String>();
		  includeList.add("java.security.*");
		  includeList.add("javax.crypto.*");
		  Options.v().set_include(includeList);
	}*/
	
	private static void exclude(){
		List<String> excludeList = new LinkedList<String> ();
		excludeList.add("jdk.internal.*");
		excludeList.add("java.*");
		excludeList.add("sun.");
		excludeList.add("sunw.");
		excludeList.add("com.*");
		excludeList.add("apple.awt.");
	    Options.v().set_exclude(excludeList);
		
		//this option must be disabled for a sound call graph
	    Options.v().set_no_bodies_for_excluded(true);
	    Options.v().set_allow_phantom_refs(true);
	}
	
	/*private static void enableSparkCallGraph(){
	    HashMap<String,String> opt = new HashMap<String,String>();
	    opt.put("simple-edges-bidirectional","true");
	    opt.put("on-fly-cg","true");
	    SparkTransformer.v().transform("",opt);
	    PhaseOptions.v().setPhaseOption("cg.spark", "enabled:true");
	}*/		
}