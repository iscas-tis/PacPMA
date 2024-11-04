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

package pacpma.lp.solver.octave;

import static pacpma.util.Util.zeroByPrecision;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import pacpma.externaltool.ToolRunner;
import pacpma.log.LogEngine;
import pacpma.lp.ConstraintComparison;
import pacpma.lp.LPVariable;
import pacpma.lp.OptimizationDirection;
import pacpma.lp.solver.LPSolver;
import pacpma.lp.solver.LPSolverExecutionStep;
import pacpma.options.OptionsPacPMA;

/**
 * Class to use Octave as an external LP solver.
 * 
 * @author Andrea Turrini
 *
 */
public class OctaveTool implements LPSolver {
    private final static LogEngine logEngine = OptionsPacPMA.getLogEngineInstance();
    
    private final static String FIELD_SEPARATOR = ":";
    private final static String ENTRY_SEPARATOR = ",";
    
    private final static String LOAD_MATRIX_A_OPEN = "A = load('";
    private final static String LOAD_MATRIX_A_CLOSE = "');\n";
    
    private final static String LOAD_VECTOR_B_OPEN = "b = load('";
    private final static String LOAD_VECTOR_B_CLOSE = "');\n";
    
    private final static String LOAD_CTYPE_OPEN = "ctype = fileread('";
    private final static String LOAD_CTYPE_CLOSE = "');\n";
    
    private final static String CONFIGURATION = "format long;\n"; 
    private final static String MYFUN = "function str = myfun(vec)\n"
            + "\tstr = sprintf('" + ENTRY_SEPARATOR + "%.20f',vec);\n"
            + "\tstr = str(2:end);\n"
            + "end\n";
    private final static String PARAMS = "param.msglev=0;\nparam.lpsolver=1;\n";
    private final static String CALL = "[opt_var, opt_value, errnum, extra] = glpk (c, A, b, lb, ub, ctype, vartype, sense, param);\n";
    private final static String ERRNUM = "fprintf('errnum:%d\\n', errnum);\n";
    private final static String STATUS = "fprintf('extra.status:%d\\n', extra.status);\n";
    private final static String OPT_VARIABLES = "fprintf('opt_var:%s\\n', myfun(opt_var));\n";

    private LPSolverExecutionStep solverExpectedStep = LPSolverExecutionStep.SET_VARIABLES;
    
    private LPVariable[] lpvariables = null;
    
    private StringBuilder vartype = null;
    private StringBuilder lowerbound = null;
    private StringBuilder upperbound = null;

    private StringBuilder objectiveFunction = null;
    private String sense = null;
    
    private StringBuilder matrixA = null;
    private StringBuilder vectorB = null;
    private StringBuilder ctype = null;
    
    private BigDecimal lambdaValue = null;
    
    @Override
    public void setVariables(List<LPVariable> listOfVariables) {
        logEngine.log(LogEngine.LEVEL_INFO, "OctaveTool: setting variables");
        assert solverExpectedStep == LPSolverExecutionStep.SET_VARIABLES;
        solverExpectedStep = LPSolverExecutionStep.SET_OBJECTIVE_FUNCTION;
        
        lpvariables = listOfVariables.toArray(new LPVariable[0]);
        
        vartype = new StringBuilder("vartype = \"");
        lowerbound = new StringBuilder("lb = [");
        upperbound = new StringBuilder("ub = [");
        int nvariables = lpvariables.length;
        // variable 0 is lambda; we manage it explicitly
        vartype.append('C');
        lowerbound.append("0");
        if (OptionsPacPMA.isLambdaUnbounded()) {
            upperbound.append("Inf");
        } else {
            upperbound.append(OptionsPacPMA.getLambda().toPlainString());
        }
        // all other variables are unbounded
        for (int i = 1; i < nvariables; i++) {
            vartype.append('C');
            lowerbound.append(";-Inf");
            upperbound.append(";Inf");
        }
        vartype.append("\";\n");
        lowerbound.append("];\n");
        upperbound.append("];\n");
        logEngine.log(LogEngine.LEVEL_INFO, "OctaveTool: setting variables done");
    }

