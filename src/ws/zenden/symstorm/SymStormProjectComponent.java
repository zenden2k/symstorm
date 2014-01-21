package ws.zenden.symstorm;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpFileImpl;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.MethodImpl;
import org.apache.commons.net.util.Base64;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SymStormProjectComponent implements ProjectComponent {

    private Project project;
    private ArrayList<RoutingDumpReader.Entry> routes;
    private Long routesLastModified;
    private VirtualFile webDir;


    public class Result {
        public final String header;
        public final String footer;

        public Result(String header, String footer) {
            this.header= header;
            this.footer= footer;
        }
    }

    public SymStormProjectComponent(Project project) {
        this.project = project;
        webDir = project.getBaseDir();
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

    public String getControllerUsingRouterMatch(String url) {
        PropertiesComponent properties = PropertiesComponent.getInstance(this.project);
        String appPath = properties.getValue("symfonyAppPath", DefaultSettings.appPath);
        try {
            ProcessBuilder procBuilder = new ProcessBuilder("php", getPath(project, appPath + "console"), "router:match", url);
            procBuilder.redirectErrorStream(true);
            Process process = procBuilder.start();
            InputStream stdout = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
            String line;
            String result = "";
            while ((line = reader.readLine()) != null) {
                result += line;
            }
            if (result.isEmpty() || result.contains("None of the routes matches")) {
                return null;
            }

            Matcher matcher = Pattern.compile("(?i)Route \"(.+)\"").matcher(result);

            if (matcher.find()) {
                Map<String, String> routes = getRoutesFromUrlGeneratorFile();
                String controller = routes.get(matcher.group(1));
                if (controller != null) {
                    return controller;
                }
            }
        } catch (IOException ex) {

        }
        return null;
    }

    public Map<String, String> getRoutesFromUrlGeneratorFile() {
        PropertiesComponent properties = PropertiesComponent.getInstance(project);
        String appPath = properties.getValue("symfonyAppPath", DefaultSettings.appPath);
        String env = "dev";
        String urlGeneratorPath = appPath + "cache/" + env + "/appDev" + "UrlGenerator.php";
        urlGeneratorPath = getPath(project, urlGeneratorPath);
//        String urlGeneratorPath = getPath(project, Settings.getInstance(project).pathToUrlGenerator);
        File urlGeneratorFile = new File(urlGeneratorPath);
//        System.out.println("urlGeneratorPath=" + urlGeneratorPath);
        VirtualFile virtualUrlGeneratorFile = VfsUtil.findFileByIoFile(urlGeneratorFile, false);
        Map<String, String> routes = new HashMap<String, String>();

        Matcher matcher = null;
        try {
            matcher = Pattern.compile("'((?:[^'\\\\]|\\\\.)*)' => [^\\n]+'_controller' => '((?:[^'\\\\]|\\\\.)*)'[^\\n]+\n").matcher(VfsUtil.loadText(virtualUrlGeneratorFile));
        } catch (IOException ignored) {
        }

        if ( null == matcher ) {
            return routes;
        }

        while (matcher.find()) {
            String routeName = matcher.group(1);
            // dont add _assetic_04d92f8, _assetic_04d92f8_0
            if(!routeName.matches("_assetic_[0-9a-z]+[_\\d+]*")) {
                String controller = matcher.group(2).replace("\\\\", "\\");
                routes.put(routeName, controller);
            }
        }

        return routes;
    }

    String getController(String url) {
        try {
            PropertiesComponent properties = PropertiesComponent.getInstance(project);
            DefaultSettings.RouteFindMethod routeFindMethod = DefaultSettings.RouteFindMethod.values()[properties.getOrInitInt("routeFindMethod", DefaultSettings.routeFindMethod.ordinal())];
            String controller = null;
            if ( routeFindMethod == DefaultSettings.RouteFindMethod.USING_ROUTE_MATCH ) {
                controller = getControllerUsingRouterMatch(url);
            } /*else {
                    controller = getController(path);
                }*/
            if ( controller != null ) {
                return controller;
            } else {

                ArrayList<RoutingDumpReader.Entry>list =  getRoutes();


                for ( RoutingDumpReader.Entry entry: list ) {
                    Pattern p = Pattern.compile(entry.rewriteCond) ;
                    if ( p.matcher(url).matches() ) {
                        return entry.controller;
                        /*MethodImpl el = (MethodImpl)getPsiElement(entry.controller);
                        if ( el != null ) {
                            MyListElement mle = new MyListElement(
                                    "",el.getContainingFile().getVirtualFile()
                                    ,"controller",el.getContainingClass().getName() + "::" + el.getName());
                            mle.psiElement = el;
                            elements.add(mle);
                        } */
                    }
                }
            }
        }    catch ( Exception ex) {
            System.err.println("Error while executing command");
            ex.printStackTrace();
        }
        finally {
            //is.close();
        }
        return null;
    }

    String getControllerUsingUrlMatcher(String url) {
        /** TODO: resolve service name
         *
         */
        PropertiesComponent properties = PropertiesComponent.getInstance(project);
        String appPath = properties.getValue("symfonyAppPath", DefaultSettings.appPath);
        String env = "dev";
        String envUpper =  "Dev"/*Character.toUpperCase(env.charAt(0)) + env.substring(1)*/;
        String urlMatcherPath = appPath + "cache/" + env + "/app" + envUpper +"UrlMatcher.php";
        String urlGenerator =   appPath + "cache/" + env + "/app" + envUpper +"UrlGenerator.php";

        VirtualFile appDevUrlMatcherFile =  webDir.findFileByRelativePath(urlMatcherPath);
        VirtualFile appDevUrlGeneratorFile =  webDir.findFileByRelativePath(urlGenerator);
        if ( appDevUrlMatcherFile == null || urlGenerator == null ) {
            env = "prod";
            envUpper = "Prod";
            urlMatcherPath = appPath + "cache/" + env + "/app" + envUpper +"UrlMatcher.php";
            urlGenerator =      appPath + "cache/" + env + "/app" + envUpper +"UrlGenerator.php";
            appDevUrlMatcherFile =  webDir.findFileByRelativePath(urlMatcherPath);
            appDevUrlGeneratorFile =  webDir.findFileByRelativePath(urlGenerator);
            if ( appDevUrlMatcherFile == null || appDevUrlGeneratorFile == null ) {
                return null;
            }
        }
        PsiFile psiFile = PsiManager.getInstance(project).findFile(appDevUrlMatcherFile);
        PhpFile urlGeneratorPsiFile = (PhpFile)PsiManager.getInstance(project).findFile(appDevUrlGeneratorFile);
        PhpFileImpl phpFile =      (PhpFileImpl) psiFile;
        ArrayList<PhpFile> processedFiles = new ArrayList<PhpFile>();
        Result res1 = processFile(phpFile, 0, processedFiles, false /* true */);
        String path = url.replaceAll("/$","");
        if ( path.isEmpty() ) {
            path = "/";
        }
        byte[] pathb =   path.getBytes();
        byte[] encodedPath = Base64.encodeBase64(path.getBytes());
//        Result res2 = processFile(urlGeneratorPsiFile, 0, processedFiles, true );
        String res =    res1.header + /*res2.header +*/ res1.footer/* + res2.footer*/;

        res += "\n<?php " +
                "function checkMethod($method = 'GET') {\n" +
                "\ttry { \n" +
                "\t\t$url = base64_decode('"+new String(encodedPath)+"'); \n" +
                "\t\t$reqContext = new RequestContext($url, $method);\n" +
                "\t\t$matcher = new appDevUrlMatcher($reqContext);\n" +
                "\t\t$res = $matcher->match($url);\n" +
                "\t\tif ( strpos($res['_controller'], 'RedirectController::urlRedirectAction') !== false ) {\n" +
                "\t\t\t$res = $matcher->match($res['path']);\n" +
                "\t\t}\n" +
                "\t\treturn $res;\n" +
                "\t} catch ( \\Exception $ex ) {\n" +
                "\t\t\n" +
                "\t}\n" +
                "\treturn false;\n" +
                "}\n" +
                "$result = array();\n" +
                "$methods = array('GET', 'POST', 'PUT', 'DELETE' );\n" +
                "foreach ( $methods as $method  ) {\n" +
                "\t$res = checkMethod($method);\n" +
                "\tif ( $res ) {\n" +
                "\t\t$result[$res['_route']] = $res;\n" +
                "\t}\n" +
                "}\n" +
                "echo json_encode(array_values($result));";

        String result = null;

        try {
            /** TODO: Use pipe instead of temporary file */

            File temp = File.createTempFile("phpresult.php", ".tmp");
            PrintWriter out = new PrintWriter(temp);

            out.println(res);
            out.close();

            ProcessBuilder procBuilder = new ProcessBuilder("php",temp.getAbsolutePath());
            procBuilder.redirectErrorStream(true);
            Process process = procBuilder.start();
            OutputStream stdin = process.getOutputStream ();
            InputStream stdout = process.getInputStream ();
            InputStreamReader isrStdout = new InputStreamReader(stdout);
            BufferedReader reader = new BufferedReader(isrStdout);
            String line;
            String phpResult = "";

            int bufferSize = 500;
            int i = 0;

            String controller = null;
            try {
                JSONArray myjson = new JSONArray(phpResult);
                for ( i =0; i < myjson.length(); i++ ) {
                    controller = ((JSONObject)myjson.get(i)).getString("_controller");
                }
            }   catch ( Exception ex ) {
                System.err.println("Invalid result");
                System.err.println(phpResult);
            }


            isrStdout.close();
            temp.delete();
//            System.out.println(controller);
            if ( controller != null && !controller.isEmpty() ) {
                result =  controller;
            }
            try {
                int exitVal = process.waitFor();
            } catch ( Exception ex ) {

            }
        } catch ( Exception ex) {
            System.err.println("Error while executing command");
            ex.printStackTrace();
        }
        finally {
            //is.close();
        }
        return result;
    }



    protected Result processFile(PhpFile phpFile, int level,  ArrayList<PhpFile> processedFiles, boolean isMatcherFile ) {
        if ( level > 8 || processedFiles.contains(phpFile)) {
            return new Result("","");
        }
        processedFiles.add(phpFile);
        Map<String,PhpNamedElement> topLevelDefs = phpFile.getTopLevelDefs();
        String res ="";
        for (Map.Entry<String, PhpNamedElement> entry : topLevelDefs.entrySet()) {
            if ( entry.getValue() instanceof PhpUse) {
                PhpUse useStatement = (PhpUse) entry.getValue();
                ClassReference classRef = useStatement.getClassReference();
                String name = classRef.getName();
                java.util.Collection<? extends PhpNamedElement> els = classRef.resolveGlobal(true);
                for ( PhpNamedElement el: els ) {
                    PhpFile containingFile = (PhpFile)el.getContainingFile();
                    String fileName = containingFile.getVirtualFile().getPath();
                    if ( fileName.contains("src/Symfony/") ) {
                        res += processFile(containingFile, level + 1, processedFiles, false).header;
                    }
                }
            }   else  if ( entry.getValue() instanceof PhpClass) {
                PhpClass phpClass =     ((PhpClass) entry.getValue());
                ExtendsList extendsList = phpClass.getExtendsList();
                if ( extendsList != null ) {
                    ClassReference classRef = extendsList.getReferenceElement();
                    if ( classRef != null ) {
                        String className = classRef.getName();
                        java.util.Collection<? extends PhpNamedElement> els = classRef.resolveGlobal(true);
                        for ( PhpNamedElement el: els ) {
                            PhpFile containingFile = (PhpFile)el.getContainingFile();
                            String fileName = containingFile.getVirtualFile().getPath();
                            if ( fileName.contains("src/Symfony") ) {
                                res += processFile(containingFile, level + 1, processedFiles, false).header;
                            }
                        }
                    }
                }
                ImplementsList implementsList  = phpClass.getImplementsList();
                if ( implementsList != null ) {
                    List<ClassReference> implementsListClassRef = implementsList.getReferenceElements();
                    if ( implementsListClassRef != null ) {
                        for ( ClassReference ref: implementsListClassRef )   {
                            String refName = ref.getName();
                            if ( ref != null ) {
                                java.util.Collection<? extends PhpNamedElement> els = ref.resolveGlobal(true);
                                for ( PhpNamedElement el: els ) {
                                    if ( el.getNamespaceName().equals(phpClass.getNamespaceName()) ) {
                                        PhpFile containingFile = (PhpFile)el.getContainingFile();
                                        String fileName = containingFile.getVirtualFile().getPath();
                                        if ( fileName.contains("src/Symfony") ) {
                                            res += processFile(containingFile, level + 1, processedFiles, false).header;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        String curFileText = phpFile.getText();
        if ( level == 0 ) {
            curFileText = curFileText.replace("extends Symfony\\", "extends \\Symfony\\");
        }

        if ( isMatcherFile ) {
            curFileText = curFileText.replace("use Symfony\\Component\\Routing\\RequestContext;","");
            curFileText = curFileText.replace("use Symfony\\Component\\Routing\\Exception\\RouteNotFoundException;","");
            curFileText = curFileText.replace("use Psr\\Log\\LoggerInterface;","");
            curFileText = curFileText.replace("use Symfony\\Component\\HttpKernel\\Log\\LoggerInterface;","");

        }
        curFileText += "\n?>";
        if ( level == 0 ) {
            return  new Result(res, curFileText);
        }   else {
            return new Result(res+curFileText, "");
        }
    }

    private String getPath(Project project, String path) {
        return project.getBasePath() + "/" + path;
    }

    PsiElement getPsiElement(String controller) {
        PhpIndex phpIndex = PhpIndex.getInstance(project);

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
}
