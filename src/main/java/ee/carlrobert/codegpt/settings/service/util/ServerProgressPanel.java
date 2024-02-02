package ee.carlrobert.codegpt.settings.service.util;

import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.AsyncProcessIcon;
import java.awt.FlowLayout;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;

public class ServerProgressPanel extends JPanel {

  private final JBLabel label = new JBLabel();

  public ServerProgressPanel() {
    setVisible(false);
    add(new AsyncProcessIcon("sign_in_spinner"));
    add(label);
  }

  public void startProgress(String text) {
    setVisible(true);
    updateText(text);
  }

  public void updateText(String text) {
    label.setText(text);
  }

  public void displayComponent(JComponent component) {
    removeAll();
    add(component);
    revalidate();
    repaint();
  }
}