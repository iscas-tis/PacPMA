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

package pacpma.lp.solver.lpsolve;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;

import pacpma.log.Logger;
import pacpma.lp.ConstraintComparison;
import pacpma.lp.LPVariable;
import pacpma.lp.OptimizationDirection;
import pacpma.lp.solver.LPSolver;
import pacpma.lp.solver.LPSolverExecutionStep;
import pacpma.options.OptionsPacPMA;

import static pacpma.util.Util.zeroByPrecision;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to use the system library lp_solve as an external LP solver.
 * 
 * @author Andrea Turrini
 *
 */
public class LPSolveLibrary implements LPSolver {
    /** Encoding constant {@code false} of lp_solve. */
    private final static byte FALSE = 0;
    /** Encoding constant {@code true} of lp_solve. */
    private final static byte TRUE = 1;
    /** Encoding less-or-equal in lp_solve. */
    private final static int LE = 1;
    /** Encoding greater-or-equal in lp_solve. */    
    private final static int GE = 2;

    /* return codes of lp_solve */

    private final static int OPTIMAL = 0;
    private final static int SUBOPTIMAL = 1;
    private final static int PRESOLVED = 9;
    private final static int FEASFOUND = 12;

    /* reporting details mode of lp_solve */

    private final static int NEUTRAL = 0; //the most quiet one...

    private final static class LpSolve {

        static native Pointer make_lp(int rows, int columns);

        static native void delete_lp(Pointer lp);
        
        static native byte set_add_rowmode(Pointer lp, byte turnon);

        static native byte add_constraint(Pointer lp, double[] row, int constr_type, double rh);

        static native int solve(Pointer lp);
        
        static native byte set_col_name(Pointer lp, int column, String name);
        
        static native double get_var_primalresult(Pointer lp, int index);
        
        static native int get_Nrows(Pointer lp);

        static native byte set_obj_fn(Pointer lp, double[] row);

        static native void set_maxim(Pointer lp);

        static native void set_minim(Pointer lp);

        static native void set_debug(Pointer lp, byte debug);
        
        static native void set_verbose(Pointer lp, int verbose);

        static native byte set_bounds(Pointer lp, int column, double lower, double upper);

        static native byte set_lowbo(Pointer lp, int column, double lower);

        static native byte set_unbounded(Pointer lp, int column);

        static {
            registerLibrary(LpSolve.class, "lpsolve55");
        }

        /**
         * Register a native library to an given class using JNA. 
         * None of the arguments to this function may be {@code null}.
         * 
         * @param handler class to which to map the library to.
         * @param libraryName base name of the native library
         */
        public static void registerLibrary(Class<?> handler, String libraryName) {
            assert handler != null;
            assert libraryName != null;
            Map<String,Object> options = new HashMap<>();
            /* The following lines fix a problem occurring otherwise in Linux.
             * Library.OPTION_OPEN_FLAGS must *not* be set on Windows, though,
             * because it breaks things there.
             */
            if (Platform.isLinux()) {
                options.put(Library.OPTION_OPEN_FLAGS, 2);
            }
            NativeLibrary library = NativeLibrary.getInstance(libraryName, options);
            Native.register(handler, library);
        }
    }
    
    private LPVariable[] lpvariables = null;
    private Pointer lpsolveInstance = null;
    private LPSolverExecutionStep solverExpectedStep = LPSolverExecutionStep.SET_VARIABLES;
    
    private BigDecimal lambdaValue = null;

    @Override
    public void setVariables(List<LPVariable> listOfVariables) {
        Logger.log(Logger.LEVEL_INFO, "LPSolveLibrary: setting variables");
        assert solverExpectedStep == LPSolverExecutionStep.SET_VARIABLES;
        solverExpectedStep = LPSolverExecutionStep.SET_OBJECTIVE_FUNCTION;
        
        lpvariables = listOfVariables.toArray(new LPVariable[0]);
        lpsolveInstance = LpSolve.make_lp(0, lpvariables.length + 1);
        
        LpSolve.set_verbose(lpsolveInstance, NEUTRAL);
        LpSolve.set_debug(lpsolveInstance, FALSE);
        
        //adding names to variables, needed to retrieve their value after solving the problem
        for (int var = 0; var < lpvariables.length; var++) {
            LpSolve.set_col_name(lpsolveInstance, var + 1, lpvariables[var].getName());
        }
        
        //setting all variables as unbounded, except for lambda (variable 1)
        if (OptionsPacPMA.isLambdaUnbounded()) {
            LpSolve.set_lowbo(lpsolveInstance, 1, 0.0);
        } else {
            LpSolve.set_bounds(lpsolveInstance, 1, 0.0, OptionsPacPMA.getLambda().doubleValue());
        }
        for (int var = 2; var <= lpvariables.length; var++) {
            LpSolve.set_unbounded(lpsolveInstance, var);
        }
        
        LpSolve.set_add_rowmode(lpsolveInstance, TRUE);
        Logger.log(Logger.LEVEL_INFO, "LPSolveLibrary: setting variables done");
    }

