/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.core.storage.fedora;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.fcrepo.client.BadRequestException;
import org.fcrepo.client.FedoraDatastream;
import org.fcrepo.client.FedoraException;
import org.fcrepo.client.FedoraObject;
import org.fcrepo.client.FedoraRepository;
import org.fcrepo.client.FedoraResource;
import org.fcrepo.client.ForbiddenException;
import org.fcrepo.client.impl.FedoraRepositoryImpl;
import org.roda.core.common.iterables.CloseableIterable;
import org.roda.core.data.exceptions.AlreadyExistsException;
import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.v2.ip.StoragePath;
import org.roda.core.storage.Binary;
import org.roda.core.storage.BinaryVersion;
import org.roda.core.storage.Container;
import org.roda.core.storage.ContentPayload;
import org.roda.core.storage.DefaultContainer;
import org.roda.core.storage.DefaultDirectory;
import org.roda.core.storage.DefaultStoragePath;
import org.roda.core.storage.DirectResourceAccess;
import org.roda.core.storage.Directory;
import org.roda.core.storage.Entity;
import org.roda.core.storage.Resource;
import org.roda.core.storage.StorageService;
import org.roda.core.storage.StorageServiceUtils;
import org.roda.core.storage.fedora.utils.FedoraConversionUtils;
import org.roda.core.storage.fedora.utils.FedoraUtils;
import org.roda.core.storage.fs.FileStorageService;
import org.roda.core.storage.utils.StorageRecursiveListingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that persists binary files and their containers in Fedora.
 *
 * @author Sébastien Leroux <sleroux@keep.pt>
 * @author Hélder Silva <hsilva@keep.pt>
 */
public class FedoraStorageService implements StorageService {
  public static final String RODA_PREFIX = "roda";
  public static final String RODA_NAMESPACE = "http://www.roda-project.org/roda#";

  public static final String FEDORA_CONTAINER = "fedora:Container";
  public static final String FEDORA_BINARY = "fedora:Binary";
  public static final String FEDORA_RESOURCE_METADATA = "fcr:metadata";

  private static final Logger LOGGER = LoggerFactory.getLogger(FedoraStorageService.class);

  private String fedoraURL;
  private String fedoraUsername;
  private String fedoraPassword;
  private FedoraRepository fedoraRepository;

  /**
   * Public constructor (for using without user credentials)
   *
   * @param fedoraURL
   *          Fedora base URL
   */
  public FedoraStorageService(String fedoraURL) {
    this.fedoraURL = fedoraURL;
    this.fedoraUsername = null;
    this.fedoraPassword = null;
    this.fedoraRepository = new FedoraRepositoryImpl(fedoraURL);
  }

  /**
   * Public constructor
   *
   * @param fedoraURL
   *          Fedora base URL
   * @param username
   *          Fedora username
   * @param password
   *          Fedora password
   */
  public FedoraStorageService(String fedoraURL, String username, String password) {
    this.fedoraURL = fedoraURL;
    this.fedoraUsername = username;
    this.fedoraPassword = password;
    this.fedoraRepository = new FedoraRepositoryImpl(fedoraURL, username, password);
  }

  public String getFedoraURL() {
    return fedoraURL;
  }

  public String getFedoraUsername() {
    return fedoraUsername;
  }

  public String getFedoraPassword() {
    return fedoraPassword;
  }

  public FedoraRepository getFedoraRepository() {
    return fedoraRepository;
  }

  @Override
  public CloseableIterable<Container> listContainers()
    throws AuthorizationDeniedException, RequestNotValidException, NotFoundException, GenericException {
    return new IterableContainer(fedoraRepository);
  }

  @Override
  public Container createContainer(StoragePath storagePath)
    throws AuthorizationDeniedException, AlreadyExistsException, RequestNotValidException, GenericException {

    try {
      fedoraRepository.createObject(FedoraUtils.createFedoraPath(storagePath));
      return new DefaultContainer(storagePath);
    } catch (ForbiddenException e) {
      throw new AuthorizationDeniedException("Could not create container", e);
    } catch (org.fcrepo.client.AlreadyExistsException e) {
      throw new AlreadyExistsException("Could not create container", e);
    } catch (FedoraException e) {
      throw new GenericException("Could not create container", e);
    }
  }

