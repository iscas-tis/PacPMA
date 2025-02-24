/****************************************************************************

    PacPMA - the PAC-based Parametric Model Analyzer
    Copyright (C) 2023

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

 *****************************************************************************/

package pacpma.options;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import nlopt.Algorithm;
import pacpma.algebra.Constant;
import pacpma.algebra.Parameter;
import pacpma.algebra.TemplateFunction;
import pacpma.algebra.function.ExpressionFunction;
import pacpma.algebra.polynomial.Polynomial;
import pacpma.approach.Approach;
import pacpma.approach.direct.DIRECTApproach;
import pacpma.approach.scenario.ScenarioApproach;
import pacpma.log.OnfileLogEngine;
import pacpma.log.LogEngine;
import pacpma.log.NullLogEngine;
import pacpma.log.InmemoryLogEngine;
import pacpma.lp.solver.LPSolver;
import pacpma.lp.solver.lpsolve.LPSolveLibrary;
import pacpma.lp.solver.matlab.MatlabFileTool;
import pacpma.lp.solver.matlab.MatlabTool;
import pacpma.lp.solver.octave.OctaveFileTool;
import pacpma.lp.solver.octave.OctaveTool;
import pacpma.modelchecker.batch.BatchModelChecker;
import pacpma.modelchecker.batch.prism.PrismSMCTool;
import pacpma.modelchecker.batch.storm.StormCWrapper;
import pacpma.modelchecker.batch.storm.StormPython;
import pacpma.modelchecker.batch.storm.StormTool;
import pacpma.modelchecker.batch.storm.StormsmcCWrapper;
import pacpma.modelchecker.batch.synthetic.SyntheticOctave;
import pacpma.util.Util;

/**
 * Options for the PAC-based Parametric Model Analyzer.
 * 
 * Options are taken from the invocation at command line, checked for their
 * presence when mandatory, and made available to the other classes.
 * 
 * @author Andrea Turrini
 *
 */
public class OptionsPacPMA {
    private final static String EMPTY = "";
    
    private final static String SPACE = " ";

    private final static String COMMA = ",";
    private final static String EQUAL = "=";
    private final static String COLON = ":";

    private final static Collection<String> COLLECTION_VERTICES = new HashSet<>();
    static {
        COLLECTION_VERTICES.add("+");
        COLLECTION_VERTICES.add("add");
    }

    public final static String MODELTYPE_PRISM = "prism";
    public final static String MODELTYPE_JANI = "jani";
    private final static String DEFAULT_MODELTYPE = MODELTYPE_PRISM;
    private final static Collection<String> COLLECTION_MODELTYPE = new HashSet<>(2);
    static {
        COLLECTION_MODELTYPE.add(MODELTYPE_PRISM);
        COLLECTION_MODELTYPE.add(MODELTYPE_JANI);
    }
    
    public final static String APPROACH_SCENARIO = "scenario";
    public final static String APPROACH_DIRECT = "direct";
    private final static String DEFAULT_APPROACH = APPROACH_SCENARIO;
    private final static Collection<String> COLLECTION_APPROACH = new HashSet<>();
    static {
        COLLECTION_APPROACH.add(APPROACH_DIRECT);
        COLLECTION_APPROACH.add(APPROACH_SCENARIO);
    }
    
    public final static String DIRECT_ALGORITHM_DIRECT = "direct";
    public final static String DIRECT_ALGORITHM_DIRECT_L = "direct-l";
    public final static String DIRECT_ALGORITHM_DIRECT_L_RAND = "direct-l-rand";
    public final static String DIRECT_ALGORITHM_DIRECT_NOSCAL = "direct-noscal";
    public final static String DIRECT_ALGORITHM_DIRECT_L_NOSCAL = "direct-l-noscal";
    public final static String DIRECT_ALGORITHM_DIRECT_L_RAND_NOSCAL = "direct-l-rand-noscal";
    public final static String DIRECT_ALGORITHM_DIRECT_ORIG = "direct-orig";
    public final static String DIRECT_ALGORITHM_DIRECT_L_ORIG = "direct-l-orig";
    public final static String DEFAULT_DIRECT_ALGORITHM = DIRECT_ALGORITHM_DIRECT;
    private final static Collection<String> COLLECTION_DIRECT_ALGORITHM = new HashSet<>();
    static {
        COLLECTION_DIRECT_ALGORITHM.add(DIRECT_ALGORITHM_DIRECT_L);
        COLLECTION_DIRECT_ALGORITHM.add(DIRECT_ALGORITHM_DIRECT_L_RAND);
        COLLECTION_DIRECT_ALGORITHM.add(DIRECT_ALGORITHM_DIRECT_NOSCAL);
        COLLECTION_DIRECT_ALGORITHM.add(DIRECT_ALGORITHM_DIRECT_L_NOSCAL);
        COLLECTION_DIRECT_ALGORITHM.add(DIRECT_ALGORITHM_DIRECT_L_RAND_NOSCAL);
        COLLECTION_DIRECT_ALGORITHM.add(DIRECT_ALGORITHM_DIRECT_ORIG);
        COLLECTION_DIRECT_ALGORITHM.add(DIRECT_ALGORITHM_DIRECT_L_ORIG);
    }
   
    private final static String DEFAULT_DIRECT_STOPPING_VALUE_ABSOLUTE = "1e-8";
    
    private final static String DIRECT_OPTIMIZATION_MIN = "min";
    private final static String DIRECT_OPTIMIZATION_MAX = "max";
    private final static Collection<String> COLLECTION_DIRECT_OPTIMIZATION = new HashSet<>();
    static {
        COLLECTION_DIRECT_OPTIMIZATION.add(DIRECT_OPTIMIZATION_MAX);
        COLLECTION_DIRECT_OPTIMIZATION.add(DIRECT_OPTIMIZATION_MIN);
    }
    private final static String DEFAULT_DIRECT_OPTIMIZATION_DIRECTION = DIRECT_OPTIMIZATION_MIN;

    public final static String LPSOLVER_LPSOLVE = "lpsolve";
    public final static String LPSOLVER_MATLAB = "matlab";
    public final static String LPSOLVER_MATLAB_FILE = "matlab-file";
    public final static String LPSOLVER_OCTAVE = "octave";
    public final static String LPSOLVER_OCTAVE_FILE = "octave-file";
    private final static String DEFAULT_LPSOLVER = LPSOLVER_OCTAVE;
    private final static Collection<String> COLLECTION_LPSOLVER = new HashSet<>();
    static {
        COLLECTION_LPSOLVER.add(LPSOLVER_LPSOLVE);
        COLLECTION_LPSOLVER.add(LPSOLVER_MATLAB_FILE);
        COLLECTION_LPSOLVER.add(LPSOLVER_MATLAB);
        COLLECTION_LPSOLVER.add(LPSOLVER_OCTAVE_FILE);
        COLLECTION_LPSOLVER.add(LPSOLVER_OCTAVE);
    }
    
