#!/bin/bash -e

docker pull harness/delegate:latest

docker run -it --hostname=$(hostname) \
-e ACCOUNT_ID=ACCOUNT_ID \
-e ACCOUNT_SECRET=ACCOUNT_KEY \
-e MANAGER_HOST_AND_PORT=https://localhost:9090 \
-e WATCHER_STORAGE_URL=http://localhost:8888 \
-e WATCHER_CHECK_LOCATION=watcherci.txt \
-e DELEGATE_STORAGE_URL=http://localhost:8888 \
-e DELEGATE_CHECK_LOCATION=delegateci.txt \
-e DESCRIPTION="description here" \
-e PROXY_HOST= \
-e PROXY_PORT= \
-e PROXY_SCHEME= \
-e NO_PROXY= \
-e POLL_FOR_TASKS=false \
harness/delegate:latest
