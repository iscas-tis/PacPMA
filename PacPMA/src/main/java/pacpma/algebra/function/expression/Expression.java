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

package pacpma.algebra.function.expression;

import java.math.BigDecimal;
import java.util.Map;

import pacpma.algebra.Variable;

/**
 * @author Andrea Turrini
 *
 */
public interface Expression {
    /**
     * Evaluates the expression with respect to the given value of the 
     * variables.
     * 
     * @param values
     *            the values of the variables
     * @return the evaluated expression
     */
    BigDecimal evaluate(Map<Variable, BigDecimal> values);
    
    /**
     * Generates a LaTeX expression for this expression, with coefficients for 
     * the terms taken from {@code coefficientValues}.
     * 
     * @return a LaTeX expression representing this expression
     */
    String getLatexExpression();
    
    /**
     * Generates a mathematical expression for this expression, with coefficients 
     * for the terms taken from {@code coefficientValues}.
     * 
     * @return a mathematical expression representing this expression
     */
    String getMathExpression();
    
    /**
     * Generates a Matlab expression for this expression, with coefficients 
     * for the terms taken from {@code coefficientValues}.
     * 
     * @return a MATLAB expression representing this expression
     */
    String getMatlabExpression();
}
