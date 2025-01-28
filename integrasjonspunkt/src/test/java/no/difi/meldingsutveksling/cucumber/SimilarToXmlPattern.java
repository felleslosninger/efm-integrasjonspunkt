/*
 * Copyright (C) 2011 Thomas Akehurst
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package no.difi.meldingsutveksling.cucumber;

import wiremock.com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tomakehurst.wiremock.common.xml.Xml;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xmlunit.XMLUnitException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.*;
import org.xmlunit.placeholder.PlaceholderDifferenceEvaluator;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.github.tomakehurst.wiremock.common.LocalNotifier.notifier;
import static org.xmlunit.diff.ComparisonType.*;

public class SimilarToXmlPattern extends StringValuePattern {

    private static final List<ComparisonType> COUNTED_COMPARISONS = Collections.unmodifiableList(Arrays.asList(
            ELEMENT_TAG_NAME,
            SCHEMA_LOCATION,
            NO_NAMESPACE_SCHEMA_LOCATION,
            NODE_TYPE,
            TEXT_VALUE,
            PROCESSING_INSTRUCTION_TARGET,
            PROCESSING_INSTRUCTION_DATA,
            ELEMENT_NUM_ATTRIBUTES,
            ATTR_VALUE,
            CHILD_NODELIST_LENGTH,
            CHILD_LOOKUP,
            ATTR_NAME_LOOKUP
    ));

    private final Document xmlDocument;
    private final Boolean enablePlaceholders;
    private final String placeholderOpeningDelimiterRegex;
    private final String placeholderClosingDelimiterRegex;
    private final DifferenceEvaluator diffEvaluator;

    SimilarToXmlPattern(@JsonProperty("equalToXml") String expectedValue) {
        this(expectedValue, null, null, null);
    }

    private SimilarToXmlPattern(@JsonProperty("equalToXml") String expectedValue,
                                @JsonProperty("enablePlaceholders") Boolean enablePlaceholders,
                                @JsonProperty("placeholderOpeningDelimiterRegex") String placeholderOpeningDelimiterRegex,
                                @JsonProperty("placeholderClosingDelimiterRegex") String placeholderClosingDelimiterRegex) {
        super(expectedValue);
        xmlDocument = Xml.read(expectedValue);
        this.enablePlaceholders = enablePlaceholders;
        this.placeholderOpeningDelimiterRegex = placeholderOpeningDelimiterRegex;
        this.placeholderClosingDelimiterRegex = placeholderClosingDelimiterRegex;
        if (enablePlaceholders != null && enablePlaceholders) {
            diffEvaluator = DifferenceEvaluators.chain(IGNORE_UNCOUNTED_COMPARISONS,
                    new PlaceholderDifferenceEvaluator(placeholderOpeningDelimiterRegex, placeholderClosingDelimiterRegex));
        } else {
            diffEvaluator = IGNORE_UNCOUNTED_COMPARISONS;
        }
    }

    public String getEqualToXml() {
        return expectedValue;
    }

    @Override
    public String getExpected() {
        return Xml.prettyPrint(getValue());
    }

    public Boolean isEnablePlaceholders() {
        return enablePlaceholders;
    }

    public String getPlaceholderOpeningDelimiterRegex() {
        return placeholderOpeningDelimiterRegex;
    }

    public String getPlaceholderClosingDelimiterRegex() {
        return placeholderClosingDelimiterRegex;
    }

    @Override
    public MatchResult match(final String value) {
        return new MatchResult() {
            @Override
            public boolean isExactMatch() {
                if (!StringUtils.hasLength(value)) {
                    return false;
                }
                try {
                    Diff diff = DiffBuilder.compare(Input.from(expectedValue))
                            .withTest(value)
                            .withComparisonController(ComparisonControllers.StopWhenDifferent)
                            .ignoreWhitespace()
                            .ignoreComments()
                            .withDifferenceEvaluator(diffEvaluator)
                            .withNodeMatcher(new OrderInvariantNodeMatcher())
                            .withDocumentBuilderFactory(Xml.newDocumentBuilderFactory())
                            .checkForSimilar()
                            .build();

                    return !diff.hasDifferences();
                } catch (XMLUnitException e) {
                    notifier().info("Failed to process XML. " + e.getMessage() +
                            "\nExpected:\n" + expectedValue +
                            "\n\nActual:\n" + value);
                    return false;
                }
            }

            @Override
            public double getDistance() {
                if (!StringUtils.hasLength(value)) {
                    return 1.0;
                }

                final AtomicInteger totalComparisons = new AtomicInteger(0);
                final AtomicInteger differences = new AtomicInteger(0);

                Diff diff;
                try {
                    diff = DiffBuilder.compare(Input.from(expectedValue))
                            .withTest(value)
                            .ignoreWhitespace()
                            .ignoreComments()
                            .withDifferenceEvaluator(diffEvaluator)
                            .withComparisonListeners((comparison, outcome) -> {
                                if (COUNTED_COMPARISONS.contains(comparison.getType()) && comparison.getControlDetails().getValue() != null) {
                                    totalComparisons.incrementAndGet();
                                    if (outcome == ComparisonResult.DIFFERENT) {
                                        differences.incrementAndGet();
                                    }
                                }
                            })
                            .withDocumentBuilderFactory(Xml.newDocumentBuilderFactory())
                            .checkForSimilar()
                            .build();
                } catch (XMLUnitException e) {
                    notifier().info("Failed to process XML. " + e.getMessage() +
                            "\nExpected:\n" + expectedValue +
                            "\n\nActual:\n" + value);
                    return 1.0;
                }

                notifier().info(
                        StreamSupport.stream(diff.getDifferences().spliterator(), false)
                                .map(Difference::toString)
                                .collect(Collectors.joining("\n"))
                );

                return differences.doubleValue() / totalComparisons.doubleValue();
            }
        };
    }

    private static final DifferenceEvaluator IGNORE_UNCOUNTED_COMPARISONS = (comparison, outcome) -> {
        if (COUNTED_COMPARISONS.contains(comparison.getType()) && comparison.getControlDetails().getValue() != null) {
            return outcome;
        }

        return ComparisonResult.EQUAL;
    };


    private static final class OrderInvariantNodeMatcher extends DefaultNodeMatcher {
        @Override
        public Iterable<Map.Entry<Node, Node>> match(Iterable<Node> controlNodes, Iterable<Node> testNodes) {

            return super.match(
                    sort(controlNodes),
                    sort(testNodes)
            );
        }

        private static Iterable<Node> sort(Iterable<Node> nodes) {
            return StreamSupport.stream(nodes.spliterator(), false).sorted(COMPARATOR).collect(Collectors.toList());
        }

        private static final Comparator<Node> COMPARATOR = Comparator.comparing(Node::getLocalName);
    }
}
