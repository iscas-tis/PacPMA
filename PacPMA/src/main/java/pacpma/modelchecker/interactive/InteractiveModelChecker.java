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

package pacpma.modelchecker.interactive;

import java.util.List;

import pacpma.algebra.Constant;
import pacpma.modelchecker.ModelChecker;
import pacpma.modelchecker.ModelCheckerResult;

/**
 * An interactive model checker.
 * 
 * @author Andrea Turrini
 *
 */
public interface InteractiveModelChecker extends ModelChecker {
    
    /**
     * Starts the model checker and enables {@link #check(List)}.
     * 
     * @throws IllegalStateException
     *         if the provided information is not enough to run the model checker
     */
    public void startModelChecker() throws IllegalStateException;
    
    /**
     * Performs the analysis using {@code parameterValues} as values for the parameters.
     * 
     * @param parameterValues the values of the parameters
     * @return the result from the model checker
     * @throws IllegalStateException if {@link #startModelChecker()} was not called or {@link #stopModelChecker()} has been already called
     */
    public ModelCheckerResult check(List<Constant> parameterValues) throws IllegalStateException;
    
    /**
     * Stops the model checker.
     * 
     * @throws IllegalStateException if {@link #startModelChecker()} was not called or {@link #stopModelChecker()} has been already called
     */
    public void stopModelChecker() throws IllegalStateException;

}
