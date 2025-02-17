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
import java.math.MathContext;
import java.util.Map;

import pacpma.algebra.Variable;
import pacpma.options.OptionsPacPMA;

/**
 * @author Andrea Turrini
 *
 */
public class ExpressionSqrt implements Expression {
    private final Expression inner;
    
    public ExpressionSqrt(Expression inner) {
        this.inner = inner;
    }

    @Override
    public BigDecimal evaluate(Map<Variable, BigDecimal> values) {
        MathContext mc = new MathContext(OptionsPacPMA.getExpressionPrecision());
        return inner.evaluate(values).sqrt(mc);
    }

    @Override
    public String getLatexExpression() {
        return "\\sqrt{" + inner.getLatexExpression() + "}";
    }

    @Override
    public String getMathExpression() {
        return "sqrt(" + inner.getMathExpression() + ")";
    }

    @Override
    public String getMatlabExpression() {
        return "(" + inner.getMatlabExpression() + ").^0.5";
    }

}
