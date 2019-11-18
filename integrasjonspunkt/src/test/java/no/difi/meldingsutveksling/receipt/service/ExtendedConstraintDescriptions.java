package no.difi.meldingsutveksling.receipt.service;

import org.springframework.restdocs.constraints.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ExtendedConstraintDescriptions {

    private final Class<?> clazz;
    private final ConstraintResolver constraintResolver;
    private final ConstraintDescriptionResolver descriptionResolver;

    /**
     * Create a new {@code ExtendedConstraintDescriptions} for the given {@code clazz}.
     * Constraints will be resolved using a {@link ValidatorConstraintResolver} and
     * descriptions will be resolved using a
     * {@link ResourceBundleConstraintDescriptionResolver}.
     *
     * @param clazz the class
     */
    ExtendedConstraintDescriptions(Class<?> clazz) {
        this(clazz, new ValidatorConstraintResolver(),
                new ResourceBundleConstraintDescriptionResolver());
    }

    /**
     * Create a new {@code ExtendedConstraintDescriptions} for the given {@code clazz}.
     * Constraints will be resolved using the given {@code constraintResolver} and
     * descriptions will be resolved using a
     * {@link ResourceBundleConstraintDescriptionResolver}.
     *
     * @param clazz              the class
     * @param constraintResolver the constraint resolver
     */
    public ExtendedConstraintDescriptions(Class<?> clazz, ConstraintResolver constraintResolver) {
        this(clazz, constraintResolver,
                new ResourceBundleConstraintDescriptionResolver());
    }

    /**
     * Create a new {@code ExtendedConstraintDescriptions} for the given {@code clazz}.
     * Constraints will be resolved using a {@link ValidatorConstraintResolver} and
     * descriptions will be resolved using the given {@code descriptionResolver}.
     *
     * @param clazz               the class
     * @param descriptionResolver the description resolver
     */
    public ExtendedConstraintDescriptions(Class<?> clazz,
                                          ConstraintDescriptionResolver descriptionResolver) {
        this(clazz, new ValidatorConstraintResolver(), descriptionResolver);
    }

    /**
     * Create a new {@code ExtendedConstraintDescriptions} for the given {@code clazz}.
     * Constraints will be resolved using the given {@code constraintResolver} and
     * descriptions will be resolved using the given {@code descriptionResolver}.
     *
     * @param clazz               the class
     * @param constraintResolver  the constraint resolver
     * @param descriptionResolver the description resolver
     */
    public ExtendedConstraintDescriptions(Class<?> clazz, ConstraintResolver constraintResolver,
                                          ConstraintDescriptionResolver descriptionResolver) {
        this.clazz = clazz;
        this.constraintResolver = constraintResolver;
        this.descriptionResolver = descriptionResolver;
    }

    /**
     * Returns a list of the descriptions for the constraints on the given property.
     *
     * @param property the property
     * @return the list of constraint descriptions
     */
    List<String> descriptionsForProperty(String property) {
        return this.constraintResolver
                .resolveForProperty(property, this.clazz)
                .stream()
                .map(descriptionResolver::resolveDescription)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Returns a list of the descriptions for the constraints on the given property and validation group.
     *
     * @param property the property
     * @param group    the validation group
     * @return the list of constraint descriptions
     */
    List<String> descriptionsForProperty(String property, Class<?> group) {
        return this.constraintResolver
                .resolveForProperty(property, this.clazz)
                .stream()
                .filter(p -> hasGroup(p, group))
                .map(descriptionResolver::resolveDescription)
                .sorted()
                .collect(Collectors.toList());
    }

    private boolean hasGroup(Constraint constraint, Class<?> group) {
        Class<?>[] groups = (Class<?>[]) constraint.getConfiguration().get("groups");
        return groups == null || groups.length == 0 || Arrays.asList(groups).contains(group);
    }
}
