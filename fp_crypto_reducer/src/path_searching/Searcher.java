package path_searching;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import graph_builder.Context;
import graph_builder.ControlFlowGraphGenerator;
import sink_finder.Sink;
import soot.MethodOrMethodContext;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.internal.JIfStmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.Targets;
import soot.tagkit.Tag;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class Searcher {
	
	public static void pathSearch(List<Context> contexts, Context context, ExceptionalUnitGraph cfg, CallGraph cg, String pathToClasses, String pathToSingleClass, String mainClass) {
		Unit block = context.block;
		if(context.pathConditions == null)
			context.pathConditions = new ArrayList<String>();
		if(context.pathInformation == null)
			context.pathInformation = new ArrayList<Integer>();
		//replace the branch keyword in conditionals to show information about the jump target
		String line = block.toString();
    	if(block instanceof JIfStmt) {
    		JIfStmt ifBlock = (JIfStmt) block;
    		Unit jumpTarget = ifBlock.getTarget();
    		String jumpString = jumpTarget.toString();
    		if(line.contains("(branch)")) {
    			line = line.replace("(branch)", jumpString);
    		}
    	}
		//add the information of current block to the path information
		context.pathConditions.add(line);
		context.pathInformation.add(context.codeLine);
		//check if there are predecessors in the current cfg or cg -> if not, check if we are at the program entry point and then add the path to list of feasible paths
		if(!cfg.getPredsOf(context.block).isEmpty())
			pathSearchBlocks(contexts, cfg, cg, context, pathToClasses, pathToSingleClass, mainClass);
		else {
			SootMethod method = cfg.getBody().getMethod();
			List<Edge> edges = getCallers(cg, method.toString());
			if(edges.isEmpty() && method.toString().contains(": void main(java.lang.String[])>"))
				contexts.add(context);
			else
				pathSearchCall(contexts, edges, cfg.getBody().getMethod().toString(), cg, context, pathToClasses, pathToSingleClass, mainClass);
		}
	}
	
	public static void pathSearchBlocks(List<Context> contexts, ExceptionalUnitGraph cfg, CallGraph cg, Context context, String pathToClasses, String pathToSingleClass, String mainClass) {
		Unit currentBlock = context.block;
		List<Unit> predecessorBlocks = cfg.getPredsOf(currentBlock);
		String unitString = "";
		
		//check if a predecessor is the start of a loop -> add the entire loop information to our path information and get ready to skip the loop
		for(Unit block: predecessorBlocks) {
			if(block.toString().equals("goto [?= (branch)]")) {
			    Iterator<Unit> units = cfg.getBody().getUnits().snapshotIterator();
			    List<String> loopLines = new ArrayList<String>();
			    while(units.hasNext()){
			    	Unit unit = units.next();
			    	Tag tag = unit.getTag("LineNumberTag");
			    	if(unit.toString().equals(block.toString()) && tag.toString().equals(block.getTag("LineNumberTag").toString())) {
			    		unit = units.next();
			    		while(!(unit.toString().equals(currentBlock.toString()) && tag.toString().equals(currentBlock.getTag("LineNumberTag").toString())) && units.hasNext()) {
			    			loopLines.add(unit.toString());
			    			unit = units.next();
			    		}
			    		unitString = unit.toString();
			    		if(context.pathConditions.size() >= 2) {
			    			context.pathConditions.set(context.pathConditions.size() - 2, "--loop: " + unitString);
			    		}
			    		for(int i = loopLines.size() - 1; i > -1; i--) {
			    			context.pathConditions.add("--loop: " + unitString);
			    			context.pathConditions.add(loopLines.get(i));
			    		}
			    		break;
			    	}
			    }
			}
		}
		//continue path searching through predecessors of current block
		for(Unit block: predecessorBlocks) {
			ArrayList<String> newPathConditions = new ArrayList<String>();
			newPathConditions.addAll(context.pathConditions);
			if(block.toString().equals("goto [?= (branch)]")) {
				newPathConditions.add("--loop: " + unitString);
			}
			else {
				if(!unitString.isEmpty()) {
					newPathConditions.add("--loop: " + unitString);
					newPathConditions.add("goto [?= (branch)]");
				}
				newPathConditions.add("--nextblock--");
			}
			
			ArrayList<Integer> newPathInformation = new ArrayList<Integer>();
			newPathInformation.addAll(context.pathInformation);
			Context newContext = new Context(context.secureMethod, 
					context.sensitiveArgument, 
					block, 
					(block.getTag("LineNumberTag") != null ? Integer.parseInt(block.getTag("LineNumberTag").toString()) : -1), 
					context.containingMethod, 
					newPathInformation, 
					newPathConditions);
			pathSearch(contexts, newContext, cfg, cg, pathToClasses, pathToSingleClass, mainClass);
			//skip the actual inside of the loop
			if(block.toString().equals("goto [?= (branch)]")) {
				break;
			}	
		}
	}
	
	public static void pathSearchCall(List<Context> contexts, List<Edge> callers, String method, CallGraph cg, Context context, String pathToClasses, String pathToSingleClass, String mainClass) {
		for (Edge caller: callers) {
			String methodContext = caller.getSrc().toString();
			//skip recursion
			if(method.equals(methodContext))
					continue;
			//gather file path information for cfg builder
			String className = "";
			String pathToClass = methodContext.toString().substring(1).split("[:]")[0].substring(0);
			if(pathToClass.contains(".")) {
				String[] pathArray = pathToClass.split("[.]"); 
				pathToClass = pathToClass.replace(".", "\\");
				pathToClass = pathToClass.substring(0, pathToClass.lastIndexOf("\\"));
				className = pathArray[pathArray.length - 1];
			}
			else {
				className = pathToClass;
				pathToClass = pathToSingleClass;
			}
			String surroundingMethod = methodContext;
			//create new cfg and continue path search on that cfg
			ExceptionalUnitGraph cfg = (new ControlFlowGraphGenerator(pathToClasses, pathToClass, className, surroundingMethod)).generateGraph();
			Unit unit = getUnit(cfg, method, Integer.parseInt(caller.srcUnit().getTag("LineNumberTag").toString()));
			context.pathConditions.add("--nextmethod--");
			ArrayList<String> newPathConditions = new ArrayList<String>();
			newPathConditions.addAll(context.pathConditions);
			ArrayList<Integer> newPathInformation = new ArrayList<Integer>();
			newPathInformation.addAll(context.pathInformation);
			Context newContext = new Context(context.secureMethod, 
					context.sensitiveArgument, 
					unit, 
					(caller.srcUnit().getTag("LineNumberTag") != null ? Integer.parseInt(caller.srcUnit().getTag("LineNumberTag").toString()) : -1), 
					method,
					newPathInformation, 
					newPathConditions);
			pathSearch(contexts, newContext, cfg, cg, pathToClasses, pathToSingleClass, mainClass);
		}
	}
	
	public static String getSensitiveArgument(String line, int argumentIndex) {
		String variable = line.substring(line.toString().indexOf(">(") + 2);
		variable = variable.substring(0, variable.length() - 1);
		String[] arguments = variable.split("[,]");
		return arguments[argumentIndex - 1];
	}
	
	public static boolean isLiteral(String variable) {
		if(variable.startsWith("\"")) {
			variable.replace('"', ' ');
			variable.trim();
			return true;
		}
		return false;
	}
	
	public static boolean isNumber(String variable) {
	    try {
	        Double.parseDouble(variable);
	    } catch (Exception e) {
	        return false;
	    }
	    return true;
	}
	
	public static boolean isBoolean(String variable) {
		if(variable.equals("true") || variable.equals("false"))
			return true;
	    return false;
	}
	
	/**
	 * Checks whether the path 
	 * 
	 * @param path
	 * @param sink
	 * @return
	 */
	public static boolean isValidButInsecurePath(List<String> path, Sink sink) {
		/*System.out.println("------------------------");
		for(String str: path) {
			if(!str.startsWith("--"))
				System.out.println(str);
		}
		System.out.println("------------------------");
		System.out.println();*/
		List<String> variables = new ArrayList<String>();
		List<String> values = new ArrayList<String>();
		List<String> aliases = new ArrayList<String>();
		List<String> storage = new ArrayList<String>();
		int storageIndex = 0;
		for(int i = path.size() - 1; i > -1; i--) {
			String line = path.get(i);
			if(i > 1) {
				//path jumps to a different method
				if(path.get(i - 1).equals("--nextmethod--")) {
					String argumentsString = line.substring(line.indexOf(">(") + 1);
					if(!argumentsString.equals("()")) {
						argumentsString = argumentsString.substring(1, argumentsString.length() - 1);
						storage = new ArrayList<String>();
						if(argumentsString.contains(",")) {
							String[] argumentsArray = argumentsString.split("[,]");
							for(String argument: argumentsArray) {
								storage.add(argument.trim());
							}
						}
						else {
							storage.add(argumentsString.trim());
						}
						storageIndex = 0;
					}
				}
			}
			//variable assignment
			if(line.contains(" = ") && !line.startsWith("if ")) {
				String variable = line.split(" = ")[0].trim();
				String value = line.split(" = ")[1].trim();
				if(!variables.contains(variable)) {
					variables.add(variable);
					aliases.add(variable);
					if(variables.contains(value)) {
						int indexValue = variables.indexOf(value);
						aliases.set(aliases.size() - 1, aliases.get(aliases.size() - 1) + "," + value);
						values.add(values.get(indexValue));
					}
					else {
						values.add(value);
					}
				}
				else {
					int indexVariable = variables.indexOf(variable);
					if(variables.contains(value)) {
						int indexValue = variables.indexOf(value);
						aliases.set(indexVariable, aliases.get(indexVariable) + "," + value);
						values.set(indexVariable, values.get(indexValue));
					}
					else {
						values.set(indexVariable, value);
					}
				}
			}
			//propagating the method inputs to the new block
			if(line.contains(":= @parameter") && !storage.isEmpty()){
				String variable = line.split(" := ")[0].trim();
				variables.add(variable);
				aliases.add(variable);
				if(variables.contains(storage.get(storageIndex))) {
					aliases.set(aliases.size() - 1, aliases.get(aliases.size() - 1) + "," + storage.get(storageIndex));
					int index = variables.indexOf(storage.get(storageIndex));
					values.add(values.get(index));
				}
				else {
					values.add(storage.get(storageIndex));
				}
				line = variable + " = " + storage.get(storageIndex);
				path.set(i, line);
				storageIndex++;
			}
			//checking value of conditional statement if possible 
			//TODO jumping not necessary -> needs to be re-thought -> rather decide whether path valid or not
			if(line.startsWith("if ")) {
				if(line.contains("goto")) {
					//boolean jumped = false;
					String goToString = line.split("goto", 2)[1].trim();
					line = line.split("goto")[0].substring(2).trim();
					//decide whether or not to jump
					String variable = "";
					String value = "";
					String conditional = "";
					String nextLine = "";
					if(line.contains("==")) {
						variable = line.split("==")[0].trim();
						value = line.split("==")[1].trim();
						conditional = "==";
					}
					else if(line.contains("!=")) {
						variable = line.split("!=")[0].trim();
						value = line.split("!=")[1].trim();
						conditional = "!=";
					}
					else if(line.contains(">=")) {
						variable = line.split(">=")[0].trim();
						value = line.split(">=")[1].trim();
						conditional = ">=";
					}
					else if(line.contains("<=")) {
						variable = line.split("<=")[0].trim();
						value = line.split("<=")[1].trim();
						conditional = "<=";
					}
					else if(line.contains(">")) {
						variable = line.split(">")[0].trim();
						value = line.split(">")[1].trim();
						conditional = ">";
					}
					else if(line.contains("<")) {
						variable = line.split("<")[0].trim();
						value = line.split("<")[1].trim();
						conditional = "<";
					}
					if(variables.contains(variable)) {
						int indexVariable = variables.indexOf(variable);
						variable = values.get(indexVariable);
						if(variables.contains(value)) {
							int indexValue = variables.indexOf(value);
							value = values.get(indexValue);
						}
					}

					if(isLiteral(value) && isLiteral(variable)) {
						if(conditional == "==") {
							nextLine = "false";
							if(variable.substring(1, variable.length() - 1) == value.substring(1, variable.length() - 1))
								nextLine = goToString;
						}
						else if(conditional == "!=") {
							nextLine = "false";
							if(variable.substring(1, variable.length() - 1) != value.substring(1, variable.length() - 1))
								nextLine = goToString;
						}
					}					
					else if(isNumber(value) && isNumber(variable)) {
						if(conditional == "==") {
							nextLine = "false";
							if(Double.parseDouble(variable) == Double.parseDouble(value))
								nextLine = goToString;
						}
						else if(conditional == "!=") {
							nextLine = "false";
							if(Double.parseDouble(variable) != Double.parseDouble(value))
								nextLine = goToString;
						}
						else if(conditional == ">=") {
							nextLine = "false";
							if(Double.parseDouble(variable) >= Double.parseDouble(value))
								nextLine = goToString;
						}
						else if(conditional == "<=") {
							nextLine = "false";
							if(Double.parseDouble(variable) <= Double.parseDouble(value))
								nextLine = goToString;
						}
						else if(conditional == ">") {
							nextLine = "false";
							if(Double.parseDouble(variable) > Double.parseDouble(value))
								nextLine = goToString;
						}
						else if(conditional == "<") {
							nextLine = "false";
							if(Double.parseDouble(variable) < Double.parseDouble(value))
								nextLine = goToString;
						}
					}
					else if(isBoolean(value) && isBoolean(variable)) {
						if(conditional == "==") {
							nextLine = "false";
							if(variable == value)
								nextLine = goToString;
						}
						else if(conditional == "!=") {
							nextLine = "false";
							if(variable != value)
								nextLine = goToString;
						}
					}
					else {
						continue;
					}
					
					if(nextLine.equals(goToString)) {
						if(!path.get(i - 2).equals(goToString)) {
							return false;
						}
					}
					else if(nextLine.equals("false")) {
						if(path.get(i - 2).equals(goToString)) {
							return false;
						}
					}
				}
			}
			//loop -> loop is skipped and known variables that are overwritten in loop are marked with unknown value
			if(line.equals("goto [?= (branch)]")) {
				if(i > 0) {
					i = i - 1;
					String loopName = path.get(i);
					while (loopName.equals(path.get(i)) && i > 1) {
						String loopLine = path.get(i - 1);
						if(loopLine.contains(" = ") && !loopLine.startsWith("if ")) {
							String variable = loopLine.split(" = ")[0].trim();
							if(variables.contains(variable)) {
								int indexVariable = variables.indexOf(variable);
								values.set(indexVariable, "?");
							}
						}
						i = i - 2;
					}
					i = i + 2;
				}
			}
			//ending point -> line that contains the sink, check if value of secure parameter is known and valid
			if(i == 0) {
				int index = 0;
				String secureArgument = "";
				boolean runBackwardsEvaluation = false;
				String argumentsString = line.substring(line.indexOf(">(") + 2, line.length() - 1);
				if(argumentsString.contains(",")) {
					String[] argumentsArray = argumentsString.split("[,]");
					int indexVariables = -1;
					for(String argument: argumentsArray) {
						index += 1;
						if(variables.contains(argument)) {
							indexVariables = variables.indexOf(argument);
							argument = values.get(indexVariables);
						}
						if(sink.getSecureArgument() == index) {
				    		if(isLiteral(argument)) {
				    			if(sink.isValidValue(argument.substring(1, argument.length() - 1))) {
				    				return false;
				    			}
				    		}
				    		else if(indexVariables != -1) {
				    			secureArgument = variables.get(indexVariables);
				    			runBackwardsEvaluation = true;
				    		}
						}
					}
				}
				else {
					String argument = line.substring(line.indexOf(">(") + 2, line.length() - 1);
					int indexVariables = -1;
					if(variables.contains(argument)) {
						indexVariables = variables.indexOf(argument);
						argument = values.get(indexVariables);
					}
		    		if(isLiteral(argument)) {
		    			if(sink.isValidValue(argument.substring(1, argument.length() - 1))) {
		    				return false;
		    			}
		    		}
		    		else if(indexVariables != -1){
		    			secureArgument = variables.get(indexVariables);
		    			runBackwardsEvaluation = true;
		    		}
				}
				//check if sink argument has been sanitized
				if(runBackwardsEvaluation) {
					boolean sanitized = false;
					for(int j = path.size() - 1; j > -1; j--) {
						String newLine = path.get(j);
						//check if if statement acts as sanitizer
						if(newLine.startsWith("if ")) {
							if(newLine.contains("goto")) {
								sanitized = ifStatementSanitized(sanitized, newLine, secureArgument, variables, values, aliases, path, j, sink);
							}
						}
						//check if secure argument or alias is reassigned
						if(newLine.contains(" = ") && !newLine.startsWith("if ")) {
							String variable = newLine.split(" = ")[0].trim();
							String value = newLine.split(" = ")[1].trim();
							if(variables.contains(variable)) {
								int indexVariable = variables.indexOf(variable);
								List<String> aliasList = Arrays.asList(aliases.get(indexVariable).split("[,]"));
								if(aliasList.contains(secureArgument)) {
									if(aliasList.contains(value)) {
										String newAliases = "";
										for(String alias: aliasList) {
											if(!alias.equals(value)) {
												newAliases = newAliases + alias + ",";
											}
										}
										newAliases = newAliases.substring(0, newAliases.length() - 1);
										aliases.set(indexVariable, newAliases);
									}
									else {
										sanitized = false;
									}
								}
									
							}
							else {
								if(variable.equals(secureArgument))
									sanitized = false;
							}
						}
						//loop -> loop is skipped and known variables that are overwritten in loop are marked with unknown value
						if(newLine.equals("goto [?= (branch)]")) {
							if(j > 0) {
								j = j - 1;
								String loopName = path.get(j);
								while (loopName.equals(path.get(j))) {
									j = j - 2;
								}
								j = j + 2;
							}
						}
					}
					if(sanitized)
						return false;
				}
				return true;
			}
		}
		return false;
	}
	
	public static boolean ifStatementSanitized(boolean sanitized, String newLine, String secureArgument, List<String> variables, List<String> values, List<String> aliases, List<String> path, int j, Sink sink) {
		String goToString = newLine.split("goto", 2)[1].trim();
		newLine = newLine.split("goto")[0].substring(2).trim();
		String variable = "";
		String value = "";
		boolean equals = false;
		//check conditional operator and whether or not equals has output true
		if(newLine.contains("==")) {
			variable = newLine.split("==")[0].trim();
			value = newLine.split("==")[1].trim();
			if(value.equals("1"))
				equals = true;
		}
		else if(newLine.contains("!=")) {
			variable = newLine.split("!=")[0].trim();
			value = newLine.split("!=")[1].trim();
			if(value.equals("0"))
				equals = true;
		}
		//if equals is true and the next line on the path is the jump target
		if(path.get(j-2).equals(goToString) && equals) {
			if(variables.contains(variable)) {
				String variableValue = values.get(variables.indexOf(variable));
				//check if the variable involved in the equality check is the sink argument
				if(variableValue.contains(secureArgument + ".<java.lang.String: boolean equals(java.lang.Object)>")) {
					String validValue = variableValue.substring(variableValue.indexOf("<java.lang.String: boolean equals(java.lang.Object)>") + 53, variableValue.length() - 1);
					if(isLiteral(validValue)) {
						validValue = validValue.substring(1, validValue.length() - 1);
					}
					//check whether equality has been proven with a valid value
					if(sink.isValidValue(validValue)) {
						sanitized = true;
					}
					
				}
				//check whether the variable involved in the equality check is one of the aliases of the sink argument
				else {
					if(variables.contains(secureArgument)) {
						int indexVariable = variables.indexOf(secureArgument);
						List<String> aliasList = Arrays.asList(aliases.get(indexVariable).split("[,]"));
						for(String alias: aliasList) {
							if(variableValue.contains(alias + ".<java.lang.String: boolean equals(java.lang.Object)>")) {
								String validValue = variableValue.substring(variableValue.indexOf("<java.lang.String: boolean equals(java.lang.Object)>") + 53, variableValue.length() - 1);
								if(isLiteral(validValue)) {
									validValue = validValue.substring(1, validValue.length() - 1);
								}
								//check whether equality has been proven with a valid value
								if(sink.isValidValue(validValue)) {
									sanitized = true;
									break;
								}
							}
						}
					}
				}
			}
		}
		return sanitized;
	}
	
	public static List<Edge> getCallers(CallGraph cg, String signatureCallee) {
		ArrayList<Edge> callers = new ArrayList<Edge>();
		Iterator<MethodOrMethodContext> allCallers = cg.sourceMethods();
		while(allCallers.hasNext()){
			MethodOrMethodContext source = allCallers.next();
			if(!source.toString().contains("<jdk.internal") && !source.toString().contains("<java.lang")){
				Iterator<MethodOrMethodContext> targets = new Targets(cg.edgesOutOf(source));	
				Iterator<Edge> edgeIterator = cg.edgesOutOf(source);
				while (targets.hasNext()){
					MethodOrMethodContext target = targets.next();
					Edge edge = edgeIterator.next();
					if(target.toString().equals(signatureCallee)) {
						callers.add(edge);
					}
				}
			}
		}
		return callers;
	}
	
	
	public static Unit getUnit(ExceptionalUnitGraph cfg, String methodSignature, int codeLine) {
	    Iterator<Unit> it = cfg.getBody().getUnits().snapshotIterator();
	    Unit unit;
		
	    while(it.hasNext()){
	    	unit = it.next();
	    	int line = unit.getTag("LineNumberTag") != null ? Integer.parseInt(unit.getTag("LineNumberTag").toString()) : -1;

	    	if(line == codeLine && unit.toString().contains(methodSignature))
	    		return unit;
	    } 
	    return null;
	}
}
