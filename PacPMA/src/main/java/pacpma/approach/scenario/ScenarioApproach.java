/****************************************************************************

    PACModel - a PAC-based model checker
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

package pacpma.approach.scenario;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import pacpma.algebra.Constant;
import pacpma.algebra.Parameter;
import pacpma.algebra.TemplateFunction;
import pacpma.algebra.Variable;
import pacpma.approach.Approach;
import pacpma.log.LogEngine;
import pacpma.lp.ConstraintComparison;
import pacpma.lp.LPVariable;
import pacpma.lp.OptimizationDirection;
import pacpma.lp.solver.LPSolver;
import pacpma.modelchecker.ModelChecker;
import pacpma.modelchecker.ModelCheckerResult;
import pacpma.modelchecker.Range;
import pacpma.modelchecker.parallel.ModelCheckerInstance;
import pacpma.modelchecker.parallel.ModelCheckerParallel;
import pacpma.options.OptionsPacPMA;
import pacpma.sample.BoundaryPointsGenerator;
import pacpma.sample.RandomSampler;

/**
 * Scenario approach to synthesize the function.
 *  
 * @author Andrea Turrini
 *
 */
public class ScenarioApproach implements Approach {
    private final static String LAMBDA = "lambda";
    
    private final LogEngine logEngineInstance;
    
    public ScenarioApproach(LogEngine logEngineInstance) {
        this.logEngineInstance = logEngineInstance;
    }

