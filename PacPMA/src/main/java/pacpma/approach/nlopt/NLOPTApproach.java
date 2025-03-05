/****************************************************************************

    PacPMA - the PAC-based Parametric Model Analyzer
    Copyright (C) 2025

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

package pacpma.approach.nlopt;

import java.util.ArrayList;
import java.util.List;

import nlopt.DoubleVector;
import nlopt.Opt;
import nlopt.Result;
import pacpma.algebra.Constant;
import pacpma.algebra.Parameter;
import pacpma.approach.Approach;
import pacpma.log.LogEngine;
import pacpma.modelchecker.ModelCheckerResult;
import pacpma.modelchecker.interactive.InteractiveModelChecker;
import pacpma.modelchecker.interactive.storm.StormCWrapper;
import pacpma.options.OptionsPacPMA;

/**
 * NLOPT approach to find minimum.
 * 
 * @author Andrea Turrini
 *
 */
public class NLOPTApproach implements Approach {
    private static LogEngine logEngineInstance;
    private static List<Parameter> parameters;
    private static InteractiveModelChecker modelChecker;
    
    public NLOPTApproach(LogEngine logEngineInstance) {
        NLOPTApproach.logEngineInstance = logEngineInstance;
        
        System.loadLibrary("nloptjni");
    }

    @Override
    public void doAnalysis() {
        parameters = OptionsPacPMA.getParameters();
        
        modelChecker = new StormCWrapper();
        modelChecker.setModelType(OptionsPacPMA.getModelType());
        modelChecker.setModelFile(OptionsPacPMA.getModelFile());
        modelChecker.setPropertyFormula(OptionsPacPMA.getPropertyFormula());
        modelChecker.setConstants(OptionsPacPMA.getConstants());
        modelChecker.setOptions(OptionsPacPMA.getModelCheckerOptions());
        modelChecker.startModelChecker();
        
        Opt optProblem = new Opt(OptionsPacPMA.getDirectAlgorithm(), parameters.size());
        
        DoubleVector lb = new DoubleVector();
        DoubleVector ub = new DoubleVector();
        for (Parameter p : parameters) {
            lb.add(p.getLowerbound().doubleValue());
            ub.add(p.getUpperbound().doubleValue());
        }
        
        optProblem.setLowerBounds(lb);
        optProblem.setUpperBounds(ub);
        
        if (OptionsPacPMA.isOptimizationDirectionMin()) {
            optProblem.setMinObjective(NLOPTApproach::f);
        } else {
            optProblem.setMaxObjective(NLOPTApproach::f);
        }
        
        Double d = OptionsPacPMA.getOptimizationStoppingValueAbsolute();
        if (d != null) {
            optProblem.setFtolAbs(d);
        }
        d = OptionsPacPMA.getOptimizationStoppingValueRelative();
        if (d != null) {
            optProblem.setFtolRel(d);
        }
        d = OptionsPacPMA.getOptimizationStoppingParametersAbsolute();
        if (d != null) {
            optProblem.setXtolAbs(d);
        }
        d = OptionsPacPMA.getOptimizationStoppingParametersRelative();
        if (d != null) {
            optProblem.setXtolRel(d);
        }
        
        DoubleVector resultVector = optProblem.optimize(lb);
        
        modelChecker.stopModelChecker();
        
        double optVal = optProblem.lastOptimumValue();
        System.out.println("Number of iterations: " + optProblem.getNumevals());
        logEngineInstance.log(LogEngine.LEVEL_INFO, "NLOPTApproach: number of iterations: " + optProblem.getNumevals());
        
        Result result = optProblem.lastOptimizeResult();
        switch (result) {
            case SUCCESS:
            case STOPVAL_REACHED:
            case FTOL_REACHED:
            case XTOL_REACHED:
            case MAXEVAL_REACHED:
            case MAXTIME_REACHED:
                int nPars = resultVector.size();
                List<Constant> optParameters = new ArrayList<>(nPars);
                for (int i = 0; i < nPars; i++) {
                    optParameters.add(new Constant(parameters.get(i).getName(), resultVector.get(i).toString()));
                }
                logEngineInstance.log(LogEngine.LEVEL_INFO, "NLOPTApproach: analysis completed; computed optimal value " + optVal + " at " + optParameters);
                System.out.println("Optimal value: " + optVal);
                System.out.println("Coordinates of optimal value: " + optParameters);
                break;
            default:
                logEngineInstance.log(LogEngine.LEVEL_INFO, "NLOPTApproach: failed analysis with result " + result);
                System.out.println("NLOPTApproach: failed analysis with result " + result);
                break;
        }
    }

    private static double f(double[] x, double[] grad) {
        int nPars = x.length;
        List<Constant> instances = new ArrayList<>(nPars);
        for (int i = 0; i < nPars; i++) {
            instances.add(new Constant(parameters.get(i).getName(), String.valueOf(x[i])));
        }
        ModelCheckerResult result = modelChecker.check(instances);
        if (result.isInfinite()) {
            logEngineInstance.log(LogEngine.LEVEL_WARNING, "NLOPTApproach: model checking result is infinite for instance " + instances.toString());
            return Double.POSITIVE_INFINITY;
        } else {
            return result.getResult().doubleValue();
        }
    }
}
