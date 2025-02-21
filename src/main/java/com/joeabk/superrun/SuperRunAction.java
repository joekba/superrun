package com.joeabk.superrun;

import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.impl.EditConfigurationsDialog;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SuperRunAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        // Get all run configurations
        RunManager runManager = RunManager.getInstance(project);
        List<RunnerAndConfigurationSettings> allConfigurations = runManager.getAllSettings();
        allConfigurations = allConfigurations.stream().filter(data -> data.getUniqueID().contains("Application"))
                .collect(Collectors.toList());

        // Show configuration selection dialog
        ServiceSelectionDialog dialog = new ServiceSelectionDialog(project, allConfigurations);
        if (dialog.showAndGet()) {
            // Execute selected configurations with delay
            Map<RunnerAndConfigurationSettings, Boolean> selectedConfigs = dialog.getSelectedConfigurations();
            int delaySeconds = dialog.getDelaySeconds();
            executeConfigurations(project, selectedConfigs, delaySeconds);
        }
    }

    private void executeConfigurations(Project project, Map<RunnerAndConfigurationSettings, Boolean> configurations, int delaySeconds) {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        long delay = 0;

        for (Map.Entry<RunnerAndConfigurationSettings, Boolean> entry : configurations.entrySet()) {
            RunnerAndConfigurationSettings config = entry.getKey();
            boolean isDebugMode = entry.getValue();

            executorService.schedule(() -> {
                // Ensure UI operations are performed on the EDT
                ApplicationManager.getApplication().invokeLater(() -> {
                    Executor executor = isDebugMode ? ExecutorRegistry.getInstance().getExecutorById("Debug")
                            : ExecutorRegistry.getInstance().getExecutorById("Run");
                    if (executor != null) {
                        try {
                            ProgramRunner<?> runner = ProgramRunner.getRunner(executor.getId(), config.getConfiguration());
                            if (runner != null) {
                                ExecutionEnvironment env = new ExecutionEnvironment(executor, runner, config, project);
                                runner.execute(env);
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            }, delay, TimeUnit.SECONDS);

            delay += delaySeconds; // Add delay for the next configuration
        }
    }
}

class StyledPanel extends JPanel {
    public StyledPanel(LayoutManager layout) {
        super(layout);
        setBackground(UIUtil.getPanelBackground());
        setBorder(JBUI.Borders.empty(10));
    }
}

class ServiceSelectionDialog extends DialogWrapper {
    private final List<RunnerAndConfigurationSettings> configurations;
    private final JTable table;
    private final DefaultTableModel tableModel;
    private JSpinner delaySpinner;
    private final Project project;

    protected ServiceSelectionDialog(Project project, List<RunnerAndConfigurationSettings> configurations) {
        super(project);
        this.project = project;
        this.configurations = configurations;

        // Initialize table model
        tableModel = new DefaultTableModel(new Object[]{"Service", "Run", "Debug"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 0; // Only allow editing Run and Debug columns
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1 || columnIndex == 2) {
                    return Boolean.class; // Render Run and Debug columns as checkboxes
                }
                return super.getColumnClass(columnIndex);
            }
        };

        // Load saved order
        SuperRunSettings settings = SuperRunSettings.getInstance(project);
        if (settings != null) {
            List<String> savedOrder = settings.getState().configurationOrder;
            if (!savedOrder.isEmpty()) {
                // Reorder configurations based on saved order
                configurations.sort((a, b) -> {
                    int indexA = savedOrder.indexOf(a.getUniqueID());
                    int indexB = savedOrder.indexOf(b.getUniqueID());
                    return Integer.compare(indexA, indexB);
                });
            }
        }

        // Populate table model with configurations
        for (RunnerAndConfigurationSettings config : configurations) {
            tableModel.addRow(new Object[]{config.getConfiguration().getName(), false, false});
        }

        // Create table
        table = new JBTable(tableModel);
        table.setRowSelectionAllowed(true); // Allow row selection
        table.setFocusable(true); // Ensure the table is focusable

        // Add double-click listener
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.out.println("Mouse clicked: " + e.getClickCount()); // Debug statement
                if (e.getClickCount() == 2) { // Double-click
                    System.out.println("Double-click detected"); // Debug statement
                    editSelectedConfiguration();
                }
            }
        });


        init();
        setTitle("Select Services to Run");
    }

    @Override
    protected JComponent createCenterPanel() {
        StyledPanel mainPanel = new StyledPanel(new BorderLayout(0, JBUI.scale(10)));

        // Add delay input field
        JPanel delayPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        delayPanel.add(new JLabel("Delay between startups (seconds):"));
        delaySpinner = new JSpinner(new SpinnerNumberModel(5, 0, 60, 1));
        delayPanel.add(delaySpinner);
        mainPanel.add(delayPanel, BorderLayout.NORTH);

        // Wrap table in a scroll pane
        JBScrollPane scrollPane = new JBScrollPane(table);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setPreferredSize(new Dimension(500, 300));

        // Add up and down buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton upButton = new JButton("↑ Up");
        JButton downButton = new JButton("↓ Down");

        upButton.addActionListener(e -> moveRow(-1)); // Move selected row up
        downButton.addActionListener(e -> moveRow(1)); // Move selected row down

        buttonPanel.add(upButton);
        buttonPanel.add(downButton);

        // Add components to main panel
        mainPanel.add(buttonPanel, BorderLayout.CENTER);
        mainPanel.add(scrollPane, BorderLayout.SOUTH);

        return mainPanel;
    }

    private void moveRow(int direction) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= tableModel.getRowCount()) {
            return; // No row selected or invalid row
        }

        int newRow = selectedRow + direction;
        if (newRow < 0 || newRow >= tableModel.getRowCount()) {
            return; // Cannot move outside table bounds
        }

        // Swap rows in the table model
        tableModel.moveRow(selectedRow, selectedRow, newRow);

        // Swap rows in the configurations list
        Collections.swap(configurations, selectedRow, newRow);

        // Update the selected row
        table.setRowSelectionInterval(newRow, newRow);

        // Save the new order
        saveConfigurationOrder();
    }

    private void editSelectedConfiguration() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            System.out.println("No valid row selected"); // Debug statement
            return;
        }

        int modelRow = table.convertRowIndexToModel(selectedRow);
        if (modelRow >= configurations.size()) {
            System.out.println("Invalid row index: " + modelRow);
            return;
        }

        RunnerAndConfigurationSettings selectedConfig = configurations.get(modelRow);
        if (selectedConfig == null) {
            System.out.println("Selected configuration is null");
            return;
        }

        System.out.println("Selected configuration: " + selectedConfig.getName()); // Debug statement

        // Set the selected configuration as the active configuration
        RunManager runManager = RunManager.getInstance(project);
        runManager.setSelectedConfiguration(selectedConfig);

        // Open the Run Configuration Editor
        EditConfigurationsDialog dialog = new EditConfigurationsDialog(project);
        dialog.show(); // This should now open with the selected config preselected
    }


    public Map<RunnerAndConfigurationSettings, Boolean> getSelectedConfigurations() {
        Map<RunnerAndConfigurationSettings, Boolean> selected = new LinkedHashMap<>();

        for (int i = 0; i < configurations.size(); i++) {
            boolean runSelected = (Boolean) tableModel.getValueAt(i, 1);
            boolean debugSelected = (Boolean) tableModel.getValueAt(i, 2);

            if (runSelected || debugSelected) {
                selected.put(configurations.get(i), debugSelected);
            }
        }

        return selected;
    }

    public int getDelaySeconds() {
        return (int) delaySpinner.getValue();
    }

    // Getter for configurations
    public List<RunnerAndConfigurationSettings> getConfigurations() {
        return configurations;
    }

    // Save the order of configurations
    public void saveConfigurationOrder() {
        SuperRunSettings settings = SuperRunSettings.getInstance(project);
        if (settings != null) {
            List<String> order = new ArrayList<>();
            for (RunnerAndConfigurationSettings config : configurations) {
                order.add(config.getUniqueID());
            }
            settings.getState().configurationOrder = order;
        }
    }
}