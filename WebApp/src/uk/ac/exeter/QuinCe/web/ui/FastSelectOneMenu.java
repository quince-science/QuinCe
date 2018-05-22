package uk.ac.exeter.QuinCe.web.ui;

import org.primefaces.component.selectonemenu.SelectOneMenu;

public class FastSelectOneMenu extends SelectOneMenu {
  @Override
  public String getEffectSpeed() {
    String effectSpeed = super.getEffectSpeed();
    if (effectSpeed == null) {
      effectSpeed = "fast";
    }
    return effectSpeed;
  }
}
