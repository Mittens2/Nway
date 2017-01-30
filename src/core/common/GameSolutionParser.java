package core.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.orsoncharts.util.json.parser.ParseException;

import core.domain.Model;
import core.domain.Tuple;

public class GameSolutionParser {
	private JSONArray gameModel;
	private ArrayList<Model> models;
	
	public GameSolutionParser(String filePath, ArrayList<Model> models){
		//models = Model.readModelsFile(modelsFile);
		this.models = models;
		if (models.size() > 16){
			models = new ArrayList<Model>(models.subList(0, 10));
		}
		JSONParser parser = new JSONParser();
		try {
			gameModel = (JSONArray) parser.parse(new FileReader(filePath));
			//System.out.println(gameModel);
			} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException | org.json.simple.parser.ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public ArrayList<Tuple> solutionCalculator(){
		ArrayList<Tuple> solution = new ArrayList<Tuple>();
		for (Object o : gameModel){
			JSONObject jsonObject =  (JSONObject) o;
			JSONArray elements = (JSONArray) jsonObject.get("alien");
			Tuple currTuple = new Tuple();
			for (Object e : elements)
			{
				String elemString = (String) e;
				String[] elemDescriptor = elemString.split("_");
				int model = Integer.parseInt(elemDescriptor[0]);
				int element = Integer.parseInt(elemDescriptor[1]);
				currTuple = currTuple.newExpanded(models.get(model).getElements().get(element), models);
			}
			if (currTuple.getSize() > 0){
				solution.add(currTuple);
			}
		}
		System.out.println("MM solution score: " + AlgoUtil.calcGroupWeight(solution));
		//AlgoUtil.printTuples(solution);
		return solution;
	}
	
	public void createNewGame(String newFilePath){
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(newFilePath, "utf-8");
		} catch (FileNotFoundException | UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		writer.print("[");
		for (Object o : gameModel){
			JSONObject jsonObject =  (JSONObject) o;
			JSONArray elements = (JSONArray) jsonObject.get("alien");
			for (Object e : elements){
				writer.print("{\"alien\":[\"");
				writer.print(e);
				writer.print("\"]}");
			}
		}
		writer.print("]");
		writer.close();
	}
}