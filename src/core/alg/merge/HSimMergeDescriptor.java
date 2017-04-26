package core.alg.merge;

import core.common.N_WAY;
import core.common.N_WAY.ALG_POLICY;
import core.common.N_WAY.ORDER_BY;



public class HSimMergeDescriptor extends MergeDescriptor {
	
	public enum Seed{
		RANDOM,
		SIZE,
		WEIGHT,
		BAR
	}
	
	public enum Highlight{
		ONE_ALL,
		ONE_ONE,
		TUPLE_SIZE,
		NONE
	}
	
	public enum Choose{
		LOCAL,
		GLOBAL,
		BAR
	}
	
	public enum Reshuffle{
		NONE,
		ALL,
		UNUSED
	}
	
	boolean elementAsc;
	Seed seed;
	Highlight highlight;
	Choose choose;
	Reshuffle reshuffle;
	
	public HSimMergeDescriptor(N_WAY.ALG_POLICY pol, boolean modelAsc, boolean elementAsc, Highlight highlight, 
			Choose choose, Reshuffle reshuffle, Seed seed) {
		super(pol, modelAsc, null);
		this.seed = seed;
		this.elementAsc = elementAsc;
		this.highlight = highlight;
		this.choose = choose;
		this.reshuffle = reshuffle;
	}
}
