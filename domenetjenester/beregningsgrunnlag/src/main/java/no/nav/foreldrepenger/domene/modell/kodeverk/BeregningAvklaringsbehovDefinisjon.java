package no.nav.foreldrepenger.domene.modell.kodeverk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.TempAvledeKode;

/**
 * Definerer aksjonspunkter i beregning.
 */
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum BeregningAvklaringsbehovDefinisjon implements Kodeverdi {

    OVERSTYRING_AV_BEREGNINGSGRUNNLAG(
            BeregningAvklaringsbehovKodeDefinition.OVERSTYRING_AV_BEREGNINGSGRUNNLAG_KODE,
            "Overstyring av beregningsgrunnlag"),
    VURDER_FAKTA_FOR_ATFL_SN(
            BeregningAvklaringsbehovKodeDefinition.VURDER_FAKTA_FOR_ATFL_SN_KODE,
            "Vurder fakta for arbeidstaker, frilans og selvstendig næringsdrivende"),
    VURDER_GRADERING_UTEN_BEREGNINGSGRUNNLAG(
            BeregningAvklaringsbehovKodeDefinition.VURDER_GRADERING_UTEN_BEREGNINGSGRUNNLAG_KODE,
            "Vurder gradering på andel uten beregningsgrunnlag"),
    AVKLAR_AKTIVITETER(BeregningAvklaringsbehovKodeDefinition.AVKLAR_AKTIVITETER_KODE,
            "Avklar aktivitet for beregning"),
    AUTO_VENT_PÅ_INNTEKT_RAPPORTERINGSFRIST(
            BeregningAvklaringsbehovKodeDefinition.AUTO_VENT_PÅ_INNTEKT_RAPPORTERINGSFRIST_KODE,
            "Vent på rapporteringsfrist for inntekt"),
    AUTO_VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT(
            BeregningAvklaringsbehovKodeDefinition.AUTO_VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT_KODE,
            "Vent på siste meldekort for AAP eller DP-mottaker"),
    AUTO_VENT_FRISINN(
            BeregningAvklaringsbehovKodeDefinition.AUTO_VENT_FRISINN_KODE,
            "Vent på mangel i løsning: 36 måneder med ytelse"),
    AUTO_VENT_FRISINN_ATFL_SAMME_ORG(
            BeregningAvklaringsbehovKodeDefinition.AUTO_VENT_FRISINN_ATFL_SAMME_ORG_KODE,
            "Arbeidstaker og frilanser i samme organisasjon, kan ikke beregnes"),
    FORDEL_BEREGNINGSGRUNNLAG(
            BeregningAvklaringsbehovKodeDefinition.FORDEL_BEREGNINGSGRUNNLAG_KODE,
            "Fordel beregningsgrunnlag"),
    FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS(
            BeregningAvklaringsbehovKodeDefinition.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS_KODE,
            "Fastsette beregningsgrunnlag for arbeidstaker/frilanser skjønnsmessig"),
    FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE(
            BeregningAvklaringsbehovKodeDefinition.FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE_KODE,
            "Fastsett beregningsgrunnlag for selvstendig næringsdrivende"),
    FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET(
            BeregningAvklaringsbehovKodeDefinition.FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET_KODE,
            "Fastsett beregningsgrunnlag for SN som er ny i arbeidslivet"),
    AUTO_VENT_ULIKE_STARTDATOER_SVP(
            BeregningAvklaringsbehovKodeDefinition.AUTO_VENT_ULIKE_STARTDATOER_SVP_KODE,
            "Autopunkt ulike startdatoer svangerskapspenger"),
    AUTO_VENT_DELVIS_TILRETTELEGGING_OG_REFUSJON_SVP(
            BeregningAvklaringsbehovKodeDefinition.AUTO_VENT_DELVIS_TILRETTELEGGING_OG_REFUSJON_SVP_KODE,
            "Autopunkt delvis SVP og refusjon"),
    AUTO_VENT_AAP_DP_ENESTE_AKTIVITET_SVP(
            BeregningAvklaringsbehovKodeDefinition.AUTO_VENT_AAP_DP_ENESTE_AKTIVITET_SVP_KODE,
            "Autopunkt AAP/DP eneste aktivitet SVP"),
    VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE(
            BeregningAvklaringsbehovKodeDefinition.VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE_KODE,
            "Vurder varig endret/nyoppstartet næring selvstendig næringsdrivende"),
    FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD(
            BeregningAvklaringsbehovKodeDefinition.FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD_KODE,
            "Fastsett beregningsgrunnlag for tidsbegrenset arbeidsforhold"),
    VURDER_REFUSJON_BERGRUNN(
        BeregningAvklaringsbehovKodeDefinition.VURDER_REFUSJON_BERGRUNN,
        "Vurder endring i refusjon refusjon"),

    UNDEFINED,
    ;

    static final String KODEVERK = "BEREGNING_AVKLARINGSBEHOV_DEF";

    private static final Map<String, BeregningAvklaringsbehovDefinisjon> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String kode;

    @JsonIgnore
    private String navn;

    private BeregningAvklaringsbehovDefinisjon() {
        // for hibernate
    }

    private BeregningAvklaringsbehovDefinisjon(String kode, String navn) {
        this.kode = Objects.requireNonNull(kode);
        this.navn = navn;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String toString() {
        return super.toString() + "('" + getKode() + "')";
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static BeregningAvklaringsbehovDefinisjon fraKode(Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(BeregningAvklaringsbehovDefinisjon.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BeregningAvklaringsbehovDefinisjon: " + kode);
        }
        return ad;
    }

    public static Map<String, BeregningAvklaringsbehovDefinisjon> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }
}
