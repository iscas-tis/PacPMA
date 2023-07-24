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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import pacpma.algebra.Parameter;

/**
 * Random sample generator
 * 
 * @author Andrea Turrini
 *
 */
public class RandomSampler implements Sampler {
    private final Random randomGenerator;
    private final long numberSamples;
    
    public RandomSampler(Random randomGenerator, long numberSamples) {
        this.randomGenerator = randomGenerator;
        this.numberSamples = numberSamples;
    }
    
    @Override
    public List<Map<Parameter, BigDecimal>> getSamples(List<Parameter> parameters) {
        List<Map<Parameter, BigDecimal>> samples = new LinkedList<>();
        
        for (int i = 0; i < numberSamples; i++) {
            Map<Parameter, BigDecimal> singleSample = new HashMap<>(2*parameters.size());
            parameters.forEach((p) -> singleSample.put(p, getSample(p)));
            samples.add(singleSample);
        }
        
        return samples;
    }
    
    private BigDecimal getSample(Parameter p) {
        return nextValue(p.getLowerbound(), p.getUpperbound());
    }
    
    private BigDecimal nextValue(BigDecimal lowerbound, BigDecimal upperbound) {
        return new BigDecimal(randomGenerator.nextDouble(lowerbound.doubleValue(), upperbound.doubleValue()));
    }
}