  @Override
  public Container getContainer(StoragePath storagePath)
    throws RequestNotValidException, AuthorizationDeniedException, NotFoundException, GenericException {

    if (!storagePath.isFromAContainer()) {
      throw new RequestNotValidException("The storage path provided isn't from a container: " + storagePath);
    }

    try {
      return FedoraConversionUtils
        .fedoraObjectToContainer(fedoraRepository.getObject(FedoraUtils.createFedoraPath(storagePath)));
    } catch (ForbiddenException e) {
      throw new AuthorizationDeniedException("Could not get container", e);
    } catch (BadRequestException e) {
      throw new RequestNotValidException("Could not get container", e);
    } catch (org.fcrepo.client.NotFoundException e) {
      throw new NotFoundException("Could not get container", e);
    } catch (FedoraException e) {
      throw new GenericException("Could not get container", e);
    }
  }

  @Override
  public void deleteContainer(StoragePath storagePath)
    throws AuthorizationDeniedException, NotFoundException, GenericException {
    try {
      fedoraRepository.getObject(FedoraUtils.createFedoraPath(storagePath)).forceDelete();
    } catch (ForbiddenException e) {
      throw new AuthorizationDeniedException("Could not delete container", e);
    } catch (org.fcrepo.client.NotFoundException e) {
      throw new NotFoundException("Could not delete container", e);
    } catch (FedoraException e) {
      throw new GenericException("Could not get container", e);
    }
  }

  @Override
  public CloseableIterable<Resource> listResourcesUnderContainer(StoragePath storagePath, boolean recursive)
    throws AuthorizationDeniedException, RequestNotValidException, NotFoundException, GenericException {
    if (recursive == true) {
      return StorageRecursiveListingUtils.listAllUnderContainer(this, storagePath);
    } else {
      return new IterableResource(fedoraRepository, storagePath);
    }
  }

  @Override
  public Long countResourcesUnderContainer(StoragePath storagePath, boolean recursive)
    throws AuthorizationDeniedException, RequestNotValidException, NotFoundException, GenericException {

    if (recursive == true) {
      return StorageRecursiveListingUtils.countAllUnderContainer(this, storagePath);
    } else {
      try {
        Collection<FedoraResource> children = fedoraRepository.getObject(FedoraUtils.createFedoraPath(storagePath))
          .getChildren(null);
        return Long.valueOf(children.size());
      } catch (ForbiddenException e) {
        throw new AuthorizationDeniedException("Could not count resource under directory", e);
      } catch (BadRequestException e) {
        throw new RequestNotValidException("Could not count resource under directory", e);
      } catch (org.fcrepo.client.NotFoundException e) {
        throw new NotFoundException("Could not count resource under directory", e);
      } catch (FedoraException e) {
        throw new GenericException("Could not count resource under directory", e);
      }
    }
  }

  @Override
  public Directory createDirectory(StoragePath storagePath)
    throws AuthorizationDeniedException, AlreadyExistsException, GenericException {
    try {
      fedoraRepository.createObject(FedoraUtils.createFedoraPath(storagePath));
      return new DefaultDirectory(storagePath);
    } catch (ForbiddenException e) {
      throw new AuthorizationDeniedException("Could not create directory", e);
    } catch (org.fcrepo.client.AlreadyExistsException e) {
      throw new AlreadyExistsException("Could not create directory", e);
    } catch (FedoraException e) {
      throw new GenericException("Could not create directory", e);
    }

  }

