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

package pacpma;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import pacpma.algebra.Constant;
import pacpma.algebra.Parameter;
import pacpma.algebra.Polynomial;
import pacpma.algebra.Variable;
import pacpma.log.Logger;
import pacpma.lp.ConstraintComparison;
import pacpma.lp.LPVariable;
import pacpma.lp.OptimizationDirection;
import pacpma.lp.solver.LPSolver;
import pacpma.modelchecker.ModelChecker;
import pacpma.modelchecker.ModelCheckerResult;
import pacpma.modelchecker.parallel.ModelCheckerInstance;
import pacpma.modelchecker.parallel.ModelCheckerParallel;
import pacpma.options.OptionsPacPMA;
import pacpma.sample.BoundaryPointsGenerator;
import pacpma.sample.RandomSampler;

/**
 * PAC model main class
 * 
 * @author Andrea Turrini
 */
public final class PacPMA {
    private final static String LAMBDA = "lambda";

    public static void main(String[] args) {
        if (OptionsPacPMA.parseOptions(args)) {
            Logger.setup(OptionsPacPMA.getLogLevel(), OptionsPacPMA.getLogFile());
            
            Random randomNumberGenerator = new Random(OptionsPacPMA.getSeed());
            
            List<Parameter> parameters = OptionsPacPMA.getParameters();
            Variable.setVariables(parameters);
            
            Logger.log(Logger.LEVEL_INFO, "PacPMA: Setting up polynomial");
            Polynomial polynomial = new Polynomial(OptionsPacPMA.getDegree());
            Logger.log(Logger.LEVEL_INFO, "PacPMA: Setting up polynomial done");
            
            List<Map<Parameter, BigDecimal>> samples = new LinkedList<>();
            
            Logger.log(Logger.LEVEL_INFO, "PacPMA: Generating samples");
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
            Logger.log(Logger.LEVEL_INFO, "PacPMA: Generating samples done");
            
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
                System.out.println("Number of polynomial coefficients: " + polynomial.getCoefficients().size());
                return;
            }
            
            Logger.log(Logger.LEVEL_INFO, "PacPMA: Setting up LP solver");
            List<String> lpVariableNames = new LinkedList<>();
            lpVariableNames.add(LAMBDA);
            lpVariableNames.addAll(polynomial.getCoefficients());
            LPVariable.setVariables(lpVariableNames);
            
            final LPVariable LP_LAMBDA = LPVariable.asVariable(LAMBDA);
            
            List<LPVariable> lpVariables = LPVariable.getVariables();
            
            LPSolver lpSolver = OptionsPacPMA.getLPSolverInstance();
            lpSolver.setVariables(lpVariables);
            
            Map<LPVariable, BigDecimal> lpObjectiveFunction = new HashMap<>();
            lpVariables.forEach(lpvar -> lpObjectiveFunction.put(lpvar, BigDecimal.ZERO));
            lpObjectiveFunction.put(LP_LAMBDA, BigDecimal.ONE);
            lpSolver.setObjectiveFunction(OptimizationDirection.MIN, lpObjectiveFunction);
            
            Logger.log(Logger.LEVEL_INFO, "PacPMA: Setting up model checker pool");
            
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
            Logger.log(Logger.LEVEL_INFO, "PacPMA: Setting up model checker pool done");
            
            Logger.log(Logger.LEVEL_INFO, "PacPMA: Calling model checker");
            Map<Integer, ModelCheckerResult> modelcheckerResults = modelcheckerparallel.check();
            Logger.log(Logger.LEVEL_INFO, "PacPMA: Calling model checker done");
            
            if (OptionsPacPMA.useLogging()) {
                Logger.saveToFile();
            }

            Logger.log(Logger.LEVEL_INFO, "PacPMA: Collecting model checker results");
            for (Map<Integer, List<Constant>> samplesValues : bucketParameterValues) {
                for (Integer identifier : samplesValues.keySet()) {
                    Map<Parameter, BigDecimal> sample = samples.get(identifier);
                    ModelCheckerResult modelcheckerResult = modelcheckerResults.get(identifier);
                    if (modelcheckerResult == null) {
                        System.out.println("No result computed");
                        Logger.log(Logger.LEVEL_ERROR, "No result computed");
                        if (OptionsPacPMA.useLogging()) {
                            Logger.saveToFile();
                        }
                        return;
                    }
                    if (modelcheckerResult.isInfinite()) {
                        System.out.println("Value of λ: not computed");
                        System.out.println("Appromixated function: infinity");
                        Logger.log(Logger.LEVEL_INFO, "Value of λ: not computed");
                        Logger.log(Logger.LEVEL_INFO, "Appromixated function: infinity");
                        if (OptionsPacPMA.useLogging()) {
                            Logger.saveToFile();
                        }
                        return;
                    }
                    Map<Variable, BigDecimal> variablesValues = new HashMap<>();
                    sample.forEach((p,v) -> variablesValues.put(Variable.asVariable(p), v));
                    Map<String, BigDecimal> polynomialCoefficients = polynomial.evaluate(variablesValues);
                    
                    Map<LPVariable, BigDecimal> lpConstraint = new HashMap<>();
                    polynomialCoefficients.forEach((coefficient, value) -> lpConstraint.put(LPVariable.asVariable(coefficient), value));
                    lpConstraint.put(LP_LAMBDA, BigDecimal.ONE);
                    lpSolver.addConstraint(lpConstraint, ConstraintComparison.GE, modelcheckerResult.getResult());
                    lpConstraint.put(LP_LAMBDA, BigDecimal.ONE.negate());
                    lpSolver.addConstraint(lpConstraint, ConstraintComparison.LE, modelcheckerResult.getResult());
                }
            }
            Logger.log(Logger.LEVEL_INFO, "PacPMA: Collecting model checker results done");
            Logger.log(Logger.LEVEL_INFO, "PacPMA: Setting up LP solver done");

            Logger.log(Logger.LEVEL_INFO, "PacPMA: Calling LP solver");
            Map<LPVariable, BigDecimal> lpSolution = lpSolver.solve();
            Logger.log(Logger.LEVEL_INFO, "PacPMA: Calling LP solver done");
            if (lpSolution == null) {
                System.out.println("Failed to approximate the function for " + OptionsPacPMA.getPropertyFormula());
                Logger.log(Logger.LEVEL_INFO, "Failed to approximate the function for " + OptionsPacPMA.getPropertyFormula());
            } else {
                String lambdaValue = lpSolver.getLambdaValue().toString();
                Map<String, BigDecimal> solution = new HashMap<>();
                lpSolution.forEach((variable, value) -> solution.put(variable.getName(), value));
                String polynomialExpression = null;
                switch (OptionsPacPMA.getFormatPolynomial()) {
                case OptionsPacPMA.FORMAT_LATEX:
                    polynomialExpression = polynomial.getLatexExpression(solution);
                    break;
                case OptionsPacPMA.FORMAT_MATH:
                    polynomialExpression = polynomial.getMathExpression(solution);
                    break;
                case OptionsPacPMA.FORMAT_MATLAB:
                    polynomialExpression = polynomial.getMatlabExpression(solution);
                    break;
                }
                System.out.println("Value of λ: " + lambdaValue);
                System.out.println("Appromixated function: " + polynomialExpression);
                Logger.log(Logger.LEVEL_INFO, "Value of λ: " + lambdaValue);
                Logger.log(Logger.LEVEL_INFO, "Appromixated function: " + polynomialExpression);
            }
            if (OptionsPacPMA.useLogging()) {
                Logger.saveToFile();
            }
        }
    }
}
