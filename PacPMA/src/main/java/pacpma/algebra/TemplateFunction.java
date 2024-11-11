/****************************************************************************

    PACModel - a PAC-based model checker
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

package pacpma.algebra;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author Andrea Turrini
 *
 */
public interface TemplateFunction {
    
    /**
     * States whether the template function is a valid template function, like 
     * using only defined variables and expressions.
     * 
     * @return whether the function is valid
     */
    default boolean isValid() {
        return true;
    }
    
    /**
     * Provides the list of coefficients of all terms in this template function.
     * 
     * @return the list of coefficients
     */
    List<String> getCoefficients();
    
    /**
     * Evaluates the terms in this template function with respect to the given 
     * value of the variables.
     * 
     * @param values
     *            the values of the variables
     * @return a {@link Map} assigning to each term (identified by its coefficient 
     * as by {@link TemplateFunction#getCoefficients()}) its evaluation
     */
    Map<String, BigDecimal> evaluate(Map<Variable, BigDecimal> values);
    
    /**
     * Generates a LaTeX expression for this template function, with 
     * coefficients for the terms taken from {@code coefficientValues}.
     * 
     * @param coefficientValues
     *            the values associated to the coefficients
     *            {@link TemplateFunction#getCoefficient()}
     * @return a LaTeX expression representing this template function
     */
    String getLatexExpression(Map<String, BigDecimal> coefficientValues);
    
    /**
     * Generates a mathematical expression for this template function, with 
     * coefficients for the terms taken from {@code coefficientValues}.
     * 
     * @param coefficientValues
     *            the values associated to the coefficients
     *            {@link TemplateFunction#getCoefficient()}
     * @return a LaTeX expression representing this template function
     */
    String getMathExpression(Map<String, BigDecimal> coefficientValues);
    
    /**
     * Generates a MATLAB expression for this template function, with 
     * coefficients for the terms taken from {@code coefficientValues}.
     * 
     * @param coefficientValues
     *            the values associated to the coefficients
     *            {@link TemplateFunction#getCoefficient()}
     * @return a LaTeX expression representing this template function
     */
    String getMatlabExpression(Map<String, BigDecimal> coefficientValues);
}
