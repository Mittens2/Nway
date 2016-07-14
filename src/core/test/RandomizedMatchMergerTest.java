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
	protected RandomizedMatchMerger rmm1;
	
	
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
		ArrayList<Tuple> soln = new ArrayList<Tuple>();
		//Test 1
		for (Model m: toycase4Models){
			for (Element e: m.getElements()){
				Tuple t = new Tuple().newExpanded(e, toycase4Models);
				soln.add(t);
			}
		}
		rmm1 = new RandomizedMatchMerger((ArrayList<Model>) toycase4Models.clone(), md_hl0_sd2);
		rmm1.run();
		assertEquals(soln, rmm1.getTuplesInMatch());
		assertEquals(AlgoUtil.calcGroupWeight(rmm1.getTuplesInMatch()), BigDecimal.ZERO);
		
		// Test 2
		soln = new ArrayList<Tuple>();
		Model mod1 = toycase5Models.get(0);
		Model mod2 = toycase5Models.get(1);
		Tuple t1 = new Tuple().newExpanded(mod1.getElementByLabel('"' + "[1,2]" + '"'), toycase5Models);
		t1 = t1.newExpanded(mod2.getElementByLabel('"' + "[2,3]" + '"'), toycase5Models);
		Tuple t2 = new Tuple().newExpanded(mod1.getElementByLabel('"' + "[3,4]" + '"'), toycase5Models);
		t2 = t2.newExpanded(mod2.getElementByLabel('"' + "[4,5]" + '"'), toycase5Models);
		soln.add(t1);
		soln.add(t2);
		rmm1 = new RandomizedMatchMerger((ArrayList<Model>) toycase5Models.clone(), md_hl0_sd2);
		rmm1.run();
		assertEquals(soln, rmm1.getTuplesInMatch());
		assertEquals(new BigDecimal(2.0 / 3.0, N_WAY.MATH_CTX), AlgoUtil.calcGroupWeight(rmm1.getTuplesInMatch()));

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
