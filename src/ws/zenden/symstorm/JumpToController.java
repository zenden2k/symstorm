package ws.zenden.symstorm;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.codeInsight.navigation.NavigationUtil;
import java.util.HashMap;
import java.util.Map;

public class JumpToController extends AnAction implements DumbAware, MyModel.Callback {
    Project project;
    protected static Class myInAction = null;
    private static String lastString = null;
    private static final Map<Class, Pair<String, Integer>> ourLastStrings = new HashMap<Class, Pair<String, Integer>>();

    public JumpToController() {

    }

    public void actionPerformed(final AnActionEvent e) {
        myInAction = JumpToController.class;
        final Project project = e.getData(PlatformDataKeys.PROJECT);
        //final Component component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT);

        this.project = project;
        VirtualFile baseDir = project.getBaseDir();
        VirtualFile webDir = baseDir;
        showNavigationPopup(e, new MyGotoFileModel(project, webDir), this, "title");
    }

    protected <T> void showNavigationPopup(AnActionEvent e, MyModel model, MyModel.Callback callback/*, final GotoActionCallback<T> callback*/) {
        showNavigationPopup(e, model, callback, null);
    }

    protected <T> void showNavigationPopup(AnActionEvent e, MyModel model, MyModel.Callback callback
                                           /*final GotoActionCallback<T> callback*/, final String findTitle) {
        final Project project = e.getData(PlatformDataKeys.PROJECT);

        boolean mayRequestOpenInCurrentWindow = model.willOpenEditor() && FileEditorManagerEx.getInstanceEx(project).hasSplitOrUndockedWindows();
        final Class startedAction = /*myInAction*/JumpToController.class;

        Pair<String, Integer> start = getInitialText(e.getData(PlatformDataKeys.EDITOR));
        final MyPopup popup = MyPopup.createPopup(project, model, /*getPsiContext(e),*/ start.first, mayRequestOpenInCurrentWindow, start.second);
        popup.invoke(this, ModalityState.current(), true);
    }


    private static Pair<String, Integer> getInitialText(Editor editor) {
        if (editor != null) {

        }
        return Pair.create("", 0);
    }

    /*@Override   */
    public void elementChosen(final Object element) {
        if (element == null) return;
        ApplicationManager.getApplication().invokeLater(
                new Runnable() {
                    public void run() {
                        MyListElement el = (MyListElement) element;
                        NavigationUtil.activateFileWithPsiElement(el.psiElement, false);
                    }
                }
        );
    }

    public void onClose(final MyPopup popup) {
        ourLastStrings.put(myInAction, Pair.create(popup.getEnteredText(), popup.getSelectedIndex()));
    }

}
