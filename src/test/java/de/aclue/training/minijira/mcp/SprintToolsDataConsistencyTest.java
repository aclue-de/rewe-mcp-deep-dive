package de.aclue.training.minijira.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.aclue.training.minijira.store.IssueStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs get_sprint_report against the real data/issues.json and checks it against the
 * exact Sprint 42 facts locked down by DataConsistencyTest.
 */
@Disabled("Lab 2 · Part B: remove @Disabled once get_sprint_report is implemented")
class SprintToolsDataConsistencyTest {

    private SprintTools sprintTools;

    @BeforeEach
    void setUp() {
        IssueStore store = new IssueStore(new ObjectMapper().findAndRegisterModules(), "data/issues.json");
        sprintTools = new SprintTools(store);
    }

    @Test
    void sprint42ReportMatchesTheRealData() {
        String report = sprintTools.getSprintReport("Sprint 42");

        assertThat(report).contains("BLOCKED: 2");

        String blockedSection = report.substring(report.indexOf("Blocked tickets"));
        assertThat(blockedSection).contains("SHOP-103").contains("SHOP-105");

        String highPrioSection = report.substring(report.indexOf("high priority"));
        assertThat(highPrioSection).contains("SHOP-101").contains("SHOP-103");
    }
}
