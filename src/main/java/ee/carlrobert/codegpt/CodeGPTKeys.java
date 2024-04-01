package ee.carlrobert.codegpt;

import com.intellij.openapi.util.Key;
import java.util.List;

public class CodeGPTKeys {

  public static final Key<String> PREVIOUS_INLAY_TEXT =
      Key.create("codegpt.editor.inlay.prev-value");
  public static final Key<List<ReferencedFile>> SELECTED_FILES =
      Key.create("codegpt.selectedFiles");
  public static final Key<String> UPLOADED_FILE_PATH = Key.create("codegpt.uploadedFilePath");
}
