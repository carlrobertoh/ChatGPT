package ee.carlrobert.codegpt.completions.llama;

import ee.carlrobert.codegpt.conversations.message.Message;
import java.util.List;

public enum PromptTemplate {

  CHAT_ML("Chat Markup Language (ChatML)") {
    @Override
    public String buildPrompt(String systemPrompt, String userPrompt, List<Message> history) {
      StringBuilder prompt = new StringBuilder();

      if (systemPrompt != null && !systemPrompt.isEmpty()) {
        prompt.append("<|im_start|>system\n")
            .append(systemPrompt)
            .append("<|im_end|>\n");
      }

      for (Message message : history) {
        prompt.append("<|im_start|>user\n")
            .append(message.getPrompt())
            .append("<|im_end|>\n")
            .append("<|im_start|>assistant\n")
            .append(message.getResponse())
            .append("<|im_end|>\n");
      }

      return prompt.append("<|im_start|>user\n")
          .append(userPrompt)
          .append("<|im_end|>")
          .toString();
    }
  },
  LLAMA("Llama") {
    @Override
    public String buildPrompt(String systemPrompt, String userPrompt, List<Message> history) {
      StringBuilder prompt = new StringBuilder();

      if (systemPrompt != null && !systemPrompt.isEmpty()) {
        prompt.append("<<SYS>>")
            .append(systemPrompt)
            .append("<</SYS>>\n");
      }

      for (Message message : history) {
        prompt.append("[INST]")
            .append(message.getPrompt())
            .append("[/INST]\n")
            .append(message.getResponse()).append("\n");
      }

      return prompt.append("[INST]")
          .append(userPrompt)
          .append("[/INST]")
          .toString();
    }
  },
  LLAMA_3("Llama 3") {
    @Override
    public String buildPrompt(String systemPrompt, String userPrompt, List<Message> history) {
      StringBuilder prompt = new StringBuilder();

      if (systemPrompt != null && !systemPrompt.isBlank()) {
        prompt.append("<|start_header_id|>system<|end_header_id|>\n\n")
            .append(systemPrompt)
            .append("<|eot_id|>");
      }

      for (Message message : history) {
        prompt.append("<|start_header_id|>user<|end_header_id|>\n\n")
                .append(message.getPrompt())
                .append("<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n")
                .append(message.getResponse()).append("<|eot_id|>");
      }

      return prompt.append("<|start_header_id|>user<|end_header_id|>\n\n")
          .append(userPrompt)
          .append("<|eot_id|>")
          .toString();
    }
  },
  MIXTRAL_INSTRUCT("Mixtral Instruct") {
    @Override
    public String buildPrompt(String systemPrompt, String userPrompt, List<Message> history) {
      StringBuilder prompt = new StringBuilder();

      if (systemPrompt != null && !systemPrompt.isEmpty()) {
        prompt.append(systemPrompt)
            .append("\n");
      }

      for (Message message : history) {
        prompt.append("<s> [INST] ")
            .append(message.getPrompt())
            .append(" [/INST] ")
            .append(message.getResponse()).append("</s> ");
      }

      return prompt.append("[INST] ")
          .append(userPrompt)
          .append(" [/INST]")
          .toString();
    }
  },
  TORA("ToRA") {
    @Override
    public String buildPrompt(String systemPrompt, String userPrompt, List<Message> history) {
      StringBuilder prompt = new StringBuilder();

      for (Message message : history) {
        prompt.append("<|user|>\n")
            .append(message.getPrompt())
            .append("\n<|assistant|>\n")
            .append(message.getResponse()).append("\n");
      }

      return prompt.append("<|user|>\n")
          .append(userPrompt)
          .append("\n<|assistant|>")
          .toString();
    }
  },
  ALPACA("Alpaca/Vicuna") {
    @Override
    public String buildPrompt(String systemPrompt, String userPrompt, List<Message> history) {
      StringBuilder prompt = new StringBuilder();

      prompt.append("""
              Below is an instruction that describes a task. \
              Write a response that appropriately completes the request.

              """);

      for (Message message : history) {
        prompt.append("### Instruction\n")
            .append(message.getPrompt())
            .append("\n\n")
            .append("### Response:\n")
            .append(message.getResponse())
            .append("\n\n");
      }

      return prompt.append("### Instruction\n")
          .append(userPrompt)
          .append("\n\n")
          .append("### Response:\n")
          .toString();
    }
  },
  DEEPSEEK_CODER("DeepSeek Coder") {
    @Override
    public String buildPrompt(String systemPrompt, String userPrompt, List<Message> history) {
      StringBuilder prompt = new StringBuilder();

      String defaultSystemPrompt = "You are an AI programming assistant, "
          + "utilizing the Deepseek Coder model, developed by Deepseek Company, "
          + "and you only answer questions related to computer science. "
          + "For politically sensitive questions, security and privacy issues, "
          + "and other non-computer science questions, you will refuse to answer";

      prompt.append("<｜begin▁of▁sentence｜>");
      if (systemPrompt != null && !systemPrompt.isEmpty()) {
        prompt.append(systemPrompt);
      } else {
        prompt.append(defaultSystemPrompt);
      }
      prompt.append("\n\n");

      for (Message message : history) {
        prompt.append("### Instruction\n")
            .append(message.getPrompt())
            .append("\n\n")
            .append("### Response:\n")
            .append(message.getResponse())
            .append("<|EOT|>\n\n");
      }

      return prompt.append("### Instruction\n")
          .append(userPrompt)
          .append("\n\n")
          .append("### Response:\n")
          .toString();
    }
  };

  private final String label;

  PromptTemplate(String label) {
    this.label = label;
  }

  public abstract String buildPrompt(String systemPrompt, String userPrompt, List<Message> history);

  @Override
  public String toString() {
    return label;
  }
}
