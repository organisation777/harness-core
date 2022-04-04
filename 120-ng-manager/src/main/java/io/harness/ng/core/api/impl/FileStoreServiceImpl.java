/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.delegate.beans.FileBucket.CONFIGS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.FileUploadLimit;
import io.harness.file.beans.NGBaseFile;
import io.harness.ng.core.api.FileStoreService;
import io.harness.ng.core.dto.filestore.FileDTO;
import io.harness.ng.core.dto.filestore.NGFileType;
import io.harness.ng.core.dto.filestore.node.FileStoreNodeDTO;
import io.harness.ng.core.dto.filestore.node.FolderNodeDTO;
import io.harness.ng.core.entities.NGFile;
import io.harness.ng.core.mapper.FileStoreNodeDTOMapper;
import io.harness.repositories.filestore.spring.FileStoreRepository;
import io.harness.stream.BoundedInputStream;

import software.wings.service.intfc.FileService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Singleton
@OwnedBy(CDP)
public class FileStoreServiceImpl implements FileStoreService {
  private final FileService fileService;
  private final FileStoreRepository fileStoreRepository;
  private final FileUploadLimit fileUploadLimit;

  @Inject
  public FileStoreServiceImpl(
      FileService fileService, FileStoreRepository fileStoreRepository, FileUploadLimit fileUploadLimit) {
    this.fileService = fileService;
    this.fileStoreRepository = fileStoreRepository;
    this.fileUploadLimit = fileUploadLimit;
  }

  @Override
  public FileDTO create(@Valid FileDTO fileDto, InputStream content) {
    // save entities into configs.files and configs.files by using fileService
    NGBaseFile baseFile = new NGBaseFile();
    baseFile.setFileName(fileDto.getName());
    baseFile.setAccountId(fileDto.getAccountIdentifier());
    baseFile.setFileUuid(UUIDGenerator.generateUuid());
    String fileId = fileService.saveFile(
        baseFile, new BoundedInputStream(content, fileUploadLimit.getEncryptedFileLimit()), CONFIGS);
    // use mapper to create NGFile from fileDto and NGBaseFile
    // save NGFile into nfFile by using fileStoreRepository or fileStoreRepositoryCustom
    return null;
  }

  @Override
  public FileDTO get(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    // get entity from DB by using fileStoreRepository or fileStoreRepositoryCustom
    return null;
  }

  @Override
  public FileDTO update(@Valid FileDTO fileDto, InputStream content) {
    // update entities in configs.files and configs.files by using fileService
    // update entity in DB by using fileStoreRepository or fileStoreRepositoryCustom
    return null;
  }

  @Override
  public boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    // delete entities in configs.files and configs.files by using fileService
    // delete entity in DB by using fileStoreRepository or fileStoreRepositoryCustom
    return false;
  }

  @Override
  public FolderNodeDTO listFolderNodes(@NotNull String accountIdentifier, String orgIdentifier,
      String projectIdentifier, @Valid FolderNodeDTO folderNode) {
    return populateFolderNode(folderNode, accountIdentifier, orgIdentifier, projectIdentifier);
  }

  private FolderNodeDTO populateFolderNode(
      FolderNodeDTO folderNode, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    List<FileStoreNodeDTO> fileStoreNodes =
        listFolderChildren(accountIdentifier, orgIdentifier, projectIdentifier, folderNode.getFolderIdentifier());
    for (FileStoreNodeDTO node : fileStoreNodes) {
      folderNode.addChild(node);
    }
    return folderNode;
  }

  private List<FileStoreNodeDTO> listFolderChildren(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String folderIdentifier) {
    return listFilesByParentIdentifier(accountIdentifier, orgIdentifier, projectIdentifier, folderIdentifier)
        .stream()
        .filter(Objects::nonNull)
        .map(ngFile
            -> ngFile.getType() == NGFileType.FOLDER ? FileStoreNodeDTOMapper.getFolderNodeDTO(ngFile)
                                                     : FileStoreNodeDTOMapper.getFileNodeDTO(ngFile))
        .collect(Collectors.toList());
  }

  private List<NGFile> listFilesByParentIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String parentIdentifier) {
    return fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndParentIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, parentIdentifier);
  }
}
