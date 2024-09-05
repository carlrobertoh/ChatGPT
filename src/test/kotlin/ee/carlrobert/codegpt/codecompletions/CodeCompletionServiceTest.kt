package ee.carlrobert.codegpt.codecompletions

import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.util.TextRange
import com.intellij.testFramework.PlatformTestUtil
import ee.carlrobert.codegpt.CodeGPTKeys
import ee.carlrobert.codegpt.settings.configuration.ConfigurationSettings
import ee.carlrobert.codegpt.settings.service.llama.LlamaSettings
import ee.carlrobert.codegpt.util.file.FileUtil.getResourceContent
import ee.carlrobert.llm.client.http.RequestEntity
import ee.carlrobert.llm.client.http.exchange.StreamHttpExchange
import ee.carlrobert.llm.client.util.JSONUtil.e
import ee.carlrobert.llm.client.util.JSONUtil.jsonMapResponse
import org.assertj.core.api.Assertions.assertThat
import testsupport.IntegrationTest

class CodeCompletionServiceTest : IntegrationTest() {
    private val cursorPosition = VisualPosition(1, 0)

    fun testFetchCodeCompletionLlama() {
        useLlamaService()
        LlamaSettings.getCurrentState().isCodeCompletionsEnabled = true
        myFixture.configureByText(
            "CompletionTest.java",
            """
                [INPUT]

                [\INPUT]
            """.trimIndent()
        )
        val editor = myFixture.editor
        val expectedCompletion = "TEST_SINGLE_LINE_OUTPUT\nTEST_MULTI_LINE_OUTPUT"
        val prefix = """
             [INPUT]
             c
             """.trimIndent() // 128 tokens
        val suffix = """
             
             [\INPUT]
             """.trimIndent() // 128 tokens
        expectLlama(StreamHttpExchange { request: RequestEntity ->
            assertThat(request.uri.path)
                .isEqualTo("/completion")
            assertThat(request.method).isEqualTo("POST")
            assertThat(request.body)
                .extracting("prompt")
                .isEqualTo(
                    InfillPromptTemplate.CODE_LLAMA.buildPrompt(
                        InfillRequest.Builder(prefix, suffix).build()
                    )
                )
            listOf(
                jsonMapResponse(
                    e(
                        "content",
                        expectedCompletion
                    ), e("stop", true)
                )
            )
        })
        editor.caretModel.moveToVisualPosition(cursorPosition)

        myFixture.type('c')

        PlatformTestUtil.waitWithEventsDispatching(
            "Editor inlay assertions failed",
            {
                val singleLineInlayElement =
                    editor.getUserData(CodeGPTKeys.SINGLE_LINE_INLAY)
                val multiLineInlayElement =
                    editor.getUserData(CodeGPTKeys.MULTI_LINE_INLAY)
                if (singleLineInlayElement != null && multiLineInlayElement == null) {
                    val singleLine =
                        (singleLineInlayElement.renderer as InlayInlineElementRenderer)
                            .getInlayText()
                    return@waitWithEventsDispatching "TEST_SINGLE_LINE_OUTPUT" == singleLine
                }
                false
            }, 5
        )
    }

    fun testApplyInlayAction() {
        ConfigurationSettings.getState().autoFormattingEnabled = false
        myFixture.configureByText(
            "CompletionTest.java",
            getResourceContent("/codecompletions/code-completion-file.txt")
        )
        val editor = myFixture.editor
        editor.caretModel.moveToVisualPosition(cursorPosition)
        val expectedSingleLineInlay = "FIRST_LINE"
        val expectedMultiLineInlay = "SECOND_LINE\nTHIRD_LINE"
        val expectedInlay = """
             $expectedSingleLineInlay
             $expectedMultiLineInlay
             """.trimIndent()
        val cursorOffsetBeforeApply = editor.caretModel.offset
        CodeCompletionServiceOld.getInstance(project)
            .addInlays(editor, cursorOffsetBeforeApply, expectedInlay)

        myFixture.performEditorAction(CodeCompletionServiceOld.APPLY_INLAY_ACTION_ID)

        val newTextRange = TextRange(cursorOffsetBeforeApply, editor.caretModel.offset)
        val appliedInlay = editor.document.getText(newTextRange)
        assertThat(appliedInlay).isEqualTo(expectedInlay)
    }
}