    public final static String LOGENGINE_INMEMORY = "inmemory";
    public final static String LOGENGINE_ONFILE = "onfile";
    private final static String DEFAULT_LOGENGINE = LOGENGINE_ONFILE;
    private final static Collection<String> COLLECTION_LOGENGINE = new HashSet<>();
    static {
        COLLECTION_LOGENGINE.add(LOGENGINE_INMEMORY);
        COLLECTION_LOGENGINE.add(LOGENGINE_ONFILE);
    }
    
    public final static String MODELCHECKER_PRISMSMC = "prismsmc";
    public final static String MODELCHECKER_STORM = "storm";
    public final static String MODELCHECKER_STORMC = "stormc";
    public final static String MODELCHECKER_STORMPY = "stormpy";
    public final static String MODELCHECKER_STORMSMCC = "stormsmcc";
    public final static String MODELCHECKER_SYNTHETIC_OCTAVE = "synthetic-octave";
    private final static String DEFAULT_MODELCHECKER = MODELCHECKER_STORMC;
    private final static Collection<String> COLLECTION_MODELCHECKER = new HashSet<>();
    static {
        COLLECTION_MODELCHECKER.add(MODELCHECKER_PRISMSMC);
        COLLECTION_MODELCHECKER.add(MODELCHECKER_STORM);
        COLLECTION_MODELCHECKER.add(MODELCHECKER_STORMC);
        COLLECTION_MODELCHECKER.add(MODELCHECKER_STORMPY);
        COLLECTION_MODELCHECKER.add(MODELCHECKER_STORMSMCC);
        COLLECTION_MODELCHECKER.add(MODELCHECKER_SYNTHETIC_OCTAVE);
    }
    
    public final static String DEFAULT_MODELCHECKER_THREADS = "1";

    public final static String FORMAT_LATEX = "latex"; 
    public final static String FORMAT_MATH = "math"; 
    public final static String FORMAT_MATLAB = "matlab"; 
    private final static String DEFAULT_FORMAT = FORMAT_MATH;
    private final static Collection<String> COLLECTION_FORMAT = new HashSet<>();
    static {
        COLLECTION_FORMAT.add(FORMAT_LATEX);
        COLLECTION_FORMAT.add(FORMAT_MATH);
        COLLECTION_FORMAT.add(FORMAT_MATLAB);
    }
    
    public final static String PRISMSMC_METHOD_ACI = "aci";
    public final static String PRISMSMC_METHOD_APMC = "apmc";
    public final static String PRISMSMC_METHOD_CI = "ci";
    public final static String PRISMSMC_METHOD_SPRT = "sprt";
    private final static String DEFAULT_PRISMSMC_METHOD = PRISMSMC_METHOD_APMC;
    private final static Collection<String> COLLECTION_PRISMSMC_METHOD = new HashSet<>();
    static {
        COLLECTION_PRISMSMC_METHOD.add(PRISMSMC_METHOD_ACI);
        COLLECTION_PRISMSMC_METHOD.add(PRISMSMC_METHOD_APMC);
        COLLECTION_PRISMSMC_METHOD.add(PRISMSMC_METHOD_CI);
        COLLECTION_PRISMSMC_METHOD.add(PRISMSMC_METHOD_SPRT);
    }
    
    private final static String DEFAULT_EXPRESSION_PRECISION = "10";
    
    private final static String LAMBDA_INFINITE = "Infinity";

    public final static String DEFAULT_LPSOLVER_PRECISION = "10e-10";
    
    public final static String DEFAULT_LPSOLVER_SCALING_FACTOR = "0";
    
    private final static String DEFAULT_DEGREE = "2";
    
    private final static String DEFAULT_LAMBDA = LAMBDA_INFINITE;
    private final static String DEFAULT_EPSILON = "0.05";
    private final static String DEFAULT_ETA = "0.05";
    
    private final static String DEFAULT_BOUNDARY_POINTS = "0";
    
    private final static String DEFAULT_LOGLEVEL = "0";
    private final static String DEFAULT_LOGFILE = "pacpma.log";
    
    private final static String DEFAULT_SHOW_RANGE = "false";

    private final static Options options = new Options();

    private final static Option option_help = 
            Option.builder("h")
                .longOpt("help")
                .desc("print this help and exit")
                .build();

    private final static Option option_logEngine = 
            Option.builder()
                .longOpt("log-engine")
                .hasArg()
                .argName(getAlternatives(COLLECTION_LOGENGINE))
                .desc("log engine; default: " + DEFAULT_LOGENGINE)
                .build();
    
    private final static Option option_logLevel = 
            Option.builder()
            .longOpt("log-level")
            .hasArg()
            .argName("level")
            .desc("the log level to use, with " + LogEngine.LEVEL_NONE + " (no log) ≤ level ≤ " + LogEngine.LEVEL_ALL + " (full log); default: " + DEFAULT_LOGLEVEL)
            .build();
            
    private final static Option option_logFile = 
            Option.builder()
                .longOpt("logfile")
                .hasArg()
                .argName("filepath")
                .desc("file where to store the generated logging information; default: " + DEFAULT_LOGFILE)
                .build();

    private final static Option option_statistics = 
            Option.builder()
                .longOpt("statistics")
                .desc("print statistics about the model")
                .build();
    
    private final static Option option_show_range = 
            Option.builder()
                .longOpt("showrange")
                .desc("whether to show the range of the values returned by the model checker; default: " + DEFAULT_SHOW_RANGE)
                .build();

    private final static Option option_samples = 
            Option.builder("S")
                .longOpt("samples")
                .argName("long")
                .hasArg()
                .desc("number of samples to take; default: computed from the other options")
                .build();
    
    private final static Option option_vertices = 
            Option.builder("V")
                .longOpt("vertices")
                .hasArg()
                .argName(getAlternatives(COLLECTION_VERTICES))
                .optionalArg(true)
                .desc("use all 2^#parameters vertices of the parameters' hypercube as samples; add one of <" + getAlternatives(COLLECTION_VERTICES) + "> to use them in addition to ordinary samples")
                .build();
    
    private final static Option option_boundary_points = 
            Option.builder("b")
                .longOpt("boundary-points")
                .argName("int")
                .hasArg()
                .desc("number of points to take uniformly between parameter bounds, with  b ≥ 0, used only in if vertices are included;default: " + DEFAULT_BOUNDARY_POINTS)
                .build();
    
    private final static Option option_seed = 
            Option.builder("s")
                .longOpt("seed")
                .argName("long")
                .hasArg()
                .desc("seed for the random number generator")
                .build();
    
