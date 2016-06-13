package core.common;

import java.awt.Font;
import java.awt.geom.Ellipse2D;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.CombinedDomainCategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.function.Function2D;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.ApplicationFrame;


public class ResultsPlotter extends ApplicationFrame {
	 private ArrayList<String> caseLabels;
	 private final DefaultCategoryDataset catDataset;
	 private final DefaultBoxAndWhiskerCategoryDataset bwDataset;
	
	public ResultsPlotter(final String title, ArrayList<String> caseLabels){
		super(title);
		this.caseLabels = caseLabels;
		this.catDataset = new DefaultCategoryDataset();
		this.bwDataset = new DefaultBoxAndWhiskerCategoryDataset();
	}
	
	private void createCategoryDataset(ArrayList<BigDecimal> results){
		for (int i = 0; i < results.size(); i++){
			catDataset.addValue(results.get(i), "alg1/alg2", caseLabels.get(i));
		}
	}
	
	private void createXYCategoryDataset(){
		for (int i = 0; i < caseLabels.size(); i++){
			catDataset.addValue(i + 1, "x = y", caseLabels.get(i));
		}
	}
	
	private void createBoxAndWhiskerDataset(ArrayList<ArrayList<BigDecimal>> results){
		for (int i = 0; i < results.size(); i++){
			bwDataset.add(results.get(i), "alg1/alg2", caseLabels.get(i));
		}
	}
	
	public void createChartSingle(ArrayList<BigDecimal> alg1data, ArrayList<BigDecimal> alg2data){
		ArrayList<BigDecimal> results = new ArrayList<BigDecimal>();
		for (int i = 0; i < alg1data.size(); i++){
			results.add(alg1data.get(i).divide(alg2data.get(i)));
		}
		createCategoryDataset(results);
        final NumberAxis rangeAxis = new NumberAxis("Ratio");
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        final LineAndShapeRenderer renderer = new LineAndShapeRenderer();
        
        renderer.setSeriesLinesVisible(0, false);
        renderer.setSeriesShapesVisible(1, false);
        renderer.setSeriesShape(0, new Ellipse2D.Double(-3, -3, 6, 6));
        final CategoryAxis domainAxis = new CategoryAxis("Category");
        final CategoryPlot plot = new CategoryPlot(catDataset, domainAxis, rangeAxis, renderer);
        plot.setDomainGridlinesVisible(true);
        
        final JFreeChart result = new JFreeChart(
            this.getTitle(),
            new Font("SansSerif", Font.BOLD, 12),
            plot,
            true
        );
        final ChartPanel chartPanel = new ChartPanel(result);
        chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
        setContentPane(chartPanel);
	}
	
	public void createChartMultiple(ArrayList<ArrayList<BigDecimal>> alg1data, 
			ArrayList<ArrayList<BigDecimal>> alg2data){
		ArrayList<ArrayList<BigDecimal>> result = new ArrayList<ArrayList<BigDecimal>>();
		for (int i = 0; i < alg1data.size(); i++){
			ArrayList<BigDecimal> ratio = new ArrayList<BigDecimal>();
			ArrayList<BigDecimal> run1 = alg1data.get(i);
			ArrayList<BigDecimal> run2 = alg2data.get(i);
			for (int j = 0; j < run1.size(); j++){
				ratio.add(run1.get(j).divide(run2.get(j)));
			}
			result.add(ratio);
		}
		createBoxAndWhiskerDataset(result);
		createXYCategoryDataset();
		
		final NumberAxis rangeAxis = new NumberAxis("Ratio");
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        final LineAndShapeRenderer renderer2 = new LineAndShapeRenderer();
        final BoxAndWhiskerRenderer renderer1 = new BoxAndWhiskerRenderer();
        
        renderer2.setSeriesShapesVisible(0, false);
        final CategoryAxis domainAxis = new CategoryAxis("Category");
        final CategoryPlot subplot2 = new CategoryPlot(catDataset, null, rangeAxis, renderer1);
        final CategoryPlot subplot1 =  new CategoryPlot(bwDataset, null, rangeAxis, renderer2);
        final CombinedDomainCategoryPlot plot = new CombinedDomainCategoryPlot(domainAxis);
        plot.add(subplot1);
        plot.add(subplot2);
        
        final JFreeChart chart = new JFreeChart(
            this.getTitle(),
            new Font("SansSerif", Font.BOLD, 12),
            plot,
            true
        );
        final ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
        setContentPane(chartPanel);
		

	}

}