  @Override
  public Directory createRandomDirectory(StoragePath parentStoragePath)
    throws AuthorizationDeniedException, RequestNotValidException, NotFoundException, GenericException {

    FedoraObject directory;
    try {
      StoragePath storagePath = DefaultStoragePath.parse(parentStoragePath, UUID.randomUUID().toString());
      do {
        try {
          // XXX may want to change create object to native Fedora method that
          // creates a random object
          directory = fedoraRepository.createObject(FedoraUtils.createFedoraPath(storagePath));
        } catch (org.fcrepo.client.AlreadyExistsException e) {
          directory = null;
          LOGGER.warn("Got a colision when creating random directory", e);
        }
      } while (directory == null);
      return new DefaultDirectory(storagePath);
    } catch (ForbiddenException e) {
      throw new AuthorizationDeniedException("Error creating random directory under " + parentStoragePath, e);
    } catch (org.fcrepo.client.NotFoundException e) {
      throw new NotFoundException("Error creating random directory under " + parentStoragePath, e);
    } catch (FedoraException e) {
      throw new GenericException("Error creating random directory under " + parentStoragePath, e);
    }
  }

  @Override
  public Directory getDirectory(StoragePath storagePath)
    throws RequestNotValidException, GenericException, AuthorizationDeniedException, NotFoundException {
    if (storagePath.isFromAContainer()) {
      throw new RequestNotValidException("Invalid storage path for a directory: " + storagePath);
    }
    try {
      FedoraObject object = fedoraRepository.getObject(FedoraUtils.createFedoraPath(storagePath));
      return FedoraConversionUtils.fedoraObjectToDirectory(fedoraRepository.getRepositoryUrl(), object);
    } catch (ForbiddenException e) {
      throw new AuthorizationDeniedException("Error getting directory " + storagePath, e);
    } catch (BadRequestException e) {
      throw new RequestNotValidException("Error getting directory " + storagePath, e);
    } catch (org.fcrepo.client.NotFoundException e) {
      throw new NotFoundException("Error getting directory " + storagePath, e);
    } catch (FedoraException e) {
      // Unfortunately Fedora does not give a better error when requesting a
      // file as a directory
      throw new RequestNotValidException("Error getting directory " + storagePath, e);
    }
  }

  @Override
  public CloseableIterable<Resource> listResourcesUnderDirectory(StoragePath storagePath, boolean recursive)
    throws AuthorizationDeniedException, RequestNotValidException, NotFoundException, GenericException {
    if (recursive) {
      return StorageRecursiveListingUtils.listAllUnderDirectory(this, storagePath);
    } else {
      return new IterableResource(fedoraRepository, storagePath);
    }
  }

  @Override
  public Long countResourcesUnderDirectory(StoragePath storagePath, boolean recursive)
    throws NotFoundException, GenericException, AuthorizationDeniedException, RequestNotValidException {
    if (recursive) {
      return StorageRecursiveListingUtils.countAllUnderDirectory(this, storagePath);
    } else {
      try {
        Collection<FedoraResource> children = fedoraRepository.getObject(FedoraUtils.createFedoraPath(storagePath))
          .getChildren(null);
        return Long.valueOf(children.size());
      } catch (ForbiddenException e) {
        throw new AuthorizationDeniedException("Could not count resource under directory", e);
      } catch (BadRequestException e) {
        throw new RequestNotValidException("Could not count resource under directory", e);
      } catch (org.fcrepo.client.NotFoundException e) {
        throw new NotFoundException("Could not count resource under directory", e);
      } catch (FedoraException e) {
        throw new GenericException("Could not count resource under directory", e);
      }
    }
  }

  @Override
  public Binary createBinary(StoragePath storagePath, ContentPayload payload, boolean asReference)
    throws GenericException, RequestNotValidException, AuthorizationDeniedException, AlreadyExistsException,
    NotFoundException {
    if (asReference) {
      // TODO method to create binary as reference.
      throw new GenericException("Creating binary as reference not yet supported");
    } else {
      try {
        FedoraDatastream binary = fedoraRepository.createDatastream(FedoraUtils.createFedoraPath(storagePath),
          FedoraConversionUtils.contentPayloadToFedoraContent(payload));

        return FedoraConversionUtils.fedoraDatastreamToBinary(binary);
      } catch (ForbiddenException e) {
        throw new AuthorizationDeniedException("Error creating binary", e);
      } catch (org.fcrepo.client.AlreadyExistsException e) {
        throw new AlreadyExistsException("Error creating binary", e);
      } catch (org.fcrepo.client.NotFoundException e) {
        throw new NotFoundException("Error creating binary", e);
      } catch (FedoraException e) {
        throw new GenericException("Error creating binary", e);
      }
    }

  }

