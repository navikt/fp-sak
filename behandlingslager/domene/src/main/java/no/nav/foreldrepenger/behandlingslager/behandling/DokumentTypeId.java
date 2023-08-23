package no.nav.foreldrepenger.behandlingslager.behandling;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.MedOffisiellKode;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 * Opprettet fra https://kodeverk-web.nais.adeo.no/kodeverksoversikt/kodeverk/DokumentTypeId-er
 *
 * DokumenttypeId er ute av arkivmålbildet og noe som fagsystemene selv bør håndtere vis a vis selvbetjening og dokprod
 * Det er mulig at Gosys bruker kodeverket en tid til - men for arkiv er tittel og brevkode (navskjema) det som teller.
 *
 * Trenger man ny strukturerte dokument innen domenet Foreldrepenger - innfør en kode Fnnnnn og bli enig om tittel (evt bruk av journalpost-metadata)
 */

public enum DokumentTypeId implements Kodeverdi, MedOffisiellKode {

    // Søknader
    SØKNAD_SVANGERSKAPSPENGER("SØKNAD_SVANGERSKAPSPENGER", "I000001", "Søknad om svangerskapspenger"),
    SØKNAD_FORELDREPENGER_ADOPSJON("SØKNAD_FORELDREPENGER_ADOPSJON", "I000002", "Søknad om foreldrepenger ved adopsjon"),
    SØKNAD_ENGANGSSTØNAD_FØDSEL("SØKNAD_ENGANGSSTØNAD_FØDSEL", "I000003", "Søknad om engangsstønad ved fødsel"),
    SØKNAD_ENGANGSSTØNAD_ADOPSJON("SØKNAD_ENGANGSSTØNAD_ADOPSJON", "I000004", "Søknad om engangsstønad ved adopsjon"),
    SØKNAD_FORELDREPENGER_FØDSEL("SØKNAD_FORELDREPENGER_FØDSEL", "I000005", "Søknad om foreldrepenger ved fødsel"),
    FLEKSIBELT_UTTAK_FORELDREPENGER("FLEKSIBELT_UTTAK_FORELDREPENGER", "I000006", "Utsettelse eller gradert uttak av foreldrepenger (fleksibelt uttak)"),
    FORELDREPENGER_ENDRING_SØKNAD("FORELDREPENGER_ENDRING_SØKNAD", "I000050", "Søknad om endring av uttak av foreldrepenger eller overføring av kvote"),

    // Klage + Tilbakekreving
    KLAGE_DOKUMENT("KLAGE_DOKUMENT", "I000027", "Klage/anke"),
    KLAGE_ETTERSENDELSE("I500027", "I500027", "Ettersendelse til klage/anke"),
    TILBAKE_UTTALSELSE("I000114", "I000114", "Uttalelse tilbakekreving"),

    // Inntekt
    INNTEKTSOPPLYSNING_SELVSTENDIG("INNTEKTSOPPLYSNING_SELVSTENDIG", "I000007", "Inntektsopplysninger om selvstendig næringsdrivende og/eller frilansere som skal ha foreldrepenger eller svangerskapspenger"),
    INNTEKTSOPPLYSNINGER("INNTEKTSOPPLYSNINGER", "I000026", "Inntektsopplysninger for arbeidstaker som skal ha sykepenger, foreldrepenger, svangerskapspenger, pleie-/opplæringspenger"),
    RESULTATREGNSKAP("RESULTATREGNSKAP", "I000032", "Resultatregnskap"),
    INNTEKTSMELDING("INNTEKTSMELDING", "I000067", "Inntektsmelding"),

    // Aktiv bruk i logikk
    LEGEERKLÆRING("LEGEERKLÆRING", "I000023", "Legeerklæring"),
    DOK_INNLEGGELSE("DOK_INNLEGGELSE", "I000037", "Dokumentasjon av innleggelse i helseinstitusjon"),
    DOK_HV("DOK_HV", "I000116", "Bekreftelse på øvelse eller tjeneste i Forsvaret eller Sivilforsvaret"),
    DOK_NAV_TILTAK("DOK_NAV_TILTAK", "I000117", "Bekreftelse på tiltak i regi av Arbeids- og velferdsetaten"),

