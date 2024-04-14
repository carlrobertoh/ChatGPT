package ee.carlrobert.codegpt.completions;

import static ee.carlrobert.codegpt.credentials.CredentialsStore.CredentialKey.CUSTOM_SERVICE_API_KEY;
import static ee.carlrobert.codegpt.util.file.FileUtil.getResourceContent;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.ApplicationManager;
import ee.carlrobert.codegpt.EncodingManager;
import ee.carlrobert.codegpt.ReferencedFile;
import ee.carlrobert.codegpt.completions.llama.LlamaModel;
import ee.carlrobert.codegpt.completions.llama.PromptTemplate;
import ee.carlrobert.codegpt.conversations.Conversation;
import ee.carlrobert.codegpt.conversations.ConversationsState;
import ee.carlrobert.codegpt.conversations.message.Message;
import ee.carlrobert.codegpt.credentials.CredentialsStore;
import ee.carlrobert.codegpt.settings.GeneralSettings;
import ee.carlrobert.codegpt.settings.IncludedFilesSettings;
import ee.carlrobert.codegpt.settings.configuration.ConfigurationSettings;
import ee.carlrobert.codegpt.settings.service.ServiceType;
import ee.carlrobert.codegpt.settings.service.anthropic.AnthropicSettings;
import ee.carlrobert.codegpt.settings.service.custom.CustomServiceSettings;
import ee.carlrobert.codegpt.settings.service.custom.CustomServiceSettingsState;
import ee.carlrobert.codegpt.settings.service.llama.LlamaSettings;
import ee.carlrobert.codegpt.settings.service.openai.OpenAISettings;
import ee.carlrobert.codegpt.settings.service.you.YouSettings;
import ee.carlrobert.codegpt.telemetry.core.configuration.TelemetryConfiguration;
import ee.carlrobert.codegpt.telemetry.core.service.UserId;
import ee.carlrobert.codegpt.util.PlaceholderUtil;
import ee.carlrobert.codegpt.util.file.FileUtil;
import ee.carlrobert.llm.client.anthropic.completion.ClaudeBase64Source;
import ee.carlrobert.llm.client.anthropic.completion.ClaudeCompletionDetailedMessage;
import ee.carlrobert.llm.client.anthropic.completion.ClaudeCompletionMessage;
import ee.carlrobert.llm.client.anthropic.completion.ClaudeCompletionRequest;
import ee.carlrobert.llm.client.anthropic.completion.ClaudeCompletionStandardMessage;
import ee.carlrobert.llm.client.anthropic.completion.ClaudeMessageImageContent;
import ee.carlrobert.llm.client.anthropic.completion.ClaudeMessageTextContent;
import ee.carlrobert.llm.client.llama.completion.LlamaCompletionRequest;
import ee.carlrobert.llm.client.openai.completion.OpenAIChatCompletionModel;
import ee.carlrobert.llm.client.openai.completion.request.OpenAIChatCompletionDetailedMessage;
import ee.carlrobert.llm.client.openai.completion.request.OpenAIChatCompletionMessage;
import ee.carlrobert.llm.client.openai.completion.request.OpenAIChatCompletionRequest;
import ee.carlrobert.llm.client.openai.completion.request.OpenAIChatCompletionStandardMessage;
import ee.carlrobert.llm.client.openai.completion.request.OpenAIImageUrl;
import ee.carlrobert.llm.client.openai.completion.request.OpenAIMessageImageURLContent;
import ee.carlrobert.llm.client.openai.completion.request.OpenAIMessageTextContent;
import ee.carlrobert.llm.client.you.completion.YouCompletionRequest;
import ee.carlrobert.llm.client.you.completion.YouCompletionRequestMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.jetbrains.annotations.Nullable;

public class CompletionRequestProvider {

  public static final String COMPLETION_SYSTEM_PROMPT = getResourceContent(
      "/prompts/default-completion-system-prompt.txt");

  public static final String GENERATE_COMMIT_MESSAGE_SYSTEM_PROMPT = getResourceContent(
      "/prompts/generate-commit-message-system-prompt.txt");

  public static final String FIX_COMPILE_ERRORS_SYSTEM_PROMPT = getResourceContent(
      "/prompts/fix-compile-errors.txt");

