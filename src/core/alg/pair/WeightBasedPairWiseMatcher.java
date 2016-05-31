package core.alg.pair;

import java.util.ArrayList;
import java.util.Comparator;

import core.alg.merge.HungarianMerger;
import core.domain.Model;

public class WeightBasedPairWiseMatcher extends PairWiseMatch {
	
	private boolean bestFirst = true;
	
	public WeightBasedPairWiseMatcher(ArrayList<Model> lst, boolean useBestFirst) {
		super(useBestFirst?"Pairwise Best Match First":"Pairwise Worst Match First",lst, useBestFirst);
		bestFirst = useBestFirst;
	}

	@Override
	public Comparator<HungarianMerger> getPolicyComperator(final boolean useBestFirst) {
		// TODO Auto-generated method stub
		
		return new Comparator<HungarianMerger>() {
			@Override
			public int compare(HungarianMerger mp1, HungarianMerger mp2) {
				if(bestFirst)
					return (mp1.getWeight().compareTo(mp2.getWeight()) < 0)?1:-1;
				return (mp1.getWeight().compareTo(mp2.getWeight()) <0)?-1:1;
			}
		};
	}

}
