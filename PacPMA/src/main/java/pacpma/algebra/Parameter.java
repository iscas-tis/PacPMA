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

package pacpma.algebra;

import java.math.BigDecimal;

/**
 * A parameter, as given from command line.
 * 
 * @author Andrea Turrini
 *
 */
public class Parameter {
    private final String name;
    private final BigDecimal lowerbound;
    private final BigDecimal upperbound;

    /**
     * @param name
     *            the name of the parameter; it must be a non-empty string
     * @param lowerbound
     *            the lower bound for the parameter
     * @param upperbound
     *            the upper bound for the parameter; it must be strictly larger than
     *            the lower bound
     */
    public Parameter(String name, BigDecimal lowerbound, BigDecimal upperbound) {
        assert name != null;
        assert name.length() > 0;
        assert lowerbound.compareTo(upperbound) < 0;

        this.name = name;
        this.lowerbound = lowerbound;
        this.upperbound = upperbound;
    }

    /**
     * @return the name of this parameter
     */
    public String getName() {
        return name;
    }

    /**
     * @return the lowerbound for this parameter
     */
    public BigDecimal getLowerbound() {
        return lowerbound;
    }

    /**
     * @return the upperbound for this parameter
     */
    public BigDecimal getUpperbound() {
        return upperbound;
    }

    @Override
    public String toString() {
        return name + "=[" + lowerbound + ";" + upperbound + "]";
    }
}
