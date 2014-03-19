package compling.gui.grammargui.util;

public interface IElement<T> {
	IElement<T> parent();
	T content();
}