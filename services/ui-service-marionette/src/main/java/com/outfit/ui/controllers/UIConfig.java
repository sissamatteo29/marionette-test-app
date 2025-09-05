package com.outfit.ui.controllers;

public class UIConfig {

    public String decideUI() {
        switch(com.outfit.ui.__marionette.BehaviourRegistry.getBehaviourId("com/outfit/ui/controllers/UIConfig.java", "decideUI")) {
            case "beautiful":
                return decideUI_beautiful();
            default:
            case "default":
                return decideUI_default();
        }
    }

    public String decideUI_beautiful() {
        return "home_beautiful";
    }

    public String decideUI_default() {
        return "home";
    }
}