  private final EncodingManager encodingManager = EncodingManager.getInstance();
  private final Conversation conversation;

  public CompletionRequestProvider(Conversation conversation) {
    this.conversation = conversation;
  }

  public static String getPromptWithContext(List<ReferencedFile> referencedFiles,
      String userPrompt) {
    var includedFilesSettings = IncludedFilesSettings.getCurrentState();
    var repeatableContext = referencedFiles.stream()
        .map(item -> includedFilesSettings.getRepeatableContext()
            .replace("{FILE_PATH}", item.getFilePath())
            .replace("{FILE_CONTENT}", format(
                "```%s%n%s%n```",
                item.getFileExtension(),
                item.getFileContent().trim())))
        .collect(joining("\n\n"));

    return includedFilesSettings.getPromptTemplate()
        .replace("{REPEATABLE_CONTEXT}", repeatableContext)
        .replace("{QUESTION}", userPrompt);
  }

  public static OpenAIChatCompletionRequest buildOpenAILookupCompletionRequest(String context) {
    return new OpenAIChatCompletionRequest.Builder(
        List.of(
            new OpenAIChatCompletionStandardMessage(
                "system",
                getResourceContent("/prompts/method-name-generator.txt")),
            new OpenAIChatCompletionStandardMessage("user", context)))
        .setModel(OpenAISettings.getCurrentState().getModel())
        .setStream(false)
        .build();
  }

  public static Request buildCustomOpenAICompletionRequest(String system, String context) {
    return buildCustomOpenAIChatCompletionRequest(
        CustomServiceSettings.getCurrentState(),
        List.of(
            new OpenAIChatCompletionStandardMessage("system", system),
            new OpenAIChatCompletionStandardMessage("user", context)),
        true);
  }

  public static Request buildCustomOpenAILookupCompletionRequest(String context) {
    return buildCustomOpenAIChatCompletionRequest(
        CustomServiceSettings.getCurrentState(),
        List.of(
            new OpenAIChatCompletionStandardMessage(
                "system",
                getResourceContent("/prompts/method-name-generator.txt")),
            new OpenAIChatCompletionStandardMessage("user", context)),
        false);
  }

  public static LlamaCompletionRequest buildLlamaLookupCompletionRequest(String context) {
    return new LlamaCompletionRequest.Builder(PromptTemplate.LLAMA
        .buildPrompt(getResourceContent("/prompts/method-name-generator.txt"), context, List.of()))
        .setStream(false)
        .build();
  }

  public LlamaCompletionRequest buildLlamaCompletionRequest(
      Message message,
      ConversationType conversationType) {
    var settings = LlamaSettings.getCurrentState();
    PromptTemplate promptTemplate;
    if (settings.isRunLocalServer()) {
      promptTemplate = settings.isUseCustomModel()
          ? settings.getLocalModelPromptTemplate()
          : LlamaModel.findByHuggingFaceModel(settings.getHuggingFaceModel()).getPromptTemplate();
    } else {
      promptTemplate = settings.getRemoteModelPromptTemplate();
    }

    var systemPrompt = COMPLETION_SYSTEM_PROMPT;
    if (conversationType == ConversationType.FIX_COMPILE_ERRORS) {
      systemPrompt = FIX_COMPILE_ERRORS_SYSTEM_PROMPT;
    }

    var prompt = promptTemplate.buildPrompt(
        systemPrompt,
        message.getPrompt(),
        conversation.getMessages());
    var configuration = ConfigurationSettings.getCurrentState();
    return new LlamaCompletionRequest.Builder(prompt)
        .setN_predict(configuration.getMaxTokens())
        .setTemperature(configuration.getTemperature())
        .setTop_k(settings.getTopK())
        .setTop_p(settings.getTopP())
        .setMin_p(settings.getMinP())
        .setRepeat_penalty(settings.getRepeatPenalty())
        .build();
  }

