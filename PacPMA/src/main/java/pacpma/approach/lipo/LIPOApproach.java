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
    private final int iterationCounterLimit;
    private final double exploitationThreshold;
    private final double exploitationCounterLimit;
    
    private int iterationCounter;

    private double valueMax;
    private double[] coordinatesValueMax = null;
    private double valueMaxSecond;
    private double[] coordinatesValueMaxSecond = null;

    public LIPOApproach(LogEngine logEngineInstance) {
        LIPOApproach.logEngineInstance = logEngineInstance;
        parameters = OptionsPacPMA.getParameters().toArray(new Parameter[0]);
        iterationCounterLimit = OptionsPacPMA.getOptimizationStoppingIterationLimit();
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
        double[] y = new double[iterationCounterLimit];
        double[][] x = new double[iterationCounterLimit][d];
        double[] loss = new double[iterationCounterLimit];
        double[] k_arr = new double[iterationCounterLimit];

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
        double[] u = new double[d];
        for (int i = 0; i < d; i++) {
            u[i] = randomNumberGenerator.nextDouble();
        }
        coordinatesValueMax = new double[d];
        for (int i = 0; i < d; i++) {
            x[0][i] = coordinatesValueMax[i] = u[i] * (bound_maxs[i] - bound_mins[i]) + bound_mins[i];
        }
        List<Constant> instances = new ArrayList<>(d);
        for (int i = 0; i < d; i++) {
            instances.add(new Constant(parameters[i].getName(), String.valueOf(coordinatesValueMax[i])));
        }
        ModelCheckerResult result = modelChecker.check(instances);
        if (result.isInfinite()) {
            logEngineInstance.log(LogEngine.LEVEL_WARNING, "LIPOApproach: model checking result is infinite for instance " + instances.toString());
            return;
        } else {
            y[0] = result.getResult().doubleValue();
        }
        k_arr[0] = k;
        
        valueMax = y[0];
        int tMax = 0;
        
        iterationCounter = 1;
        while (improve()) {
            logEngineInstance.log(LogEngine.LEVEL_INFO, "LIPOApproach: iteration: " + iterationCounter);
            
            double[] x_prop = new double[d];
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
            for (int i = 0; i < d; i++) {
                x[iterationCounter][i] = x_prop[i];
            }
            instances = new ArrayList<>(d);
            for (int i = 0; i < d; i++) {
                instances.add(new Constant(parameters[i].getName(), String.valueOf(x_prop[i])));
            }
            result = modelChecker.check(instances);
            if (result.isInfinite()) {
                logEngineInstance.log(LogEngine.LEVEL_WARNING, "LIPOApproach: model checking result is infinite for instance " + instances.toString());
                return;
            } else {
                y[iterationCounter] = result.getResult().doubleValue();
                if (valueMax < y[iterationCounter]) {
                    valueMaxSecond = valueMax;
                    valueMax = y[iterationCounter];
                    coordinatesValueMaxSecond = coordinatesValueMax;
                    coordinatesValueMax = x_prop;
                    tMax = iterationCounter;
                }
            }
            loss[iterationCounter] = valueMax;
            
            double[] new_x_dist = new double[iterationCounter];
            for (int i = 0; i < iterationCounter; i++) {
                double sum = 0;
                for (int j = 0; j < d; j++) {
                    sum = sum + Math.pow(x[i][j] - x[iterationCounter][j], 2);
                }
                new_x_dist[i] = Math.sqrt(sum);
            }
            double[] new_y_dist = new double[iterationCounter];
            for (int i = 0; i < iterationCounter; i++) {
                new_y_dist[i] = Math.abs(y[i] - y[iterationCounter]);
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
            k_arr[iterationCounter] = k;
            
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
        return iterationCounter < iterationCounterLimit;
    }

    private double upper_bound(int limit, double[] x_prop, double[] y, double[][] x, double k) {
        double min_value = Double.POSITIVE_INFINITY;
        for (int i = 0; i < limit; i++) {
            double norm2 = 0;
            for (int j = 0; j < x_prop.length; j++) {
                norm2 = norm2 + Math.pow(x_prop[j] - x[i][j], 2);
            }
            double cur_value = y[i] + k *  Math.sqrt(norm2);
            if (min_value > cur_value) {
                min_value = cur_value;
            }
        }
        return min_value;
    }
    
}
