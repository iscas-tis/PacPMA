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

package pacpma.approach.bayesian;

import pacpma.algebra.Parameter;
import pacpma.approach.Approach;
import pacpma.log.LogEngine;
import pacpma.options.OptionsPacPMA;

/**
 * Bayesian approach to find minimum/maximum.
 * 
 * @author Andrea Turrini
 *
 */
public class BayesianApproach implements Approach {
    private static LogEngine logEngineInstance;
    private final Parameter[] parameters;

    public BayesianApproach(LogEngine logEngineInstance) {
        BayesianApproach.logEngineInstance = logEngineInstance;
        parameters = OptionsPacPMA.getParameters().toArray(new Parameter[0]);
    }

    @Override
    public void doAnalysis() {
    }
}
