package org.roda.core.plugins.plugins.internal.disposal.schedule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.common.RodaConstants.PreservationEventType;
import org.roda.core.data.exceptions.AlreadyExistsException;
import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.IllegalOperationException;
import org.roda.core.data.exceptions.InvalidParameterException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.v2.LiteOptionalWithCause;
import org.roda.core.data.v2.index.filter.Filter;
import org.roda.core.data.v2.index.filter.OneOfManyFilterParameter;
import org.roda.core.data.v2.index.filter.SimpleFilterParameter;
import org.roda.core.data.v2.ip.AIP;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.core.data.v2.ip.disposal.DisposalSchedule;
import org.roda.core.data.v2.ip.disposal.aipMetadata.DisposalAIPMetadata;
import org.roda.core.data.v2.jobs.Job;
import org.roda.core.data.v2.jobs.PluginParameter;
import org.roda.core.data.v2.jobs.PluginState;
import org.roda.core.data.v2.jobs.PluginType;
import org.roda.core.data.v2.jobs.Report;
import org.roda.core.data.v2.validation.ValidationException;
import org.roda.core.index.IndexService;
import org.roda.core.index.utils.IterableIndexResult;
import org.roda.core.model.ModelService;
import org.roda.core.plugins.AbstractPlugin;
import org.roda.core.plugins.Plugin;
import org.roda.core.plugins.PluginException;
import org.roda.core.plugins.RODAObjectsProcessingLogic;
import org.roda.core.plugins.orchestrate.JobPluginInfo;
import org.roda.core.plugins.plugins.PluginHelper;
import org.roda.core.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Miguel Guimarães <mguimaraes@keep.pt>
 */
public class DisassociateDisposalScheduleToAIPPlugin extends AbstractPlugin<AIP> {
  private static final Logger LOGGER = LoggerFactory.getLogger(DisassociateDisposalScheduleToAIPPlugin.class);

  private boolean recursive = true;

  private static final Map<String, PluginParameter> pluginParameters = new HashMap<>();
  static {
    pluginParameters.put(RodaConstants.PLUGIN_PARAMS_RECURSIVE,
      new PluginParameter(RodaConstants.PLUGIN_PARAMS_RECURSIVE, "Recursive mode",
        PluginParameter.PluginParameterType.BOOLEAN, "true", true, false, "Execute in recursive mode."));
  }

  @Override
  public List<PluginParameter> getParameters() {
    ArrayList<PluginParameter> parameters = new ArrayList<>();
    parameters.add(pluginParameters.get(RodaConstants.PLUGIN_PARAMS_RECURSIVE));
    return parameters;
  }

  @Override
  public void setParameterValues(Map<String, String> parameters) throws InvalidParameterException {
    super.setParameterValues(parameters);
    if (parameters.containsKey(RodaConstants.PLUGIN_PARAMS_RECURSIVE)) {
      recursive = Boolean.parseBoolean(parameters.get(RodaConstants.PLUGIN_PARAMS_RECURSIVE));
    }
  }

  @Override
  public String getVersionImpl() {
    return "1.0";
  }

  public static String getStaticName() {
    return "Disassociate disposal schedule";
  }

  @Override
  public String getName() {
    return getStaticName();
  }

  public static String getStaticDescription() {
    return "";
  }

  @Override
  public String getDescription() {
    return getStaticDescription();
  }

  @Override
  public PreservationEventType getPreservationEventType() {
    return PreservationEventType.POLICY_ASSIGNMENT;
  }

  @Override
  public String getPreservationEventDescription() {
    return "Disassociate disposal schedule from AIP";
  }

  @Override
  public String getPreservationEventSuccessMessage() {
    return "";
  }

  @Override
  public String getPreservationEventFailureMessage() {
    return "";
  }

  @Override
  public void init() throws PluginException {
    // do nothing
  }

  @Override
  public Report beforeAllExecute(IndexService index, ModelService model, StorageService storage)
    throws PluginException {
    // do nothing
    return null;
  }

