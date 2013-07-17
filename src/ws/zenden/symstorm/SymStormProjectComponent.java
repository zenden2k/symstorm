package ws.zenden.symstorm;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class SymStormProjectComponent implements ProjectComponent {

    private Project project;
    private ArrayList<RoutingDumpReader.Entry> routes;
    private Long routesLastModified;

    public SymStormProjectComponent(Project project) {
        this.project = project;
    }

    public void initComponent() {
    }

    public void disposeComponent() {
    }

    @NotNull
    public String getComponentName() {
        return "SymStormProjectComponent";
    }

    public void projectOpened() {
    }

    public void projectClosed() {
    }

    public ArrayList<RoutingDumpReader.Entry> getRoutes() {
        PropertiesComponent properties = PropertiesComponent.getInstance(project);
        String appPath = properties.getValue("symfonyAppPath", DefaultSettings.appPath);
        String urlGeneratorPath = getPath(project, appPath + "cache/dev/appDevUrlGenerator.php");
        File urlGeneratorFile = new File(urlGeneratorPath);
        //Long routesLastModified = urlGeneratorFile.lastModified();

        if ( this.routes != null &&  ( System.currentTimeMillis() - routesLastModified < 30 * 60* 1000 ) ) {
            return this.routes;
        }

        try {
            ProcessBuilder procBuilder = new ProcessBuilder("php", getPath(project, appPath +  "console"),
                    "router:dump-apache");
            procBuilder.redirectErrorStream(true);
            Process process = procBuilder.start();
            InputStream stdout = process.getInputStream();
            InputStreamReader isrStdout = new InputStreamReader(stdout);
            RoutingDumpReader reader = new RoutingDumpReader(isrStdout);
            ArrayList<RoutingDumpReader.Entry> list = reader.getEntries();
            this.routes = list;
            this.routesLastModified = System.currentTimeMillis();
            isrStdout.close();
            try {
                int exitVal = process.waitFor();
            } catch ( Exception ex ) {
            }
        } catch (Exception ex) {
        }

        return   this.routes;
    }

    private String getPath(Project project, String path) {
        return project.getBasePath() + "/" + path;
    }
}
