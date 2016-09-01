package core.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
public static ArrayList<Tuple> solutionScoreCalculator(String filePath, String modelsFile){
	ArrayList<Model> models = Model.readModelsFile(modelsFile);
	if (models.size() > 16){
		models = new ArrayList<Model>(models.subList(0, 10));
	}
	ArrayList<Tuple> solution = new ArrayList<Tuple>();
	JSONParser parser = new JSONParser();
	JSONArray gameModel = null;
	try {
		gameModel = (JSONArray) parser.parse(new FileReader(filePath));
		System.out.println(gameModel);
		} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (org.json.simple.parser.ParseException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
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
		solution.add(currTuple);
	}
	System.out.println(AlgoUtil.calcGroupWeight(solution));
	return solution;
	}
}