    private final static Option option_degree = 
            Option.builder("d")
                .longOpt("degree")
                .argName("integer")
                .hasArg()
                .desc("degree of the polynomial, with degree ≥ 0; default: " + DEFAULT_DEGREE + ". Ignored when a template function is provided")
                .build();
    
    private final static Option option_template = 
            Option.builder("tf")
                .longOpt("template-function")
                .argName("terms")
                .hasArg()
                .desc("template function, given as the list of its terms")
                .build();
    
    private final static Option option_lambda = 
            Option.builder("λ")
                .longOpt("lambda")
                .argName("real|" + LAMBDA_INFINITE)
                .hasArg()
                .desc("maximum value of the margin λ, with λ ≥ 0; default: " + DEFAULT_LAMBDA)
                .build();
    
    private final static Option option_epsilon = 
            Option.builder("ε")
                .longOpt("epsilon")
                .argName("real")
                .hasArg()
                .desc("value of the error rate ε, with 0 < ε < 1; default: " + DEFAULT_EPSILON)
                .build();
    
    private final static Option option_eta = 
            Option.builder("η")
                .longOpt("eta")
                .argName("real")
                .hasArg()
                .desc("value of the significance level η, with 0 < η < 1; default: " + DEFAULT_ETA)
                .build();

    private final static Option option_modelFile = 
            Option.builder("f")
                .longOpt("model-file")
                .argName("filepath")
                .hasArg()
                .desc("input file with the model to analyze")
                .build();

    private final static Option option_approach = 
            Option.builder("a")
                .longOpt("approach")
                .argName(getAlternatives(COLLECTION_APPROACH))
                .hasArg()
                .desc("analysis approach; default: " + DEFAULT_APPROACH)
                .build();
    
    private final static Option option_modelType = 
            Option.builder("t")
                .longOpt("model-type")
                .argName(getAlternatives(COLLECTION_MODELTYPE))
                .hasArg()
                .desc("type of the model to analyze; default: " + DEFAULT_MODELTYPE)
                .build();
    
    private final static Option option_property = 
            Option.builder("p")
                .longOpt("prop")
                .argName("string")
                .hasArg()
                .desc("property formula to analyze")
                .build();

    private final static Option option_consts = 
            Option.builder("C")
                .longOpt("constants")
                .argName("name=value(,name=value)*")
                .hasArg()
                .desc("values for the constants")
                .build();
    
    private final static Option option_params = 
            Option.builder("P")
                .longOpt("parameters")
                .argName("name=l:u(,name=l:u)*")
                .hasArg()
                .desc("intervals for the parameters, with l < u being decimal numbers")
                .build();
    
    private final static Option option_direct_algorithm = 
            Option.builder()
                .longOpt("direct-algorithm")
                .argName(getAlternatives(COLLECTION_DIRECT_ALGORITHM))
                .hasArg()
                .desc("underlying DIRECT algorithm; default: " + DEFAULT_DIRECT_ALGORITHM + ". For details, see https://https://nlopt.readthedocs.io/en/latest/NLopt_Algorithms/#direct-and-direct-l")
                .build();
    
    private final static Option option_direct_optimization_direction = 
            Option.builder()
                .longOpt("direct-optimization-direction")
                .argName(getAlternatives(COLLECTION_DIRECT_OPTIMIZATION))
                .hasArg()
                .desc("DIRECT optimization direction; default: " + DEFAULT_DIRECT_OPTIMIZATION_DIRECTION)
                .build();
    
    private final static Option option_direct_stopping_value_absolute = 
            Option.builder()
                .longOpt("direct-stopping-value-absolute")
                .argName("real")
                .hasArg()
                .desc("DIRECT stopping threshold based on the absolute variation of the computed value between two successive optimization steps; if no other stopping criterion is used, a default of " + DEFAULT_DIRECT_STOPPING_VALUE_ABSOLUTE + " is used")
                .build();
    
    private final static Option option_direct_stopping_value_relative = 
            Option.builder()
                .longOpt("direct-stopping-value-relative")
                .argName("real")
                .hasArg()
                .desc("DIRECT stopping threshold based on the relative variation of the computed value between two successive optimization steps")
                .build();
    
    private final static Option option_direct_stopping_parameters_absolute = 
            Option.builder()
                .longOpt("direct-stopping-parameters-absolute")
                .argName("real")
                .hasArg()
                .desc("DIRECT stopping threshold based on the absolute variation of the L2 norm of the parameters between two successive optimization steps")
                .build();
    
    private final static Option option_direct_stopping_parameters_relative = 
            Option.builder()
                .longOpt("direct-stopping-parameters-relative")
                .argName("real")
                .hasArg()
                .desc("DIRECT stopping threshold based on the relative variation of the L2 norm of the parameters between two successive optimization steps")
                .build();
    
    private final static Option option_expression_precision = 
            Option.builder()
                .longOpt("expression-precision")
                .argName("int")
                .hasArg()
                .desc("bit-size precision ≥ 0 when evaluating expressions possibly requiring infinitely many bits; default: " + DEFAULT_EXPRESSION_PRECISION)
                .build();
    
    private final static Option option_lpsolver = 
            Option.builder("l")
                .longOpt("lpsolver")
                .argName(getAlternatives(COLLECTION_LPSOLVER))
                .hasArg()
                .desc("LP problem solver; default: " + DEFAULT_LPSOLVER)
                .build();
    
    private final static Option option_lpsolver_precision = 
            Option.builder()
                .longOpt("lpsolver-precision")
                .argName("real")
                .hasArg()
                .desc("precision of the LP solver: all numbers whose absolute value is at most the given precision ≥ 0 are replaced by 0; default: " + DEFAULT_LPSOLVER_PRECISION)
                .build();
    
    private final static Option option_lpsolver_scaling_factor = 
            Option.builder()
                .longOpt("lpsolver-scalingfactor")
                .argName("int")
                .hasArg()
                .desc("scaling factor ≥ 0 for the LP solver: all coefficients are multiplied by 10^factor and the result divided by 10^factor; default: " + DEFAULT_LPSOLVER_SCALING_FACTOR)
                .build();
    
    private final static Option option_modelchecker = 
            Option.builder("mc")
                .longOpt("model-checker")
                .argName(getAlternatives(COLLECTION_MODELCHECKER))
                .hasArg()
                .desc("model checker; default: " + DEFAULT_MODELCHECKER)
                .build();

    private final static Option option_modelcheckerThreads = 
            Option.builder()
                .longOpt("model-checker-threads")
                .argName("integer")
                .hasArg()
                .desc("number of model checker threads ≥ 1 to run in parallel; default: " + DEFAULT_MODELCHECKER_THREADS)
                .build();

