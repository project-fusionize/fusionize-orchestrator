package dev.fusionize.worker.component;

import dev.fusionize.worker.component.annotations.EnableRuntimeComponents;
import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class RuntimeComponentBeanRegistrar implements ImportBeanDefinitionRegistrar {
    private BeanFactory beanFactory;
    private static final Logger logger = LoggerFactory.getLogger(RuntimeComponentBeanRegistrar.class);


    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(
                metadata.getAnnotationAttributes(EnableRuntimeComponents.class.getName())
        );

        if (attributes == null) {
            return;
        }
        String[] packageNames = attributes.getStringArray("basePackages");
        List<String> basePackages = new ArrayList<>(Arrays.asList(packageNames));

        // Get base packages from basePackageClasses
        Class<?>[] basePackageClasses = attributes.getClassArray("basePackageClasses");
        for (Class<?> clazz : basePackageClasses) {
            basePackages.add(ClassUtils.getPackageName(clazz));
        }

        // If no packages specified, use the package of the configuration class
        if (basePackages.isEmpty()) {
            basePackages.add(ClassUtils.getPackageName(metadata.getClassName()));
        }

        // Scan for @RuntimeComponent annotations
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RuntimeComponentDefinition.class));

        for (String basePackage : basePackages) {
            scanner.findCandidateComponents(basePackage).forEach(beanDefinition -> {
                String beanName;
                try {

                    // Extract custom bean name from @RuntimeComponent value if present
                    Class<?> beanClass = Class.forName(beanDefinition.getBeanClassName());
                    RuntimeComponentDefinition componentDefinition = beanClass.getAnnotation(RuntimeComponentDefinition.class);
                    if (componentDefinition != null && StringUtils.hasText(componentDefinition.value())) {
                        beanName = componentDefinition.value();
                    } else {
                        // Generate default bean name
                        beanName = generateBeanName(beanClass);
                    }

                } catch (ClassNotFoundException e) {
                    logger.error("Registration Error for RuntimeComponentFactory: {} -> {}", beanDefinition.getBeanClassName(),
                            "Cannot load class: " + beanDefinition.getBeanClassName());
                    return;
                }


                if (!registry.containsBeanDefinition(beanName)) {
                    logger.info("Register bean: {}", beanName);
                    registry.registerBeanDefinition(beanName, beanDefinition);
                }
            });
        }
    }

    private String generateBeanName(Class<?> clazz) {
        String shortName = ClassUtils.getShortName(clazz);
        return Character.toLowerCase(shortName.charAt(0)) + shortName.substring(1);
    }

}
