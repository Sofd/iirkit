package de.sofd.iirkit;

import java.awt.BorderLayout;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import javax.swing.JPanel;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JToolBar;

/**
 * A BR frame window.
 */
public class BRFrameView extends FrameView {

    private final List<BRViewPanel> viewPanels = new ArrayList<BRViewPanel>();
    private List<BRViewPanel> activeViewPanels;
    private final Map<String, Object> attributes = new HashMap<String, Object>();

    public BRFrameView(App app, int frameNumber) {
        this(app,
             GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0].getDefaultConfiguration(),
             0);
    }

    public BRFrameView(App app, GraphicsConfiguration graphicsConfiguration, int frameNumber) {

        super(app);

        this.frameNumber = frameNumber;

        setFrame(new JFrame(createTitle(), graphicsConfiguration));
        getFrame().setIconImage(new javax.swing.ImageIcon(getClass().getResource("/de/sofd/iirkit/resources/favicon.png")).getImage());

        initComponents();

        mainToolBar = new JToolBar();
        mainToolBar.setFloatable(false);
        mainPanel.add(mainToolBar, BorderLayout.PAGE_START);

        listsPanel = new JPanel();
        mainPanel.add(listsPanel, BorderLayout.CENTER);

        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

            @Override
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String) (evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer) (evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = App.getApplication().getMainFrame();
            aboutBox = new BRAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        App.getApplication().show(aboutBox);
    }

    @Action
    public void keyboardMouseHelp() {
        if (keyAndMouseHelpBox == null) {
            JFrame mainFrame = App.getApplication().getMainFrame();
            keyAndMouseHelpBox = new KeystrokesHelpDialog(mainFrame, false);
            keyAndMouseHelpBox.setLocationRelativeTo(mainFrame);
        }
        if (!keyAndMouseHelpBox.isVisible()) {
            App.getApplication().show(keyAndMouseHelpBox);
        }
    }

    private String createTitle() {
        return getResourceMap().getString("Application.title") + " - Window " + frameNumber;
    }

    public JToolBar getMainToolBar() {
        return mainToolBar;
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public JPanel getListsPanel() {
        return listsPanel;
    }

    public int getViewPanelsCount() {
        return viewPanels.size();
    }

    public List<BRViewPanel> getViewPanels()  {
        return Collections.unmodifiableList(viewPanels);
    }

    public BRViewPanel getViewPanel(int i) {
        return viewPanels.get(i);
    }

    void addViewPanel(BRViewPanel p) {
        viewPanels.add(p);
    }

    void setActiveViewPanelsCount(int count) {
        assert(count <= viewPanels.size());
        activeViewPanels = Collections.unmodifiableList(viewPanels.subList(0, count));
    }

    public List<BRViewPanel> getActiveViewPanels() {
        return Collections.unmodifiableList(activeViewPanels);
    }

    public void setDisplayRange(int firstIdx, int lastIdx) {
        assert(lastIdx < viewPanels.size());
        listsPanel.removeAll();
        int count = lastIdx - firstIdx + 1;
        if (count < 6) {
            //1-row layout
            listsPanel.setLayout(new GridLayout(1, count, 10, 0));
        } else {
            //2-rows layout
            count = (count / 2) * 2 + (count % 2) * 2;  // next multiple of 2 => we may end up displaying lastIdx + 1
            int columnCount = count / 2;
            listsPanel.setLayout(new GridLayout(2, columnCount, 10, 0));
        }

        for (int i = 0; i < count; i++) {
            int idx = firstIdx + i;
            BRViewPanel viewPanel = viewPanels.get(idx);
            listsPanel.add(viewPanel);
        }
    }

    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public Set<String> getAttributeNames() {
        return attributes.keySet();
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public Object putAttribute(String name, Object value) {
        return attributes.put(name, value);
    }

    public Object removeAttribute(String name) {
        return attributes.remove(name);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        shortcutsMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(de.sofd.iirkit.App.class).getContext().getResourceMap(BRFrameView.class);
        mainPanel.setBackground(resourceMap.getColor("mainPanel.background")); // NOI18N
        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setLayout(new java.awt.BorderLayout());

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(de.sofd.iirkit.App.class).getContext().getActionMap(BRFrameView.class, this);
        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        shortcutsMenuItem.setAction(actionMap.get("keyboardMouseHelp")); // NOI18N
        shortcutsMenuItem.setText(resourceMap.getString("shortcutsMenuItem.text")); // NOI18N
        shortcutsMenuItem.setName("shortcutsMenuItem"); // NOI18N
        helpMenu.add(shortcutsMenuItem);

        jSeparator1.setName("jSeparator1"); // NOI18N
        helpMenu.add(jSeparator1);

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setBackground(resourceMap.getColor("statusPanel.background")); // NOI18N
        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        org.jdesktop.layout.GroupLayout statusPanelLayout = new org.jdesktop.layout.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(statusPanelSeparator, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
            .add(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(statusMessageLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 204, Short.MAX_VALUE)
                .add(progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(statusAnimationLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(statusPanelLayout.createSequentialGroup()
                .add(statusPanelSeparator, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(statusMessageLabel)
                    .add(statusAnimationLabel)
                    .add(progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(3, 3, 3))
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPopupMenu.Separator jSeparator1;
    protected javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JMenuItem shortcutsMenuItem;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    // End of variables declaration//GEN-END:variables
    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;
    private JDialog aboutBox;
    private JDialog keyAndMouseHelpBox;
    private int frameNumber;
    final javax.swing.ActionMap finalActionMap = org.jdesktop.application.Application.getInstance(App.class).getContext().getActionMap(BRFrameView.class, this);
    final BRFrameView finalThis = this;
    protected JToolBar mainToolBar;
    protected JPanel listsPanel;
    boolean dummyActionTaskdoInBackground = false;
}
