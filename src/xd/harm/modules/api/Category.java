package xd.harm.modules.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Category {

    Combat("Combat", "C"),
    Movement("Movement", "B"),
    Render("Render", "E"),
    Player("Player", "A"),
    Misc("Misc", "D");

    private final String name;
    private final String icon;
}
