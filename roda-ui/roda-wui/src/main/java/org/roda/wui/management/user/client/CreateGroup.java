/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
/**
 * 
 */
package org.roda.wui.management.user.client;

import java.util.List;

import org.roda.core.data.exceptions.NotFoundException;
import org.roda.core.data.v2.user.Group;
import org.roda.wui.client.common.UserLogin;
import org.roda.wui.common.client.HistoryResolver;
import org.roda.wui.common.client.tools.Tools;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import config.i18n.client.UserManagementMessages;

/**
 * @author Luis Faria
 * 
 */
public class CreateGroup extends Composite {

  public static final HistoryResolver RESOLVER = new HistoryResolver() {

    @Override
    public void resolve(List<String> historyTokens, final AsyncCallback<Widget> callback) {
      Group group = new Group();
      CreateGroup createGroup = new CreateGroup(group);
      callback.onSuccess(createGroup);
    }

    @Override
    public void isCurrentUserPermitted(AsyncCallback<Boolean> callback) {
      UserLogin.getInstance().checkRoles(new HistoryResolver[] {MemberManagement.RESOLVER}, false, callback);
    }

    public List<String> getHistoryPath() {
      return Tools.concat(MemberManagement.RESOLVER.getHistoryPath(), getHistoryToken());
    }

    public String getHistoryToken() {
      return "create_group";
    }
  };

  interface MyUiBinder extends UiBinder<Widget, CreateGroup> {
  }

  private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

  private final Group group;

  private static UserManagementMessages messages = (UserManagementMessages) GWT.create(UserManagementMessages.class);

  @UiField
  Button buttonApply;

  @UiField
  Button buttonCancel;

  @UiField(provided = true)
  GroupDataPanel groupDataPanel;

  /**
   * Create a new panel to create a group
   * 
   * @param group
   *          the group to create
   */
  public CreateGroup(Group group) {
    this.group = group;

    this.groupDataPanel = new GroupDataPanel(true, false, true);
    this.groupDataPanel.setGroup(group);

    initWidget(uiBinder.createAndBindUi(this));
  }

  @UiHandler("buttonApply")
  void buttonApplyHandler(ClickEvent e) {
    if (groupDataPanel.isChanged()) {
      if (groupDataPanel.isValid()) {
        final Group group = groupDataPanel.getGroup();

        UserManagementService.Util.getInstance().addGroup(group, new AsyncCallback<Void>() {

          public void onSuccess(Void result) {
            Tools.newHistory(MemberManagement.RESOLVER);
          }

          public void onFailure(Throwable caught) {
            errorMessage(caught);
          }
        });
      }
    } else {
      Tools.newHistory(MemberManagement.RESOLVER);
    }
  }

  @UiHandler("buttonCancel")
  void buttonCancelHandler(ClickEvent e) {
    cancel();
  }

  private void cancel() {
    Tools.newHistory(MemberManagement.RESOLVER);
  }

  private void errorMessage(Throwable caught) {
    if (caught instanceof NotFoundException) {
      Window.alert(messages.editGroupNotFound(group.getName()));
      cancel();
    } else {
      Window.alert(messages.editGroupFailure(CreateGroup.this.group.getName(), caught.getMessage()));
    }
  }
}
