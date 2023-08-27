package ee.carlrobert.codegpt.settings;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import ee.carlrobert.codegpt.CodeGPTBundle;
import ee.carlrobert.codegpt.credentials.UserCredentialsManager;
import ee.carlrobert.codegpt.settings.state.SettingsState;
import ee.carlrobert.codegpt.user.UserManager;
import ee.carlrobert.codegpt.user.auth.AuthenticationHandler;
import ee.carlrobert.codegpt.user.auth.AuthenticationService;
import ee.carlrobert.codegpt.util.SwingUtils;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;

public class UserDetailsSettingsPanel extends JPanel {

  private final JBTextField emailField;
  private final JBPasswordField passwordField;
  private final JButton signInButton;
  private final JTextPane signUpTextPane;
  private final AsyncProcessIcon loadingSpinner;

  public UserDetailsSettingsPanel(Disposable parentDisposable, SettingsState settings) {
    super(new BorderLayout());
    emailField = new JBTextField(settings.getEmail(), 25);
    passwordField = new JBPasswordField();
    passwordField.setColumns(25);
    if (!settings.getEmail().isEmpty()) {
      passwordField.setText(UserCredentialsManager.getInstance().getAccountPassword());
    }
    signInButton = new JButton(CodeGPTBundle.get("settingsConfigurable.section.userAuthentication.signIn.label"));
    signUpTextPane = createSignUpTextPane();
    loadingSpinner = new AsyncProcessIcon("sign_in_spinner");
    loadingSpinner.setBorder(JBUI.Borders.emptyLeft(8));
    loadingSpinner.setVisible(false);

    var emailValidator = createInputValidator(parentDisposable, emailField);
    var passwordValidator = createInputValidator(parentDisposable, passwordField);

    signInButton.addActionListener(e -> {
      emailValidator.revalidate();
      passwordValidator.revalidate();
      if (emailValidator.getValidationInfo() == null && passwordValidator.getValidationInfo() == null) {
        loadingSpinner.resume();
        loadingSpinner.setVisible(true);
        AuthenticationService.getInstance()
            .signInAsync(emailField.getText(), new String(passwordField.getPassword()), new UserAuthenticationHandler());
      }
    });

    if (UserManager.getInstance().getSession() == null) {
      add(createUserAuthenticationPanel(emailField, passwordField, false));
    } else {
      add(createUserInformationPanel());
    }
  }

  public String getEmail() {
    return emailField.getText();
  }

  public void setEmail(String email) {
    emailField.setText(email);
  }

  public String getPassword() {
    return new String(passwordField.getPassword());
  }

  private ComponentValidator createInputValidator(Disposable parentDisposable, JComponent component) {
    var validator = new ComponentValidator(parentDisposable)
        .withValidator(() -> {
          String value;
          if (component instanceof JBTextField) {
            value = ((JBTextField) component).getText();
          } else {
            value = new String(((JPasswordField) component).getPassword());
          }

          if (StringUtil.isEmpty(value)) {
            return new ValidationInfo("This field is required.", component).withOKEnabled();
          }

          return null;
        })
        .andStartOnFocusLost()
        .installOn(component);
    validator.enableValidation();
    return validator;
  }

  private JTextPane createSignUpTextPane() {
    var textPane = createTextPane(
        "<html><a href=\"https://subscription-starter-etdkim4m3-carlrobertoh-gmailcom.vercel.app/signin#auth-sign-up\">Don't have an account? Sign up</a></html>");
    textPane.setBorder(JBUI.Borders.emptyLeft(4));
    return textPane;
  }

  private JTextPane createTextPane(String htmlContent) {
    var textPane = new JTextPane();
    textPane.setContentType("text/html");
    textPane.putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, true);
    textPane.setText(htmlContent);
    textPane.addHyperlinkListener(SwingUtils::handleHyperlinkClicked);
    textPane.setEditable(false);
    return textPane;
  }

  private JPanel createFooterPanel() {
    var panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    panel.setBorder(JBUI.Borders.empty());
    panel.add(signInButton);
    panel.add(signUpTextPane);
    panel.add(loadingSpinner);
    return panel;
  }

  private JPanel createUserAuthenticationPanel(JBTextField emailAddressField, JBPasswordField passwordField, boolean withInvalidCredentials) {
    var contentPanelBuilder = FormBuilder.createFormBuilder()
        .addVerticalGap(8)
        .addLabeledComponent("Email address:", emailAddressField)
        .addLabeledComponent("Password:", passwordField)
        .addVerticalGap(4)
        .addComponentToRightColumn(createFooterPanel())
        .addVerticalGap(4);

    if (withInvalidCredentials) {
      var invalidCredentialsLabel = new JBLabel("Invalid login credentials");
      invalidCredentialsLabel.setForeground(JBColor.red);
      invalidCredentialsLabel.setBorder(JBUI.Borders.emptyLeft(4));

      contentPanelBuilder.addComponentToRightColumn(invalidCredentialsLabel);
    }

    return FormBuilder.createFormBuilder()
        .addComponent(new TitledSeparator(CodeGPTBundle.get("settingsConfigurable.section.userAuthentication.title")))
        .addComponent(JBUI.Panels
            .simplePanel(contentPanelBuilder.getPanel())
            .withBorder(JBUI.Borders.emptyLeft(16)))
        .getPanel();
  }

  private JPanel createUserInformationPanel() {
    var userManager = UserManager.getInstance();
    var contentPanelBuilder = FormBuilder.createFormBuilder()
        .addLabeledComponent("Email address:",
            new JBLabel(userManager.getSession()
                .getUser()
                .getEmail()).withFont(JBFont.label().asBold()));

    if (userManager.isSubscribed()) {
      contentPanelBuilder.addLabeledComponent("Subscription:",
          new JBLabel(userManager.getSubscription()
              .getPrices()
              .getProducts()
              .getName()).withFont(JBFont.label().asBold()));
    } else {
      contentPanelBuilder.addComponent(createTextPane(
          "<html>You haven't subscribed to any plan yet. Subscribe <a href=\"https://codegpt-starter.vercel.app\">now</a>.</html>"));
    }

    var signOutButton = new JButton("Sign Out");
    signOutButton.addActionListener(e -> {
      userManager.clearSession();
      refreshView(createUserAuthenticationPanel(emailField, passwordField, false));
    });

    return FormBuilder.createFormBuilder()
        .addComponent(new TitledSeparator(CodeGPTBundle.get("settingsConfigurable.section.userInformation.title")))
        .addVerticalGap(8)
        .addComponent(JBUI.Panels
            .simplePanel(contentPanelBuilder.addVerticalGap(4)
                .addComponent(signOutButton)
                .getPanel())
            .withBorder(JBUI.Borders.emptyLeft(16)))
        .getPanel();
  }

  class UserAuthenticationHandler implements AuthenticationHandler {

    @Override
    public void handleAuthenticated() {
      SwingUtilities.invokeLater(() -> refreshView(createUserInformationPanel()));
    }

    @Override
    public void handleInvalidCredentials() {
      SwingUtilities.invokeLater(() -> refreshView(createUserAuthenticationPanel(emailField, passwordField, true)));
    }

    @Override
    public void handleGenericError() {
      // TODO
    }
  }

  private void refreshView(JPanel contentPanel) {
    loadingSpinner.suspend();
    loadingSpinner.setVisible(false);
    removeAll();
    add(contentPanel);
    revalidate();
    repaint();
  }
}
