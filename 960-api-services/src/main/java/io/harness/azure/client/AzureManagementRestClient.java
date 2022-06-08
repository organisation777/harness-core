/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.azure.client;

import io.harness.azure.model.AzureConstants;

import software.wings.helpers.ext.azure.AksClusterCredentials;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;
import rx.Observable;

public interface AzureManagementRestClient {
  String APP_VERSION = "2020-10-01";

  @GET("providers/Microsoft.Management/managementGroups?api-version=2020-10-01")
  Observable<Response<ResponseBody>> listManagementGroups(@Header("Authorization") String bearerAuthHeader);

  @Headers({"Content-Type: application/json; charset=utf-8", "accept-language: en-US"})
  @GET
  Observable<Response<ResponseBody>> listNext(
      @Header("Authorization") String bearerAuthHeader, @Url String nextUrl, @Query("api-version") String appVersion);

  @POST("subscriptions/{" + AzureConstants.SUBSCRIPTION + "}/resourceGroups/{" + AzureConstants.RESOURCE_GROUP
      + "}/providers/Microsoft.ContainerService/managedClusters/{" + AzureConstants.AKS_CLUSTER_NAME
      + "}/listClusterUserCredential?api-version=2022-02-01")
  Call<AksClusterCredentials>
  listClusterUserCredential(@Header("Authorization") String accessToken,
      @Path(value = AzureConstants.SUBSCRIPTION) String subscription,
      @Path(value = AzureConstants.RESOURCE_GROUP) String resourceGroup,
      @Path(value = AzureConstants.AKS_CLUSTER_NAME) String aksClusterName);

  @POST("subscriptions/{" + AzureConstants.SUBSCRIPTION + "}/resourceGroups/{" + AzureConstants.RESOURCE_GROUP
      + "}/providers/Microsoft.ContainerService/managedClusters/{" + AzureConstants.AKS_CLUSTER_NAME
      + "}/listClusterAdminCredential?api-version=2022-02-01")
  Call<AksClusterCredentials>
  listClusterAdminCredential(@Header("Authorization") String accessToken,
      @Path(value = AzureConstants.SUBSCRIPTION) String subscription,
      @Path(value = AzureConstants.RESOURCE_GROUP) String resourceGroup,
      @Path(value = AzureConstants.AKS_CLUSTER_NAME) String aksClusterName);

  @GET("subscriptions/{subscriptionId}/tagNames?api-version=2020-10-01")
  Observable<Response<ResponseBody>> listTags(
      @Header("Authorization") String bearerAuthHeader, @Path("subscriptionId") String subscriptionId);
}
