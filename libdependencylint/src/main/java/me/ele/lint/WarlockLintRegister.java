package me.ele.lint;

import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.Issue;
import java.util.Arrays;
import java.util.List;

public class WarlockLintRegister extends IssueRegistry {
  @Override public List<Issue> getIssues() {
    return Arrays.asList(MethodNameCheckDetector.ISSUE, WarlockDependencyDetector.ISSUE);
  }
}
