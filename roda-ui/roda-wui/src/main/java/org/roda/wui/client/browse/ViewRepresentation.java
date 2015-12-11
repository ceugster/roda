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
package org.roda.wui.client.browse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.httpclient.HttpStatus;
import org.roda.core.data.adapter.filter.Filter;
import org.roda.core.data.adapter.filter.SimpleFilterParameter;
import org.roda.core.data.adapter.sort.Sorter;
import org.roda.core.data.adapter.sublist.Sublist;
import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.IndexResult;
import org.roda.core.data.v2.Representation;
import org.roda.core.data.v2.RepresentationState;
import org.roda.core.data.v2.SimpleDescriptionObject;
import org.roda.core.data.v2.SimpleFile;
import org.roda.wui.client.common.UserLogin;
import org.roda.wui.client.common.lists.FileList;
import org.roda.wui.client.main.BreadcrumbItem;
import org.roda.wui.client.main.BreadcrumbPanel;
import org.roda.wui.common.client.ClientLogger;
import org.roda.wui.common.client.HistoryResolver;
import org.roda.wui.common.client.tools.JavascriptUtils;
import org.roda.wui.common.client.tools.RestUtils;
import org.roda.wui.common.client.tools.Tools;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.media.client.Audio;
import com.google.gwt.media.client.Video;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;

import config.i18n.client.BrowseMessages;

/**
 * @author Luis Faria
 * 
 */
public class ViewRepresentation extends Composite {

  public static final HistoryResolver RESOLVER = new HistoryResolver() {

    @Override
    public void resolve(final List<String> historyTokens, final AsyncCallback<Widget> callback) {
      if (historyTokens.size() > 1) {
        final String aipId = historyTokens.get(0);
        final String representationId = historyTokens.get(1);

        BrowserService.Util.getInstance().getItemBundle(aipId, LocaleInfo.getCurrentLocale().getLocaleName(),
          new AsyncCallback<BrowseItemBundle>() {

          @Override
          public void onFailure(Throwable caught) {
            errorRedirect(callback);
          }

          @Override
          public void onSuccess(final BrowseItemBundle itemBundle) {
            if (itemBundle != null && verifyRepresentation(itemBundle.getRepresentations(), representationId)) {
              if (historyTokens.size() > 2) {
                final String fileId = historyTokens.get(2);

                Filter filter = new Filter();
                filter.add(new SimpleFilterParameter(RodaConstants.FILE_AIPID, aipId));
                filter.add(new SimpleFilterParameter(RodaConstants.FILE_REPRESENTATIONID, representationId));
                filter.add(new SimpleFilterParameter(RodaConstants.FILE_FILEID, fileId));

                BrowserService.Util.getInstance().getRepresentationFiles(filter, new Sorter(), new Sublist(), null,
                  LocaleInfo.getCurrentLocale().getLocaleName(), new AsyncCallback<IndexResult<SimpleFile>>() {

                  @Override
                  public void onSuccess(IndexResult<SimpleFile> result) {
                    if (result.getResults().size() == 1) {
                      SimpleFile simpleFile = result.getResults().get(0);
                      ViewRepresentation view;
                      if (simpleFile.isFile()) {
                        view = new ViewRepresentation(aipId, itemBundle, representationId, fileId, simpleFile);
                      } else {
                        view = new ViewRepresentation(aipId, itemBundle, representationId, fileId);
                      }
                      callback.onSuccess(view);
                    } else {
                      errorRedirect(callback);
                    }
                  }

                  @Override
                  public void onFailure(Throwable caught) {
                    errorRedirect(callback);
                  }
                });

              } else {
                ViewRepresentation view = new ViewRepresentation(aipId, itemBundle, representationId);
                callback.onSuccess(view);
              }
            } else {
              errorRedirect(callback);
            }
          }
        });
      } else {
        errorRedirect(callback);
      }
    }

    @Override
    public void isCurrentUserPermitted(AsyncCallback<Boolean> callback) {
      UserLogin.getInstance().checkRoles(new HistoryResolver[] {Browse.RESOLVER}, false, callback);
    }

    public List<String> getHistoryPath() {
      return Tools.concat(Browse.RESOLVER.getHistoryPath(), getHistoryToken());
    }

    public String getHistoryToken() {
      return "view";
    }

    private boolean verifyRepresentation(List<Representation> representations, String representationId) {
      boolean exist = false;
      for (Representation representation : representations) {
        if (representation.getId().equals(representationId)) {
          exist = true;
        }
      }
      return exist;
    }

    private void errorRedirect(AsyncCallback<Widget> callback) {
      Tools.newHistory(Browse.RESOLVER);
      callback.onSuccess(null);
    }
  };

