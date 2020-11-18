package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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
public enum BehandlingÅrsakType implements Kodeverdi {

    // MANUELL OPPRETTING - GUI-anvendelse
    RE_FEIL_I_LOVANDVENDELSE("RE-LOV", "Feil lovanvendelse"),
    RE_FEIL_REGELVERKSFORSTÅELSE("RE-RGLF", "Feil regelverksforståelse"),
    RE_FEIL_ELLER_ENDRET_FAKTA("RE-FEFAKTA", "Feil eller endret fakta"),
    RE_FEIL_PROSESSUELL("RE-PRSSL", "Prosessuell feil"),
    RE_ANNET("RE-ANNET", "Annet"),

    RE_OPPLYSNINGER_OM_MEDLEMSKAP("RE-MDL", "Nye opplysninger om medlemskap"),
    RE_OPPLYSNINGER_OM_OPPTJENING("RE-OPTJ", "Nye opplysninger om opptjening"),
    RE_OPPLYSNINGER_OM_FORDELING("RE-FRDLING", "Nye opplysninger om uttak"),
    RE_OPPLYSNINGER_OM_INNTEKT("RE-INNTK", "Nye opplysninger om inntekt"),
    RE_OPPLYSNINGER_OM_FØDSEL("RE-FØDSEL", "Fødsel"),
    RE_OPPLYSNINGER_OM_DØD("RE-DØD", "Dødsfall"),
    RE_OPPLYSNINGER_OM_SØKERS_REL("RE-SRTB", "Nye opplysninger om søkers relasjon til barnet"),
    RE_OPPLYSNINGER_OM_SØKNAD_FRIST("RE-FRIST", "Nye opplysninger som kan påvirke vurderingen av søknadsfristen"),
    RE_OPPLYSNINGER_OM_BEREGNINGSGRUNNLAG("RE-BER-GRUN", "Nye opplysninger som kan påvirke beregningsgrunnlaget"),

    // KLAGE - Manuelt opprettet revurdering (obs: årsakene kan også bli satt på en automatisk opprettet revurdering)
    RE_KLAGE_UTEN_END_INNTEKT("RE-KLAG-U-INNTK", "Klage/ankebehandling uten endrede inntektsopplysninger"),
    RE_KLAGE_MED_END_INNTEKT("RE-KLAG-M-INNTK", "Klage/ankebehandling med endrede inntektsopplysninger"),
    ETTER_KLAGE("ETTER_KLAGE", "Ny behandling eller revurdering etter klage eller anke"),

    // Etterkontroll + funksjonell
    RE_MANGLER_FØDSEL("RE-MF", "Manglende informasjon om fødsel i folkeregisteret"),
    RE_MANGLER_FØDSEL_I_PERIODE("RE-MFIP", "Manglende informasjon om fødsel i folkeregisteret mellom uke 26 og 29"),
    RE_AVVIK_ANTALL_BARN("RE-AVAB", "Avvik i antall barn"),

    // Mottak
    RE_ENDRING_FRA_BRUKER("RE-END-FRA-BRUKER", "Endringssøknad fra bruker"),
    RE_ENDRET_INNTEKTSMELDING("RE-END-INNTEKTSMELD", "Mottatt ny inntektsmelding"),
    BERØRT_BEHANDLING("BERØRT-BEHANDLING", "Endring i den andre forelderens uttak"),

    // G-regulering
    RE_SATS_REGULERING("RE-SATS-REGULERING", "Regulering av grunnbeløp"),

    //For automatiske informasjonsbrev
    INFOBREV_BEHANDLING("INFOBREV_BEHANDLING", "Sende informasjonsbrev"),
    INFOBREV_OPPHOLD("INFOBREV_OPPHOLD", "Sende informasjonsbrev om opphold det ikke er søkt om"),

    //For å vurdere opphør av ytelse
    OPPHØR_YTELSE_NYTT_BARN("OPPHØR-NYTT-BARN", "Identifisert overlapp av ytelser"),

