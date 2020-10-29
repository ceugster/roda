package org.roda.wui.api.controllers;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.exceptions.AlreadyExistsException;
import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.IllegalOperationException;
import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.exceptions.RODAException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.v2.index.select.SelectedItems;
import org.roda.core.data.v2.index.select.SelectedItemsList;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.roda.core.data.v2.ip.disposal.DisposalConfirmation;
import org.roda.core.data.v2.ip.disposal.DisposalHold;
import org.roda.core.data.v2.ip.disposal.DisposalRule;
import org.roda.core.data.v2.ip.disposal.DisposalSchedule;
import org.roda.core.data.v2.jobs.Job;
import org.roda.core.data.v2.log.LogEntryState;
import org.roda.core.data.v2.user.User;
import org.roda.wui.client.browse.bundle.DisposalConfirmationExtraBundle;
import org.roda.wui.common.ControllerAssistant;
import org.roda.wui.common.RodaWuiController;

/**
 * @author Miguel Guimarães <mguimaraes@keep.pt>
 */
public class Disposals extends RodaWuiController {

  private Disposals() {
    super();
  }

  /*
   * ---------------------------------------------------------------------------
   * ---------------- REST related methods - start -----------------------------
   * ---------------------------------------------------------------------------
   */
  public static DisposalSchedule createDisposalSchedule(User user, DisposalSchedule disposalSchedule)
    throws GenericException, AuthorizationDeniedException, RequestNotValidException, NotFoundException,
    AlreadyExistsException {
    final ControllerAssistant controllerAssistant = new ControllerAssistant() {};

    // check user permissions
    controllerAssistant.checkRoles(user);

    // validate disposal schedule
    DisposalsHelper.validateDisposalSchedule(disposalSchedule);

    LogEntryState state = LogEntryState.SUCCESS;

    try {
      return BrowserHelper.createDisposalSchedule(disposalSchedule, user);
    } catch (RODAException e) {
      state = LogEntryState.FAILURE;
      throw e;
    } finally {
      // register action
      controllerAssistant.registerAction(user, state, RodaConstants.CONTROLLER_DISPOSAL_SCHEDULE_PARAM,
        disposalSchedule);
    }
  }

  public static DisposalSchedule updateDisposalSchedule(User user, DisposalSchedule disposalSchedule)
    throws AuthorizationDeniedException, GenericException, NotFoundException, RequestNotValidException {
    final ControllerAssistant controllerAssistant = new ControllerAssistant() {};

    // check user permissions
    controllerAssistant.checkRoles(user);

    // validate disposal schedule
    DisposalsHelper.validateDisposalSchedule(disposalSchedule);

    LogEntryState state = LogEntryState.SUCCESS;

    try {
      // delegate
      return BrowserHelper.updateDisposalSchedule(disposalSchedule, user);
    } catch (RODAException e) {
      state = LogEntryState.FAILURE;
      throw e;
    } finally {
      // register action
      controllerAssistant.registerAction(user, disposalSchedule.getId(), state,
        RodaConstants.CONTROLLER_DISPOSAL_SCHEDULE_PARAM, disposalSchedule);
    }
  }

  public static void deleteDisposalSchedule(User user, String disposalScheduleId) throws RequestNotValidException,
    GenericException, NotFoundException, AuthorizationDeniedException, IllegalOperationException {
    ControllerAssistant controllerAssistant = new ControllerAssistant() {};

    // check user permissions
    controllerAssistant.checkRoles(user);

    LogEntryState state = LogEntryState.SUCCESS;

    try {
      // delegate
      BrowserHelper.deleteDisposalSchedule(disposalScheduleId);
    } catch (RODAException e) {
      state = LogEntryState.FAILURE;
      throw e;
    } finally {
      // register action
      controllerAssistant.registerAction(user, disposalScheduleId, state,
        RodaConstants.CONTROLLER_DISPOSAL_SCHEDULE_ID_PARAM, disposalScheduleId);
    }
  }

