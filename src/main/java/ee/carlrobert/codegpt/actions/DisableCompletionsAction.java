package ee.carlrobert.codegpt.actions;

import static ee.carlrobert.codegpt.settings.service.ServiceType.CUSTOM_OPENAI;
import static ee.carlrobert.codegpt.settings.service.ServiceType.LLAMA_CPP;
import static ee.carlrobert.codegpt.settings.service.ServiceType.OPENAI;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import ee.carlrobert.codegpt.codecompletions.CodeGPTEditorManager;
import ee.carlrobert.codegpt.settings.GeneralSettings;
import ee.carlrobert.codegpt.settings.configuration.ConfigurationSettings;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Disables code-completion.<br/> Publishes message to {@link CodeCompletionEnabledListener#TOPIC}
 */
public class DisableCompletionsAction extends AnAction {

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    ConfigurationSettings.getCurrentState().setCodeCompletionsEnabled(false);
    CodeGPTEditorManager.getInstance().disposeAllInlays(e.getProject());
    ApplicationManager.getApplication()
        .getMessageBus().syncPublisher(CodeCompletionEnabledListener.TOPIC)
        .onCodeCompletionsEnabledChange(false);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    var selectedService = GeneralSettings.getCurrentState().getSelectedService();
    var codeCompletionEnabled = ConfigurationSettings.getCurrentState().isCodeCompletionsEnabled();
    e.getPresentation().setEnabled(codeCompletionEnabled);
    e.getPresentation()
        .setVisible(codeCompletionEnabled && List.of(CUSTOM_OPENAI, OPENAI, LLAMA_CPP)
            .contains(selectedService));
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }
}