  @Override
  public Binary createRandomBinary(StoragePath parentStoragePath, ContentPayload payload, boolean asReference)
    throws GenericException, RequestNotValidException, AuthorizationDeniedException, NotFoundException {
    if (asReference) {
      // TODO method to create binary as reference.
      throw new GenericException("Creating binary as reference not yet supported");
    } else {
      try {
        FedoraDatastream binary;
        StoragePath storagePath = DefaultStoragePath.parse(parentStoragePath, UUID.randomUUID().toString());
        do {
          try {
            // XXX may want to change create object to native Fedora method that
            // creates a random datastream
            binary = fedoraRepository.createDatastream(FedoraUtils.createFedoraPath(storagePath),
              FedoraConversionUtils.contentPayloadToFedoraContent(payload));
          } catch (org.fcrepo.client.AlreadyExistsException e) {
            binary = null;
            LOGGER.warn("Got a colision when creating random bianry", e);
          }
        } while (binary == null);

        return FedoraConversionUtils.fedoraDatastreamToBinary(binary);
      } catch (ForbiddenException e) {
        throw new AuthorizationDeniedException(e.getMessage(), e);
      } catch (org.fcrepo.client.NotFoundException e) {
        throw new NotFoundException(e.getMessage(), e);
      } catch (FedoraException e) {
        throw new GenericException(e.getMessage(), e);
      }
    }
  }

  @Override
  public Binary updateBinaryContent(StoragePath storagePath, ContentPayload payload, boolean asReference,
    boolean createIfNotExists)
      throws GenericException, AuthorizationDeniedException, RequestNotValidException, NotFoundException {
    if (asReference) {
      // TODO method to update binary as reference.
      throw new GenericException("Updating binary as reference not yet supported");
    } else {
      try {
        FedoraDatastream datastream = fedoraRepository.getDatastream(FedoraUtils.createFedoraPath(storagePath));

        datastream.updateContent(FedoraConversionUtils.contentPayloadToFedoraContent(payload));

        return FedoraConversionUtils.fedoraDatastreamToBinary(datastream);
      } catch (ForbiddenException e) {
        throw new AuthorizationDeniedException("Error updating binary content", e);
      } catch (org.fcrepo.client.NotFoundException e) {
        if (createIfNotExists) {
          try {
            return createBinary(storagePath, payload, asReference);
          } catch (AlreadyExistsException e1) {
            throw new GenericException("Error updating binary content", e1);
          }
        } else {
          throw new NotFoundException("Error updating binary content", e);
        }
      } catch (FedoraException e) {
        throw new GenericException("Error updating binary content", e);
      }

    }
  }

  @Override
  public Binary getBinary(StoragePath storagePath)
    throws RequestNotValidException, GenericException, NotFoundException, AuthorizationDeniedException {
    try {
      FedoraDatastream ds = fedoraRepository.getDatastream(FedoraUtils.createFedoraPath(storagePath));

      if (!isDatastream(ds)) {
        throw new RequestNotValidException("The resource obtained as being a datastream isn't really a datastream");
      }

      return FedoraConversionUtils.fedoraDatastreamToBinary(ds);
    } catch (ForbiddenException e) {
      throw new AuthorizationDeniedException(e.getMessage(), e);
    } catch (BadRequestException e) {
      throw new RequestNotValidException(e.getMessage(), e);
    } catch (org.fcrepo.client.NotFoundException e) {
      throw new NotFoundException(e.getMessage(), e);
    } catch (FedoraException e) {
      throw new GenericException(e.getMessage(), e);
    }
  }

  private boolean isDatastream(FedoraDatastream ds) throws FedoraException {
    Collection<String> mixins = ds.getMixins();
    return !mixins.contains(FEDORA_CONTAINER);
  }

