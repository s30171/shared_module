package idv.mark.platform.core.process;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/** Redacts supplied secret values and common credential-bearing log/URL forms. */
public final class SecretRedactor {
    private static final Pattern HEADER = Pattern.compile("(?i)((?:authorization|api[-_]?key|access[-_]?token|refresh[-_]?token|password|cookie|secret)\\s*[:=]\\s*)(?:Bearer\\s+)?[^\\s,;]+(?:$)?");
    private static final Pattern QUERY = Pattern.compile("(?i)([?&](?:key|token|access_token|refresh_token|api_key|signature|sig)=)[^&#\\s]+");
    private static final Pattern USER_INFO = Pattern.compile("(https?://)[^/@\\s]+:[^/@\\s]+@");
    private final List<String> secrets;

    public SecretRedactor(Collection<String> secrets) {
        this.secrets = secrets.stream().filter(s -> s != null && !s.isEmpty()).distinct()
                .sorted((a, b) -> Integer.compare(b.length(), a.length())).toList();
    }
    public String redact(String value) {
        if (value == null) return null;
        String result = value;
        for (String secret : secrets) result = result.replace(secret, "[REDACTED]");
        result = QUERY.matcher(result).replaceAll("$1[REDACTED]");
        result = HEADER.matcher(result).replaceAll("$1[REDACTED]");
        return USER_INFO.matcher(result).replaceAll("$1[REDACTED]@");
    }
}
