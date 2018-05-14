package pl.poznan.put.gui.component;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;

/**
 * An extension of JCheckBoxMenuItem that doesn't close the menu when selected.
 *
 * @author Darryl
 */
public class StayOpenCheckBoxMenuItem extends JCheckBoxMenuItem {
  private static final long serialVersionUID = 1L;
  private MenuElement[] path;

  /** @see JCheckBoxMenuItem#JCheckBoxMenuItem(String, boolean) */
  public StayOpenCheckBoxMenuItem(final String text, final boolean selected) {
    super(text, selected);

    getModel()
        .addChangeListener(
            event -> {
              if (getModel().isArmed() && isShowing()) {
                path = MenuSelectionManager.defaultManager().getSelectedPath();
              }
            });
  }

  /**
   * Overridden to reopen the menu.
   *
   * @param pressTime the time to "hold down" the button, in milliseconds
   */
  @Override
  public final void doClick(final int pressTime) {
    super.doClick(pressTime);
    MenuSelectionManager.defaultManager().setSelectedPath(path);
  }
}
