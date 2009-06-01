package com.intellij.packageDependencies.ui;

import com.intellij.analysis.AnalysisScopeBundle;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.*;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public abstract class UsagesPanel extends JPanel implements Disposable, DataProvider {
  protected static final Logger LOG = Logger.getInstance("#com.intellij.packageDependencies.ui.UsagesPanel");

  private final Project myProject;
  protected ProgressIndicator myCurrentProgress;
  private JComponent myCurrentComponent;
  private UsageView myCurrentUsageView;
  protected final Alarm myAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);

  public UsagesPanel(Project project) {
    super(new BorderLayout());
    myProject = project;
  }

  public void setToInitialPosition() {
    cancelCurrentFindRequest();
    setToComponent(createLabel(getInitialPositionText()));
  }

  public abstract String getInitialPositionText();
  public abstract String getCodeUsagesString();


  protected void cancelCurrentFindRequest() {
    if (myCurrentProgress != null) {
      myCurrentProgress.cancel();
    }
  }

  protected void showUsages(final UsageInfoToUsageConverter.TargetElementsDescriptor descriptor, final UsageInfo[] usageInfos) {
    if (myCurrentUsageView != null) {
      Disposer.dispose(myCurrentUsageView);
    }
    try {
      Usage[] usages = UsageInfoToUsageConverter.convert(descriptor, usageInfos);
      UsageViewPresentation presentation = new UsageViewPresentation();
      presentation.setCodeUsagesString(getCodeUsagesString());
      myCurrentUsageView = UsageViewManager.getInstance(myProject).createUsageView(UsageTarget.EMPTY_ARRAY, usages, presentation, null);
      setToComponent(myCurrentUsageView.getComponent());
    }
    catch (ProcessCanceledException e) {
      setToCanceled();
    }
  }

  private void setToCanceled() {
    setToComponent(createLabel(AnalysisScopeBundle.message("usage.view.canceled")));
  }

  protected void setToComponent(final JComponent cmp) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        if (myCurrentComponent != null) {
          if (myCurrentUsageView != null && myCurrentComponent == myCurrentUsageView.getComponent()){
            myCurrentUsageView.dispose();
          }
          remove(myCurrentComponent);
        }
        myCurrentComponent = cmp;
        add(cmp, BorderLayout.CENTER);
        revalidate();
      }
    });
  }

  public void dispose(){
    if (myCurrentUsageView != null){
      Disposer.dispose(myCurrentUsageView);
    }
  }

  private static JComponent createLabel(String text) {
    JLabel label = new JLabel(text);
    label.setHorizontalAlignment(SwingConstants.CENTER);
    return label;
  }

  @Nullable
  @NonNls
  public Object getData(@NonNls String dataId) {
    if (dataId.equals(DataConstants.HELP_ID)) {
      return "ideaInterface.find";
    }
    return null;
  }
}
