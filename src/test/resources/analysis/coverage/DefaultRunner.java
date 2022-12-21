import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import utils.Call;
import utils.TestException;
import utils.TestRuntimeException;

public class DefaultRunner {

    public static void main(String[] args) throws Exception {
        // get the class
        Class<?> clazz = Class.forName(args[0]);

        // instantiate the class
        Constructor<?> constructor = clazz.getConstructor();
        Object instance = constructor.newInstance();

        for (Method method : clazz.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(Call.class)) {
                continue;
            }
            method.setAccessible(true);

            // parse parameters
            Object[][] paramSets = getParameters(method);

            // call the method for each set of parameters
            for (Object[] paramSet : paramSets) {
                invokeMethod(method, instance, paramSet);
            }
        }
    }

    public static void invokeMethod(Method method, Object instance, Object[] params) {
        String paramsString = Arrays.stream(params)
                .map(String::valueOf)
                .collect(Collectors.joining(", "));

        String summaryString = String.format("@Call %s(%s)", method.getName(), paramsString);
        Throwable exception = null;

        try {
            method.invoke(instance, params);

        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            summaryString += String.format(" threw %s", cause.getClass().getSimpleName());
            if (!declaresException(method, cause)) {
                exception = cause;
            }

        } catch (Throwable e) {
            exception = e;

        } finally {
            System.out.println(summaryString);
            if (exception != null) {
                exception.printStackTrace();
            }
        }
    }

    public static boolean declaresException(Method method, Throwable throwable) {
        Call annotation = method.getAnnotation(Call.class);
        Class<? extends Throwable> throwableClass = annotation.exception();
        return throwableClass.isInstance(throwable);
    }

    public static Object[][] getParameters(Method method) throws Exception {
        Call annotation = method.getAnnotation(Call.class);
        String[] strings = annotation.params();
        if (strings.length == 0) {
            return new Object[][] {new Object[0]};
        }
        return parseParameterSets(strings, method.getParameterTypes());
    }

    public static Object[][] parseParameterSets(String[] strings, Class<?>[] types) throws Exception {
        Object[][] params = new Object[strings.length][types.length];
        for (int i = 0; i < strings.length; i++) {
            params[i] = parseParameterSet(strings[i], types);
        }
        return params;
    }

    public static Object[] parseParameterSet(String string, Class<?>[] types) throws Exception {
        Object[] params = new Object[types.length];
        String[] split = string.trim().split("\\s*,\\s*");
        for (int i = 0; i < types.length; i++) {
            params[i] = parseParameter(split[i], types[i]);
        }
        return params;
    }

    public static Object parseParameter(String string, Class<?> type) throws Exception {
        if ("null".equals(string)) {
            return null;
        } else if (String.class.isAssignableFrom(type)) {
            return string;
        } else if (int.class.isAssignableFrom(type)) {
            return Integer.parseInt(string);
        } else if (Integer.class.isAssignableFrom(type)) {
            return (Integer) Integer.parseInt(string);
        } else if (boolean.class.isAssignableFrom(type)) {
            return Boolean.parseBoolean(string);
        } else if (Enum.class.isAssignableFrom(type)) {
            Method valueOf = type.getMethod("valueOf", String.class);
            return valueOf.invoke(null, string);
        }
        return null;
    }
}
