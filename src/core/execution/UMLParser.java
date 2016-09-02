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
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;


public class UMLParser {
	
	public static class UMLClass{
		public String name;
		public String depID;
		public ArrayList<String> properties;
		
		public UMLClass(String name){
			this.name = name;
			properties = new ArrayList<String>();
		}
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
		for (int i = 16; i < desired; i++){
			umlcs.add(parseUML("/home/amit/workspace/" + caseName +
		+ i + "/" + caseName + i + "_java.xmi"));
		}
		writeToFile(umlcs, caseName);
	}
	
	private static void writeToFile(ArrayList<ArrayList<UMLClass>> umlclasses, String modelName){
		try{
			
			PrintWriter writer = new PrintWriter("models/" + modelName + ".csv", "utf-8");
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
	private static ArrayList<String> getFeatures1(ArrayList<ArrayList<String>> lines, String dependent){
		ArrayList<String> currLine = new ArrayList<String>();
		ArrayList<String> features = new ArrayList<String>();
		String[] dependencies;
		// Find desired line.
		for (ArrayList<String> line: lines){
			if (line.get(0).equals(dependent)){
				currLine = line;
			}
		}
		// Read dependencies.
		if (currLine.size() == 0){
			//System.out.println(dependent);
			features.add(dependent);
		}
		else if (currLine.size() == 2){
			dependencies = currLine.get(1).split("\\s+\\|\\s+");
			for (String dep: dependencies){
				features.add(dep);
			}
		}
		// Dependencies recurse further.
		else if (currLine.size() == 3){
			dependencies = currLine.get(1).split("\\s+");
			for (String dep: dependencies){
				ArrayList<String> allFeatures = new ArrayList<String>();
				//System.out.println(dep);
				char exp = dep.charAt(dep.length() - 1);
				if (exp == ']'){
					boolean keep = new Random().nextBoolean();
					if (keep)
						features.addAll(getFeatures1(lines, dep.substring(1, dep.length() - 1)));
				}
				else if (exp == '*'){
					allFeatures.addAll(getFeatures1(lines, dep.substring(0, dep.length() - 1)));
					int rand = new Random().nextInt(allFeatures.size());
					ArrayList<Integer> featInds = new ArrayList<Integer>();
					for (int i = 0; i < allFeatures.size(); i++) featInds.add(i);
					Collections.shuffle(featInds, new Random(System.nanoTime()));
					for (int i = 0; i < rand; i++) features.add(allFeatures.get(featInds.get(i)));
				}
				else if (exp == '+'){
					allFeatures.addAll(getFeatures1(lines, dep.substring(0, dep.length() - 1)));
					int rand = new Random().nextInt(allFeatures.size()) + 1;
					ArrayList<Integer> featInds = new ArrayList<Integer>();
					for (int i = 0; i < allFeatures.size(); i++) featInds.add(i);
					Collections.shuffle(featInds, new Random(System.nanoTime()));
					for (int i = 0; i < rand; i++) features.add(allFeatures.get(featInds.get(i)));
				}
				else{
					allFeatures.addAll(getFeatures1(lines, dep));
					if (allFeatures.size() > 0)
						features.add(allFeatures.get(new Random().nextInt(allFeatures.size())));
				}
			}
		}
		return features;
	}
	
	public static void createFeatureLists(String caseName, boolean random, int desired){
		String filePath = "/home/amit/Downloads/SuperimpositionExamples/Java/" + caseName + "/" + caseName;
		File file = new File(filePath + ".model");
		ArrayList<ArrayList<String>> featureCombs = new ArrayList<ArrayList<String>>();
		if (file.exists()){
			try{
				Scanner scan = new Scanner(file);
				scan.useDelimiter(";");
				ArrayList<ArrayList<String>> lines = new ArrayList<ArrayList<String>>();
				while (scan.hasNext()){
					ArrayList<String> line = new ArrayList<String>();
					String currLine = scan.next().trim();
					line.addAll(Arrays.asList(currLine.split("\\s*:+\\s*")));
					//System.out.println(line);
					lines.add(line);
				}
				for (int i = 0; i < desired; i++){
					ArrayList<String> features = getFeatures1(lines, "SPL");
					featureCombs.add(features);
				}
				scan.close();
			} catch (Exception e){
				e.printStackTrace();
			}
		}
		else{
			filePath = filePath + "Comp.features";
			file = new File(filePath);
			featureCombs = getFeatures2(file, random, desired);
		}
		try{
			int count = 0;
			for (ArrayList<String> featureList: featureCombs){
				PrintWriter writer = new PrintWriter(filePath + count + ".features", "utf-8");
				for (String feature: featureList) {
					writer.println(feature);
					System.out.println(feature);
				}
				count++;
				writer.close();
			}
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	public static ArrayList<ArrayList<String>> getFeatures2(File file, boolean random, int desired){
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
			ArrayList<ArrayList<String>> featureCombs = new ArrayList<ArrayList<String>>();
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
				for (ArrayList<String> feats: featureCombs)
					feats.addAll(featureBase);
			}
			return featureCombs;
		} catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}
	private static ArrayList<ArrayList<String>> getAllFeatureCombs(ArrayList<String> soFar, ArrayList<String> features)
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
	}
}