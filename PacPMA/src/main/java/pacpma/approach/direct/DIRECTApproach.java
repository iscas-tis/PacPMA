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

package pacpma.approach.direct;

import java.util.List;

import nlopt.Algorithm;
import nlopt.DoubleVector;
import nlopt.Opt;
import nlopt.Result;
import pacpma.algebra.Parameter;
import pacpma.approach.Approach;
import pacpma.log.LogEngine;
import pacpma.options.OptionsPacPMA;

/**
 * DIRECT approach to find minimum.
 * 
 * @author Andrea Turrini
 *
 */
public class DIRECTApproach implements Approach {
    private final LogEngine logEngineInstance;
    
    public DIRECTApproach(LogEngine logEngineInstance) {
        this.logEngineInstance = logEngineInstance;
        
        System.loadLibrary("nloptjni");
    }

    @Override
    public void doAnalysis() {
        List<Parameter> parameters = OptionsPacPMA.getParameters();
        int nPars = parameters.size();
        
        Opt optProblem = new Opt(Algorithm.GN_DIRECT, nPars);
        
        DoubleVector lb = new DoubleVector();
        DoubleVector ub = new DoubleVector();
        for (Parameter p : parameters) {
            lb.add(p.getLowerbound().doubleValue());
            ub.add(p.getUpperbound().doubleValue());
        }
        
        optProblem.setLowerBounds(lb);
        optProblem.setUpperBounds(ub);
        
        optProblem.setMaxObjective(DIRECTApproach::f);
       
        DoubleVector resultVector = optProblem.optimize(lb);
        double optVal = optProblem.lastOptimumValue();
        Result result = optProblem.lastOptimizeResult();
        switch (result) {
            case SUCCESS:
                logEngineInstance.log(LogEngine.LEVEL_INFO, "DIRECTApproach: analysis completed; computed value " + optVal);
                break;
            default:
                logEngineInstance.log(LogEngine.LEVEL_INFO, "DIRECTApproach: failed analysis with result " + result);
                break;
        }
    }

    private static double f(double[] x, double[] grad) {
        return 0;
    }
}
