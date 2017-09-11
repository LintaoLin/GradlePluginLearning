package me.ele.lint;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.model.AndroidLibrary;
import com.android.builder.model.Dependencies;
import com.android.builder.model.MavenCoordinates;
import com.android.builder.model.Variant;
import com.android.ide.common.repository.GradleCoordinate;
import com.android.ide.common.repository.MavenRepositories;
import com.android.repository.Revision;
import com.android.repository.io.FileOpUtils;
import com.android.tools.lint.checks.GradleDetector;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.DefaultPosition;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.utils.Pair;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.File;
import java.util.List;
import java.util.Map;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.GroovyCodeVisitor;
import org.codehaus.groovy.ast.builder.AstBuilder;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.NamedArgumentListExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;

import static com.android.SdkConstants.GRADLE_PLUGIN_MINIMUM_VERSION;
import static com.android.ide.common.repository.GradleCoordinate.COMPARE_PLUS_HIGHER;

public class WarlockDependencyDetector extends GradleDetector {

  public static final Issue ISSUE = Issue.create(
      "WarlockModuleCheck",
      "check newest warlock module dependency",
      "this detector looks for usages of module where the version you are using is not the "
          + "current newest version. Using older versions may lead build error when dependence"
          + "module change it's interface.",
      Category.CORRECTNESS,
      4,
      Severity.WARNING,
      new Implementation(WarlockDependencyDetector.class, Scope.GRADLE_SCOPE)
  );

  @Override public boolean appliesTo(@NonNull Context context, @NonNull File file) {
    return true;
  }

  @Override public void visitBuildScript(@NonNull Context context, Map<String, Object> sharedData) {
    try {
      visitQuietly(context, sharedData);
    } catch (Throwable t) {
      // ignore
      // Parsing the build script can involve class loading that we sometimes can't
      // handle. This happens for example when running lint in build-system/tests/api/.
      // This is a lint limitation rather than a user error, so don't complain
      // about these. Consider reporting a Issue#LINT_ERROR.
    }
  }

