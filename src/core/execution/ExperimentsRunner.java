package core.execution;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;

import core.Main;
import core.common.ResultsPlotter;
import core.common.Statistics;
import core.domain.Model;

public class ExperimentsRunner{
	static class ExperimentRunner implements Callable<Double[][]>{  
		private String modelsFile;
		private String resultsFile;
		private int runsToAvg;
		private int divideUp;
		private int numOfModelsToUse;
		private int numCases;
		
		public ExperimentRunner(String modelsFiles, String resultsFiles, int runsToAvg,
				int divideUp, int numOfModelsToUse, int numCases) {
	        this.modelsFile = modelsFiles;
	        this.resultsFile = resultsFiles;
	        this.runsToAvg = runsToAvg;
	        this.divideUp = divideUp;
	        this.numOfModelsToUse = numOfModelsToUse;
	        this.numCases = numCases;
		}
    
        @Override
        public Double[][] call(){
        	/**
    		 * Runs a simple experiment (i.e. produces graphs that do not show the spread of scores).
    		 * Saves all of the graphs of the different setting of the algorithm being compared to NwM.
    		 */
    		Double[][] expResults = new Double[4][numCases];
			ArrayList<Model> models = Model.readModelsFile(modelsFile);
			String subcase = modelsFile.substring(modelsFile.lastIndexOf("/") + 1, modelsFile.indexOf("."));
			if (models.size() > divideUp){
				models = new ArrayList<Model>(models.subList(0, numOfModelsToUse));
			}
			double[][] scores = null;
			double[] timeSums = null;
			int[] iterSums = null;
			for (int j = 0; j < runsToAvg; j++){
				Runner runner = new Runner(models, resultsFile, null, -1, false);
				runner.execute(subcase);
				ArrayList<RunResult> rrs = runner.getRunResults();
				if (scores == null){
					scores = new double[rrs.size()][runsToAvg];
					timeSums = new double[rrs.size()];
					iterSums = new int[rrs.size()];
				}
				for (int k = 0; k < rrs.size(); k++){
					timeSums[k] += rrs.get(k).execTime / 1000.0;
					iterSums[k] += rrs.get(k).iterations;
					scores[k][j] = rrs.get(k).weight.doubleValue();
				}
			}
			//ResultsPlotter rp = new ResultsPlotter(subcase, "");
			for (int j = 0; j < scores.length; j++){
				//expResults[3][j] = ((double) iterSums[j]) / runsToAvg;
				//expResults[2][j] = timeSums[j] / runsToAvg;
				//rp.addBarDataPoint(scoreSums[j] / runsToAvg, settings[j], subcase);
				Statistics stats = new Statistics(scores[j]);
				expResults[0][j] = stats.getMean();
				expResults[1][j] = stats.getStdDev();
				
			}
			return expResults;
        }
	}
	
