package no.nav.foreldrepenger.domene.iay.modell.kodeverk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
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
public enum PensjonTrygdType implements Kodeverdi, YtelseType {

    UDEFINERT("-", "Undefined", null),
    ALDERSPENSJON("ALDERSPENSJON", "Alderspensjon", "alderspensjon"),
    ANNET("ANNET", "Annet", "annet"),
    AFP("AFP", "Avtalefestet pensjon", "avtalefestetPensjon"),
    BARNEPENSJON("BARNEPENSJON", "Barnepensjon", "barnepensjon"),
    BARNEPENSJON_ANDRE("BARNEPENSJON_ANDRE", "Barnepensjon fra andre enn folketrygden", "barnepensjonFraAndreEnnFolketrygden"),
    BIL("BIL", "Bil", "bil"),
    BOLIG("BOLIG", "Bolig", "bolig"),
    EKTEFELLE("EKTEFELLE", "Ektefelletillegg", "ektefelletillegg"),
    ELEKTRONISK_KOMMUNIKASJON("ELEKTRONISK_KOMMUNIKASJON", "Elektronisk kommunikasjon", "elektroniskKommunikasjon"),
    INNSKUDDS_ENGANGS("INNSKUDDS_ENGANGS", "Engangsutbetaling innskuddspensjon", "engangsutbetalingInnskuddspensjon"),
    ETTERLATTE_PENSJON("ETTERLATTE_PENSJON", "Etterlatte pensjon", "etterlattepensjon"),
    ETTERLØNN("ETTERLØNN", "Etterlønn", "etterloenn"),
    ETTERLØNN_OG_ETTERPENSJON("ETTERLØNN_OG_ETTERPENSJON", "Etterlønn og etterpensjon", "etterloennOgEtterpensjon"),
    FØDERÅD("FØDERÅD", "Føderåd", "foederaad"),
    INTRODUKSJONSSTØNAD("INTRODUKSJONSSTØNAD", "Introduksjonsstønad", "introduksjonsstoenad"),
    IPA_IPS_BARNEPENSJON("IPA_IPS_BARNEPENSJON", "Ipa eller ips barnepensjon", "ipaEllerIpsBarnepensjon"),
    IPA_IPS_ENGANGSUTBETALING("IPA_IPS_ENGANGSUTBETALING", "Ipa eller ips engangsutbetaling", "ipaEllerIpsEngangsutbetaling"),
    IPA_IPS_PERIODISKE("IPA_IPS_PERIODISKE", "Ipa eller ips periodiske ytelser", "ipaEllerIpsPeriodiskeYtelser"),
    IPA_IPS_UFØRE("IPA_IPS_UFØRE", "Ipa eller ips uførepensjon", "ipaEllerIpsUfoerepensjon"),
    KRIGSPENSJON("KRIGSPENSJON", "Krigspensjon", "krigspensjon"),
    KVALIFISERINGSSTØNAD("KVALIFISERINGSSTØNAD", "Kvalifiseringsstønad", "kvalifiseringstoenad"),
    NY_AFP("NY_AFP", "Ny avtalefestet pensjon privat sektor", "nyAvtalefestetPensjonPrivatSektor"),
    NYE_LIVRENTER("NYE_LIVRENTER", "Nye livrenter i arbeidsforhold og livrenter fortsettelsesforsikringer",
            "nyeLivrenterIArbeidsforholdOgLivrenterFortsettelsesforsikringer"),
    OVERGANGSSTØNAD_ENSLIG("OVERGANGSSTØNAD_ENSLIG", "Overgangsstønad til enslig mor eller far",
            "overgangsstoenadTilEnsligMorEllerFarSomBegynteAaLoepe31Mars2014EllerTidligere"),
    OVERGANGSSTØNAD_EKTEFELLE("OVERGANGSSTØNAD_EKTEFELLE", "Overgangsstønad til gjenlevende ektefelle", "overgangsstoenadTilGjenlevendeEktefelle"),
    PENSJON_DØDSMÅNED("PENSJON_DØDSMÅNED", "Pensjon i dødsmåned", "pensjonIDoedsmaaneden"),
    LIVRENTER("LIVRENTER", "Pensjon og livrenter i arbeidsforhold", "pensjonOgLivrenterIArbeidsforhold"),
    RENTEFORDEL_LÅN("RENTEFORDEL_LÅN", "Rentefordel lån", "rentefordelLaan"),
    SUPPLERENDE_STØNAD("SUPPLERENDE_STØNAD", "Supplerende stønad til person med kort botid i Norge", "supplerendeStoenadTilPersonKortBotidNorge"),
    UFØREPENSJON("UFØREPENSJON", "Uførepensjon", "ufoerepensjon"),
    UFØREPENSJON_ANDRE("UFØREPENSJON_ANDRE", "Uførepensjon fra andre enn folketrygden", "ufoerepensjonFraAndreEnnFolketrygden"),
    UFØREPENSJON_ANDRE_ETTEROPPGJØR("UFØREPENSJON_ANDRE_ETTEROPPGJØR", "Uførepensjon etteroppgjør fra andre enn folketrygden",
            "ufoereytelseEtteroppgjoerFraAndreEnnFolketrygden"),
    UNDERHOLDNINGSBIDRAG("UNDERHOLDNINGSBIDRAG", "Underholdningsbidrag tidligere ektefelle", "underholdsbidragTilTidligereEktefelle"),
    ;

    private static final Map<String, PensjonTrygdType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "PENSJON_TRYGD_BESKRIVELSE";

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
    @JsonIgnore
    private String offisiellKode;

    private PensjonTrygdType(String kode) {
        this.kode = kode;
    }

    private PensjonTrygdType(String kode, String navn, String offisiellKode) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
    }

    @JsonCreator
    public static PensjonTrygdType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent PensjonTrygdType: " + kode);
        }
        return ad;
    }

    public static Map<String, PensjonTrygdType> kodeMap() {
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
        return offisiellKode;
    }

}