    private final static Option option_modelcheckerPath = 
            Option.builder()
                .longOpt("model-checker-path")
                .argName("filepath")
                .hasArg()
                .desc("path to the model checker executable; default: search into $PATH")
                .build();

    private final static Option option_modelcheckerOptions = 
            Option.builder()
                .longOpt("model-checker-options")
                .argName("options")
                .hasArg()
                .desc("single string representing a space-separated list of options to be passed to the model checker")
                .build();
    
    private final static Option option_prismsmcMethod = 
            Option.builder()
                .longOpt("prismsmc-method")
                .argName(getAlternatives(COLLECTION_PRISMSMC_METHOD))
                .hasArg()
                .desc("smc engine to use; default: " + DEFAULT_PRISMSMC_METHOD)
                .build();

    private final static Option option_prismsmcApprox = 
            Option.builder()
                .longOpt("prismsmc-approx")
                .argName("real")
                .hasArg()
                .desc("value of the \"-simapprox\" parameter of Prism, strictly between 0 and 1")
                .build();

    private final static Option option_prismsmcConf = 
            Option.builder()
                .longOpt("prismsmc-conf")
                .argName("real")
                .hasArg()
                .desc("value of the \"-simconf\" parameter of Prism, strictly between 0 and 1")
                .build();
    
    private final static Option option_prismsmcPathlen = 
            Option.builder()
                .longOpt("prismsmc-pathlen")
                .argName("int")
                .hasArg()
                .desc("value of the \"-simpathlen\" parameter of Prism, at least 1")
                .build();
    
    private final static Option option_prismsmcSamples = 
            Option.builder()
                .longOpt("prismsmc-samples")
                .argName("int")
                .hasArg()
                .desc("value of the \"-simsamples\" parameter of Prism, at least 1")
                .build();
    
    private final static Option option_format = 
            Option.builder()
                .longOpt("format")
                .argName(getAlternatives(COLLECTION_FORMAT))
                .hasArg()
                .desc("print format for the generated approximation function; default: " + DEFAULT_FORMAT)
                .build();
    
    static {
        options.addOption(option_help);
        
        options.addOption(option_logEngine);
        options.addOption(option_logLevel);
        options.addOption(option_logFile);
        
        options.addOption(option_statistics);
        
        options.addOption(option_show_range);
        
        options.addOption(option_samples);
        options.addOption(option_vertices);
        options.addOption(option_boundary_points);
        options.addOption(option_seed);
        options.addOption(option_degree);
        options.addOption(option_template);
        options.addOption(option_lambda);
        options.addOption(option_epsilon);
        options.addOption(option_eta);
        options.addOption(option_approach);
        options.addOption(option_modelFile);
        options.addOption(option_modelType);
        options.addOption(option_property);
        options.addOption(option_consts);
        options.addOption(option_params);
        options.addOption(option_direct_algorithm);
        options.addOption(option_direct_optimization_direction);
        options.addOption(option_direct_stopping_value_absolute);
        options.addOption(option_direct_stopping_value_relative);
        options.addOption(option_direct_stopping_parameters_absolute);
        options.addOption(option_direct_stopping_parameters_relative);
        options.addOption(option_lpsolver);
        options.addOption(option_lpsolver_precision);
        options.addOption(option_lpsolver_scaling_factor);
        options.addOption(option_expression_precision);
        options.addOption(option_modelchecker);
        options.addOption(option_modelcheckerPath);
//        options.addOption(option_modelcheckerOptions);
        options.addOption(option_modelcheckerThreads);
        options.addOption(option_prismsmcMethod);
        options.addOption(option_prismsmcApprox);
        options.addOption(option_prismsmcConf);
        options.addOption(option_prismsmcPathlen);
        options.addOption(option_prismsmcSamples);
        options.addOption(option_format);
    }

    private static boolean printStatistics;
    
    private static boolean showRange;
    
    private static String logEngine;
    private static int logLevel;
    private static String logFile;
    private static boolean useLog;
    
    private static final List<String> parsingErrors = new ArrayList<>(10);
    private static int samples;
    private static boolean vertices;
    private static boolean verticesAddition;
    private static int boundaryPoints;
    private static long seed;
    private static int degree;
    private static String templateFunctionString;
    private static TemplateFunction templateFunction = null;
    private static BigDecimal epsilon;
    private static BigDecimal eta;
    private static BigDecimal lambda;
    private static boolean lambdaInfinite;
    private static String approach;
    private static String modelFile;
    private static String modelType;
    private static String property;
    private static String lpsolver;
    private static BigDecimal lpsolverPrecision;
    private static BigDecimal lpsolverFactor;
    private static int expressionPrecision;
    private static boolean directOptimizationDirectionMin;
    private static String directAlgorithm = null;
    private static Double directStoppingValueAbsolute = null;
    private static Double directStoppingValueRelative = null;
    private static Double directStoppingParametersAbsolute = null;
    private static Double directStoppingParametersRelative = null;
    private static String modelchecker;
    private static String modelcheckerPath;
    private static List<String> modelcheckerOptions;
    private static int modelcheckerThreads;
    private static String prismsmc_method;
    private static String prismsmc_approx = null;
    private static String prismsmc_conf = null;
    private static String prismsmc_pathlen = null;
    private static String prismsmc_samples = null;
    private static String format;
    private static List<Constant> constants;
    private static List<Parameter> parameters;

    private static LogEngine logEngineInstance = null; 
    
