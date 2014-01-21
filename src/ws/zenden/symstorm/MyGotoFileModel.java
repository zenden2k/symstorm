package ws.zenden.symstorm;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.PsiElementListCellRenderer;
import com.intellij.ide.util.gotoByName.GotoFileCellRenderer;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpFileImpl;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.MethodImpl;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.net.util.Base64;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.intellij.plugins.relaxNG.compact.psi.util.EscapeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.*;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyGotoFileModel implements MyModel {
  private final int myMaxSize;


  List<String> cachedFileList = new ArrayList<String> ();
  String cachedPattern = null;
  Project myProject  = null;
  VirtualFile webDir;





  public MyGotoFileModel(Project project,  VirtualFile webDir ) {
      myProject = project;
            this.webDir = webDir;


        myMaxSize = WindowManagerEx.getInstanceEx().getFrame(project).getSize().width;
  }

  protected boolean acceptItem(final NavigationItem item) {
    return true;
  }

  @Nullable
  //@Override
  protected FileType filterValueFor(NavigationItem item) {
    return item instanceof PsiFile ? ((PsiFile) item).getFileType() : null;
  }

  public String getPromptText() {
    return "[SymStorm] Enter page URL:";
  }

  public String getCheckBoxName() {
    return IdeBundle.message("checkbox.include.non.project.files");
  }

  public char getCheckBoxMnemonic() {
    return SystemInfo.isMac?'P':'n';
  }

  public String getNotInMessage() {
    return IdeBundle.message("label.no.non.java.files.found");
  }

  public String getNotFoundMessage() {
    return IdeBundle.message("label.no.files.found");
  }

  public boolean loadInitialCheckBoxState() {
    PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(myProject);
    return propertiesComponent.isTrueValue("GoToClass.includeJavaFiles");
  }

  public void saveInitialCheckBoxState(boolean state) {
    PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(myProject);
    propertiesComponent.setValue("GoToClass.includeJavaFiles", Boolean.toString(state));
  }

  public PsiElementListCellRenderer getListCellRenderer() {
      //return new DefaultPsiElementCellRenderer();
    return new GotoFileCellRenderer(myMaxSize);
  }

  @Nullable
  public String getFullName(final Object element) {
    if (element instanceof PsiFile) {
      final VirtualFile virtualFile = ((PsiFile)element).getVirtualFile();
      return virtualFile != null ? virtualFile.getPath() : null;
    }

    return getElementName(element);
  }

  @NotNull
  public String[] getSeparators() {
    return new String[] {"/", "\\"};
  }

    public String getHelpId() {
        return "procedures.navigating.goto.class";
    }

    public boolean willOpenEditor() {
        return true;
    }

    public Object[] getElementsByName(String name, boolean checkBoxState, String pattern) {
        return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getElementName(Object element) {
        return "myGotoFileModel";
    }

    public String[] getNames(boolean checkBoxState) {
        return new String[]{ };
    }

    public Object[] getElementsByPattern(String userPattern) {
        ArrayList<MyListElement> elements = new ArrayList<MyListElement>();
        MyListElement error= new MyListElement("", null, "error", "");
        PropertiesComponent properties = PropertiesComponent.getInstance(myProject);
        String appPath = properties.getValue("appPath", DefaultSettings.appPath);
        String result = "";

        String hostName = "";
        String path = "";
        URL url = null;
        try {
            url = new URL(userPattern);
            hostName = url.getHost();
            path = url.getPath();
        } catch (Exception ex) {

        }

//        System.out.println(path);
        if ( !path.isEmpty() ) {
            SymStormProjectComponent projectComponent = myProject.getComponent(SymStormProjectComponent.class);
            String controller = projectComponent.getController(path);
            if ( controller != null ) {
                MethodImpl el = (MethodImpl)getPsiElement(controller);
                if ( el != null ) {
                    MyListElement mle = new MyListElement(
                            "",el.getContainingFile().getVirtualFile()
                            ,"controller",el.getContainingClass().getName() + "::" + el.getName());
                    mle.psiElement = el;
                    elements.add(mle);
                }
            }
        }
        return elements.toArray();
    }


    PsiElement getPsiElement(String controller) {
        PhpIndex phpIndex = PhpIndex.getInstance(myProject);

        String controllerName = controller;
        controllerName = controllerName.replace("\\:",":");

        if(controllerName.contains(":")) {
            String className = controllerName.substring(0, controllerName.indexOf(":"));
            className = className.replace("\\\\","\\");
            String methodName = controllerName.substring(controllerName.lastIndexOf(":") + 1);

            Collection<? extends PhpNamedElement> classes = phpIndex.getClassesByName(className);
            for ( PhpNamedElement el: classes) {
            }
            Collection<? extends PhpNamedElement> methodCalls = phpIndex.getBySignature("#M#C\\" + className + "." + methodName, null, 0);
            PsiElement[] elements = methodCalls.toArray(new PsiElement[methodCalls.size()]);
            for ( PsiElement el: elements ) {
                return el;
            }
        }
        return null;
    }

    private String getPath(Project project, String path) {
        return project.getBasePath() + "/" + path;
    }
}