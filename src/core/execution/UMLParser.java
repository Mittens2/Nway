package core.execution;

import java.io.File;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;

public class UMLParser {
	private static Random rand;
	public static class UMLClass{
		public String name;
		public String depID;
		public ArrayList<String> properties;
		
		public UMLClass(String name){
			this.name = name;
			properties = new ArrayList<String>();
		}
	}
	
	public static class LogicalExp{
		public ArrayList<Operation> ops;
		public ArrayList<LogicalExp> children;
		public String name;
		public boolean not;
		public LogicalExp(){
			this.name = "";
			this.ops = new ArrayList<Operation>();
			this.children = new ArrayList<LogicalExp>();
		}
		public void addChild(LogicalExp child){
			children.add(child);
		}
		public ArrayList<LogicalExp> getChildren(){
			return children;
		}
		public void addOp(String op){
			switch (op){
				case "implies": ops.add(Operation.implies);
					break;
				case "iff": ops.add(Operation.iff);
					break;
				case "and": ops.add(Operation.and);
					break;
				case "or": ops.add(Operation.or);
					break;
			}
		}
		public ArrayList<Operation> getOps(){
			return ops;
		}
		public void isNot(){
			not = true;
		}
		public void setName(String name){
			this.name = name;
		}
		
	}
	public enum Operation{
		implies, iff, and, or;
	}
	public class Dependency{
		public ArrayList<String> dependencies;
		
		public Dependency(){
			dependencies = new ArrayList<String>();
		}
		public void addDepenedency(String dep){
			dependencies.add(dep);
		}
		public int getLength(){
			return dependencies.size();
		}
	}
	
	public enum READING{
		NONE,
		CLASS,
		ABSTRACTION
	}
	
