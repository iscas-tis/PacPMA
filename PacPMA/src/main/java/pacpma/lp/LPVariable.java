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

package pacpma.lp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class representing the variables to be used in the LP problem.
 * The reserved variable "lambda" is always present.
 * 
 * @author Andrea Turrini
 *
 */
public class LPVariable {
    private static final Map<String, LPVariable> variables = new HashMap<>();
    private static final List<LPVariable> orderedVariables = new ArrayList<>();

    private final String variableName;

    private LPVariable(String variable) {
        this.variableName = variable;
    }

    public static void setVariables(List<String> variableNames) {
        for (String variableName : variableNames) {
            LPVariable variable = new LPVariable(variableName);
            variables.put(variableName, variable);
            orderedVariables.add(variable);
        }
    }

    public static List<LPVariable> getVariables() {
        return orderedVariables;
    }
    
    public static LPVariable asVariable(String variableName) {
        return variables.get(variableName);
    }

    public String getName() {
        return variableName;
    }

    @Override
    public String toString() {
        return variableName;
    }

    @Override
    public int hashCode() {
        return variableName.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof LPVariable) {
            return variableName.equals(((LPVariable) other).variableName);
        }
        return false;
    }
}