  public YouCompletionRequest buildYouCompletionRequest(Message message) {
    var requestBuilder = new YouCompletionRequest.Builder(message.getPrompt())
        .setUseGPT4Model(YouSettings.getCurrentState().isUseGPT4Model())
        .setChatMode(YouSettings.getCurrentState().getChatMode())
        .setCustomModel(YouSettings.getCurrentState().getCustomModel())
        .setChatHistory(conversation.getMessages().stream()
            .map(prevMessage -> new YouCompletionRequestMessage(
                prevMessage.getPrompt(),
                prevMessage.getResponse()))
            .toList());
    if (TelemetryConfiguration.getInstance().isEnabled()
        && !ApplicationManager.getApplication().isUnitTestMode()) {
      requestBuilder.setUserId(UUID.fromString(UserId.INSTANCE.get()));
    }
    return requestBuilder.build();
  }

  public OpenAIChatCompletionRequest buildOpenAIChatCompletionRequest(
      @Nullable String model,
      CallParameters callParameters) {
    var configuration = ConfigurationSettings.getCurrentState();
    return new OpenAIChatCompletionRequest.Builder(buildMessages(model, callParameters))
        .setModel(model)
        .setMaxTokens(configuration.getMaxTokens())
        .setStream(true)
        .setTemperature(configuration.getTemperature()).build();
  }

  public Request buildCustomOpenAIChatCompletionRequest(
      CustomServiceSettingsState customConfiguration,
      CallParameters callParameters) {
    return buildCustomOpenAIChatCompletionRequest(
        customConfiguration,
        buildMessages(callParameters),
        true);
  }

