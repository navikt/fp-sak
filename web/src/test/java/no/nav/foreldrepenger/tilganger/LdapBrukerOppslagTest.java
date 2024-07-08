package no.nav.foreldrepenger.tilganger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import javax.naming.LimitExceededException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.exception.TekniskException;

@ExtendWith(MockitoExtension.class)
class LdapBrukerOppslagTest {

    @Mock
    private LdapContext context;
    private LdapName baseSearch;

    @BeforeEach
    void beforeEach() throws Exception {
        baseSearch = new LdapName("ou=ServiceAccounts,dc=test,dc=local");
    }

    @Test
    void skal_liste_ut_brukernavn_når_det_er_i_resultatet() {
        var attributes = new BasicAttributes();
        attributes.put("displayName", "Lars Saksbehandler");
        attributes.put("cn", "L999999");
        attributes.put(new BasicAttribute("memberOf"));
        var resultat = new SearchResult("CN=L999999,OU=ApplAccounts", null, attributes);
        assertEquals("Lars Saksbehandler", new LdapBrukerOppslag(context, baseSearch).getDisplayName(resultat));
    }

    @Test
    void skal_liste_ut_gruppene_når_det_er_i_resultatet() {
        var attributes = new BasicAttributes();
        attributes.put("displayName", "Lars Saksbehandler");
        attributes.put("cn", "L999999");
        var memberOf = new BasicAttribute("memberOf");
        memberOf.add("CN=myGroup");
        memberOf.add("OU=ourGroup");
        attributes.put(memberOf);
        var resultat = new SearchResult("CN=L999999,OU=ApplAccounts", null, attributes);
        assertThat(new LdapBrukerOppslag(null, null).getMemberOf(resultat)).contains("CN=myGroup", "OU=ourGroup");
    }

    @Test
    void skal_gi_exception_når_søket_gir_ingen_treff() throws Exception {

        var heleResultatet = new SearchMock(Collections.emptyList());
        Mockito.when(context.search(ArgumentMatchers.eq(baseSearch), ArgumentMatchers.eq("(cn=L999999)"), ArgumentMatchers.any(SearchControls.class)))
                .thenReturn(heleResultatet);

        var ldapBrukeroppslag = new LdapBrukerOppslag(context, baseSearch);
        assertThrows(IntegrasjonException.class, () -> ldapBrukeroppslag.hentBrukerinformasjon("L999999"));
    }

    @Test
    void skal_gi_exception_når_søket_gir_to_treff() throws Exception {
        Mockito.when(context.search(ArgumentMatchers.eq(baseSearch), ArgumentMatchers.eq("(cn=L999999)"), ArgumentMatchers.any(SearchControls.class)))
                .thenThrow(new LimitExceededException("This is a test"));

        var ldapBrukeroppslag = new LdapBrukerOppslag(context, baseSearch);
        var e = assertThrows(IntegrasjonException.class, () -> ldapBrukeroppslag.hentBrukerinformasjon("L999999"));
        assertEquals("F-137440", e.getKode());

    }

    @Test
    void skal_gi_exception_når_svaret_mangler_forventet_attibutt() throws Exception {

        var attributes = new BasicAttributes();
        attributes.put("cn", "L999999");
        var resultat = new SearchResult("CN=L999999,OU=ApplAccounts", null, attributes);
        var heleResultatet = new SearchMock(List.of(resultat));
        Mockito.when(context.search(ArgumentMatchers.eq(baseSearch), ArgumentMatchers.eq("(cn=L999999)"), ArgumentMatchers.any(SearchControls.class)))
                .thenReturn(heleResultatet);

        var ldapBrukeroppslag = new LdapBrukerOppslag(context, baseSearch);
        var e = assertThrows(IntegrasjonException.class, () -> ldapBrukeroppslag.hentBrukerinformasjon("L999999"));
        assertTrue(e.getMessage().contains("Resultat fra LDAP manglet påkrevet attributtnavn displayName"));
    }

    @Test
    void skal_gi_exception_når_det_søkes_med_spesialtegn() {
        var ldap = new LdapBrukerOppslag(context, baseSearch);
        var e = assertThrows(TekniskException.class, () -> ldap.hentBrukerinformasjon("L999999) or (cn=A*"));
        assertEquals("F-271934", e.getKode());

    }

    private static class SearchMock implements NamingEnumeration<SearchResult> {

        private int index = 0;
        private final List<SearchResult> resultList;

        SearchMock(List<SearchResult> resultList) {
            this.resultList = resultList;
        }

        @Override
        public SearchResult next() {
            throw new IllegalArgumentException("Test---not implemented");
        }

        @Override
        public boolean hasMore() {
            throw new IllegalArgumentException("Test---not implemented");
        }

        @Override
        public void close() {

        }

        @Override
        public boolean hasMoreElements() {
            return index < resultList.size();
        }

        @Override
        public SearchResult nextElement() {
            return resultList.get(index++);
        }
    }
}
