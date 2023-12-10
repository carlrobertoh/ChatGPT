package ee.carlrobert.codegpt.ui;

import static com.intellij.openapi.ui.Messages.CANCEL;
import static com.intellij.openapi.ui.Messages.OK;
import static ee.carlrobert.codegpt.Icons.Default;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import com.intellij.execution.ExecutionBundle;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DoNotAskOption;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.Balloon.Position;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import ee.carlrobert.codegpt.CodeGPTBundle;
import ee.carlrobert.codegpt.conversations.ConversationsState;
import ee.carlrobert.codegpt.indexes.FolderStructureTreePanel;
import ee.carlrobert.codegpt.settings.configuration.ConfigurationState;
import ee.carlrobert.codegpt.settings.state.IncludedFilesSettingsState;
import ee.carlrobert.codegpt.util.EditorUtil;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;

public class OverlayUtil {

  public static Notification getDefaultNotification(String content, NotificationType type) {
    return new Notification("CodeGPT Notification Group", "CodeGPT", content, type);
  }

  public static void showNotification(String content, NotificationType type) {
    Notifications.Bus.notify(getDefaultNotification(content, type));
  }

  public static int showFileStructureDialog(
      Project project,
      FolderStructureTreePanel folderStructureTreePanel) {
    var dialogBuilder = new DialogBuilder(project);
    dialogBuilder.setNorthPanel(JBUI.Panels.simplePanel(new JBLabel(
            "<html>"
                + "<p>Indexing files enables direct queries related to your codebase.</p>"
                + "<br/>"
                + "<p>File indexing occurs locally on your computer; "
                + "no files are sent to any 3rd party services.</p>"
                + "<p>For additional information, refer to the "
                + "<a href=\"https://google.com\">CodeGPT documentation</a>.</p>"
                + "</html>")
            .setCopyable(true)
            .setAllowAutoWrapping(true)
            .withFont(JBFont.medium()))
        .withBorder(JBUI.Borders.emptyBottom(12)));
    dialogBuilder.setCenterPanel(folderStructureTreePanel.getPanel());
    dialogBuilder.addOkAction().setText("Start Indexing");
    dialogBuilder.addCancelAction();
    dialogBuilder.setTitle("Choose Files for Indexing");
    return dialogBuilder.show();
  }

  public static int showMultiFilePromptDialog(
      Project project,
      JBTextArea promptTemplateTextArea,
      JBTextArea repeatableContextTextArea,
      JBLabel totalTokensLabel,
      JComponent component) {
    var dialogBuilder = new DialogBuilder(project);
    dialogBuilder.setTitle("Include In Context");
    dialogBuilder.setActionDescriptors();
    var fileTreeScrollPane = ScrollPaneFactory.createScrollPane(component);
    fileTreeScrollPane.setPreferredSize(
        new Dimension(480, component.getPreferredSize().height + 48));
    dialogBuilder.setNorthPanel(FormBuilder.createFormBuilder()
        .addLabeledComponent("Prompt template:", promptTemplateTextArea, true)
        .addLabeledComponent("Repeatable context:", repeatableContextTextArea, true)
        .addVerticalGap(4)
        .addComponent(JBUI.Panels.simplePanel()
            .addToRight(getRestoreButton(promptTemplateTextArea, repeatableContextTextArea)))
        .addVerticalGap(16)
        .addComponent(new JBLabel("Choose the files that you wish to include in the final prompt")
            .setCopyable(false)
            .setAllowAutoWrapping(true))
        .addVerticalGap(4)
        .addLabeledComponent(totalTokensLabel, fileTreeScrollPane, true)
        .addVerticalGap(16)
        .getPanel());
    dialogBuilder.addOkAction().setText("Continue");
    dialogBuilder.addCancelAction();
    return dialogBuilder.show();
  }

  private static JButton getRestoreButton(JBTextArea promptTemplateTextArea,
      JBTextArea repeatableContextTextArea) {
    var restoreButton = new JButton("Restore to Defaults");
    restoreButton.addActionListener(e -> {
      var includedFilesSettings = IncludedFilesSettingsState.getInstance();
      includedFilesSettings.setPromptTemplate(IncludedFilesSettingsState.DEFAULT_PROMPT_TEMPLATE);
      includedFilesSettings.setRepeatableContext(
          IncludedFilesSettingsState.DEFAULT_REPEATABLE_CONTEXT);
      promptTemplateTextArea.setText(IncludedFilesSettingsState.DEFAULT_PROMPT_TEMPLATE);
      repeatableContextTextArea.setText(IncludedFilesSettingsState.DEFAULT_REPEATABLE_CONTEXT);
    });
    return restoreButton;
  }

