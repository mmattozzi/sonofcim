package sonofcim;

import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import java.util.Map;

public class UserQuoter {

    SimpleJdbcTemplate simpleJdbcTemplate;

    public UserQuoter(SimpleJdbcTemplate simpleJdbcTemplate) {
        this.simpleJdbcTemplate = simpleJdbcTemplate;
    }

    public String getQuote(String username) {
        if (username == null) return null;

        Map<String, Object> row = simpleJdbcTemplate.queryForMap(
                "select id, message, nick from messages where nick = ? order by rand() limit 1", username);
        String quote = String.format("#%d %s: %s", row.get("id"), row.get("nick"), row.get("message"));
        return quote;
    }
    
    public String getMessage(String msgGuid) {
        if (msgGuid == null) return null;

        Map<String, Object> row = simpleJdbcTemplate.queryForMap("select nick, message from messages where id = ?", msgGuid);
        String quote = row.get("nick") + ": " + row.get("message");

        return quote;
    }
}
