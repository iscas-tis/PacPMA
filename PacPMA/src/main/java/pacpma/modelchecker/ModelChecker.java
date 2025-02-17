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

package pacpma.modelchecker;

import java.util.List;

import pacpma.algebra.Constant;

/**
 * Interface representing a model checker.
 * 
 * @author Andrea Turrini
 *
 */
public interface ModelChecker {
    
    /**
     * Sets the path to the model file to analyze.
     * 
     * @param filePath
     *            the path to the model file
     * @return this model checker, for chaining methods invocations
     */
    default ModelChecker setModelFile(String filePath) {
        return this;
    };

    /**
     * Sets the model type, like prism or jani.
     * 
     * @param modelType
     *            the model type
     * @return this model checker, for chaining methods invocations
     */
    default ModelChecker setModelType(String modelType) {
        return this;
    };

    /**
     * Sets the quantitative property formula to verify. It must be a valid formula
     * supported by the actual model checker.
     * 
     * @param propertyFormula
     *            the property formula
     * @return this model checker, for chaining methods invocations
     */
    default ModelChecker setPropertyFormula(String propertyFormula) {
        return this;
    };

    /**
     * Sets the constants of the models.
     * 
     * @param constants
     *            the list of constants
     * @return this model checker, for chaining methods invocations
     */
    default ModelChecker setConstants(List<Constant> constants) {
        return this;
    };

    /**
     * Additional options to be passed to the model checker.
     * 
     * @param options
     *            the list of options
     * @return this model checker, for chaining methods invocations
     */
    default ModelChecker setOptions(List<String> options) {
        return this;
    };
    
    /**
     * Provides the range of the values computed by the model checker on the provided
     * constants, or {@code null} if not computed
     * 
     * @return an interval about the range of the values computed by the model checker
     */
    default Range getRange() {
        return null;
    };

}