  @Override
  public Report execute(IndexService index, ModelService model, StorageService storage,
    List<LiteOptionalWithCause> liteList) throws PluginException {
    return PluginHelper.processObjects(this, (RODAObjectsProcessingLogic<AIP>) (index1, model1, storage1, report,
      cachedJob, jobPluginInfo, plugin, objects) -> {
      if (recursive) {
        calculateResourcesCounter(index, jobPluginInfo, objects);
      }
      processAIP(model1, index1, report, jobPluginInfo, cachedJob, objects);
    }, index, model, storage, liteList);
  }

  private void processAIP(ModelService model, IndexService index, Report report, JobPluginInfo jobPluginInfo,
    Job cachedJob, List<AIP> objects) {
    for (AIP aip : objects) {
      PluginState state = PluginState.SUCCESS;
      String outcomeText;
      DisposalSchedule disposalSchedule;

      LOGGER.debug("Processing AIP {}", aip.getId());
      Report reportItem = PluginHelper.initPluginReportItem(this, aip.getId(), AIP.class);
      PluginHelper.updatePartialJobReport(this, model, reportItem, false, cachedJob);

      if (aip.getDisposalScheduleId() == null) {
        state = PluginState.SKIPPED;
        LOGGER.info(
          "Disposal schedule disassociation was skipped because AIP '{}' is not associated with any disposal schedule",
          aip.getId());
        jobPluginInfo.incrementObjectsProcessed(state);
        reportItem.setPluginState(state).setPluginDetails("Disposal schedule disassociation was skipped because AIP '"
          + aip.getId() + "' is not associated with any disposal schedule");
        outcomeText = "Disposal schedule disassociation was skipped because AIP '" + aip.getId()
          + "' is not associated with any disposal schedule";
      } else {
        if (StringUtils.isNotBlank(aip.getDisposalConfirmationId())) {
          state = PluginState.FAILURE;
          LOGGER.error(
            "Error disassociation disposal schedule from AIP '{}': This AIP is part of a disposal confirmation report and the schedule cannot be changed",
            aip.getId());
          jobPluginInfo.incrementObjectsProcessedWithFailure();
          reportItem.setPluginState(state).setPluginDetails("Error disassociating disposal schedule from AIP '"
            + aip.getId() + "': This AIP is part of a disposal confirmation report and the schedule cannot be changed");
          outcomeText = PluginHelper.createOutcomeTextForDisposalSchedule(
            "failed to be disassociate from AIP '" + aip.getId()
              + "'; This AIP is part of a disposal confirmation report and the schedule cannot be changed",
            aip.getDisposalScheduleId(), null);
        } else {
          try {
            disposalSchedule = model.retrieveDisposalSchedule(aip.getDisposalScheduleId());
            if (aip.getDisposal() != null) {
              aip.getDisposal().setSchedule(null);
              model.updateDisposalSchedule(disposalSchedule, cachedJob.getUsername());
              model.updateAIP(aip, cachedJob.getUsername());
              reportItem.setPluginState(state).setPluginDetails(
                "Disposal schedule '" + aip.getDisposalScheduleId() + "' was successfully disassociated from AIP");

              outcomeText = PluginHelper.createOutcomeTextForDisposalSchedule(
                " was successfully disassociated from AIP", disposalSchedule.getId(), disposalSchedule.getTitle());
            } else {
              state = PluginState.SKIPPED;
              reportItem.setPluginState(state)
                .setPluginDetails("Disposal schedule '" + aip.getDisposalScheduleId() + "' does not exist on this AIP");
              outcomeText = PluginHelper.createOutcomeTextForDisposalSchedule(" does not exist on this AIP",
                disposalSchedule.getId(), disposalSchedule.getTitle());
            }
            jobPluginInfo.incrementObjectsProcessedWithSuccess();
          } catch (RequestNotValidException | GenericException | NotFoundException | AuthorizationDeniedException
            | IllegalOperationException e) {
            LOGGER.error("Error disassociating disposal schedule {} from AIP {}: {}", aip.getDisposalScheduleId(),
              aip.getId(), e.getMessage(), e);
            state = PluginState.FAILURE;
            jobPluginInfo.incrementObjectsProcessedWithFailure();
            reportItem.setPluginState(state)
              .setPluginDetails("Error disassociating disposal schedule " + aip.getId() + ": " + e.getMessage());
            outcomeText = PluginHelper.createOutcomeTextForDisposalSchedule(
              "failed to be disassociate from AIP '" + aip.getId() + "'", aip.getDisposalScheduleId(), null);
          }
        }
      }

      report.addReport(reportItem);
      PluginHelper.updatePartialJobReport(this, model, reportItem, true, cachedJob);

      if (recursive) {
        processChildren(model, index, cachedJob, aip, jobPluginInfo, report);
      }

      try {
        PluginHelper.createPluginEvent(this, aip.getId(), model, index, null, null, state, outcomeText, true,
          cachedJob);
      } catch (ValidationException | RequestNotValidException | NotFoundException | GenericException
        | AuthorizationDeniedException | AlreadyExistsException e) {
        LOGGER.error("Error creating event: {}", e.getMessage(), e);
      }
    }
  }

