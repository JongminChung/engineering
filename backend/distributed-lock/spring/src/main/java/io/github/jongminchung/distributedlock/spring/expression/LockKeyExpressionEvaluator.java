package io.github.jongminchung.distributedlock.spring.expression;

import java.lang.reflect.Method;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class LockKeyExpressionEvaluator {
    private final ExpressionParser parser = new SpelExpressionParser();

    public String evaluate(String expression, Method method, Object[] args) {
        if (expression == null || expression.isBlank()) {
            return null;
        }

        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("method", method);
        context.setVariable("args", args);

        return parser.parseExpression(expression).getValue(context, String.class);
    }
}
