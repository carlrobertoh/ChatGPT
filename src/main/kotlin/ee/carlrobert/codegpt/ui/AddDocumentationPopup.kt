package ee.carlrobert.codegpt.ui

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import ee.carlrobert.codegpt.CodeGPTBundle
import ee.carlrobert.codegpt.CodeGPTKeys
import ee.carlrobert.codegpt.settings.documentation.DocumentationDetailsState
import ee.carlrobert.codegpt.settings.documentation.DocumentationSettings
import javax.swing.JComponent

class AddDocumentationDialog(private val project: Project) : DialogWrapper(project) {

    private var nameField = JBTextField("", 40).apply {
        emptyText.text = "CodeGPT docs"
    }
    private var urlField = JBTextField("", 40).apply {
        emptyText.text = "https://docs.codegpt.ee"
    }
    private var saveCheckbox =
        JBCheckBox(CodeGPTBundle.get("addDocumentation.popup.form.saveCheckbox.label"), true)

    val documentationDetails: DocumentationDetails
        get() = DocumentationDetails(nameField.text, urlField.text)

    init {
        title = CodeGPTBundle.get("addDocumentation.popup.title")
        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        row {
            cell(nameField)
                .label(
                    CodeGPTBundle.get("addDocumentation.popup.form.name.label"),
                    LabelPosition.TOP
                )
                .focused()
        }
        row {
            cell(urlField)
                .label(
                    CodeGPTBundle.get("addDocumentation.popup.form.url.label"),
                    LabelPosition.TOP
                )
        }.rowComment(CodeGPTBundle.get("addDocumentation.popup.form.url.comment"))
        row { cell(saveCheckbox) }.topGap(TopGap.SMALL)
    }

    override fun doOKAction() {
        val documentationDetails = DocumentationDetails(nameField.text, urlField.text)
        project.putUserData(CodeGPTKeys.ADDED_DOCUMENTATION, documentationDetails)

        if (saveCheckbox.isSelected) {
            val newState = DocumentationDetailsState()
            newState.url = documentationDetails.url
            newState.name = documentationDetails.name

            service<DocumentationSettings>().state.documentations.add(newState)
        }

        super.doOKAction()
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class DocumentationDetails(
    @JsonProperty(value = "name") var name: String,
    @JsonProperty(value = "url") var url: String
)
