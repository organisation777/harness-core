/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.apiexamples;

public class InputSetAPIConstants {
  public static final String CREATE_API = "inputSet:\n"
      + "    name: Sample Input Set\n"
      + "    tags: {}\n"
      + "    identifier: Sample_Input_Set\n"
      + "    orgIdentifier: default\n"
      + "    projectIdentifier: MISC\n"
      + "    pipeline:\n"
      + "        identifier: Sample_Pipeline\n"
      + "        stages:\n"
      + "            - stage:\n"
      + "                  identifier: Sample_Stage\n"
      + "                  type: Approval\n"
      + "                  spec:\n"
      + "                      execution:\n"
      + "                          steps:\n"
      + "                              - step:\n"
      + "                                    identifier: Approval_Step\n"
      + "                                    type: HarnessApproval\n"
      + "                                    spec:\n"
      + "                                        approvers:\n"
      + "                                            userGroups:\n"
      + "                                                - account.Admins\n"
      + "                              - step:\n"
      + "                                    identifier: Shellscript_Step\n"
      + "                                    type: ShellScript\n"
      + "                                    spec:\n"
      + "                                        source:\n"
      + "                                            type: Inline\n"
      + "                                            spec:\n"
      + "                                                script: echo \"ShellScript\"\n"
      + "            - stage:\n"
      + "                  identifier: Sample_Deploy_Stage\n"
      + "                  type: Deployment\n"
      + "                  spec:\n"
      + "                      serviceConfig:\n"
      + "                          serviceRef: Service1\n"
      + "                      infrastructure:\n"
      + "                          environmentRef: Env1\n"
      + "                          infrastructureDefinition:\n"
      + "                              type: KubernetesDirect\n"
      + "                              spec:\n"
      + "                                  connectorRef: account.harnessciplatform\n"
      + "                                  namespace: sample\n";
}