  private void visitQuietly(@NonNull final Context context, Map<String, Object> sharedData) {
    CharSequence sequence = context.getContents();
    if (sequence == null) {
      return;
    }

    final String source = sequence.toString();
    List<ASTNode> astNodes = new AstBuilder().buildFromString(source);
    GroovyCodeVisitor visitor = new CodeVisitorSupport() {
      private List<MethodCallExpression> mMethodCallStack = Lists.newArrayList();

      @Override
      public void visitMethodCallExpression(MethodCallExpression expression) {
        mMethodCallStack.add(expression);
        super.visitMethodCallExpression(expression);
        Expression arguments = expression.getArguments();
        String parent = expression.getMethodAsString();
        String parentParent = getParentParent();
        if (arguments instanceof ArgumentListExpression) {
          ArgumentListExpression ale = (ArgumentListExpression) arguments;
          List<Expression> expressions = ale.getExpressions();
          if (expressions.size() == 1 &&
              expressions.get(0) instanceof ClosureExpression) {
            if (isInterestingBlock(parent, parentParent)) {
              ClosureExpression closureExpression =
                  (ClosureExpression) expressions.get(0);
              Statement block = closureExpression.getCode();
              if (block instanceof BlockStatement) {
                BlockStatement bs = (BlockStatement) block;
                for (Statement statement : bs.getStatements()) {
                  if (statement instanceof ExpressionStatement) {
                    ExpressionStatement e = (ExpressionStatement) statement;
                    if (e.getExpression() instanceof MethodCallExpression) {
                      checkDslProperty(parent,
                          (MethodCallExpression) e.getExpression(),
                          parentParent);
                    }
                  } else if (statement instanceof ReturnStatement) {
                    // Single item in block
                    ReturnStatement e = (ReturnStatement) statement;
                    if (e.getExpression() instanceof MethodCallExpression) {
                      checkDslProperty(parent,
                          (MethodCallExpression) e.getExpression(),
                          parentParent);
                    }
                  }
                }
              }
            }
          }
        } else if (arguments instanceof TupleExpression) {
          if (isInterestingStatement(parent, parentParent)) {
            TupleExpression te = (TupleExpression) arguments;
            Map<String, String> namedArguments = Maps.newHashMap();
            List<String> unnamedArguments = Lists.newArrayList();
            te.getExpressions().forEach(subExpr -> {
              if (subExpr instanceof NamedArgumentListExpression) {
                NamedArgumentListExpression nale = (NamedArgumentListExpression) subExpr;
                for (MapEntryExpression mae : nale.getMapEntryExpressions()) {
                  namedArguments.put(mae.getKeyExpression().getText(),
                      mae.getValueExpression().getText());
                }
              }
            });
            //for (Expression subExpr : te.getExpressions()) {
            //  if (subExpr instanceof NamedArgumentListExpression) {
            //    NamedArgumentListExpression nale = (NamedArgumentListExpression) subExpr;
            //    for (MapEntryExpression mae : nale.getMapEntryExpressions()) {
            //      namedArguments.put(mae.getKeyExpression().getText(),
            //          mae.getValueExpression().getText());
            //    }
            //  }
            //}
            checkMethodCall(context, parent, parentParent, namedArguments, unnamedArguments,
                expression);
          }
        }
        assert !mMethodCallStack.isEmpty();
        assert mMethodCallStack.get(mMethodCallStack.size() - 1) == expression;
        mMethodCallStack.remove(mMethodCallStack.size() - 1);
      }

      private String getParentParent() {
        for (int i = mMethodCallStack.size() - 2; i >= 0; i--) {
          MethodCallExpression expression = mMethodCallStack.get(i);
          Expression arguments = expression.getArguments();
          if (arguments instanceof ArgumentListExpression) {
            ArgumentListExpression ale = (ArgumentListExpression) arguments;
            List<Expression> expressions = ale.getExpressions();
            if (expressions.size() == 1 &&
                expressions.get(0) instanceof ClosureExpression) {
              return expression.getMethodAsString();
            }
          }
        }

        return null;
      }

      private void checkDslProperty(String parent, MethodCallExpression c,
          String parentParent) {
        String property = c.getMethodAsString();
        if (isInterestingProperty(property, parent, getParentParent())) {
          String value = getText(c.getArguments());
          checkDslPropertyAssignment(context, property, value, parent, parentParent, c, c);
        }
      }

      private String getText(ASTNode node) {
        Pair<Integer, Integer> offsets = getOffsets(node, context);
        return source.substring(offsets.getFirst(), offsets.getSecond());
      }
    };

    for (ASTNode node : astNodes) {
      node.visit(visitor);
    }
  }

  @Override
  protected void checkDslPropertyAssignment(
      @NonNull Context context,
      @NonNull String property,
      @NonNull String value,
      @NonNull String parent,
      @Nullable String parentParent,
      @NonNull Object valueCookie,
      @NonNull Object statementCookie) {

    if (parent.equals("dependencies")) {
      if (!value.startsWith("files('") && !value.endsWith("')")) {
        String dependency = getStringLiteralValue(value);
        if (dependency == null) {
          dependency = getNamedDependency(value);
        }
        // If the dependency is a GString (i.e. it uses Groovy variable substitution,
        // with a $variable_name syntax) then don't try to parse it.
        if (dependency != null) {
          GradleCoordinate gc = GradleCoordinate.parseCoordinateString(dependency);
          if (gc != null && dependency.contains("$")) {
            gc = resolveCoordinate(context, gc);
          }
          if (gc != null) {
            //if (gc.acceptsGreaterRevisions()) {
              //String message = "Avoid using + in version numbers; can lead "
              //    + "to unpredictable and unrepeatable builds (" + dependency + ")";
              //report(context, valueCookie, PLUS, message);
            //}
            if (!dependency.startsWith(SdkConstants.GRADLE_PLUGIN_NAME) ||
                !checkGradlePluginDependency(context, gc, valueCookie)) {
              checkDependency(context, gc, statementCookie);
            }
          }
        }
      }
    }
  }