  public static int showDeleteConversationDialog() {
    return Messages.showYesNoDialog(
        CodeGPTBundle.get("dialog.deleteConversation.description"),
        CodeGPTBundle.get("dialog.deleteConversation.title"),
        Default);
  }

  public static int showTokenLimitExceededDialog() {
    return MessageDialogBuilder.okCancel(
            CodeGPTBundle.get("dialog.tokenLimitExceeded.title"),
            CodeGPTBundle.get("dialog.tokenLimitExceeded.description"))
        .yesText(CodeGPTBundle.get("dialog.continue"))
        .noText(CodeGPTBundle.get("dialog.cancel"))
        .icon(Default)
        .doNotAsk(new DoNotAskOption.Adapter() {
          @Override
          public void rememberChoice(boolean isSelected, int exitCode) {
            if (isSelected) {
              ConversationsState.getInstance().discardAllTokenLimits();
            }
          }

          @NotNull
          @Override
          public String getDoNotShowMessage() {
            return ExecutionBundle.message("don.t.ask.again");
          }

          @Override
          public boolean shouldSaveOptionsOnCancel() {
            return true;
          }
        })
        .guessWindowAndAsk() ? OK : CANCEL;
  }

  public static int showTokenSoftLimitWarningDialog(int tokenCount) {
    return MessageDialogBuilder.okCancel(
            CodeGPTBundle.get("dialog.tokenSoftLimitExceeded.title"),
            format(CodeGPTBundle.get("dialog.tokenSoftLimitExceeded.description"), tokenCount))
        .yesText(CodeGPTBundle.get("dialog.continue"))
        .noText(CodeGPTBundle.get("dialog.cancel"))
        .icon(Default)
        .doNotAsk(new DoNotAskOption.Adapter() {
          @Override
          public void rememberChoice(boolean isSelected, int exitCode) {
            if (isSelected) {
              ConfigurationState.getInstance().setIgnoreGitCommitTokenLimit(true);
            }
          }

          @NotNull
          @Override
          public String getDoNotShowMessage() {
            return ExecutionBundle.message("don.t.ask.again");
          }

          @Override
          public boolean shouldSaveOptionsOnCancel() {
            return true;
          }
        })
        .guessWindowAndAsk() ? OK : CANCEL;
  }

  public static void showSelectedEditorSelectionWarning(AnActionEvent event) {
    var locationOnScreen = ((MouseEvent) event.getInputEvent()).getLocationOnScreen();
    locationOnScreen.y = locationOnScreen.y - 16;
    showSelectedEditorSelectionWarning(requireNonNull(event.getProject()), locationOnScreen);
  }

  public static void showSelectedEditorSelectionWarning(
      @NotNull Project project,
      Point locationOnScreen) {
    showWarningBalloon(
        EditorUtil.getSelectedEditor(project) == null
            ? "Unable to locate a selected editor"
            : "Please select a target code before proceeding",
        locationOnScreen);
  }

  public static void showWarningBalloon(String content, Point locationOnScreen) {
    showBalloon(content, MessageType.WARNING, locationOnScreen);
  }

  public static void showInfoBalloon(String content, Point locationOnScreen) {
    showBalloon(content, MessageType.INFO, locationOnScreen);
  }

  private static void showBalloon(String content, MessageType messageType, Point locationOnScreen) {
    JBPopupFactory.getInstance()
        .createHtmlTextBalloonBuilder(content, messageType, null)
        .setFadeoutTime(2500)
        .createBalloon()
        .show(RelativePoint.fromScreen(locationOnScreen), Balloon.Position.above);
  }

  public static void showBalloon(String content, MessageType messageType, JComponent component) {
    JBPopupFactory.getInstance()
        .createHtmlTextBalloonBuilder(content, messageType, null)
        .setFadeoutTime(2500)
        .createBalloon()
        .show(RelativePoint.getSouthOf(component), Position.below);
  }
}
