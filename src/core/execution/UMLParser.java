package core.execution;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;


public class UMLParser {
	
	public class UMLClass{
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
	
	public void parseUML(String filePath){
		ArrayList<UMLClass> modelClasses = new ArrayList<UMLClass>();
		Map<String, String> classes = new HashMap<String, String>();
		Map<String, ArrayList<String>> dependencies = new HashMap<String, ArrayList<String>>();
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
			   		if (line.matches("^UML:Class i.*") || line.matches("^UML:Interface ")){
			   			reading = READING.CLASS;
			   			currClass = new UMLClass(getName(line));
			   			classes.put(getID(line), currClass.name);
			   		}
			   		else if (line.matches("^UML:Abstraction.*")){
			   			reading = READING.ABSTRACTION;
			   			currAbs = getIDAbs(line);
			   			dependencies.put(currAbs, new ArrayList<String>());
			   		}
			   	}
			   	else if (reading == READING.CLASS){
			   		if (line.matches("^UML:Attribute.*") || line.matches("^UML:Operation.*"))
			   			currClass.properties.add(getName(line));
			   		else if (line.matches("^UML:Abstraction.*"))
			   			currClass.depID = getID(line);
			   		else if (line.matches("^/UML:Class.*")){
			   			modelClasses.add(currClass);
			   			reading = READING.NONE;
			   		}
			   	}
			   	else{
			   		if (line.matches("^UML:Class.*"))
			   			dependencies.get(currAbs).add(getID(line).substring(9));
			   		else if (line.matches("^/UML:Abstraction.*"))
			   			reading = READING.NONE;
			   	}
			}
			for (UMLClass umlc: modelClasses){
				System.out.print(umlc.name + ":" + "(" + umlc.depID + ")");
				for (String p: umlc.properties)
					System.out.print(p + ",");
				System.out.println();
			}
			for (String id: dependencies.keySet())
				System.out.println(id + ":" + dependencies.get(id));
		} catch (Exception e){
			e.printStackTrace();
		}
		
	}
	
	public String getName(String line){
		String newLine = line.substring(line.indexOf("name=") + 6);
		return newLine.substring(0, newLine.indexOf('"'));
	}
	
	public String getID(String line){
		String newLine = line.substring(line.indexOf("xmi.idref=") + 11);
		return newLine.substring(0, newLine.indexOf('"'));
	}
	
	public String getIDAbs(String line){
		String newLine = line.substring(line.indexOf("xmi.id=") + 11);
		return newLine.substring(0, newLine.indexOf('"'));
	}
	
}
