/****************************************************************************

    PacPMA - the PAC-based Parametric Model Analyzer
    Copyright (C) 2024

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

package pacpma.modelchecker;

import java.util.List;

import pacpma.algebra.Constant;

/**
 * A simple class representing intervals for model checker results
 * @author Andrea Turrini
 *
 */
public class Range {
    private ModelCheckerResult lowerbound = null;
    private ModelCheckerResult upperbound = null;
    private List<Constant> lowerboundParameters = null;
    private List<Constant> upperboundParameters = null;
   
    
    public Range() {    
    }
    
    public Range(ModelCheckerResult value, List<Constant> parameters) {
        lowerbound = value;
        upperbound = value;
        lowerboundParameters = parameters;
        upperboundParameters = parameters;
    }

    /**
     * @return the lower bound of the range
     */
    public ModelCheckerResult getLowerbound() {
        return lowerbound;
    }

    /**
     * @return the parameters corresponding to the lower bound of the range
     */
    public List<Constant> getLowerboundParameters() {
        return lowerboundParameters;
    }

    /**
     * @return the upper bound of the range
     */
    public ModelCheckerResult getUpperbound() {
        return upperbound;
    }

    /**
     * @return the parameters corresponding to the upper bound of the range
     */
    public List<Constant> getUpperboundParameters() {
        return upperboundParameters;
    }

    /**
     * Update the range to include the new value
     * @param value the value to be included in the range
     * @param parameters the parameters corresponding to the {@code value}
     */
    public void updateRange(ModelCheckerResult value, List<Constant> parameters) {
        if (lowerbound == null) {
            lowerbound = value;
            lowerboundParameters = parameters;
        } else {
            if (lowerbound.compareTo(value) > 0) {
                lowerbound = value;
                lowerboundParameters = parameters;
            }
        }
        
        if (upperbound == null) {
            upperbound = value;
            upperboundParameters = parameters;
        } else {
            if (upperbound.compareTo(value) < 0) {
                upperbound = value;
                upperboundParameters = parameters;
            }
        }
    }

    /**
     * Update the range to include the new range
     * @param range the range to be included in the current range
     */
    public void updateRange(Range range) {
        if (lowerbound == null) {
            lowerbound = range.lowerbound;
            lowerboundParameters = range.lowerboundParameters;
        } else {
            if (lowerbound.compareTo(range.getLowerbound()) > 0) {
                lowerbound = range.lowerbound;
                lowerboundParameters = range.lowerboundParameters;
            }
        }
        
        if (upperbound == null) {
            upperbound = range.upperbound;
            upperboundParameters = range.upperboundParameters;
        } else {
            if (upperbound.compareTo(range.getUpperbound()) < 0) {
                upperbound = range.upperbound;
                upperboundParameters = range.upperboundParameters;
            }
        }
    }
}
