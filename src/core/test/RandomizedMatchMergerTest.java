package core.test;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import core.alg.merge.MergeDescriptor;
import core.alg.merge.RandomizedMatchMerger;
import core.common.N_WAY;
import core.domain.Model;

public class RandomizedMatchMergerTest {
	
	protected ArrayList<Model> hospitalModels;
	protected MergeDescriptor md1;
	protected RandomizedMatchMerger rmm1;
	
	
	@Before
	public void setUp() throws Exception {
		hospitalModels = Model.readModelsFile("models/hospitals.csv");
		md1 = new MergeDescriptor(false, false, N_WAY.ORDER_BY.PROPERTY, highlight, choose, randomize, switchTuples, 0);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testExpectedMatchesAndScore(){
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