  interface MyUiBinder extends UiBinder<Widget, ViewRepresentation> {
  }

  private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

  // @SuppressWarnings("unused")
  private ClientLogger logger = new ClientLogger(getClass().getName());

  private static final BrowseMessages messages = GWT.create(BrowseMessages.class);

  private String aipId;
  private BrowseItemBundle itemBundle;
  private String representationId;
  @SuppressWarnings("unused")
  private String fileId;
  private SimpleFile simpleFile = null;

  static final int WINDOW_WIDTH = 1200;

  @UiField
  BreadcrumbPanel breadcrumb;

  @UiField
  HorizontalPanel previewPanel;

  @UiField(provided = true)
  FileList filesPanel;

  @UiField
  FlowPanel filePreview;

  @UiField
  Button back;

  @UiField
  Button nextFile;

  @UiField
  Button previousFile;

  @UiField
  Button downloadFile;

  /**
   * Create a new panel to view a representation
   * 
   * @param aipId
   * @param itemBundle
   * @param representationId
   * 
   */
  public ViewRepresentation(String aipId, BrowseItemBundle itemBundle, String representationId) {
    this(aipId, itemBundle, representationId, null, null);
  }

  /**
   * Create a new panel to view a representation
   * 
   * @param aipId
   * @param itemBundle
   * @param representationId
   * @param fileId
   * 
   */
  public ViewRepresentation(String aipId, BrowseItemBundle itemBundle, String representationId, String fileId) {
    this(aipId, itemBundle, representationId, fileId, null);
  }

  /**
   * Create a new panel to view a representation
   * 
   * @param aipId
   * @param itemBundle
   * @param representationId
   * @param fileId
   * @param simpleFile
   * 
   */
  public ViewRepresentation(String aipId, BrowseItemBundle itemBundle, String representationId, String fileId,
    SimpleFile simpleFile) {
    this.aipId = aipId;
    this.itemBundle = itemBundle;
    this.representationId = representationId;
    this.fileId = fileId;
    this.simpleFile = simpleFile;

    Filter filter = new Filter();
    filter.add(new SimpleFilterParameter(RodaConstants.FILE_AIPID, aipId));
    filter.add(new SimpleFilterParameter(RodaConstants.FILE_REPRESENTATIONID, representationId));
    /* TODO add fileId as a filter */
    filesPanel = new FileList(filter, null, null);

    initWidget(uiBinder.createAndBindUi(this));

    breadcrumb.updatePath(getBreadcrumbs(itemBundle, simpleFile));
    breadcrumb.setVisible(true);

    back.setText(messages.backButton());
    nextFile.setText(messages.viewRepresentationNextFileButton());
    previousFile.setText(messages.viewRepresentationPreviousFileButton());
    downloadFile.setText(messages.viewRepresentationDownloadFileButton());

    downloadFile.setEnabled(false);

    /* TODO Controll next and previous file button */
    nextFile.setVisible(false);
    previousFile.setVisible(false);

    filesPanel.getSelectionModel().addSelectionChangeHandler(new Handler() {

      @Override
      public void onSelectionChange(SelectionChangeEvent event) {
        if (Window.getClientWidth() < WINDOW_WIDTH) {
          view();
        } else {
          changeURL();
          filePreview();
        }
      }
    });

    previewPanel.addStyleName("viewRepresentationPreviewPanel");
    filesPanel.addStyleName("viewRepresentationFilesPanel");
    filePreview.addStyleName("viewRepresentationFilePreview");
    previewPanel.setCellWidth(filePreview, "100%");

    panelsControl();

    Window.addResizeHandler(new ResizeHandler() {

      @Override
      public void onResize(ResizeEvent event) {
        panelsControl();
      }
    });

    filePreview();

  }

