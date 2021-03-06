package com.cedricziel.idea.typo3.persistence.codeInsight;

import com.cedricziel.idea.typo3.psi.PhpElementsUtil;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Matches
 * <p>
 * \TYPO3\CMS\Core\Database::getConnectionForTable()
 * \TYPO3\CMS\Core\Database::getQueryBuilderForTable()
 * <p>
 * and provides autocompletion
 */
public class DoctrineTablesContributor extends CompletionContributor {

    private static final String EXT_TABLES_SQL_FILENAME = "ext_tables.sql";

    public DoctrineTablesContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
                PsiElement element = completionParameters.getPosition().getParent();
                ParameterList parameterList = PsiTreeUtil.getParentOfType(element, ParameterList.class);
                if (parameterList == null) {
                    return;
                }

                MethodReference methodReference = PsiTreeUtil.getParentOfType(element, MethodReference.class);
                if (methodReference == null) {
                    return;
                }

                if (PhpElementsUtil.isMethodWithFirstStringOrFieldReference(methodReference, "getConnectionForTable")) {
                    completeAvailableTables(methodReference, completionResultSet);

                    return;
                }

                if (PhpElementsUtil.isMethodWithFirstStringOrFieldReference(methodReference, "getQueryBuilderForTable")) {
                    completeAvailableTables(methodReference, completionResultSet);

                    return;
                }

                String methodName = methodReference.getName();
                ClassReference classReference = PsiTreeUtil.getChildOfType(methodReference, ClassReference.class);
                if (classReference != null && methodName != null) {
                    String name = classReference.getFQN();

                    // there can be multiple classes in one project scope that share the same FQN
                    Collection<PhpClass> phpClasses = PhpIndex.getInstance(element.getProject()).getClassesByFQN(name);
                    for (PhpClass c : phpClasses) {
                        Method method = c.findMethodByName(methodName);

                        ParameterList originalMethodParameterList = PsiTreeUtil.getChildOfType(method, ParameterList.class);
                        if (originalMethodParameterList != null) {
                            Parameter firstParameter = PsiTreeUtil.getChildOfType(originalMethodParameterList, Parameter.class);

                            // completion on method with no arguments
                            if (firstParameter == null) {
                                return;
                            }

                            String parameterName = firstParameter.getName();
                            if (parameterName.equals("table") || parameterName.equals("tableName")) {
                                completeAvailableTables(methodReference, completionResultSet);

                                return;
                            }
                        }
                    }
                }
            }
        });
    }

    private void completeAvailableTables(MethodReference element, @NotNull CompletionResultSet completionResultSet) {
        PsiFile[] extSqlFiles = FilenameIndex.getFilesByName(element.getProject(), EXT_TABLES_SQL_FILENAME, GlobalSearchScope.allScope(element.getProject()));

        Set<String> tableNames = new HashSet<>();

        for (PsiFile psiFile : extSqlFiles) {

            if (psiFile != null) {

                CharSequence charSequence = LoadTextUtil.loadText(psiFile.getVirtualFile());

                final Matcher matcher = Pattern
                        .compile("create\\s+table\\s+(if\\s+not\\s+exists\\s+)?([a-zA-Z_0-9]+)", Pattern.CASE_INSENSITIVE)
                        .matcher(charSequence);

                try {
                    while (matcher.find()) {
                        if (matcher.groupCount() < 2) {
                            return;
                        }

                        tableNames.add(matcher.group(2));
                    }
                } catch (IllegalStateException e) {
                    // no matches
                }
            }
        }

        for (String name : tableNames) {
            completionResultSet.addElement(new LookupElement() {
                @NotNull
                @Override
                public String getLookupString() {

                    return name;
                }
            });
        }
    }
}
