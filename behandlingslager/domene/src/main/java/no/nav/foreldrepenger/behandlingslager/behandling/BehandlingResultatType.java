package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum BehandlingResultatType implements Kodeverdi {

    IKKE_FASTSATT("IKKE_FASTSATT", "Ikke fastsatt"),
    INNVILGET("INNVILGET", "Innvilget"),
    AVSLÅTT("AVSLÅTT", "Avslått"),
    OPPHØR("OPPHØR", "Opphør"),
    HENLAGT_SØKNAD_TRUKKET("HENLAGT_SØKNAD_TRUKKET", "Henlagt, søknaden er trukket"),
    HENLAGT_FEILOPPRETTET("HENLAGT_FEILOPPRETTET", "Henlagt, søknaden er feilopprettet"),
    HENLAGT_BRUKER_DØD("HENLAGT_BRUKER_DØD", "Henlagt, brukeren er død"),
    MERGET_OG_HENLAGT("MERGET_OG_HENLAGT", "Mottatt ny søknad"),
    HENLAGT_SØKNAD_MANGLER("HENLAGT_SØKNAD_MANGLER", "Henlagt, søknad mangler"),
    FORELDREPENGER_ENDRET("FORELDREPENGER_ENDRET", "Sak er endret"),
    FORELDREPENGER_SENERE("FORELDREPENGER_SENERE", "Sak er endret"),
    INGEN_ENDRING("INGEN_ENDRING", "Ingen endring"),
    @Deprecated // Tidligere brukt ifm flytting til infotrygd ved feilopprettede saker
    MANGLER_BEREGNINGSREGLER("MANGLER_BEREGNINGSREGLER", "Mangler beregningsregler"),

    // Klage
    KLAGE_AVVIST("KLAGE_AVVIST", "Klage er avvist"),
    KLAGE_MEDHOLD("KLAGE_MEDHOLD", "Medhold"),
    KLAGE_DELVIS_MEDHOLD("KLAGE_DELVIS_MEDHOLD", "Delvis medhold"),
    KLAGE_OMGJORT_UGUNST("KLAGE_OMGJORT_UGUNST", "Omgjort til ugunst"),
    KLAGE_YTELSESVEDTAK_OPPHEVET("KLAGE_YTELSESVEDTAK_OPPHEVET", "Ytelsesvedtak opphevet"),
    KLAGE_YTELSESVEDTAK_STADFESTET("KLAGE_YTELSESVEDTAK_STADFESTET", "Ytelsesvedtak stadfestet"),
    KLAGE_TILBAKEKREVING_VEDTAK_STADFESTET("KLAGE_TILBAKEKREVING_VEDTAK_STADFESTET", "Tilbakekrevingsvedtak stadfestet"), // Brukes av kun Tilbakekreving eller Tilbakekreving Revurdering
    HENLAGT_KLAGE_TRUKKET("HENLAGT_KLAGE_TRUKKET", "Henlagt, klagen er trukket"),
    HJEMSENDE_UTEN_OPPHEVE("HJEMSENDE_UTEN_OPPHEVE", "Behandlingen er hjemsendt"),

    // Anke
    ANKE_AVVIST("ANKE_AVVIST", "Anke er avvist"),
    ANKE_MEDHOLD("ANKE_MEDHOLD", "Medhold"),
    ANKE_DELVIS_MEDHOLD("ANKE_DELVIS_MEDHOLD", "Delvis medhold"),
    ANKE_OMGJORT_UGUNST("ANKE_OMGJORT_UGUNST", "Omgjort til ugunst"),
    ANKE_OPPHEVE_OG_HJEMSENDE("ANKE_OPPHEVE_OG_HJEMSENDE", "Vedtak opphevet"),
    ANKE_HJEMSENDE_UTEN_OPPHEV("ANKE_HJEMSENDE_UTEN_OPPHEV", "Behandlingen er hjemsendt"),
    ANKE_YTELSESVEDTAK_STADFESTET("ANKE_YTELSESVEDTAK_STADFESTET", "Vedtak stadfestet"),
    HENLAGT_ANKE_TRUKKET("HENLAGT_ANKE_TRUKKET", "Henlagt, anken er trukket"),

    // Innsyn
    INNSYN_INNVILGET("INNSYN_INNVILGET", "Innsynskrav er innvilget"),
    INNSYN_DELVIS_INNVILGET("INNSYN_DELVIS_INNVILGET", "Innsynskrav er delvis innvilget"),
    INNSYN_AVVIST("INNSYN_AVVIST", "Innsynskrav er avvist"),
    HENLAGT_INNSYN_TRUKKET("HENLAGT_INNSYN_TRUKKET", "Henlagt, innsynskrav er trukket"),

    ;

    private static final Set<BehandlingResultatType> HENLEGGELSESKODER_FOR_SØKNAD = Set.of(HENLAGT_SØKNAD_TRUKKET, HENLAGT_FEILOPPRETTET, HENLAGT_BRUKER_DØD, HENLAGT_SØKNAD_MANGLER, MANGLER_BEREGNINGSREGLER);
    private static final Set<BehandlingResultatType> ALLE_HENLEGGELSESKODER = Set.of(HENLAGT_SØKNAD_TRUKKET, HENLAGT_FEILOPPRETTET, HENLAGT_BRUKER_DØD, HENLAGT_KLAGE_TRUKKET, HENLAGT_ANKE_TRUKKET, MERGET_OG_HENLAGT, HENLAGT_SØKNAD_MANGLER, HENLAGT_INNSYN_TRUKKET, MANGLER_BEREGNINGSREGLER);
    private static final Set<BehandlingResultatType> ALLE_INNVILGET_KODER = Set.of(INNVILGET, FORELDREPENGER_ENDRET, FORELDREPENGER_SENERE, INGEN_ENDRING);

    private static final Map<String, BehandlingResultatType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private final String navn;
    @JsonValue
    private final String kode;

    BehandlingResultatType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static BehandlingResultatType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BehandlingResultatType: " + kode);
        }
        return ad;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKode() {
        return kode;
    }

    public static Set<BehandlingResultatType> getAlleHenleggelseskoder() {
        return ALLE_HENLEGGELSESKODER;
    }

    public static Set<BehandlingResultatType> getHenleggelseskoderForSøknad() {
        return HENLEGGELSESKODER_FOR_SØKNAD;
    }

    public static Set<BehandlingResultatType> getAlleInnvilgetKoder() {
        return ALLE_INNVILGET_KODER;
    }

    public boolean erHenlagt() {
        return ALLE_HENLEGGELSESKODER.contains(this);
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<BehandlingResultatType, String> {
        @Override
        public String convertToDatabaseColumn(BehandlingResultatType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public BehandlingResultatType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

}
