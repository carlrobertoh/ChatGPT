package ee.carlrobert.codegpt.settings.service.llama;

import com.intellij.openapi.ui.panel.ComponentPanelBuilder;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.fields.IntegerField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import ee.carlrobert.codegpt.CodeGPTBundle;
import ee.carlrobert.codegpt.settings.state.llama.LlamaRequestSettings;
import ee.carlrobert.codegpt.ui.PromptTemplateField;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Form containing fields for all {@link LlamaRequestSettings}
 */
public class LlamaRequestsForm {

  private final IntegerField topKField;
  private final JBTextField topPField;
  private final JBTextField minPField;
  private final JBTextField repeatPenaltyField;

  public LlamaRequestsForm(LlamaRequestSettings settings) {
    topKField = new IntegerField();
    topKField.setColumns(12);
    topKField.setValue(settings.getTopK());
    topPField = new JBTextField(12);
    topPField.setText(String.valueOf(settings.getTopP()));
    minPField = new JBTextField(12);
    minPField.setText(String.valueOf(settings.getMinP()));
    repeatPenaltyField = new JBTextField(12);
    repeatPenaltyField.setText(String.valueOf(settings.getRepeatPenalty()));
  }

  public JPanel getForm() {
    return FormBuilder.createFormBuilder()
        .addLabeledComponent(
            CodeGPTBundle.get("settingsConfigurable.service.llama.topK.label"),
            topKField)
        .addComponentToRightColumn(
            createComment("settingsConfigurable.service.llama.topK.comment"))
        .addLabeledComponent(
            CodeGPTBundle.get("settingsConfigurable.service.llama.topP.label"),
            topPField)
        .addComponentToRightColumn(
            createComment("settingsConfigurable.service.llama.topP.comment"))
        .addLabeledComponent(
            CodeGPTBundle.get("settingsConfigurable.service.llama.minP.label"),
            minPField)
        .addComponentToRightColumn(
            createComment("settingsConfigurable.service.llama.minP.comment"))
        .addLabeledComponent(
            CodeGPTBundle.get("settingsConfigurable.service.llama.repeatPenalty.label"),
            repeatPenaltyField)
        .addComponentToRightColumn(
            createComment("settingsConfigurable.service.llama.repeatPenalty.comment"))
        .addComponentFillVertically(new JPanel(), 0)
        .getPanel();
  }

  public int getTopK() {
    return topKField.getValue();
  }

  public void setTopK(int topK) {
    topKField.setValue(topK);
  }

  public double getTopP() {
    return Double.parseDouble(topPField.getText());
  }

  public void setTopP(double topP) {
    topPField.setText(String.valueOf(topP));
  }

  public double getMinP() {
    return Double.parseDouble(minPField.getText());
  }

  public void setMinP(double minP) {
    minPField.setText(String.valueOf(minP));
  }

  public double getRepeatPenalty() {
    return Double.parseDouble(repeatPenaltyField.getText());
  }

  public void setRepeatPenalty(double repeatPenalty) {
    repeatPenaltyField.setText(String.valueOf(repeatPenalty));
  }

  private JLabel createComment(String messageKey) {
    var comment = ComponentPanelBuilder.createCommentComponent(
        CodeGPTBundle.get(messageKey), true);
    comment.setBorder(JBUI.Borders.empty(0, 4));
    return comment;
  }

  public LlamaRequestSettings getRequestSettings(){
    return new LlamaRequestSettings(
        topKField.getValue(),
        Double.parseDouble(topPField.getText()),
        Double.parseDouble(minPField.getText()),
        Double.parseDouble(repeatPenaltyField.getText()));
  }
}