  public static DisposalHold createDisposalHold(User user, DisposalHold disposalHold) throws GenericException,
    AuthorizationDeniedException, RequestNotValidException, NotFoundException, AlreadyExistsException {
    final ControllerAssistant controllerAssistant = new ControllerAssistant() {};

    // check user permissions
    controllerAssistant.checkRoles(user);

    // validate disposal hold
    DisposalsHelper.validateDisposalHold(disposalHold);

    LogEntryState state = LogEntryState.SUCCESS;

    try {
      return BrowserHelper.createDisposalHold(disposalHold, user);
    } catch (RODAException e) {
      state = LogEntryState.FAILURE;
      throw e;
    } finally {
      // register action
      controllerAssistant.registerAction(user, state, RodaConstants.CONTROLLER_DISPOSAL_HOLD_PARAM, disposalHold);
    }
  }

  public static DisposalHold updateDisposalHold(User user, DisposalHold disposalHold)
    throws AuthorizationDeniedException, GenericException, NotFoundException, RequestNotValidException {
    final ControllerAssistant controllerAssistant = new ControllerAssistant() {};

    // check user permissions
    controllerAssistant.checkRoles(user);

    // validate disposal hold
    DisposalsHelper.validateDisposalHold(disposalHold);

    LogEntryState state = LogEntryState.SUCCESS;

    try {
      // delegate
      return BrowserHelper.updateDisposalHold(disposalHold, user);
    } catch (RODAException e) {
      state = LogEntryState.FAILURE;
      throw e;
    } finally {
      // register action
      controllerAssistant.registerAction(user, disposalHold.getId(), state,
        RodaConstants.CONTROLLER_DISPOSAL_HOLD_PARAM, disposalHold);
    }
  }

  public static void deleteDisposalHold(User user, String disposalHoldId) throws RequestNotValidException,
    GenericException, NotFoundException, AuthorizationDeniedException, IllegalOperationException {
    ControllerAssistant controllerAssistant = new ControllerAssistant() {};

    // check user permissions
    controllerAssistant.checkRoles(user);

    LogEntryState state = LogEntryState.SUCCESS;

    try {
      // delegate
      BrowserHelper.deleteDisposalHold(disposalHoldId);
    } catch (RODAException e) {
      state = LogEntryState.FAILURE;
      throw e;
    } finally {
      // register action
      controllerAssistant.registerAction(user, disposalHoldId, state, RodaConstants.CONTROLLER_DISPOSAL_HOLD_ID_PARAM,
        disposalHoldId);
    }
  }

  public static DisposalConfirmation recoverDisposalConfirmation(User user, String disposalConfirmationId) {
    return null;
  }

  public static Job createDisposalConfirmation(User user, String title,
    DisposalConfirmationExtraBundle confirmationMetadata, SelectedItems<IndexedAIP> objects)
    throws AuthorizationDeniedException, NotFoundException, GenericException, RequestNotValidException {
    final ControllerAssistant controllerAssistant = new ControllerAssistant() {};

    // check user permissions
    controllerAssistant.checkRoles(user);

    LogEntryState state = LogEntryState.SUCCESS;

    try {
      return BrowserHelper.createDisposalConfirmationReport(user, objects, title, confirmationMetadata);
    } catch (RODAException e) {
      state = LogEntryState.FAILURE;
      throw e;
    } finally {
      // register action
      controllerAssistant.registerAction(user, state, RodaConstants.CONTROLLER_DISPOSAL_CONFIRMATION_METADATA_PARAM,
        confirmationMetadata);
    }
  }

  public static DisposalConfirmationExtraBundle retrieveDisposalConfirmationExtraBundle(User user)
    throws AuthorizationDeniedException {
    final ControllerAssistant controllerAssistant = new ControllerAssistant() {};

    // check user permissions
    controllerAssistant.checkRoles(user);

    LogEntryState state = LogEntryState.SUCCESS;
    try {
      return BrowserHelper.retrieveDisposalConfirmationExtraBundle();
    } finally {
      // register action
      controllerAssistant.registerAction(user, state);
    }
  }

