package com.vyra.be_virtual_your_assets.handler;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import jakarta.validation.Constraint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/** Every message the app can receive has Indonesian text, so none shows up in English by accident. */
class ErrorConstantTranslationTest {

    /** Admin portal requests: the portal always asks for English. */
    private static final Set<String> ADMIN_PACKAGES = Set.of("admin", "adminmember", "audit");

    /** DTOs used inside another request: their errors are reported under the parent's field. */
    private static final Map<String, String> NESTED = Map.of(
            "DashboardCardSetting", "updatePreferencesRequest.dashboardLayout",
            "NotificationPreferenceItem", "updateNotificationPreferencesRequest.items");

    @Test
    void everyErrorConstant_hasIndonesianText() {
        List<String> missing = Arrays.stream(ErrorConstant.values())
                .filter(c -> c.getMessageId() == null || c.getMessageId().isBlank()
                        || c.getMessageId().equals(c.getMessage()))
                .map(Enum::name)
                .toList();
        assertThat(missing).as("ErrorConstant entries without Indonesian text (third argument)").isEmpty();
    }

    @Test
    void everyRequestValidationMessage_hasIndonesianText() throws Exception {
        Properties indonesian = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/messages_id.properties")) {
            indonesian.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));
        List<String> missing = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents("com.vyra.be_virtual_your_assets.dto")) {
            Class<?> type = Class.forName(definition.getBeanClassName());
            String pkg = type.getPackageName().substring(type.getPackageName().lastIndexOf('.') + 1);
            if (ADMIN_PACKAGES.contains(pkg)) continue;
            String simple = type.getSimpleName();
            String path = NESTED.getOrDefault(simple, Character.toLowerCase(simple.charAt(0)) + simple.substring(1));
            for (Field field : type.getDeclaredFields()) {
                collect(field, path + "." + field.getName(), indonesian, missing);
            }
            for (Method method : type.getDeclaredMethods()) {
                String name = method.getName();
                if (name.startsWith("is") && name.length() > 2) {
                    collect(method, path + "." + Character.toLowerCase(name.charAt(2)) + name.substring(3), indonesian, missing);
                }
            }
        }
        assertThat(missing).as("validation keys missing from messages_id.properties").isEmpty();
    }

    private static void collect(AnnotatedElement element, String path, Properties indonesian, List<String> missing) {
        for (Annotation annotation : element.getAnnotations()) {
            if (!annotation.annotationType().isAnnotationPresent(Constraint.class)) continue;
            if (!hasOwnMessage(annotation)) continue; // Hibernate Validator's default text
            String key = annotation.annotationType().getSimpleName() + "." + path;
            if (!indonesian.containsKey(key)) missing.add(key);
        }
    }

    private static boolean hasOwnMessage(Annotation annotation) {
        try {
            Object message = annotation.annotationType().getMethod("message").invoke(annotation);
            return message instanceof String text && !text.startsWith("{");
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}
