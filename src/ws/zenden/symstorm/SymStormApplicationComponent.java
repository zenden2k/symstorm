package ws.zenden.symstorm;

import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.ide.XmlRpcServer;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.elements.impl.MethodImpl;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

public class SymStormApplicationComponent implements ApplicationComponent {
    private static final String HANDLER_NAME = "symStormHandler";
    private final XmlRpcServer myXmlRpcServer;

    public SymStormApplicationComponent(final XmlRpcServer xmlRpcServer) {
        myXmlRpcServer = xmlRpcServer;
    }

    public void initComponent() {
        myXmlRpcServer.addHandler(HANDLER_NAME, new OpenFileHandler());
    }

    public void disposeComponent() {
        myXmlRpcServer.removeHandler(HANDLER_NAME);
    }

    @NotNull
    public String getComponentName() {
        return "SymStormApplicationComponent";
    }

    public static class OpenFileHandler {
        @SuppressWarnings({"MethodMayBeStatic"})
        public boolean open(final String url, final String controller,  final String method ) {
            final Application application = ApplicationManager.getApplication();

            application.invokeLater(new Runnable() {
                public void run() {
                    final Project[] openProjects =
                            ProjectManager.getInstance().getOpenProjects();
                    if (openProjects.length == 0) {
                        return;
                    }

                    String hostName = "";
                    String path = "";
                    URL urlObject = null;
                    try {
                        urlObject = new URL(url);
                        hostName = urlObject.getHost();
                        path = urlObject.getPath();
                    } catch (Exception ex) {

                    }
                    // Detect project
                    Project targetProject = null;
                    for ( Project project : openProjects ) {
                        PropertiesComponent properties = PropertiesComponent.getInstance(project);
                        String hosts = properties.getValue("hosts", DefaultSettings.hosts).toLowerCase();
                        if ( !hosts.isEmpty() ) {
                            String[] hostsArray = hosts.split(",");
                            if ( Arrays.asList(hostsArray).contains(hostName ) ) {
                                targetProject = project;
                                break;
                            }
                        }
                    }

                    if ( targetProject == null ) {
                        targetProject = openProjects[0];
                    }
                    if ( !controller.isEmpty() ) {
                        PhpIndex phpIndex = PhpIndex.getInstance(targetProject);
                        Collection<? extends PhpNamedElement> methodCalls = phpIndex.getBySignature("#M#C\\" + controller + (method.isEmpty() ? "" : "." + method), null, 0);
                        PsiElement[] elements = methodCalls.toArray(new PsiElement[methodCalls.size()]);
                        if ( elements.length != 0 ) {
                            NavigationUtil.activateFileWithPsiElement(elements[0], false);
                        }
                        ProjectUtil.focusProjectWindow(targetProject, true);
                    }  else {
                        SymStormProjectComponent projectComponent = targetProject.getComponent(SymStormProjectComponent.class);
                        String controller = projectComponent.getController(path);
                        MethodImpl el = (MethodImpl)projectComponent.getPsiElement(controller);
                        if ( el != null ) {
                            NavigationUtil.activateFileWithPsiElement(el, false);
                            ProjectUtil.focusProjectWindow(targetProject, true);
                        }
                    }
                }
            });

            return true;
        }
    }
}