    @Override
    public void setObjectiveFunction(OptimizationDirection direction, Map<LPVariable, BigDecimal> terms) {
        logEngine.log(LogEngine.LEVEL_INFO, "OctaveTool: setting objective");
        assert solverExpectedStep == LPSolverExecutionStep.SET_OBJECTIVE_FUNCTION;
        solverExpectedStep = LPSolverExecutionStep.ADD_CONSTRAINTS;
        
        switch (direction) {
        case MIN:
            sense = "sense = 1;\n";
            break;
        case MAX:
            sense = "sense = -1;\n";
            break;
        }
        boolean isFirst = true;
        objectiveFunction = new StringBuilder("c = [");
        for (LPVariable lpvariable : lpvariables) {
            BigDecimal coefficient = terms.get(lpvariable);
            if (coefficient != null) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    objectiveFunction.append(",");
                }
                objectiveFunction.append(coefficient.toPlainString());
            }
        }
        objectiveFunction.append("];\n");
        logEngine.log(LogEngine.LEVEL_INFO, "OctaveTool: setting objective done");
    }

    @Override
    public void addConstraint(Map<LPVariable, BigDecimal> terms, ConstraintComparison comparison, BigDecimal bound) {
        assert solverExpectedStep == LPSolverExecutionStep.ADD_CONSTRAINTS;
        
        BigDecimal factor = OptionsPacPMA.getLPSolverFactor();
        
        if (matrixA == null) {
            matrixA = new StringBuilder();
            vectorB = new StringBuilder();
            ctype = new StringBuilder();
        } else {
            matrixA.append("\n");
            vectorB.append("\n");
        }
        boolean isFirst = true;
        for (LPVariable lpvariable : lpvariables) {
            if (isFirst) {
                isFirst = false;
            } else {
                matrixA.append(',');
            }
            matrixA.append(terms.getOrDefault(lpvariable, BigDecimal.ZERO).multiply(factor).toPlainString());
        }
        vectorB.append(bound.multiply(factor).toPlainString());
        switch(comparison) {
        case GE:
            ctype.append('L');
            break;
        case LE:
            ctype.append('U');
            break;
        }
    }

    @Override
    public Map<LPVariable, BigDecimal> solve() {
        logEngine.log(LogEngine.LEVEL_INFO, "OctaveTool: starting solving problem");
        assert solverExpectedStep == LPSolverExecutionStep.ADD_CONSTRAINTS;
        solverExpectedStep = LPSolverExecutionStep.SOLVED;
        
        logEngine.log(LogEngine.LEVEL_INFO, "OctaveTool: creating LP problem file");
        
        File lpproblemMatrixATempFile = null;
        try {
            lpproblemMatrixATempFile = File.createTempFile("lpproblemMatrixA", ".dat");
            lpproblemMatrixATempFile.deleteOnExit();
        } catch (Exception ioe) {
            return null;
        }
        File lpproblemVectorBTempFile = null;
        try {
            lpproblemVectorBTempFile = File.createTempFile("lpproblemVectorB", ".dat");
            lpproblemVectorBTempFile.deleteOnExit();
        } catch (Exception ioe) {
            return null;
        }
        File lpproblemCtypeTempFile = null;
        try {
            lpproblemCtypeTempFile = File.createTempFile("lpproblemCtype", ".dat");
            lpproblemCtypeTempFile.deleteOnExit();
        } catch (Exception ioe) {
            return null;
        }
        File lpproblemTempFile = null;
        try {
            lpproblemTempFile = File.createTempFile("lpproblem", ".octave");
            lpproblemTempFile.deleteOnExit();
        } catch (Exception ioe) {
            return null;
        }

        StringBuffer lpProblem = new StringBuffer();
        lpProblem.append(CONFIGURATION)
            .append(MYFUN)
            .append(sense)
            .append(objectiveFunction)
            .append(LOAD_MATRIX_A_OPEN)
            .append(lpproblemMatrixATempFile.getAbsolutePath())
            .append(LOAD_MATRIX_A_CLOSE)
            .append(LOAD_VECTOR_B_OPEN)
            .append(lpproblemVectorBTempFile.getAbsolutePath())
            .append(LOAD_VECTOR_B_CLOSE)
            .append(LOAD_CTYPE_OPEN)
            .append(lpproblemCtypeTempFile.getAbsolutePath())
            .append(LOAD_CTYPE_CLOSE)
            .append(vartype)
            .append(lowerbound)
            .append(upperbound)
            .append(PARAMS)
            .append(CALL)
            .append(ERRNUM)
            .append(STATUS)
            .append(OPT_VARIABLES);
    
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(lpproblemTempFile))) {
            bw.write(lpProblem.toString());
        } catch (IOException ioe) {
            return null;
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(lpproblemMatrixATempFile))) {
            bw.write(matrixA.toString());
        } catch (IOException ioe) {
            return null;
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(lpproblemVectorBTempFile))) {
            bw.write(vectorB.toString());
        } catch (IOException ioe) {
            return null;
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(lpproblemCtypeTempFile))) {
            bw.write(ctype.toString());
        } catch (IOException ioe) {
            return null;
        }
        logEngine.log(LogEngine.LEVEL_INFO, "OctaveTool: creating LP problem file done");
        
        logEngine.log(LogEngine.LEVEL_INFO, "OctaveTool: calling octave");
        List<String> command = new LinkedList<>();
        command.add("octave");
        command.add("--no-gui");
        command.add("--no-history");
        command.add("--no-window-system");
        command.add("--silent");
        command.add("--no-line-editing");
        command.add(lpproblemTempFile.getAbsolutePath());
        
        List<String> octaveOutput = new ToolRunner(command).run();
        logEngine.log(LogEngine.LEVEL_INFO, "OctaveTool: calling octave done");

        if (octaveOutput == null) {
            // some error occurred during the LP problem solution
            return null;            
        }
        // first line is "errnum:number"
        if (!"0".equals(octaveOutput.get(0).split(FIELD_SEPARATOR)[1])) {
            // some error occurred during the LP problem solution
            return null;
        }
        
        // second line is "extra.status:number"
        String number = octaveOutput.get(1).split(FIELD_SEPARATOR)[1];
        if (!"2".equals(number) && !"5".equals(number)) {
            // no feasible (optimal, resp.) solution found
            return null;
        }
        
        logEngine.log(LogEngine.LEVEL_INFO, "OctaveTool: collecting output values");
        // third line is "opt_var:value,value,..."
        Map<LPVariable, BigDecimal> results = new HashMap<>();

        String[] values = octaveOutput.get(2).split(FIELD_SEPARATOR)[1].split(ENTRY_SEPARATOR);
        for (int i = 0; i < lpvariables.length; i++) {
            results.put(lpvariables[i], zeroByPrecision(new BigDecimal(values[i])));
        }

        lambdaValue = results.get(lpvariables[0]);
        logEngine.log(LogEngine.LEVEL_INFO, "OctaveTool: collecting output values done");

        logEngine.log(LogEngine.LEVEL_INFO, "OctaveTool: solving problem done");
        return results;
    }

    @Override
    public BigDecimal getLambdaValue() {
        return lambdaValue;
    }
}
