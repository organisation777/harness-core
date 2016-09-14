package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.beans.stats.DeploymentActivityStatistics;
import software.wings.beans.stats.WingsStatistics;
import software.wings.service.intfc.StatisticsService;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by anubhaw on 8/15/16.
 */
@Api("/statistics")
@Path("/statistics")
@Produces("application/json")
@Timed
@ExceptionMetered
public class StatisticsResource {
  @Inject private StatisticsService statisticsService;

  /**
   * Cards rest response.
   *
   * @return the rest response
   */
  @GET
  @Path("summary")
  public RestResponse<List<WingsStatistics>> cards() {
    return new RestResponse<>(statisticsService.getSummary());
  }

  /**
   * Key stats rest response.
   *
   * @return the rest response
   */
  @GET
  @Path("keystats")
  public RestResponse<List<WingsStatistics>> keyStats() {
    return new RestResponse<>(statisticsService.getKeyStats());
  }

  /**
   * Deployment activities rest response.
   *
   * @param numOfDays the num of days
   * @param endDate   the end date
   * @return the rest response
   */
  @GET
  @Path("deployment-activities")
  public RestResponse<DeploymentActivityStatistics> deploymentActivities(
      @QueryParam("numOfDays") Integer numOfDays, @QueryParam("endDate") Long endDate) {
    return new RestResponse<>(statisticsService.getDeploymentActivities(numOfDays, endDate));
  }

  /**
   * Top consumers rest response.
   *
   * @return the rest response
   */
  @GET
  @Path("top-consumers")
  public RestResponse<WingsStatistics> topConsumers() {
    return new RestResponse<>(statisticsService.getTopConsumers());
  }
}
