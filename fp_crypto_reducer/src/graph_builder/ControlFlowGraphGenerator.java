package graph_builder;

import java.io.File;
import java.util.LinkedList;
//import java.util.List;
import java.util.Map;

import soot.Body;
import soot.BodyTransformer;
import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.Transform;
import soot.options.Options;
import soot.toolkits.graph.ExceptionalUnitGraph;

/**
 * Class that generates a control flow graph in the form of an ExceptionalUnitGraph object. 
 * 
 * @author Lukas Krawczyk
 *
 */
public class ControlFlowGraphGenerator extends BodyTransformer{
	private String pathToClasses;
	private String pathToSingleClass;
	private String mainClass;
	private String methodName;
	private static ExceptionalUnitGraph cfg;
	
	/**
	 * Self explanatory contructor for this class. 
	 *
	 * @param pathToClasses			The path to the directory of the main class.
	 * @param pathToSingleClass 	The path to the class that contains the method for which the control flow graph is being generated.
	 * @param mainClass  			Name of the main class that serves as the entry point for a call graph from which the method associated with this control flow graph is reachable.
	 * @param methodName  			Signature of the method for which the control flow graph is being generated.
	 */
	public ControlFlowGraphGenerator(String pathToClasses, String pathToSingleClass, String mainClass, String methodName){
		this.pathToClasses = pathToClasses;
		this.pathToSingleClass = pathToSingleClass;
		this.mainClass = mainClass;
		this.methodName = methodName;
		if(this.pathToClasses.equals(this.pathToSingleClass))
			this.pathToSingleClass = "";
	}
	
	/**
	 * A method that generates the control flow graph.
     * The custom phase that is being added to Soot's package manager, is used to create and extract the correct ExceptionalUnitGraph.	 
	 *
	 * @return  	The control flow graph.
	 */
	public ExceptionalUnitGraph generateGraph(){
		G.reset();
		
	    String javaPath = System.getProperty("java.class.path");
		javaPath = this.pathToClasses + javaPath.substring(javaPath.indexOf(";"));
	    String jrePath = System.getProperty("java.home")+"/lib/rt.jar";
	    String finalPath = javaPath+File.pathSeparator+jrePath+File.pathSeparator+this.pathToClasses + "\\" + pathToSingleClass;
	    Scene.v().setSootClassPath(finalPath);

	    ControlFlowGraphGenerator analysis = new ControlFlowGraphGenerator(this.pathToClasses, this.pathToSingleClass, this.mainClass, this.methodName);
	    PackManager.v().getPack("jtp").add(new Transform("jtp.ControlFlowGraphGenerator", analysis));

	    exclude();
	    //include();
	    
	    Options.v().set_app(true);
	    Options.v().set_keep_line_number(true);
	    Options.v().setPhaseOption("jb", "use-original-names:true");
	    
	    SootClass sootMainClass = Scene.v().loadClassAndSupport(this.mainClass);
	    Scene.v().setMainClass(sootMainClass);
	    Scene.v().loadNecessaryClasses();
	    
	    PackManager.v().runPacks();
	    
	    return ControlFlowGraphGenerator.cfg;
	}

	/*private static void include(){
		  List<String> includeList = new LinkedList<String>();
		  includeList.add("java.security.");
		  includeList.add("javax.crypto.");
		  Options.v().set_include(includeList);
	}*/
	
	private static void exclude(){
		LinkedList<String> excludeList = new LinkedList<String> ();
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
	
	/**
	 * The transform method that is called within Soot's custom phase.
	 * Used to create an ExceptionalUnitGraph with a Jimple body of the correct method as the basis.
	 * A static variable is used to extract the graph, that would otherwise be lost at the end of Soot's analysis.
	 *
	 * @param b  			The body of the method.
	 * @param phaseName  	The name of the phase.
	 * @param options  		A map of options used for the analysis.
	 */
	@Override
	protected void internalTransform(Body b, String phaseName,
	Map<String, String> options){	
		if(b.getMethod().getSignature().equals(this.methodName))
			ControlFlowGraphGenerator.cfg = new ExceptionalUnitGraph(b);
	}
}