	public static void runSingleExperiment(ArrayList<String> modelsFiles, ArrayList<String> resultsFiles, int runsToAvg, 
			int divideUp, int numOfModelsToUse){
		//FileInputStream fileIn;
		FileOutputStream fileOut;
		HSSFWorkbook workbook;
		HSSFSheet sheet;
		final DecimalFormat df = new DecimalFormat("###.###");
		try{
			//fileIn = new FileInputStream(new File("results/bestParams.xls"));
			workbook = new HSSFWorkbook();
			sheet = workbook.getSheet("sheet1");
			if (sheet == null){
				sheet = workbook.createSheet("sheet1");
				Row header = sheet.createRow(0);
				header.createCell(0).setCellValue("case");
				header.createCell(1).setCellValue("nwm_score");
				header.createCell(2).setCellValue("hs % improve");
				header.createCell(3).setCellValue("iterations");
				header.createCell(4).setCellValue("time");
			}
		}
		catch (Exception e){
			e.printStackTrace();
			return;
		}
		for (int i = 0; i < modelsFiles.size(); i++){
			String mf = modelsFiles.get(i);
			String rf = resultsFiles.get(i);
			ArrayList<Model> models = Model.readModelsFile(mf);
			String subcase = mf.substring(mf.lastIndexOf("/") + 1, mf.indexOf("."));
			if (models.size() > divideUp){
				models = new ArrayList<Model>(models.subList(0, numOfModelsToUse));
			}
			double[] valueSums = new double[4];
			for (int j = 0; j < runsToAvg; j++){
				Runner runner = new Runner(models, rf, null, -1, false);
				runner.execute(subcase);
				ArrayList<RunResult> rrs = runner.getRunResults();
				valueSums[0] += rrs.get(0).weight.doubleValue();
				valueSums[1] += rrs.get(1).weight.doubleValue();
				valueSums[2] += rrs.get(1).iterations;
				valueSums[3] += rrs.get(1).execTime / 1000;
			}
			valueSums[1] = ((valueSums[1] / valueSums[0] - 1) * 100) * runsToAvg;
			Row newRow = sheet.createRow(sheet.getLastRowNum() + 1);
			newRow.createCell(0).setCellValue(subcase);
			for (int j = 0; j < valueSums.length; j++){
				newRow.createCell(j + 1).setCellValue(df.format(valueSums[j] / runsToAvg));
			}
		}
		try{
			//fileIn.close();
			fileOut = new FileOutputStream(new File("results/bestParams.xls"));
			workbook.write(fileOut); 
			fileOut.close();
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public static void convertScoretoPercent(ArrayList<String> modelsFiles, ArrayList<String> resultsFiles){
		FileInputStream fileIn;
		FileOutputStream fileOut;
		HSSFWorkbook workbook;
		HSSFSheet percentSheet;
		HSSFSheet scoreSheet;
		final DecimalFormat df = new DecimalFormat("###.#####");
		Map<String, Double> nwmScores = new HashMap<String, Double>();
		for (int i = 0; i < modelsFiles.size(); i++){
			String mf = modelsFiles.get(i);
			String rf = resultsFiles.get(i);
			ArrayList<Model> models = Model.readModelsFile(mf);
			if (models.size() > 20){
				models = new ArrayList<Model>(models.subList(0, 10));
			}
			String subcase = mf.substring(mf.lastIndexOf("/") + 1, mf.indexOf("."));
			Runner runner = new Runner(models, rf, null, -1, false);
			runner.execute(subcase);
			ArrayList<RunResult> rrs = runner.getRunResults();
			nwmScores.put(subcase, rrs.get(0).weight.doubleValue());
		}
		try{
			fileIn = new FileInputStream(new File("/home/amit/SASUniversityEdition/myfolders/fullExp_newHS.xls"));
			workbook = new HSSFWorkbook(fileIn);
			scoreSheet = workbook.getSheet("Long Form");
			percentSheet = workbook.getSheet("Percent Form");
			if (percentSheet == null){
				percentSheet = workbook.createSheet("Percent Form");
			}
			Row header = percentSheet.createRow(0);
			header.createCell(0).setCellValue("case");
			header.createCell(1).setCellValue("highlight");
			header.createCell(2).setCellValue("choose");
			header.createCell(3).setCellValue("switchBuckets");
			header.createCell(4).setCellValue("reshuffle");
			header.createCell(5).setCellValue("seed");
			header.createCell(6).setCellValue("percent");
			for (int i = 1; i < scoreSheet.getLastRowNum(); i++){
				Row oldRow = scoreSheet.getRow(i);
				Row newRow = percentSheet.createRow(i);
				String subcase = oldRow.getCell(0).getStringCellValue();
				newRow.createCell(0).setCellValue(subcase);
				for (int j = 1; j < oldRow.getLastCellNum() - 1; j++){
					newRow.createCell(j).setCellValue(oldRow.getCell(j).getStringCellValue());
				}
				double score = oldRow.getCell(oldRow.getLastCellNum() - 1).getNumericCellValue();
				double percentIncr = (score / nwmScores.get(subcase) - 1) * 100;
				newRow.createCell(oldRow.getLastCellNum() - 1).setCellValue(percentIncr);
			}
			fileOut = new FileOutputStream(new File("/home/amit/SASUniversityEdition/myfolders/fullExp_newHS.xls"));
			workbook.write(fileOut); 
			fileIn.close();
			fileOut.close();
		}
		catch (Exception e){
			e.printStackTrace();
			return;
		}
	}
	
	public static void runConcurrentExperiment(ArrayList<String> modelsFiles, ArrayList<String> resultsFiles,
			int runsToAvg, int divideUp, int numOfModelsToUse, int numCases){
		ExecutorService executor = Executors.newFixedThreadPool(15);
		List<ExperimentRunner> exps = new ArrayList<ExperimentRunner>();
		for (int i = 0; i < modelsFiles.size(); i++){
			exps.add(new ExperimentRunner(modelsFiles.get(i), resultsFiles.get(i), runsToAvg, divideUp,
					numOfModelsToUse, numCases));
		}
		List<Future<Double[][]>> results = null;
		try {
			results = executor.invokeAll(exps);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		executor.shutdown();
		ArrayList<String> subcases = new ArrayList<String>();
		for (String mf: modelsFiles){
			subcases.add(mf.substring(mf.lastIndexOf("/") + 1, mf.indexOf(".")));
		}
		FileInputStream fileIn;
		FileOutputStream fileOut;
		HSSFWorkbook workbook;
		HSSFSheet sheet;
		try{
			fileIn = new FileInputStream(new File(Main.home + "results/experimentResults.xls"));
			workbook = new HSSFWorkbook(fileIn);
			sheet = workbook.getSheet("Block Form");
			if (sheet == null){
				sheet = workbook.createSheet("Block Form");
				Row header = sheet.createRow(0);
				header.createCell(0).setCellValue("Case");
				for (int i = 1; i <= numCases; i++){
					header.createCell(i).setCellValue("c" + i);
				}
			}
			for (int i = 0; i < subcases.size(); i++) {
				Row newRow = sheet.createRow(sheet.getLastRowNum() + 1);
				newRow.createCell(0).setCellValue(subcases.get(i));
				int cell = 1;
				for (Double score: results.get(i).get()[0]){
					newRow.createCell(cell).setCellValue(score);
					cell++;
				}
			}   
		}
		catch (Exception e){
			e.printStackTrace();
			return;
		} 
		try {
			fileOut = new FileOutputStream(new File(Main.home + "results/experimentResults.xls"));
			workbook.write(fileOut); 
			fileIn.close();
			fileOut.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}