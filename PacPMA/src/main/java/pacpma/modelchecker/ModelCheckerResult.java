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

package pacpma.modelchecker;

import java.math.BigDecimal;

/**
 * @author Andrea Turrini
 *
 */
public class ModelCheckerResult implements Comparable<ModelCheckerResult> {
    private final boolean isInfinite;
    private final BigDecimal result;
    
    public ModelCheckerResult() {
        this(true, null);
    }
    
    public ModelCheckerResult(BigDecimal result) {
        this(false, result);
    }
    
    private ModelCheckerResult(boolean isInfinite, BigDecimal result) {
        this.isInfinite = isInfinite;
        this.result = result;
    }

    /**
     * @return whether the result is infinite
     */
    public boolean isInfinite() {
        return isInfinite;
    }

    /**
     * @return the result, or {@code null} if it is infinite
     */
    public BigDecimal getResult() {
        return result;
    }

    @Override
    public int compareTo(ModelCheckerResult o) {
        if (isInfinite) {
            if (o.isInfinite) {
                return 0;
            } else {
                return 1;
            }
        } else {
            if (o.isInfinite) {
                return -1;
            } else {
                return result.compareTo(o.result);
            }
        }
    }
    
    @Override
    public String toString() {
        if (isInfinite) {
            return "∞";
        } else {
            return result.toString();
        }
    }
}
