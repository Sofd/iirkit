package de.sofd.iirkit;

import de.sofd.iirkit.service.IirService;
import de.sofd.iirkit.service.User;
import java.awt.Color;
import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import sun.swing.DefaultLookup;

/**
 *
 * @author sofd
 */
public class SessionListCellRenderer extends DefaultListCellRenderer {

    private IirService iirService;
    private SecurityContext securityContext;

    public SessionListCellRenderer(IirService iirService, SecurityContext securityContext) {
        this.iirService = iirService;
        this.securityContext = securityContext;
    }

    @Override
    public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {
        setComponentOrientation(list.getComponentOrientation());

        Color bg = null;
        Color fg = null;

        JList.DropLocation dropLocation = list.getDropLocation();
        if (dropLocation != null
                && !dropLocation.isInsert()
                && dropLocation.getIndex() == index) {

            bg = DefaultLookup.getColor(this, ui, "List.dropCellBackground");
            fg = DefaultLookup.getColor(this, ui, "List.dropCellForeground");

            isSelected = true;
        }

        if (isSelected) {
            setBackground(bg == null ? list.getSelectionBackground() : bg);
            setForeground(fg == null ? list.getSelectionForeground() : fg);
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        if (value instanceof Icon) {
            setIcon((Icon) value);
            setText("");
        } else if (value instanceof User) {
            setIcon(null);
            User user = (User) value;
            int nCases = iirService.getNumberOfCasesOf(user);
            int nDoneCases = iirService.getNumberOfDoneCasesOf(user);
            if (true /*user not disabled*/) {
                if (user.equals(securityContext.getUser())) {
                    setForeground(nDoneCases == 0 ? Color.BLUE : Color.GREEN);
                } else {
                    setForeground(Color.GRAY);
                }
            } else {
                setForeground(Color.RED);
            }
            setText("(" + nDoneCases + "/" + nCases + ") " + user.getName());
        } else {
            setIcon(null);
            setText((value == null) ? "" : value.toString());
        }

        setEnabled(list.isEnabled());
        setFont(list.getFont());

        Border border = null;
        if (cellHasFocus) {
            if (isSelected) {
                border = DefaultLookup.getBorder(this, ui, "List.focusSelectedCellHighlightBorder");
            }
            if (border == null) {
                border = DefaultLookup.getBorder(this, ui, "List.focusCellHighlightBorder");
            }
        } else {
            border = new EmptyBorder(1, 1, 1, 1);
        }
        setBorder(border);

        return this;
    }
}
