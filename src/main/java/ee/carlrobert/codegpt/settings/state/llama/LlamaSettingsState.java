package ee.carlrobert.codegpt.settings.state.llama;

import com.intellij.util.xmlb.annotations.Transient;
import ee.carlrobert.codegpt.completions.llama.LlamaCompletionModel;

public class LlamaSettingsState {

  protected boolean runLocalServer = true;
  protected LlamaLocalSettings localSettings = new LlamaLocalSettings();
  protected LlamaRemoteSettings remoteSettings = new LlamaRemoteSettings();
  protected LlamaRequestSettings requestSettings = new LlamaRequestSettings();

  public LlamaSettingsState() {
  }

  public LlamaSettingsState(boolean runLocalServer, LlamaLocalSettings localSettings,
      LlamaRemoteSettings remoteSettings, LlamaRequestSettings requestSettings) {
    this.runLocalServer = runLocalServer;
    this.localSettings = localSettings;
    this.remoteSettings = remoteSettings;
    this.requestSettings = requestSettings;
  }

  public String getUsedModelPath() {
    return LlamaCompletionModel.getModelPath(localSettings.getModel());
  }

  public LlamaCompletionModel getUsedModel() {
    return localSettings.getModel();
  }

  public boolean isRunLocalServer() {
    return runLocalServer;
  }

  public void setRunLocalServer(boolean runLocalServer) {
    this.runLocalServer = runLocalServer;
  }

  @Transient
  public boolean isModified(LlamaSettingsState settingsState) {
    return localSettings.isModified(settingsState.getLocalSettings())
        || localSettings.getCredentials()
        .isModified(settingsState.getLocalSettings().getCredentials())
        || remoteSettings.isModified(settingsState.getRemoteSettings())
        || remoteSettings.getCredentials()
        .isModified(settingsState.getRemoteSettings().getCredentials())
        || runLocalServer != settingsState.isRunLocalServer()
        || requestSettings.isModified(settingsState.getRequestSettings());
  }

  public void apply(LlamaSettingsState settingsState) {
    runLocalServer = settingsState.isRunLocalServer();
    localSettings = settingsState.getLocalSettings();
    remoteSettings = settingsState.getRemoteSettings();
    requestSettings = settingsState.getRequestSettings();
  }

  public LlamaLocalSettings getLocalSettings() {
    return localSettings;
  }

  public void setLocalSettings(LlamaLocalSettings localSettings) {
    this.localSettings = localSettings;
  }

  public LlamaRemoteSettings getRemoteSettings() {
    return remoteSettings;
  }

  public void setRemoteSettings(LlamaRemoteSettings remoteSettings) {
    this.remoteSettings = remoteSettings;
  }

  public LlamaRequestSettings getRequestSettings() {
    return requestSettings;
  }

  public void setRequestSettings(
      LlamaRequestSettings llamaRequestSettings) {
    this.requestSettings = llamaRequestSettings;
  }

}