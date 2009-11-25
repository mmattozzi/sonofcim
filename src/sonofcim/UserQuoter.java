package sonofcim;

import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

public class UserQuoter {

    SimpleJdbcTemplate simpleJdbcTemplate;

    public UserQuoter(SimpleJdbcTemplate simpleJdbcTemplate) {
        this.simpleJdbcTemplate = simpleJdbcTemplate;
    }

    public String getQuote(String username) {
        if (username == null) return null;

        String quote = simpleJdbcTemplate.queryForObject("select message from messages where nick = ? order by rand() limit 1",
                String.class, username);

        return quote;
    }
}
