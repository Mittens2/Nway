package core.alg;

import java.util.ArrayList;

import core.domain.Model;
import core.domain.Tuple;

public interface Matchable {
	 
	 ArrayList<Tuple> getTuplesInMatch();
	 ArrayList<Model> getModels();
}