    /**
     * Parse and check the command line arguments to extract the options for the PAC
     * Model checker.
     * 
     * @param commandLineArguments
     *            the command line arguments to parse
     * @return whether the options have been parsed correctly
     */
    public static boolean parseOptions(String[] commandLineArguments) {
        CommandLineParser clparser = new DefaultParser();
        CommandLine commandline = null;

        try {
            commandline = clparser.parse(options, commandLineArguments);

            if (commandline.hasOption(option_help)) {
                HelpFormatter help = new HelpFormatter();
                help.printHelp("PAC Model", options);
                return false;
            } else {
                int tmpInt = 0;
                BigDecimal tmpBD = null;
                long tmpLong = 0;

                printStatistics = commandline.hasOption(option_statistics);
                
                showRange = commandline.hasOption(option_show_range);
                
                logEngine = commandline.getOptionValue(option_logEngine, DEFAULT_LOGENGINE);
                if (!COLLECTION_LOGENGINE.contains(logEngine)) {
                    parsingErrors.add(getInvalidMessage(commandline, option_logEngine));
                }

                approach = commandline.getOptionValue(option_approach, DEFAULT_APPROACH);
                if (!COLLECTION_APPROACH.contains(approach)) {
                    parsingErrors.add(getInvalidMessage(commandline, option_approach));
                }
                
                try {
                    tmpInt = Integer.valueOf(commandline.getOptionValue(option_logLevel, DEFAULT_LOGLEVEL));
                    if (tmpInt < 0) {
                        parsingErrors.add("The option " + option_logLevel.getLongOpt() + " must be between " + LogEngine.LEVEL_NONE + " and " + LogEngine.LEVEL_ALL);
                    }
                } catch (NumberFormatException nfe) {
                    parsingErrors.add(getInvalidMessage(commandline, option_logLevel));
                }
                logLevel = tmpInt;
                
                useLog = logLevel > LogEngine.LEVEL_NONE;

                logFile = commandline.getOptionValue(option_logFile, DEFAULT_LOGFILE) ;
                
                tmpLong = new Random().nextLong();
                if (commandline.hasOption(option_seed)) {
                    try {
                        tmpLong = Long.valueOf(commandline.getOptionValue(option_seed));
                    } catch (NumberFormatException nfe) {
                        parsingErrors.add(getInvalidMessage(commandline, option_seed));
                    }
                }
                seed = tmpLong;

                try {
                    tmpInt = Integer.valueOf(commandline.getOptionValue(option_degree, DEFAULT_DEGREE));
                    if (tmpInt < 0) {
                        parsingErrors.add("The option " + option_degree.getLongOpt() + " must be at least 0");
                    }
                } catch (NumberFormatException nfe) {
                    parsingErrors.add(getInvalidMessage(commandline, option_degree));
                }
                degree = tmpInt;

                try {
                    tmpBD = new BigDecimal(commandline.getOptionValue(option_epsilon, DEFAULT_EPSILON));
                    if (tmpBD.compareTo(BigDecimal.ZERO) <= 0 || tmpBD.compareTo(BigDecimal.ONE) >= 0) {
                        parsingErrors.add("The option " + option_epsilon.getLongOpt() + " must be strictly between 0 and 1");
                    }
                } catch (NumberFormatException nfe) {
                    parsingErrors.add(getInvalidMessage(commandline, option_epsilon));
                }
                epsilon = tmpBD;

                try {
                    tmpBD = new BigDecimal(commandline.getOptionValue(option_eta, DEFAULT_ETA));
                    if (tmpBD.compareTo(BigDecimal.ZERO) <= 0 || tmpBD.compareTo(BigDecimal.ONE) >= 0) {
                        parsingErrors.add(option_eta.getLongOpt() + " must be scrictly between 0 and 1");
                    }
                } catch (NumberFormatException nfe) {
                    parsingErrors.add(getInvalidMessage(commandline, option_eta));
                }
                eta = tmpBD;

                String tmpLambda = commandline.getOptionValue(option_lambda, LAMBDA_INFINITE);
                lambdaInfinite = tmpLambda.equals(LAMBDA_INFINITE);
                if (lambdaInfinite) {
                    lambda = null;
                } else {
                    try {
                        lambda = new BigDecimal(tmpLambda);
                        if (tmpBD.compareTo(BigDecimal.ZERO) < 0) {
                            parsingErrors.add(option_lambda.getLongOpt() + " must be at least 0");
                        }
                    } catch (NumberFormatException nfe) {
                        parsingErrors.add(getInvalidMessage(commandline, option_lambda));
                    }
                }
                
                directAlgorithm = commandline.getOptionValue(option_direct_algorithm, DEFAULT_DIRECT_ALGORITHM);
                if (!COLLECTION_DIRECT_ALGORITHM.contains(directAlgorithm)) {
                    parsingErrors.add(getInvalidMessage(commandline, option_direct_algorithm));
                }
                
                switch (commandline.getOptionValue(option_direct_optimization_direction, DEFAULT_DIRECT_OPTIMIZATION_DIRECTION)) {
                case DIRECT_OPTIMIZATION_MIN:
                    directOptimizationDirectionMin = true;
                    break;
                case DIRECT_OPTIMIZATION_MAX:
                    directOptimizationDirectionMin = false;
                    break;
                default:
                    parsingErrors.add(getInvalidMessage(commandline, option_direct_optimization_direction)); 
                }
                
                boolean has_stopping_criterion = false;

                if (commandline.hasOption(option_direct_stopping_value_relative)) {
                    try {
                        directStoppingValueRelative = Double.valueOf(commandline.getOptionValue(option_direct_stopping_value_relative));
                        has_stopping_criterion = true;
                    } catch (NumberFormatException nfe) {
                        parsingErrors.add(getInvalidMessage(commandline, option_direct_stopping_value_relative));
                    }
                }

                if (commandline.hasOption(option_direct_stopping_parameters_absolute)) {
                    try {
                        directStoppingParametersAbsolute = Double.valueOf(commandline.getOptionValue(option_direct_stopping_parameters_absolute));
                        has_stopping_criterion = true;
                    } catch (NumberFormatException nfe) {
                        parsingErrors.add(getInvalidMessage(commandline, option_direct_stopping_parameters_absolute));
                    }
                }

                if (commandline.hasOption(option_direct_stopping_parameters_relative)) {
                    try {
                        directStoppingParametersRelative = Double.valueOf(commandline.getOptionValue(option_direct_stopping_parameters_relative));
                        has_stopping_criterion = true;
                    } catch (NumberFormatException nfe) {
                        parsingErrors.add(getInvalidMessage(commandline, option_direct_stopping_parameters_relative));
                    }
                }

                try {
                    if (has_stopping_criterion) {
                        if (commandline.hasOption(option_direct_stopping_value_absolute)) {
                            directStoppingValueAbsolute = Double.valueOf(commandline.getOptionValue(option_direct_stopping_value_absolute));
                        } 
                    } else {
                        directStoppingValueAbsolute = Double.valueOf(commandline.getOptionValue(option_direct_stopping_value_absolute, DEFAULT_DIRECT_STOPPING_VALUE_ABSOLUTE));
                    }
                } catch (NumberFormatException nfe) {
                    parsingErrors.add(getInvalidMessage(commandline, option_direct_stopping_value_absolute));
                }

                if (commandline.hasOption(option_modelFile)) {
                    modelFile = commandline.getOptionValue(option_modelFile);
                } else {
                    parsingErrors.add(getMissingMandatoryOptionMessage(option_modelFile));
                }

                modelType = commandline.getOptionValue(option_modelType, DEFAULT_MODELTYPE);
                if (!COLLECTION_MODELTYPE.contains(modelType)) {
                    parsingErrors.add(getInvalidMessage(commandline, option_modelType));
                }

                if (commandline.hasOption(option_property)) {
                    property = commandline.getOptionValue(option_property);
                } else {
                    parsingErrors.add(getMissingMandatoryOptionMessage(option_property));
                }

                constants = parseConstants(commandline);

                if (commandline.hasOption(option_params)) {
                    parameters = parseParameters(commandline);
                } else {
                    parsingErrors.add(getMissingMandatoryOptionMessage(option_params));
                }

                if (parsingErrors.isEmpty()) {
                    if (commandline.hasOption(option_samples)) {
                        try {
                            tmpInt = Integer.valueOf(commandline.getOptionValue(option_samples));
                            if (tmpInt <= 0) {
                                parsingErrors.add(getInvalidMessage(commandline, option_samples));
                            }
                        } catch (NumberFormatException nfe) {
                            parsingErrors.add(getInvalidMessage(commandline, option_samples));
                        }
                    } else {
                        tmpInt = Util.minimumNumberSamples(epsilon.doubleValue(), eta.doubleValue(), 1 + Util.numberCoefficients(parameters.size(), degree));
                        if (tmpInt <= 0) {
                            parsingErrors.add("The computed value '" + tmpInt
                                    + "' for the option" + option_samples.getLongOpt() + " is not valid");
                        }
                    }
                }
                samples = tmpInt;

                vertices = commandline.hasOption(option_vertices);
                verticesAddition = vertices && COLLECTION_VERTICES.contains(commandline.getOptionValue(option_vertices, EMPTY));

                boundaryPoints = Integer.valueOf(DEFAULT_BOUNDARY_POINTS);
                if (vertices) {
                    try {
                        tmpInt = Integer.valueOf(commandline.getOptionValue(option_boundary_points, DEFAULT_BOUNDARY_POINTS));
                        if (tmpInt < 0) {
                            parsingErrors.add(option_boundary_points.getLongOpt() + " must be at least 0");
                        }
                    } catch (NumberFormatException nfe) {
                        parsingErrors.add(getInvalidMessage(commandline, option_boundary_points));
                    }
                    boundaryPoints = tmpInt;
                }

                lpsolver = commandline.getOptionValue(option_lpsolver, DEFAULT_LPSOLVER);
                if (!COLLECTION_LPSOLVER.contains(lpsolver)) {
                    parsingErrors.add(getInvalidMessage(commandline, option_lpsolver));
                }

                try {
                    tmpBD = new BigDecimal(commandline.getOptionValue(option_lpsolver_precision, DEFAULT_LPSOLVER_PRECISION));
                    if (tmpBD.compareTo(BigDecimal.ZERO) < 0) {
                        parsingErrors.add("The option " + option_lpsolver_precision.getLongOpt() + " must be at least 0");
                    }
                } catch (NumberFormatException nfe) {
                    parsingErrors.add(getInvalidMessage(commandline, option_lpsolver_precision));
                }
                lpsolverPrecision = tmpBD;

                try {
                    tmpInt = Integer.valueOf(commandline.getOptionValue(option_lpsolver_scaling_factor, DEFAULT_LPSOLVER_SCALING_FACTOR));
                    if (tmpInt < 0) {
                        parsingErrors.add("The option " + option_lpsolver_scaling_factor.getLongOpt() + " must be at least 0");
                    }
                } catch (NumberFormatException nfe) {
                    parsingErrors.add(getInvalidMessage(commandline, option_lpsolver_scaling_factor));
                }
                lpsolverFactor = BigDecimal.TEN.pow(tmpInt);

                try {
                    tmpInt = Integer.valueOf(commandline.getOptionValue(option_expression_precision, DEFAULT_EXPRESSION_PRECISION));
                    if (tmpInt < 0) {
                        parsingErrors.add("The option " + option_expression_precision.getLongOpt() + " must be at least 0");
                    }
                } catch (NumberFormatException nfe) {
                    parsingErrors.add(getInvalidMessage(commandline, option_expression_precision));
                }
                expressionPrecision = tmpInt;

                modelchecker = commandline.getOptionValue(option_modelchecker, DEFAULT_MODELCHECKER);
                if (!COLLECTION_MODELCHECKER.contains(modelchecker)) {
                    parsingErrors.add(getInvalidMessage(commandline, option_modelchecker));
                }

                if (commandline.hasOption(option_modelcheckerPath)) {
                    modelcheckerPath = commandline.getOptionValue(option_modelcheckerPath);
                } else {
                    modelcheckerPath = null;
                }
                
                if (commandline.hasOption(option_modelcheckerOptions)) {
                    String[] mcOptions = commandline.getOptionValue(option_modelcheckerOptions).split(SPACE);
                    modelcheckerOptions = Arrays.asList(mcOptions);
                } else {
                    modelcheckerOptions = new ArrayList<>(0);
                }

                prismsmc_method = commandline.getOptionValue(option_prismsmcMethod, DEFAULT_PRISMSMC_METHOD);
                if (!COLLECTION_PRISMSMC_METHOD.contains(prismsmc_method)) {
                    parsingErrors.add(getInvalidMessage(commandline, option_prismsmcMethod));
                }

                if (commandline.hasOption(option_prismsmcApprox)) {
                    try {
                        prismsmc_approx = commandline.getOptionValue(option_prismsmcApprox);
                        Double val = Double.valueOf(prismsmc_approx);
                        if (val <= 0 || val >= 1) {
                            parsingErrors.add("The option " + option_prismsmcApprox.getLongOpt() + " must be strictly between 0 and 1");
                        }
                    } catch (NumberFormatException nfe) {
                        parsingErrors.add(getInvalidMessage(commandline, option_prismsmcApprox));
                    }
                }

                if (commandline.hasOption(option_prismsmcConf)) {
                    try {
                        prismsmc_conf = commandline.getOptionValue(option_prismsmcConf);
                        Double val = Double.valueOf(prismsmc_conf);
                        if (val <= 0 || val >= 1) {
                            parsingErrors.add("The option " + option_prismsmcConf.getLongOpt() + " must be strictly between 0 and 1");
                        }
                    } catch (NumberFormatException nfe) {
                        parsingErrors.add(getInvalidMessage(commandline, option_prismsmcConf));
                    }
                }

                if (commandline.hasOption(option_prismsmcPathlen)) {
                    try {
                        prismsmc_pathlen = commandline.getOptionValue(option_prismsmcPathlen);
                        Integer val = Integer.valueOf(prismsmc_pathlen);
                        if (val <= 0) {
                            parsingErrors.add("The option " + option_prismsmcPathlen.getLongOpt() + " must be at least 1");
                        }
                    } catch (NumberFormatException nfe) {
                        parsingErrors.add(getInvalidMessage(commandline, option_prismsmcPathlen));
                    }
                }

                if (commandline.hasOption(option_prismsmcSamples)) {
                    try {
                        prismsmc_samples = commandline.getOptionValue(option_prismsmcSamples);
                        Integer val = Integer.valueOf(prismsmc_samples);
                        if (val <= 0) {
                            parsingErrors.add("The option " + option_prismsmcSamples.getLongOpt() + " must be at least 1");
                        }
                    } catch (NumberFormatException nfe) {
                        parsingErrors.add(getInvalidMessage(commandline, option_prismsmcSamples));
                    }
                }
                
                if (prismsmc_approx != null && prismsmc_conf != null && prismsmc_samples != null) {
                    parsingErrors.add("Cannot use all three options " + option_prismsmcApprox.getLongOpt() + ", " + option_prismsmcConf.getLongOpt() + ", " + option_prismsmcSamples.getLongOpt() + " at the same time");
                }

                try {
                    tmpInt = Integer.valueOf(commandline.getOptionValue(option_modelcheckerThreads, DEFAULT_MODELCHECKER_THREADS));
                    if (tmpInt < 1) {
                        parsingErrors.add("The option " + option_modelcheckerThreads.getLongOpt() + " must be at least 1");
                    }
                } catch (NumberFormatException nfe) {
                    parsingErrors.add(getInvalidMessage(commandline, option_modelcheckerThreads));
                }
                modelcheckerThreads = tmpInt;
                
                format = commandline.getOptionValue(option_format, DEFAULT_FORMAT);
                if (!COLLECTION_FORMAT.contains(format)) {
                    parsingErrors.add(getInvalidMessage(commandline, option_format));
                }
                
                templateFunctionString = commandline.getOptionValue(option_template);
            }
        } catch (ParseException pe) {
            parsingErrors.add(pe.getMessage());
        }
        parsingErrors.forEach((s) -> System.out.println(s));
        return parsingErrors.isEmpty();
    }

