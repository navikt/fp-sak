package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum HistorikkinnslagType implements Kodeverdi {

    //12.10.2020 Maltypen brukes ikke i frontend, kanskje i usynk

    // Mal Type 1
    BREV_SENT("BREV_SENT", "Brev sendt", HistorikkinnslagMal.MAL_TYPE_1),
    BREV_BESTILT("BREV_BESTILT", "Brev bestilt", HistorikkinnslagMal.MAL_TYPE_1),
    ARBEIDSGIVERPORTALEN("ARBEIDSGIVERPORTALEN", "Arbeidsgiverportalen", HistorikkinnslagMal.MAL_TYPE_1),
    BEH_STARTET_PÅ_NYTT("BEH_STARTET_PÅ_NYTT", "Behandling startet på nytt", HistorikkinnslagMal.MAL_TYPE_1),
    BEH_STARTET("BEH_STARTET", "Behandling startet", HistorikkinnslagMal.MAL_TYPE_1),
    BEH_OPPDATERT_NYE_OPPL("BEH_OPPDATERT_NYE_OPPL", "Behandlingen oppdatert med nye opplysninger", HistorikkinnslagMal.MAL_TYPE_1),
    BEH_MAN_GJEN("BEH_MAN_GJEN", "Gjenoppta behandling", HistorikkinnslagMal.MAL_TYPE_1),
    BEH_GJEN("BEH_GJEN", "Behandling gjenopptatt", HistorikkinnslagMal.MAL_TYPE_1),
    BEH_AVBRUTT_VUR("BEH_AVBRUTT_VUR", "Vurdering før vedtak", HistorikkinnslagMal.MAL_TYPE_1),
    ANKEBEH_STARTET("ANKEBEH_STARTET", "Anke mottatt", HistorikkinnslagMal.MAL_TYPE_1),
    VRS_REV_IKKE_SNDT("VRS_REV_IKKE_SNDT", "Varsel om revurdering ikke sendt", HistorikkinnslagMal.MAL_TYPE_1),
    VEDLEGG_MOTTATT("VEDLEGG_MOTTATT", "Vedlegg mottatt", HistorikkinnslagMal.MAL_TYPE_1),
    TERMINBEKREFTELSE_UGYLDIG("TERMINBEKREFTELSE_UGYLDIG", "Terminbekreftelsens utstedt dato er før 22. svangerskapsuke. Behandlingen fortsatt uten ny terminbekreftelse", HistorikkinnslagMal.MAL_TYPE_1),
    SPOLT_TILBAKE("SPOLT_TILBAKE", "Behandlingen er flyttet", HistorikkinnslagMal.MAL_TYPE_1),
    REVURD_OPPR("REVURD_OPPR", "Revurdering opprettet", HistorikkinnslagMal.MAL_TYPE_1),
    REGISTRER_PAPIRSØK("REGISTRER_PAPIRSØK", "Registrer papirsøknad", HistorikkinnslagMal.MAL_TYPE_1),
    NYE_REGOPPLYSNINGER("NYE_REGOPPLYSNINGER", "Nye registeropplysninger", HistorikkinnslagMal.MAL_TYPE_1),
    MIGRERT_FRA_INFOTRYGD_FJERNET("MIGRERT_FRA_INFOTRYGD_FJERNET", "Behandling gjelder ikke lenger flytting av sak fra Infotrygd", HistorikkinnslagMal.MAL_TYPE_1),
    MIGRERT_FRA_INFOTRYGD("MIGRERT_FRA_INFOTRYGD", "Behandling gjelder flytting av sak fra Infotrygd", HistorikkinnslagMal.MAL_TYPE_1),
    MANGELFULL_SØKNAD("MANGELFULL_SØKNAD", "Mangelfull søknad", HistorikkinnslagMal.MAL_TYPE_1),
    KLAGEBEH_STARTET("KLAGEBEH_STARTET", "Klage mottatt", HistorikkinnslagMal.MAL_TYPE_1),
    INNSYN_OPPR("INNSYN_OPPR", "Innsynsbehandling opprettet", HistorikkinnslagMal.MAL_TYPE_1),
    KØET_BEH_GJEN("KØET_BEH_GJEN", "Køet behandling er gjenopptatt", HistorikkinnslagMal.MAL_TYPE_1),
    ENDRET_DEKNINGSGRAD("ENDRET_DEKNINGSGRAD", "Dekningsgrad er endret", HistorikkinnslagMal.MAL_TYPE_1),
    OPPGAVE_VEDTAK("OPPGAVE_VEDTAK", "Oppgave før vedtak", HistorikkinnslagMal.MAL_TYPE_1),

    // Mal Type 2
    VEDTAK_FATTET("VEDTAK_FATTET", "Vedtak fattet", HistorikkinnslagMal.MAL_TYPE_2),
    UENDRET_UTFALL("UENDRET_UTFALL", "Uendret utfall", HistorikkinnslagMal.MAL_TYPE_2),
    TILBAKEKREVING_VIDEREBEHANDLING("TILBAKEKR_VIDEREBEHANDLING", "Metode for å håndtere tilbakekreving av feilutbetailng er valgt.", HistorikkinnslagMal.MAL_TYPE_2),
    REGISTRER_OM_VERGE("REGISTRER_OM_VERGE", "Registrering av opplysninger om verge/fullmektig", HistorikkinnslagMal.MAL_TYPE_2),
    FORSLAG_VEDTAK_UTEN_TOTRINN("FORSLAG_VEDTAK_UTEN_TOTRINN", "Vedtak foreslått", HistorikkinnslagMal.MAL_TYPE_2),
    FORSLAG_VEDTAK("FORSLAG_VEDTAK", "Vedtak foreslått og sendt til beslutter", HistorikkinnslagMal.MAL_TYPE_2),

    // Mal Type 3
    SAK_RETUR("SAK_RETUR", "Sak retur", HistorikkinnslagMal.MAL_TYPE_3),
    SAK_GODKJENT("SAK_GODKJENT", "Sak godkjent", HistorikkinnslagMal.MAL_TYPE_3),

    // Mal Type 4
    FJERNET_VERGE("FJERNET_VERGE", "Opplysninger om verge/fullmektig fjernet", HistorikkinnslagMal.MAL_TYPE_4),
    IVERKSETTELSE_VENT("IVERKSETTELSE_VENT", "Behandlingen venter på iverksettelse", HistorikkinnslagMal.MAL_TYPE_4),
    BEH_VENT("BEH_VENT", "Behandling på vent", HistorikkinnslagMal.MAL_TYPE_4),
    BEH_KØET("BEH_KØET", "Behandlingen er satt på vent", HistorikkinnslagMal.MAL_TYPE_4),
    AVBRUTT_BEH("AVBRUTT_BEH", "Behandling er henlagt", HistorikkinnslagMal.MAL_TYPE_4),

    // Mal Type 5
    UTTAK("UTTAK", "Behandlet soknadsperiode", HistorikkinnslagMal.MAL_TYPE_5),
    KLAGE_BEH_NK("KLAGE_BEH_NK", "Klagebehandling KA", HistorikkinnslagMal.MAL_TYPE_5),
    KLAGE_BEH_NFP("KLAGE_BEH_NFP", "Klagebehandling NFP", HistorikkinnslagMal.MAL_TYPE_5),
    FAKTA_ENDRET("FAKTA_ENDRET", "Fakta endret", HistorikkinnslagMal.MAL_TYPE_5),
    BYTT_ENHET("BYTT_ENHET", "Bytt enhet", HistorikkinnslagMal.MAL_TYPE_5),
    ANKE_BEH("ANKE_BEH", "Ankebehandling", HistorikkinnslagMal.MAL_TYPE_5),

    // Mal Type 6
    NY_INFO_FRA_TPS("NY_INFO_FRA_TPS", "Ny info fra TPS", HistorikkinnslagMal.MAL_TYPE_6),

    // Mal Type 7
    OVERSTYRT("OVERSTYRT", "Overstyrt", HistorikkinnslagMal.MAL_TYPE_7),

    // Mal Type 8
    OPPTJENING("OPPTJENING", "Behandlet opptjeningsperiode", HistorikkinnslagMal.MAL_TYPE_8),

    // Mal Type 9
    OVST_UTTAK_SPLITT("OVST_UTTAK_SPLITT", "Manuelt overstyring av uttak - splitting av periode", HistorikkinnslagMal.MAL_TYPE_9),
    FASTSATT_UTTAK_SPLITT("FASTSATT_UTTAK_SPLITT", "Manuelt fastsetting av uttak - splitting av periode", HistorikkinnslagMal.MAL_TYPE_9),

    // Mal Type 10
    FASTSATT_UTTAK("FASTSATT_UTTAK", "Manuelt fastsetting av uttak", HistorikkinnslagMal.MAL_TYPE_10),
    OVST_UTTAK("OVST_UTTAK", "Manuelt overstyring av uttak", HistorikkinnslagMal.MAL_TYPE_10),

    UDEFINERT("-", "Ikke definert", null),

    //Mal Type 11
    AVKLART_AKTIVITETSKRAV("AVKLART_AKTIVITETSKRAV", "Avklart aktivitetskrav", HistorikkinnslagMal.MAL_TYPE_11),
    ;

    @Transient
    private String mal;

    private static final Map<String, HistorikkinnslagType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "HISTORIKKINNSLAG_TYPE";

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

    HistorikkinnslagType(String kode, String navn, String mal) {
        this.kode = kode;
        this.navn = navn;
        this.mal = mal;
    }

    public static HistorikkinnslagType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent HistorikkinnslagType: " + kode);
        }
        return ad;
    }

    public static Map<String, HistorikkinnslagType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
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
    public String getKode() {
        return kode;
    }

    public String getMal() {
        return mal;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<HistorikkinnslagType, String> {
        @Override
        public String convertToDatabaseColumn(HistorikkinnslagType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public HistorikkinnslagType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