  public static Job deleteDisposalConfirmation(User user, SelectedItems<DisposalConfirmation> selectedItems,
    String details) throws RequestNotValidException, GenericException, NotFoundException, AuthorizationDeniedException {
    ControllerAssistant controllerAssistant = new ControllerAssistant() {};

    // check user permissions
    controllerAssistant.checkRoles(user);

    LogEntryState state = LogEntryState.SUCCESS;

    try {
      // delegate
      return BrowserHelper.deleteDisposalConfirmation(user, selectedItems, details);
    } catch (RODAException e) {
      state = LogEntryState.FAILURE;
      throw e;
    } finally {
      // register action
      controllerAssistant.registerAction(user, state, RodaConstants.CONTROLLER_SELECTED_PARAM, selectedItems);
    }
  }

  public static Job associateDisposalSchedule(User user, SelectedItems<IndexedAIP> selected, String disposalScheduleId,
    Boolean applyToHierarchy, Boolean overwriteAll)
    throws AuthorizationDeniedException, GenericException, NotFoundException, RequestNotValidException {
    final ControllerAssistant controllerAssistant = new ControllerAssistant() {};

    // check user permissions
    controllerAssistant.checkRoles(user);

    LogEntryState state = LogEntryState.SUCCESS;
    try {
      // delegate
      return BrowserHelper.associateDisposalSchedule(user, selected, disposalScheduleId, applyToHierarchy, overwriteAll);
    } catch (RODAException e) {
      state = LogEntryState.FAILURE;
      throw e;
    } finally {
      // register action
      controllerAssistant.registerAction(user, state, RodaConstants.CONTROLLER_SELECTED_PARAM, selected,
        RodaConstants.CONTROLLER_DISPOSAL_SCHEDULE_ID_PARAM, disposalScheduleId);
    }
  }

  public static Job disassociateDisposalSchedule(User user, SelectedItems<IndexedAIP> selected)
    throws AuthorizationDeniedException, GenericException, NotFoundException, RequestNotValidException {
    final ControllerAssistant controllerAssistant = new ControllerAssistant() {};

    // check user permissions
    controllerAssistant.checkRoles(user);

    LogEntryState state = LogEntryState.SUCCESS;
    try {
      // delegate
      return BrowserHelper.disassociateDisposalSchedule(user, selected);
    } catch (RODAException e) {
      state = LogEntryState.FAILURE;
      throw e;
    } finally {
      // register action
      controllerAssistant.registerAction(user, state, RodaConstants.CONTROLLER_SELECTED_PARAM, selected);
    }
  }

  public static Job destroyRecordsInDisposalConfirmationReport(User user,
    SelectedItemsList<DisposalConfirmation> selectedItems)
    throws AuthorizationDeniedException, GenericException, NotFoundException, RequestNotValidException {
    final ControllerAssistant controllerAssistant = new ControllerAssistant() {};

    // check user permissions
    controllerAssistant.checkRoles(user);

    LogEntryState state = LogEntryState.SUCCESS;
    try {
      // delegate
      return BrowserHelper.destroyRecordsInDisposalConfirmation(user, selectedItems);
    } catch (RODAException e) {
      state = LogEntryState.FAILURE;
      throw e;
    } finally {
      // register action
      controllerAssistant.registerAction(user, state, RodaConstants.CONTROLLER_SELECTED_PARAM, selectedItems);
    }
  }

  public static void recoverRecordsInDisposalConfirmationReport() {

  }

  public static void permanentlyDeleteRecordsInDisposalConfirmationReport() {
  }

