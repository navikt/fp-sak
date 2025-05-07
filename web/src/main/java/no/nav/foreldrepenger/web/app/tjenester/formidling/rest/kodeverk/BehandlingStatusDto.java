package no.nav.foreldrepenger.web.app.tjenester.formidling.rest.kodeverk;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * NB: Pass på! Ikke legg koder vilkårlig her
 * Denne definerer etablerte behandlingstatuser ihht. modell angitt av FFA (Forretning og Fag).
 */
public enum BehandlingStatusDto {

    AVSLUTTET("AVSLU"),
    FATTER_VEDTAK("FVED"),
    IVERKSETTER_VEDTAK("IVED"),
    OPPRETTET("OPPRE"),
    UTREDES("UTRED")
    ;

    @JsonValue
    private final String kode;

    BehandlingStatusDto(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }
}
