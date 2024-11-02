package no.nav.foreldrepenger.behandlingslager.virksomhet;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.behandlingslager.diff.TraverseValue;


/**
 * Id som genereres fra Nav aktørregister. Denne iden benyttes til interne forhold i Nav og vil ikke endres f.eks. dersom bruker går fra
 * DNR til FNR i Folkeregisteret. Tilsvarende vil den kunne referere personer som har ident fra et utenlandsk system.
 *
 * Støtter også kunstige orgnummer (internt definert konstant i fp - orgnummer=342352362)
 */
@Embeddable
public class OrgNummer implements Serializable, Comparable<OrgNummer>, IndexKey, TraverseValue {

    /**
     * Orgnr for KUNSTIG organisasjoner. Går sammen med OrganisasjonType#KUNSTIG.
     * (p.t. kun en kunstig organisasjon som holder på arbeidsforhold lagt til av saksbehandler.)
     */
    public static final String KUNSTIG_ORG = "342352362";  // magic constant

    @JsonValue
    @Column(name = "org_nummer", updatable = false, length = 50)
    private String orgNummer;

    public OrgNummer(String orgNummer) {
        Objects.requireNonNull(orgNummer, "orgNummer");
        if (!erGyldigOrgnr(orgNummer)) {
            // skal ikke skje, funksjonelle feilmeldinger håndteres ikke her.
            throw new IllegalArgumentException("Ikke gyldig orgnummer: " + orgNummer);
        }
        this.orgNummer = orgNummer;
    }

    protected OrgNummer() {
        // for hibernate
    }

    public static boolean erKunstig(String orgNr) {
        return KUNSTIG_ORG.equals(orgNr);
    }

    @Override
    public int compareTo(OrgNummer o) {
        // TODO: Burde ikke finnes
        return orgNummer.compareTo(o.orgNummer);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof OrgNummer other)) {
            return false;
        }
        return Objects.equals(orgNummer, other.orgNummer);
    }

    public String getId() {
        return orgNummer;
    }

    @Override
    public String getIndexKey() {
        return orgNummer;
    }

    @Override
    public int hashCode() {
        return Objects.hash(orgNummer);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + tilMaskertNummer(orgNummer) + ">";
    }

    public static String tilMaskertNummer(String orgNummer) {
        if (orgNummer == null) {
            return null;
        }
        var length = orgNummer.length();
        if (length <= 4) {
            return "*".repeat(length);
        }
        return "*".repeat(length - 4) + orgNummer.substring(length - 4);
    }

    /** @return false hvis ikke gyldig orgnr. */
    public static boolean erGyldigOrgnr(String ident) {
        return erKunstig(ident) || OrganisasjonsNummerValidator.erGyldig(ident);
    }
}
