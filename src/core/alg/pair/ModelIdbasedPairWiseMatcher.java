package core.alg.pair;

import java.util.ArrayList;
import java.util.Comparator;

import core.alg.merge.HungarianMerger;
import core.common.ModelIdComparator;
import core.domain.Model;

public class ModelIdbasedPairWiseMatcher extends PairWiseMatch {

	private boolean largerFirst;

	public ModelIdbasedPairWiseMatcher(ArrayList<Model> lst, boolean largerFirst) {
		super((largerFirst?"Pairwise higher id First":"Pairwise lower id First"),lst, largerFirst);
		this.largerFirst = largerFirst;
	}

	@Override
	public Comparator<HungarianMerger> getPolicyComperator(final boolean largerFrst) {
		return new ModelIdComparator(largerFrst);
	}
}