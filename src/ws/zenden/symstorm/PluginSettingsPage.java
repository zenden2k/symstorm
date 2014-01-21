package ws.zenden.symstorm;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import javax.swing.*;
import  com.intellij.ide.util.PropertiesComponent;

import java.awt.*;

public class PluginSettingsPage implements Configurable  {

    private JTextField appPathTextField;
    private JCheckBox enablePlugin;
    private JRadioButton usingUrlMatcherFile;
    private JRadioButton usingRouterMatch;
    private JRadioButton usingRouterDump;
    private JTextField hostsTextField;

    private JTextField secretKeyTextField;
    Project project;

    public PluginSettingsPage(Project project) {
        this.project = project;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "SymStorm";
    }

    @Override
    public JComponent createComponent() {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout
                (panel,  BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel panel1 = new JPanel();
        panel1.setLayout(new BoxLayout(panel1, BoxLayout.X_AXIS));

        enablePlugin = new JCheckBox("Enable SymStorm for this project");
//        panel1.add(enablePlugin);
//        panel1.add(Box.createHorizontalGlue());

        JPanel panel2 = new JPanel();
        panel2.setLayout( new BoxLayout(panel2,  BoxLayout.X_AXIS));

        appPathTextField = new JTextField(30);
        JLabel label = new JLabel("App path:");
        panel2.add(label );
        label.setLabelFor(appPathTextField);

        panel2.add(appPathTextField);
        panel2.add(Box.createHorizontalGlue());

        JPanel panel3 = new JPanel();

        enablePlugin = new JCheckBox("Enable SymStorm for this project");

        usingUrlMatcherFile = new JRadioButton("Use url matcher file");
        usingRouterMatch = new JRadioButton("Use router:match command");
        usingRouterDump = new JRadioButton("Use router:dump command");
        ButtonGroup bg = new ButtonGroup();
        bg.add(usingUrlMatcherFile);
        bg.add(usingRouterMatch);
        bg.add(usingRouterDump);

        JPanel panel4 = new JPanel();
        panel4.setLayout(new BoxLayout(panel4, BoxLayout.X_AXIS));
        panel4.add(usingUrlMatcherFile);
        panel4.add(Box.createHorizontalGlue());

        JPanel panel5 = new JPanel();
        panel5.setLayout(new BoxLayout(panel5, BoxLayout.X_AXIS));
        panel5.add(usingRouterMatch);
        panel5.add(Box.createHorizontalGlue());

        JPanel panel6 = new JPanel();
        panel6.setLayout(new BoxLayout(panel6, BoxLayout.X_AXIS));
        panel6.add(usingRouterDump);
        panel6.add(Box.createHorizontalGlue());

        appPathTextField.setMaximumSize(appPathTextField.getPreferredSize());

        JPanel panel7 = new JPanel();
        panel7.setLayout( new BoxLayout(panel7,  BoxLayout.X_AXIS));

        hostsTextField = new JTextField(30);
        hostsTextField.setMaximumSize(appPathTextField.getPreferredSize());
        JLabel hostsTextFieldLabel = new JLabel("List of hosts (comma separated):");
        panel7.add(hostsTextFieldLabel );
        label.setLabelFor(appPathTextField);

        panel7.add(hostsTextField);
        panel7.add(Box.createHorizontalGlue());

        panel.add(panel1);
        panel.add(Box.createVerticalStrut(8));
        
        panel.add( panel2 );
        panel.add(Box.createVerticalStrut(8));
        //panel.add(usingUrlMatcherFile);
     //   panel.add(usingRouterMatch);
//        panel.add(panel4);
        panel.add(panel5);
        panel.add(panel6);
        panel.add(panel7);
        panel.add(Box.createVerticalGlue());

        PropertiesComponent properties = PropertiesComponent.getInstance(project);
        appPathTextField.setText(properties.getValue("symfonyAppPath", DefaultSettings.appPath));
        enablePlugin.setSelected(properties.getBoolean("enablePlugin", true));
        hostsTextField.setText(properties.getValue("hosts", DefaultSettings.hosts));

        DefaultSettings.RouteFindMethod routeFindMethod = DefaultSettings.RouteFindMethod.values()[properties.getOrInitInt("routeFindMethod", DefaultSettings.routeFindMethod.ordinal())];
        if ( routeFindMethod == DefaultSettings.RouteFindMethod.USING_ROUTE_MATCH ) {
            usingRouterMatch.setSelected(true);
        } else if ( routeFindMethod == DefaultSettings.RouteFindMethod.USING_ROUTE_DUMP )   {
            usingRouterDump.setSelected(true);
        } else {
            usingUrlMatcherFile.setSelected(true);
        }
        return panel;
    }

    @Override
    public void apply() throws ConfigurationException {
        PropertiesComponent properties = PropertiesComponent.getInstance(project);
        properties.setValue("symfonyAppPath", appPathTextField.getText());
        properties.setValue("enablePlugin", String.valueOf(enablePlugin.isSelected()) );
        properties.setValue("hosts", hostsTextField.getText());

        DefaultSettings.RouteFindMethod routeFindMethod;
        if ( usingRouterMatch.isSelected() ) {
            routeFindMethod = DefaultSettings.RouteFindMethod.USING_ROUTE_MATCH;
        } else if ( usingRouterDump.isSelected() ) {
            routeFindMethod = DefaultSettings.RouteFindMethod.USING_ROUTE_DUMP;
        } else {
            routeFindMethod = DefaultSettings.RouteFindMethod.USING_URL_MATCHER_FILE;
        }
        properties.setValue("routeFindMethod", String.valueOf(routeFindMethod.ordinal()) );
    }

    @Override
    public boolean isModified() {
        return true;
    }

    @Override
    public String getHelpTopic() {
        return null;
    }

    @Override
    public void disposeUIResources() {

    }

    @Override
    public void reset() {

    }
}
