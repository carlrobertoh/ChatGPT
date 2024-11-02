package ee.carlrobert.codegpt.settings.service.anthropic;

import java.util.Objects;

public class AnthropicSettingsState {

  private String apiVersion = "2023-06-01";
  private String model = "claude-3-opus-20240229";
  private String host = "";

  public String getApiVersion() {
    return apiVersion;
  }

  public void setApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public String getHost() {
    return host;
  }

  public void setHost(final String host) {
    this.host = host;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AnthropicSettingsState that = (AnthropicSettingsState) o;
    return Objects.equals(apiVersion, that.apiVersion) && Objects.equals(model, that.model) && Objects.equals(host, that.host);
  }

  @Override
  public int hashCode() {
    return Objects.hash(apiVersion, model, host);
  }
}
