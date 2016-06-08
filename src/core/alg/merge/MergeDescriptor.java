package core.alg.merge;

import core.common.N_WAY;
import core.common.N_WAY.ALG_POLICY;

public class MergeDescriptor {

	public ALG_POLICY algPolicy;
	public boolean asc;
	public N_WAY.ORDER_BY orderBy;
	public boolean elementAsc;

	public MergeDescriptor(N_WAY.ALG_POLICY algPolicy, boolean asc, N_WAY.ORDER_BY orderBy) {
		this.algPolicy = algPolicy;
		this.asc = asc;
		this.orderBy = orderBy;
		this.elementAsc = false;
	}
	
	public MergeDescriptor(boolean modelAsc, boolean elementAsc, N_WAY.ORDER_BY orderBy) {
		this.algPolicy = N_WAY.ALG_POLICY.RANDOM;
		this.asc = modelAsc;
		this.elementAsc = elementAsc;
		this.orderBy = orderBy;
	}
	
	public static MergeDescriptor EMPTY = new MergeDescriptor(N_WAY.ALG_POLICY.PAIR_WISE, true, N_WAY.ORDER_BY.MODEL_ID);

}
