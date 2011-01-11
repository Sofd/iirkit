package de.sofd.iirkit;

import de.sofd.viskit.ui.imagelist.JImageListView;
import javax.swing.JPanel;
import java.awt.event.MouseListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author olaf
 */
public class BRViewPanel extends JPanel {

    private int panelIdx;
    private BRFrameView parentFrameView;
    private final Map<String, Object> attributes = new HashMap<String, Object>();

    public BRViewPanel(int idx) {
        this.panelIdx = idx;
    }

    public void setParentFrameView(BRFrameView parentFrameView) {
        this.parentFrameView = parentFrameView;
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

}