    @Override
    public void doAnalysis() {
        Random randomNumberGenerator = new Random(OptionsPacPMA.getSeed());
        
        List<Parameter> parameters = OptionsPacPMA.getParameters();
        Variable.setVariables(parameters);
        
        logEngineInstance.log(LogEngine.LEVEL_INFO, "ScenarioApproach: Setting up template function");
        TemplateFunction templateFunction = OptionsPacPMA.getTemplateFunction();
        logEngineInstance.log(LogEngine.LEVEL_INFO, "ScenarioApproach: Setting up template function done");
        if (!templateFunction.isValid()) {
            logEngineInstance.log(LogEngine.LEVEL_ERROR, "ScenarioApproach: template function not valid");
            return;
        }
        
        List<Map<Parameter, BigDecimal>> samples = new LinkedList<>();
        
        logEngineInstance.log(LogEngine.LEVEL_INFO, "ScenarioApproach: Generating samples");
        if (OptionsPacPMA.useVerticesAsSamples()) {
            samples.addAll(new BoundaryPointsGenerator().getSamples(parameters));
        }
        
        int randomSamples = OptionsPacPMA.getNumberSamples();
        if (!OptionsPacPMA.useVerticesAsAdditionalSamples()) {
            randomSamples = randomSamples - samples.size();
        }
        if (randomSamples > 0) {
            samples.addAll(new RandomSampler(randomNumberGenerator, randomSamples).getSamples(parameters));
        }
        logEngineInstance.log(LogEngine.LEVEL_INFO, "ScenarioApproach: Generating samples done");
        
        if (OptionsPacPMA.printStatistics()) {
            System.out.println("Value of ε: " + OptionsPacPMA.getEpsilon());
            System.out.println("Value of η: " + OptionsPacPMA.getEta());
            System.out.println("λ is unbounded: " + OptionsPacPMA.isLambdaUnbounded());
            if (!OptionsPacPMA.isLambdaUnbounded()) {
                System.out.println("Value of λ: " + OptionsPacPMA.getLambda());
            }
            System.out.println("Degree of the polynomial: " + OptionsPacPMA.getDegree());
            System.out.println("Number of parameters: " + parameters.size());
            System.out.println("Number of random samples: " + OptionsPacPMA.getNumberSamples());
            System.out.println("Number of total samples: " + samples.size());
            System.out.println("Number of template function coefficients: " + templateFunction.getCoefficients().size());
            return;
        }
        
        logEngineInstance.log(LogEngine.LEVEL_INFO, "ScenarioApproach: Setting up LP solver");
        List<String> lpVariableNames = new LinkedList<>();
        lpVariableNames.add(LAMBDA);
        lpVariableNames.addAll(templateFunction.getCoefficients());
        LPVariable.setVariables(lpVariableNames);
        
        final LPVariable LP_LAMBDA = LPVariable.asVariable(LAMBDA);
        
        List<LPVariable> lpVariables = LPVariable.getVariables();
        
        LPSolver lpSolver = OptionsPacPMA.getLPSolverInstance();
        lpSolver.setVariables(lpVariables);
        
        Map<LPVariable, BigDecimal> lpObjectiveFunction = new HashMap<>();
        lpVariables.forEach(lpvar -> lpObjectiveFunction.put(lpvar, BigDecimal.ZERO));
        lpObjectiveFunction.put(LP_LAMBDA, BigDecimal.ONE);
        lpSolver.setObjectiveFunction(OptimizationDirection.MIN, lpObjectiveFunction);
        
        logEngineInstance.log(LogEngine.LEVEL_INFO, "ScenarioApproach: Setting up model checker pool");
        
        int numberThreads = OptionsPacPMA.getModelCheckerThreads();
        List<ModelCheckerInstance> modelCheckerInstances = new ArrayList<>(numberThreads);
        List<Map<Integer, List<Constant>>> bucketParameterValues = new ArrayList<>(numberThreads);
        for (int i = 0; i < numberThreads; i++) {
            Map<Integer, List<Constant>> currentParameterValues = new HashMap<Integer, List<Constant>>();
            bucketParameterValues.add(currentParameterValues);

            ModelChecker currentModelChecker = OptionsPacPMA.getModelCheckerInstance();
            currentModelChecker.setModelFile(OptionsPacPMA.getModelFile());
            currentModelChecker.setModelType(OptionsPacPMA.getModelType());
            currentModelChecker.setPropertyFormula(OptionsPacPMA.getPropertyFormula());
            currentModelChecker.setConstants(OptionsPacPMA.getConstants());
            currentModelChecker.setParameterValues(currentParameterValues);
            currentModelChecker.setOptions(OptionsPacPMA.getModelCheckerOptions());
            modelCheckerInstances.add(new ModelCheckerInstance(currentModelChecker));
        }
        int i = 0;
        for (Map<Parameter, BigDecimal> sample : samples) {
            List<Constant> modelcheckerParameterValues = new LinkedList<>();
            sample.forEach((p,v) -> modelcheckerParameterValues.add(new Constant(p.getName(), v.toString())));
            bucketParameterValues.get(i % numberThreads).put(i, modelcheckerParameterValues);
            i++;
        }
        ModelCheckerParallel modelcheckerparallel = new ModelCheckerParallel(modelCheckerInstances);
        logEngineInstance.log(LogEngine.LEVEL_INFO, "ScenarioApproach: Setting up model checker pool done");
        
        logEngineInstance.log(LogEngine.LEVEL_INFO, "ScenarioApproach: Calling model checker");
        Map<Integer, ModelCheckerResult> modelcheckerResults = modelcheckerparallel.check();
        logEngineInstance.log(LogEngine.LEVEL_INFO, "ScenarioApproach: Calling model checker done");
        if (OptionsPacPMA.showRange()) {
            Range range = modelcheckerparallel.getRange();
            logEngineInstance.log(LogEngine.LEVEL_INFO, "ScenarioApproach: computed range: [" + range.getLowerbound() + ", " + range.getUpperbound() + "]");
            System.out.println("Computed range: [" + range.getLowerbound() + ", " + range.getUpperbound() + "]");
        }
        
        if (OptionsPacPMA.useLogging()) {
            logEngineInstance.flush();
        }

        logEngineInstance.log(LogEngine.LEVEL_INFO, "ScenarioApproach: Collecting model checker results");
        for (Map<Integer, List<Constant>> samplesValues : bucketParameterValues) {
            for (Integer identifier : samplesValues.keySet()) {
                Map<Parameter, BigDecimal> sample = samples.get(identifier);
                ModelCheckerResult modelcheckerResult = modelcheckerResults.get(identifier);
                if (modelcheckerResult == null) {
                    System.out.println("No result computed");
                    logEngineInstance.log(LogEngine.LEVEL_ERROR, "No result computed");
                    if (OptionsPacPMA.useLogging()) {
                        logEngineInstance.flush();
                    }
                    return;
                }
                if (modelcheckerResult.isInfinite()) {
                    System.out.println("Value of λ: not computed");
                    System.out.println("Appromixated function: infinity");
                    logEngineInstance.log(LogEngine.LEVEL_INFO, "Value of λ: not computed");
                    logEngineInstance.log(LogEngine.LEVEL_INFO, "Appromixated function: infinity");
                    if (OptionsPacPMA.useLogging()) {
                        logEngineInstance.flush();
                    }
                    return;
                }
                Map<Variable, BigDecimal> variablesValues = new HashMap<>();
                sample.forEach((p,v) -> variablesValues.put(Variable.asVariable(p), v));
                Map<String, BigDecimal> templateCoefficients = templateFunction.evaluate(variablesValues);
                
                Map<LPVariable, BigDecimal> lpConstraint = new HashMap<>();
                templateCoefficients.forEach((coefficient, value) -> lpConstraint.put(LPVariable.asVariable(coefficient), value));
                lpConstraint.put(LP_LAMBDA, BigDecimal.ONE);
                lpSolver.addConstraint(lpConstraint, ConstraintComparison.GE, modelcheckerResult.getResult());
                lpConstraint.put(LP_LAMBDA, BigDecimal.ONE.negate());
                lpSolver.addConstraint(lpConstraint, ConstraintComparison.LE, modelcheckerResult.getResult());
            }
        }
        logEngineInstance.log(LogEngine.LEVEL_INFO, "ScenarioApproach: Collecting model checker results done");
        logEngineInstance.log(LogEngine.LEVEL_INFO, "ScenarioApproach: Setting up LP solver done");

        logEngineInstance.log(LogEngine.LEVEL_INFO, "ScenarioApproach: Calling LP solver");
        Map<LPVariable, BigDecimal> lpSolution = lpSolver.solve();
        logEngineInstance.log(LogEngine.LEVEL_INFO, "ScenarioApproach: Calling LP solver done");
        if (lpSolution == null) {
            System.out.println("Failed to approximate the function for " + OptionsPacPMA.getPropertyFormula());
            logEngineInstance.log(LogEngine.LEVEL_INFO, "Failed to approximate the function for " + OptionsPacPMA.getPropertyFormula());
        } else {
            String lambdaValue = lpSolver.getLambdaValue().toString();
            Map<String, BigDecimal> solution = new HashMap<>();
            lpSolution.forEach((variable, value) -> solution.put(variable.getName(), value));
            String templateExpression = null;
            switch (OptionsPacPMA.getFunctionFormat()) {
            case OptionsPacPMA.FORMAT_LATEX:
                templateExpression = templateFunction.getLatexExpression(solution);
                break;
            case OptionsPacPMA.FORMAT_MATH:
                templateExpression = templateFunction.getMathExpression(solution);
                break;
            case OptionsPacPMA.FORMAT_MATLAB:
                templateExpression = templateFunction.getMatlabExpression(solution);
                break;
            }
            System.out.println("Value of λ: " + lambdaValue);
            System.out.println("Appromixated function: " + templateExpression);
            logEngineInstance.log(LogEngine.LEVEL_INFO, "Value of λ: " + lambdaValue);
            logEngineInstance.log(LogEngine.LEVEL_INFO, "Appromixated function: " + templateExpression);
        }
        if (OptionsPacPMA.useLogging()) {
            logEngineInstance.flush();
        }
    } 

}
