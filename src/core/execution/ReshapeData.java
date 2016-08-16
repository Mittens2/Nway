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
		String[] seedings = {"rand", "score-a", "score-d", "size-a", "size-d", "bar-a", "bar-d"};
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
			newFirstRow.createCell(3).setCellValue("swithBuckets");
			newFirstRow.createCell(4).setCellValue("reshuffle");
			newFirstRow.createCell(5).setCellValue("seed");
			newFirstRow.createCell(6).setCellValue("score");
			for(int i = 1; i <= 1; i++){
				Row oldRow = origSheet.getRow(i);
				for (int j = 0; j < size - 1; j++){
					Row newRow = reshapeSheet.createRow((i - 1) * size + j + 1);
					newRow.createCell(0).setCellValue(oldRow.getCell(0).getStringCellValue());
					newRow.createCell(1).setCellValue(j / 56);
					newRow.createCell(2).setCellValue((j % 56) / 28);
					newRow.createCell(3).setCellValue((j % 28) / 14);
					//System.out.println(oldRow.getCell(j + 1).getNumericCellValue());
					newRow.createCell(4).setCellValue((j % 14) / 7);
					newRow.createCell(5).setCellValue(j % 7);
					newRow.createCell(6).setCellValue(oldRow.getCell(j + 1).getNumericCellValue());
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
