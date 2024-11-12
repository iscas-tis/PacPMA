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

package pacpma.algebra.function.expression;

import java.math.BigDecimal;
import java.util.Map;

import pacpma.algebra.Variable;

/**
 * @author Andrea Turrini
 *
 */
public class ExpressionTerm implements Expression {
    private static int counter = 0;

    private final Expression expression;
    private final String identifier;
    
    /**
     * Constructs a term for the given expression, together with a unique 
     * identifier.
     * 
     * @param expression
     *      the expression for this expression
     */
    public ExpressionTerm(Expression expression) {
        this.expression = expression;
        this.identifier = "term_" + counter;
        counter++;
    }
    
    /**
     * Returns the unique identifier associated with this term.
     * 
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public BigDecimal evaluate(Map<Variable, BigDecimal> values) {
        return expression.evaluate(values);
    }

    @Override
    public String getLatexExpression() {
        return expression.getLatexExpression();
    }

    @Override
    public String getMathExpression() {
        return expression.getMathExpression();
    }

    @Override
    public String getMatlabExpression() {
        return expression.getMatlabExpression();
    }

}
