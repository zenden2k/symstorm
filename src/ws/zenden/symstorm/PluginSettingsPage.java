package ws.zenden.symstorm;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import javax.swing.*;
import  com.intellij.ide.util.PropertiesComponent;

public class PluginSettingsPage implements Configurable  {

    private JTextField appPathTextField;
    private JCheckBox enablePlugin;
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
        JPanel panel1 = new JPanel();
        panel1.setLayout(new BoxLayout(panel1, BoxLayout.X_AXIS));

        enablePlugin = new JCheckBox("Enable SymStorm for this project");
        panel1.add(enablePlugin);
        panel1.add(Box.createHorizontalGlue());

        JPanel panel2 = new JPanel();
        panel2.setLayout( new BoxLayout(panel2,  BoxLayout.X_AXIS));

        appPathTextField = new JTextField(30);
        JLabel label = new JLabel("App path:");
        panel2.add(label );
        label.setLabelFor(appPathTextField);

        panel2.add(appPathTextField);
        panel2.add(Box.createHorizontalGlue());


        appPathTextField.setMaximumSize(appPathTextField.getPreferredSize());

        panel.add(panel1);
        panel.add(Box.createVerticalStrut(8));
        
        panel.add( panel2 );
        panel.add(Box.createVerticalStrut(8));
        panel.add(Box.createVerticalGlue());
        PropertiesComponent properties = PropertiesComponent.getInstance(project);
        appPathTextField.setText(properties.getValue("aPath", DefaultSettings.appPath));
        enablePlugin.setSelected(properties.getBoolean("enablePlugin", true));

        return panel;
    }

    @Override
    public void apply() throws ConfigurationException {
        PropertiesComponent properties = PropertiesComponent.getInstance(project);
        properties.setValue("symfonyAppPath", appPathTextField.getText());
        properties.setValue("enablePlugin", String.valueOf(enablePlugin.isSelected()) );
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
