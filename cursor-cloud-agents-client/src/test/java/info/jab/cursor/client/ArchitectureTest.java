package info.jab.cursor.client;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

/**
 * Architectural tests to verify that interface return types use types from the openapi module.
 *
 * <p>This test ensures that:
 * <ul>
 *   <li>Methods in CursorAgentGeneralEndpoints, CursorAgentInformation, and CursorAgentManagement
 *       return types that either are standard Java types or use types from the openapi module</li>
 *   <li>Model classes in the client.model package use types from the openapi module</li>
 * </ul>
 */
@AnalyzeClasses(
    packages = {"info.jab.cursor.client", "info.jab.cursor.generated.client.model"},
    importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {

    private static final String OPENAPI_MODEL_PACKAGE = "info.jab.cursor.generated.client.model";
    private static final String CLIENT_MODEL_PACKAGE = "info.jab.cursor.client.model";

    @ArchTest
    static final ArchRule interfacesReturnTypesShouldUseOpenApiTypes = methods()
        .that()
        .areDeclaredIn(CursorAgentGeneralEndpoints.class)
        .or()
        .areDeclaredIn(CursorAgentInformation.class)
        .or()
        .areDeclaredIn(CursorAgentManagement.class)
        .should(new ArchCondition<JavaMethod>("return types should use openapi module types") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                JavaClass returnType = method.getRawReturnType();

                // Allow standard Java types
                if (isStandardJavaType(returnType)) {
                    return;
                }

                // Check if return type is from client.model package
                if (returnType.getPackageName().equals(CLIENT_MODEL_PACKAGE)) {
                    // Verify that the return type class uses types from openapi module
                    Set<Dependency> dependencies = returnType.getDirectDependenciesFromSelf();
                    boolean usesOpenApiTypes = dependencies.stream()
                        .map(Dependency::getTargetClass)
                        .anyMatch(dep -> dep.getPackageName().startsWith(OPENAPI_MODEL_PACKAGE));

                    if (!usesOpenApiTypes) {
                        String message = String.format(
                            "Method %s.%s() returns %s which should use types from %s",
                            method.getOwner().getName(),
                            method.getName(),
                            returnType.getName(),
                            OPENAPI_MODEL_PACKAGE
                        );
                        events.add(SimpleConditionEvent.violated(method, message));
                    }
                } else if (!returnType.getPackageName().startsWith(OPENAPI_MODEL_PACKAGE)) {
                    // If return type is not from openapi module and not a standard type, it's a violation
                    String message = String.format(
                        "Method %s.%s() returns %s which should be from %s or use types from %s",
                        method.getOwner().getName(),
                        method.getName(),
                        returnType.getName(),
                        CLIENT_MODEL_PACKAGE,
                        OPENAPI_MODEL_PACKAGE
                    );
                    events.add(SimpleConditionEvent.violated(method, message));
                }
            }

            private boolean isStandardJavaType(JavaClass javaClass) {
                String packageName = javaClass.getPackageName();
                return packageName.startsWith("java.")
                    || packageName.startsWith("javax.")
                    || packageName.equals("java.lang")
                    || javaClass.isPrimitive()
                    || javaClass.getName().equals("void");
            }
        });

    @ArchTest
    static final ArchRule clientModelClassesShouldUseOpenApiTypes = classes()
        .that()
        .resideInAPackage(CLIENT_MODEL_PACKAGE)
        .should(new ArchCondition<JavaClass>("use types from openapi module") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                Set<Dependency> dependencies = javaClass.getDirectDependenciesFromSelf();
                boolean usesOpenApiTypes = dependencies.stream()
                    .map(Dependency::getTargetClass)
                    .anyMatch(dep -> dep.getPackageName().startsWith(OPENAPI_MODEL_PACKAGE));

                if (!usesOpenApiTypes) {
                    String message = String.format(
                        "Class %s should use types from %s",
                        javaClass.getName(),
                        OPENAPI_MODEL_PACKAGE
                    );
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }
        });
}

