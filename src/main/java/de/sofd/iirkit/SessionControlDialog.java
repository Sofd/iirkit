package de.sofd.iirkit;

import de.sofd.iirkit.form.FormRunner;
import de.sofd.iirkit.service.Case;
import de.sofd.iirkit.service.IirService;
import de.sofd.iirkit.service.User;
import java.awt.Toolkit;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedHashMap;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jdesktop.application.Action;

/**
 *
 * @author sofd
 */
public class SessionControlDialog extends javax.swing.JDialog {

    private static final Logger logger = Logger.getLogger(SessionControlDialog.class);

    private App app;
    private final IirService iirService;
    private final SecurityContext securityContext;
    private final CaseRunner caseRunner; //TODO: make this per - *SessionRunner? (but then we must take care that there aren't multiple frame sets)

    /** Creates new form SessionSelectionDialog */
    public SessionControlDialog(App app, IirService iirSvc, BRHandler brHandler, SecurityContext securityCtx, java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        this.app = app;
        caseRunner = new CaseRunner(app, brHandler);
        initComponents();
        setModal(modal);

        this.iirService = iirSvc;
        this.securityContext = securityCtx;

        //clearButton.setEnabled(false);
        rereadButton.setEnabled(false);
        logButton.setEnabled(false);
        okButton.setEnabled(false);
        auditButton.setEnabled(false);
        //unlockButton.setEnabled(false);
        if (securityContext.getAuthority().equals(SecurityContext.Authority.Manager)) {
            //unlockButton.setVisible(true);
        } else {
            //unlockButton.setVisible(false);
        }

        this.addWindowListener(new java.awt.event.WindowAdapter() {

            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                Toolkit.getDefaultToolkit().beep();
                System.exit(0);
            }
        });

