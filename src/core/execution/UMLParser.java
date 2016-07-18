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
	
	public static ArrayList<UMLClass> parseUML(String filePath){
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
			   		if (line.matches("^UML:Class i.*") || 
			   				(line.matches("^UML:Interface.*") && getName(line) != null)){
			   			reading = READING.CLASS;
			   			currClass = new UMLClass(getName(line));
			   			classes.put(getID(line), currClass.name);
			   		}
			   		else if (line.matches("^UML:Abstraction.*")){
			   			reading = READING.ABSTRACTION;
			   			currAbs = getIDAbs(line);
			   		}
			   	}
			   	else if (reading == READING.CLASS){
			   		if (line.matches("^UML:Attribute.*") || line.matches("^UML:Operation.*"))
			   			currClass.properties.add(getName(line));
			   		else if (line.matches("^UML:Abstraction.*"))
			   			currClass.depID = getID(line).substring(3);
			   		else if (line.matches("^/UML:Class.*")){
			   			modelClasses.add(currClass);
			   			reading = READING.NONE;
			   		}
			   	}
			   	else{
			   		if (line.matches("^UML:Interface.*")){
			   			String longID = getID(line).substring(9);
			   			dependencies.put(currAbs, longID);
			   		}
			   		else if (line.matches("^/UML:Abstraction.*"))
			   			reading = READING.NONE;
			   	}
			}
			for (UMLClass umlc: modelClasses){
				if (umlc.depID != null)
					umlc.properties.add("ex_" + dependencies.get(umlc.depID));
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
	
	public static void UMLtoXLS(){
		String caseName = "GasBoilerSystem";
		creatFeatureListFiles("/home/amit/Downloads/SuperimpositionExamples/UML/" 
		+ caseName + "/" + caseName + "Comp", 12);
		ArrayList<ArrayList<UMLClass>> umlcs = new ArrayList<ArrayList<UMLClass>>();
		for (int i = 0; i < 8; i++){
			umlcs.add(parseUML("/home/amit/Downloads/SuperimpositionExamples/UML/" +
					caseName + "/" + caseName + "Comp" + i + "/boiler.xmi"));
		}
		writeToFile(umlcs, caseName);
	}
	
	public static void writeToFile(ArrayList<ArrayList<UMLClass>> umlclasses, String modelName){
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
	
	public static void creatFeatureListFiles(String filePath, int desired){
		try{
			// Scan features from file.
			ArrayList<String> features = new ArrayList<String>();
			Scanner scan = new Scanner(new File(filePath + ".features"));
			while(scan.hasNext()){
				String feature = scan.next();
				features.add(feature);
			}
			scan.close();
			// Randomly choose 1 - features.size() features.
			ArrayList<Integer> seeds = new ArrayList<Integer>();
			ArrayList<String> featsChosen = new ArrayList<String>();
			for (int i = 0; i < features.size(); i++) seeds.add(i);
			int numFeatures = new Random().nextInt(features.size() - 1) + 1;
			for (int i = 0; i < numFeatures; i++){
				Collections.shuffle(seeds);
				featsChosen.add(features.get(seeds.get(seeds.size() - 1)));
				seeds.remove(seeds.size() - 1);
			}
			// Write randomly chosen features to new file.
			for (int j = 0; j < desired; j++){
				//Path file = Paths.get("filePath" + j + ".features");
				//Files.createFile(file);
				PrintWriter writer = new PrintWriter(filePath + j + ".features", "utf-8");
				for (String feature: featsChosen) {
					writer.println(feature);
					System.out.println(feature);
				}
				writer.close();
			}
			
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
}