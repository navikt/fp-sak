package no.nav.foreldrepenger.behandlingslager.uttak;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;


@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum ManuellBehandlingÅrsak implements Kodeverdi {

    UKJENT("-", "Ikke definert"),
    STØNADSKONTO_TOM("5001", "Stønadskonto tom for stønadsdager. Vurder bruk av annen stønadskonto eller avslå perioden."),
    UGYLDIG_STØNADSKONTO("5002", "Ikke gyldig grunn for uttak av denne stønadskontoen. Vurder bruk av annen stønadskonto eller avslå perioden."),
    BEGRUNNELSE_IKKE_GYLDIG("5003", "Ikke gyldig grunn for overføring av kvote. Vurder bruk av annen stønadskonto eller avslå perioden."),
    AKTIVITEKTSKRAVET_MÅ_SJEKKES_MANUELT("5004", "Kontroller mors krav til aktivitet."),
    MANGLENDE_SØKT("5005", "Manglende søkt periode. Fastsett hvilken stønadskonto perioden skal trekkes fra."),
    AVKLAR_ARBEID("5006", "Søker er i arbeid i perioden. Vurder konsekvens for arbeid i perioden."),
    ADOPSJON_IKKE_STØTTET("5007", "Støtte for automatisk behandling av adopsjon er ikke implementert i saksbehandlingsløsningen."),
    FORELDREPENGER_IKKE_IMPLEMENTERT("5008", "Foreldrepenger ikke implementert"),
    SØKER_HAR_IKKE_OMSORG("5009", "Søker har ikke omsorg for barnet. Vurder bruk av annen stønadskonto eller avslå perioden."),
    SØKNADSFRIST("5010", "For sent fremsatt søknad. Vurder om uttak i perioden er gyldig."),
    IKKE_GYLDIG_GRUNN_FOR_UTSETTELSE("5011", "Ikke gyldig grunn for utsettelse av perioden, avslå utsettelsen, og sett riktig stønadskonto som skal benyttes."),
    PERIODE_UAVKLART("5012", "Perioden er uavklart. Vurder trekkdager og sett utbetalingsgrad."),
    IKKE_SAMTYKKE("5013", "Ikke samtykke mellom foreldrene"),
    VURDER_SAMTIDIG_UTTAK("5014", "Vurder samtidig uttak av foreldrepenger."),
    FORELDREPENGER_FØR_FØDSEL_STARTER_FOR_TIDLIG("5015", "Ugyldig stønadskonto - Foreldrepenger før fødsel starter for tidlig eller slutter for sent"),
    VURDER_OVERFØRING("5016", "Vurder søknad om overføring av kvote."),
    UGYLDIG_STØNADSKONTO_FAR_SØKT_FPFF("5017", "Ugyldig stønadskonto - Far/medmor søkt om foreldrepenger før fødsel"),
    OPPHØR_INNGANGSVILKÅR("5018", "Opphør av foreldrepenger fordi inngangsvilkår ikke oppfylt, avslå stønadsperiode"),
    STEBARNSADOPSJON("5019", "Stebarnsadopsjon - sjekk uttak med tanke på aktivitetskravet"),
    SØKT_FOR_SENT("5021", "Søkt for sent"),
    OPPHOLD_STØRRE_ENN_TILGJENGELIGE_DAGER("5024", "Opphold større enn tilgjengelige dager"),
    IKKE_HELTIDSARBEID("5025", "Søker er ikke registrert med en heltidsstilling i Aa-registeret. Avklar om søker jobber 100 % og dermed har rett til utsettelse"),
    DØDSFALL("5026", "Vurder uttak med hensyn på dødsfall"),
    ADOPSJON_IKKE_STØTTET2("5098", "Adopsjon ikke implementert, må behandles manuelt"),
    FORELDREPENGER_KONTO_IKKE_STØTTET("5099", "Foreldrepenger ikke implementert, må behandles manuelt"),
    ;
    private static final Map<String, ManuellBehandlingÅrsak> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "MANUELL_BEHANDLING_AARSAK";

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

    ManuellBehandlingÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static ManuellBehandlingÅrsak fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent ManuellBehandlingÅrsak: " + kode);
        }
        return ad;
    }
    public static Map<String, ManuellBehandlingÅrsak> kodeMap() {
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
        return this.getKode();
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<ManuellBehandlingÅrsak, String> {
        @Override
        public String convertToDatabaseColumn(ManuellBehandlingÅrsak attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public ManuellBehandlingÅrsak convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
