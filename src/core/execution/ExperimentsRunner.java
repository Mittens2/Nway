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
	static class ExperimentRunner implements Callable<double[][]>{  
		private String modelsFile;
		private String resultsFile;
		private int runsToAvg;
		private int divideUp;
		private int numOfModelsToUse;
		private int numCases;
		private int expType;
		
		public ExperimentRunner(String modelsFiles, String resultsFiles, int runsToAvg,
				int divideUp, int numOfModelsToUse, int numCases, int expType) {
	        this.modelsFile = modelsFiles;
	        this.resultsFile = resultsFiles;
	        this.runsToAvg = runsToAvg;
	        this.divideUp = divideUp;
	        this.numOfModelsToUse = numOfModelsToUse;
	        this.numCases = numCases;
	        this.expType = expType;
		}
    
        @Override
        public double[][] call(){
        	/**
    		 * Runs a simple experiment (i.e. produces graphs that do not show the spread of scores).
    		 * Saves all of the graphs of the different setting of the algorithm being compared to NwM.
    		 */
        	if (expType == 0){
        		return runBigExp();
        	}
        	else{
        		return runSeedExp();
        	}
        }
        	
        private double[][] runBigExp(){
    		double[][] expResults = new double[5][numCases];
			ArrayList<Model> models = Model.readModelsFile(modelsFile);
			String subcase = modelsFile.substring(modelsFile.lastIndexOf("/") + 1, modelsFile.indexOf("."));
			if (models.size() > divideUp){
				models = new ArrayList<Model>(models.subList(0, numOfModelsToUse));
			}
			double[][] scores = null;
			double[] timeSums = null;
			double[] firstChangeSums = null;
			double[] gapSums = null;
			for (int j = 0; j < runsToAvg; j++){
				Runner runner = new Runner(models, resultsFile, null, -1, false);
				runner.execute(subcase);
				ArrayList<RunResult> rrs = runner.getRunResults();
				if (scores == null){
					scores = new double[rrs.size()][runsToAvg];
					timeSums = new double[rrs.size()];
					firstChangeSums = new double[rrs.size()];
					gapSums = new double[rrs.size()];
				}
				for (int k = 0; k < rrs.size(); k++){
					timeSums[k] += rrs.get(k).execTime / 1000.0;
					scores[k][j] = rrs.get(k).weight.doubleValue();
					gapSums[k] += rrs.get(k).gap;
					firstChangeSums[k] += rrs.get(k).firstChange;
				}
			}
			//ResultsPlotter rp = new ResultsPlotter(subcase, "");
			for (int j = 0; j < scores.length; j++){
				//rp.addBarDataPoint(scoreSums[j] / runsToAvg, settings[j], subcase);
				Statistics stats = new Statistics(scores[j]);
				expResults[0][j] = stats.getMean();
				expResults[1][j] = gapSums[j] / runsToAvg;
				expResults[2][j] = firstChangeSums[j] / runsToAvg;
				expResults[3][j] = timeSums[j] / runsToAvg;
				expResults[4][j] = stats.getStdDev();
				
			}
			return expResults;
        }
        
        private double[][] runSeedExp(){
			ArrayList<Model> models = Model.readModelsFile(modelsFile);
			String subcase = modelsFile.substring(modelsFile.lastIndexOf("/") + 1, modelsFile.indexOf("."));
			if (models.size() > divideUp){
				models = new ArrayList<Model>(models.subList(0, numOfModelsToUse));
			}
			double nwmScore = -1;
			double[][] valueSums = null;
			for (int j = 0; j < runsToAvg; j++){
				Runner runner = new Runner(models, resultsFile, null, -1, false);
				runner.execute(subcase);
				ArrayList<RunResult> rrs = runner.getRunResults();
				if (valueSums == null){
					valueSums = new double[rrs.size() - 1][7];
				}
				nwmScore = rrs.get(0).weight.doubleValue();
				for (int k = 1; k < rrs.size(); k++){
					valueSums[k - 1][0] += rrs.get(k).weight.doubleValue();
					valueSums[k - 1][2] += rrs.get(k).execTime / 1000;
					valueSums[k - 1][3] += rrs.get(k).iterations;
					valueSums[k - 1][4] += rrs.get(k).firstChange;
					valueSums[k - 1][5] += rrs.get(k).gap;
					valueSums[k - 1][6] += rrs.get(k).seedsUsed;
				}
			}
			for (int k = 0; k < valueSums.length; k++){
				valueSums[k][1] = ((valueSums[k][0] / nwmScore - 1) * 100) * runsToAvg;
				for (int i = 0; i < valueSums[k].length; i++){
					valueSums[k][i] = valueSums[k][i] / runsToAvg;
				}
			}
			return valueSums;
        }
			
	}

	public static void runSeedExperiment(ArrayList<String> modelsFiles, ArrayList<String> resultsFiles, int runsToAvg, 
			int divideUp, int numOfModelsToUse){
		//FileInputStream fileIn;
		String[] mdLabels = {
				"rand", "score-a", "score-d", "size-a", "size-d", "bar-a", "bar-d",
				"rand, rs", "score-a, rs", "score-d, rs", "size-a, rs", "size-d, rs", "bar-a, rs", "bar-d, rs",
		};
		ExecutorService executor = Executors.newFixedThreadPool(15);
		List<ExperimentRunner> exps = new ArrayList<ExperimentRunner>();
		for (int i = 0; i < modelsFiles.size(); i++){
			exps.add(new ExperimentRunner(modelsFiles.get(i), resultsFiles.get(i), runsToAvg, divideUp,
					numOfModelsToUse, 0, 1));
		}
		List<Future<double[][]>> results = null;
		try {
			results = executor.invokeAll(exps);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		executor.shutdown();
		System.out.println(results.size());
		ArrayList<String> subcases = new ArrayList<String>();
		for (String mf: modelsFiles){
			subcases.add(mf.substring(mf.lastIndexOf("/") + 1, mf.indexOf(".")));
		}
		
		FileOutputStream fileOut;
		HSSFWorkbook workbook;
		HSSFSheet sheet;
		final DecimalFormat df = new DecimalFormat("###.#####");
		workbook = new HSSFWorkbook();
		sheet = workbook.createSheet("sheet1");
		Row header = sheet.createRow(0);
		header.createCell(0).setCellValue("case");
		header.createCell(1).setCellValue("params");
		header.createCell(2).setCellValue("hs_score");
		header.createCell(3).setCellValue("hs % improve");
		header.createCell(4).setCellValue("time");
		header.createCell(5).setCellValue("iterations");
		header.createCell(6).setCellValue("avg 1st change");
		header.createCell(7).setCellValue("avg gap");
		header.createCell(8).setCellValue("seeds used");
		try{
			for (int i = 0; i < results.size(); i++){
				double[][] valueSums = results.get(i).get();
				String subcase = subcases.get(i);
				for (int k = 0; k < valueSums.length; k++){
					Row newRow = sheet.createRow(sheet.getLastRowNum() + 1);
					newRow.createCell(0).setCellValue(subcase);
					newRow.createCell(1).setCellValue(mdLabels[k]);
					for (int j = 0; j < valueSums[k].length; j++){
						newRow.createCell(j + 2).setCellValue(df.format(valueSums[k][j]));
					}
				}
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		try{
			fileOut = new FileOutputStream(new File(Main.home + "results/seedStats.xls"));
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
					numOfModelsToUse, numCases, 0));
		}
		List<Future<double[][]>> results = null;
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
		//FileInputStream fileIn;
		//FileOutputStream fileOut;
		//HSSFWorkbook workbook;
		ArrayList<HSSFWorkbook> workbooks;
		HSSFSheet sheet;
		try{
			//fileIn = new FileInputStream(new File(Main.home + "results/experimentResults.xls"));
			//workbook = new HSSFWorkbook();
			workbooks = new ArrayList<HSSFWorkbook>();
			for (int j = 0; j < 5; j++){
				HSSFWorkbook workbook = new HSSFWorkbook();
				sheet = workbook.createSheet("Block Form");
				Row header = sheet.createRow(0);
				header.createCell(0).setCellValue("Case");
				for (int i = 1; i <= numCases; i++){
					header.createCell(i).setCellValue("c" + i);
				}
				for (int i = 0; i < subcases.size(); i++) {
					Row newRow = sheet.createRow(sheet.getLastRowNum() + 1);
					newRow.createCell(0).setCellValue(subcases.get(i));
					int cell = 1;
					System.out.println(i + " " + j);
					for (Double score: results.get(i).get()[j]){
						newRow.createCell(cell).setCellValue(score);
						cell++;
					}
				} 
				workbooks.add(workbook);
			}
		}
		catch (Exception e){
			e.printStackTrace();
			return;
		} 
		try {
			ArrayList<FileOutputStream> fileOuts = new ArrayList<FileOutputStream>();
			fileOuts.add(new FileOutputStream(new File(Main.home + "results/scoreResults.xls")));
			fileOuts.add(new FileOutputStream(new File(Main.home + "results/changeResults.xls")));
			fileOuts.add(new FileOutputStream(new File(Main.home + "results/gapResults.xls")));
			fileOuts.add(new FileOutputStream(new File(Main.home + "results/timeResults.xls")));
			fileOuts.add(new FileOutputStream(new File(Main.home + "results/stdDevResults.xls")));
			for (int i = 0; i < workbooks.size(); i++){
				workbooks.get(i).write(fileOuts.get(i));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}