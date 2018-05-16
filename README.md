# N-way matching Algorithms

## Running Experiments
The code from Nway can be run in a number of ways to yield different results. Each experiment runs some algorithm that performs n-way model matching. Some experiments can only be run with a single algorithm at a time, while others can be run with as many as desired (specified alongside type of experiement). The dependencies - how many algorithms, variants, and models are needed to run the experiment - are listed under the description of each experimental run.

The following algorithms may be run with the code from N-way:
1. [Pairwise](https://github.com/Mittens2/Nway/tree/aco/src/core/alg/pair)
2. [Greedy](https://github.com/Mittens2/Nway/tree/aco/src/core/alg/greedy)
3. [NwM](https://github.com/Mittens2/Nway/blob/aco/src/core/alg/merge/HungarianMerger.java)
4. [HSim](https://github.com/Mittens2/Nway/blob/aco/src/core/alg/merge/HumanSimulator.java)
5. [Local Element Search](https://github.com/Mittens2/Nway/blob/aco/src/core/alg/search/Search.java)
6. [Ant-Colony Optimization](https://github.com/Mittens2/Nway/blob/aco/src/core/alg/search/ACO.java)

The descriptions of 1-3 can be found in https://people.csail.mit.edu/mjulia/publications/N-Way_Model_Merging_2013.pdf.
In order to designate which algorithms are run, go to the execute method in Runner.java, and uncomment lines that run desired algorithms.

Some algorithms (Pairwise, Greedy, and HSim) can be run with a number of different configurations. In order to designate the different configurations of the desired algorithm, go to the allPermOnAlg method in Runner.java and uncomment desired configurations of the algorithm.

Each type of experiment is run from the main method of the Main.java file. The different experimental runs available are described next.

### Full Experiment (Alg * 1)
The runFullExperiment(modelsFiles, resultsFiles, runsToAvg, divideUp, numCases) method in ExperimentsRunner.java can be used to run a “full experiment”, i.e. all desired variants of a single algorithm over all desired cases. The different fields are described below:
* modelsFiles: List containing filenames where models from which each case id read.
* resultsFiles: List containing filenames where results for each model suite is written.
* runsToAvg: If algorithm has some randomness inherent to it (i.e. Pairwise, Greedy with chunkify, or HSim), scores can be averaged over some number of runs.
* divideUp: If you don’t want to use every model from every case, then can designate maximum allowable models per case using this field.
* numCases: The total number of cases in the experiment, i.e., the total number of variants of the algorithm being run.

The results produce results on five different measurements - score, standard deviation, runtime, avg gap, and avg 1st change stored in results/scoreResults, results/stdDevResults.xls, results/timeResults.xls, results/gapResults.xls, results/changeResults.xls respectively. Each of these measurements is described in the results section.

### Single Variant Experiment (Alg * 1)
The runSingleVariantExperiment(models, results, runsToAvg, divideUp, numCases) method in ExperimentsRunner.java can be used to run a “single variant experiment”, i.e. a single variant of a single algorithm over all desired cases. The different fields are described below:
* modelsFiles: List containing filenames where models from which each case is read.
* resultsFiles: List containing filenames where results for each model suite is written.
* runsToAvg: If algorithm has some randomness inherent to it (i.e. Pairwise, Greedy with chunkify, or HSim), scores can be averaged over some number of runs.
divideUp: If you don’t want to use every model from every case, then can designate maximum allowable models per case using this field.

The results produced for the variant of the algorithm are stored in results/seedStats.xls. The different recordings are described in the results section.

### Single Case Run (Alg +)
The singleBatchRun(modelsFile, resultsFile, numOfModels, toChunkify) method in Main.java can be used to run any number of variants of any algorithm on a
* single case. Fields are described:
* modelsFile: Filename from which models are read.
* resultsFiles: Filenames where results for case are stored.
* numOfModels: How many models from the desired case you want to use.
* toChunkify: If running the Greedy algorithm, this flag designates whether Greedy is performed over chunks of elements, or the entire case at once.

The results for each variant run are printed in the console, as well as written to the resultsFile.

## Results

The results of a full experiment over every possible configuration of HSim improving on NwM, as well as the results of a single variant experiment with the best configuration of HSim are stored in the results folder. The results of the full experiment are stored in four different files - all_params_scores.xls, all_params_time.xls, all_params_gap.xls, and all_params_1stchange.xls. The results stored in each file are described:
* all_params_scores.xls: The different scores of the different configurations of HSim. Results can be viewed either in Block Form or Long Form. Scores in long form can also be viewed as relative percent improvement to the original NwM score.
* all_params_time.xls: The different runtimes of each configuration of HSim. Can be viewed in Block or Long form.
* all_params_gap.xls: The avg gap between successive changes that HSim makes, i.e., how many seeds are tried in between successive changes to the solution.
* all_params_1stchange.xls: The avg amount of seeds used per iteration before a change to the solution is made.

These are the same measurements that are made when running a full experiment as described in case studies, along with the standard deviation over the runs averaged to obtain each score.

The results folder also contains a file called best_params.xls. This file contains the results of a single variant run with the best configuration of HSim. It is organized by case. For each case the score obtained by HSim after improving NwM, the percent improvement of HSim over the original NwM score, the runtime, avg iterations, avg 1st change, avg gap, and total number of seeds used are recorded. These are the same measurements that are recorded when running a single variant experiment.

### Statistical Tests

Running Statistical Tests using SAS: Any of the SAS programs in the folder “SAS” can be used to run some statistical test on one of the datasets in the results folder. SAS can be downloaded from here: http://www.sas.com/en_us/software/university-edition.html. The different statistical tests available are described:
* IMPORT SHEET: Imports a desired dataset into SAS.
* PROC MIXED: Performs a linear mixed effect model regression test on the data. Should be used with the long form of any of the xls files produced by running a full experiment. Results show which parameters in HSim are significant, as well as the averages of each setting of the parameters.
* PROC TTEST: Performs a t-test on the data. Should be used with xls folder produced by running a single variant experiment. Results show whether difference between scores of HSim and NwM are significant.
