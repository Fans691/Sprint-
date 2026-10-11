package mg.itu.framework.controller;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

import mg.itu.framework.annotation.Binding;

public class MethodeController {

    public Object[] getMethodParameters(HttpServletRequest request, Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] parameters = new Object[parameterTypes.length];
        List<String> parameterNames = getParameterNames(request);
        int scalarParameterIndex = 0;

        for (int i = 0; i < parameterTypes.length; i++) {
            Binding binding = method.getParameters()[i].getAnnotation(Binding.class);
            if (binding != null) {
                parameters[i] = bindObject(request, parameterTypes[i], binding.value());
            } else {
                if (scalarParameterIndex >= parameterNames.size()) {
                    throw new IllegalArgumentException("Parametre manquant pour le type "
                            + parameterTypes[i].getName());
                }
                String value = request.getParameter(parameterNames.get(scalarParameterIndex));
                parameters[i] = convertParameter(value, parameterTypes[i]);
                scalarParameterIndex++;
            }
        }

        return parameters;
    }

    private List<String> getParameterNames(HttpServletRequest request) {
        List<String> names = new ArrayList<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            names.add(parameterNames.nextElement());
        }
        return names;
    }

    private Object bindObject(HttpServletRequest request, Class<?> targetType, String prefix) {
        if (isSimpleType(targetType)) {
            return convertParameter(request.getParameter(prefix), targetType);
        }
        return bindObject(request, targetType, prefix, new HashSet<Class<?>>());
    }

    private Object bindObject(HttpServletRequest request, Class<?> targetType, String prefix,
            Set<Class<?>> objectStack) {
        if (!objectStack.add(targetType)) {
            throw new IllegalArgumentException("Cycle detecte pendant le binding du type "
                    + targetType.getName());
        }

        try {
            Object instance = targetType.getDeclaredConstructor().newInstance();
            for (Field field : getBindableFields(targetType)) {
                String fieldPrefix = joinPath(prefix, field.getName());

                if (List.class.isAssignableFrom(field.getType())) {
                    field.set(instance, bindList(request, field, fieldPrefix));
                } else if (isSimpleType(field.getType())) {
                    field.set(instance, convertParameter(
                            request.getParameter(fieldPrefix), field.getType()));
                } else {
                    field.set(instance, bindObject(
                            request, field.getType(), fieldPrefix, objectStack));
                }
            }
            return instance;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("Impossible de construire le type lie : "
                    + targetType.getName()
                    + ". Un constructeur sans argument est requis.", exception);
        } finally {
            objectStack.remove(targetType);
        }
    }

    private List<?> bindList(HttpServletRequest request, Field field, String fieldPrefix) {
        Class<?> elementType = getListElementType(field);
        List<Object> values = new ArrayList<>();

        for (Integer index : getIndexes(request, fieldPrefix)) {
            String elementPrefix = fieldPrefix + "[" + index + "]";
            if (isSimpleType(elementType)) {
                values.add(convertParameter(request.getParameter(elementPrefix), elementType));
            } else {
                values.add(bindObject(
                        request, elementType, elementPrefix, new HashSet<Class<?>>()));
            }
        }

        return values;
    }

    private List<Integer> getIndexes(HttpServletRequest request, String fieldPrefix) {
        List<Integer> indexes = new ArrayList<>();
        String indexPrefix = fieldPrefix + "[";

        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String parameterName = parameterNames.nextElement();
            if (!parameterName.startsWith(indexPrefix)) {
                continue;
            }

            int closingBracket = parameterName.indexOf("]", indexPrefix.length());
            if (closingBracket == -1) {
                continue;
            }

            String indexText = parameterName.substring(indexPrefix.length(), closingBracket);
            try {
                int index = Integer.parseInt(indexText);
                if (!indexes.contains(index)) {
                    indexes.add(index);
                }
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Indice de liste invalide : " + indexText, exception);
            }
        }

        Collections.sort(indexes);
        return indexes;
    }

    private Class<?> getListElementType(Field field) {
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType)) {
            throw new IllegalArgumentException("La liste " + field.getName()
                    + " doit declarer son type generique, par exemple List<Personne>.");
        }

        Type elementType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
        if (!(elementType instanceof Class<?>)) {
            throw new IllegalArgumentException(
                    "Type d'element non supporte pour la liste " + field.getName());
        }
        return (Class<?>) elementType;
    }

    private List<Field> getBindableFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (!Modifier.isStatic(modifiers) && !field.isSynthetic()) {
                    field.setAccessible(true);
                    fields.add(field);
                }
            }
        }
        return fields;
    }

    private String joinPath(String prefix, String name) {
        return prefix == null || prefix.isEmpty() ? name : prefix + "." + name;
    }

    private boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || type == String.class
                || type == Integer.class
                || type == Long.class
                || type == Double.class
                || type == Float.class
                || type == Boolean.class
                || type == Short.class
                || type == Byte.class
                || type == Character.class
                || type.isEnum();
    }

    @SuppressWarnings("unchecked")
    private Object convertParameter(String value, Class<?> targetType) {
        if (value == null) {
            return targetType.isPrimitive() ? primitiveDefault(targetType) : null;
        }

        if (targetType == String.class) return value;
        if (targetType == int.class || targetType == Integer.class) return Integer.parseInt(value);
        if (targetType == long.class || targetType == Long.class) return Long.parseLong(value);
        if (targetType == double.class || targetType == Double.class) return Double.parseDouble(value);
        if (targetType == float.class || targetType == Float.class) return Float.parseFloat(value);
        if (targetType == boolean.class || targetType == Boolean.class) return Boolean.parseBoolean(value);
        if (targetType == short.class || targetType == Short.class) return Short.parseShort(value);
        if (targetType == byte.class || targetType == Byte.class) return Byte.parseByte(value);
        if (targetType == char.class || targetType == Character.class) return value.charAt(0);
        if (targetType.isEnum()) {
            return Enum.valueOf((Class<? extends Enum>) targetType.asSubclass(Enum.class), value);
        }

        throw new IllegalArgumentException("Type non supporte : " + targetType.getName());
    }

    private Object primitiveDefault(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return null;
    }
}