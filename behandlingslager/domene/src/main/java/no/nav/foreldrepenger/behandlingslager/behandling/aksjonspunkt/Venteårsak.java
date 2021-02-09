package no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.TempAvledeKode;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum Venteårsak implements Kodeverdi {

    UDEFINERT("-", "Ikke definert"),

    AAP_DP_ENESTE_AKTIVITET_SVP("AAP_DP_ENESTE_AKTIVITET_SVP", "Bruker har ikke rett til svangerskapspenger når eneste aktivitet er AAP/DP"),
    ANKE_OVERSENDT_TIL_TRYGDERETTEN("ANKE_OVERSENDT_TIL_TRYGDERETTEN", "Venter på at saken blir behandlet hos Trygderetten"),
    ANKE_VENTER_PAA_MERKNADER_FRA_BRUKER("ANKE_VENTER_PAA_MERKNADER_FRA_BRUKER", "Venter på merknader fra bruker"),
    AVV_DOK("AVV_DOK", "Avventer dokumentasjon"),
    AVV_FODSEL("AVV_FODSEL", "Avventer fødsel"),
    AVV_RESPONS_REVURDERING("AVV_RESPONS_REVURDERING", "Avventer respons på varsel om revurdering"),
    DELVIS_TILRETTELEGGING_OG_REFUSJON_SVP("DELVIS_TILRETTELEGGING_OG_REFUSJON_SVP", "Behandlingen er satt på vent på grunn av mangel i løsningen. Det jobbes med å løse dette."),
    FLERE_ARBEIDSFORHOLD_SAMME_ORG_SVP("FLERE_ARBEIDSFORHOLD_SAMME_ORG_SVP", "Håndterer foreløpig ikke flere arbeidsforhold i samme virksomhet for SVP"),
    FOR_TIDLIG_SOKNAD("FOR_TIDLIG_SOKNAD", "Venter pga for tidlig søknad"),
    GRADERING_FLERE_ARBEIDSFORHOLD("GRADERING_FLERE_ARBEIDSFORHOLD", "Behandlingen er satt på vent på grunn av mangel i løsningen. Det jobbes med å løse dette."),
    REFUSJON_3_MÅNEDER("REFUSJON_3_MÅNEDER", "Behandlingen er satt på vent på grunn av mangel i løsningen. Det jobbes med å løse dette."),
    SCANN("SCANN", "Venter på scanning"),
    ULIKE_STARTDATOER_SVP("ULIKE_STARTDATOER_SVP", "Behandlingen er satt på vent på grunn av mangel i løsningen. Det jobbes med å løse dette."),
    UTV_FRIST("UTV_FRIST", "Utvidet frist"),
    VENT_DØDFØDSEL_80P_DEKNINGSGRAD("VENT_DØDFØDSEL_80P_DEKNINGSGRAD", "Mangel i løsning for oppgitt 80% dekningsgrad med dødfødsel"),
    VENT_FEIL_ENDRINGSSØKNAD("VENT_FEIL_ENDRINGSSØKNAD", "Behandlingen er satt på vent på grunn av potensielt feil i endringssøknad"),
    VENT_GRADERING_UTEN_BEREGNINGSGRUNNLAG("VENT_GRADERING_UTEN_BEREGNINGSGRUNNLAG", "Mangel i løsning for oppgitt gradering der utbetaling ikke finnes"),
    VENT_INFOTRYGD("VENT_INFOTRYGD", "Venter på en ytelse i Infotrygd"),
    VENT_INNTEKT_RAPPORTERINGSFRIST("VENT_INNTEKT_RAPPORTERINGSFRIST", "Inntekt rapporteringsfrist"),
    VENT_MILITÆR_BG_UNDER_3G("VENT_MILITÆR_OG_BG_UNDER_3G", "Behandlingen er satt på vent på grunn av mangel i løsningen. Det jobbes med å løse dette."),
    VENT_OPDT_INNTEKTSMELDING("VENT_OPDT_INNTEKTSMELDING", "Venter på oppdatert inntektsmelding"),
    VENT_OPPTJENING_OPPLYSNINGER("VENT_OPPTJENING_OPPLYSNINGER", "Venter på opptjeningsopplysninger"),
    VENT_PÅ_NY_INNTEKTSMELDING_MED_GYLDIG_ARB_ID("VENT_PÅ_NY_INNTEKTSMELDING_MED_GYLDIG_ARB_ID", "Venter på ny inntektsmelding med arbeidsforholdId som stemmer med Aareg"),
    VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT("VENT_PÅ_SISTE_AAP_MELDEKORT", "Venter på siste meldekort for AAP eller dagpenger før første uttaksdag."),
    VENT_REGISTERINNHENTING("VENT_REGISTERINNHENTING", "Venter på registerinformasjon"),
    VENT_SØKNAD_SENDT_INFORMASJONSBREV("VENT_SØKNAD_SENDT_INFORMASJONSBREV", "Sendt informasjonsbrev venter søknad."),
    VENT_TIDLIGERE_BEHANDLING("VENT_TIDLIGERE_BEHANDLING", "Venter på iverksettelse av en tidligere behandling i denne saken"),
    VENT_ÅPEN_BEHANDLING("VENT_ÅPEN_BEHANDLING", "Søker eller den andre forelderen har en åpen behandling"),

    OPPD_ÅPEN_BEH("OPPD_ÅPEN_BEH", "Venter på oppdatering av åpen behandling"),
    VENT_DEKGRAD_REGEL("VENT_DEKGRAD_REGEL", "Venter på 80% dekningsgrad-regel"),
    VENT_ØKONOMI("VENT_ØKONOMI", "Venter på økonomiløsningen"),
    VENTELØNN_ELLER_MILITÆR_MED_FLERE_AKTIVITETER("VENTELØNN_ELLER_MILITÆR_MED_FLERE_AKTIVITETER", "Mangel i løsning for oppgitt ventelønn og/eller militær i kombinasjon med andre statuser"),
    VENT_BEREGNING_TILBAKE_I_TID("VENT_BEREGNING_TILBAKE_I_TID", "Endring i tilkjent ytelse bakover i tid. Dette håndteres ikke i løsningen enda."),
    AAP_DP_SISTE_10_MND_SVP("AAP_DP_SISTE_10_MND_SVP", "Behandlingen er satt på vent på grunn av mangel i løsningen. Det jobbes med å løse dette."),
    FL_SN_IKKE_STOTTET_FOR_SVP("FL_SN_IKKE_STOTTET_FOR_SVP", "Behandlingen er satt på vent på grunn av mangel i løsningen. Det jobbes med å løse dette."),
    VENT_MANGLENDE_ARBEIDSFORHOLD("VENT_MANGLENDE_ARBEIDSFORHOLD", "Sak settes på vent pga kommune- og fylkesammenslåing. Meld saken i Porten."),
    VENT_MANGLENDE_SYKEMELDING("VENT_MANGLENDE_SYKEMELDING", "Sak settes på vent pga manglende sykemelding ved sykepenger basert på dagpenger"),
    ;
    public static final String KODEVERK = "VENT_AARSAK";
    private static final Map<String, Venteårsak> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @JsonIgnore
    private String navn;

    private String kode;

    private Venteårsak(String kode) {
        this.kode = kode;
    }

    private Venteårsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Venteårsak fraKode(@JsonProperty(value = "kode") Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(Venteårsak.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Venteårsak: " + kode);
        }
        return ad;
    }

    public static Map<String, Venteårsak> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<Venteårsak, String> {
        @Override
        public String convertToDatabaseColumn(Venteårsak attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public Venteårsak convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
