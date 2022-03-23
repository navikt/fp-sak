package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
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
    HENLAGT_SØKNAD_MANGLER("HENLAGT_SØKNAD_MANGLER", "Henlagt søknad mangler"),
    FORELDREPENGER_ENDRET("FORELDREPENGER_ENDRET", "Sak er endret"),
    FORELDREPENGER_SENERE("FORELDREPENGER_SENERE", "Sak er endret"),
    INGEN_ENDRING("INGEN_ENDRING", "Ingen endring"),
    @Deprecated // Tidligere brukt ifm flytting til infotrygd ved feilopprettede saker
    MANGLER_BEREGNINGSREGLER("MANGLER_BEREGNINGSREGLER", "Mangler beregningsregler"),

    // Klage
    KLAGE_AVVIST("KLAGE_AVVIST", "Klage er avvist"),
    KLAGE_MEDHOLD("KLAGE_MEDHOLD", "Medhold"),
    KLAGE_YTELSESVEDTAK_OPPHEVET("KLAGE_YTELSESVEDTAK_OPPHEVET", "Ytelsesvedtak opphevet"),
    KLAGE_YTELSESVEDTAK_STADFESTET("KLAGE_YTELSESVEDTAK_STADFESTET", "Ytelsesvedtak stadfestet"),
    KLAGE_TILBAKEKREVING_VEDTAK_STADFESTET("KLAGE_TILBAKEKREVING_VEDTAK_STADFESTET", "Vedtak tilbakekreving stadfestet"), // Brukes av kun Tilbakekreving eller Tilbakekreving Revurdering
    HENLAGT_KLAGE_TRUKKET("HENLAGT_KLAGE_TRUKKET", "Henlagt, klagen er trukket"),
    HJEMSENDE_UTEN_OPPHEVE("HJEMSENDE_UTEN_OPPHEVE", "Behandlingen er hjemsendt"),

    // Anke
    ANKE_AVVIST("ANKE_AVVIST", "Anke er avvist"),
    ANKE_OMGJOER("ANKE_OMGJOER", "Bruker har fått omgjøring i anke"),
    ANKE_OPPHEVE_OG_HJEMSENDE("ANKE_OPPHEVE_OG_HJEMSENDE", "Bruker har fått vedtaket opphevet og hjemsendt i anke"),
    ANKE_HJEMSENDE_UTEN_OPPHEV("ANKE_HJEMSENDE_UTEN_OPPHEV", "Bruker har fått vedtaket hjemsendt i anke"),
    ANKE_YTELSESVEDTAK_STADFESTET("ANKE_YTELSESVEDTAK_STADFESTET", "Anken er stadfestet/opprettholdt"),

    // Innsyn
    INNSYN_INNVILGET("INNSYN_INNVILGET", "Innsynskrav er innvilget"),
    INNSYN_DELVIS_INNVILGET("INNSYN_DELVIS_INNVILGET", "Innsynskrav er delvis innvilget"),
    INNSYN_AVVIST("INNSYN_AVVIST", "Innsynskrav er avvist"),
    HENLAGT_INNSYN_TRUKKET("HENLAGT_INNSYN_TRUKKET", "Henlagt, innsynskrav er trukket"),

    ;

    private static final Set<BehandlingResultatType> HENLEGGELSESKODER_FOR_SØKNAD = Set.of(HENLAGT_SØKNAD_TRUKKET, HENLAGT_FEILOPPRETTET, HENLAGT_BRUKER_DØD, HENLAGT_SØKNAD_MANGLER, MANGLER_BEREGNINGSREGLER);
    private static final Set<BehandlingResultatType> ALLE_HENLEGGELSESKODER = Set.of(HENLAGT_SØKNAD_TRUKKET, HENLAGT_FEILOPPRETTET, HENLAGT_BRUKER_DØD, HENLAGT_KLAGE_TRUKKET, MERGET_OG_HENLAGT, HENLAGT_SØKNAD_MANGLER, HENLAGT_INNSYN_TRUKKET, MANGLER_BEREGNINGSREGLER);
    private static final Set<BehandlingResultatType> KLAGE_KODER = Set.of(KLAGE_MEDHOLD, KLAGE_YTELSESVEDTAK_STADFESTET, KLAGE_YTELSESVEDTAK_OPPHEVET, KLAGE_AVVIST, HJEMSENDE_UTEN_OPPHEVE);
    private static final Set<BehandlingResultatType> ANKE_KODER = Set.of(ANKE_AVVIST, ANKE_OMGJOER, ANKE_OPPHEVE_OG_HJEMSENDE, ANKE_HJEMSENDE_UTEN_OPPHEV, ANKE_YTELSESVEDTAK_STADFESTET);
    private static final Set<BehandlingResultatType> INNSYN_KODER = Set.of(INNSYN_INNVILGET, INNSYN_DELVIS_INNVILGET, INNSYN_AVVIST);
    private static final Set<BehandlingResultatType> ALLE_INNVILGET_KODER = Set.of(INNVILGET, FORELDREPENGER_ENDRET, FORELDREPENGER_SENERE, INGEN_ENDRING);

    private static final Map<AnkeVurdering, BehandlingResultatType> ANKE_RESULTAT = Map.ofEntries(
        Map.entry(AnkeVurdering.ANKE_AVVIS, BehandlingResultatType.ANKE_AVVIST),
        Map.entry(AnkeVurdering.ANKE_STADFESTE_YTELSESVEDTAK, BehandlingResultatType.ANKE_YTELSESVEDTAK_STADFESTET),
        Map.entry(AnkeVurdering.ANKE_OMGJOER, BehandlingResultatType.ANKE_OMGJOER),
        Map.entry(AnkeVurdering.ANKE_OPPHEVE_OG_HJEMSENDE,  BehandlingResultatType.ANKE_OPPHEVE_OG_HJEMSENDE),
        Map.entry(AnkeVurdering.ANKE_HJEMSEND_UTEN_OPPHEV, BehandlingResultatType.ANKE_HJEMSENDE_UTEN_OPPHEV)
    );

    private static final Map<KlageVurdering, BehandlingResultatType> KLAGE_RESULTAT = Map.ofEntries(
        Map.entry(KlageVurdering.AVVIS_KLAGE, BehandlingResultatType.KLAGE_AVVIST),
        Map.entry(KlageVurdering.STADFESTE_YTELSESVEDTAK, BehandlingResultatType.KLAGE_YTELSESVEDTAK_STADFESTET),
        Map.entry(KlageVurdering.MEDHOLD_I_KLAGE, BehandlingResultatType.KLAGE_MEDHOLD),
        Map.entry(KlageVurdering.OPPHEVE_YTELSESVEDTAK,  BehandlingResultatType.KLAGE_YTELSESVEDTAK_OPPHEVET),
        Map.entry(KlageVurdering.HJEMSENDE_UTEN_Å_OPPHEVE, BehandlingResultatType.HJEMSENDE_UTEN_OPPHEVE)
    );

    private static final Map<String, BehandlingResultatType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "BEHANDLING_RESULTAT_TYPE";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;
    @JsonValue
    private String kode;

    BehandlingResultatType(String kode) {
        this.kode = kode;
    }

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

    public static Map<String, BehandlingResultatType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    public static Set<BehandlingResultatType> getAlleHenleggelseskoder() {
        return ALLE_HENLEGGELSESKODER;
    }

    public static Set<BehandlingResultatType> getHenleggelseskoderForSøknad() {
        return HENLEGGELSESKODER_FOR_SØKNAD;
    }

    public static Set<BehandlingResultatType> getKlageKoder() {
        return KLAGE_KODER;
    }

    public static Set<BehandlingResultatType> getAnkeKoder() {
        return ANKE_KODER;
    }

    public static Set<BehandlingResultatType> getInnsynKoder() {
        return INNSYN_KODER;
    }

    public static Set<BehandlingResultatType> getAlleInnvilgetKoder() {
        return ALLE_INNVILGET_KODER;
    }

    public boolean erHenlagt() {
        return ALLE_HENLEGGELSESKODER.contains(this);
    }

    public static BehandlingResultatType tolkBehandlingResultatType(AnkeVurdering vurdering) {
        return ANKE_RESULTAT.get(vurdering);
    }

    public static BehandlingResultatType tolkBehandlingResultatType(KlageVurdering vurdering, boolean erPåklagdEksternBehandling) {
        if (erPåklagdEksternBehandling && KlageVurdering.STADFESTE_YTELSESVEDTAK.equals(vurdering)) {
            return KLAGE_TILBAKEKREVING_VEDTAK_STADFESTET;
        }
        return KLAGE_RESULTAT.get(vurdering);
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