    /**
     * @return whether to print statistics
     */
    public static boolean printStatistics() {
        return printStatistics;
    }

    /**
     * @return whether to show the range
     */
    public static boolean showRange() {
        return showRange;
    }

    /**
     * @return whether to generate debug information
     */
    public static boolean generateDebugInformation() {
        return logFile != null;
    }

    /**
     * @return whether to use logging 
     */
    public static boolean useLogging() {
        return useLog;
    }
    
    /**
     * @return the log level
     */
    public static int getLogLevel() {
        return logLevel;
    }
    
    /**
     * @return the file where to store logged information
     */
    public static String getLogFile() {
        return logFile;
    }
    
    /**
     * @return the samples
     */
    public static int getNumberSamples() {
        return samples;
    }

    /**
     * @return whether to use parameters' hypercube's vertices as samples
     */
    public static boolean useVerticesAsSamples() {
        return vertices;
    }

    /**
     * @return the number of points inside the parameters' bounds
     */
    public static int getBoundaryPoints() {
        return boundaryPoints;
    }

    /**
     * @return whether to use parameters' hypercube's vertices as samples, in addition to the ordinary ones
     */
    public static boolean useVerticesAsAdditionalSamples() {
        return verticesAddition;
    }

    /**
     * @return the seed
     */
    public static long getSeed() {
        return seed;
    }

