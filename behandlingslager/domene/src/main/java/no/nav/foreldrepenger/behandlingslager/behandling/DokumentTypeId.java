package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.AttributeConverter;
import javax.persistence.Column;
import javax.persistence.Converter;
import javax.persistence.Embeddable;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.kodeverk.arkiv.DokumentType;

/**
 * DokumentTypeId er et kodeverk som forvaltes av Kodeverkforvaltning. Det er et subsett av kodeverket DokumentType, mer spesifikt alle
 * inngående dokumenttyper.
 *
 */
@Embeddable
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class DokumentTypeId implements DokumentType {

    private static final Map<String, DokumentTypeId> KODER = new LinkedHashMap<>();

    public static final DokumentTypeId SØKNAD_ENGANGSSTØNAD_FØDSEL = new DokumentTypeId("SØKNAD_ENGANGSSTØNAD_FØDSEL", "I000003");
    public static final DokumentTypeId SØKNAD_ENGANGSSTØNAD_ADOPSJON = new DokumentTypeId("SØKNAD_ENGANGSSTØNAD_ADOPSJON", "I000004");
    public static final DokumentTypeId ETTERSENDT_SØKNAD_ENGANGSSTØNAD_FØDSEL = new DokumentTypeId("ETTERSENDT_SØKNAD_ENGANGSSTØNAD_FØDSEL", "I500003");
    public static final DokumentTypeId ETTERSENDT_SØKNAD_ENGANGSSTØNAD_ADOPSJON = new DokumentTypeId("ETTERSENDT_SØKNAD_ENGANGSSTØNAD_ADOPSJON", "I500004");
    public static final DokumentTypeId SØKNAD_FORELDREPENGER_FØDSEL = new DokumentTypeId("SØKNAD_FORELDREPENGER_FØDSEL", "I000005");
    public static final DokumentTypeId SØKNAD_FORELDREPENGER_ADOPSJON = new DokumentTypeId("SØKNAD_FORELDREPENGER_ADOPSJON", "I000002");
    public static final DokumentTypeId FORELDREPENGER_ENDRING_SØKNAD = new DokumentTypeId("FORELDREPENGER_ENDRING_SØKNAD", "I000050");
    public static final DokumentTypeId FLEKSIBELT_UTTAK_FORELDREPENGER = new DokumentTypeId("FLEKSIBELT_UTTAK_FORELDREPENGER", "I000006");
    public static final DokumentTypeId ETTERSENDT_SØKNAD_FORELDREPENGER_ADOPSJON = new DokumentTypeId("ETTERSENDT_SØKNAD_FORELDREPENGER_ADOPSJON", "I500002");
    public static final DokumentTypeId ETTERSENDT_SØKNAD_FORELDREPENGER_FØDSEL = new DokumentTypeId("ETTERSENDT_SØKNAD_FORELDREPENGER_FØDSEL", "I500005");
    public static final DokumentTypeId ETTERSENDT_FORELDREPENGER_ENDRING_SØKNAD = new DokumentTypeId("ETTERSENDT_FORELDREPENGER_ENDRING_SØKNAD", "I500050");
    public static final DokumentTypeId SØKNAD_SVANGERSKAPSPENGER = new DokumentTypeId("SØKNAD_SVANGERSKAPSPENGER", "I000001");
    public static final DokumentTypeId INNTEKTSMELDING = new DokumentTypeId("INNTEKTSMELDING", "I000067");
    public static final DokumentTypeId INNTEKTSOPPLYSNINGER = new DokumentTypeId("INNTEKTSOPPLYSNINGER", "I000026");
    public static final DokumentTypeId DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL = new DokumentTypeId("DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL", "I000041");
    public static final DokumentTypeId DOKUMENTASJON_AV_OMSORGSOVERTAKELSE = new DokumentTypeId("DOKUMENTASJON_AV_OMSORGSOVERTAKELSE", "I000042");
    public static final DokumentTypeId BEKREFTELSE_VENTET_FØDSELSDATO = new DokumentTypeId("BEKREFTELSE_VENTET_FØDSELSDATO", "I000062");
    public static final DokumentTypeId FØDSELSATTEST = new DokumentTypeId("FØDSELSATTEST", "I000063");
    public static final DokumentTypeId DOK_INNLEGGELSE = new DokumentTypeId("DOK_INNLEGGELSE", "I000037");
    public static final DokumentTypeId DOK_MORS_UTDANNING_ARBEID_SYKDOM = new DokumentTypeId("DOK_MORS_UTDANNING_ARBEID_SYKDOM", "I000038");
    public static final DokumentTypeId LEGEERKLÆRING = new DokumentTypeId("LEGEERKLÆRING", "I000023");
    public static final DokumentTypeId KLAGE_DOKUMENT = new DokumentTypeId("KLAGE_DOKUMENT", "I000027");
    public static final DokumentTypeId KLAGE_ETTERSENDELSE = new DokumentTypeId("I500027", "I500027");
    public static final DokumentTypeId ANNET = new DokumentTypeId("ANNET", "I000060");

    public static final DokumentTypeId UDEFINERT = new DokumentTypeId("-", null);

    private static final Set<String> VEDLEGG_TYPER = Set.of(BEKREFTELSE_VENTET_FØDSELSDATO, FØDSELSATTEST, LEGEERKLÆRING,
        DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL, DOKUMENTASJON_AV_OMSORGSOVERTAKELSE, DOK_INNLEGGELSE, DOK_MORS_UTDANNING_ARBEID_SYKDOM)
        .stream().flatMap(dti -> List.of(dti.getKode(), dti.getOffisiellKode()).stream()).collect(Collectors.toSet());

    private static final Set<String> SØKNAD_TYPER = Set.of(SØKNAD_ENGANGSSTØNAD_FØDSEL, SØKNAD_FORELDREPENGER_FØDSEL,
        SØKNAD_ENGANGSSTØNAD_ADOPSJON, SØKNAD_FORELDREPENGER_ADOPSJON, SØKNAD_SVANGERSKAPSPENGER)
        .stream().flatMap(dti -> List.of(dti.getKode(), dti.getOffisiellKode()).stream()).collect(Collectors.toSet());

    private static final Set<String> ENDRING_SØKNAD_TYPER = Set.of(FORELDREPENGER_ENDRING_SØKNAD, FLEKSIBELT_UTTAK_FORELDREPENGER)
        .stream().flatMap(dti -> List.of(dti.getKode(), dti.getOffisiellKode()).stream()).collect(Collectors.toSet());

    private static final Set<String> ANDRE_SPESIAL_TYPER = Set.of(INNTEKTSMELDING, KLAGE_DOKUMENT)
        .stream().flatMap(dti -> List.of(dti.getKode(), dti.getOffisiellKode()).stream()).collect(Collectors.toSet());

    public static final String KODEVERK = "DOKUMENT_TYPE_ID";

    @JsonIgnore
    @javax.persistence.Transient
    private String offisiellKode;

    @Column(name = "kode")
    private String kode;

    private DokumentTypeId(String kode, String offisiellKode) {
        this.kode = kode;
        this.offisiellKode = offisiellKode;
        KODER.put(kode, this);

    }

    public static Set<String> getVedleggTyper() {
        return VEDLEGG_TYPER;
    }

    public static Set<String> getSpesialTyperKoder() {
        Set<String> typer = new LinkedHashSet<>(SØKNAD_TYPER);
        typer.addAll(ENDRING_SØKNAD_TYPER);
        typer.addAll(ANDRE_SPESIAL_TYPER);
        return Collections.unmodifiableSet(typer);
    }

    public static Set<String> getSøknadTyper() {
        return SØKNAD_TYPER;
    }

    public static Set<String> getEndringSøknadTyper() {
        return ENDRING_SØKNAD_TYPER;
    }

    public boolean erSøknadType() {
        return SØKNAD_TYPER.contains(this.getKode());
    }

    public boolean erEndringsSøknadType() {
        return ENDRING_SØKNAD_TYPER.contains(this.getKode());
    }

    public boolean erInntektsmelding() {
        return INNTEKTSMELDING.getKode().equals(this.getKode()) || INNTEKTSMELDING.getOffisiellKode().equals(this.getOffisiellKode());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || !(obj instanceof DokumentType))
            return false;

        DokumentType other = (DokumentType) obj;
        return Objects.equals(kode, other.getKode())
            && Objects.equals(getKodeverk(), other.getKodeverk());

    }

    @Override
    public int hashCode() {
        return Objects.hash(getKode(), getKodeverk());
    }

    @JsonCreator
    public static DokumentTypeId fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);

        if (ad == null) {
            // midlertidig fallback til vi endrer til offisille kodeverdier
            ad = finnForKodeverkEiersKode(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent DokumentTypeId: " + kode);
            }
        }
        return ad;
    }

    @Override
    public String getNavn() {
        return getKode();
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

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<DokumentTypeId, String> {
        @Override
        public String convertToDatabaseColumn(DokumentTypeId attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public DokumentTypeId convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

    public static DokumentTypeId finnForKodeverkEiersKode(String offisiellDokumentType) {
        if (offisiellDokumentType == null)
            return DokumentTypeId.UDEFINERT;

        Optional<DokumentTypeId> dokId = KODER.values().stream().filter(k -> Objects.equals(k.offisiellKode, offisiellDokumentType)).findFirst();
        if (dokId.isPresent()) {
            return dokId.get();
        } else {
            return new DokumentTypeId(offisiellDokumentType, offisiellDokumentType);
        }
    }

    public static boolean erSøknadType(DokumentType dokumentTypeId) {
        return fraKode(dokumentTypeId.getKode()).erSøknadType();
    }

    public static boolean erEndringsSøknadType(DokumentType dokumentTypeId) {
        return fraKode(dokumentTypeId.getKode()).erEndringsSøknadType();
    }
}
