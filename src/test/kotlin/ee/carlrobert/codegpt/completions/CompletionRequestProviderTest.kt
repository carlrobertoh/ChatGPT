package ee.carlrobert.codegpt.completions

import ee.carlrobert.codegpt.conversations.ConversationService
import ee.carlrobert.codegpt.conversations.message.Message
import ee.carlrobert.codegpt.credentials.CredentialsStore.CredentialKey
import ee.carlrobert.codegpt.credentials.CredentialsStore.setCredential
import ee.carlrobert.codegpt.settings.configuration.ConfigurationSettings
import ee.carlrobert.llm.client.openai.completion.OpenAIChatCompletionModel
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import testsupport.IntegrationTest

class CompletionRequestProviderTest : IntegrationTest() {

  fun testChatCompletionRequestWithSystemPromptOverride() {
    setCredential(CredentialKey.OPENAI_API_KEY, "TEST_API_KEY")
    ConfigurationSettings.getCurrentState().systemPrompt = "TEST_SYSTEM_PROMPT"
    val conversation = ConversationService.getInstance().startConversation()
    val firstMessage = createDummyMessage(500)
    val secondMessage = createDummyMessage(250)
    conversation.addMessage(firstMessage)
    conversation.addMessage(secondMessage)

    val request = CompletionRequestProvider(conversation)
      .buildOpenAIChatCompletionRequest(
        OpenAIChatCompletionModel.GPT_3_5.code,
        CallParameters(
          conversation,
          ConversationType.DEFAULT,
          Message("TEST_CHAT_COMPLETION_PROMPT"),
          false))

    assertThat(request.messages)
      .extracting("role", "content")
      .containsExactly(
        Tuple.tuple("system", "TEST_SYSTEM_PROMPT"),
        Tuple.tuple("user", "TEST_PROMPT"),
        Tuple.tuple("assistant", firstMessage.response),
        Tuple.tuple("user", "TEST_PROMPT"),
        Tuple.tuple("assistant", secondMessage.response),
        Tuple.tuple("user", "TEST_CHAT_COMPLETION_PROMPT"))
  }

  fun testChatCompletionRequestWithoutSystemPromptOverride() {
    val conversation = ConversationService.getInstance().startConversation()
    val firstMessage = createDummyMessage(500)
    val secondMessage = createDummyMessage(250)
    conversation.addMessage(firstMessage)
    conversation.addMessage(secondMessage)

    val request = CompletionRequestProvider(conversation)
      .buildOpenAIChatCompletionRequest(
        OpenAIChatCompletionModel.GPT_3_5.code,
        CallParameters(
          conversation,
          ConversationType.DEFAULT,
          Message("TEST_CHAT_COMPLETION_PROMPT"),
          false))

    assertThat(request.messages)
      .extracting("role", "content")
      .containsExactly(
        Tuple.tuple("system", CompletionRequestProvider.COMPLETION_SYSTEM_PROMPT),
        Tuple.tuple("user", "TEST_PROMPT"),
        Tuple.tuple("assistant", firstMessage.response),
        Tuple.tuple("user", "TEST_PROMPT"),
        Tuple.tuple("assistant", secondMessage.response),
        Tuple.tuple("user", "TEST_CHAT_COMPLETION_PROMPT"))
  }

  fun testChatCompletionRequestRetry() {
    ConfigurationSettings.getCurrentState().systemPrompt = CompletionRequestProvider.COMPLETION_SYSTEM_PROMPT
    val conversation = ConversationService.getInstance().startConversation()
    val firstMessage = createDummyMessage("FIRST_TEST_PROMPT", 500)
    val secondMessage = createDummyMessage("SECOND_TEST_PROMPT", 250)
    conversation.addMessage(firstMessage)
    conversation.addMessage(secondMessage)

    val request = CompletionRequestProvider(conversation)
      .buildOpenAIChatCompletionRequest(
        OpenAIChatCompletionModel.GPT_3_5.code,
        CallParameters(
          conversation,
          ConversationType.DEFAULT,
          secondMessage,
          true))

    assertThat(request.messages)
      .extracting("role", "content")
      .containsExactly(
        Tuple.tuple("system", CompletionRequestProvider.COMPLETION_SYSTEM_PROMPT),
        Tuple.tuple("user", "FIRST_TEST_PROMPT"),
        Tuple.tuple("assistant", firstMessage.response),
        Tuple.tuple("user", "SECOND_TEST_PROMPT"))
  }

  fun testReducedChatCompletionRequest() {
    val conversation = ConversationService.getInstance().startConversation()
    conversation.addMessage(createDummyMessage(50))
    conversation.addMessage(createDummyMessage(100))
    conversation.addMessage(createDummyMessage(150))
    conversation.addMessage(createDummyMessage(1000))
    val remainingMessage = createDummyMessage(2000)
    conversation.addMessage(remainingMessage)
    conversation.discardTokenLimits()

    val request = CompletionRequestProvider(conversation)
      .buildOpenAIChatCompletionRequest(
        OpenAIChatCompletionModel.GPT_3_5.code,
        CallParameters(
          conversation,
          ConversationType.DEFAULT,
          Message("TEST_CHAT_COMPLETION_PROMPT"),
          false))

    assertThat(request.messages)
      .extracting("role", "content")
      .containsExactly(
        Tuple.tuple("system", CompletionRequestProvider.COMPLETION_SYSTEM_PROMPT),
        Tuple.tuple("user", "TEST_PROMPT"),
        Tuple.tuple("assistant", remainingMessage.response),
        Tuple.tuple("user", "TEST_CHAT_COMPLETION_PROMPT"))
  }

  fun testTotalUsageExceededException() {
    val conversation = ConversationService.getInstance().startConversation()
    conversation.addMessage(createDummyMessage(1500))
    conversation.addMessage(createDummyMessage(1500))
    conversation.addMessage(createDummyMessage(1500))

    assertThrows(TotalUsageExceededException::class.java) {
      CompletionRequestProvider(conversation)
        .buildOpenAIChatCompletionRequest(
          OpenAIChatCompletionModel.GPT_3_5.code,
          CallParameters(
            conversation,
            ConversationType.DEFAULT,
            createDummyMessage(100),
            false)) }
  }

  private fun createDummyMessage(tokenSize: Int): Message {
    return createDummyMessage("TEST_PROMPT", tokenSize)
  }

  private fun createDummyMessage(prompt: String, tokenSize: Int): Message {
    val message = Message(prompt)
    // 'zz' = 1 token, prompt = 6 tokens, 7 tokens per message (GPT-3),
    message.response = "zz".repeat((tokenSize) - 6 - 7)
    return message
  }
}
