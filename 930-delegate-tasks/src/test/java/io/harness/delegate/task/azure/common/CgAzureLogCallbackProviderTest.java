/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.delegate.task.azure.common;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ABOSII;

import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
public class CgAzureLogCallbackProviderTest extends CategoryTest {
  @Mock private ILogStreamingTaskClient logStreamingTaskClient;

  @InjectMocks private CgAzureLogCallbackProvider logCallbackProvider;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testObtainLogCallback() {
    logCallbackProvider.obtainLogCallback("test");
    verify(logStreamingTaskClient).obtainLogCallback("test");
  }
}