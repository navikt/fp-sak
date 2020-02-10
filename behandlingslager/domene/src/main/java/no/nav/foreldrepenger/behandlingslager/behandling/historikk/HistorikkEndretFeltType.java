package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

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

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum HistorikkEndretFeltType implements Kodeverdi {

    UDEFINIERT("-", "Ikke definert"),
    ADOPSJONSVILKARET("ADOPSJONSVILKARET", "Adopsjon"),
    OPPTJENINGSVILKARET("OPPTJENINGSVILKARET", "Opptjeningsvilkåret"),
    ADOPTERER_ALENE("ADOPTERER_ALENE", "Adopterer alene"),
    AKTIVITET("AKTIVITET", "Aktivitet"),
    AKTIVITET_PERIODE("AKTIVITET_PERIODE", "Perioden med aktivitet"),
    ALENEOMSORG("ALENEOMSORG", "Aleneomsorg"),
    ANTALL_BARN("ANTALL_BARN", "Antall barn"),
    AVKLARSAKSOPPLYSNINGER("AVKLARSAKSOPPLYSNINGER", "Avklar saksopplysninger"),
    BEHANDLENDE_ENHET("BEHANDLENDE_ENHET", "Behandlende enhet"),
    BEHANDLING("BEHANDLING", "Behandling"),
    BRUK_ANTALL_I_SOKNAD("BRUK_ANTALL_I_SOKNAD", "Bruk antall fra søknad"),
    BRUK_ANTALL_I_TPS("BRUK_ANTALL_I_TPS", "Bruk antall fra folkeregisteret"),
    BRUK_ANTALL_I_VEDTAKET("BRUK_ANTALL_I_VEDTAKET", "Bruk antall fra vedtaket"),
    BRUTTO_NAERINGSINNTEKT("BRUTTO_NAERINGSINNTEKT", "Brutto næringsinntekt"),
    DOKUMENTASJON_FORELIGGER("DOKUMENTASJON_FORELIGGER", "Dokumentasjon foreligger"),
    EKTEFELLES_BARN("EKTEFELLES_BARN", "Ektefelles barn"),
    ENDRING_NAERING("ENDRING_NAERING", "Endring i næring"),
    ENDRING_TIDSBEGRENSET_ARBEIDSFORHOLD("ENDRING_TIDSBEGRENSET_ARBEIDSFORHOLD", "Endring tidsbegrenset arbeidsforhold"),
    ER_SOKER_BOSATT_I_NORGE("ER_SOKER_BOSATT_I_NORGE", "Er søker bosatt i Norge?"),
    FODSELSVILKARET("FODSELSVILKARET", "Fødsel"),
    FODSELSDATO("FODSELSDATO", "Fødselsdato"),
    FORDELING_FOR_ANDEL("FORDELING_FOR_ANDEL", "Fordeling for arbeidsforhold"),
    FORDELING_FOR_NY_ANDEL("FORDELING_FOR_NY_ANDEL", "Ny andel med fordeling"),
    FORDELING_ETTER_BESTEBEREGNING("FORDELING_ETTER_BESTEBEREGNING", "Fordeling etter besteberegning"),
    FORELDREANSVARSVILKARET("FORELDREANSVARSVILKARET", "Foreldreansvar"),
    FRILANS_INNTEKT("FRILANS_INNTEKT", "Frilans inntekt"),
    FRILANSVIRKSOMHET("FRILANSVIRKSOMHET", "Frilansvirksomhet"),
    GYLDIG_MEDLEM_FOLKETRYGDEN("GYLDIG_MEDLEM_FOLKETRYGDEN", "Gyldig medlem i folketrygden"),
    INNTEKT_FRA_ARBEIDSFORHOLD("INNTEKT_FRA_ARBEIDSFORHOLD", "Inntekt fra arbeidsforhold"),
    LØNNSENDRING_I_PERIODEN("LØNNSENDRING_I_PERIODEN", "Lønnsendring i beregningsperioden"),
    MANN_ADOPTERER("MANN_ADOPTERER", "Mann adopterer"),
    MOTTAR_YTELSE_ARBEID("MOTTAR_YTELSE_ARBEID", "Mottar søker ytelse for arbeid i {value}"),
    MOTTAR_YTELSE_FRILANS("MOTTAR_YTELSE_FRILANS", "Mottar søker ytelse for frilansaktiviteten"),
    MOTTATT_DATO("MOTTATT_DATO", "Mottatt dato"),
    OMSORG("OMSORG", "Omsorg"),
    OMSORGSOVERTAKELSESDATO("OMSORGSOVERTAKELSESDATO", "Omsorgsovertakelsesdato"),
    OMSORGSVILKAR("OMSORGSVILKAR", "Foreldreansvar"),
    IKKE_OMSORG_PERIODEN("IKKE_OMSORG_PERIODEN", "Perioden Søker har ikke omsorg for barnet"),
    INNTEKTSKATEGORI_FOR_ANDEL("INNTEKTSKATEGORI_FOR_ANDEL", "Inntektskategori for andel"),
    OPPHOLDSRETT_EOS("OPPHOLDSRETT_EOS", "Bruker har oppholdsrett i EØS"),
    OPPHOLDSRETT_IKKE_EOS("OPPHOLDSRETT_IKKE_EOS", "Bruker har ikke oppholdsrett i EØS"),
    OVERSTYRT_BEREGNING("OVERSTYRT_BEREGNING", "Overstyrt beregning"),
    OVERSTYRT_VURDERING("OVERSTYRT_VURDERING", "Overstyrt vurdering"),
    SELVSTENDIG_NÆRINGSDRIVENDE("SELVSTENDIG_NAERINGSDRIVENDE", "Selvstendig næringsdrivende"),
    SOKERSOPPLYSNINGSPLIKT("SOKERSOPPLYSNINGSPLIKT", "Søkers opplysningsplikt"),
    SVANGERSKAPSPENGERVILKÅRET("SVANGERSKAPSPENGERVILKÅRET", "Svangerskapsvilkåret"),
    SOKNADSFRIST("SOKNADSFRIST", "Søknadsfrist"),
    SOKNADSFRISTVILKARET("SOKNADSFRISTVILKARET", "Søknadsfristvilkåret"),
    STARTDATO_FRA_SOKNAD("STARTDATO_FRA_SOKNAD", "Startdato fra søknad"),
    TERMINDATO("TERMINDATO", "Termindato"),
    UTSTEDTDATO("UTSTEDTDATO", "Utstedtdato"),
    VILKAR_SOM_ANVENDES("VILKAR_SOM_ANVENDES", "Vilkår som anvendes"),
    FASTSETT_RESULTAT_PERIODEN("FASTSETT_RESULTAT_PERIODEN", "Fastsett resultat for perioden"),
    AVKLART_PERIODE("AVKLART_PERIODE", "Avklart periode"),
    ANDEL_ARBEID("ANDEL_ARBEID", "Andel i arbeid"),
    UTTAK_TREKKDAGER("UTTAK_TREKKDAGER", "Trekkdager"),
    UTTAK_STØNADSKONTOTYPE("UTTAK_STØNADSKONTOTYPE", "Stønadskontotype"),
    UTTAK_PERIODE_RESULTAT_TYPE("UTTAK_PERIODE_RESULTAT_TYPE", "Resultattype for periode"),
    UTTAK_PROSENT_UTBETALING("UTTAK_PROSENT_UTBETALING", "Utbetalingsgrad"),
    UTTAK_SAMTIDIG_UTTAK("UTTAK_SAMTIDIG_UTTAK", "Samtidig uttak"),
    UTTAK_TREKKDAGER_FLERBARN_KVOTE("UTTAK_TREKKDAGER_FLERBARN_KVOTE", "Trekkdager flerbarn kvote"),
    UTTAK_PERIODE_RESULTAT_ÅRSAK("UTTAK_PERIODE_RESULTAT_ÅRSAK", "Resultat årsak"),
    UTTAK_GRADERING_ARBEIDSFORHOLD("UTTAK_GRADERING_ARBEIDSFORHOLD", "Gradering av arbeidsforhold"),
    UTTAK_GRADERING_AVSLAG_ÅRSAK("UTTAK_GRADERING_AVSLAG_ÅRSAK", "Årsak avslag gradering"),
    UTTAK_SPLITT_TIDSPERIODE("UTTAK_SPLITT_TIDSPERIODE", "Resulterende periode ved splitting"),
    SYKDOM("SYKDOM", "Sykdom"),
    ARBEIDSFORHOLD("ARBEIDSFORHOLD", "Arbeidsforhold"),
    NY_FORDELING("NY_FORDELING", "Ny fordeling for"),
    NY_AKTIVITET("NY_AKTIVITET", "Det er lagt til ny aktivitet for"),
    NYTT_REFUSJONSKRAV("NYTT_REFUSJONSKRAV", "Nytt refusjonskrav"),
    INNTEKT("INNTEKT", "Inntekt"),
    INNTEKTSKATEGORI("INNTEKTSKATEGORI", "Inntektskategori"),
    NAVN("NAVN", "Navn"),
    FNR("FNR", "Fødselsnummer"),
    PERIODE_FOM("PERIODE_FOM", "Periode f.o.m."),
    PERIODE_TOM("PERIODE_TOM", "Periode t.o.m."),
    MANDAT("MANDAT", "Mandat"),
    KONTAKTPERSON("KONTAKTPERSON", "Kontaktperson"),
    BRUKER_TVUNGEN("BRUKER_TVUNGEN", "Bruker er under tvungen forvaltning"),
    TYPE_VERGE("TYPE_VERGE", "Type verge"),
    DAGPENGER_INNTEKT("DAGPENGER_INNTEKT", "Dagpenger"),
    KLAGE_RESULTAT_NFP("KLAGE_RESULTAT_NFP", "Resultat"),
    KLAGE_RESULTAT_KA("KLAGE_RESULTAT_KA", "Ytelsesvedtak"),
    KLAGE_OMGJØR_ÅRSAK("KLAGE_OMGJØR_ÅRSAK", "Årsak til omgjøring"),
    ER_KLAGER_PART("ER_KLAGER_PART", "Er klagefrist overholdt"),
    ER_KLAGE_KONKRET("ER_KLAGE_KONKRET", "Er klagefrist overholdt"),
    ER_KLAGEFRIST_OVERHOLDT("ER_KLAGEFRIST_OVERHOLDT", "Er klagefrist overholdt"),
    ER_KLAGEN_SIGNERT("ER_KLAGEN_SIGNERT", "Er klagefrist overholdt"),
    PA_KLAGD_BEHANDLINGID("PA_KLAGD_BEHANDLINGID", "Påklagd behandlingId"),
    ANKE_RESULTAT("ANKE_RESULTAT", "Anke resultat"),
    ANKE_OMGJØR_ÅRSAK("ANKE_OMGJØR_ÅRSAK", "Årsak til omgjøring"),
    ER_ANKER_IKKE_PART("ER_ANKER_IKKE_PART", "Angir om anker ikke er part i saken."),
    ER_ANKE_IKKE_KONKRET("ER_ANKE_IKKE_KONKRET", "Er anke ikke konkret."),
    ER_ANKEFRIST_IKKE_OVERHOLDT("ER_ANKEFRIST_IKKE_OVERHOLDT", "Er ankefrist ikke overholdt"),
    ER_ANKEN_IKKE_SIGNERT("ER_ANKEN_IKKE_SIGNERT", "er anken ikke signert."),
    PA_ANKET_BEHANDLINGID("PA_ANKET_BEHANDLINGID", "på anket behandlingsId."),
    VURDER_ETTERLØNN_SLUTTPAKKE("VURDER_ETTERLØNN_SLUTTPAKKE", "Har søker inntekt fra etterlønn eller sluttpakke"),
    FASTSETT_ETTERLØNN_SLUTTPAKKE("FASTSETT_ETTERLØNN_SLUTTPAKKE", "Fastsett søkers månedsinntekt fra etterlønn eller sluttpakke"),
    ER_VILKÅRENE_TILBAKEKREVING_OPPFYLT("ER_VILKÅRENE_TILBAKEKREVING_OPPFYLT", "Er vilkårene for tilbakekreving oppfylt"),
    ER_SÆRLIGE_GRUNNER_TIL_REDUKSJON("ER_SÆRLIGE_GRUNNER_TIL_REDUKSJON", "Er det særlige grunner til reduksjon"),
    FASTSETT_VIDERE_BEHANDLING("FASTSETT_VIDERE_BEHANDLING", "Fastsett videre behandling"),
    RETT_TIL_FORELDREPENGER("RETT_TIL_FORELDREPENGER", "Rett til foreldrepenger"),
    VURDER_GRADERING_PÅ_ANDEL_UTEN_BG("VURDER_GRADERING_PÅ_ANDEL_UTEN_BG", "Vurder om søker har gradering på en andel uten beregningsgrunnlag"),
    DEKNINGSGRAD("DEKNINGSGRAD", "Dekningsgrad"),
    TILBAKETREKK("TILBAKETREKK", "Tilbaketrekk"),
    UTLAND("UTLAND", "Utland"),
    HEL_TILRETTELEGGING_FOM("HEL_TILRETTELEGGING_FOM", "Hel tilrettelegging FOM"),
    DELVIS_TILRETTELEGGING_FOM("DELVIS_TILRETTELEGGING_FOM", "Delvis tilrettelegging FOM"),
    STILLINGSPROSENT("STILLINGSPROSENT", "Stillingsprosent"),
    SLUTTE_ARBEID_FOM("SLUTTE_ARBEID_FOM", "Slutte arbeid FOM"),
    TILRETTELEGGING_BEHOV_FOM("TILRETTELEGGING_BEHOV_FOM", "Tilrettelegging behov FOM"),
    FARESIGNALER("FARESIGNALER", "Faresignaler"),
    MILITÆR_ELLER_SIVIL("MILITÆR_ELLER_SIVIL", "Militær- eller siviltjeneste"),
    NY_REFUSJONSFRIST("NY_REFUSJONSFRIST", "Ny refusjonsfrist"),
    ORGANISASJONSNUMMER("ORGANISASJONSNUMMER", "Organisasjonsnummer"),
    ARBEIDSFORHOLD_BEKREFTET_TOM_DATO("ARBEIDSFORHOLD_BEKREFTET_TOM_DATO", "Til og med dato fastsatt av saksbehandler"),
    ANKE_AVVIST_ÅRSAK("ANKE_AVVIST_ÅRSAK", "Årsak til avvist anke")
    ;

    private static final Map<String, HistorikkEndretFeltType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "HISTORIKK_ENDRET_FELT_TYPE";

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

    private HistorikkEndretFeltType(String kode) {
        this.kode = kode;
    }

    private HistorikkEndretFeltType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static HistorikkEndretFeltType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent HistorikkEndretFeltType: " + kode);
        }
        return ad;
    }

    public static Map<String, HistorikkEndretFeltType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }
    
    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<HistorikkEndretFeltType, String> {
        @Override
        public String convertToDatabaseColumn(HistorikkEndretFeltType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public HistorikkEndretFeltType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
