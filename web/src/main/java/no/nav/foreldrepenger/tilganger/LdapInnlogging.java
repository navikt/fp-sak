package no.nav.foreldrepenger.tilganger;

import static java.lang.String.format;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.exception.TekniskException;

public class LdapInnlogging {

    private static final Environment ENV = Environment.current();

    private LdapInnlogging() {
        throw new IllegalArgumentException("skal ikke instansieres");
    }

    public static LdapContext lagLdapContext() {
        var authMode = ENV.getProperty("ldap.auth", "simple");
        var url = getRequiredProperty("ldap.url");

        var environment = new Hashtable<String, Object>();  //metodeparameter krever Hashtable
        environment.put(Context.INITIAL_CONTEXT_FACTORY, ENV.getProperty("ldap.ctxfactory", "com.sun.jndi.ldap.LdapCtxFactory"));
        environment.put(Context.PROVIDER_URL, url);
        environment.put(Context.SECURITY_AUTHENTICATION, authMode);

        if ("simple".equals(authMode)) {
            var user = getRequiredProperty("ldap.username") + "@" + getRequiredProperty("ldap.domain");
            environment.put(Context.SECURITY_CREDENTIALS, getRequiredProperty("ldap.password"));
            environment.put(Context.SECURITY_PRINCIPAL, user);
        } else if ("none".equals(authMode)) {
            // do nothing
        } else {
            // støtter ikke [java.naming.security.authentication]="strong" eller andre.
            // Ignorerer også foreløpig.
        }

        try {
            return new InitialLdapContext(environment, null);
        } catch (NamingException e) {
            throw new TekniskException("F-222862", String.format("Klarte ikke koble til LDAP på URL %s", url));
        }
    }

    static String getRequiredProperty(String key) {
        return ENV.getRequiredProperty(key,
            () -> new TekniskException("F-055498", format("Klarte ikke koble til LDAP da påkrevd prroperty %s ikke er satt", key)));
    }
}