  private void changeURL() {
    String url = Window.Location.createUrlBuilder().buildString();
    String viewUrl = url.substring(url.indexOf("view/"));
    logger.debug(viewUrl);
    if (viewUrl.split("/").length == 3) {
      url = url.replace(viewUrl, viewUrl + "/" + filesPanel.getSelectionModel().getSelectedObject().getId());
    } else {
      url = url.replace(viewUrl, viewUrl.substring(0, viewUrl.lastIndexOf("/")) + "/"
        + filesPanel.getSelectionModel().getSelectedObject().getId());
    }
    JavascriptUtils.updateURLWithoutReloading(url);
  }

  private List<BreadcrumbItem> getBreadcrumbs(BrowseItemBundle itemBundle, SimpleFile simpleFile) {
    List<BreadcrumbItem> ret = new ArrayList<>();
    SimpleDescriptionObject sdo = itemBundle.getSdo();
    List<Representation> representations = itemBundle.getRepresentations();
    Representation rep = selectRepresentation(representations, representationId);

    ret.add(new BreadcrumbItem(
      getBreadcrumbLabel((sdo.getTitle() != null) ? sdo.getTitle() : sdo.getId(), "description-level-representation"),
      Tools.concat(Browse.RESOLVER.getHistoryPath(), aipId)));
    ret.add(new BreadcrumbItem(getBreadcrumbLabel(representationType(rep), "representation"),
      Tools.concat(ViewRepresentation.RESOLVER.getHistoryPath(), aipId, representationId)));

    if (simpleFile != null) {
      for (String folder : simpleFile.getPath()) {
        if (!(folder.equals(aipId) || folder.equals(representationId) || folder.equals(simpleFile.getId())
          || folder.isEmpty())) {
          ret.add(new BreadcrumbItem(getBreadcrumbLabel(folder, "folder"),
            Tools.concat(ViewRepresentation.RESOLVER.getHistoryPath(), aipId, representationId, folder)));
        }
      }

      ret.add(new BreadcrumbItem(getBreadcrumbLabel(simpleFile.getOriginalName(), "file"),
        Tools.concat(ViewRepresentation.RESOLVER.getHistoryPath(), aipId, representationId, simpleFile.getId())));
    }

    return ret;
  }

  private Representation selectRepresentation(List<Representation> representations, String representationId) {
    Representation rep = null;
    for (Representation representation : representations) {
      if (representation.getId().equals(representationId)) {
        rep = representation;
      }
    }
    return rep;
  }

  private String representationType(Representation rep) {
    SafeHtml labelText;
    Set<RepresentationState> statuses = rep.getStatuses();
    if (statuses.containsAll(Arrays.asList(RepresentationState.ORIGINAL, RepresentationState.NORMALIZED))) {
      labelText = messages.downloadTitleOriginalAndNormalized();
    } else if (statuses.contains(RepresentationState.ORIGINAL)) {
      labelText = messages.downloadTitleOriginal();
    } else if (statuses.contains(RepresentationState.NORMALIZED)) {
      labelText = messages.downloadTitleNormalized();
    } else {
      labelText = messages.downloadTitleDefault();
    }
    return labelText.asString();
  }

  private SafeHtml getBreadcrumbLabel(String label, String level) {
    SafeHtml elementLevelIconSafeHtml = getElementLevelIconSafeHtml(level);
    SafeHtmlBuilder builder = new SafeHtmlBuilder();
    builder.append(elementLevelIconSafeHtml).append(SafeHtmlUtils.fromString(label));
    SafeHtml breadcrumbLabel = builder.toSafeHtml();
    return breadcrumbLabel;
  }

  private SafeHtml getElementLevelIconSafeHtml(String level) {
    SafeHtml icon;
    if (level.equals("description-level-representation")) {
      icon = SafeHtmlUtils.fromSafeConstant("<i class='description-level description-level-representational'></i>");
    } else if (level.equals("representation")) {
      icon = SafeHtmlUtils.fromSafeConstant("<i class='fa fa-files-o'></i>");
    } else if (level.equals("folder")) {
      icon = SafeHtmlUtils.fromSafeConstant("<i class='fa fa-folder-o'></i>");
    } else {
      icon = SafeHtmlUtils.fromSafeConstant("<i class='fa fa-file-o'></i>");
    }
    return icon;
  }

