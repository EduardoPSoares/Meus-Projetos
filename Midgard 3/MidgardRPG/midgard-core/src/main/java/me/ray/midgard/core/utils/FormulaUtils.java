package me.ray.midgard.core.utils;

import java.util.Map;

public class FormulaUtils {

    /**
     * Evaluates a mathematical expression string with variables.
     * Supported operators: +, -, *, /, ^, %
     * Example: evaluate("base * (1 + str / 100)", Map.of("base", 10.0, "str", 50.0))
     */
    public static double evaluate(String expression, Map<String, Double> variables) {
        // Ordena variáveis por comprimento decrescente para evitar colisão de substrings
        // Ex: "str" substituiria dentro de "strength" se processado primeiro
        java.util.List<Map.Entry<String, Double>> sorted = new java.util.ArrayList<>(variables.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
        for (Map.Entry<String, Double> entry : sorted) {
            expression = expression.replace(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return eval(expression);
    }

    public static double eval(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') { nextChar(); }
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) { throw new RuntimeException("Unexpected: " + (char)ch); }
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if (eat('+')) { x += parseTerm(); } // addition
                    else if (eat('-')) { x -= parseTerm(); } // subtraction
                    else { return x; }
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if (eat('*')) { x *= parseFactor(); }
                    else if (eat('/')) {
                        double divisor = parseFactor();
                        if (divisor == 0) { throw new ArithmeticException("Division by zero in expression"); }
                        x /= divisor;
                    } else if (eat('%')) {
                        double divisor = parseFactor();
                        if (divisor == 0) { throw new ArithmeticException("Modulus by zero in expression"); }
                        x %= divisor;
                    }
                    else { return x; }
                }
            }

            double parseFactor() {
                if (eat('+')) { return parseFactor(); } // unary plus
                if (eat('-')) { return -parseFactor(); } // unary minus

                double x;
                int startPos = this.pos;
                if (eat('(')) { // parentheses
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.') { nextChar(); }
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if (ch >= 'a' && ch <= 'z') { // functions
                    while (ch >= 'a' && ch <= 'z') { nextChar(); }
                    String func = str.substring(startPos, this.pos);
                    x = parseFactor();
                    if (func.equals("sqrt")) { x = Math.sqrt(x); }
                    else if (func.equals("sin")) { x = Math.sin(Math.toRadians(x)); }
                    else if (func.equals("cos")) { x = Math.cos(Math.toRadians(x)); }
                    else if (func.equals("tan")) { x = Math.tan(Math.toRadians(x)); }
                    else { throw new RuntimeException("Unknown function: " + func); }
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }

                if (eat('^')) { x = Math.pow(x, parseFactor()); } // exponentiation

                return x;
            }
        }.parse();
    }
}
