package ee.carlrobert.codegpt.ui.checkbox;

import static java.util.stream.Collectors.toList;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.file.PsiDirectoryImpl;
import com.intellij.ui.CheckedTreeNode;
import ee.carlrobert.embedding.CheckedFile;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public class PsiElementCheckboxTree extends FileCheckboxTree {

  public PsiElementCheckboxTree(@NotNull PsiElement rootElement) {
    super(createFileTypesRenderer(), createNode(rootElement));
    setRootVisible(true);
  }

  public List<CheckedFile> getCheckedFiles() {
    return Arrays.stream(getCheckedNodes(
            PsiElement.class,
            node -> Optional.ofNullable(node.getContainingFile()).isPresent()))
        .map(item -> new CheckedFile(new File(item.getContainingFile().getVirtualFile().getPath())))
        .collect(toList());
  }

  private static CheckedTreeNode createNode(PsiElement element) {
    if (!(element instanceof PsiDirectory || element instanceof PsiFile)) {
      return null;
    }

    CheckedTreeNode node = new CheckedTreeNode(element);
    if (element instanceof PsiDirectory) {
      for (PsiElement child : element.getChildren()) {
        CheckedTreeNode childNode = createNode(child);
        if (childNode != null) {
          node.add(childNode);
        }
      }
    }
    return node;
  }

  private static @NotNull FileCheckboxTreeCellRenderer createFileTypesRenderer() {
    return new FileCheckboxTreeCellRenderer() {
      @Override
      void updatePresentation(Object userObject) {
        var psiElement = (PsiElement) userObject;
        Optional.ofNullable(psiElement)
            .map(PsiElement::getContainingFile)
            .ifPresentOrElse(
                item -> {
                  var virtualFile = item.getVirtualFile();
                  if (virtualFile != null) {
                    updateFilePresentation(getTextRenderer(), virtualFile);
                  }
                },
                () -> {
                  if (userObject instanceof PsiDirectoryImpl) {
                    updateFolderPresentation((PsiDirectoryImpl) userObject);
                  }
                });
      }

      private void updateFolderPresentation(PsiDirectoryImpl psiDirectory) {
        var icon = psiDirectory.getIcon(Iconable.ICON_FLAG_VISIBILITY);
        getTextRenderer().setIcon(icon == null ? AllIcons.Nodes.Folder : icon);
        getTextRenderer().append(psiDirectory.getName());
      }
    };
  }
}
