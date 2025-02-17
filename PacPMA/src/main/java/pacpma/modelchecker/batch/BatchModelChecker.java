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

package pacpma.modelchecker.batch;

import java.util.List;
import java.util.Map;

import pacpma.algebra.Constant;
import pacpma.modelchecker.ModelChecker;
import pacpma.modelchecker.ModelCheckerResult;

/**
 * Interface representing a batch model checker.
 * 
 * @author Andrea Turrini
 *
 */
public interface BatchModelChecker extends ModelChecker {

    /**
     * Sets the parameter values to make the model checkable.
     * 
     * @param parameterValues
     *            a mapping between some integer identifier and the corresponding list of constants
     * @return this model checker, for chaining methods invocations
     */
    default BatchModelChecker setParameterValues(Map<Integer, List<Constant>> parameterValues) {
        return this;
    };

    /**
     * Checks the given formula against the model instantiated with the provided
     * constants.
     * 
     * @return a map associating to each integer identifier given in
     *         {@link #setConstants(Map)} the corresponding value of the checked
     *         quantitative formula
     * @throws IllegalStateException
     *             if the model can't be checked with the provided information
     */
    public Map<Integer, ModelCheckerResult> check() throws IllegalStateException;
}
