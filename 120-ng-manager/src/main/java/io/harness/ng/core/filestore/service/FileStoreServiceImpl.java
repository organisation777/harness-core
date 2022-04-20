/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.filestore.service;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.beans.FileBucket.FILE_STORE;

import static java.lang.String.format;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.exception.DuplicateEntityException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.file.beans.NGBaseFile;
import io.harness.filestore.FileStoreConstants;
import io.harness.filestore.NGFileType;
import io.harness.ng.core.beans.SearchPageParams;
import io.harness.ng.core.dto.filestore.FileDTO;
import io.harness.ng.core.dto.filestore.node.FileStoreNodeDTO;
import io.harness.ng.core.dto.filestore.node.FolderNodeDTO;
import io.harness.ng.core.entities.NGFile;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ng.core.filestore.utils.FileReferencedByHelper;
import io.harness.ng.core.mapper.FileDTOMapper;
import io.harness.ng.core.mapper.FileStoreNodeDTOMapper;
import io.harness.repositories.filestore.FileStoreRepositoryCriteriaCreator;
import io.harness.repositories.filestore.spring.FileStoreRepository;
import io.harness.stream.BoundedInputStream;

import software.wings.app.MainConfiguration;
import software.wings.service.intfc.FileService;

import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;

@Singleton
@OwnedBy(CDP)
@Slf4j
public class FileStoreServiceImpl implements FileStoreService {
  private final FileService fileService;
  private final FileStoreRepository fileStoreRepository;
  private final MainConfiguration configuration;
  private final FileReferencedByHelper fileReferencedByHelper;

  @Inject
  public FileStoreServiceImpl(FileService fileService, FileStoreRepository fileStoreRepository,
      MainConfiguration configuration, FileReferencedByHelper fileReferencedByHelper) {
    this.fileService = fileService;
    this.fileStoreRepository = fileStoreRepository;
    this.configuration = configuration;
    this.fileReferencedByHelper = fileReferencedByHelper;
  }

  @Override
  public FileDTO create(@NotNull FileDTO fileDto, InputStream content, Boolean draft) {
    log.info("Creating {}: {}", fileDto.getType().name().toLowerCase(), fileDto);

    if (existInDatabase(fileDto)) {
      throw new DuplicateEntityException(getDuplicateEntityMessage(fileDto));
    }

    NGFile ngFile = FileDTOMapper.getNGFileFromDTO(fileDto, draft);

    if (shouldStoreFileContent(content, ngFile)) {
      log.info("Start creating file in file system, identifier: {}", fileDto.getIdentifier());
      saveFile(fileDto, ngFile, content);
    }

    try {
      ngFile = fileStoreRepository.save(ngFile);
      return FileDTOMapper.getFileDTOFromNGFile(ngFile);
    } catch (DuplicateKeyException e) {
      throw new DuplicateEntityException(getDuplicateEntityMessage(fileDto));
    }
  }

  @Override
  public FileDTO update(@NotNull FileDTO fileDto, InputStream content, @NotNull String identifier) {
    if (isEmpty(identifier)) {
      throw new InvalidArgumentsException("File identifier cannot be empty");
    }

    NGFile existingFile = fetchFileOrThrow(
        fileDto.getAccountIdentifier(), fileDto.getOrgIdentifier(), fileDto.getProjectIdentifier(), identifier);

    NGFile updatedNGFile = FileDTOMapper.updateNGFile(fileDto, existingFile);
    if (shouldStoreFileContent(content, updatedNGFile)) {
      log.info("Start updating file in file system, identifier: {}", identifier);
      saveFile(fileDto, updatedNGFile, content);
    }
    fileStoreRepository.save(updatedNGFile);
    return FileDTOMapper.getFileDTOFromNGFile(updatedNGFile);
  }

  @Override
  public File downloadFile(@NotNull String accountIdentifier, String orgIdentifier, String projectIdentifier,
      @NotNull String fileIdentifier) {
    if (isEmpty(fileIdentifier)) {
      throw new InvalidArgumentsException("File identifier cannot be null or empty");
    }
    if (isEmpty(accountIdentifier)) {
      throw new InvalidArgumentsException("Account identifier cannot be null or empty");
    }

    NGFile ngFile = fetchFileOrThrow(accountIdentifier, orgIdentifier, projectIdentifier, fileIdentifier);
    if (ngFile.isFolder()) {
      throw new InvalidArgumentsException(
          format("Downloading folder not supported, fileIdentifier: %s", fileIdentifier));
    }

    File file = new File(Files.createTempDir(), ngFile.getName());
    log.info("Start downloading file, fileIdentifier: {}, filePath: {}", fileIdentifier, file.getPath());
    return fileService.download(ngFile.getFileUuid(), file, FILE_STORE);
  }

