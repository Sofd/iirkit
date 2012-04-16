package de.sofd.iirkit;

import de.sofd.iirkit.service.Case;
import java.util.List;

/**
 *
 * @author olaf
 */
public interface BRContext {
    Case getCurrentCase();
    List<BRFrameView> getCurrentCaseFrames();

    boolean isReadOnly();

    boolean isShowPreviousResult();
}
