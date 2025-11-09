package no.nav.foreldrepenger.behandlingslager.uttak.fp;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;


public enum ManuellBehandlingÅrsak implements Kodeverdi {

    UKJENT(STANDARDKODE_UDEFINERT, "Ikke definert"),
    STØNADSKONTO_TOM("5001", "Stønadskonto tom for stønadsdager. Vurder bruk av annen stønadskonto eller avslå perioden."),
    UGYLDIG_STØNADSKONTO("5002", "Ikke gyldig grunn for uttak av denne stønadskontoen. Vurder bruk av annen stønadskonto eller avslå perioden."),
    BEGRUNNELSE_IKKE_GYLDIG("5003", "Ikke gyldig grunn for overføring av kvote. Vurder bruk av annen stønadskonto eller avslå perioden."),
    AKTIVITEKTSKRAVET_MÅ_SJEKKES_MANUELT("5004", "Kontroller mors krav til aktivitet."),
    MANGLENDE_SØKT("5005", "Manglende søkt periode. Fastsett hvilken stønadskonto perioden skal trekkes fra."),
    AVKLAR_ARBEID("5006", "Søker er i arbeid i perioden. Vurder konsekvens for arbeid i perioden."),
    ADOPSJON_IKKE_STØTTET("5007", "Støtte for automatisk behandling av adopsjon er ikke implementert i saksbehandlingsløsningen."),
    SØKER_HAR_IKKE_OMSORG("5009", "Søker har ikke omsorg for barnet. Vurder bruk av annen stønadskonto eller avslå perioden."),
    SØKNADSFRIST("5010", "For sent fremsatt søknad. Vurder om uttak i perioden er gyldig."),
    IKKE_GYLDIG_GRUNN_FOR_UTSETTELSE("5011", "Ikke gyldig grunn for utsettelse av perioden, avslå utsettelsen, og sett riktig stønadskonto som skal benyttes."),
    PERIODE_UAVKLART("5012", "Perioden er uavklart. Vurder trekkdager og sett utbetalingsgrad."),
    VURDER_SAMTIDIG_UTTAK("5014", "Vurder samtidig uttak av foreldrepenger."),
    VURDER_OVERFØRING("5016", "Vurder søknad om overføring av kvote."),
    OPPHØR_INNGANGSVILKÅR("5018", "Opphør av foreldrepenger fordi inngangsvilkår ikke oppfylt, avslå stønadsperiode"),
    STEBARNSADOPSJON("5019", "Stebarnsadopsjon - sjekk uttak med tanke på aktivitetskravet"),
    OPPHOLD_STØRRE_ENN_TILGJENGELIGE_DAGER("5024", "Opphold større enn tilgjengelige dager"),
    IKKE_HELTIDSARBEID("5025", "Søker er ikke registrert med en heltidsstilling i Aa-registeret. Avklar om søker jobber 100 % og dermed har rett til utsettelse"),
    DØDSFALL("5026", "Vurder uttak med hensyn på dødsfall"),
    MOR_UFØR("5027", "Vurder fars/medmors rett til uttak på grunn av mors uføretrygd"),
    OVERLAPPENDE_PLEIEPENGER_MED_INNLEGGELSE("5028", "Innvilget pleiepenger med innleggelse, vurder riktig ytelse"),
    OVERLAPPENDE_PLEIEPENGER_UTEN_INNLEGGELSE("5029", "Innvilget pleiepenger uten innleggelse, vurder riktig ytelse"),
    FAR_SØKER_FØR_FØDSEL("5030", "Far/medmor søker før fødsel/omsorg"),
    VURDER_OM_UTSETTELSE("5031", "Vurder om det skal være utsettelse i perioden"),
    AKTIVITETSKRAV_DELVIS_ARBEID("5032", "Vurder utbetalingsgrad og trekkdager når mor er i delvis arbeid (under 75% arbeidsprosent)");

    private static final Map<String, ManuellBehandlingÅrsak> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "MANUELL_BEHANDLING_AARSAK";

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

    ManuellBehandlingÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }


    public static ManuellBehandlingÅrsak fraKode(String kode) {
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

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getKode() {
        return kode;
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
