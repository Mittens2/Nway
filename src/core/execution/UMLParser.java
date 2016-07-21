package core.execution;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
			   		if (line.contains("ClassDeclaration") || line.contains("InterfaceDeclaration")){
			   		//if (line.matches("^UML:Class i.*") || 
			   		   //(line.matches("^UML:Interface.*") && getName(line) != null)){
			   			reading = READING.CLASS;
			   			currClass = new UMLClass(getName(line));
			   			classes.put(getID(line), currClass.name);
			   		}
			   		/*else if (line.matches("^UML:Abstraction.*")){
			   			reading = READING.ABSTRACTION;
			   			currAbs = getIDAbs(line);
			   		}*/
			   	}
			   	else if (reading == READING.CLASS){
			   		if (line.contains("MethodDeclaration"))
			   			currClass.properties.add(getName(line));
			   		/*if (line.matches("^UML:Attribute.*") || line.matches("^UML:Operation.*")){
			   			if (!getName(line).equals("variable") && getName(line).equals("reg")){
			   				currClass.properties.add(getName(line));
			   			}
			   		}*/
			   		/*else if (line.matches("^UML:Abstraction.*")){
			   			currClass.depID = getID(line).substring(3);
			   		}*/
			   		else if (line.contains("/ownedElements")){
			   		//else if (line.matches("^/UML:Class.*")){
			   			modelClasses.add(currClass);
			   			reading = READING.NONE;
			   		}
			   	}
			   	/*else{
			   		if (line.matches("^UML:Interface.*")){
			   			String longID = getID(line).substring(9);
			   			dependencies.put(currAbs, longID);
			   		}
			   		else if (line.matches("^/UML:Abstraction.*"))
			   			reading = READING.NONE;
			   	}*/
			}
			for (UMLClass umlc: modelClasses){
				//if (umlc.depID != null)
					//umlc.properties.add("ex_" + dependencies.get(umlc.depID));
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
	
	public static void UMLtoCSV(){
		String caseName = "GameOfLife";
		//creatFeatureListFiles("/home/amit/Downloads/SuperimpositionExamples/UML/" 
		//+ caseName + "/" + caseName + "Comp", 12);
		ArrayList<ArrayList<UMLClass>> umlcs = new ArrayList<ArrayList<UMLClass>>();
		//for (int i = 0; i < 15; i++){
			//umlcs.add(parseUML("/home/amit/Downloads/SuperimpositionExamples/UML/" +
					//caseName + "/" + caseName + "Comp" + i + "/ClassDiagram.xmi"));
		//}
		for (int i = 0; i < 8; i++){
			umlcs.add(parseUML("/home/amit/workspace/" + caseName + i + "/" + caseName + i + "_java.xmi"));
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
	
	public static void creatFeatureListFiles(String filePath, boolean random, int desired){
		try{
			// Scan features from file.
			ArrayList<String> features = new ArrayList<String>();
			Scanner scan = new Scanner(new File(filePath + ".features"));
			while(scan.hasNext()){
				String feature = scan.next();
				features.add(feature);
			}
			scan.close();
			// Randomly choose 1 to features.size() features.
			ArrayList<ArrayList<String>> featureCombs = new ArrayList<ArrayList<String>>();
			if (random){
				for (int j = 0; j < desired; j++){
					ArrayList<Integer> seeds = new ArrayList<Integer>();
					ArrayList<String> featsChosen = new ArrayList<String>();
					//featsChosen.add(features.get(0));
					Random rand = new Random();
					for (int i = 0; i < features.size(); i++) seeds.add(i);
					int numFeatures = rand.nextInt(features.size() - features.size() / 2) + features.size() / 2;
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
			int count = 0;
			Runtime rt = Runtime.getRuntime();
			for (ArrayList<String> featureList: featureCombs){
				// Write randomly chosen features to new file.
				//Path file = Paths.get("filePath" + j + ".features");
				//Files.createFile(file);
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