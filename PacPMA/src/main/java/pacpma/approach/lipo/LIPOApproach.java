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

package pacpma.approach.lipo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import pacpma.algebra.Constant;
import pacpma.algebra.Parameter;
import pacpma.approach.Approach;
import pacpma.log.LogEngine;
import pacpma.modelchecker.ModelCheckerResult;
import pacpma.modelchecker.interactive.InteractiveModelChecker;
import pacpma.modelchecker.interactive.storm.StormCWrapper;
import pacpma.options.OptionsPacPMA;

/**
 * Approach based on AdaptLIPO to find minimum/maximum.
 * Implementation adapted from https://github.com/UBC-CS/lipo-python/src/sequential.py
 * 
 * @author Andrea Turrini
 *
 */
public class LIPOApproach implements Approach {
    private static LogEngine logEngineInstance;
    private final Parameter[] parameters;
    private final double exploitationThreshold;
    private final double exploitationCounterLimit;
    
    private int iterationCounter;

    private double valueMax;
    private Double[] coordinatesValueMax;
    private Double valueMaxSecond = null;
    private Double[] coordinatesValueMaxSecond = null;

    public LIPOApproach(LogEngine logEngineInstance) {
        LIPOApproach.logEngineInstance = logEngineInstance;
        parameters = OptionsPacPMA.getParameters().toArray(new Parameter[0]);
        exploitationThreshold = OptionsPacPMA.getExploitationThreshold();
        exploitationCounterLimit = OptionsPacPMA.getExploitationLimit();
    }

    @Override
    public void doAnalysis() {
        Random randomNumberGenerator = new Random(OptionsPacPMA.getSeed());
        
        InteractiveModelChecker modelChecker = new StormCWrapper();
        modelChecker.setModelType(OptionsPacPMA.getModelType());
        modelChecker.setModelFile(OptionsPacPMA.getModelFile());
        modelChecker.setPropertyFormula(OptionsPacPMA.getPropertyFormula());
        modelChecker.setConstants(OptionsPacPMA.getConstants());
        modelChecker.setOptions(OptionsPacPMA.getModelCheckerOptions());
        modelChecker.startModelChecker();
                
        // dimension of the domain
        final int d = parameters.length;
        
        double alpha = 0.01/d;

        // preallocate the output arrays
        List<Double> y = new ArrayList<>();
        List<Double[]> x = new ArrayList<>();
        List<Double> loss = new ArrayList<>();
        List<Double> k_arr = new ArrayList<>();

        // the lower/upper bounds on each dimension
        double[] bound_mins = new double[d];
        double[] bound_maxs = new double[d];
        for (int i = 0; i < d; i++) {
            bound_mins[i] = parameters[i].getLowerbound().doubleValue();
            bound_maxs[i] = parameters[i].getUpperbound().doubleValue();
        }
        
        // initialization with randomly drawn point in domain and k = 0
        double k = 0;
        double k_est = Double.NEGATIVE_INFINITY;
        Double[] u = new Double[d];
        for (int i = 0; i < d; i++) {
            u[i] = randomNumberGenerator.nextDouble();
        }
        coordinatesValueMax = new Double[d];
        for (int i = 0; i < d; i++) {
            coordinatesValueMax[i] = u[i] * (bound_maxs[i] - bound_mins[i]) + bound_mins[i];
        }
        x.add(coordinatesValueMax);
        
        List<Constant> instances = new ArrayList<>(d);
        for (int i = 0; i < d; i++) {
            instances.add(new Constant(parameters[i].getName(), String.valueOf(coordinatesValueMax[i])));
        }
        ModelCheckerResult result = modelChecker.check(instances);
        if (result.isInfinite()) {
            logEngineInstance.log(LogEngine.LEVEL_WARNING, "LIPOApproach: model checking result is infinite for instance " + instances.toString());
            return;
        } else {
            y.add(result.getResult().doubleValue());
        }
        k_arr.add(k);
        
        valueMax = y.get(0);
        int tMax = 0;
        
        iterationCounter = 1;
        while (improve()) {
            logEngineInstance.log(LogEngine.LEVEL_INFO, "LIPOApproach: iteration: " + iterationCounter);
            
            Double[] x_prop = new Double[d];
            for (int i = 0; i < d; i++) {
                u[i] = randomNumberGenerator.nextDouble();
            }
            for (int i = 0; i < d; i++) {
                x_prop[i] = u[i] * (bound_maxs[i] - bound_mins[i]) + bound_mins[i];
            }
            // check if we are exploring or exploiting
            if (randomNumberGenerator.nextDouble() > exploitationThreshold) { // enter to exploit w/ prob (1-p)
                logEngineInstance.log(LogEngine.LEVEL_INFO, "LIPOApproach: starting exploiting");
                // exploiting - ensure we're drawing from potential maximizers
                int iters = 0;
                while (iters < exploitationCounterLimit && upper_bound(iterationCounter, x_prop, y, x, k) < valueMax) {
                    for (int i = 0; i < d; i++) {
                        u[i] = randomNumberGenerator.nextDouble();
                    }
                    for (int i = 0; i < d; i++) {
                        x_prop[i] = u[i] * (bound_maxs[i] - bound_mins[i]) + bound_mins[i];
                    }
                    iters++;
                }
                logEngineInstance.log(LogEngine.LEVEL_INFO, "LIPOApproach: exploiting done");
            } else {
                logEngineInstance.log(LogEngine.LEVEL_INFO, "LIPOApproach: no exploiting in this iteration");
            }
            x.add(x_prop);
            
            instances = new ArrayList<>(d);
            for (int i = 0; i < d; i++) {
                instances.add(new Constant(parameters[i].getName(), String.valueOf(x_prop[i])));
            }
            result = modelChecker.check(instances);
            if (result.isInfinite()) {
                logEngineInstance.log(LogEngine.LEVEL_WARNING, "LIPOApproach: model checking result is infinite for instance " + instances.toString());
                return;
            }
            double yValue = result.getResult().doubleValue();
            y.add(yValue);
            if (valueMax < yValue) {
                valueMaxSecond = valueMax;
                valueMax = yValue;
                coordinatesValueMaxSecond = coordinatesValueMax;
                coordinatesValueMax = x_prop;
                tMax = iterationCounter;
            }
            loss.add(valueMax);
            
            double[] new_x_dist = new double[iterationCounter];
            for (int i = 0; i < iterationCounter; i++) {
                double sum = 0;
                Double[] x_prev = x.get(i);
                for (int j = 0; j < d; j++) {
                    sum = sum + Math.pow(x_prev[j] - x_prop[j], 2);
                }
                new_x_dist[i] = Math.sqrt(sum);
            }
            double[] new_y_dist = new double[iterationCounter];
            for (int i = 0; i < iterationCounter; i++) {
                new_y_dist[i] = Math.abs(y.get(i) - yValue);
            }
            for (int i = 0; i < iterationCounter; i++) {
                double div = new_y_dist[i]/new_x_dist[i];
                if (k_est < div) {
                    k_est = div;
                }
            }
            double i_t = Math.ceil(Math.log(k_est)/Math.log(1+alpha));
            k = Math.pow(1+alpha, i_t);
            logEngineInstance.log(LogEngine.LEVEL_WARNING, "LIPOApproach: Lipschitz constant estimate: " + k);
            k_arr.add(k);
            
            iterationCounter++;
        }
        
        modelChecker.stopModelChecker();
        
        List<Constant> optParameters = new ArrayList<>(d);
        for (int i = 0; i < d; i++) {
            optParameters.add(new Constant(parameters[i].getName(), Double.toString(coordinatesValueMax[i])));
        }
        logEngineInstance.log(LogEngine.LEVEL_INFO, "LIPOApproach: analysis completed; computed optimal value " + valueMax + " at " + optParameters + " during iteration " + tMax);
        System.out.println("Optimal value: " + valueMax);
        System.out.println("Coordinates of optimal value: " + optParameters);
        System.out.println("Iteration of optimal value: " + tMax);      
    }
    