    // Hendelser
    RE_HENDELSE_FØDSEL("RE-HENDELSE-FØDSEL", "Melding om registrert fødsel i folkeregisteret"),
    RE_HENDELSE_DØD_FORELDER("RE-HENDELSE-DØD-F", "Melding om registrert død på forelder i folkeregisteret"),
    RE_HENDELSE_DØD_BARN("RE-HENDELSE-DØD-B", "Melding om registrert død på barn i folkeregisteret"),
    RE_HENDELSE_DØDFØDSEL("RE-HENDELSE-DØDFØD", "Melding om registrert dødfødsel i folkeregisteret"),


    // UTGÅTT. men ikke slett - BehandlingÅrsak-tabellen er rensket og ikke bruk disse som nye behandlingsårsaker!
    // Det ligger en hel del i historikk-innslag (inntil evt konvertert) og de brukes til vise tekst frontend.
    // OPPLYSNINGER_OM_YTELSER brukes til å lage nye historikkinnslag - de øvrige er historiske

    @Deprecated // Registeroppdatering. 49450 forekomster i HISTORIKKINNSLAG_FELT
    RE_OPPLYSNINGER_OM_YTELSER("RE-YTELSE", "Nye opplysninger om ytelse"),

    @Deprecated // Registeroppdatering. 1 forekomst i HISTORIKKINNSLAG_FELT
    RE_REGISTEROPPLYSNING("RE-REGISTEROPPL", "Nye registeropplysninger"),
    @Deprecated // Køing - i tillegg til aksjonspunkt. 532 forekomster i HISTORIKKINNSLAG_FELT
    KØET_BEHANDLING("KØET-BEHANDLING", "Søker eller den andre forelderen har en åpen behandling"),
    @Deprecated // Infotrygd hendelse-feed. 2 forekomster i HISTORIKKINNSLAG_FELT
    RE_TILSTØTENDE_YTELSE_INNVILGET("RE-TILST-YT-INNVIL", "Tilstøtende ytelse innvilget"),
    @Deprecated // Infotrygd hendelse-feed. 2 forekomster i HISTORIKKINNSLAG_FELT
    RE_TILSTØTENDE_YTELSE_OPPHØRT("RE-TILST-YT-OPPH", "Tilstøtende ytelse opphørt"),

    // La stå
    UDEFINERT("-", "Ikke definert"),

    ;

    public static final String KODEVERK = "BEHANDLING_AARSAK"; //$NON-NLS-1$

    private static final Map<String, BehandlingÅrsakType> KODER = new LinkedHashMap<>();

    @JsonIgnore
    private String navn;

    private String kode;

    private BehandlingÅrsakType(String kode) {
        this.kode = kode;
    }

    private BehandlingÅrsakType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static BehandlingÅrsakType fraKode(@JsonProperty(value = "kode") Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(BehandlingÅrsakType.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BehandlingÅrsakType: " + kode);
        }
        return ad;
    }

    public static Map<String, BehandlingÅrsakType> kodeMap() {
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

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<BehandlingÅrsakType, String> {
        @Override
        public String convertToDatabaseColumn(BehandlingÅrsakType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public BehandlingÅrsakType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

    public static Set<BehandlingÅrsakType> årsakerForAutomatiskRevurdering() {
        return Set.of(RE_MANGLER_FØDSEL, RE_MANGLER_FØDSEL_I_PERIODE, RE_AVVIK_ANTALL_BARN);
    }

    public static Set<BehandlingÅrsakType> årsakerForEtterkontroll() {
        return Set.of(RE_MANGLER_FØDSEL, RE_MANGLER_FØDSEL_I_PERIODE, RE_AVVIK_ANTALL_BARN);
    }

    public static Set<BehandlingÅrsakType> årsakerEtterKlageBehandling() {
        return Set.of(ETTER_KLAGE, RE_KLAGE_MED_END_INNTEKT, RE_KLAGE_UTEN_END_INNTEKT);
    }

    public static Set<BehandlingÅrsakType> årsakerRelatertTilDød() {
        return Set.of(RE_OPPLYSNINGER_OM_DØD, RE_HENDELSE_DØD_BARN, RE_HENDELSE_DØD_FORELDER, RE_HENDELSE_DØDFØDSEL);
    }
}
