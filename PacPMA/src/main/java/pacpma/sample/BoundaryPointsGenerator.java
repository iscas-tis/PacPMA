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

package pacpma.sample;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import pacpma.algebra.Parameter;
import pacpma.options.OptionsPacPMA;

/**
 * Class that generates samples corresponding to the vertices of the hypercube
 * induced by the parameters.
 * 
 * @author Andrea Turrini
 *
 */
public class BoundaryPointsGenerator implements Sampler {
    
    public BoundaryPointsGenerator() {}

    @Override
    public List<Map<Parameter, BigDecimal>> getSamples(List<Parameter> parameters) {
        List<Map<Parameter, BigDecimal>> samples = new LinkedList<>();
        Map<Parameter, BigDecimal> currentSample = new HashMap<>();
        
        recursiveGenerator(samples, parameters, currentSample, 0);
        
        return samples;
    }

    private void recursiveGenerator(
            List<Map<Parameter, BigDecimal>> samples, 
            List<Parameter> parameters, 
            Map<Parameter, BigDecimal> currentSample, 
            int index) {
        if (index >= parameters.size()) {
            samples.addAll(getBoundaryPoints(currentSample));
        } else {
            Parameter parameter = parameters.get(index);
            currentSample.put(parameter, parameter.getLowerbound());
            recursiveGenerator(samples, parameters, currentSample, index + 1);
            currentSample.put(parameter, parameter.getUpperbound());
            recursiveGenerator(samples, parameters, currentSample, index + 1);
        }
    }
    
    private List<Map<Parameter, BigDecimal>> getBoundaryPoints(Map<Parameter, BigDecimal> currentSample) {
        MathContext mathContext = new MathContext(15, RoundingMode.HALF_DOWN); 
        List<Map<Parameter, BigDecimal>> boundarySamples = new LinkedList<>();
        boundarySamples.add(currentSample);
        for (Parameter boundary : currentSample.keySet()) {
            BigDecimal boundaryLowerbound = boundary.getLowerbound();
            if (currentSample.get(boundary).equals(boundaryLowerbound)) {
                int numPoints = OptionsPacPMA.getBoundaryPoints();
                if (numPoints > 0) {
                    BigDecimal step = boundary.getUpperbound().subtract(boundaryLowerbound).divide(new BigDecimal(numPoints + 1), mathContext);
                    BigDecimal currentValue = boundaryLowerbound;
                    for (int i = 1; i <= numPoints; i++) {
                        currentValue = currentValue.add(step);
                        Map<Parameter, BigDecimal> newSample = new HashMap<>();
                        currentSample.forEach((p,v) -> newSample.put(p, v));
                        newSample.put(boundary, currentValue);
                        boundarySamples.add(newSample);
                    }
                }
            }
        }
        return boundarySamples;
    }
}
