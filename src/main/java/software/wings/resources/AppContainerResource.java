package software.wings.resources;

import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static software.wings.beans.ArtifactSource.SourceType.HTTP;
import static software.wings.beans.FileUrlSource.Builder.aFileUrlSource;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.service.intfc.FileService.FileBucket.PLATFORMS;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AppContainer;
import software.wings.beans.ArtifactSource.SourceType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.beans.FileUploadSource;
import software.wings.beans.RestResponse;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppContainerService;
import software.wings.utils.BoundedInputStream;

import java.io.InputStream;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by anubhaw on 5/4/16.
 */

@Path("/app-containers")
@Produces("application/json")
@AuthRule
@Timed
@ExceptionMetered
public class AppContainerResource {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private AppContainerService appContainerService;

  @GET
  public RestResponse<PageResponse<AppContainer>> list(
      @QueryParam("appId") String appId, @BeanParam PageRequest<AppContainer> request) {
    request.addFilter("appId", appId, EQ);
    return new RestResponse<>(appContainerService.list(request));
  }

  @GET
  @Path("{appContainerId}")
  public RestResponse<AppContainer> get(@PathParam("appContainerId") String appContainerId) {
    return new RestResponse<>(appContainerService.get(appContainerId));
  }

  @POST
  @Consumes(MULTIPART_FORM_DATA)
  public RestResponse<String> uploadPlatform(@QueryParam("appId") String appId,
      @FormDataParam("sourceType") SourceType sourceType, @FormDataParam("url") String urlString,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail, @BeanParam AppContainer appContainer) {
    appContainer.setAppId(appId);
    setSourceForAppContainer(sourceType, urlString, appContainer);
    uploadedInputStream =
        updateTheUploadedInputStream(urlString, uploadedInputStream, appContainer.getSource().getSourceType());
    String fileId = appContainerService.save(appContainer, uploadedInputStream, PLATFORMS);
    return new RestResponse<>(fileId);
  }

  @PUT
  @Path("{appContainerId}")
  @Consumes(MULTIPART_FORM_DATA)
  public RestResponse<String> updatePlatform(@QueryParam("appId") String appId,
      @PathParam("appContainerId") String appContainerId, @FormDataParam("sourceType") SourceType sourceType,
      @FormDataParam("url") String urlString, @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail, @BeanParam AppContainer appContainer) {
    appContainer.setAppId(appId);
    setSourceForAppContainer(sourceType, urlString, appContainer);
    uploadedInputStream =
        updateTheUploadedInputStream(urlString, uploadedInputStream, appContainer.getSource().getSourceType());
    String fileId = appContainerService.update(appContainerId, appContainer, uploadedInputStream, PLATFORMS);
    return new RestResponse<>(fileId);
  }

  private void setSourceForAppContainer(
      SourceType sourceType, String urlString, AppContainer appContainer) { // Fixme: use jsonSubType
    if (sourceType.equals(HTTP)) {
      appContainer.setSource(aFileUrlSource().withUrl(urlString).build());
    } else {
      appContainer.setSource(new FileUploadSource());
    }
  }

  @DELETE
  @Path("{appContainerId}")
  public void deletePlatform(@PathParam("appId") String appId, @PathParam("appContainerId") String appContainerId) {
    appContainerService.delete(appContainerId);
  }

  private InputStream updateTheUploadedInputStream(String urlString, InputStream inputStream, SourceType sourceType) {
    if (sourceType.equals(HTTP)) {
      inputStream =
          BoundedInputStream.getBoundedStreamForUrl(urlString, 4 * 1000 * 1000 * 1000); // TODO: read from config
    }
    return inputStream;
  }
}