    // Vedlegg fra brukerdialog - brukes i opplysningsplikt (ManglendeVedlegg)
    DOK_MORS_UTDANNING_ARBEID_SYKDOM("DOK_MORS_UTDANNING_ARBEID_SYKDOM", "I000038", "Dokumentasjon av mors utdanning, arbeid eller sykdom"),
    DOK_MILITÆR_SIVIL_TJENESTE("DOK_MILITÆR_SIVIL_TJENESTE", "I000039", "Dokumentasjon av militær- eller siviltjeneste"),
    DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL("DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL", "I000041", "Dokumentasjon av termindato (lev. kun av mor), fødsel eller dato for omsorgsovertakelse"),
    DOKUMENTASJON_AV_OMSORGSOVERTAKELSE("DOKUMENTASJON_AV_OMSORGSOVERTAKELSE", "I000042", "Dokumentasjon av dato for overtakelse av omsorg"),
    DOK_ETTERLØNN("DOK_ETTERLØNN", "I000044", "Dokumentasjon av etterlønn/sluttvederlag"),
    BESKRIVELSE_FUNKSJONSNEDSETTELSE("BESKRIVELSE_FUNKSJONSNEDSETTELSE", "I000045", "Beskrivelse av funksjonsnedsettelse"),
    ANNET_SKJEMA_IKKE_NAV("ANNET_SKJEMA_IKKE_NAV", "I000049", "Annet skjema (ikke NAV-skjema)"),
    BEKREFTELSE_DELTAR_KVALIFISERINGSPROGRAM("BEKREFTELSE_DELTAR_KVALIFISERINGSPROGRAM", "I000051", "Bekreftelse på deltakelse i kvalifiseringsprogrammet"),
    BEKREFTELSE_FRA_STUDIESTED("BEKREFTELSE_FRA_STUDIESTED", "I000061", "Bekreftelse fra studiested/skole"),
    BEKREFTELSE_VENTET_FØDSELSDATO("BEKREFTELSE_VENTET_FØDSELSDATO", "I000062", "Bekreftelse på ventet fødselsdato"),
    FØDSELSATTEST("FØDSELSATTEST", "I000063", "Fødselsattest"),
    ELEVDOKUMENTASJON_LÆRESTED("ELEVDOKUMENTASJON_LÆRESTED", "I000064", "Elevdokumentasjon fra lærested"),
    BEKREFTELSE_FRA_ARBEIDSGIVER("BEKREFTELSE_FRA_ARBEIDSGIVER", "I000065", "Bekreftelse fra arbeidsgiver"),
    I000110("I000110", "I000110", "Dokumentasjon av aleneomsorg"),
    I000111("I000111", "I000111", "Dokumentasjon av begrunnelse for hvorfor man søker tilbake i tid"),
    I000112("I000112", "I000112", "Dokumentasjon av deltakelse i introduksjonsprogrammet"),

    // Tidligere selvbetjening - kan antagelig fjernes snart
    DOK_FERIE("DOK_FERIE", "I000036", "Dokumentasjon av ferie"),
    DOK_ASYL_DATO("DOK_ASYL_DATO", "I000040", "Dokumentasjon av dato for asyl"),
    DOK_ARBEIDSFORHOLD("DOK_ARBEIDSFORHOLD", "I000043", "Dokumentasjon av arbeidsforhold"),
    KVITTERING_DOKUMENTINNSENDING("KVITTERING_DOKUMENTINNSENDING", "I000046", "Kvittering dokumentinnsending"),
    BRUKEROPPLASTET_DOKUMENTASJON("BRUKEROPPLASTET_DOKUMENTASJON", "I000047", "Brukeropplastet dokumentasjon"),
    BEKREFTELSE_OPPHOLDSTILLATELSE("BEKREFTELSE_OPPHOLDSTILLATELSE", "I000055", "Bekreftelse på oppholdstillatelse"),
    KOPI_SKATTEMELDING("KOPI_SKATTEMELDING", "I000066", "Kopi av likningsattest eller selvangivelse"),
    I000107("I000107", "I000107", "Vurdering av arbeidsmulighet/sykmelding"),
    I000108("I000108", "I000108", "Opplysninger om muligheter og behov for tilrettelegging ved svangerskap"),
    I000109("I000109", "I000109", "Skjema for tilrettelegging og omplassering ved graviditet"),
    OPPHOLDSOPPLYSNINGER("OPPHOLDSOPPLYSNINGER", "I001000", "Oppholdsopplysninger"),