  @Override
  public boolean delete(
      @NotNull String accountIdentifier, String orgIdentifier, String projectIdentifier, @NotNull String identifier) {
    if (isEmpty(identifier)) {
      throw new InvalidArgumentsException("File identifier cannot be empty");
    }
    if (isEmpty(accountIdentifier)) {
      throw new InvalidArgumentsException("Account identifier cannot be null or empty");
    }
    if (FileStoreConstants.ROOT_FOLDER_IDENTIFIER.equals(identifier)) {
      throw new InvalidArgumentsException(
          format("Root folder [%s] can not be deleted.", FileStoreConstants.ROOT_FOLDER_IDENTIFIER));
    }

    NGFile file = fetchFileOrThrow(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    validateIsReferencedBy(file);
    return deleteFileOrFolder(file);
  }

  @Override
  public FolderNodeDTO listFolderNodes(@NotNull String accountIdentifier, String orgIdentifier,
      String projectIdentifier, @NotNull FolderNodeDTO folderNodeDTO) {
    return populateFolderNode(folderNodeDTO, accountIdentifier, orgIdentifier, projectIdentifier);
  }

  @Override
  public Page<EntitySetupUsageDTO> listReferencedBy(SearchPageParams pageParams, @NotNull String accountIdentifier,
      String orgIdentifier, String projectIdentifier, @NotNull String identifier, EntityType entityType) {
    if (isEmpty(identifier)) {
      throw new InvalidArgumentsException("File identifier cannot be empty");
    }
    if (isEmpty(accountIdentifier)) {
      throw new InvalidArgumentsException("Account identifier cannot be null or empty");
    }
    NGFile file = fetchFileOrThrow(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return fileReferencedByHelper.getReferencedBy(pageParams, file, entityType);
  }

  private boolean existInDatabase(FileDTO fileDto) {
    return fileStoreRepository
        .findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(fileDto.getAccountIdentifier(),
            fileDto.getOrgIdentifier(), fileDto.getProjectIdentifier(), fileDto.getIdentifier())
        .isPresent();
  }

  private boolean shouldStoreFileContent(InputStream content, NGFile ngFile) {
    return content != null && !ngFile.isDraft() && ngFile.isFile();
  }

  private NGFile fetchFileOrThrow(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    return fileStoreRepository
        .findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, identifier)
        .orElseThrow(
            ()
                -> new InvalidArgumentsException(format(
                    "Not found file/folder with identifier [%s], accountIdentifier [%s], orgIdentifier [%s] and projectIdentifier [%s]",
                    identifier, accountIdentifier, orgIdentifier, projectIdentifier)));
  }

  private String getDuplicateEntityMessage(@NotNull FileDTO fileDto) {
    return format("Try creating another %s, %s with identifier [%s] already exists in the parent folder [%s]",
        fileDto.getType().name().toLowerCase(), fileDto.getType().name().toLowerCase(), fileDto.getIdentifier(),
        fileDto.getParentIdentifier());
  }

  private void saveFile(FileDTO fileDto, NGFile ngFile, @NotNull InputStream content) {
    BoundedInputStream fileContent =
        new BoundedInputStream(content, configuration.getFileUploadLimits().getFileStoreFileLimit());
    NGBaseFile ngBaseFile = FileDTOMapper.getNgBaseFileFromFileDTO(fileDto);
    fileService.saveFile(ngBaseFile, fileContent, FILE_STORE);
    ngFile.setSize(fileContent.getTotalBytesRead());
    ngFile.setFileUuid(ngBaseFile.getFileUuid());
    ngFile.setChecksumType(ngBaseFile.getChecksumType());
    ngFile.setChecksum(ngBaseFile.getChecksum());
    ngFile.setDraft(false);
  }

  // in the case when we need to return the whole folder structure, create recursion on this method
  private FolderNodeDTO populateFolderNode(
      FolderNodeDTO folderNode, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    List<FileStoreNodeDTO> fileStoreNodes =
        listFolderChildren(accountIdentifier, orgIdentifier, projectIdentifier, folderNode.getIdentifier());
    for (FileStoreNodeDTO node : fileStoreNodes) {
      folderNode.addChild(node);
    }
    return folderNode;
  }

  private List<FileStoreNodeDTO> listFolderChildren(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String folderIdentifier) {
    return listFilesByParentIdentifierSortedByLastModifiedAt(
        accountIdentifier, orgIdentifier, projectIdentifier, folderIdentifier)
        .stream()
        .filter(Objects::nonNull)
        .map(ngFile
            -> ngFile.isFolder() ? FileStoreNodeDTOMapper.getFolderNodeDTO(ngFile)
                                 : FileStoreNodeDTOMapper.getFileNodeDTO(ngFile))
        .collect(Collectors.toList());
  }

  private List<NGFile> listFilesByParentIdentifierSortedByLastModifiedAt(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String parentIdentifier) {
    return fileStoreRepository.findAllAndSort(
        FileStoreRepositoryCriteriaCreator.createCriteriaByScopeAndParentIdentifier(
            Scope.of(accountIdentifier, orgIdentifier, projectIdentifier), parentIdentifier),
        FileStoreRepositoryCriteriaCreator.createSortByLastModifiedAtDesc());
  }

  private void validateIsReferencedBy(NGFile fileOrFolder) {
    if (NGFileType.FOLDER.equals(fileOrFolder.getType())) {
      if (anyFileInFolderHasReferences(fileOrFolder)) {
        throw new InvalidArgumentsException(format(
            "Folder [%s], or its subfolders, contain file(s) referenced by other entities and can not be deleted.",
            fileOrFolder.getIdentifier()));
      }
    } else {
      if (isFileReferencedByOtherEntities(fileOrFolder)) {
        throw new InvalidArgumentsException(
            format("File [%s] is referenced by other entities and can not be deleted.", fileOrFolder.getIdentifier()));
      }
    }
  }

  private boolean anyFileInFolderHasReferences(NGFile folder) {
    List<NGFile> childrenFiles = listFilesByParent(folder);
    if (isEmpty(childrenFiles)) {
      return false;
    }
    return childrenFiles.stream().filter(Objects::nonNull).anyMatch(this::isReferencedByOtherEntities);
  }

  private boolean isReferencedByOtherEntities(NGFile fileOrFolder) {
    if (NGFileType.FOLDER.equals(fileOrFolder.getType())) {
      return anyFileInFolderHasReferences(fileOrFolder);
    } else {
      return isFileReferencedByOtherEntities(fileOrFolder);
    }
  }

  private boolean isFileReferencedByOtherEntities(NGFile file) {
    return fileReferencedByHelper.isFileReferencedByOtherEntities(file);
  }

  private boolean deleteFileOrFolder(NGFile fileOrFolder) {
    if (NGFileType.FOLDER.equals(fileOrFolder.getType())) {
      return deleteFolder(fileOrFolder);
    } else {
      return deleteFile(fileOrFolder);
    }
  }

  private boolean deleteFolder(NGFile folder) {
    List<NGFile> childrenFiles = listFilesByParent(folder);
    if (!isEmpty(childrenFiles)) {
      childrenFiles.stream().filter(Objects::nonNull).forEach(this::deleteFileOrFolder);
    }
    try {
      fileStoreRepository.delete(folder);
      log.info("Folder [{}] deleted.", folder.getName());
      return true;
    } catch (Exception e) {
      log.error("Failed to delete folder [{}].", folder.getName(), e);
      return false;
    }
  }

  private List<NGFile> listFilesByParent(NGFile parent) {
    return fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndParentIdentifier(
        parent.getAccountIdentifier(), parent.getOrgIdentifier(), parent.getProjectIdentifier(),
        parent.getIdentifier());
  }

  private boolean deleteFile(NGFile file) {
    try {
      fileService.deleteFile(file.getFileUuid(), FILE_STORE);
      fileStoreRepository.delete(file);
      log.info("File [{}] deleted.", file.getIdentifier());
      return true;
    } catch (Exception e) {
      log.error("Failed to delete file [{}].", file.getIdentifier(), e);
      return false;
    }
  }
}