    /**
     * @return the degree
     */
    public static int getDegree() {
        return degree;
    }

    /**
     * @return the template function
     */
    public static TemplateFunction getTemplateFunction() {
        if (templateFunction == null) {
            if (templateFunctionString == null) {
                templateFunction = new Polynomial(degree);
            } else {
                 templateFunction = new ExpressionFunction(templateFunctionString);
            }
        }
        return templateFunction;
    }
    
    /**
     * @return the epsilon
     */
    public static BigDecimal getEpsilon() {
        return epsilon;
    }

    /**
     * @return the eta
     */
    public static BigDecimal getEta() {
        return eta;
    }

    /**
     * @return the lambda
     */
    public static BigDecimal getLambda() {
        return lambda;
    }

    /**
     * @return whether lambda is unbounded
     */
    public static boolean isLambdaUnbounded() {
        return lambdaInfinite;
    }

    /**
     * @return whether the DIRECT optimization direction is minimize
     */
    public static boolean isDirectOptimizationDirectionMin() {
        return directOptimizationDirectionMin;
    }
    
    /**
     * @return the DIRECT stopping threshold based on value absolute variation
     */
    public static Double getDirectStoppingValueAbsolute() {
        return directStoppingValueAbsolute;
    }
    
    /**
     * @return the DIRECT stopping threshold based on value relative variation
     */
    public static Double getDirectStoppingValueRelative() {
        return directStoppingValueRelative;
    }
    
    /**
     * @return the DIRECT stopping threshold based on parameters absolute variation
     */
    public static Double getDirectStoppingParametersAbsolute() {
        return directStoppingParametersAbsolute;
    }
    
    /**
     * @return the DIRECT stopping threshold based on parameters relative variation
     */
    public static Double getDirectStoppingParametersRelative() {
        return directStoppingParametersRelative;
    }
    
   /**
     * @return the model file
     */
    public static String getModelFile() {
        return modelFile;
    }

    /**
     * @return the model type
     */
    public static String getModelType() {
        return modelType;
    }

    /**
     * @return the property
     */
    public static String getPropertyFormula() {
        return property;
    }

    /**
     * @return the constants
     */
    public static List<Constant> getConstants() {
        return constants;
    }

    /**
     * @return the parameters
     */
    public static List<Parameter> getParameters() {
        return parameters;
    }

    /**
     * @return the expressionPrecision
     */
    public static int getExpressionPrecision() {
        return expressionPrecision;
    }

    /**
     * @return the LP solver
     */
    public static String getLPSolver() {
        return lpsolver;
    }
    
    /**
     * Generates and returns a new approach solver specified as option at command line.
     * 
     * @param logEngineInstance the {@link LogEngine} instance to be used for logging.
     * 
     * @return an instance of the chosen LP solver
     */
    public static Approach getAppraochInstance(LogEngine logEngineInstance) {
        switch (approach) {
        case APPROACH_DIRECT:
            return new DIRECTApproach(logEngineInstance);
        case APPROACH_SCENARIO:
            return new ScenarioApproach(logEngineInstance);
        default:
            throw new UnsupportedOperationException("Unexpected approach");
        }
    }
    
    /**
     * Returns the underlying DIRECT algorithm.
     * 
     * @return the DIRECT algorithm
     */
    public static Algorithm getDirectAlgorithm() {
        switch (directAlgorithm) {
        case DIRECT_ALGORITHM_DIRECT:
            return Algorithm.GN_DIRECT;
        case DIRECT_ALGORITHM_DIRECT_L:
            return Algorithm.GN_DIRECT_L;
        case DIRECT_ALGORITHM_DIRECT_L_RAND:
            return Algorithm.GN_DIRECT_L_RAND;
        case DIRECT_ALGORITHM_DIRECT_NOSCAL:
            return Algorithm.GN_DIRECT_NOSCAL;
        case DIRECT_ALGORITHM_DIRECT_L_NOSCAL:
            return Algorithm.GN_DIRECT_L_NOSCAL;
        case DIRECT_ALGORITHM_DIRECT_L_RAND_NOSCAL:
            return Algorithm.GN_DIRECT_L_RAND_NOSCAL;
        case DIRECT_ALGORITHM_DIRECT_ORIG:
            return Algorithm.GN_ORIG_DIRECT;
        case DIRECT_ALGORITHM_DIRECT_L_ORIG:
            return Algorithm.GN_ORIG_DIRECT_L;
        default:
            throw new UnsupportedOperationException("Unexpected DIRECT algorithm " + directAlgorithm);
        }
    }
    
