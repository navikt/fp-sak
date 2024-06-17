package no.nav.foreldrepenger.web.app.util;

import java.util.Collection;
import java.util.Locale;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LdapUtil {

    private static final Logger LOG = LoggerFactory.getLogger(LdapUtil.class);

    private LdapUtil() {
        // SONAR - Add a private constructor to hide the implicit public one
    }

    public static Collection<String> filtrerGrupper(Collection<String> grupper) {
        return grupper.stream().map(LdapUtil::filterDNtoCNvalue).toList();
    }

    private static String filterDNtoCNvalue(String value) {
        if (value.toLowerCase(Locale.ROOT).contains("cn=")) {
            try {
                var ldapname = new LdapName(value);
                for (var rdn : ldapname.getRdns()) {
                    if ("CN".equalsIgnoreCase(rdn.getType())) {
                        var cn = rdn.getValue().toString();
                        LOG.debug("uid on DN form. Filtered from {} to {}", value, cn);
                        return cn;
                    }
                }
            } catch (InvalidNameException e) {
                LOG.debug("value not on DN form. Skipping filter. {}", e.getExplanation());
            }
        }
        return value;
    }
}
