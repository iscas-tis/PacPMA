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
public class ExpressionNumber implements Expression {
    private final BigDecimal numberValue;
    private final String number;
    
    public ExpressionNumber(String number) {
        this.number = number;
        numberValue = new BigDecimal(number);
    }

    @Override
    public BigDecimal evaluate(Map<Variable, BigDecimal> values) {
        return numberValue;
    }

    @Override
    public String getLatexExpression() {
        return number;
    }

    @Override
    public String getMathExpression() {
        return number;
    }

    @Override
    public String getMatlabExpression() {
        return number;
    }

}