    private boolean improve() {
        boolean canImprove = true;
        
        Integer icl = OptionsPacPMA.getOptimizationStoppingIterationLimit();
        if (icl != null) {
            canImprove &= iterationCounter < icl;
        }
        
        Double sc = OptionsPacPMA.getOptimizationStoppingValueAbsolute();
        if (sc != null && valueMaxSecond != null) {
            canImprove &= Math.abs(valueMax - valueMaxSecond) >= sc;
        }
        
        sc = OptionsPacPMA.getOptimizationStoppingValueRelative();
        if (sc != null && valueMaxSecond != null && valueMax != 0) {
            canImprove &= Math.abs((valueMax - valueMaxSecond) / valueMax) >= sc;
        }
        
        sc = OptionsPacPMA.getOptimizationStoppingParametersAbsolute();
        if (sc != null && coordinatesValueMaxSecond != null) {
            canImprove &= norm(coordinatesValueMax, coordinatesValueMaxSecond) >= sc;
        }
        
        sc = OptionsPacPMA.getOptimizationStoppingParametersRelative();
        double normCVM = norm(coordinatesValueMax);
        if (sc != null && coordinatesValueMaxSecond != null && normCVM != 0) {
            canImprove &= norm(coordinatesValueMax, coordinatesValueMaxSecond) / normCVM >= sc;
        }
        
        return canImprove;
    }

    private double upper_bound(int limit, Double[] x_prop, List<Double> y, List<Double[]> x, double k) {
        double min_value = Double.POSITIVE_INFINITY;
        for (int i = 0; i < limit; i++) {
            double cur_value = y.get(i) + k *  norm(x.get(i), x_prop);
            if (min_value > cur_value) {
                min_value = cur_value;
            }
        }
        return min_value;
    }
    
    private double norm(Double[] v1, Double[] v2) {
        assert (v1.length == v2.length);
        
        double norm2 = 0;
        for (int j = 0; j < v1.length; j++) {
            norm2 = norm2 + Math.pow(v1[j] - v2[j], 2);
        }
        return Math.sqrt(norm2);
    }
    
    private double norm(Double[] vect) {
        double norm2 = 0;
        for (int j = 0; j < vect.length; j++) {
            norm2 = norm2 + Math.pow(vect[j], 2);
        }
        return Math.sqrt(norm2);
    }
}
