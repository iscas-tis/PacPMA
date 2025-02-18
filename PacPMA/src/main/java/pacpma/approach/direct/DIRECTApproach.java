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

package pacpma.approach.direct;

import java.util.ArrayList;
import java.util.List;

import nlopt.Algorithm;
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
 * DIRECT approach to find minimum.
 * 
 * @author Andrea Turrini
 *
 */
public class DIRECTApproach implements Approach {
    private static LogEngine logEngineInstance;
    private static List<Parameter> parameters;
    private static InteractiveModelChecker modelChecker;
    
    public DIRECTApproach(LogEngine logEngineInstance) {
        DIRECTApproach.logEngineInstance = logEngineInstance;
        
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
        
        Opt optProblem = new Opt(Algorithm.GN_DIRECT, parameters.size());
        
        DoubleVector lb = new DoubleVector();
        DoubleVector ub = new DoubleVector();
        for (Parameter p : parameters) {
            lb.add(p.getLowerbound().doubleValue());
            ub.add(p.getUpperbound().doubleValue());
        }
        
        optProblem.setLowerBounds(lb);
        optProblem.setUpperBounds(ub);
        
        if (OptionsPacPMA.isDirectOptimizationDirectionMin()) {
            optProblem.setMinObjective(DIRECTApproach::f);
        } else {
            optProblem.setMaxObjective(DIRECTApproach::f);
        }
        
        Double d = OptionsPacPMA.getDirectStoppingValueAbsolute();
        if (d != null) {
            optProblem.setFtolAbs(d);
        }
        d = OptionsPacPMA.getDirectStoppingValueRelative();
        if (d != null) {
            optProblem.setFtolRel(d);
        }
        d = OptionsPacPMA.getDirectStoppingParametersAbsolute();
        if (d != null) {
            optProblem.setXtolAbs(d);
        }
        d = OptionsPacPMA.getDirectStoppingParametersRelative();
        if (d != null) {
            optProblem.setXtolRel(d);
        }
        
        DoubleVector resultVector = optProblem.optimize(lb);
        
        modelChecker.stopModelChecker();
        
        double optVal = optProblem.lastOptimumValue();
        System.out.println("DIRECTApproach: number of iterations: " + optProblem.getNumevals());
        logEngineInstance.log(LogEngine.LEVEL_INFO, "DIRECTApproach: number of iterations: " + optProblem.getNumevals());
        
        Result result = optProblem.lastOptimizeResult();
        switch (result) {
            case SUCCESS:
            case STOPVAL_REACHED:
            case FTOL_REACHED:
            case XTOL_REACHED:
            case MAXEVAL_REACHED:
            case MAXTIME_REACHED:
                logEngineInstance.log(LogEngine.LEVEL_INFO, "DIRECTApproach: analysis completed; computed value " + optVal);
                System.out.println("DIRECTApproach: analysis completed; computed value " + optVal);
                break;
            default:
                logEngineInstance.log(LogEngine.LEVEL_INFO, "DIRECTApproach: failed analysis with result " + result);
                System.out.println("DIRECTApproach: failed analysis with result " + result);
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
            logEngineInstance.log(LogEngine.LEVEL_WARNING, "DIRECTApproach: model checking result is infinite for instance " + instances.toString());
            throw new IllegalArgumentException("DIRECTApproach: model checking result is infinite for instance " + instances.toString());
        } else {
            return result.getResult().doubleValue();
        }
    }
}