    /**
     * Generates and returns a new instance of the LP solver specified as option
     * at command line.
     * 
     * @return an instance of the chosen LP solver
     */
    public static LPSolver getLPSolverInstance() {
        switch (lpsolver) {
        case LPSOLVER_LPSOLVE:
            return new LPSolveLibrary();
        case LPSOLVER_MATLAB_FILE:
            return new MatlabFileTool();
        case LPSOLVER_MATLAB:
            return new MatlabTool();
        case LPSOLVER_OCTAVE_FILE:
            return new OctaveFileTool();
        case LPSOLVER_OCTAVE:
            return new OctaveTool();
        default:
            throw new UnsupportedOperationException("Unexpected LP solver");
        }
    }

    /**
     * @return the precision for the LP solver
     */
    public static BigDecimal getLPSolverPrecision() {
        return lpsolverPrecision;
    }
    
    /**
     * @return the factor for the LP solver coefficients
     */
    public static BigDecimal getLPSolverFactor() {
        return lpsolverFactor;
    }

    /**
     * Generates and returns a new instance of the log engine specified as option
     * at command line.
     * 
     * @return an instance of the chosen log engine
     */
    public static LogEngine getLogEngineInstance() {
        if (logEngineInstance == null) {
            if (useLog) {
                switch (logEngine) {
                case LOGENGINE_INMEMORY:
                    logEngineInstance = new InmemoryLogEngine();
                    break;
                case LOGENGINE_ONFILE:
                    logEngineInstance = new OnfileLogEngine();
                    break;
                default:
                    throw new UnsupportedOperationException("Unexpected logger");
                }
            } else {
                logEngineInstance = new NullLogEngine();
            }
        }
        return logEngineInstance;
    }

   /**
     * @return the model checker
     */
    public static String getModelChecker() {
        return modelchecker;
    }
    
    /**
     * Generates and returns a new instance of the model checker specified as option
     * at command line.
     * 
     * @return an instance of the chosen model checker
     */
    public static BatchModelChecker getModelCheckerInstance() {
        switch (modelchecker) {
        case MODELCHECKER_PRISMSMC:
            return new PrismSMCTool();
        case MODELCHECKER_STORM:
            return new StormTool();
        case MODELCHECKER_STORMC:
            return new StormCWrapper();
        case MODELCHECKER_STORMPY:
            return new StormPython();
        case MODELCHECKER_STORMSMCC:
            return new StormsmcCWrapper();
        case MODELCHECKER_SYNTHETIC_OCTAVE:
            return new SyntheticOctave();
        default:
            throw new UnsupportedOperationException("Unexpected model checker");
        }
    }

    /**
     * @return the model checker path, or {@code null} if not set
     */
    public static String getModelCheckerPath() {
        return modelcheckerPath;
    }

    /**
     * @return the model checker options
     */
    public static List<String> getModelCheckerOptions() {
        return modelcheckerOptions;
    }

    /**
     * @return the number of model checker threads
     */
    public static int getModelCheckerThreads() {
        return modelcheckerThreads;
    }
   
    /**
     * @return the SMC engine to use with Prism
     */
    public static String getPrismsmcMethod() {
        return prismsmc_method;
    }

    /**
     * @return the approximation value to pass to Prism
     */
    public static String getPrismsmcApprox() {
        return prismsmc_approx;
    }

    /**
     * @return the confidence value to pass to Prism
     */
    public static String getPrismsmcConf() {
        return prismsmc_conf;
    }

    /**
     * @return the path length to pass to Prism
     */
    public static String getPrismsmcPathlen() {
        return prismsmc_pathlen;
    }

    /**
     * @return the number of samples to pass to Prism
     */
    public static String getPrismsmcSamples() {
        return prismsmc_samples;
    }

    /**
     * @return how to print the approximated function
     */
    public static String getFunctionFormat() {
        return format;
    }

    private static List<Constant> parseConstants(CommandLine commandline) {
        List<Constant> constants = new ArrayList<>();
        if (commandline.hasOption(option_consts)) {
            String[] clconstants = commandline.getOptionValue(option_consts).split(COMMA);
            for (String clconstant : clconstants) {
                String[] components = clconstant.split(EQUAL);
                if (components.length == 2) {
                    constants.add(new Constant(components[0], components[1]));
                } else {
                    parsingErrors.add("Invalid constant definition: " + clconstant);
                }
            }
        }
        return constants;
    }

    private static List<Parameter> parseParameters(CommandLine commandline) {
        List<Parameter> parameters = new ArrayList<>();
        String[] clparameters = commandline.getOptionValue(option_params).split(COMMA);
        for (String clparameter : clparameters) {
            String[] components = clparameter.split(EQUAL);
            if (components.length == 2) {
                String name = components[0];
                String[] bounds = components[1].split(COLON);
                if (bounds.length == 2) {
                    BigDecimal lowerbound = null;
                    BigDecimal upperbound = null;
                    try {
                        lowerbound = new BigDecimal(bounds[0]);
                        upperbound = new BigDecimal(bounds[1]);
                    } catch (NumberFormatException nfe) {
                    }
                    if (lowerbound != null && upperbound != null && lowerbound.compareTo(upperbound) < 0) {
                        parameters.add(new Parameter(name, lowerbound, upperbound));
                    } else {
                        parsingErrors.add("Invalid parameter definition: " + clparameter);
                    }
                } else {
                    parsingErrors.add("Invalid parameter definition: " + clparameter);
                }
            } else {
                parsingErrors.add("Invalid parameter definition: " + clparameter);
            }
        }
        return parameters;
    }
    
    private static String getAlternatives(Collection<String> collection) {
        StringBuilder alternatives = new StringBuilder();
        boolean isFirst = true;
        for (String item : collection) {
            if (isFirst) {
                isFirst = false;
            } else {
                alternatives.append("|");
            }
            alternatives.append(item);
        }
        return alternatives.toString();
    }
    
    private static String getInvalidMessage(CommandLine commandline, Option option) {
        return "The provided value '" + commandline.getOptionValue(option) + "' for the option "
                + option.getLongOpt() + " is not valid";
    }
    
    private static String getMissingMandatoryOptionMessage(Option option) {
        return "Missing mandatory option " + option.getLongOpt();
    }
}