	private static ArrayList<UMLClass> parseUML(String filePath){
		ArrayList<UMLClass> modelClasses = new ArrayList<UMLClass>();
		Map<String, String> classes = new HashMap<String, String>();
		//Map<String, ArrayList<String>> dependencies = new HashMap<String, ArrayList<String>>();
		Map<String, String> dependencies = new HashMap<String, String>();
		UMLClass currClass = null;
		String currAbs = null;
		READING reading = READING.NONE;
		try{
			Scanner scan = new Scanner(new File(filePath));
			scan.useDelimiter(Pattern.compile("<"));
			while (scan.hasNext()) {
			    String line = scan.next();
				//System.out.println(line);
			   	if (reading == READING.NONE){
			   		if ((line.contains("ClassDeclaration") || line.contains("InterfaceDeclaration")) && line.contains("originalCompilationUnit")){
			   			reading = READING.CLASS;
			   			currClass = new UMLClass(getName(line));
			   			System.out.println(currClass);
			   			classes.put(getID(line), currClass.name);
			   		}
			   	}
			   	else if (reading == READING.CLASS){
			   		if (line.contains("MethodDeclaration"))
			   			currClass.properties.add("m_" + getName(line));
			   		else if (line.contains("FieldDeclaration")){
			   			System.out.println(line);
			   			while(scan.hasNext() && (!line.contains("fragment") || line.contains("/bodyDeclarations"))){
			   				System.out.println(line);
			   				line = scan.next();
			   			}
			   			if (getName(line) != null)
			   				currClass.properties.add("f_" + getName(line));
			   		}
			   		else if (line.contains("/ownedElements")){
			   			modelClasses.add(currClass);
			   			reading = READING.NONE;
			   		}
			   	}
			}
			for (UMLClass umlc: modelClasses){
				System.out.print(umlc.name + ":");
				for (String p: umlc.properties)
					System.out.print(p + ",");
				System.out.println();
			}
			scan.close();
			return modelClasses;
		} catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public static String getName(String line){
		if (line.indexOf("name=") != -1){
			String newLine = line.substring(line.indexOf("name=") + 6);
			return newLine.substring(0, newLine.indexOf('"'));
		}
		else
			return null;
	}
	
	public static String getID(String line){
		String newLine = line.substring(line.indexOf("xmi.idref=") + 11);
		return newLine.substring(0, newLine.indexOf('"'));
	}
	
	public static String getIDAbs(String line){
		String newLine = line.substring(line.indexOf("xmi.id=") + 11);
		return newLine.substring(0, newLine.indexOf('"'));
	}
	
	public static void UMLtoCSV(String caseName, int desired){
		ArrayList<ArrayList<UMLClass>> umlcs = new ArrayList<ArrayList<UMLClass>>();
		for (int i = 0; i < desired; i++){
			umlcs.add(parseUML("/Users/amitkadan/programming/workspace/" + caseName +
		+ i + "/" + caseName + i + "_java.xmi"));
		}
		writeToFile(umlcs, caseName);
	}
	
	private static void writeToFile(ArrayList<ArrayList<UMLClass>> umlclasses, String modelName){
		try{
			
			PrintWriter writer = new PrintWriter("models/FH/" + modelName + ".csv", "utf-8");
			int modNum = 1;
			for (ArrayList<UMLClass> model: umlclasses){
				for (UMLClass element: model){
					writer.print(modNum);
					writer.print("," + element.name);
					writer.print(",n_" + element.name);
					for (String prop: element.properties){
						writer.print(";" + prop);
					}
					writer.println();
				}
				modNum++;
			}
			writer.close();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	private static Set<String> getFeatures1(Map<String, List<String>> lines, String dependent, String cmd){
		List<String> currLine = lines.get(dependent);
		Set<String> features = new HashSet<String>();
		String[] dependencies;
		// Read dependencies.
		if (currLine == null){// Base case
			if (cmd.equals("[")){
				if (rand.nextBoolean())
					features.add(dependent);
			}
			else{
				features.add(dependent);
			}
		}
		else if (currLine.size() == 1){// Case 1: list separated by "|"
			dependencies = currLine.get(0).split("\\s+\\|\\s+");
			if (cmd.equals("]")){
				if (rand.nextBoolean())
					features.add(dependencies[rand.nextInt(dependencies.length)]);
			}
			else if (cmd.equals("+")){
				if (dependencies.length == 1) features.add(dependencies[0]);
				else{
					int randInt = rand.nextInt(dependencies.length);
					ArrayList<Integer> featInds = new ArrayList<Integer>();
					for (int i = 0; i < dependencies.length; i++) featInds.add(i);
					Collections.shuffle(featInds, new Random(System.nanoTime()));
					for (int i = 0; i < randInt; i++) features.add(dependencies[featInds.get(i)]);
				}
			}
			else if (cmd.equals("*")){
				int randInt = rand.nextInt(dependencies.length);
				ArrayList<Integer> featInds = new ArrayList<Integer>();
				for (int i = 0; i < dependencies.length; i++) featInds.add(i);
				Collections.shuffle(featInds, new Random(System.nanoTime()));
				for (int i = 0; i < randInt; i++) features.add(dependencies[featInds.get(i)]);
			}
			else{
				features.add(dependencies[rand.nextInt(dependencies.length)]);
			}
		}
		else if (currLine.size() == 2){// Case 2: dependencies recurse further
			dependencies = currLine.get(0).split("\\s+");
			ArrayList<Set<String>> callFeatures = new ArrayList<Set<String>>();
			for (String dep: dependencies){
				char exp = dep.charAt(dep.length() - 1);
				if (exp == ']'){
					callFeatures.add(getFeatures1(lines, dep.substring(1, dep.length() - 1), "["));
				}
				else if (exp == '*'){
					callFeatures.add(getFeatures1(lines, dep.substring(0, dep.length() - 1), "*"));
				}
				else if (exp == '+'){
					callFeatures.add(getFeatures1(lines, dep.substring(0, dep.length() - 1), "+"));
				}
				else{
					callFeatures.add(getFeatures1(lines, dep, ""));
				}
			}
			if (cmd == "*"){
				Collections.shuffle(callFeatures, new Random(System.nanoTime()));
				for (int i = 0; i < rand.nextInt(callFeatures.size()); i++) features.addAll(callFeatures.get(i));
			}
			else if (cmd == "+"){
				Collections.shuffle(callFeatures, new Random(System.nanoTime()));
				for (int i = 0; i < 1 + rand.nextInt(callFeatures.size() - 1); i++) features.addAll(callFeatures.get(i));
			}
			else if (cmd == "["){
				if (rand.nextBoolean()) features.addAll(callFeatures.get(rand.nextInt(callFeatures.size())));
			}
			else if (cmd == "all"){
				for (Set<String> callFeature: callFeatures) features.addAll(callFeature);
			}
			else{
				features.addAll(callFeatures.get(rand.nextInt(callFeatures.size())));
			}
		}
		return features;
	}
	
	private static ArrayList<LogicalExp> readLogicalDeps(Scanner scan){
		ArrayList<LogicalExp> logExps = new ArrayList<LogicalExp>();
		while (scan.hasNext()){
			String line = scan.next().trim();
			if (line.contains("#"))
				break;
			String[] curr = line.split("implies|iff");
			LogicalExp root = new LogicalExp();
			if (line.contains("implies"))
				root.addOp("implies");
			else
				root.addOp("iff");
			root.addChild(getLogicalExp(curr[0].trim()));
			root.addChild(getLogicalExp(curr[1].trim()));
			logExps.add(root);
		}
		return logExps;
	}
	
	private static LogicalExp getLogicalExp(String exp){
		String[] exps = exp.split("\\s+(?![^\\(]*\\))");
		LogicalExp root = new LogicalExp();
		//for (String s: exps) System.out.print(s + ", ");
		//System.out.println();
		if (exps.length == 1){
			if (exps[0].startsWith("(")){
				exps[0] = exps[0].substring(1, exps[0].length() - 1);
				root.addChild(getLogicalExp(exps[0]));
			}
			else{
				root.setName(exps[0]);
			}
		}
		else{
			for (int i = 0; i < exps.length; i++){
				if (exps[i].equals("not")){
					i++;
					LogicalExp child = getLogicalExp(exps[i]);
					child.isNot();
					root.addChild(child);
				}
				else if (exps[i].equals("and") || exps[i].equals("or")){
					root.addOp(exps[i]);
				}
				else{
					if (exps[i].startsWith("(")){
						exps[i] = exps[i].substring(1, exps[i].length() - 1);
					}
					root.addChild(getLogicalExp(exps[i]));
				}
			}
		}
		return root;
	}
	private static void parseLogicalExps(Set<String> features, ArrayList<LogicalExp> logExps){
		for (LogicalExp le: logExps){
			if (le.ops.get(0) == Operation.iff){
				if (evalLogicalExps(features, le.children.get(0))){
					features.addAll(getLogicalConsequences(le.children.get(1)));
				}
				if (evalLogicalExps(features, le.children.get(1))){
					features.addAll(getLogicalConsequences(le.children.get(0)));
				}
			}
			else{
				if (evalLogicalExps(features, le.children.get(0))){
					features.addAll(getLogicalConsequences(le.children.get(1)));
				}
			}
		}
	}
	private static boolean evalLogicalExps(Set<String> features, LogicalExp exp){
		if (!exp.name.equals("")){
			if (exp.not ^ features.contains(exp.name))
				return true;
			else
				return false;
		}
		ArrayList<LogicalExp> children = exp.getChildren();
		ArrayList<Operation> ops = exp.getOps();
		boolean statement = evalLogicalExps(features, children.get(0));
		for (int i = 0; i < ops.size(); i++){
			if (ops.get(i) == Operation.and){
				statement = statement && evalLogicalExps(features, children.get(i + 1));
			}
			else{
				statement = statement || evalLogicalExps(features, children.get(i + 1));
			}
			if (!statement)
				break;
		}
		return statement;

	}
	
	private static Set<String> getLogicalConsequences(LogicalExp exp){
		Set<String> newFeats = new HashSet<String>();
		if (!exp.name.equals("")){
			newFeats.add(exp.name);
		}
		else{
			ArrayList<LogicalExp> children = exp.getChildren();
			ArrayList<Operation> ops = exp.getOps();
			newFeats.addAll(getLogicalConsequences(children.get(0)));
			for (int i = 0; i < ops.size(); i++){
				if (ops.get(i) == Operation.and){
					newFeats.addAll(getLogicalConsequences(children.get(i + 1)));
				}
				else{
					int random = rand.nextInt(3);
					if (random == 1){
						newFeats = getLogicalConsequences(children.get(i + 1));
					}
					else if (random == 2){
						newFeats.addAll(getLogicalConsequences(children.get(i + 1)));
					}
				}
			}
		}
		System.out.println(newFeats);
		return newFeats;
	}
	
	public static void createFeatureLists(String caseName, boolean random, int desired){
		String filePath = "/Users/amitkadan/Downloads/SuperimpositionExamples/Java/***Potential/" + caseName + "/" + caseName;
		File file = new File(filePath + ".model");
		ArrayList<Set<String>> featureCombs = new ArrayList<Set<String>>();
		if (file.exists()){
			try{
				rand = new Random();
				Scanner scan = new Scanner(file);
				scan.useDelimiter(";");
				Map<String, List<String>> lines = new HashMap<String, List<String>>();
				ArrayList<LogicalExp> logExps = new ArrayList<LogicalExp>();
				while (scan.hasNext()){
					String currLine = scan.next().trim();
					if (currLine.startsWith("%")){// Logical statement dependencies
						logExps = readLogicalDeps(scan);
					}
					else{
						List<String> line = Arrays.asList(currLine.split("\\s*:+\\s*"));
						lines.put(line.get(0), line.subList(1, line.size()));
					}
				}
				scan.close();
				for (int i = 0; i < desired; i++){
					Set<String> features = getFeatures1(lines, caseName, "all");
					featureCombs.add(features);
				}
				for (Set<String> features: featureCombs)
					parseLogicalExps(features, logExps);
			} catch (Exception e){
				e.printStackTrace();
			}
		}
		else{
			//filePath = filePath + "Comp.features";
			//file = new File(filePath);
			//featureCombs = getFeatures2(file, random, desired);
		}
		try{
			int count = 0;
			for (Set<String> featureList: featureCombs){
				PrintWriter writer = new PrintWriter(filePath + count + ".features", "utf-8");
				for (String feature: featureList) {
					writer.println(feature);
					//System.out.println(feature);
				}
				count++;
				writer.close();
			}
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	/*public static ArrayList<Set<String>> getFeatures2(File file, boolean random, int desired){
		try{
			// Scan features from file.
			Scanner scan = new Scanner(file);
			ArrayList<String> features = new ArrayList<String>();
			//System.out.println(scan.next());
			while(scan.hasNext()){
				features.add(scan.next());
			}
			scan.close();
			// Randomly choose 1 to features.size() features.
			ArrayList<Set<String>> featureCombs = new ArrayList<Set<String>>();
			if (random){
				for (int j = 0; j < desired; j++){
					ArrayList<Integer> seeds = new ArrayList<Integer>();
					ArrayList<String> featsChosen = new ArrayList<String>();
					featsChosen.add(features.get(0));
					Random rand = new Random();
					for (int i = 1; i < features.size(); i++) seeds.add(i);
					int numFeatures = rand.nextInt(features.size() - 1);
					Collections.shuffle(seeds);
					for (int i = 0; i < numFeatures; i++){
						featsChosen.add(features.get(seeds.get(seeds.size() - 1)));
						seeds.remove(seeds.size() - 1);
					}
					featureCombs.add(featsChosen);
				}
			}
			else{
				ArrayList<String> featureBase = new ArrayList<String>(features.subList(0, 5));
				featureCombs = getAllFeatureCombs(new ArrayList<String>(), new ArrayList<String>(features.subList(5,  9)));
				for (Sett<String> feats: featureCombs)
					feats.addAll(featureBase);
			}
			return featureCombs;
		} catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}
	private static ArrayList<Set<String>> getAllFeatureCombs(ArrayList<String> soFar, ArrayList<String> features)
	{
	    ArrayList<ArrayList<String>> combs=new ArrayList<ArrayList<String>>();
	    // Loop through the first list looking for elements
	    for(int i = features.size() - 1; i >= 0; i--)
	    {
	       ArrayList<String> temp = new ArrayList<String>(soFar);
	       temp.add(features.get(i));
	       features.remove(i);
	       if (features.size() >= 1) {
	           combs.addAll(getAllFeatureCombs(temp, new ArrayList<String>(features)));
	       } 
	       combs.add(temp);
	    }
	    return combs;
	}*/
}