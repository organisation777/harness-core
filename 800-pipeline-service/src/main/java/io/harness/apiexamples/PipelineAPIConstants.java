/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.apiexamples;

public class PipelineAPIConstants {
    public static final String CREATE_API = "pipeline:\n" +
            "  name: Sample Pipeline\n" +
            "  identifier: Sample_Pipeline\n" +
            "  allowStageExecutions: false\n" +
            "  projectIdentifier: Temp\n" +
            "  orgIdentifier: default\n" +
            "  tags: {}\n" +
            "  stages:\n" +
            "    - stage:\n" +
            "        name: Sample Stage\n" +
            "        identifier: Sample_Stage\n" +
            "        description: \"\"\n" +
            "        type: Approval\n" +
            "        spec:\n" +
            "          execution:\n" +
            "            steps:\n" +
            "              - step:\n" +
            "                  name: Approval Step\n" +
            "                  identifier: Approval_Step\n" +
            "                  type: HarnessApproval\n" +
            "                  timeout: 1d\n" +
            "                  spec:\n" +
            "                    approvalMessage: |-\n" +
            "                      Please review the following information\n" +
            "                      and approve the pipeline progression\n" +
            "                    includePipelineExecutionHistory: true\n" +
            "                    approvers:\n" +
            "                      minimumCount: 1\n" +
            "                      disallowPipelineExecutor: false\n" +
            "                      userGroups: <+input>\n" +
            "                    approverInputs: []\n" +
            "              - step:\n" +
            "                  type: ShellScript\n" +
            "                  name: Shellscript Step\n" +
            "                  identifier: Shellscript_Step\n" +
            "                  spec:\n" +
            "                    shell: Bash\n" +
            "                    onDelegate: true\n" +
            "                    source:\n" +
            "                      type: Inline\n" +
            "                      spec:\n" +
            "                        script: <+input>\n" +
            "                    environmentVariables: []\n" +
            "                    outputVariables: []\n" +
            "                    executionTarget: {}\n" +
            "                  timeout: 10m\n" +
            "        tags: {}\n" +
            "    - stage:\n" +
            "        name: Sample Deploy Stage\n" +
            "        identifier: Sample_Deploy_Stage\n" +
            "        description: \"\"\n" +
            "        type: Deployment\n" +
            "        spec:\n" +
            "          serviceConfig:\n" +
            "            serviceRef: <+input>\n" +
            "            serviceDefinition:\n" +
            "              spec:\n" +
            "                variables: []\n" +
            "              type: Kubernetes\n" +
            "          infrastructure:\n" +
            "            environmentRef: <+input>\n" +
            "            infrastructureDefinition:\n" +
            "              type: KubernetesDirect\n" +
            "              spec:\n" +
            "                connectorRef: <+input>\n" +
            "                namespace: <+input>\n" +
            "                releaseName: release-<+INFRA_KEY>\n" +
            "            allowSimultaneousDeployments: false\n" +
            "          execution:\n" +
            "            steps:\n" +
            "              - step:\n" +
            "                  name: Rollout Deployment\n" +
            "                  identifier: rolloutDeployment\n" +
            "                  type: K8sRollingDeploy\n" +
            "                  timeout: 10m\n" +
            "                  spec:\n" +
            "                    skipDryRun: false\n" +
            "            rollbackSteps:\n" +
            "              - step:\n" +
            "                  name: Rollback Rollout Deployment\n" +
            "                  identifier: rollbackRolloutDeployment\n" +
            "                  type: K8sRollingRollback\n" +
            "                  timeout: 10m\n" +
            "                  spec: {}\n" +
            "        tags: {}\n" +
            "        failureStrategies:\n" +
            "          - onFailure:\n" +
            "              errors:\n" +
            "                - AllErrors\n" +
            "              action:\n" +
            "                type: StageRollback";
}
