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

import pacpma.algebra.Constant;
import pacpma.algebra.Variable;
import pacpma.options.OptionsPacPMA;

/**
 * @author Andrea Turrini
 *
 */
public class ExpressionVariable implements Expression {
    private final String identifier;
    private final BigDecimal constantValue;
    private final Variable variable;
    
    public ExpressionVariable(String identifier) {
        this.identifier = identifier;
        
        Variable var = null;
        for (Variable v : Variable.getVariables()) {
            if (v.getName().equals(identifier)) {
                var = v;
                break;
            }
        }
        variable = var;
        
        BigDecimal cv = null;
        for (Constant c : OptionsPacPMA.getConstants()) {
            if (c.getName().equals(identifier)) {
                cv = new BigDecimal(c.getValue());
                break;
            }
        }
        constantValue = cv;
    }

    @Override
    public BigDecimal evaluate(Map<Variable, BigDecimal> values) {
        if (constantValue != null) {
            return constantValue;
        } else {
            if (variable != null) {
                BigDecimal value = values.get(variable);
                if (value != null) {
                    return value;
                }
            }
        }
        throw new UnsupportedOperationException("Undefined identifier " + identifier);
    }

    @Override
    public String getLatexExpression() {
        return "\\mathit{" + identifier + "}";
    }

    @Override
    public String getMathExpression() {
        return identifier;
    }

    @Override
    public String getMatlabExpression() {
        return identifier;
    }

}