  @UiHandler("back")
  void buttonBackHandler(ClickEvent e) {
    Tools.newHistory(Tools.concat(ViewRepresentation.RESOLVER.getHistoryPath(), aipId, representationId));
  }

  @UiHandler("nextFile")
  void buttonNextFileHandler(ClickEvent e) {
    filesPanel.nextItemSelection();
  }

  @UiHandler("previousFile")
  void buttonPreviousFileHandler(ClickEvent e) {
    filesPanel.previousItemSelection();
  }

  @UiHandler("downloadFile")
  void buttonDownloadFileHandler(ClickEvent e) {
    SafeUri downloadUri = null;
    if (simpleFile != null) {
      downloadUri = RestUtils.createRepresentationFileDownloadUri(aipId, representationId, simpleFile.getId());
    } else if (filesPanel.getSelectionModel().getSelectedObject() != null) {
      downloadUri = RestUtils.createRepresentationFileDownloadUri(aipId, representationId,
        filesPanel.getSelectionModel().getSelectedObject().getId());
    }
    if (downloadUri != null) {
      Window.Location.assign(downloadUri.asString());
    }
  }

  private void panelsControl() {
    if (simpleFile == null) {
      if (Window.getClientWidth() < WINDOW_WIDTH) {
        hideFilePreview();
      } else {
        showFilePreview();
      }
    } else {
      if (Window.getClientWidth() < WINDOW_WIDTH) {
        hideFilesList();
      } else {
        showFilesList();
      }
    }
  }

  private void hideFilesList() {
    filesPanel.setVisible(false);
  }

  private void showFilesList() {
    filesPanel.setVisible(true);
  }

  private void showFilePreview() {
    filesPanel.removeStyleName("fullWidth");
    previewPanel.setCellWidth(filePreview, "100%");
    filePreview.setVisible(true);
  }

  private void hideFilePreview() {
    filesPanel.addStyleName("fullWidth");
    previewPanel.setCellWidth(filePreview, "0px");
    filePreview.setVisible(false);
  }

  private void view() {
    Tools.newHistory(Browse.RESOLVER, ViewRepresentation.RESOLVER.getHistoryToken(), aipId, representationId,
      filesPanel.getSelectionModel().getSelectedObject().getId());
  }

  private void filePreview() {
    filePreview.clear();

    simpleFile = (filesPanel.getSelectionModel().getSelectedObject() != null)
      ? filesPanel.getSelectionModel().getSelectedObject() : simpleFile;
    if (simpleFile != null && simpleFile.getOriginalName() != null) {
      breadcrumb.updatePath(getBreadcrumbs(itemBundle, simpleFile));
      downloadFile.setEnabled(true);

      if (simpleFile.getOriginalName().toLowerCase().contains(".png")
        || simpleFile.getOriginalName().toLowerCase().contains(".jpg")) {
        imagePreview(simpleFile);
      } else if (simpleFile.getOriginalName().toLowerCase().contains(".pdf")) {
        pdfPreview(simpleFile);
      } else if (simpleFile.getOriginalName().toLowerCase().contains(".xml")) {
        textPreview(simpleFile);
      } else if (simpleFile.getOriginalName().toLowerCase().contains(".mp3")) {
        audioPreview(simpleFile);
      } else if (simpleFile.getOriginalName().toLowerCase().contains(".mp4")) {
        videoPreview(simpleFile);
      } else {
        notSupportedPreview();
      }
    } else if (simpleFile == null) {
      emptyPreview();
    } else {
      errorPreview();
    }
  }

  private void emptyPreview() {
    HTML html = new HTML();
    SafeHtmlBuilder b = new SafeHtmlBuilder();

    b.append(SafeHtmlUtils.fromSafeConstant("<i class='fa fa-file fa-5'></i>"));
    b.append(SafeHtmlUtils.fromSafeConstant("<h4 class='emptymessage'>"));
    b.append(SafeHtmlUtils.fromString(messages.viewRepresentationEmptyPreview()));
    b.append(SafeHtmlUtils.fromSafeConstant("</h4>"));

    html.setHTML(b.toSafeHtml());
    filePreview.add(html);
    html.setStyleName("viewRepresentationEmptyPreview");
  }

