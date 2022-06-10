package io.harness.notification.evaluator;

import io.harness.engine.expressions.functors.SecretFunctor;
import io.harness.expression.EngineExpressionEvaluator;

public class SecretExpressionEvaluator extends EngineExpressionEvaluator {
  private final long expressionFunctorToken;

  public SecretExpressionEvaluator(long expressionFunctorToken) {
    super(null);
    this.expressionFunctorToken = expressionFunctorToken;
  }

  @Override
  protected void initialize() {
    super.initialize();
    this.addToContext("secrets", new SecretFunctor(expressionFunctorToken));
  }
}
