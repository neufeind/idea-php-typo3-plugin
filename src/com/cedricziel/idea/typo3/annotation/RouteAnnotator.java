package com.cedricziel.idea.typo3.annotation;

import com.cedricziel.idea.typo3.container.RouteProvider;
import com.cedricziel.idea.typo3.psi.PhpElementsUtil;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;

public class RouteAnnotator implements Annotator {
    @Override
    public void annotate(@NotNull PsiElement psiElement, @NotNull AnnotationHolder annotationHolder) {

        if (!(psiElement instanceof StringLiteralExpression)) {
            return;
        }

        StringLiteralExpression literalExpression = (StringLiteralExpression) psiElement;
        String value = literalExpression.getContents();

        if (value.isEmpty()) {
            return;
        }

        PsiElement methodReference = PsiTreeUtil.getParentOfType(psiElement, MethodReference.class);
        if (PhpElementsUtil.isMethodWithFirstStringOrFieldReference(methodReference, "getAjaxUrl") && !hasArrayCreationParent(psiElement)) {
            annotateAjaxRoutes(psiElement, annotationHolder, value);
        }
        if (PhpElementsUtil.isMethodWithFirstStringOrFieldReference(methodReference, "buildUriFromRoute") && !hasArrayCreationParent(psiElement)) {
            annotateRoutes(psiElement, annotationHolder, value);
        }
    }

    private boolean hasArrayCreationParent(@NotNull PsiElement psiElement) {
        if (psiElement.getParent() == null) {
            return false;
        }
        return psiElement.getParent().getParent() instanceof ArrayCreationExpression;
    }

    private void annotateAjaxRoutes(PsiElement psiElement, AnnotationHolder annotationHolder, String value) {
        RouteProvider routeProvider = new RouteProvider();
        routeProvider.collect(psiElement.getProject());

        annotateRoute(psiElement, annotationHolder, value, routeProvider, RouteProvider.ROUTE_TYPE_AJAX);
    }

    private void annotateRoutes(PsiElement psiElement, AnnotationHolder annotationHolder, String value) {
        RouteProvider routeProvider = new RouteProvider();
        routeProvider.collect(psiElement.getProject());

        annotateRoute(psiElement, annotationHolder, value, routeProvider, RouteProvider.ROUTE_TYPE_BACKEND);
    }


    private void annotateRoute(PsiElement psiElement, AnnotationHolder annotationHolder, String value, RouteProvider routeProvider, String routeType) {
        if (routeProvider.has(value, routeType)) {
            TextRange range = new TextRange(psiElement.getTextRange().getStartOffset(), psiElement.getTextRange().getEndOffset());
            Annotation annotation = annotationHolder.createInfoAnnotation(range, null);
            annotation.setTextAttributes(DefaultLanguageHighlighterColors.STRING);
        } else {
            TextRange range = new TextRange(psiElement.getTextRange().getStartOffset(), psiElement.getTextRange().getEndOffset());
            annotationHolder.createErrorAnnotation(range, "Unresolved route");
        }
    }
}
