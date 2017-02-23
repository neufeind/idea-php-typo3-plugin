package com.cedricziel.idea.typo3.action;

import com.cedricziel.idea.typo3.TYPO3CMSIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ToolbarItemAction extends DumbAwareAction {

    public ToolbarItemAction() {
        super("ToolbarItem", "Create a Toolbar Item", TYPO3CMSIcons.TYPO3_ICON);
    }

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {

    }
}
