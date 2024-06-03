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

package pacpma.lp.solver.matlab;

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
 * Class to use Matlab as an external LP solver.
 * 
 * @author Andrea Turrini
 *
 */
public class MatlabTool implements LPSolver {
    private final static LogEngine logEngine = OptionsPacPMA.getLogEngineInstance();
    
    private final static String FIELD_SEPARATOR = ":";
    private final static String ENTRY_SEPARATOR = ",";
    
    private final static String LOAD_MATRIX_A_OPEN = "A = readmatrix('";
    private final static String LOAD_MATRIX_A_CLOSE = "');\n";
    
    private final static String LOAD_VECTOR_B_OPEN = "b = readmatrix('";
    private final static String LOAD_VECTOR_B_CLOSE = "');\n";
    
    private final static String CONFIGURATION = "format long;\n";
    private final static String MYFUN = "function str = myfun(vec)\n"
            + "\tstr = sprintf('" + ENTRY_SEPARATOR + "%.20f',vec);\n"
            + "\tstr = str(2:end);\n"
            + "end\n";
    private final static String OPTIONS = "options = optimoptions('linprog');\n"
            + "options = optimoptions(options,'Display', 'off');\n"
            + "options = optimoptions(options,'OptimalityTolerance', 1e-9);\n"
            + "options = optimoptions(options,'Algorithm', 'dual-simplex');\n";
//            + "options = optimoptions(options,'Algorithm', 'interior-point');\n";
//            + "options = optimoptions(options,'Algorithm', 'interior-point-legacy');\n";
    private final static String CALL = "[opt_var, fval, exitflag, output] = linprog (c, A, b, [], [], lb, ub, options);\n";
    private final static String EXITFLAG = "fprintf('exitflag:%d\\n', exitflag);\n";
    private final static String OPT_VARIABLES = "fprintf('opt_var:%s\\n', myfun(opt_var));\n";

    private LPSolverExecutionStep solverExpectedStep = LPSolverExecutionStep.SET_VARIABLES;
    
    private LPVariable[] lpvariables = null;
    
    private StringBuilder lowerbound = null;
    private StringBuilder upperbound = null;

    private StringBuilder objectiveFunction = null;
    
    private StringBuilder matrixA = null;
    private StringBuilder vectorB = null;
    
    private BigDecimal lambdaValue = null;
    
    @Override
    public void setVariables(List<LPVariable> listOfVariables) {
        logEngine.log(LogEngine.LEVEL_INFO, "MatlabTool: setting variables");
        assert solverExpectedStep == LPSolverExecutionStep.SET_VARIABLES;
        solverExpectedStep = LPSolverExecutionStep.SET_OBJECTIVE_FUNCTION;
        
        lpvariables = listOfVariables.toArray(new LPVariable[0]);
        
        lowerbound = new StringBuilder("lb = [");
        upperbound = new StringBuilder("ub = [");
        int nvariables = lpvariables.length;
        // variable 0 is lambda; we manage it explicitly
        lowerbound.append("0");
        if (OptionsPacPMA.isLambdaUnbounded()) {
            upperbound.append("Inf");
        } else {
            upperbound.append(OptionsPacPMA.getLambda().toPlainString());
        }
        // all other variables are unbounded
        for (int i = 1; i < nvariables; i++) {
            lowerbound.append(";-Inf");
            upperbound.append(";Inf");
        }
        lowerbound.append("];\n");
        upperbound.append("];\n");
        logEngine.log(LogEngine.LEVEL_INFO, "MatlabTool: setting variables done");
    }

    @Override
    public void setObjectiveFunction(OptimizationDirection direction, Map<LPVariable, BigDecimal> terms) {
        logEngine.log(LogEngine.LEVEL_INFO, "MatlabTool: setting objective");
        assert solverExpectedStep == LPSolverExecutionStep.SET_OBJECTIVE_FUNCTION;
        solverExpectedStep = LPSolverExecutionStep.ADD_CONSTRAINTS;
        
        switch (direction) {
        case MIN:
            objectiveFunction = new StringBuilder("c = [");
            break;
        case MAX:
            objectiveFunction = new StringBuilder("c = -[");
            break;
        }
        boolean isFirst = true;
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
        logEngine.log(LogEngine.LEVEL_INFO, "MatlabTool: setting objective done");
    }

