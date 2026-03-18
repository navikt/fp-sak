package no.nav.foreldrepenger.domene.iay.modell.kodeverk;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum InntektYtelseType implements Kodeverdi {

    // Ytelse utbetalt til person som er arbeidstaker/frilanser/ytelsesmottaker
    AAP("Arbeidsavklaringspenger", Kategori.YTELSE),
    DAGPENGER("Dagpenger arbeid og hyre", Kategori.YTELSE),
    FORELDREPENGER("Foreldrepenger", Kategori.YTELSE),
    SVANGERSKAPSPENGER("Svangerskapspenger", Kategori.YTELSE),
    SYKEPENGER("Sykepenger", Kategori.YTELSE),
    OMSORGSPENGER("Omsorgspenger", Kategori.YTELSE),
    OPPLÆRINGSPENGER("Opplæringspenger", Kategori.YTELSE),
    PLEIEPENGER("Pleiepenger", Kategori.YTELSE),
    OVERGANGSSTØNAD_ENSLIG("Overgangsstønad til enslig mor eller far", Kategori.YTELSE),
    VENTELØNN("Ventelønn", Kategori.YTELSE),

    // Feriepenger Ytelse utbetalt til person som er arbeidstaker/frilanser/ytelsesmottaker
    // TODO slå sammen til FERIEPENGER_YTELSE - eller ta de med under hver ytelse???
    FERIEPENGER_FORELDREPENGER("Feriepenger foreldrepenger", Kategori.YTELSE),
    FERIEPENGER_SVANGERSKAPSPENGER("Feriepenger svangerskapspenger", Kategori.YTELSE),
    FERIEPENGER_OMSORGSPENGER("Feriepenger omsorgspenger", Kategori.YTELSE),
    FERIEPENGER_OPPLÆRINGSPENGER("Feriepenger opplæringspenger", Kategori.YTELSE),
    FERIEPENGER_PLEIEPENGER("Feriepenger pleiepenger", Kategori.YTELSE),
    FERIEPENGER_SYKEPENGER("Feriepenger sykepenger", Kategori.YTELSE),
    FERIETILLEGG_DAGPENGER("Ferietillegg dagpenger ", Kategori.YTELSE),

    // Annen ytelse utbetalt til person
    KVALIFISERINGSSTØNAD("Kvalifiseringsstønad", Kategori.TRYGD),

    // Ytelse utbetalt til person som er næringsdrivende, fisker/lott, dagmamma eller jord/skogbruker
    FORELDREPENGER_NÆRING("Foreldrepenger næring", Kategori.NÆRING),
    SVANGERSKAPSPENGER_NÆRING("Svangerskapspenger næring", Kategori.NÆRING),
    SYKEPENGER_NÆRING("Sykepenger næring", Kategori.NÆRING),
    OMSORGSPENGER_NÆRING("Omsorgspenger næring", Kategori.NÆRING),
    OPPLÆRINGSPENGER_NÆRING("Opplæringspenger næring", Kategori.NÆRING),
    PLEIEPENGER_NÆRING("Pleiepenger næring", Kategori.NÆRING),
    DAGPENGER_NÆRING("Dagpenger næring", Kategori.NÆRING),

    // Annen ytelse utbetalt til person som er næringsdrivende
    ANNET("Annet", Kategori.NÆRING),
    VEDERLAG("Vederlag", Kategori.NÆRING),
    LOTT_KUN_TRYGDEAVGIFT("Lott kun trygdeavgift", Kategori.NÆRING),
    KOMPENSASJON_FOR_TAPT_PERSONINNTEKT("Kompensasjon for tapt personinntekt", Kategori.NÆRING)
    ;

    @JsonIgnore
    private final String navn;

    @SuppressWarnings("unused") // Kategori: Beholder for likhet med Abakus
    InntektYtelseType(String navn, Kategori kategori) {
        this.navn = navn;
    }

    public static InntektYtelseType fraKode(String kode) {
        return kode != null ? InntektYtelseType.valueOf(kode) : null;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    @JsonValue
    public String getKode() {
        return name();
    }

    public enum Kategori { YTELSE, NÆRING, TRYGD }
}
