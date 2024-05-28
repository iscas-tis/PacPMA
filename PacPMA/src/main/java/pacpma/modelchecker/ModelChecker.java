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

package pacpma.modelchecker;

import java.util.List;
import java.util.Map;

import pacpma.algebra.Constant;

/**
 * Interface representing a model checker.
 * 
 * @author Andrea Turrini
 *
 */
public interface ModelChecker {
    
    
    /**
     * Generates an instance of the class implementing {@link ModelChecker}
     * @return an instance of this {@link ModelChecker}
     */
    public ModelChecker getInstance();

    /**
     * Sets the path to the model file to analyze.
     * 
     * @param filePath
     *            the path to the model file
     * @return this model checker, for chaining methods invocations
     */
    public ModelChecker setModelFile(String filePath);

    /**
     * Sets the model type, like prism or jani.
     * 
     * @param modelType
     *            the model type
     * @return this model checker, for chaining methods invocations
     */
    public ModelChecker setModelType(String modelType);

    /**
     * Sets the quantitative property formula to verify. It must be a valid formula
     * supported by the actual model checker.
     * 
     * @param propertyFormula
     *            the property formula
     * @return this model checker, for chaining methods invocations
     */
    public ModelChecker setPropertyFormula(String propertyFormula);

    /**
     * Sets the constants of the models.
     * 
     * @param constants
     *            the list of constants
     * @return this model checker, for chaining methods invocations
     */
    public ModelChecker setConstants(List<Constant> constants);

    /**
     * Sets the parameter values to make the model checkable.
     * 
     * @param parameterValues
     *            a mapping between some integer identifier and the corresponding list of constants
     * @return this model checker, for chaining methods invocations
     */
    public ModelChecker setParameterValues(Map<Integer, List<Constant>> parameterValues);

    /**
     * Additional options to be passed to the model checker.
     * 
     * @param options
     *            the list of options
     * @return this model checker, for chaining methods invocations
     */
    public ModelChecker setOptions(List<String> options);

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
    
    /**
     * Provides a range of the values computed by the model checker on the provided
     * constants.
     * 
     * @return an interval about the range of the values computed by the model checker
     * @throws IllegalStateException
     *             if the model has not been checked yet
     */
    public Range range() throws IllegalStateException;
}