  private void errorPreview() {
    HTML html = new HTML();
    SafeHtmlBuilder b = new SafeHtmlBuilder();

    b.append(SafeHtmlUtils.fromSafeConstant("<i class='fa fa-exclamation-triangle fa-5'></i>"));
    b.append(SafeHtmlUtils.fromSafeConstant("<h4 class='errormessage'>"));
    b.append(SafeHtmlUtils.fromString(messages.viewRepresentationErrorPreview()));
    b.append(SafeHtmlUtils.fromSafeConstant("</h4>"));

    html.setHTML(b.toSafeHtml());
    filePreview.add(html);
    html.setStyleName("viewRepresentationErrorPreview");
  }

  private void notSupportedPreview() {
    HTML html = new HTML();
    SafeHtmlBuilder b = new SafeHtmlBuilder();

    b.append(SafeHtmlUtils.fromSafeConstant("<i class='fa fa-exclamation-triangle fa-5'></i>"));
    b.append(SafeHtmlUtils.fromSafeConstant("<h4 class='errormessage'>"));
    b.append(SafeHtmlUtils.fromString(messages.viewRepresentationNotSupportedPreview()));
    b.append(SafeHtmlUtils.fromSafeConstant("</h4>"));

    html.setHTML(b.toSafeHtml());
    filePreview.add(html);
    html.setStyleName("viewRepresentationNotSupportedPreview");
  }

  private void imagePreview(SimpleFile file) {
    Image image = new Image(RestUtils.createRepresentationFileDownloadUri(aipId, representationId, file.getId()));
    filePreview.add(image);
    image.setStyleName("viewRepresentationImageFilePreview");
  }

  private void pdfPreview(SimpleFile file) {
    String viewerHtml = GWT.getHostPageBaseURL() + "pdf/viewer.html?file=" + encode(GWT.getHostPageBaseURL()
      + RestUtils.createRepresentationFileDownloadUri(aipId, representationId, file.getId()).asString());

    Frame frame = new Frame(viewerHtml);
    filePreview.add(frame);
    frame.setStyleName("viewRepresentationPDFFilePreview");
  }

  private void textPreview(SimpleFile file) {
    RequestBuilder request = new RequestBuilder(RequestBuilder.GET,
      RestUtils.createRepresentationFileDownloadUri(aipId, representationId, file.getId()).asString());
    try {
      request.sendRequest(null, new RequestCallback() {

        @Override
        public void onResponseReceived(Request request, Response response) {
          if (response.getStatusCode() == HttpStatus.SC_OK) {
            HTML html = new HTML("<pre><code>" + SafeHtmlUtils.htmlEscape(response.getText()) + "</code></pre>");
            FlowPanel frame = new FlowPanel();
            frame.add(html);

            filePreview.add(frame);

            frame.setStyleName("viewRepresentationTextFilePreview");

            JavascriptUtils.runHighlighter(html.getElement());
            JavascriptUtils.slideToggle(html.getElement(), ".toggle-next");
            JavascriptUtils.smoothScroll(html.getElement());
          } else {
            errorPreview();
          }
        }

        @Override
        public void onError(Request request, Throwable exception) {
          errorPreview();
        }
      });
    } catch (RequestException e) {
      errorPreview();
    }
  }

  private void audioPreview(SimpleFile file) {
    Audio audioPlayer = Audio.createIfSupported();
    if (audioPlayer != null) {
      audioPlayer.addSource(
        RestUtils.createRepresentationFileDownloadUri(aipId, representationId, file.getId()).asString(), "audio/mpeg");
      audioPlayer.setControls(true);
      filePreview.add(audioPlayer);
      audioPlayer.addStyleName("viewRepresentationAudioFilePreview");
    } else {
      notSupportedPreview();
    }
  }

  private void videoPreview(SimpleFile file) {
    Video videoPlayer = Video.createIfSupported();
    if (videoPlayer != null) {
      videoPlayer.addSource(
        RestUtils.createRepresentationFileDownloadUri(aipId, representationId, file.getId()).asString(), "video/dvd");
      videoPlayer.setControls(true);
      filePreview.add(videoPlayer);
      videoPlayer.addStyleName("viewRepresentationAudioFilePreview");
    } else {
      notSupportedPreview();
    }
  }

  private String encode(String string) {
    return string.replace("?", "%3F").replace("=", "%3D");
  }
}
