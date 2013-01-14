package de.sofd.iirkit;

import de.sofd.iirkit.form.FormRunner;
import de.sofd.iirkit.service.Case;
import java.util.List;

/**
 *
 * @author olaf
 */
public interface BRContext {
    Case getCurrentCase();
    List<BRFrameView> getCurrentCaseFrames();
    FormRunner getFormRunner();

    boolean isReadOnly();

    boolean isShowPreviousResult();
}