    @Override
    public void setObjectiveFunction(OptimizationDirection direction, Map<LPVariable, BigDecimal> terms) {
        Logger.log(Logger.LEVEL_INFO, "LPSolveLibrary: setting objective");
        assert solverExpectedStep == LPSolverExecutionStep.SET_OBJECTIVE_FUNCTION;
        solverExpectedStep = LPSolverExecutionStep.ADD_CONSTRAINTS;
        
        double[] obj = new double[lpvariables.length + 1];
        for (int var = 0; var < lpvariables.length; var++) {
            obj[var + 1] = terms.get(lpvariables[var]).doubleValue();
        }
        LpSolve.set_obj_fn(lpsolveInstance, obj);
        switch(direction) {
        case MAX: 
            LpSolve.set_maxim(lpsolveInstance);
            break;
        case MIN: 
            LpSolve.set_minim(lpsolveInstance);
            break;
        }
        Logger.log(Logger.LEVEL_INFO, "LPSolveLibrary: setting objective done");
    }

    @Override
    public void addConstraint(Map<LPVariable, BigDecimal> terms, ConstraintComparison comparison, BigDecimal bound) {
        assert solverExpectedStep == LPSolverExecutionStep.ADD_CONSTRAINTS;

        BigDecimal factor = OptionsPacPMA.getLPSolverFactor();
        
        double[] row = new double[lpvariables.length + 1];
        for (int var = 0; var < lpvariables.length; var++) {
            row[var + 1] = terms.get(lpvariables[var]).multiply(factor).doubleValue();
        }
        LpSolve.add_constraint(lpsolveInstance, row, getBoundDirection(comparison), bound.multiply(factor).doubleValue());
    }

    @Override
    public Map<LPVariable, BigDecimal> solve() {
        Logger.log(Logger.LEVEL_INFO, "LPSolveLibrary: starting solving problem");
        assert solverExpectedStep == LPSolverExecutionStep.ADD_CONSTRAINTS;
        solverExpectedStep = LPSolverExecutionStep.SOLVED;
       
        LpSolve.set_add_rowmode(lpsolveInstance, FALSE);
        
        Logger.log(Logger.LEVEL_INFO, "LPSolveLibrary: calling lpsolve");
        //solving the problem
        int solveStatus = LpSolve.solve(lpsolveInstance);
        Logger.log(Logger.LEVEL_INFO, "LPSolveLibrary: calling lpsolve done");
        
        Logger.log(Logger.LEVEL_INFO, "LPSolveLibrary: collecting output values");
        Map<LPVariable, BigDecimal> results = null;
        //collecting results
        switch (solveStatus) {
        case OPTIMAL:
        case SUBOPTIMAL:
        case PRESOLVED:
        case FEASFOUND:
            results = new HashMap<>();
            int baseVariable = 1 + LpSolve.get_Nrows(lpsolveInstance);
            for (int variable = 0; variable < lpvariables.length; variable++) {
                results.put(lpvariables[variable], zeroByPrecision(new BigDecimal(LpSolve.get_var_primalresult(lpsolveInstance, baseVariable + variable))));
            }
            lambdaValue = results.get(lpvariables[0]);
            break;
        }
        Logger.log(Logger.LEVEL_INFO, "LPSolveLibrary: collecting output values done");
        
        LpSolve.delete_lp(lpsolveInstance);
        
        Logger.log(Logger.LEVEL_INFO, "LPSolveLibrary: solving problem done");
        return results;
    }

    @Override
    public BigDecimal getLambdaValue() {
        return lambdaValue;
    }
    
    private int getBoundDirection(ConstraintComparison comparison) {
        switch (comparison){
        case GE: return GE;
        case LE: return LE;
        }
        return 0;
    }

}
