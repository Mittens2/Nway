package core.common;

import java.awt.Font;
import java.math.BigDecimal;
import java.util.ArrayList;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.function.Function2D;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.ApplicationFrame;


public class ResultsPlotter extends ApplicationFrame {
	 private ArrayList<String> caseLabels;
	 private String alg1;
	 private String alg2;
	
	public ResultsPlotter(final String title, String alg1, String alg2, ArrayList<String> caseLabels){
		super(title);
		this.alg1 = alg1;
		this.alg2 = alg2;
		this.caseLabels = caseLabels;
	}
	
	private CategoryDataset createDataset(ArrayList<BigDecimal> results, String type){
		final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		for (int i = 0; i < results.size(); i++){
			dataset.addValue(results.get(i), type, caseLabels.get(i));
			dataset.addValue(i + 1, "x = y", caseLabels.get(i));
		}
		return dataset;
	}
	
	public void createChart(ArrayList<BigDecimal> alg1data, ArrayList<BigDecimal> alg2data){
		ArrayList<BigDecimal> results = new ArrayList<BigDecimal>();
		for (int i = 0; i < alg1data.size(); i++){
			results.add(alg1data.get(i).divide(alg2data.get(i)));
		}
		final CategoryDataset dataset = createDataset(results, "alg1/alg2");
        final NumberAxis rangeAxis = new NumberAxis("Ratio");
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        final LineAndShapeRenderer renderer = new LineAndShapeRenderer();
        renderer.setSeriesLinesVisible(0, false);
        renderer.setSeriesShapesVisible(1, false);
        final CategoryAxis domainAxis = new CategoryAxis("Category");
        final CategoryPlot plot = new CategoryPlot(dataset, domainAxis, rangeAxis, renderer);
        plot.setDomainGridlinesVisible(true);
        
        final JFreeChart result = new JFreeChart(
            "Combined Domain Category Plot Demo",
            new Font("SansSerif", Font.BOLD, 12),
            plot,
            true
        );
        final ChartPanel chartPanel = new ChartPanel(result);
        chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
        setContentPane(chartPanel);
	}
	
}

