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
import java.math.RoundingMode;
import java.util.Map;

import pacpma.algebra.Variable;

/**
 * @author Andrea Turrini
 *
 */
public class ExpressionCeil implements Expression {
    private final Expression inner;
    
    public ExpressionCeil(Expression inner) {
        this.inner = inner;
    }

    @Override
    public BigDecimal evaluate(Map<Variable, BigDecimal> values) {
        return inner.evaluate(values).setScale(0, RoundingMode.CEILING);
    }

    @Override
    public String getLatexExpression() {
        return "\\lceil" + inner.getLatexExpression() + "\\rceil";
    }

    @Override
    public String getMathExpression() {
        return "⌈" + inner.getMathExpression() + "⌉";
    }

    @Override
    public String getMatlabExpression() {
        return "ceil(" + inner.getMatlabExpression() + ")";
    }

}
