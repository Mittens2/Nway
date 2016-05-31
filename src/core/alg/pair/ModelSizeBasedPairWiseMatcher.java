package core.alg.pair;

import java.util.ArrayList;
import java.util.Comparator;

import core.alg.merge.HungarianMerger;
import core.domain.Model;

public class ModelSizeBasedPairWiseMatcher extends PairWiseMatch{

	private boolean largestFirst;
	
	public ModelSizeBasedPairWiseMatcher(ArrayList<Model> lst, boolean useLargestFirst) {
		super(useLargestFirst?"Pairwise Largest Models First":"Pairwise Smallest Models First",lst, useLargestFirst);
		largestFirst = useLargestFirst;
	}

	@Override
	public Comparator<HungarianMerger> getPolicyComperator(final boolean largerFrst) {
		// TODO Auto-generated method stub
		
		return new Comparator<HungarianMerger>() {
			@Override
			public int compare(HungarianMerger mp1, HungarianMerger mp2) {
				if(largestFirst){
					if(mp1.getLargerModel().size() > mp2.getLargerModel().size()) 
						return -1;
					if(mp1.getLargerModel().size() ==   mp2.getLargerModel().size()){
						if(mp1.getSmallerModel().size() > mp2.getSmallerModel().size()) 
							return -1;
						return 1;
					}
					return 1;
				}
				else{
					if(mp1.getSmallerModel().size() < mp2.getSmallerModel().size()) 
						return -1;
					if(mp1.getSmallerModel().size() ==   mp2.getSmallerModel().size()){
						if(mp1.getLargerModel().size() < mp2.getLargerModel().size()){
							return -1;
						}
						return 1;
					}
					return 1;
				}
			}
		};
	}
}
