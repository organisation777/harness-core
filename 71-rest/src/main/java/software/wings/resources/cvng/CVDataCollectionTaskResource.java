package software.wings.resources.cvng;

import static io.harness.cvng.core.services.CVNextGenConstants.CV_NEXT_GEN_SERVICE_ENDPOINTS_PREFIX;
import static software.wings.security.PermissionAttribute.ResourceType.SERVICE;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.perpetualtask.CVDataCollectionTaskService;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import retrofit2.http.POST;
import software.wings.security.annotations.Scope;

import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api(CV_NEXT_GEN_SERVICE_ENDPOINTS_PREFIX + "/"
    + "cv-data-collection-task")
@Path(CV_NEXT_GEN_SERVICE_ENDPOINTS_PREFIX)
@Produces("application/json")
@Slf4j
@Scope(SERVICE)
@ExposeInternalException(withStackTrace = true)
public class CVDataCollectionTaskResource {
  @Inject private CVDataCollectionTaskService dataCollectionTaskService;

  @POST
  @Path("create-task")
  @Timed
  @ExceptionMetered
  public RestResponse<String> createTask(
      @QueryParam("accountId") String accountId, @QueryParam("cvConfigId") String cvConfigId) {
    return new RestResponse<>(dataCollectionTaskService.create(accountId, cvConfigId));
  }

  @DELETE
  @Path("delete-task")
  @Timed
  @ExceptionMetered
  public void deleteTask(@QueryParam("accountId") String accountId, @QueryParam("taskId") String taskId) {
    dataCollectionTaskService.delete(accountId, taskId);
  }
}