    @Override
    public void addConstraint(Map<LPVariable, BigDecimal> terms, ConstraintComparison comparison, BigDecimal bound) {
        assert solverExpectedStep == LPSolverExecutionStep.ADD_CONSTRAINTS;
        
        BigDecimal factor = OptionsPacPMA.getLPSolverFactor();
        
        if (comparison == ConstraintComparison.GE) {
            factor = factor.negate();
        }

        if (matrixA == null) {
            matrixA = new StringBuilder();
            vectorB = new StringBuilder();
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
    }

    @Override
    public Map<LPVariable, BigDecimal> solve() {
        logEngine.log(LogEngine.LEVEL_INFO, "MatlabTool: starting solving problem");
        assert solverExpectedStep == LPSolverExecutionStep.ADD_CONSTRAINTS;
        solverExpectedStep = LPSolverExecutionStep.SOLVED;
        
        StringBuffer lpProblem = new StringBuffer();
        
        logEngine.log(LogEngine.LEVEL_INFO, "MatlabTool: creating LP problem file");

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
        File lpproblemTempFile = null;
        try {
            lpproblemTempFile = File.createTempFile("lpproblem", ".m");
            lpproblemTempFile.deleteOnExit();
        } catch (Exception ioe) {
            return null;
        }

        lpProblem.append(CONFIGURATION)
            .append(objectiveFunction)
            .append(LOAD_MATRIX_A_OPEN)
            .append(lpproblemMatrixATempFile.getAbsolutePath())
            .append(LOAD_MATRIX_A_CLOSE)
            .append(LOAD_VECTOR_B_OPEN)
            .append(lpproblemVectorBTempFile.getAbsolutePath())
            .append(LOAD_VECTOR_B_CLOSE)
            .append(lowerbound)
            .append(upperbound)
            .append(OPTIONS)
            .append(CALL)
            .append(EXITFLAG)
            .append(OPT_VARIABLES)
            .append(MYFUN);
    
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
        logEngine.log(LogEngine.LEVEL_INFO, "MatlabTool: creating LP problem file done");
        
        logEngine.log(LogEngine.LEVEL_INFO, "MatlabTool: calling matlab");
        String fileName = lpproblemTempFile.getName();
        fileName = fileName.substring(0, fileName.lastIndexOf('.'));
        
        List<String> command = new LinkedList<>();
        command.add("matlab");
        command.add("-nodisplay");
        command.add("-nojvm");
        command.add("-sd");
        command.add("/tmp");
        command.add("-batch");
        command.add(fileName);
        
        List<String> matlabOutput = new ToolRunner(command).run();
        logEngine.log(LogEngine.LEVEL_INFO, "MatlabTool: calling matlab done");
        
        // first line is "exitflag:number"
        if (!"1".equals(matlabOutput.get(0).split(FIELD_SEPARATOR)[1])) {
            // some error occurred during the LP problem solution
            return null;
        }
        
        logEngine.log(LogEngine.LEVEL_INFO, "MatlabTool: collecting output values");
        // second line is "opt_var:value,value,..."
        Map<LPVariable, BigDecimal> results = new HashMap<>();

        String[] values = matlabOutput.get(1).split(FIELD_SEPARATOR)[1].split(ENTRY_SEPARATOR);
        for (int i = 0; i < lpvariables.length; i++) {
            results.put(lpvariables[i], zeroByPrecision(new BigDecimal(values[i])));
        }

        lambdaValue = results.get(lpvariables[0]);
        logEngine.log(LogEngine.LEVEL_INFO, "MatlabTool: collecting output values done");

        logEngine.log(LogEngine.LEVEL_INFO, "MatlabTool: solving problem done");
        return results;
    }

    @Override
    public BigDecimal getLambdaValue() {
        return lambdaValue;
    }
}
