#!/bin/bash
# Copyright 2022 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

function download_non_apm_saas_images(){

  local_service_name="$1"
	local_version="$2"
  local_non_apm_image_path="${REGISTRY_PATH}/${REPO_PATH}/${local_service_name}:${local_version}"

  echo "INFO: Pulling NON APM IMAGE....."
	docker pull "${local_non_apm_image_path}"; STATUS=$?
	if [ "$STATUS" -eq 0 ]; then
		echo "INFO: Successfully pulled NON APM IMAGE: ${local_non_apm_image_path}:${local_version} from GCR"
	else
		echo "ERROR: Failed to pull NON APM IMAGE: ${local_non_apm_image_path}:${local_version} from GCR. Exiting..."
		exit 1
	fi
}

function sign_and_push_images_to_dockerhub(){

	local_service_name="$1"
	local_version="$2"
	local_build_number="$3"
  local_non_apm_image_path="${REGISTRY_PATH}/${REPO_PATH}/${local_service_name}:${local_version}"

	if [ "${local_service_name}" == "change-data-capture"  ]; then
	    local_service_name='cdcdata'
	fi

	local_unsigned_dockerhub_image_path=${DOCKER_PATH}/${local_service_name}:${local_build_number}
	local_signed_dockerhub_image_path=${DOCKER_PATH}/${local_service_name}-signed:${local_build_number}

	echo "INFO: Pushing Unsigned SaaS Image: ${local_non_apm_image_path} to Dockerhub path: ${local_unsigned_dockerhub_image_path}....."
	docker image inspect "${local_non_apm_image_path}" > /dev/null; STATUS=$?

	if [ ${STATUS} -eq 0 ]; then
    docker tag "${local_non_apm_image_path}" "${local_unsigned_dockerhub_image_path}"
    docker tag "${local_non_apm_image_path}" "${local_signed_dockerhub_image_path}"
    docker push "${local_unsigned_dockerhub_image_path}"; STATUS2=$?
  else
    echo "ERROR: SaaS docker image not found locally: ${local_non_apm_image_path}. Exiting....."; exit 1
  fi

   echo "INFO: Signing IMAGE: ${local_unsigned_dockerhub_image_path}....."
(cat <<END
$HARNESS_SIGNING_KEY_PASSPHRASE
$HARNESS_SIGNING_KEY_PASSPHRASE
END
) | docker trust sign "${local_signed_dockerhub_image_path}"; STATUS3=$?

	if [ "${STATUS3}" -eq 0 ]; then
		echo "INFO: Successfully signed: ${local_signed_dockerhub_image_path} with TAG:${local_build_number}"
	else
		echo "ERROR: Failed to signed Image: ${local_signed_dockerhub_image_path} with TAG:${local_build_number}"
		exit 1
	fi

}

IMAGES_LIST=(change-data-capture ci-manager manager ng-manager pipeline-service template-service \
platform-service verification-service cv-nextgen)

export REGISTRY_PATH='us.gcr.io/platform-205701'
export REPO_PATH='harness/saas-openjdk-temurin-11'
export DOCKER_PATH='harness'
export VERSION=${VERSION}
export BUILD_NUMBER=$(echo ${VERSION} | awk -F- '{print $1}')

#<+steps.build.output.outputVariables.VERSION>
if [ -z "${VERSION}" ]; then
    echo "ERROR: VERSION is not defined. Exiting..."
    exit 1
fi

echo "STEP 1: INFO: Downloading Images from GCR and signing them."
for IMAGE in "${IMAGES_LIST[@]}";
do
  download_non_apm_saas_images "$IMAGE" "$VERSION"
done

# Values required from the pipeline.
docker login -u "$DOCKER_USERNAME" -p "$DOCKER_PASSWORD"

echo "${HARNESS_SIGNING_KEY}"  | base64 -di > key.pem
chmod 600 key.pem

(cat <<END
${HARNESS_SIGNING_KEY_PASSPHRASE}
${HARNESS_SIGNING_KEY_PASSPHRASE}
END
) | docker trust key load key.pem --name harness
# Values required from the pipeline.

echo "STEP: INFO: Uploading Images to Dockerhub."
for IMAGE in "${IMAGES_LIST[@]}";
do
  sign_and_push_images_to_dockerhub "$IMAGE" "$VERSION" "$BUILD_NUMBER"
done
