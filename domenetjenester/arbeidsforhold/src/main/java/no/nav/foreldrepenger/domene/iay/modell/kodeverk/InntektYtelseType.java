package no.nav.foreldrepenger.domene.iay.modell.kodeverk;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;


@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum InntektYtelseType implements Kodeverdi {

    // Ytelse utbetalt til person som er arbeidstaker/frilanser/ytelsesmottaker
    AAP("Arbeidsavklaringspenger", Kategori.YTELSE, RelatertYtelseType.ARBEIDSAVKLARINGSPENGER),
    DAGPENGER("Dagpenger arbeid og hyre", Kategori.YTELSE, RelatertYtelseType.DAGPENGER),
    FORELDREPENGER("Foreldrepenger", Kategori.YTELSE, RelatertYtelseType.FORELDREPENGER),
    SVANGERSKAPSPENGER("Svangerskapspenger", Kategori.YTELSE, RelatertYtelseType.SVANGERSKAPSPENGER),
    SYKEPENGER("Sykepenger", Kategori.YTELSE, RelatertYtelseType.SYKEPENGER),
    OMSORGSPENGER("Omsorgspenger", Kategori.YTELSE, RelatertYtelseType.OMSORGSPENGER),
    OPPLÆRINGSPENGER("Opplæringspenger", Kategori.YTELSE, RelatertYtelseType.OPPLÆRINGSPENGER),
    PLEIEPENGER("Pleiepenger", Kategori.YTELSE, RelatertYtelseType.PLEIEPENGER_SYKT_BARN),
    OVERGANGSSTØNAD_ENSLIG("Overgangsstønad til enslig mor eller far", Kategori.YTELSE, RelatertYtelseType.ENSLIG_FORSØRGER),
    VENTELØNN("Ventelønn", Kategori.YTELSE, RelatertYtelseType.UDEFINERT),

    // Feriepenger Ytelse utbetalt til person som er arbeidstaker/frilanser/ytelsesmottaker
    // TODO slå sammen til FERIEPENGER_YTELSE - eller ta de med under hver ytelse???
    FERIEPENGER_FORELDREPENGER("Feriepenger foreldrepenger", Kategori.YTELSE, RelatertYtelseType.FORELDREPENGER),
    FERIEPENGER_SVANGERSKAPSPENGER("Feriepenger svangerskapspenger", Kategori.YTELSE, RelatertYtelseType.SVANGERSKAPSPENGER),
    FERIEPENGER_OMSORGSPENGER("Feriepenger omsorgspenger", Kategori.YTELSE, RelatertYtelseType.OMSORGSPENGER),
    FERIEPENGER_OPPLÆRINGSPENGER("Feriepenger opplæringspenger", Kategori.YTELSE, RelatertYtelseType.OPPLÆRINGSPENGER),
    FERIEPENGER_PLEIEPENGER("Feriepenger pleiepenger", Kategori.YTELSE, RelatertYtelseType.PLEIEPENGER_SYKT_BARN),
    FERIEPENGER_SYKEPENGER("Feriepenger sykepenger", Kategori.YTELSE, RelatertYtelseType.SYKEPENGER),
    FERIETILLEGG_DAGPENGER("Ferietillegg dagpenger ", Kategori.YTELSE, RelatertYtelseType.DAGPENGER),

    // Annen ytelse utbetalt til person
    KVALIFISERINGSSTØNAD("Kvalifiseringsstønad", Kategori.TRYGD, RelatertYtelseType.UDEFINERT),

    // Ytelse utbetalt til person som er næringsdrivende, fisker/lott, dagmamma eller jord/skogbruker
    FORELDREPENGER_NÆRING("Foreldrepenger næring", Kategori.NÆRING, RelatertYtelseType.FORELDREPENGER),
    SVANGERSKAPSPENGER_NÆRING("Svangerskapspenger næring", Kategori.NÆRING, RelatertYtelseType.SVANGERSKAPSPENGER),
    SYKEPENGER_NÆRING("Sykepenger næring", Kategori.NÆRING, RelatertYtelseType.SYKEPENGER),
    OMSORGSPENGER_NÆRING("Omsorgspenger næring", Kategori.NÆRING, RelatertYtelseType.OMSORGSPENGER),
    OPPLÆRINGSPENGER_NÆRING("Opplæringspenger næring", Kategori.NÆRING, RelatertYtelseType.OPPLÆRINGSPENGER),
    PLEIEPENGER_NÆRING("Pleiepenger næring", Kategori.NÆRING, RelatertYtelseType.PLEIEPENGER_SYKT_BARN),
    DAGPENGER_NÆRING("Dagpenger næring", Kategori.NÆRING, RelatertYtelseType.DAGPENGER),

    // Annen ytelse utbetalt til person som er næringsdrivende
    ANNET("Annet", Kategori.NÆRING, RelatertYtelseType.UDEFINERT),
    VEDERLAG("Vederlag", Kategori.NÆRING, RelatertYtelseType.UDEFINERT),
    LOTT_KUN_TRYGDEAVGIFT("Lott kun trygdeavgift", Kategori.NÆRING, RelatertYtelseType.UDEFINERT),
    KOMPENSASJON_FOR_TAPT_PERSONINNTEKT("Kompensasjon for tapt personinntekt", Kategori.NÆRING, RelatertYtelseType.FRISINN)
    ;

    public static final String KODEVERK = "INNTEKT_YTELSE_TYPE";

    @JsonIgnore
    private final String navn;
    @JsonIgnore
    private final RelatertYtelseType ytelseType;

    @SuppressWarnings("unused") // Kategori: Beholder for likhet med Abakus
    InntektYtelseType(String navn, Kategori kategori, RelatertYtelseType ytelseType) {
        this.navn = navn;
        this.ytelseType = ytelseType;
    }

    public static InntektYtelseType fraKode(String kode) {
        return kode != null ? InntektYtelseType.valueOf(kode) : null;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    @JsonValue
    public String getKode() {
        return name();
    }

    public RelatertYtelseType getYtelseType() {
        return ytelseType;
    }

    public enum Kategori { YTELSE, NÆRING, TRYGD }
}
