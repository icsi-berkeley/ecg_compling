package compling.gui.grammargui.util;

import java.util.List;

public interface IComposite<T> extends IElement<T> {
	List<? extends IElement<T>> children();
}