        final DefaultListModel listModel = new DefaultListModel();
        for (User user : iirService.getAllUsers()) {
            listModel.addElement(user);
        }
        sessionList.setModel(listModel);
        sessionList.setCellRenderer(new SessionListCellRenderer(iirService, securityContext));
        sessionList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                ListSelectionModel lsm = (ListSelectionModel) e.getSource();
                if (!e.getValueIsAdjusting()) {
                    if (!lsm.isSelectionEmpty()) {
                        User selectedUser = (User) listModel.get(lsm.getLeadSelectionIndex());
                        int nCases = iirService.getNumberOfCasesOf(selectedUser);
                        int nDoneCases = iirService.getNumberOfDoneCasesOf(selectedUser);
                        int nRemainingCases = nCases - nDoneCases;
                        rereadButton.setEnabled(nDoneCases > 0 && selectedUser.equals(securityContext.getUser()));
                        //unlockButton.setEnabled(securityContext.isLocked(selectedUser.getName()));
                        okButton.setEnabled(nRemainingCases > 0 && selectedUser.equals(securityContext.getUser()));
                        //clearButton.setEnabled(false);
                        logButton.setEnabled(true);
                        auditButton.setEnabled(/*nDoneCases > 0 && */securityContext.getUser().hasRole(User.ROLE_ADMIN));
                    } else {
                        okButton.setEnabled(false);
                        //clearButton.setEnabled(false);
                        logButton.setEnabled(true);
                        rereadButton.setEnabled(false);
                        //unlockButton.setEnabled(false);
                        auditButton.setEnabled(false);
                    }
                }
            }
        });
        setTitle(StringUtils.substringBefore(getTitle(), "-") + "- " + securityContext.getUser().getName() + " - " + securityContext.getAuthority());
    }

    @Override
    public java.awt.Frame getParent() {
        return (java.awt.Frame) super.getParent();
    }

    @Action
    public void okAction() {
        setVisible(false);
        new SessionRunner().runSession();
    }

    protected class SessionRunner {
        /**
         * Run all remaining cases of the logged-in user (securityContext.getUser()),
         * writing the results to the database.
         */
        public void runSession() {
            User user = securityContext.getUser();
            final Case currentCase = iirService.getNextCaseOf(user);
            if (null == currentCase) {
                caseRunner.closeCase();
                //return; //may send a "finished" event rather than exiting so the program can continue
                FormRunner.dispose();
                logger.info("DONE!");
                System.exit(0);
            }
            caseRunner.addCaseListener(new CaseAdapter() {
                @Override
                public void caseSubmitted(CaseEvent e) {
                    caseRunner.removeCaseListener(this);
                    iirService.update(currentCase);
                    logger.info("Case " + currentCase + " updated with result: " + currentCase.getResult());
                    runSession();
                }
                @Override
                public void caseClosed(CaseEvent e) {
                    caseRunner.removeCaseListener(this);
                    logger.info("case cancelled.");
                    FormRunner.dispose();
                    System.exit(0); //TODO: re-show the session control dialog instead?
                }
            });
            logger.info("starting case: " + currentCase);
            caseRunner.openCase(currentCase);
        }

    }

    @Action
    public void cancelAction() {
        Toolkit.getDefaultToolkit().beep();
        System.exit(0);
    }

    @Action
    public void auditAction() {
        User u = (User) sessionList.getSelectedValue();
        if (u != null && iirService.getNumberOfCasesOf(u) > 0) {
            new AuditSessionRunner(u).runSession();
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    protected class AuditSessionRunner {
        private final User user;
        private CaseSelectionDialog csdiag;
        private CaseListener caseListener;

        public AuditSessionRunner(User user) {
            this.user = user;
        }

        public void runSession() {
            SessionControlDialog.this.setVisible(false);
            csdiag = new CaseSelectionDialog(SessionControlDialog.this.getParent(), user, iirService);
            csdiag.addSelectedCaseChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    Case c = (Case) evt.getNewValue();
                    if (null != c) {
                        caseRunner.openCase(c, true, true);
                    }
                }
            });
            caseRunner.addCaseListener(caseListener = new CaseAdapter() {
                @Override
                public void caseSubmitted(CaseEvent e) {
                    Case nextCase  = iirService.getCaseOf(user,  caseRunner.getCurrentCase().getNumber() + 1);
                    if (nextCase != null) {
                        caseRunner.openCase(nextCase, true, true);
                    } else {
                        exitSession();
                    }
                }
            });
            csdiag.addClosedListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    exitSession();
                }
            });
            csdiag.setVisible(true);
            //TODO: sync csdiag's case list selection
            caseRunner.openCase(iirService.getCaseOf(user, 1), true, true);
        }

        private void exitSession() {
            caseRunner.closeCase();
            caseRunner.removeCaseListener(caseListener);
            csdiag.close();
            SessionControlDialog.this.setVisible(true);
        }

    }

    @Action
    public void logAction() {
        String logText = (String) JOptionPane.showInputDialog(this, "Enter log text:");
        if (logText != null) {
            logger.info(logText);
        }
    }

    @Action
    public void unlockAction() {
        /*
        if (sessionList.getSelectedIndex() > -1) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            Context.getSecurityContext().unlock(((SessionContextElement) sessionList.getSelectedValue()).getUser().getUsername());
            DefaultListModel defaultListModel = new DefaultListModel();
            for (SessionContextElement sessionContextElementFor : Context.getBlindedReadingContext().createSessionContextElementList()) {
                defaultListModel.addElement(sessionContextElementFor);
            }
            sessionList.setModel(defaultListModel);
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
         */
    }

    @Action
    public void rereadAction() {
        if (sessionList.getSelectedIndex() > -1) {
            AuthenticationDialog authenticationDialog = new AuthenticationDialog(securityContext, new JFrame(), true, SecurityContext.SUPERADMIN_USERNAME);
            authenticationDialog.setLocationRelativeTo(this);
            authenticationDialog.setVisible(true);
            if (authenticationDialog.getAuthenticationResult() != null && authenticationDialog.getAuthenticationResult().equals(SecurityContext.AuthenticationResult.OK)) {
                LinkedHashMap<Integer, Case> rereadCasesByNumber = new LinkedHashMap<Integer, Case>();
                for (Case c : iirService.getCasesOf(securityContext.getUser())) {
                    if (c.getResult() != null) {
                        rereadCasesByNumber.put(c.getNumber(), c);
                    }
                }
                Integer caseNr = (Integer) JOptionPane.showInputDialog(
                        this,
                        "Select case to reread",
                        "Reread Case",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        rereadCasesByNumber.keySet().toArray(),
                        null);
                if (caseNr != null) {
                    String logText = (String) JOptionPane.showInputDialog(this, "Reread - Case " + caseNr + ". Enter reason:", "Reread Case" + caseNr, JOptionPane.QUESTION_MESSAGE);
                    if (StringUtils.isEmpty(logText)) {
                        JOptionPane.showMessageDialog(this, "No reason entered. Reread canceled.", "Warning", JOptionPane.WARNING_MESSAGE);
                    } else {
                        setVisible(false);
                        final Case c = rereadCasesByNumber.get(caseNr);
                        caseRunner.addCaseListener(new CaseAdapter() {

                            @Override
                            public void caseSubmitted(CaseEvent e) {
                                caseRunner.removeCaseListener(this);
                                caseRunner.closeCase();
                                iirService.update(c);
                                logger.info("Case " + c + " updated with result: " + c.getResult());
                                SessionControlDialog.this.setVisible(true);
                            }
                            @Override
                            public void caseClosed(CaseEvent e) {
                                caseRunner.removeCaseListener(this);
                                logger.info("case cancelled.");
                                FormRunner.dispose();
                                System.exit(0); //TODO: re-show the session control dialog instead?
                            }
                        });
                        logger.info("re-reading case: " + c);
                        logger.info("  re-read reason: " + logText);
                        caseRunner.openCase(c);
                    }
                }
            }
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    @Action
    public void clearAction() {
        /*
        if (sessionList.getSelectedIndex() > -1) {
            int n = JOptionPane.showConfirmDialog(
                    this,
                    "Would you really want to clear form data?",
                    "Confirm",
                    JOptionPane.YES_NO_OPTION);
            if (n == JOptionPane.YES_OPTION) {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                Context.getBlindedReadingContext().clear(sessionList.getSelectedIndex());
                DefaultListModel defaultListModel = new DefaultListModel();
                for (SessionContextElement sessionContextElementFor : Context.getBlindedReadingContext().createSessionContextElementList()) {
                    defaultListModel.addElement(sessionContextElementFor);
                }
                sessionList.setModel(defaultListModel);
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
         */
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        exportFileChooser = new javax.swing.JFileChooser();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        rereadButton = new javax.swing.JButton();
        logButton = new javax.swing.JButton();
        auditButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        sessionList = new javax.swing.JList();

        exportFileChooser.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);
        exportFileChooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
        exportFileChooser.setName("exportFileChooser"); // NOI18N

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(de.sofd.iirkit.App.class).getContext().getResourceMap(SessionControlDialog.class);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setAlwaysOnTop(true);
        setModal(true);
        setName("Form"); // NOI18N

        jPanel1.setName("jPanel1"); // NOI18N

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel1)
                    .add(jLabel2))
                .addContainerGap(903, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jLabel2)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2.setName("jPanel2"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(de.sofd.iirkit.App.class).getContext().getActionMap(SessionControlDialog.class, this);
        okButton.setAction(actionMap.get("okAction")); // NOI18N
        okButton.setText(resourceMap.getString("okButton.text")); // NOI18N
        okButton.setName("okButton"); // NOI18N
        okButton.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                okButtonKeyPressed(evt);
            }
        });

        cancelButton.setAction(actionMap.get("cancelAction")); // NOI18N
        cancelButton.setText(resourceMap.getString("cancelButton.text")); // NOI18N
        cancelButton.setName("cancelButton"); // NOI18N
        cancelButton.setNextFocusableComponent(okButton);
        cancelButton.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                cancelButtonKeyPressed(evt);
            }
        });

        rereadButton.setAction(actionMap.get("rereadAction")); // NOI18N
        rereadButton.setText(resourceMap.getString("rereadButton.text")); // NOI18N
        rereadButton.setName("rereadButton"); // NOI18N

        logButton.setAction(actionMap.get("logAction")); // NOI18N
        logButton.setText(resourceMap.getString("logButton.text")); // NOI18N
        logButton.setName("logButton"); // NOI18N

        auditButton.setAction(actionMap.get("auditAction")); // NOI18N
        auditButton.setText(resourceMap.getString("auditButton.text")); // NOI18N
        auditButton.setName("auditButton"); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(rereadButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(logButton)
                .add(18, 18, 18)
                .add(auditButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 589, Short.MAX_VALUE)
                .add(cancelButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(okButton)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(okButton)
                    .add(cancelButton)
                    .add(rereadButton)
                    .add(logButton)
                    .add(auditButton))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        sessionList.setFont(resourceMap.getFont("sessionList.font")); // NOI18N
        sessionList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        sessionList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        sessionList.setName("sessionList"); // NOI18N
        sessionList.setNextFocusableComponent(cancelButton);
        sessionList.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                sessionListKeyPressed(evt);
            }
        });
        jScrollPane1.setViewportView(sessionList);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 992, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 196, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void okButtonKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_okButtonKeyPressed
        // TODO add your handling code here:
        //javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(de.sofd.hieronymusr312046.HieronymusR312046App.class).getContext().getActionMap(SessionControlDialog.class, this);
        //javax.swing.Action goAction = actionMap.get("okAction");
        //goAction.actionPerformed(null);
        okButton.doClick();
    }//GEN-LAST:event_okButtonKeyPressed

    private void cancelButtonKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_cancelButtonKeyPressed
        // TODO add your handling code here:
        cancelButton.doClick();
    }//GEN-LAST:event_cancelButtonKeyPressed

    private void sessionListKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_sessionListKeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_sessionListKeyPressed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton auditButton;
    protected javax.swing.JButton cancelButton;
    private javax.swing.JFileChooser exportFileChooser;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton logButton;
    protected javax.swing.JButton okButton;
    private javax.swing.JButton rereadButton;
    private javax.swing.JList sessionList;
    // End of variables declaration//GEN-END:variables
}
