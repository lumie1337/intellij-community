// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.util.scopeChooser;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.NamedConfigurable;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;
import com.intellij.packageDependencies.DependencyValidationManager;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.search.scope.packageSet.NamedScopeManager;
import com.intellij.psi.search.scope.packageSet.NamedScopesHolder;
import com.intellij.psi.search.scope.packageSet.PackageSet;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ScopeConfigurable extends NamedConfigurable<NamedScope> {
  private final Disposable myDisposable = Disposer.newDisposable();
  private NamedScope myScope;
  private ScopeEditorPanel myPanel;
  private String myPackageSet;
  private final JCheckBox mySharedCheckbox;
  private boolean myShareScope = false;
  private final Project myProject;
  private Icon myIcon;

  public ScopeConfigurable(final NamedScope scope, final boolean shareScope, final Project project, final Runnable updateTree) {
    super(true, updateTree);
    myScope = scope;
    myShareScope = shareScope;
    myProject = project;
    mySharedCheckbox = new JCheckBox(IdeBundle.message("share.scope.checkbox.title"), shareScope);
    myPanel = new ScopeEditorPanel(project, getHolder());
    myIcon = getHolder(myShareScope).getIcon();
    mySharedCheckbox.addActionListener(e -> {
      myIcon = getHolder().getIcon();
      myPanel.setHolder(getHolder());
    });
  }

  @Override
  public void setDisplayName(final String name) {
    if (Comparing.strEqual(myScope.getName(), name)){
      return;
    }
    final PackageSet packageSet = myScope.getValue();
    myScope = new NamedScope(name, myIcon, packageSet != null ? packageSet.createCopy() : null);
  }

  @Override
  public NamedScope getEditableObject() {
    return new NamedScope(myScope.getName(), myIcon, myPanel.getCurrentScope());
  }

  @Override
  public String getBannerSlogan() {
    return IdeBundle.message("scope.banner.text", myScope.getName());
  }

  @Override
  public String getDisplayName() {
    return myScope.getName();
  }

  public NamedScopesHolder getHolder() {
    return getHolder(mySharedCheckbox.isSelected());
  }

  private NamedScopesHolder getHolder(boolean local) {
    return (NamedScopesHolder)(local
            ? DependencyValidationManager.getInstance(myProject)
            : NamedScopeManager.getInstance(myProject));
  }

  @Override
  @Nullable
  @NonNls
  public String getHelpTopic() {
    return "project.scopes";
  }

  @Override
  public JComponent createOptionsPanel() {
    final JPanel wholePanel = new JPanel(new BorderLayout());
    wholePanel.add(myPanel.getPanel(), BorderLayout.CENTER);
    wholePanel.add(mySharedCheckbox, BorderLayout.SOUTH);
    wholePanel.setBorder(JBUI.Borders.empty(0, 10, 10, 10));
    return wholePanel;
  }

  @Override
  public boolean isModified() {
    if (mySharedCheckbox.isSelected() != myShareScope) return true;
    final PackageSet currentScope = myPanel.getCurrentScope();
    return !Comparing.strEqual(myPackageSet, currentScope != null ? currentScope.getText() : null);
  }

  @Override
  public void apply() throws ConfigurationException {
    try {
      myPanel.apply();
      final PackageSet packageSet = myPanel.getCurrentScope();
      myScope = new NamedScope(myScope.getName(), myIcon, packageSet);
      myPackageSet = packageSet != null ? packageSet.getText() : null;
      myShareScope = mySharedCheckbox.isSelected();
    }
    catch (ConfigurationException e) {
      //was canceled - didn't change anything
    }
  }

  @Override
  public void reset() {
    mySharedCheckbox.setSelected(myShareScope);
    myPanel.reset(myScope.getValue(), null);
    final PackageSet packageSet = myScope.getValue();
    myPackageSet = packageSet != null ? packageSet.getText() : null;
  }

  @Override
  public void disposeUIResources() {
    if (myPanel != null){
      myPanel.cancelCurrentProgress();
      myPanel.clearCaches();
      Disposer.dispose(myDisposable);
      myPanel = null;
    }
  }

  public void cancelCurrentProgress(){
    if (myPanel != null) { //not disposed
      myPanel.cancelCurrentProgress();
    }
  }

  public NamedScope getScope() {
    return myScope;
  }

  public void restoreCanceledProgress() {
    if (myPanel != null) {
      myPanel.restoreCanceledProgress();
    }
  }

  @Nullable
  @Override
  public Icon getIcon(boolean expanded) {
    return myIcon;
  }
}
