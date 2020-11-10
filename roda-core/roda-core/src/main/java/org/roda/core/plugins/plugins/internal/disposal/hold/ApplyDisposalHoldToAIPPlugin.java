package org.roda.core.plugins.plugins.internal.disposal.hold;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.roda.core.RodaCoreFactory;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.AlreadyExistsException;
import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.InvalidParameterException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.v2.LiteOptionalWithCause;
import org.roda.core.data.v2.ip.AIP;
import org.roda.core.data.v2.ip.disposal.DisposalHold;
import org.roda.core.data.v2.jobs.Job;
import org.roda.core.data.v2.jobs.PluginParameter;
import org.roda.core.data.v2.jobs.PluginState;
import org.roda.core.data.v2.jobs.PluginType;
import org.roda.core.data.v2.jobs.Report;
import org.roda.core.data.v2.validation.ValidationException;
import org.roda.core.index.IndexService;
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
public class ApplyDisposalHoldToAIPPlugin extends AbstractPlugin<AIP> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ApplyDisposalHoldToAIPPlugin.class);

  private String disposalHoldId;

  private static final Map<String, PluginParameter> pluginParameters = new HashMap<>();

  static {
    pluginParameters.put(RodaConstants.PLUGIN_PARAMS_DISPOSAL_HOLD_ID,
      new PluginParameter(RodaConstants.PLUGIN_PARAMS_DISPOSAL_HOLD_ID, "Disposal hold id",
        PluginParameter.PluginParameterType.STRING, "", true, false, "Disposal hold identifier"));
  }

  @Override
  public List<PluginParameter> getParameters() {
    ArrayList<PluginParameter> parameters = new ArrayList<>();
    parameters.add(pluginParameters.get(RodaConstants.PLUGIN_PARAMS_DISPOSAL_HOLD_ID));
    return parameters;
  }

  @Override
  public void setParameterValues(Map<String, String> parameters) throws InvalidParameterException {
    super.setParameterValues(parameters);
    if (parameters.containsKey(RodaConstants.PLUGIN_PARAMS_DISPOSAL_HOLD_ID)) {
      disposalHoldId = parameters.get(RodaConstants.PLUGIN_PARAMS_DISPOSAL_HOLD_ID);
    }
  }

  @Override
  public String getVersionImpl() {
    return "1.0";
  }

  public static String getStaticName() {
    return "Apply disposal hold";
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
  public RodaConstants.PreservationEventType getPreservationEventType() {
    return RodaConstants.PreservationEventType.POLICY_ASSIGNMENT;
  }

  @Override
  public String getPreservationEventDescription() {
    return "Apply disposal hold to AIP";
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
    return PluginHelper.processObjects(this, new RODAObjectsProcessingLogic<AIP>() {
      @Override
      public void process(IndexService index, ModelService model, StorageService storage, Report report, Job cachedJob,
        JobPluginInfo jobPluginInfo, Plugin<AIP> plugin, List<AIP> objects) {
        processAIP(index, model, objects, report, jobPluginInfo, cachedJob);
      }
    }, index, model, storage, liteList);
  }

  private void processAIP(IndexService index, ModelService model, List<AIP> aips, Report report,
    JobPluginInfo jobPluginInfo, Job cachedJob) {
    LOGGER.debug("Applying disposal hold {}", disposalHoldId);

    // Uses cache to retrieve the disposal hold
    DisposalHold disposalHold = RodaCoreFactory.getDisposalHold(disposalHoldId);

    if (disposalHold == null) {
      LOGGER.error("Failed to retrieve disposal hold from model");

      for (AIP aip : aips) {
        Report reportItem = PluginHelper.initPluginReportItem(this, aip.getId(), AIP.class);
        PluginHelper.updatePartialJobReport(this, model, reportItem, false, cachedJob);
        reportItem.setPluginState(PluginState.FAILURE)
          .setPluginDetails("Failed to retrieve disposal hold " + disposalHoldId);
        jobPluginInfo.incrementObjectsProcessedWithFailure();
        report.addReport(reportItem);
        PluginHelper.updatePartialJobReport(this, model, reportItem, true, cachedJob);
      }
    } else {
      for (AIP aip : aips) {
        Report reportItem = PluginHelper.initPluginReportItem(this, aip.getId(), AIP.class);
        PluginHelper.updatePartialJobReport(this, model, reportItem, false, cachedJob);

        PluginState state = PluginState.SUCCESS;
        String outcomeText;
        LOGGER.debug("Processing AIP {}", aip.getId());

        if (aip.getDisposalConfirmationId() != null) {
          state = PluginState.FAILURE;
          LOGGER.error("Error applying disposal hold to AIP '" + aip.getId()
            + "': This AIP is part of a disposal confirmation report and an hold cannot be applied");
          jobPluginInfo.incrementObjectsProcessedWithFailure();
          reportItem.setPluginState(state).setPluginDetails("Error applying disposal hold to AIP '" + aip.getId()
            + "': This AIP is part of a disposal confirmation report and an hold cannot be applied");
          outcomeText = PluginHelper.createOutcomeTextForDisposalHold(
            "failed to be applied to AIP '" + aip.getId()
              + "'; This AIP is part of a disposal confirmation report and an hold cannot be applied",
            disposalHoldId, null);
        } else {
          // check if AIP is already under the disposal hold
          if (aip.isAIPOnHold(disposalHoldId)) {
            LOGGER.info("Applying disposal hold to AIP '" + aip.getId()
              + "' was skipped because it is already on the same disposal hold");
            jobPluginInfo.incrementObjectsProcessed(state);
            reportItem.setPluginState(state).setPluginDetails("Applying disposal hold to AIP '" + aip.getId()
              + "' was skipped because it is already on the same disposal hold");
            outcomeText = "Applying disposal hold to AIP '" + aip.getId()
              + "' was skipped because it is already on the same disposal hold";
          } else {
            aip.addDisposalHold(
              DisposalHoldPluginUtils.createDisposalHoldAssociation(disposalHoldId, cachedJob.getUsername()));

            try {
              model.updateAIP(aip, cachedJob.getUsername());
              disposalHold.setFirstTimeUsed(new Date());
              model.updateDisposalHold(disposalHold, cachedJob.getUsername());

              jobPluginInfo.incrementObjectsProcessedWithSuccess();

              reportItem.setPluginState(state)
                .setPluginDetails("Disposal hold '" + disposalHoldId + "' was successfully applied to AIP");

              outcomeText = PluginHelper.createOutcomeTextForDisposalHold("was successfully applied to AIP",
                disposalHoldId, disposalHold.getTitle());
            } catch (NotFoundException | RequestNotValidException | AuthorizationDeniedException | GenericException e) {
              LOGGER.error("Error applying disposal hold {} to {}: {}", disposalHoldId, aip.getId(), e.getMessage(), e);
              state = PluginState.FAILURE;
              jobPluginInfo.incrementObjectsProcessedWithFailure();
              reportItem.setPluginState(state)
                .setPluginDetails("Error associating disposal schedule " + aip.getId() + ": " + e.getMessage());
              outcomeText = PluginHelper.createOutcomeTextForDisposalHold(" failed to be applied to AIP",
                disposalHoldId, disposalHold.getTitle());
            }
          }
        }

        report.addReport(reportItem);
        PluginHelper.updatePartialJobReport(this, model, reportItem, true, cachedJob);

        try {
          PluginHelper.createPluginEvent(this, aip.getId(), model, index, null, null, state, outcomeText, true,
            cachedJob);
        } catch (ValidationException | RequestNotValidException | NotFoundException | GenericException
          | AuthorizationDeniedException | AlreadyExistsException e) {
          LOGGER.error("Error creating event: {}", e.getMessage(), e);
        }
      }
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
    return new ApplyDisposalHoldToAIPPlugin();
  }

  @Override
  public boolean areParameterValuesValid() {
    return true;
  }
}
