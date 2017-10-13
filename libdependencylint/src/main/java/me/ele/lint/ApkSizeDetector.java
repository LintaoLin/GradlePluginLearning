package me.ele.lint;

import com.android.resources.ResourceFolderType;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import java.io.File;
import java.util.EnumSet;

public class ApkSizeDetector extends Detector implements Detector.OtherFileScanner,
    Detector.ClassScanner{

  public static final Issue ISSUE = Issue.create("ApkSizeDetector", "ApkSizeDetector","ApkSizeDetector"
  , Category.SECURITY, 10, Severity.ERROR, new Implementation(ApkSizeDetector.class, Scope.OTHER_SCOPE));

  @Override public EnumSet<Scope> getApplicableFiles() {
    return Scope.ALL;
  }

  @Override public void run(Context context) {
    super.run(context);
    if (context.getProject().getReportIssues()) {
      File file = context.file;
      //if (file.isFile() && LintUtils.endsWith(file.getAbsolutePath(), ".apk")) {
      //  if (file.length() > 1000) {
          String message = String.format("the apk is bigger than 1000, is %d", file.length());
          context.report(ISSUE, Location.create(file), message);
        //}
      //}
    }
  }
}
