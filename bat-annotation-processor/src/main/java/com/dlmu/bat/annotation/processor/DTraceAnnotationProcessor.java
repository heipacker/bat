package com.dlmu.bat.annotation.processor;

import com.dlmu.bat.annotation.DF;
import com.dlmu.bat.annotation.DP;
import com.dlmu.bat.annotation.DTrace;
import com.dlmu.bat.common.Constants;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes("com.dlmu.bat.annotation.DTrace")
public class DTraceAnnotationProcessor extends AbstractProcessor {

    private static final char STATIC_FIELD_DESC = 'S';

    private static final char INSTANCE_FIELD_DESC = 'I';

    private static final String ARGS_PREFIX = "args=";
    private static final String FIELDS_PREFIX = "fields=";
    private static final String DESC_PREFIX = "desc=";
    private static final String TYPE_PREFIX = "type=";

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Writer writer = null;
        try {
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(DTrace.class);
            if (elements == null || elements.size() == 0) return false;

            FileObject resource = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", Constants.DTRACER_CONFIG_FILE);
            writer = resource.openWriter();

            for (Element element : elements) {
                if (element.getKind() == ElementKind.METHOD) {
                    ExecutableElement executableElement = (ExecutableElement) element;
                    writer.write(generateTraceConfig(executableElement));
                    writer.write("\n");
                }
            }
        } catch (IOException e) {
            return false;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignore) {

                }
            }
        }
        return false;
    }

    /**
     * 类似这种格式：className:methodName(parameterType name):[desc=desc1:][type=type1][:args=arg1,arg2][:fields=filedName(fieldType)S|I]
     * com.mysql.jdbc.StatementImpl:executeQuery(java.lang.String originalSql):type=SQL:fields=currentCatalog(java.lang.String)I:args=originalSql
     *
     * @param element
     * @return
     */
    private String generateTraceConfig(ExecutableElement element) {
        DTrace annotation = element.getAnnotation(DTrace.class);
        if (annotation == null) return null;
        Element enclosingElement = element.getEnclosingElement();
        if (enclosingElement.getKind() != ElementKind.CLASS) return null;

        StringBuilder result = new StringBuilder();
        result.append(((TypeElement) enclosingElement).getQualifiedName().toString());
        result.append(':');
        result.append(element.getSimpleName().toString());
        result.append('(');
        List<String> traceArgs = new ArrayList<String>();
        List<String> allArgNames = new ArrayList<String>();
        List<String> allArgs = new ArrayList<String>();
        List<? extends VariableElement> parameters = element.getParameters();
        for (VariableElement parameterElement : parameters) {
            DP qp = parameterElement.getAnnotation(DP.class);
            TypeMirror type = parameterElement.asType();
            String parameterName = parameterElement.getSimpleName().toString();
            allArgNames.add(parameterName);
            allArgs.add(type.toString() + " " + parameterName);
            if (qp != null) {
                traceArgs.add(parameterName);
            }
        }
        result.append(join(allArgs));
        result.append("):");
        if (isNotEmpty(annotation.value())) {
            result.append(DESC_PREFIX).append(annotation.value()).append(':');
        }
        if (isNotEmpty(annotation.type())) {
            result.append(TYPE_PREFIX).append(annotation.type()).append(':');
        }

        //如果一个QP都没标，则trace所有参数
        if (traceArgs.size() > 0) {
            result.append(ARGS_PREFIX).append(join(traceArgs)).append(":");
        } else if (allArgNames.size() > 0) {
            result.append(ARGS_PREFIX).append(join(allArgNames)).append(":");
        }
        List<Element> fields = extractFields((TypeElement) enclosingElement);
        List<String> fieldDesc = appendFields(fields);
        if (fieldDesc.size() > 0) {
            result.append(FIELDS_PREFIX).append(join(fieldDesc)).append(":");
        }
        return result.toString();
    }

    private List<String> appendFields(List<Element> fields) {
        if (fields.size() == 0) return Collections.emptyList();
        List<String> result = new ArrayList<String>();
        for (Element field : fields) {
            result.add(fieldOf(field));
        }
        return result;
    }

    private String fieldOf(Element element) {
        VariableElement field = (VariableElement) element;
        StringBuilder result = new StringBuilder();
        result.append(field.getSimpleName().toString())
                .append('(')
                .append(field.asType().toString())
                .append(')');
        Set<Modifier> modifiers = field.getModifiers();
        if (modifiers.contains(Modifier.STATIC)) {
            result.append(STATIC_FIELD_DESC);
        } else {
            result.append(INSTANCE_FIELD_DESC);
        }

        return result.toString();
    }

    /**
     * 提取QF注解的字段
     * @param typeElement
     * @return
     */
    private List<Element> extractFields(TypeElement typeElement) {
        List<? extends Element> members = typeElement.getEnclosedElements();
        List<Element> result = new ArrayList<Element>();
        for (Element member : members) {
            if (member.getKind() == ElementKind.FIELD) {
                DF qf = member.getAnnotation(DF.class);
                if (qf != null) {
                    result.add(member);
                }
            }
        }
        return result;
    }

    private String join(List<String> list) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < list.size(); ++i) {
            result.append(list.get(i));
            if (i < list.size() - 1) {
                result.append(',');
            }
        }
        return result.toString();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    private boolean isNotEmpty(String value) {
        return !(value == null || value.length() == 0);
    }
}
