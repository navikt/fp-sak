package no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum Venteårsak implements Kodeverdi {

    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke definert"),

    /*
     * Disse er i bruk i koden i fpsak, frontend, eller kalkulus.
     */
    AVV_DOK("AVV_DOK", "Avventer dokumentasjon"), // Frontend
    AVV_FODSEL("AVV_FODSEL", "Avventer fødsel"), // Frontend
    FOR_TIDLIG_SOKNAD("FOR_TIDLIG_SOKNAD", "Venter pga. for tidlig søknad"), // Frontend
    SCANN("SCANN", "Venter på skanning"),
    UTV_FRIST("UTV_FRIST", "Utvidet frist"), // Frontend
    VENT_PÅ_BRUKERTILBAKEMELDING("VENT_PÅ_BRUKERTILBAKEMELDING", "Venter på tilbakemelding fra bruker"), // Frontend
    VENT_UTLAND_TRYGD("VENT_UTLAND_TRYGD", "Venter på utenlandsk trygdemyndighet"), // Frontend
    VENT_INNTEKT_RAPPORTERINGSFRIST("VENT_INNTEKT_RAPPORTERINGSFRIST", "Inntekt rapporteringsfrist"), // Kalkulus
    VENT_MANGLENDE_SYKEMELDING("VENT_MANGLENDE_SYKEMELDING", "Venter på siste sykemelding for sykepenger basert på dagpenger"), // Kalkulus
    VENT_OPDT_INNTEKTSMELDING("VENT_OPDT_INNTEKTSMELDING", "Venter på inntektsmelding"), // Frontend
    VENT_OPPTJENING_OPPLYSNINGER("VENT_OPPTJENING_OPPLYSNINGER", "Venter på opptjeningsopplysninger"),
    VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT("VENT_PÅ_SISTE_AAP_MELDEKORT", "Venter på siste meldekort for AAP eller dagpenger før første uttaksdag."), // Kalkulus
    VENT_SØKNAD_SENDT_INFORMASJONSBREV("VENT_SØKNAD_SENDT_INFORMASJONSBREV", "Sendt informasjonsbrev, og venter på søknad."),
    VENT_ÅPEN_BEHANDLING("VENT_ÅPEN_BEHANDLING", "Søker eller den andre forelderen har en åpen behandling"),
    VENT_KABAL("VENT_KABAL", "Klage / anke behandles av Klageinstans i egen løsning"),

    /*
     * Disse er på run-off
     */
    ANKE_OVERSENDT_TIL_TRYGDERETTEN("ANKE_OVERSENDT_TIL_TRYGDERETTEN", "Venter på at saken blir behandlet hos Trygderetten"), // Frontend
    ANKE_VENTER_PAA_MERKNADER_FRA_BRUKER("ANKE_VENTER_PAA_MERKNADER_FRA_BRUKER", "Venter på merknader fra bruker"), // Frontend
    AVV_RESPONS_REVURDERING("AVV_RESPONS_REVURDERING", "Avventer respons på varsel om revurdering"), // Frontend

    /*
     * Disse er i bruk i koden til interne formål rundt historikkinnslag, brukes ikke for aksjosnpunkt - men finnes i gamle aksjonspunkt.
     */
    VENT_TIDLIGERE_BEHANDLING("VENT_TIDLIGERE_BEHANDLING", "Venter på iverksettelse av en tidligere behandling i denne saken"),

    /*
     * Disse er ikke lenger i bruk i kode, men må beholdes pga innhold i database
     */
    AAP_DP_SISTE_10_MND_SVP("AAP_DP_SISTE_10_MND_SVP", "Behandlingen er satt på vent på grunn av mangel i løsningen. Det jobbes med å løse dette."),
    AAP_DP_ENESTE_AKTIVITET_SVP("AAP_DP_ENESTE_AKTIVITET_SVP", "Bruker har ikke rett til svangerskapspenger når eneste aktivitet er AAP/DP"),
    DELVIS_TILRETTELEGGING_OG_REFUSJON_SVP("DELVIS_TILRETTELEGGING_OG_REFUSJON_SVP", "Behandlingen er satt på vent på grunn av mangel i løsningen. Det jobbes med å løse dette."),
    FLERE_ARBEIDSFORHOLD_SAMME_ORG_SVP("FLERE_ARBEIDSFORHOLD_SAMME_ORG_SVP", "Håndterer foreløpig ikke flere arbeidsforhold i samme virksomhet for SVP"),
    FL_SN_IKKE_STOTTET_FOR_SVP("FL_SN_IKKE_STOTTET_FOR_SVP", "Behandlingen er satt på vent på grunn av mangel i løsningen. Det jobbes med å løse dette."),
    GRADERING_FLERE_ARBEIDSFORHOLD("GRADERING_FLERE_ARBEIDSFORHOLD", "Behandlingen er satt på vent på grunn av mangel i løsningen. Det jobbes med å løse dette."),
    OPPD_ÅPEN_BEH("OPPD_ÅPEN_BEH", "Venter på oppdatering av åpen behandling"),
    REFUSJON_3_MÅNEDER("REFUSJON_3_MÅNEDER", "Behandlingen er satt på vent på grunn av mangel i løsningen. Det jobbes med å løse dette."),
    VENTELØNN_ELLER_MILITÆR_MED_FLERE_AKTIVITETER("VENTELØNN_ELLER_MILITÆR_MED_FLERE_AKTIVITETER", "Mangel i løsning for oppgitt ventelønn og/eller militær i kombinasjon med andre statuser"),
    VENT_BEREGNING_TILBAKE_I_TID("VENT_BEREGNING_TILBAKE_I_TID", "Endring i tilkjent ytelse bakover i tid. Dette håndteres ikke i løsningen enda."),
    VENT_DEKGRAD_REGEL("VENT_DEKGRAD_REGEL", "Venter på 80% dekningsgrad-regel"),
    VENT_DØDFØDSEL_80P_DEKNINGSGRAD("VENT_DØDFØDSEL_80P_DEKNINGSGRAD", "Mangel i løsning for oppgitt 80% dekningsgrad med dødfødsel"),
    VENT_FEIL_ENDRINGSSØKNAD("VENT_FEIL_ENDRINGSSØKNAD", "Behandlingen er satt på vent på grunn av potensielt feil i endringssøknad"),
    VENT_GRADERING_UTEN_BEREGNINGSGRUNNLAG("VENT_GRADERING_UTEN_BEREGNINGSGRUNNLAG", "Mangel i løsning for oppgitt gradering der utbetaling ikke finnes"),
    VENT_INFOTRYGD("VENT_INFOTRYGD", "Venter på en ytelse i Infotrygd"),
    VENT_MANGLENDE_ARBEIDSFORHOLD("VENT_MANGLENDE_ARBEIDSFORHOLD", "Sak settes på vent pga kommune- og fylkesammenslåing. Meld saken i Porten."),
    VENT_MILITÆR_BG_UNDER_3G("VENT_MILITÆR_OG_BG_UNDER_3G", "Behandlingen er satt på vent på grunn av mangel i løsningen. Det jobbes med å løse dette."),
    ULIKE_STARTDATOER_SVP("ULIKE_STARTDATOER_SVP", "Behandlingen er satt på vent på grunn av mangel i løsningen. Det jobbes med å løse dette."),
    VENT_LOVENDRING_8_41("VENT_LOVENDRING_8_41", "Venter på vedtak om lovendring vedrørende beregning av næring i kombinasjon med arbeid eller frilans"),
    VENT_PÅ_KORRIGERT_BESTEBEREGNING("VENT_PÅ_KORRIGERT_BESTEBEREGNING", "Besteberegningen er feil. Feilen må meldes og korrigeres."),
    VENT_PÅ_NY_INNTEKTSMELDING_MED_GYLDIG_ARB_ID("VENT_PÅ_NY_INNTEKTSMELDING_MED_GYLDIG_ARB_ID", "Venter på ny inntektsmelding med arbeidsforholdsID som stemmer med Aa-reg"),
    VENT_REGISTERINNHENTING("VENT_REGISTERINNHENTING", "Venter på registerinformasjon"),
    VENT_ØKONOMI("VENT_ØKONOMI", "Venter på økonomiløsningen"),

    ;
    private static final Map<String, Venteårsak> KODER = new LinkedHashMap<>();

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

    Venteårsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Venteårsak fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Venteårsak: " + kode);
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