  @Override
  public void deleteResource(StoragePath storagePath)
    throws NotFoundException, AuthorizationDeniedException, GenericException {
    String fedoraPath = FedoraUtils.createFedoraPath(storagePath);
    try {
      if (fedoraRepository.exists(fedoraPath)) {
        boolean deleted = false;
        try {
          FedoraDatastream fds = fedoraRepository.getDatastream(fedoraPath);
          if (fds != null) {
            fds.forceDelete();
            deleted = true;
          }
        } catch (FedoraException e) {
          // FIXME add proper error handling
        }
        if (!deleted) {
          try {
            FedoraObject object = fedoraRepository.getObject(fedoraPath);
            if (object != null) {
              object.forceDelete();
            }
          } catch (FedoraException e) {
            // FIXME add proper error handling
          }
        }
      } else {
        throw new NotFoundException("The resource identified by the path \"" + storagePath + "\" was not found");
      }
    } catch (ForbiddenException e) {
      throw new AuthorizationDeniedException("Error deleting resource: " + storagePath, e);
    } catch (org.fcrepo.client.NotFoundException e) {
      throw new NotFoundException(e.getMessage(), e);
    } catch (FedoraException e) {
      throw new GenericException("Error deleting resource: " + storagePath, e);
    }

  }

  @Override
  public void copy(StorageService fromService, StoragePath fromStoragePath, StoragePath toStoragePath)
    throws GenericException, RequestNotValidException, AuthorizationDeniedException, NotFoundException,
    AlreadyExistsException {

    Class<? extends Entity> rootEntity = fromService.getEntity(fromStoragePath);

    if (fromService instanceof FedoraStorageService
      && ((FedoraStorageService) fromService).getFedoraURL().equalsIgnoreCase(getFedoraURL())) {
      copyInsideFedora(fromStoragePath, toStoragePath, rootEntity);
    } else {
      StorageServiceUtils.copyBetweenStorageServices(fromService, fromStoragePath, this, toStoragePath, rootEntity);
    }

  }

  private void copyInsideFedora(StoragePath fromStoragePath, StoragePath toStoragePath,
    Class<? extends Entity> rootEntity)
      throws AuthorizationDeniedException, RequestNotValidException, NotFoundException, GenericException {
    try {
      if (rootEntity.equals(Container.class) || rootEntity.equals(Directory.class)) {
        FedoraObject object = fedoraRepository.getObject(FedoraUtils.createFedoraPath(fromStoragePath));

        object.copy(FedoraUtils.createFedoraPath(toStoragePath));
      } else {
        FedoraDatastream datastream = fedoraRepository.getDatastream(FedoraUtils.createFedoraPath(fromStoragePath));

        datastream.copy(FedoraUtils.createFedoraPath(toStoragePath));
      }

    } catch (ForbiddenException e) {
      throw new AuthorizationDeniedException("Error while copying from one storage path to another", e);
    } catch (BadRequestException e) {
      throw new RequestNotValidException("Error while copying from one storage path to another", e);
    } catch (org.fcrepo.client.NotFoundException e) {
      throw new NotFoundException("Error while copying from one storage path to another", e);
    } catch (FedoraException e) {
      throw new GenericException("Error while copying from one storage path to another", e);
    }
  }

  @Override
  public void move(StorageService fromService, StoragePath fromStoragePath, StoragePath toStoragePath)
    throws GenericException, AuthorizationDeniedException, RequestNotValidException, NotFoundException,
    AlreadyExistsException {

    Class<? extends Entity> rootEntity = fromService.getEntity(fromStoragePath);

    if (fromService instanceof FedoraStorageService
      && ((FedoraStorageService) fromService).getFedoraURL().equalsIgnoreCase(getFedoraURL())) {
      moveInsideFedora(fromStoragePath, toStoragePath, rootEntity);
    } else {
      StorageServiceUtils.moveBetweenStorageServices(fromService, fromStoragePath, this, toStoragePath, rootEntity);
    }
  }