  public static Job applyDisposalHold(User user, SelectedItems<IndexedAIP> items, String disposalHoldId)
    throws AuthorizationDeniedException, GenericException, NotFoundException, RequestNotValidException {
    final ControllerAssistant controllerAssistant = new ControllerAssistant() {};

    // check user permissions
    controllerAssistant.checkRoles(user);

    LogEntryState state = LogEntryState.SUCCESS;
    try {
      // delegate
      return BrowserHelper.applyDisposalHold(user, items, disposalHoldId);
    } catch (RODAException e) {
      state = LogEntryState.FAILURE;
      throw e;
    } finally {
      // register action
      controllerAssistant.registerAction(user, state, RodaConstants.CONTROLLER_SELECTED_PARAM, items,
        RodaConstants.CONTROLLER_DISPOSAL_HOLD_ID_PARAM, disposalHoldId);
    }
  }

  public static Job liftDisposalHold(User user, SelectedItems<DisposalHold> items)
    throws AuthorizationDeniedException, GenericException, NotFoundException, RequestNotValidException {
    final ControllerAssistant controllerAssistant = new ControllerAssistant() {};

    // check user permissions
    controllerAssistant.checkRoles(user);

    LogEntryState state = LogEntryState.SUCCESS;
    try {
      // delegate
      return BrowserHelper.liftDisposalHold(user, items);
    } catch (RODAException e) {
      state = LogEntryState.FAILURE;
      throw e;
    } finally {
      // register action
      controllerAssistant.registerAction(user, state, RodaConstants.CONTROLLER_SELECTED_PARAM, items);
    }
  }

  public static DisposalRule createDisposalRule(User user, DisposalRule disposalRule) throws GenericException,
    AuthorizationDeniedException, RequestNotValidException, NotFoundException, AlreadyExistsException {
    final ControllerAssistant controllerAssistant = new ControllerAssistant() {};

    // check user permissions
    controllerAssistant.checkRoles(user);

    // validate disposal rule
    // DisposalsHelper.validateDisposalRule(disposalRule);

    LogEntryState state = LogEntryState.SUCCESS;

    try {
      return BrowserHelper.createDisposalRule(disposalRule, user);
    } catch (RODAException e) {
      state = LogEntryState.FAILURE;
      throw e;
    } finally {
      // register action
      controllerAssistant.registerAction(user, state, RodaConstants.CONTROLLER_DISPOSAL_RULE_PARAM, disposalRule);
    }
  }

  public static DisposalRule updateDisposalRule(User user, DisposalRule disposalRule)
    throws AuthorizationDeniedException, GenericException, NotFoundException, RequestNotValidException {
    final ControllerAssistant controllerAssistant = new ControllerAssistant() {};

    // check user permissions
    controllerAssistant.checkRoles(user);

    // validate disposal rule
    // DisposalsHelper.validateDisposalRule(disposalRule);

    LogEntryState state = LogEntryState.SUCCESS;

    try {
      // delegate
      return BrowserHelper.updateDisposalRule(disposalRule, user);
    } catch (RODAException e) {
      state = LogEntryState.FAILURE;
      throw e;
    } finally {
      // register action
      controllerAssistant.registerAction(user, disposalRule.getId(), state,
        RodaConstants.CONTROLLER_DISPOSAL_RULE_PARAM, disposalRule);
    }
  }

  public static void deleteDisposalRule(User user, String disposalRuleId) throws RequestNotValidException,
    GenericException, NotFoundException, AuthorizationDeniedException, IllegalOperationException {
    ControllerAssistant controllerAssistant = new ControllerAssistant() {};

    // check user permissions
    controllerAssistant.checkRoles(user);

    LogEntryState state = LogEntryState.SUCCESS;

    try {
      // delegate
      BrowserHelper.deleteDisposalRule(disposalRuleId);
    } catch (RODAException e) {
      state = LogEntryState.FAILURE;
      throw e;
    } finally {
      // register action
      controllerAssistant.registerAction(user, disposalRuleId, state, RodaConstants.CONTROLLER_DISPOSAL_RULE_ID_PARAM,
        disposalRuleId);
    }
  }

}
