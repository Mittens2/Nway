package core;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RefineryUtilities;

import core.alg.optimal.ParallelOptimal;
import core.alg.search.ACO;
import core.common.AlgoUtil;
import core.common.GameSolutionParser;
import core.common.ResultsPlotter;
import core.common.SolverDifference;
import core.common.Statistics;
import core.domain.Element;
import core.domain.Model;
import core.domain.Tuple;
import core.execution.ACORunner;
import core.execution.BatchRunner;
import core.execution.BatchRunner.BatchRunDescriptor;
import core.execution.UMLParser.UMLClass;
import core.execution.ExperimentsRunner;
import core.execution.RunResult;
import core.execution.Runner;
import core.execution.UMLParser;
import core.test.RandomizedMatchMergerTest;




public class Main {
	public static String home;
	
	/**
	 * Runs algorithms over the first numOfModelsToUse models from modelsFile in a single run.
	 * 
	 * @param modelsFile The name of the file to derive the models.
	 * @param resultsFile The name of the file which results should be written to.
	 * @param numOfModelsToUse The number of models to be used in the run.
	 * 
	 * @return List containining results of all runs.
	 */
	private static ArrayList<RunResult> singleBatchRun(String modelsFile, String resultsFile, int numOfModelsToUse, boolean toChunkify){
		ArrayList<Model> models = Model.readModelsFile(modelsFile);
		ArrayList<Model> newModels = new ArrayList<Model>();
		for (Model m: models){
			if (m.size() > 60){
				Collections.shuffle(m.getElements(), new Random(System.nanoTime()));
				int keep = new Random().nextInt(20) + 40;
				ArrayList<Element> newElems = new ArrayList<Element>();
				newElems.addAll(m.getElements().subList(0, keep));
				Model newModel = new Model(m.getId(), newElems);
				newModels.add(newModel);
			}
			else{
				newModels.add(m);
			}
		}
		models = newModels;
		String caseName = modelsFile.substring(modelsFile.lastIndexOf("/") + 1, modelsFile.indexOf("."));
		System.out.println(caseName + ", num models: " + models.size());
		Runner runner = new Runner(models, resultsFile, null, numOfModelsToUse, toChunkify);
		runner.execute(caseName);
		return runner.getRunResults();
	}
	
	/**
	 * Prints out size of S, and U (in WSP formulation)
	 * 
	 * @param modelsFile The name of the file to derive the models.
	 */
	public static void printStats(String modelsFile){
		String name = modelsFile.substring(modelsFile.lastIndexOf("/") + 1, modelsFile.indexOf("."));
		ArrayList<Model> models = Model.readModelsFile(modelsFile);
		if (models.size() > 50){
			models = new ArrayList<Model>(models.subList(0, 10));
		}
		int min = 100;
		int max = 0;
		double totes = 0;
		BigDecimal possibs = BigDecimal.ONE;
		for (Model mod: models){
			int size = mod.size();
			totes += size;
			possibs = possibs.multiply(new BigDecimal(mod.size() + 1));
			if (size > max)
				max = size;
			if (size < min)
				min = size;
		}
		
		DecimalFormat df = new DecimalFormat("0.0###E0");
		System.out.println(name + ": " +"size of S: " + df.format(possibs) + ", size of U: " + totes);
	}
	
	public static void main(String[] args) {
		// Set home to path where models/results files are located.
		Main.home = new File("").getAbsolutePath() + "/";
		// All models files.
		String hospitals = home + "models/Julia_study/hospitals.csv";
		String random = home + "models/Julia_study/random.csv";
		String randomLoose =home +  "models/Julia_study/randomLoose.csv";
		String audioControlSystem = home + "models/FH/AudioControlSystem.csv";
		String chatSystem = home + "models/FH_nogen/ChatSystem.csv";
		String notepad = home + "models/FH_nogen/fixed_notepad.csv";
		//String mobileMedia = home + "models/Egyed/MobileMedia.csv";
		
		// Models, Results, and mmResults used to compare HSim, NwM, and MM results
		ArrayList<String> models = new ArrayList<String>();
		
//		models.add(notepad);
//		models.add(audioControlSystem);
//		models.add(chatSystem);
		models.add(hospitals);
//		models.add(random);
//		models.add(randomLoose);
//		for (String m: models){
//			printStats(m);
//		}
		//ACORunner ar = new ACORunner();
		//ar.runHyperParams(models);
		singleBatchRun(random, null, 10, false);
		singleBatchRun(randomLoose, null, 10, false);
	}		
}
