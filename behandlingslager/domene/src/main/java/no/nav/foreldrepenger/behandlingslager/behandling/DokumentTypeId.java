package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.MedOffisiellKode;

/*
 * Opprettet fra https://kodeverk-web.dev.intern.nav.no/kodeverksoversikt/kodeverk/DokumentTypeId-er
 *
 * DokumenttypeId er ute av arkivmålbildet og noe som fagsystemene selv bør håndtere vis a vis selvbetjening og dokprod
 * For arkiv er tittel og brevkode (navskjema) det som teller. Uvisst om Gosys bruker kodeverket en tid til - men ikke i grensesnitt
 *
 * Bruk:
 * - Søknader - gjenkjenne, lagre og en hel del annen logikk
 * - Inntektsmeldinger - gjenkjenne, lagre og en hel del annen logikk
 * - Klage - gjenkjenne, opprette behandling, etc
 * - Tilbakekreving - mindre relevant her
 * - Vedlegg: Visning av manglende vedlegg i selvbetjening/oversikt/ettersendelse + saksbehandling/opplysningsplikt (ManglendeVedlegg)
 *
 * Trenger man nye titler, så sjekk først om de finnes i kodeverket - se lenke over og bruk "offisiellKode" fra den
 * Hvis koden ikke finnes i kodeverk DokukumentTypeId-er, så kan du legge til fritt
 * - Dok av uttak/utsettelse/overføring - typisk sykdom og innleggelse - velg fra I000120...9
 * - Dok av aktivitetskrav - typisk arbeid/studier/etc - velg fra I000130...9
 * - Annet (fødsel, omsorg, opptjening, beregning) - velg fra I000140...9
 *
 * Spørringer for å finne forekomster i DB:
 * - select skjemanummer, count(1) from SOEKNAD_VEDLEGG group by skjemanummer;
 * - select type, count(*) from MOTTATT_DOKUMENT group by type;
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

    // Inntektsmelding
    INNTEKTSMELDING("INNTEKTSMELDING", "I000067", "Inntektsmelding"),

    // Klage + Tilbakekreving
    KLAGE_DOKUMENT("KLAGE_DOKUMENT", "I000027", "Klage/anke"),
    KLAGE_ETTERSENDELSE("I500027", "Ettersendelse til klage/anke"),
    TILBAKEKREVING_UTTALSELSE("I000114", "Uttalelse tilbakekreving"),
    TILBAKEBETALING_UTTALSELSE("I000119", "Uttalelse om tilbakebetaling"),

    // Vedlegg fra brukerdialog fødsel / omsorg
    DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL("DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL", "I000041", "Dokumentasjon av termindato (lev. kun av mor), fødsel eller dato for omsorgsovertakelse"),
    DOKUMENTASJON_AV_OMSORGSOVERTAKELSE("DOKUMENTASJON_AV_OMSORGSOVERTAKELSE", "I000042", "Dokumentasjon av dato for overtakelse av omsorg"),
    BEKREFTELSE_VENTET_FØDSELSDATO("BEKREFTELSE_VENTET_FØDSELSDATO", "I000062", "Bekreftelse på ventet fødselsdato"),
    FØDSELSATTEST("FØDSELSATTEST", "I000063", "Fødselsattest"),
    TERMINBEKREFTELSE("I000141", "Terminbekreftelse"),

    // Vedlegg fra brukerdialog sykdomsrelatert - uttak/aktivitet, utsettelse, overføring
    LEGEERKLÆRING("LEGEERKLÆRING", "I000023", "Legeerklæring"),
    DOK_INNLEGGELSE("DOK_INNLEGGELSE", "I000037", "Dokumentasjon av innleggelse i helseinstitusjon"),
    BESKRIVELSE_FUNKSJONSNEDSETTELSE("BESKRIVELSE_FUNKSJONSNEDSETTELSE", "I000045", "Beskrivelse av funksjonsnedsettelse"),
    MOR_INNLAGT("I000120", "Dokumentasjon på at mor er innlagt på sykehus"),
    MOR_SYK("I000121", "Dokumentasjon på at mor er syk"),
    FAR_INNLAGT("I000122", "Dokumentasjon på at far/medmor er innlagt på sykehus"),
    FAR_SYK("I000123", "Dokumentasjon på at far/medmor er syk"),
    BARN_INNLAGT("I000124", "Dokumentasjon på at barnet er innlagt på sykehus"),

    // Catchall
    DOK_MORS_UTDANNING_ARBEID_SYKDOM("DOK_MORS_UTDANNING_ARBEID_SYKDOM", "I000038", "Dokumentasjon av mors utdanning, arbeid eller sykdom"),

    // Vedlegg fra brukerdialog aktivitetskrav + utsettelse (sammenhengende uttak HV og Nav)
    DOK_FERIE("DOK_FERIE", "I000036", "Dokumentasjon av ferie"),
    DOK_ARBEIDSFORHOLD("DOK_ARBEIDSFORHOLD", "I000043", "Dokumentasjon av arbeidsforhold"),
    BEKREFTELSE_DELTAR_KVALIFISERINGSPROGRAM("BEKREFTELSE_DELTAR_KVALIFISERINGSPROGRAM", "I000051", "Bekreftelse på deltakelse i kvalifiseringsprogrammet"),
    BEKREFTELSE_FRA_STUDIESTED("BEKREFTELSE_FRA_STUDIESTED", "I000061", "Bekreftelse fra studiested/skole"),
    BEKREFTELSE_FRA_ARBEIDSGIVER("BEKREFTELSE_FRA_ARBEIDSGIVER", "I000065", "Bekreftelse fra arbeidsgiver"),
    I000112("I000112", "Dokumentasjon av deltakelse i introduksjonsprogrammet"),
    DOK_HV("DOK_HV", "I000116", "Bekreftelse på øvelse eller tjeneste i Forsvaret eller Sivilforsvaret"),
    DOK_NAV_TILTAK("DOK_NAV_TILTAK", "I000117", "Bekreftelse på tiltak i regi av Arbeids- og velferdsetaten"),
    MOR_ARBEID_STUDIE("I000130", "Dokumentasjon på at mor studerer og arbeider til sammen heltid"),
    MOR_STUDIE("I000131", "Dokumentasjon på at mor studerer på heltid"),
    MOR_ARBEID("I000132", "Dokumentasjon på at mor er i arbeid"),
    MOR_KVALIFISERINGSPROGRAM("I000133", "Dokumentasjon av mors deltakelse i kvalifiseringsprogrammet"),

    // Svangerskapspenger
    I000109("I000109", "Skjema for tilrettelegging og omplassering ved graviditet"),
    MEDISINSK_DOK("I000142", "Medisinsk dokumentasjon"),


    // Opptjening/beregning/etc
    INNTEKTSOPPLYSNING_SELVSTENDIG("INNTEKTSOPPLYSNING_SELVSTENDIG", "I000007", "Inntektsopplysninger om selvstendig næringsdrivende og/eller frilansere som skal ha foreldrepenger eller svangerskapspenger"),
    DOK_INNTEKT("DOK_INNTEKT", "I000016", "Dokumentasjon av inntekt"),
    INNTEKTSOPPLYSNINGER("INNTEKTSOPPLYSNINGER", "I000026", "Inntektsopplysninger for arbeidstaker som skal ha sykepenger, foreldrepenger, svangerskapspenger, pleie-/opplæringspenger"),
    RESULTATREGNSKAP("RESULTATREGNSKAP", "I000032", "Resultatregnskap"),
    DOK_MILITÆR_SIVIL_TJENESTE("DOK_MILITÆR_SIVIL_TJENESTE", "I000039", "Dokumentasjon av militær- eller siviltjeneste"),
    DOK_ETTERLØNN("DOK_ETTERLØNN", "I000044", "Dokumentasjon av etterlønn/sluttvederlag"),
    DOKUMENTASJON_INNTEKT("I000146", "Dokumentasjon på inntekt"),
    INNTEKTSOPPLYSNINGSSKJEMA("I000052", "Inntektsopplysningsskjema"),
    KOPI_SKATTEMELDING("KOPI_SKATTEMELDING", "I000066", "Kopi av likningsattest eller selvangivelse"),
    SKATTEMELDING("I000140", "Skattemelding"),

    // Medlemskap
    BEKREFTELSE_OPPHOLDSTILLATELSE("BEKREFTELSE_OPPHOLDSTILLATELSE", "I000055", "Bekreftelse på oppholdstillatelse"),
    DOK_OPPHOLD("I000143", "Dokumentasjon på oppholdstillatelse"),
    OPPHOLDSOPPLYSNINGER("OPPHOLDSOPPLYSNINGER", "I001000", "Oppholdsopplysninger"),

    // Rettighet, søknadsfrist + opplysningplikt mm
    I000110("I000110", "Dokumentasjon av aleneomsorg"),
    I000111("I000111", "Dokumentasjon av begrunnelse for hvorfor man søker tilbake i tid"),
    SEN_SØKNAD("I000118", "Begrunnelse for sen søknad"),


    // Ettersendelse forsider - bør unngås som hoveddokument
    ETTERSENDT_SØKNAD_SVANGERSKAPSPENGER_SELVSTENDIG("ETTERSENDT_SØKNAD_SVANGERSKAPSPENGER_SELVSTENDIG", "I500001",
        "Ettersendelse til søknad om svangerskapspenger til selvstendig næringsdrivende og frilanser"),
    ETTERSENDT_SØKNAD_FORELDREPENGER_ADOPSJON("ETTERSENDT_SØKNAD_FORELDREPENGER_ADOPSJON", "I500002",
        "Ettersendelse til søknad om foreldrepenger ved adopsjon"),
    ETTERSENDT_SØKNAD_ENGANGSSTØNAD_FØDSEL("ETTERSENDT_SØKNAD_ENGANGSSTØNAD_FØDSEL", "I500003",
        "Ettersendelse til søknad om engangsstønad ved fødsel"),
    ETTERSENDT_SØKNAD_ENGANGSSTØNAD_ADOPSJON("ETTERSENDT_SØKNAD_ENGANGSSTØNAD_ADOPSJON", "I500004",
        "Ettersendelse til søknad om engangsstønad ved adopsjon"),
    ETTERSENDT_SØKNAD_FORELDREPENGER_FØDSEL("ETTERSENDT_SØKNAD_FORELDREPENGER_FØDSEL", "I500005",
        "Ettersendelse til søknad om foreldrepenger ved fødsel"),
    ETTERSENDT_FLEKSIBELT_UTTAK_FORELDREPENGER("ETTERSENDT_FLEKSIBELT_UTTAK_FORELDREPENGER", "I500006",
        "Ettersendelse til utsettelse eller gradert uttak av foreldrepenger (fleksibelt uttak)"),
    ETTERSENDT_FORELDREPENGER_ENDRING_SØKNAD("ETTERSENDT_FORELDREPENGER_ENDRING_SØKNAD", "I500050",
        "Ettersendelse til søknad om endring av uttak av foreldrepenger eller overføring av kvote"),

    // Alt mulig annet
    BREV_UTLAND("BREV_UTLAND", "I000028", "Brev - utland"),
    ANNET_SKJEMA_UTLAND_IKKE_NAV("ANNET_SKJEMA_UTLAND_IKKE_NAV", "I000029", "Annet skjema (ikke NAV-skjema) - utland"),
    BREV("BREV", "I000048", "Brev"),
    DOK_OPPFØLGING("I000145", "Dokumentasjon på oppfølging i svangerskapet"),
    DOK_REISE("I000144", "Dokumentasjon på reiser til og fra Norge"),
    ANNET_SKJEMA_IKKE_NAV("ANNET_SKJEMA_IKKE_NAV", "I000049", "Annet skjema (ikke NAV-skjema)"),
    ANNET("ANNET", "I000060", "Annet"),

    UDEFINERT(STANDARDKODE_UDEFINERT, "", "Ikke definert"),
    ;

    public static final String KODEVERK = "DOKUMENT_TYPE_ID";

    private static final Map<String, DokumentTypeId> KODER = new LinkedHashMap<>();

    private String navn;

    private String offisiellKode;
    @JsonValue
    private String kode;

    DokumentTypeId(String offisiellKode, String navn) {
        this(offisiellKode, offisiellKode, navn);
    }

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

    private static final Set<DokumentTypeId> ANNET_DOK_TYPER = Set.of(ANNET, ANNET_SKJEMA_IKKE_NAV, ANNET_SKJEMA_UTLAND_IKKE_NAV, BREV, BREV_UTLAND);

    private static final Set<DokumentTypeId> ETTERSENDELSE_TYPER = Set.of(ETTERSENDT_SØKNAD_SVANGERSKAPSPENGER_SELVSTENDIG,
        ETTERSENDT_SØKNAD_ENGANGSSTØNAD_FØDSEL, ETTERSENDT_SØKNAD_ENGANGSSTØNAD_ADOPSJON,
        ETTERSENDT_SØKNAD_FORELDREPENGER_FØDSEL, ETTERSENDT_SØKNAD_FORELDREPENGER_ADOPSJON,
        ETTERSENDT_FORELDREPENGER_ENDRING_SØKNAD, ETTERSENDT_FLEKSIBELT_UTTAK_FORELDREPENGER);


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

    public boolean erAnnenDokType() {
        return ANNET_DOK_TYPER.contains(this);
    }

    public boolean erEttersendelseType() {
        return ETTERSENDELSE_TYPER.contains(this);
    }

    public static Set<DokumentTypeId> ekvivalenter(Set<DokumentTypeId> dokumentTypeId) {
        return dokumentTypeId.stream()
            .flatMap(d -> EKVIVALENTER.stream().filter(e -> e.contains(d)))
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    }

    private static final Set<Set<DokumentTypeId>> EKVIVALENTER = Set.of(
        Set.of(LEGEERKLÆRING, DOK_MORS_UTDANNING_ARBEID_SYKDOM, BESKRIVELSE_FUNKSJONSNEDSETTELSE, MOR_SYK, FAR_SYK),
        Set.of(DOK_INNLEGGELSE, BESKRIVELSE_FUNKSJONSNEDSETTELSE, MOR_INNLAGT, FAR_INNLAGT, BARN_INNLAGT),
        Set.of(DOK_ARBEIDSFORHOLD, BEKREFTELSE_FRA_ARBEIDSGIVER, MOR_ARBEID, MOR_ARBEID_STUDIE, DOK_MORS_UTDANNING_ARBEID_SYKDOM),
        Set.of(BEKREFTELSE_FRA_STUDIESTED, DOK_MORS_UTDANNING_ARBEID_SYKDOM, MOR_STUDIE, MOR_ARBEID_STUDIE),
        Set.of(DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL, BEKREFTELSE_VENTET_FØDSELSDATO, FØDSELSATTEST, TERMINBEKREFTELSE),
        Set.of(BEKREFTELSE_DELTAR_KVALIFISERINGSPROGRAM, MOR_KVALIFISERINGSPROGRAM),
        Set.of(I000111, SEN_SØKNAD),
        Set.of(SKATTEMELDING, KOPI_SKATTEMELDING)
    );



    // Ulike titler er brukt i selvbetjening, fordel, sak og kodeverk
    private static final Map<String, DokumentTypeId> ALT_TITLER = Map.ofEntries(
        Map.entry("Søknad om svangerskapspenger til selvstendig næringsdrivende og frilanser", SØKNAD_SVANGERSKAPSPENGER),
        Map.entry("Søknad om svangerskapspenger for selvstendig", SØKNAD_SVANGERSKAPSPENGER),
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
        Map.entry("Kopi av skattemelding", KOPI_SKATTEMELDING),
        Map.entry("Svar på varsel om tilbakebetaling", TILBAKEKREVING_UTTALSELSE),
        Map.entry("Klage", DokumentTypeId.KLAGE_DOKUMENT),
        Map.entry("Anke", DokumentTypeId.KLAGE_DOKUMENT)
    );
}
