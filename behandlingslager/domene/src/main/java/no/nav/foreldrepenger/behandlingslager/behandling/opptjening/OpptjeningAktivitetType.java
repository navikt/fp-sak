package no.nav.foreldrepenger.behandlingslager.behandling.opptjening;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;

/**
 * <h3>Internt kodeverk</h3>
 * Definerer aktiviteter benyttet til å vurdere Opptjening.
 * <p>
 * Kodeverket sammenstiller data fra {@link ArbeidType} og {@link RelatertYtelseType}.<br>
 * Senere benyttes dette i mapping til bla. Beregningsgrunnlag.
 */

public enum OpptjeningAktivitetType implements Kodeverdi {

    ARBEIDSAVKLARING("AAP", "Arbeidsavklaringspenger",
            Set.of(),
            Set.of(RelatertYtelseType.ARBEIDSAVKLARINGSPENGER)),
    ARBEID("ARBEID", "Arbeid",
            Set.of(ArbeidType.FORENKLET_OPPGJØRSORDNING, ArbeidType.MARITIMT_ARBEIDSFORHOLD, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD),
            Set.of()),
    DAGPENGER("DAGPENGER", "Dagpenger",
            Set.of(),
            Set.of(RelatertYtelseType.DAGPENGER)),
    FORELDREPENGER("FORELDREPENGER", "Foreldrepenger",
            Set.of(),
            Set.of(RelatertYtelseType.FORELDREPENGER)),
    FRILANS("FRILANS", "Frilans",
            Set.of(ArbeidType.FRILANSER),
            Set.of()),
    FRILOPP("FRILOPP", "Frilansoppdrag",
        Set.of(ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER),
        Set.of()),
    MILITÆR_ELLER_SIVILTJENESTE("MILITÆR_ELLER_SIVILTJENESTE", "Militær- eller siviltjeneste",
            Set.of(ArbeidType.MILITÆR_ELLER_SIVILTJENESTE),
            Set.of()),
    NÆRING("NÆRING", "Næring",
            Set.of(ArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE),
            Set.of()),
    OMSORGSPENGER("OMSORGSPENGER", "Omsorgspenger",
            Set.of(),
            Set.of(RelatertYtelseType.OMSORGSPENGER)),
    OPPLÆRINGSPENGER("OPPLÆRINGSPENGER", "Opplæringspenger",
            Set.of(),
            Set.of(RelatertYtelseType.OPPLÆRINGSPENGER)),
    PLEIEPENGER("PLEIEPENGER", "Pleiepenger",
            Set.of(),
            Set.of(RelatertYtelseType.PLEIEPENGER_NÆRSTÅENDE, RelatertYtelseType.PLEIEPENGER_SYKT_BARN)),
    FRISINN("FRISINN", "FRISINN",
            Set.of(),
            Set.of(RelatertYtelseType.FRISINN)),
    ETTERLØNN_SLUTTPAKKE("ETTERLØNN_SLUTTPAKKE", "Etterlønn eller sluttpakke",
            Set.of(ArbeidType.ETTERLØNN_SLUTTPAKKE),
            Set.of()),
    SVANGERSKAPSPENGER("SVANGERSKAPSPENGER", "Svangerskapspenger",
            Set.of(),
            Set.of(RelatertYtelseType.SVANGERSKAPSPENGER)),
    SYKEPENGER("SYKEPENGER", "Sykepenger",
            Set.of(),
            Set.of(RelatertYtelseType.SYKEPENGER)),
    VENTELØNN_VARTPENGER("VENTELØNN_VARTPENGER", "Ventelønn eller vartpenger",
            Set.of(ArbeidType.VENTELØNN_VARTPENGER),
            Set.of()),
    VIDERE_ETTERUTDANNING("VIDERE_ETTERUTDANNING", "Videre- og etterutdanning",
            Set.of(ArbeidType.LØNN_UNDER_UTDANNING),
            Set.of()),
    UTENLANDSK_ARBEIDSFORHOLD("UTENLANDSK_ARBEIDSFORHOLD", "Arbeid i utlandet",
            Set.of(ArbeidType.UTENLANDSK_ARBEIDSFORHOLD),
            Set.of()),

    UTDANNINGSPERMISJON("UTDANNINGSPERMISJON", "Utdanningspermisjon",
            Set.of(), Set.of()),
    UDEFINERT(STANDARDKODE_UDEFINERT, "UDEFINERT",
            Set.of(),
            Set.of()),
            ;

    private static final Map<String, OpptjeningAktivitetType> KODER = new LinkedHashMap<>();

    private static final Map<OpptjeningAktivitetType, Set<ArbeidType>> INDEKS_OPPTJ_ARBEID = new LinkedHashMap<>();
    private static final Map<OpptjeningAktivitetType, Set<RelatertYtelseType>> INDEKS_OPPTJ_RELYT = new LinkedHashMap<>();

    public static final String KODEVERK = "OPPTJENING_AKTIVITET_TYPE";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
            INDEKS_OPPTJ_ARBEID.put(v, v.arbeidType);
            INDEKS_OPPTJ_RELYT.put(v, v.relatertYtelseType);

        }
    }

    public static final Set<OpptjeningAktivitetType> ANNEN_OPPTJENING = Set.of(VENTELØNN_VARTPENGER, MILITÆR_ELLER_SIVILTJENESTE, ETTERLØNN_SLUTTPAKKE,
        VIDERE_ETTERUTDANNING, UTENLANDSK_ARBEIDSFORHOLD, FRILANS);

    @JsonValue
    private String kode;

    private final String navn;

    private final Set<ArbeidType> arbeidType;

    private Set<RelatertYtelseType> relatertYtelseType;

    OpptjeningAktivitetType(String kode,
                            String navn,
                            Set<ArbeidType> arbeidType,
                            Set<RelatertYtelseType> relatertYtelseType) {
        this.kode = kode;
        this.navn = navn;
        this.arbeidType = arbeidType;
        this.relatertYtelseType = relatertYtelseType;
    }

    public static OpptjeningAktivitetType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent OpptjeningAktivitetType: " + kode);
        }
        return ad;
    }

    public static Map<String, OpptjeningAktivitetType> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<OpptjeningAktivitetType, String> {
        @Override
        public String convertToDatabaseColumn(OpptjeningAktivitetType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public OpptjeningAktivitetType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

    }

    public static Map<OpptjeningAktivitetType, Set<ArbeidType>> hentTilArbeidTypeRelasjoner() {
        return Collections.unmodifiableMap(INDEKS_OPPTJ_ARBEID);
    }

    public static Map<OpptjeningAktivitetType, Set<RelatertYtelseType>> hentTilRelatertYtelseTyper() {
        return Collections.unmodifiableMap(INDEKS_OPPTJ_RELYT);
    }

    public static Map<ArbeidType, Set<OpptjeningAktivitetType>> hentFraArbeidTypeRelasjoner() {
        return hentTilArbeidTypeRelasjoner().entrySet().stream()
            .flatMap(entry -> entry.getValue().stream()
                .map(v -> new AbstractMap.SimpleEntry<>(v, entry.getKey())))
            .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toSet())));
    }

    public static Map<RelatertYtelseType, Set<OpptjeningAktivitetType>> hentFraRelatertYtelseTyper() {
        return hentTilRelatertYtelseTyper().entrySet().stream()
            .flatMap(entry -> entry.getValue().stream()
                .map(v -> new AbstractMap.SimpleEntry<>(v, entry.getKey())))
            .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toSet())));
    }

}