  private void checkDependency(Context context, GradleCoordinate gradleCoordinate, Object cookie) {
    //checkLocalMavenVersions(context, gradleCoordinate, cookie, gradleCoordinate.getGroupId(), gradleCoordinate.getArtifactId(), );
    if ("me.ele.warlock".equals(gradleCoordinate.getGroupId())) {
      report(context, cookie, ISSUE, getNewerVersionAvailableMessage(gradleCoordinate, "1.10.10"));
    }
  }

  private boolean checkGradlePluginDependency(Context context, GradleCoordinate dependency,
      Object cookie) {
    GradleCoordinate latestPlugin = GradleCoordinate.parseCoordinateString(
        SdkConstants.GRADLE_PLUGIN_NAME +
            GRADLE_PLUGIN_MINIMUM_VERSION);
    return COMPARE_PLUS_HIGHER.compare(dependency, latestPlugin) < 0;
  }

  private static GradleCoordinate resolveCoordinate(@NonNull Context context,
      @NonNull GradleCoordinate gc) {
    assert gc.getRevision().contains("$") : gc.getRevision();
    Project project = context.getProject();
    Variant variant = project.getCurrentVariant();
    if (variant != null) {
      Dependencies dependencies = variant.getMainArtifact().getDependencies();
      for (AndroidLibrary library : dependencies.getLibraries()) {
        MavenCoordinates mc = library.getResolvedCoordinates();
        // Even though the method is annotated as non-null, this code can run
        // after a failed sync and there are observed scenarios where it returns
        // null in that ase
        //noinspection ConstantConditions
        if (mc != null
            && mc.getGroupId().equals(gc.getGroupId())
            && mc.getArtifactId().equals(gc.getArtifactId())) {
          List<GradleCoordinate.RevisionComponent> revisions =
              GradleCoordinate.parseRevisionNumber(mc.getVersion());
          if (!revisions.isEmpty()) {
            return new GradleCoordinate(mc.getGroupId(), mc.getArtifactId(),
                revisions, null);
          }
          break;
        }
      }
    }

    return null;
  }

  private static String getNamedDependency(@NonNull String expression) {
    //if (value.startsWith("group: 'com.android.support', name: 'support-v4', version: '21.0.+'"))
    if (expression.indexOf(',') != -1 && expression.contains("version:")) {
      String artifact = null;
      String group = null;
      String version = null;
      Splitter splitter = Splitter.on(',').omitEmptyStrings().trimResults();
      for (String property : splitter.split(expression)) {
        int colon = property.indexOf(':');
        if (colon == -1) {
          return null;
        }
        char quote = '\'';
        int valueStart = property.indexOf(quote, colon + 1);
        if (valueStart == -1) {
          quote = '"';
          valueStart = property.indexOf(quote, colon + 1);
        }
        if (valueStart == -1) {
          // For example, "transitive: false"
          continue;
        }
        valueStart++;
        int valueEnd = property.indexOf(quote, valueStart);
        if (valueEnd == -1) {
          return null;
        }
        String value = property.substring(valueStart, valueEnd);
        if (property.startsWith("group:")) {
          group = value;
        } else if (property.startsWith("name:")) {
          artifact = value;
        } else if (property.startsWith("version:")) {
          version = value;
        }
      }
      if (artifact != null && group != null && version != null) {
        return group + ':' + artifact + ':' + version;
      }
    }
    return null;
  }

  @Nullable
  private static String getStringLiteralValue(@NonNull String value) {
    if (value.length() > 2 && (value.startsWith("'") && value.endsWith("'") ||
        value.startsWith("\"") && value.endsWith("\""))) {
      return value.substring(1, value.length() - 1);
    }
    return null;
  }

