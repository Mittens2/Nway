package core.test;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import core.alg.merge.MergeDescriptor;
import core.alg.merge.RandomizedMatchMerger;
import core.common.AlgoUtil;
import core.common.N_WAY;
import core.domain.Element;
import core.domain.Model;
import core.domain.Tuple;

public class RandomizedMatchMergerTest {
	
	protected ArrayList<Model> hospitalModels;
	protected ArrayList<Model> toycase2Models;
	protected ArrayList<Model> toycase4Models;
	protected ArrayList<Model> toycase5Models;
	protected MergeDescriptor md_hl0_sd2;
	protected MergeDescriptor md_hl0_sd1d;
	protected MergeDescriptor md_hl1_sd2;
	protected MergeDescriptor md_hl2_sd2;
	protected MergeDescriptor md_hl2_sd5d;
	protected MergeDescriptor md_hl3_sd2;
	protected RandomizedMatchMerger rmm;
	
	
	@Before
	public void setUp() throws Exception {
		hospitalModels = Model.readModelsFile("models/hospitals.csv");
		toycase2Models = Model.readModelsFile("models/toycase2.csv");
		toycase4Models = Model.readModelsFile("models/toycase4.csv"); 
		toycase5Models = Model.readModelsFile("models/toycase5.csv");
		// md -> models_asc, elem_asc, hl, ch, st, sd.
		md_hl0_sd2 = new MergeDescriptor(false, false, 0, 0, true, 2);
		md_hl0_sd1d = new MergeDescriptor(false, false, 0, 0, true, 1);
		md_hl1_sd2 = new MergeDescriptor(false, false, 1, 0, true, 2);
		md_hl2_sd2 = new MergeDescriptor(false, false, 2, 0, true, 2);
		md_hl2_sd5d = new MergeDescriptor(false, false, 2, 0, true, 5);
		md_hl3_sd2 = new MergeDescriptor(false, false, 2, 0, true, 2);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testExpectedMatchesAndScore(){
		double epsilon = 0.0000001;
		ArrayList<Tuple> soln = new ArrayList<Tuple>();
		Model m1;
		Model m2;
		Model m3;
		Tuple t1;
		Tuple t2;
		//Test 1
		for (Model m: toycase4Models){
			for (Element e: m.getElements()){
				Tuple t = new Tuple().newExpanded(e, toycase4Models);
				soln.add(t);
			}
		}
		rmm = new RandomizedMatchMerger((ArrayList<Model>) toycase4Models.clone(), md_hl0_sd2);
		rmm.run();
		assertEquals(soln, rmm.getTuplesInMatch());
		assertEquals(AlgoUtil.calcGroupWeight(rmm.getTuplesInMatch()), BigDecimal.ZERO);
		
		// Test 2
		soln = new ArrayList<Tuple>();
		m1 = toycase5Models.get(0);
		m2 = toycase5Models.get(1);
		t1 = new Tuple().newExpanded(m1.getElementByLabel('"' + "[1,2]" + '"'), toycase5Models);
		t1 = t1.newExpanded(m2.getElementByLabel('"' + "[2,3]" + '"'), toycase5Models);
		t2 = new Tuple().newExpanded(m1.getElementByLabel('"' + "[3,4]" + '"'), toycase5Models);
		t2 = t2.newExpanded(m2.getElementByLabel('"' + "[4,5]" + '"'), toycase5Models);
		soln.add(t1);
		soln.add(t2);
		rmm = new RandomizedMatchMerger((ArrayList<Model>) toycase5Models.clone(), md_hl0_sd2);
		rmm.run();
		assertEquals(soln, rmm.getTuplesInMatch());
		assertEquals(new BigDecimal(2.0 / 3.0, N_WAY.MATH_CTX).doubleValue(), 
				AlgoUtil.calcGroupWeight(rmm.getTuplesInMatch()).doubleValue(), epsilon);
		
		// Test 3
		soln = new ArrayList<Tuple>();
		m1 = toycase2Models.get(0);
		m2 = toycase2Models.get(1);
		m3 = toycase2Models.get(2);
		t1 = new Tuple().newExpanded(m1.getElements().get(0), toycase2Models);
		t1 = t1.newExpanded(m2.getElements().get(0), toycase2Models);
		t2 = new Tuple().newExpanded(m3.getElements().get(0), toycase2Models);
		soln.add(t1);
		soln.add(t2);
		rmm = new RandomizedMatchMerger((ArrayList<Model>) toycase2Models.clone(), md_hl0_sd2);
		rmm.run();
		assertEquals(soln, rmm.getTuplesInMatch());
		assertEquals(new BigDecimal(4.0 / 18.0, N_WAY.MATH_CTX).doubleValue(), 
				AlgoUtil.calcGroupWeight(rmm.getTuplesInMatch()).doubleValue(), epsilon);
		
		// Test 4
		ArrayList<Tuple> soln1 = new ArrayList<Tuple>();
		ArrayList<Tuple> soln2 = new ArrayList<Tuple>();
		t1 = new Tuple().newExpanded(m2.getElements().get(0), toycase2Models);
		t1 = t1.newExpanded(m1.getElements().get(0), toycase2Models);
		t2 = new Tuple().newExpanded(m3.getElements().get(0), toycase2Models);
		soln1.add(t1);
		soln1.add(t2);
		t1 = new Tuple().newExpanded(m2.getElements().get(0), toycase2Models);
		t1 = t1.newExpanded(m3.getElements().get(0), toycase2Models);
		t2 = new Tuple().newExpanded(m1.getElements().get(0), toycase2Models);
		soln2.add(t1);
		soln2.add(t2);
		rmm = new RandomizedMatchMerger((ArrayList<Model>) toycase2Models.clone(), md_hl0_sd1d);
		rmm.run();
		System.out.println(rmm.getTuplesInMatch());
		assertTrue(soln1.equals(rmm.getTuplesInMatch()) || soln2.equals(rmm.getTuplesInMatch()));
		assertEquals(new BigDecimal(4.0 / 18.0, N_WAY.MATH_CTX).doubleValue(), 
				AlgoUtil.calcGroupWeight(rmm.getTuplesInMatch()).doubleValue(), epsilon);
		
		// Test 5
		rmm = new RandomizedMatchMerger((ArrayList<Model>) toycase2Models.clone(), md_hl2_sd2);
		rmm.run();
		assertEquals(soln, rmm.getTuplesInMatch());
		assertEquals(new BigDecimal(4.0 / 18.0, N_WAY.MATH_CTX).doubleValue(), 
				AlgoUtil.calcGroupWeight(rmm.getTuplesInMatch()).doubleValue(), epsilon);
		
		// Test 6
		rmm = new RandomizedMatchMerger((ArrayList<Model>) toycase2Models.clone(), md_hl2_sd5d);
		rmm.run();
		System.out.println(rmm.getTuplesInMatch());
		assertTrue(soln1.equals(rmm.getTuplesInMatch()) || soln2.equals(rmm.getTuplesInMatch()));
		assertEquals(new BigDecimal(4.0 / 18.0, N_WAY.MATH_CTX).doubleValue(), 
				AlgoUtil.calcGroupWeight(rmm.getTuplesInMatch()).doubleValue(), epsilon);
		
		// Test 7
		soln = new ArrayList<Tuple>();
		t1 = new Tuple().newExpanded(m1.getElements().get(0), toycase2Models);
		t1 = t1.newExpanded(m2.getElements().get(0), toycase2Models);
		t1 = t1.newExpanded(m3.getElements().get(0), toycase2Models);
		soln.add(t1);
		rmm = new RandomizedMatchMerger((ArrayList<Model>) toycase2Models.clone(), md_hl3_sd2);
		rmm.run();
		assertEquals(soln, rmm.getTuplesInMatch());
		assertEquals(new BigDecimal(4.0 / 9.0, N_WAY.MATH_CTX).doubleValue(), 
				AlgoUtil.calcGroupWeight(rmm.getTuplesInMatch()).doubleValue(), epsilon);
		
		// Test 8
		rmm = new RandomizedMatchMerger((ArrayList<Model>) toycase2Models.clone(), md_hl1_sd2);
		rmm.run();
		assertEquals(soln, rmm.getTuplesInMatch());
		assertEquals(new BigDecimal(4.0 / 9.0, N_WAY.MATH_CTX).doubleValue(), 
				AlgoUtil.calcGroupWeight(rmm.getTuplesInMatch()).doubleValue(), epsilon);
				

		
		
		
	}

	@Test
	public void testMemoryLeak() {
		
	}

	@Test
	public void testDeterministicSeeding() {
		
	}
	
	@Test
	public void testNonDeterministicSeeding() {
		
	}

	@Test
	public void testDuplicateTuples() {
		
	}
	
	@Test
	public void testDuplicateElements(){
		
	}
	
	@Test
	public void testAllDiffModelsInMatch(){
		
	}
	
	@Test
	public void testRMMandNWMdiff(){
		
	}

}
