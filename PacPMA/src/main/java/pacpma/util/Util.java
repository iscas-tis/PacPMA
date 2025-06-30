/****************************************************************************

    PacPMA - the PAC-based Parametric Model Analyzer
    Copyright (C) 2023

    This program is free software: you can redistribute it and/or modify
    it under the terms of the <a href="https://creativecommons.org/licenses/by-sa/4.0/">Creative-Commons BY-SA 4.0 license</a>.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 


 *****************************************************************************/

package pacpma.util;

import java.math.BigDecimal;
import java.util.List;

import pacpma.algebra.Constant;
import pacpma.options.OptionsPacPMA;

/**
 * Class providing utility methods.
 * 
 * @author Andrea Turrini
 *
 */
public class Util {

    /**
     * Checks whether the given value should be treated as zero since it is below
     * the desired precision.
     * 
     * @param value
     *            the value to check for "zeroness"
     * @return {@code value} if |value| >
     *         {@link OptionsPacPMA#getLPSolverPrecision()},
     *         {@link BigDecimal#ZERO} otherwise
     */
    public static BigDecimal zeroByPrecision(BigDecimal value) {
        if (value.abs().compareTo(OptionsPacPMA.getLPSolverPrecision()) <= 0) {
            return BigDecimal.ZERO;
        } else {
            return value;
        }
    }
    
    /**
     * Computes the maximum number of monomials of degree at most {@code degree}
     * occurring in the polynomial defined over {@code variables} variables. <br/>
     * Method adapted from
     * https://codereview.stackexchange.com/questions/204574/binomial-coefficient-in-java
     * 
     * @param variables
     *            the number of variables in the polynomial
     * @param degree
     *            the degree of the polynomial
     * @return the maximum number of coefficients/monomials in the polynomial
     */
    public static long numberCoefficients(int variables, int degree) {
        if (variables == 1) {
            return degree + 1;
        }
        if (degree == 1) {
            return variables + 1;
        }
        if (degree == 2) {
            if (variables % 2 == 0) {
                return (variables + 1) * (variables / 2 + 1);
            } else {
                return (variables + 1) / 2 * (variables + 2);
            }
        }

        long MOD = 1000000007L; // a prime number large enough for our purposes
        long bin; // binomial coefficient
        long nom; // nominator
        long den; // denominator

        long n = degree + variables;
        long k = Math.min(variables, degree);

        bin = 1;
        nom = n;
        den = 1;

        while (den <= k) {
            bin = Math.floorMod(bin * nom, MOD);
            bin = Math.floorMod(bin * modInverse(den, MOD), MOD);
            nom -= 1;
            den += 1;
        }

        return Math.floorMod(bin, MOD);
    }

    /**
     * The modular inverse as described in
     * https://en.wikipedia.org/wiki/Extended_Euclidean_algorithm <br/>
     * Method taken from
     * https://codereview.stackexchange.com/questions/204574/binomial-coefficient-in-java
     * 
     * @param n
     *            A positive number.
     * @param p
     *            A prime number.
     * @return The modular inverse
     */
    private static long modInverse(long n, long p) {
        // i-1 i
        long R, r; // remainders
        long S, s; // 1st Bézout coefficients
        long T, t; // 2nd Bézout coefficients

        assert n > 0 && p > 0;

        R = n;
        r = p;

        S = 1;
        s = 0;

        T = 0;
        t = 1;

        while (r > 0) {
            long _q = R / r;
            long _r = R - _q * r;
            R = r;
            r = _r;
            long _s = S - _q * s;
            S = s;
            s = _s;
            long _t = T - _q * t;
            T = t;
            t = _t;
        }

        assert R == 1; // gcd == 1 since p is assumed to be prime

        return Math.floorMod(S, p);
    }

    /**
     * Computes the minimum number of samples, according to the PAC theorem, to
     * ensure that the obtained samples provide statistical guarantees with respect
     * to the given {@code epsilon} and {@code eta}.
     * 
     * @param epsilon
     *            the error rate
     * @param eta
     *            the significance level
     * @param coefficients
     *            the number of coefficients in the polynomial
     * @return the minimum number of samples according to the PAC theorem
     */
    public static int minimumNumberSamples(double epsilon, double eta, long coefficients) {
        assert epsilon > 0;
        assert eta > 0;
        assert coefficients >= 0;

        return (int) Math.ceil((2 / epsilon) * (Math.log(1 / eta) + coefficients));
    }
    
    
    /**
     * Formats the given constants as a pretty string to be printed
     * @param constants the constants to be printed
     * @return the formatted list of constants
     */
    public static String formatConstants(List<Constant> constants) {
        final StringBuilder sb = new StringBuilder();
        constants.forEach(c -> appendConstant(sb, c, ", "));
        sb.insert(0, "[");
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Appends the given constant to the list of constants in the given string buffer
     * @param sb the string buffer to fill
     * @param c the constant to add
     */
    public static void appendConstant(StringBuilder sb, Constant c) {
        appendConstant(sb, c, ",");
    }
    
    /**
     * Appends the given constant to the list of constants in the given string buffer
     * @param sb the string buffer to fill
     * @param c the constant to add
     * @param separator the separator to use
     */
    public static void appendConstant(StringBuilder sb, Constant c, String separator) {
        if (sb.length() > 0) {
            sb.append(separator);
        }
        sb.append(c.getName()).append("=").append(c.getValue());
    }
}
