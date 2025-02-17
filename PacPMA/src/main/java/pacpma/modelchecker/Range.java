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

/**
 * A simple class representing intervals for model checker results
 * @author Andrea Turrini
 *
 */
public class Range {
    private ModelCheckerResult lowerbound = null;
    private ModelCheckerResult upperbound = null;
    
    public Range() {    
    }
    
    public Range(ModelCheckerResult value) {
        lowerbound = value;
        upperbound = value;
    }

    /**
     * @return the lower bound of the range
     */
    public ModelCheckerResult getLowerbound() {
        return lowerbound;
    }

    /**
     * @return the upper bound of the range
     */
    public ModelCheckerResult getUpperbound() {
        return upperbound;
    }

    /**
     * Update the range to include the new value
     * @param value the value to be included in the range
     */
    public void updateRange(ModelCheckerResult value) {
        if (lowerbound == null) {
            lowerbound = value;
        } else {
            if (lowerbound.compareTo(value) > 0) {
                lowerbound = value;
            }
        }
        
        if (upperbound == null) {
            upperbound = value;
        } else {
            if (upperbound.compareTo(value) < 0) {
                upperbound = value;
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
        } else {
            if (lowerbound.compareTo(range.getLowerbound()) > 0) {
                lowerbound = range.getLowerbound();
            }
        }
        
        if (upperbound == null) {
            upperbound = range.upperbound;
        } else {
            if (upperbound.compareTo(range.getUpperbound()) < 0) {
                upperbound = range.getUpperbound();
            }
        }
    }
}