    // Alt mulig annet
    ANNET("ANNET", "I000060", "Annet"),

    UDEFINERT("-", "", "Ikke definert"),
    ;

    public static final String KODEVERK = "DOKUMENT_TYPE_ID";

    private static final Map<String, DokumentTypeId> KODER = new LinkedHashMap<>();

    private String navn;

    private String offisiellKode;
    @JsonValue
    private String kode;

    DokumentTypeId(String kode, String offisiellKode, String navn) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
    }

    public static DokumentTypeId fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent DokumentTypeId: " + kode);
        }
        return ad;
    }

    public static DokumentTypeId fraKodeEllerOffisiell(String kode) {
        if (kode == null) {
            return DokumentTypeId.UDEFINERT;
        }
        return KODER.getOrDefault(kode, ANNET);
    }

    public static Map<String, DokumentTypeId> kodeMap() {
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

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
            if (v.offisiellKode != null && KODER.get(v.offisiellKode) == null) {
                KODER.putIfAbsent(v.offisiellKode, v);
            }
        }
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<DokumentTypeId, String> {
        @Override
        public String convertToDatabaseColumn(DokumentTypeId attribute) {
            if (attribute == null) {
                return null;
            }
            return attribute.getKode();
            // TODO (jol): vurdere å konvertere til å lagre offisiellKode: 1) Varsle alle med spørringer, 2) gjøre formidling robust 3) Endre her og frontend,
            //  4) fpfordel: metrics ser på fordel sine tasks og grafana må endres hvis Inntektsmelding blir til I000067
            //  return attribute.getOffisiellKode() != null ? attribute.getOffisiellKode() : UDEFINERT.getKode();
        }

        @Override
        public DokumentTypeId convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKodeEllerOffisiell(dbData);
        }
    }

    public static DokumentTypeId finnForKodeverkEiersKode(String offisiellDokumentType) {
        return Optional.ofNullable(offisiellDokumentType).map(off -> KODER.getOrDefault(offisiellDokumentType, ANNET)).orElse(UDEFINERT);
    }

    public static DokumentTypeId finnForKodeverkEiersNavn(String navn) {
        if (navn == null)
            return UDEFINERT;
        return Stream.of(values()).filter(k -> Objects.equals(k.navn, navn)).findFirst()
            .orElseGet(() -> ALT_TITLER.getOrDefault(navn, ANNET));
    }

    private static final Set<DokumentTypeId> SØKNAD_TYPER = Set.of(SØKNAD_ENGANGSSTØNAD_FØDSEL, SØKNAD_FORELDREPENGER_FØDSEL,
        SØKNAD_ENGANGSSTØNAD_ADOPSJON, SØKNAD_FORELDREPENGER_ADOPSJON, SØKNAD_SVANGERSKAPSPENGER);

    private static final Set<DokumentTypeId> ENDRING_SØKNAD_TYPER = Set.of(FORELDREPENGER_ENDRING_SØKNAD, FLEKSIBELT_UTTAK_FORELDREPENGER);

    private static final Set<DokumentTypeId> ANDRE_SPESIAL_TYPER = Set.of(INNTEKTSMELDING, KLAGE_DOKUMENT);

    public static Set<DokumentTypeId> getSpesialTyperKoder() {
        Set<DokumentTypeId> typer = new LinkedHashSet<>(SØKNAD_TYPER);
        typer.addAll(ENDRING_SØKNAD_TYPER);
        typer.addAll(ANDRE_SPESIAL_TYPER);
        return Collections.unmodifiableSet(typer);
    }

    public static Set<DokumentTypeId> getSøknadTyper() {
        return SØKNAD_TYPER;
    }

    public static Set<DokumentTypeId> getEndringSøknadTyper() {
        return ENDRING_SØKNAD_TYPER;
    }

    public boolean erSøknadType() {
        return SØKNAD_TYPER.contains(this);
    }

    public boolean erEndringsSøknadType() {
        return ENDRING_SØKNAD_TYPER.contains(this);
    }

    public boolean erForeldrepengeSøknad() {
        return Set.of(SØKNAD_FORELDREPENGER_FØDSEL, SØKNAD_FORELDREPENGER_ADOPSJON,
            FORELDREPENGER_ENDRING_SØKNAD, FLEKSIBELT_UTTAK_FORELDREPENGER).contains(this);
    }

    public boolean erInntektsmelding() {
        return INNTEKTSMELDING.equals(this);
    }

    public static Set<DokumentTypeId> ekvivalenter(Set<DokumentTypeId> dokumentTypeId) {
        return dokumentTypeId.stream()
            .filter(EKVIVALENTER::containsKey)
            .map(EKVIVALENTER::get)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    }

    private static final Map<DokumentTypeId, Set<DokumentTypeId>> EKVIVALENTER = Map.ofEntries(
        Map.entry(LEGEERKLÆRING, Set.of(DOK_MORS_UTDANNING_ARBEID_SYKDOM)),
        Map.entry(DOK_MORS_UTDANNING_ARBEID_SYKDOM, Set.of(LEGEERKLÆRING))
    );



    // Ulike titler er brukt i selvbetjening, fordel, sak og kodeverk
    private static final Map<String, DokumentTypeId> ALT_TITLER = Map.ofEntries(
        Map.entry("Søknad om svangerskapspenger til selvstendig næringsdrivende og frilanser", SØKNAD_SVANGERSKAPSPENGER),
        Map.entry("Søknad om svangerskapspenger for selvstendig", SØKNAD_SVANGERSKAPSPENGER),
        Map.entry("Inntektsopplysningsskjema", INNTEKTSOPPLYSNINGER),
        Map.entry("Bekreftelse på avtalt ferie", DOK_FERIE),
        Map.entry("Mor er innlagt i helseinstitusjon", DOK_INNLEGGELSE),
        Map.entry("Mor er i arbeid, tar utdanning eller er for syk til å ta seg av barnet", DOK_MORS_UTDANNING_ARBEID_SYKDOM),
        Map.entry("Dokumentasjon av termindato, fødsel eller dato for omsorgsovertakelse", DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL),
        Map.entry("Tjenestebevis", DOK_MILITÆR_SIVIL_TJENESTE),
        Map.entry("Dokumentasjon av overtakelse av omsorg", DOKUMENTASJON_AV_OMSORGSOVERTAKELSE),
        Map.entry("Dokumentasjon av etterlønn eller sluttvederlag", DOK_ETTERLØNN),
        Map.entry("Beskrivelse/Dokumentasjon funksjonsnedsettelse", BESKRIVELSE_FUNKSJONSNEDSETTELSE),
        Map.entry("Mor deltar i kvalifiseringsprogrammet", BEKREFTELSE_DELTAR_KVALIFISERINGSPROGRAM),
        Map.entry("Mor tar utdanning på heltid", BEKREFTELSE_FRA_STUDIESTED),
        Map.entry("Terminbekreftelse", BEKREFTELSE_VENTET_FØDSELSDATO),
        Map.entry("Kopi av skattemelding", KOPI_SKATTEMELDING),
        Map.entry("Svar på varsel om tilbakebetaling", TILBAKE_UTTALSELSE),
        Map.entry("Klage", DokumentTypeId.KLAGE_DOKUMENT),
        Map.entry("Anke", DokumentTypeId.KLAGE_DOKUMENT)
    );
}
