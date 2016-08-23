package core.execution;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;

public class ReshapeData {
	protected String inFilePath;
	
	public ReshapeData(String inFilePath){
		this.inFilePath = inFilePath;
	}
	
	public synchronized void  reshapeData() {
		FileOutputStream fileOut;
		FileInputStream fileIn;
		HSSFWorkbook workbook;
		String[] highlight = {"common_all", "common_one", "common_tuple.size", "no_hl"};
		String[] choose = {"bestLocal", "bestGlobal"};
		String [] modelSwitch = {"off", "on"};
		String[] reshuffle = {"none", "reorder+renew", "reorder"};
		String[] seedings = {"rand", "score_a", "score_d", "size_a", "size_d", "bar_a", "bar_d"};
		try {
			fileIn = new FileInputStream(new File(inFilePath));
			workbook = new HSSFWorkbook(fileIn);
			HSSFSheet origSheet = workbook.getSheet("Block Form");
			HSSFSheet reshapeSheet = workbook.getSheet("Long Form");
			if(reshapeSheet == null)
				reshapeSheet = workbook.createSheet("Long Form");
			int size = origSheet.getRow(0).getLastCellNum();
			Row newFirstRow = reshapeSheet.createRow(0);
			newFirstRow.createCell(0).setCellValue("case");
			newFirstRow.createCell(1).setCellValue("highlight");
			newFirstRow.createCell(2).setCellValue("choose");
			newFirstRow.createCell(3).setCellValue("reshuffle");
			newFirstRow.createCell(4).setCellValue("seed");
			newFirstRow.createCell(5).setCellValue("time");
			for(int i = 1; i <= 16; i++){
				Row oldRow = origSheet.getRow(i);
				for (int j = 0; j < size - 1; j++){
					Row newRow = reshapeSheet.createRow((i - 1) * (size - 1) + j + 1);
					newRow.createCell(0).setCellValue(oldRow.getCell(0).getStringCellValue());
					newRow.createCell(1).setCellValue(highlight[j / 42]);
					newRow.createCell(2).setCellValue(choose[(j % 42) / 21]);
					newRow.createCell(3).setCellValue(reshuffle[(j % 21) / 7]);
					//System.out.println(oldRow.getCell(j + 1).getNumericCellValue());
					newRow.createCell(4).setCellValue(seedings[j % 7]);
					newRow.createCell(5).setCellValue(oldRow.getCell(j + 1).getNumericCellValue());
				}
			}
			fileIn.close();
			fileOut = new FileOutputStream(new File(inFilePath));
			workbook.write(fileOut); 
			fileOut.close();
		}catch(Exception e){
			e.printStackTrace();
			
		}
			
	}
}