  private void moveInsideFedora(StoragePath fromStoragePath, StoragePath toStoragePath,
    Class<? extends Entity> rootEntity)
      throws AuthorizationDeniedException, RequestNotValidException, NotFoundException, GenericException {
    try {
      if (rootEntity.equals(Container.class) || rootEntity.equals(Directory.class)) {
        FedoraObject object = fedoraRepository.getObject(FedoraUtils.createFedoraPath(fromStoragePath));

        object.forceMove(FedoraUtils.createFedoraPath(toStoragePath));
      } else {
        FedoraDatastream datastream = fedoraRepository.getDatastream(FedoraUtils.createFedoraPath(fromStoragePath));

        datastream.forceMove(FedoraUtils.createFedoraPath(toStoragePath));
      }

    } catch (ForbiddenException e) {
      throw new AuthorizationDeniedException("Error while moving from one storage path to another", e);
    } catch (BadRequestException e) {
      throw new RequestNotValidException("Error while moving from one storage path to another", e);
    } catch (org.fcrepo.client.NotFoundException e) {
      throw new NotFoundException("Error while moving from one storage path to another", e);
    } catch (FedoraException e) {
      throw new GenericException("Error while moving from one storage path to another", e);
    }
  }

  @Override
  public Class<? extends Entity> getEntity(StoragePath storagePath)
    throws GenericException, RequestNotValidException, AuthorizationDeniedException, NotFoundException {
    if (storagePath.isFromAContainer()) {
      if (getContainer(storagePath) != null) {
        return Container.class;
      } else {
        throw new GenericException(
          "There is no Container in the storage represented by \"" + storagePath.asString() + "\"");
      }
    } else {
      // it's a directory or binary. but first let's see if that entity
      // exists in the storage
      try {
        FedoraObject object = fedoraRepository
          .getObject(FedoraUtils.createFedoraPath(storagePath) + "/" + FEDORA_RESOURCE_METADATA);

        if (object.getMixins().contains(FEDORA_CONTAINER)) {
          return Directory.class;
        } else {
          // it exists, it's not a directory, so it can only be a
          // binary
          return Binary.class;
        }
      } catch (FedoraException e) {
        throw new GenericException(
          "There is no Directory or Binary in the storage represented by \"" + storagePath.asString() + "\"", e);
      }

    }
  }

  @Override
  public DirectResourceAccess getDirectAccess(final StoragePath storagePath) {
    DirectResourceAccess ret = new DirectResourceAccess() {
      Path temp = null;

      @Override
      public Path getPath()
        throws GenericException, RequestNotValidException, AuthorizationDeniedException, NotFoundException {
        Class<? extends Entity> entity = getEntity(storagePath);
        Path path;
        try {
          temp = Files.createTempDirectory("temp");
          if (entity.equals(Container.class) || entity.equals(Directory.class)) {
            StorageService tempStorage = new FileStorageService(temp);
            tempStorage.copy(FedoraStorageService.this, storagePath, storagePath);
            path = temp;
          } else {
            path = temp.resolve(entity.getName());
            Binary binary = getBinary(storagePath);
            ContentPayload payload = binary.getContent();
            InputStream inputStream = payload.createInputStream();
            Files.copy(inputStream, path);
            IOUtils.closeQuietly(inputStream);
          }
        } catch (IOException | AlreadyExistsException e) {
          throw new GenericException(e);
        }
        return path;
      }

      @Override
      public void close() throws IOException {
        if (temp != null) {
          Files.delete(temp);
          temp = null;
        }
      }

    };
    return ret;
  }

  @Override
  public List<BinaryVersion> listBinaryVersions(StoragePath storagePath)
    throws GenericException, RequestNotValidException, NotFoundException, AuthorizationDeniedException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public BinaryVersion getBinaryVersion(StoragePath storagePath, String version) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void createBinaryVersion(StoragePath storagePath, String version) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void revertBinaryVersion(StoragePath storagePath, String version) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void deleteBinaryVersion(StoragePath storagePath, String version) {
    // TODO Auto-generated method stub
    
  }

}