  private static Request buildCustomOpenAIChatCompletionRequest(
      CustomServiceSettingsState customConfiguration,
      List<OpenAIChatCompletionMessage> messages,
      boolean streamRequest) {
    var requestBuilder = new Request.Builder().url(customConfiguration.getUrl().trim());
    var credential = CredentialsStore.INSTANCE.getCredential(CUSTOM_SERVICE_API_KEY);
    for (var entry : customConfiguration.getHeaders().entrySet()) {
      String value = entry.getValue();
      if (credential != null && value.contains("$CUSTOM_SERVICE_API_KEY")) {
        value = value.replace("$CUSTOM_SERVICE_API_KEY", credential);
      }
      requestBuilder.addHeader(entry.getKey(), value);
    }

    var body = customConfiguration.getBody().entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> {
              if (!streamRequest && "stream".equals(entry.getKey())) {
                return false;
              }

              var value = entry.getValue();
              if (value instanceof String string && "$OPENAI_MESSAGES".equals(string.trim())) {
                return messages;
              }
              return value;
            }
        ));

    try {
      var requestBody = RequestBody.create(new ObjectMapper()
          .writerWithDefaultPrettyPrinter()
          .writeValueAsString(body)
          .getBytes(StandardCharsets.UTF_8));
      return requestBuilder.post(requestBody).build();
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public ClaudeCompletionRequest buildAnthropicChatCompletionRequest(
      CallParameters callParameters) {
    var configuration = ConfigurationSettings.getCurrentState();
    var settings = AnthropicSettings.getCurrentState();
    var request = new ClaudeCompletionRequest();
    request.setModel(settings.getModel());
    request.setMaxTokens(configuration.getMaxTokens());
    request.setStream(true);
    request.setSystem(COMPLETION_SYSTEM_PROMPT);
    List<ClaudeCompletionMessage> messages = conversation.getMessages().stream()
        .filter(prevMessage -> prevMessage.getResponse() != null
            && !prevMessage.getResponse().isEmpty())
        .flatMap(prevMessage -> Stream.of(
            new ClaudeCompletionStandardMessage("user", prevMessage.getPrompt()),
            new ClaudeCompletionStandardMessage("assistant", prevMessage.getResponse())))
        .collect(toList());

    if (callParameters.getImageMediaType() != null && callParameters.getImageData().length > 0) {
      messages.add(new ClaudeCompletionDetailedMessage("user",
          List.of(
              new ClaudeMessageImageContent(new ClaudeBase64Source(
                  callParameters.getImageMediaType(),
                  callParameters.getImageData())),
              new ClaudeMessageTextContent(callParameters.getMessage().getPrompt()))));
    } else {
      messages.add(
          new ClaudeCompletionStandardMessage("user", callParameters.getMessage().getPrompt()));
    }
    request.setMessages(messages);
    return request;
  }

  private List<OpenAIChatCompletionMessage> buildMessages(CallParameters callParameters) {
    var message = callParameters.getMessage();
    var messages = new ArrayList<OpenAIChatCompletionMessage>();
    if (callParameters.getConversationType() == ConversationType.DEFAULT) {
      String systemPrompt = ConfigurationSettings.getCurrentState().getSystemPrompt();
      systemPrompt = new PlaceholderUtil().replacePlaceholder(systemPrompt);
      messages.add(new OpenAIChatCompletionStandardMessage("system", systemPrompt));
    }
    if (callParameters.getConversationType() == ConversationType.FIX_COMPILE_ERRORS) {
      messages.add(
          new OpenAIChatCompletionStandardMessage("system", FIX_COMPILE_ERRORS_SYSTEM_PROMPT));
    }

    for (var prevMessage : conversation.getMessages()) {
      if (callParameters.isRetry() && prevMessage.getId().equals(message.getId())) {
        break;
      }
      var prevMessageImageFilePath = prevMessage.getImageFilePath();
      if (prevMessageImageFilePath != null && !prevMessageImageFilePath.isEmpty()) {
        try {
          var imageFilePath = Path.of(prevMessageImageFilePath);
          var imageData = Files.readAllBytes(imageFilePath);
          var imageMediaType = FileUtil.getImageMediaType(imageFilePath.getFileName().toString());
          messages.add(new OpenAIChatCompletionDetailedMessage("user",
              List.of(
                  new OpenAIMessageImageURLContent(new OpenAIImageUrl(imageMediaType, imageData)),
                  new OpenAIMessageTextContent(prevMessage.getPrompt()))));
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      } else {
        messages.add(new OpenAIChatCompletionStandardMessage("user", prevMessage.getPrompt()));
      }
      messages.add(new OpenAIChatCompletionStandardMessage("assistant", prevMessage.getResponse()));
    }

    if (callParameters.getImageMediaType() != null && callParameters.getImageData().length > 0) {
      messages.add(new OpenAIChatCompletionDetailedMessage("user",
          List.of(
              new OpenAIMessageImageURLContent(
                  new OpenAIImageUrl(callParameters.getImageMediaType(),
                      callParameters.getImageData())),
              new OpenAIMessageTextContent(message.getPrompt()))));
    } else {
      messages.add(new OpenAIChatCompletionStandardMessage("user", message.getPrompt()));
    }
    return messages;
  }

  private List<OpenAIChatCompletionMessage> buildMessages(
      @Nullable String model,
      CallParameters callParameters) {
    var messages = buildMessages(callParameters);

    if (model == null
        || GeneralSettings.getCurrentState().getSelectedService() == ServiceType.YOU) {
      return messages;
    }

    int totalUsage = messages.parallelStream()
        .mapToInt(encodingManager::countMessageTokens)
        .sum() + ConfigurationSettings.getCurrentState().getMaxTokens();
    int modelMaxTokens;
    try {
      modelMaxTokens = OpenAIChatCompletionModel.findByCode(model).getMaxTokens();

      if (totalUsage <= modelMaxTokens) {
        return messages;
      }
    } catch (NoSuchElementException ex) {
      return messages;
    }
    return tryReducingMessagesOrThrow(messages, totalUsage, modelMaxTokens);
  }

  private List<OpenAIChatCompletionMessage> tryReducingMessagesOrThrow(
      List<OpenAIChatCompletionMessage> messages,
      int totalUsage,
      int modelMaxTokens) {
    if (!ConversationsState.getInstance().discardAllTokenLimits) {
      if (!conversation.isDiscardTokenLimit()) {
        throw new TotalUsageExceededException();
      }
    }

    // skip the system prompt
    for (int i = 1; i < messages.size(); i++) {
      if (totalUsage <= modelMaxTokens) {
        break;
      }

      var message = messages.get(i);
      if (message instanceof OpenAIChatCompletionStandardMessage) {
        totalUsage -= encodingManager.countMessageTokens(message);
        messages.set(i, null);
      }
    }

    return messages.stream().filter(Objects::nonNull).toList();
  }
}