  private void calculateResourcesCounter(IndexService index, JobPluginInfo jobPluginInfo, List<AIP> aips) {
    try {
      ArrayList<String> ancestorList = aips.stream().map(AIP::getId).collect(Collectors.toCollection(ArrayList::new));
      Filter ancestorFilter = new Filter(new OneOfManyFilterParameter(RodaConstants.AIP_ANCESTORS, ancestorList));
      int resourceCounter = index.count(IndexedAIP.class, ancestorFilter).intValue();
      jobPluginInfo.setSourceObjectsCount(resourceCounter + aips.size());
    } catch (GenericException | RequestNotValidException e) {
      // do nothing
    }
  }

  private void processChildren(ModelService model, IndexService index, Job cachedJob, AIP aipParent,
    JobPluginInfo jobPluginInfo, Report report) {
    Filter ancestorFilter = new Filter(new SimpleFilterParameter(RodaConstants.AIP_ANCESTORS, aipParent.getId()));
    try (IterableIndexResult<IndexedAIP> result = index.findAll(IndexedAIP.class, ancestorFilter, false,
      Arrays.asList(RodaConstants.INDEX_UUID, RodaConstants.AIP_ID, RodaConstants.AIP_DISPOSAL_SCHEDULE_ID,
        RodaConstants.AIP_DISPOSAL_CONFIRMATION_ID))) {
      for (IndexedAIP indexedAIP : result) {
        PluginState state = PluginState.SUCCESS;
        String outcomeText;
        DisposalSchedule disposalSchedule;

        LOGGER.debug("Processing children AIP {}", indexedAIP.getId());
        Report reportItem = PluginHelper.initPluginReportItem(this, indexedAIP.getId(), AIP.class);
        PluginHelper.updatePartialJobReport(this, model, reportItem, false, cachedJob);

        AIP aipChildren = model.retrieveAIP(indexedAIP.getId());

        if (aipChildren.getDisposalScheduleId() == null) {
          state = PluginState.SKIPPED;
          LOGGER.info("Disposal schedule disassociation was skipped because AIP '" + indexedAIP.getId()
            + "' is not associated with any disposal schedule");
          jobPluginInfo.incrementObjectsProcessed(state);
          reportItem.setPluginState(state).setPluginDetails("Disposal schedule disassociation was skipped because AIP '"
            + indexedAIP.getId() + "' is not associated with any disposal schedule");
          outcomeText = "Disposal schedule disassociation was skipped because AIP '" + indexedAIP.getId()
            + "' is not associated with any disposal schedule";
        } else {
          if (StringUtils.isBlank(indexedAIP.getDisposalConfirmationId())) {
            try {
              disposalSchedule = model.retrieveDisposalSchedule(indexedAIP.getDisposalScheduleId());
              aipChildren.getDisposal().setSchedule(null);
              model.updateDisposalSchedule(disposalSchedule, cachedJob.getUsername());
              model.updateAIP(aipChildren, cachedJob.getUsername());

              reportItem.setPluginState(PluginState.SUCCESS);
              reportItem.setPluginDetails("Remove disposal schedule: " + disposalSchedule.getId());
              jobPluginInfo.incrementObjectsProcessedWithSuccess();

              outcomeText = PluginHelper.createOutcomeTextForDisposalSchedule(
                " was successfully disassociated from AIP", disposalSchedule.getId(), disposalSchedule.getTitle());
            } catch (AuthorizationDeniedException | NotFoundException | IllegalOperationException e) {
              LOGGER.error("Error removing disposal schedule {} from AIP {}: {}", indexedAIP.getDisposalScheduleId(),
                indexedAIP.getId(), e.getMessage(), e);
              jobPluginInfo.incrementObjectsProcessedWithFailure();
              reportItem.setPluginState(PluginState.FAILURE)
                .setPluginDetails("Error removing disposal schedule " + indexedAIP.getId() + ": " + e.getMessage());
              outcomeText = PluginHelper.createOutcomeTextForDisposalSchedule(
                "failed to be disassociate from AIP '" + indexedAIP.getId() + "'", indexedAIP.getDisposalScheduleId(),
                null);
            }
          } else {
            state = PluginState.FAILURE;
            LOGGER.error("Error disassociation disposal schedule from AIP '" + indexedAIP.getId()
              + "': This AIP is part of a disposal confirmation report and the schedule cannot be changed");
            jobPluginInfo.incrementObjectsProcessedWithFailure();
            reportItem.setPluginState(state)
              .setPluginDetails("Error disassociating disposal schedule from AIP '" + indexedAIP.getId()
                + "': This AIP is part of a disposal confirmation report and the schedule cannot be changed");
            outcomeText = PluginHelper.createOutcomeTextForDisposalSchedule(
              "failed to be disassociate from AIP '" + indexedAIP.getId()
                + "'; This AIP is part of a disposal confirmation report and the schedule cannot be changed",
              indexedAIP.getDisposalScheduleId(), null);
          }
        }
        report.addReport(reportItem);
        PluginHelper.updatePartialJobReport(this, model, reportItem, true, cachedJob);

        try {
          PluginHelper.createPluginEvent(this, indexedAIP.getId(), model, index, null, null,
            reportItem.getPluginState(), outcomeText, true, cachedJob);
        } catch (ValidationException | RequestNotValidException | NotFoundException | GenericException
          | AuthorizationDeniedException | AlreadyExistsException e) {
          LOGGER.error("Error creating event: {}", e.getMessage(), e);
        }

      }
    } catch (IOException | GenericException | RequestNotValidException | NotFoundException
      | AuthorizationDeniedException e) {
      LOGGER.error("Error removing disposal schedule for children", e);
    }
  }

  @Override
  public Report afterAllExecute(IndexService index, ModelService model, StorageService storage) throws PluginException {
    // do nothing
    return null;
  }

  @Override
  public void shutdown() {
    // do nothing
  }

  @Override
  public List<Class<AIP>> getObjectClasses() {
    return Collections.singletonList(AIP.class);
  }

  @Override
  public PluginType getType() {
    return PluginType.AIP_TO_AIP;
  }

  @Override
  public List<String> getCategories() {
    return Collections.singletonList(RodaConstants.PLUGIN_CATEGORY_NOT_LISTABLE);
  }

  @Override
  public Plugin<AIP> cloneMe() {
    return new DisassociateDisposalScheduleToAIPPlugin();
  }

  @Override
  public boolean areParameterValuesValid() {
    return true;
  }
}
