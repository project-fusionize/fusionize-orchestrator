package dev.fusionize.workflow.context;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowDecisionTest {

    @Test
    void shouldDefaultToEmptyOptionNodes() {
        // setup
        var decision = new WorkflowDecision();

        // expectation
        Map<String, Boolean> optionNodes = decision.getOptionNodes();

        // validation
        assertThat(optionNodes).isNotNull().isEmpty();
    }

    @Test
    void shouldSetAndGetDecisionNode() {
        // setup
        var decision = new WorkflowDecision();

        // expectation
        decision.setDecisionNode("decision-1");

        // validation
        assertThat(decision.getDecisionNode()).isEqualTo("decision-1");
    }

    @Test
    void shouldSetAndGetOptionNodes() {
        // setup
        var decision = new WorkflowDecision();
        var options = Map.of("optionA", true, "optionB", false);

        // expectation
        decision.setOptionNodes(new HashMap<>(options));

        // validation
        assertThat(decision.getOptionNodes()).containsEntry("optionA", true).containsEntry("optionB", false);
    }

    @Test
    void shouldRenewWithDeepCopy() {
        // setup
        var original = new WorkflowDecision();
        original.setDecisionNode("decision-1");
        original.setOptionNodes(new HashMap<>(Map.of("optionA", true)));

        // expectation
        var copy = original.renew();
        original.setDecisionNode("decision-modified");
        original.getOptionNodes().put("optionB", false);

        // validation
        assertThat(copy.getDecisionNode()).isEqualTo("decision-1");
        assertThat(copy.getOptionNodes()).containsOnlyKeys("optionA");
        assertThat(copy.getOptionNodes().get("optionA")).isTrue();
    }

    @Test
    void shouldBeEqualWhenSameFields() {
        // setup
        var decision1 = new WorkflowDecision();
        decision1.setDecisionNode("decision-1");
        decision1.setOptionNodes(new HashMap<>(Map.of("optionA", true)));

        var decision2 = new WorkflowDecision();
        decision2.setDecisionNode("decision-1");
        decision2.setOptionNodes(new HashMap<>(Map.of("optionA", true)));

        // expectation & validation
        assertThat(decision1).isEqualTo(decision2);
    }

    @Test
    void shouldNotBeEqualWhenDifferentDecisionNode() {
        // setup
        var decision1 = new WorkflowDecision();
        decision1.setDecisionNode("decision-1");

        var decision2 = new WorkflowDecision();
        decision2.setDecisionNode("decision-2");

        // expectation & validation
        assertThat(decision1).isNotEqualTo(decision2);
    }

    @Test
    void shouldNotBeEqualWhenDifferentOptionNodes() {
        // setup
        var decision1 = new WorkflowDecision();
        decision1.setDecisionNode("decision-1");
        decision1.setOptionNodes(new HashMap<>(Map.of("optionA", true)));

        var decision2 = new WorkflowDecision();
        decision2.setDecisionNode("decision-1");
        decision2.setOptionNodes(new HashMap<>(Map.of("optionA", false)));

        // expectation & validation
        assertThat(decision1).isNotEqualTo(decision2);
    }

    @Test
    void shouldHaveConsistentHashCode() {
        // setup
        var decision1 = new WorkflowDecision();
        decision1.setDecisionNode("decision-1");
        decision1.setOptionNodes(new HashMap<>(Map.of("optionA", true)));

        var decision2 = new WorkflowDecision();
        decision2.setDecisionNode("decision-1");
        decision2.setOptionNodes(new HashMap<>(Map.of("optionA", true)));

        // expectation & validation
        assertThat(decision1.hashCode()).isEqualTo(decision2.hashCode());
    }

    @Test
    void shouldReturnMeaningfulToString() {
        // setup
        var decision = new WorkflowDecision();
        decision.setDecisionNode("decision-1");
        decision.setOptionNodes(new HashMap<>(Map.of("optionA", true)));

        // expectation
        var result = decision.toString();

        // validation
        assertThat(result).contains("WorkflowDecision");
        assertThat(result).contains("decision-1");
        assertThat(result).contains("optionA");
    }
}