  @NonNull
  private static Pair<Integer, Integer> getOffsets(ASTNode node, Context context) {
    if (node.getLastLineNumber() == -1 && node instanceof TupleExpression) {
      // Workaround: TupleExpressions yield bogus offsets, so use its
      // children instead
      TupleExpression exp = (TupleExpression) node;
      List<Expression> expressions = exp.getExpressions();
      if (!expressions.isEmpty()) {
        return Pair.of(
            getOffsets(expressions.get(0), context).getFirst(),
            getOffsets(expressions.get(expressions.size() - 1), context).getSecond());
      }
    }
    CharSequence source = context.getContents();
    assert source != null; // because we successfully parsed
    int start = 0;
    int end = source.length();
    int line = 1;
    int startLine = node.getLineNumber();
    int startColumn = node.getColumnNumber();
    int endLine = node.getLastLineNumber();
    int endColumn = node.getLastColumnNumber();
    int column = 1;
    for (int index = 0, len = end; index < len; index++) {
      if (line == startLine && column == startColumn) {
        start = index;
      }
      if (line == endLine && column == endColumn) {
        end = index;
        break;
      }

      char c = source.charAt(index);
      if (c == '\n') {
        line++;
        column = 1;
      } else {
        column++;
      }
    }

    return Pair.of(start, end);
  }

  @Override
  protected int getStartOffset(@NonNull Context context, @NonNull Object cookie) {
    ASTNode node = (ASTNode) cookie;
    Pair<Integer, Integer> offsets = getOffsets(node, context);
    return offsets.getFirst();
  }

  @Override
  protected Location createLocation(@NonNull Context context, @NonNull Object cookie) {
    ASTNode node = (ASTNode) cookie;
    Pair<Integer, Integer> offsets = getOffsets(node, context);
    int fromLine = node.getLineNumber() - 1;
    int fromColumn = node.getColumnNumber() - 1;
    int toLine = node.getLastLineNumber() - 1;
    int toColumn = node.getLastColumnNumber() - 1;
    return Location.create(context.file,
        new DefaultPosition(fromLine, fromColumn, offsets.getFirst()),
        new DefaultPosition(toLine, toColumn, offsets.getSecond()));
  }

  private void report(@NonNull Context context, @NonNull Object cookie, @NonNull Issue issue,
      @NonNull String message) {
    if (context.isEnabled(issue)) {
      // Suppressed?
      // Temporarily unconditionally checking for suppress comments in Gradle files
      // since Studio insists on an AndroidLint id prefix
      boolean checkComments = /*context.getClient().checkForSuppressComments()
                    &&*/ context.containsCommentSuppress();
      if (checkComments) {
        int startOffset = getStartOffset(context, cookie);
        if (startOffset >= 0 && context.isSuppressedWithComment(startOffset, issue)) {
          return;
        }
      }

      context.report(issue, createLocation(context, cookie), message);
    }
  }

  private static String getNewerVersionAvailableMessage(GradleCoordinate dependency,
      Revision version) {
    return getNewerVersionAvailableMessage(dependency, version.toString());
  }

  private static String getNewerVersionAvailableMessage(GradleCoordinate dependency,
      String version) {
    // NOTE: Keep this in sync with {@link #getOldValue} and {@link #getNewValue}
    return "A newer version of " + dependency.getGroupId() + ":" +
        dependency.getArtifactId() + " than " + dependency.getRevision() +
        " is available: " + version;
  }

  private void checkLocalMavenVersions(Context context, GradleCoordinate dependency,
      Object cookie, String groupId, String artifactId, File repository) {
    GradleCoordinate max = MavenRepositories.getHighestInstalledVersion(
        groupId, artifactId, repository, null, false, FileOpUtils.create());
    if (max != null) {
      if (COMPARE_PLUS_HIGHER.compare(dependency, max) < 0
          && context.isEnabled(DEPENDENCY)) {
        String message = getNewerVersionAvailableMessage(dependency, max.getRevision());
        report(context, cookie, DEPENDENCY, message);
      }
    }
  }
}
