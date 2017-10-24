package org.codedefenders;

import org.codedefenders.util.DatabaseAccess;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * Created by thomas on 11/07/2017.
 */
public class DatabaseAccessSanitiseTest {

    @Test
    public void testSanitiseHtml() {
        String s = DatabaseAccess.sanitise(
                "<script>/*some malicious script</script>"
        );

        assertFalse(s.contains("<"));
        assertFalse(s.contains(">"));
    }

    @Test
    public void testSanitiseSql() {
        String s = DatabaseAccess.sanitise(
                "'; DROP some_db; UPDATE users SET username='hello"
        );

        assertFalse(s.contains("'"));

        DatabaseAccess.sanitise(
                "\"; DROP some_db; UPDATE users SET username=\"hello"
        );

        assertFalse(s.contains("\""));
    }
}
