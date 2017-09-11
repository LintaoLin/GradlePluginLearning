package me.ele.lint;

import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;


public class MethodNameCheckDetector extends Detector implements Detector.JavaPsiScanner{

  public static final Issue ISSUE = Issue.create("MethodNameCheckDetector","test","test", Category.CORRECTNESS, 5,
      Severity.ERROR, new Implementation(MethodNameCheckDetector.class, Scope.JAVA_FILE_SCOPE));

  @Override public List<Class<? extends PsiElement>> getApplicablePsiTypes() {
    return Arrays.asList(PsiMethod.class);
  }

  @Override public JavaElementVisitor createPsiVisitor(JavaContext context) {
    return new JavaElementVisitor() {
      @Override public void visitMethod(PsiMethod method) {
        if (method.getName().startsWith("fuck")) {
          context.report(ISSUE, method, context.getLocation(method.getNameIdentifier()), "文明写码从我做起");
        }
      }
    };
  }

  private static void logMethod(String method) {
    File file = new File("/Users/lint/Desktop/AndroidProject/GradlePlugin/lintMethodLog.txt");
    System.out.println("dependencies ----- " + method);
    if (!file.exists()) {
      try {
        file.createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    try (
        FileWriter fileWriter = new FileWriter(file, true);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        PrintWriter out = new PrintWriter(bufferedWriter)) {
      out.printf("%d %s\n",System.currentTimeMillis(), method);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
