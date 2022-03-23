package no.nav.foreldrepenger.behandlingslager.ytelse;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum RelatertYtelseType implements Kodeverdi {

    ENSLIG_FORSØRGER("ENSLIG_FORSØRGER", "Enslig forsørger"),
    SYKEPENGER("SYKEPENGER", "Sykepenger"),
    SVANGERSKAPSPENGER("SVANGERSKAPSPENGER", "Svangerskapspenger"),
    FORELDREPENGER("FORELDREPENGER", "Foreldrepenger"),
    ENGANGSSTØNAD("ENGANGSSTØNAD", "Engangsstønad"),
    PÅRØRENDESYKDOM("PÅRØRENDESYKDOM", "Pårørendesykdom"),
    FRISINN("FRISINN", "FRISINN"),
    PLEIEPENGER_SYKT_BARN("PSB", "Pleiepenger sykt barn"),
    PLEIEPENGER_NÆRSTÅENDE("PPN", "Pleiepenger nærstående"),
    OMSORGSPENGER("OMP", "Omsorgspenger"),
    OPPLÆRINGSPENGER("OLP", "Opplæringspenger"),
    ARBEIDSAVKLARINGSPENGER("ARBEIDSAVKLARINGSPENGER", "Arbeidsavklaringspenger"),
    DAGPENGER("DAGPENGER", "Dagpenger"),
    UDEFINERT("-", "Ikke definert"),
    ;

    private static final Set<RelatertYtelseType> OPPTJENING_RELATERTYTELSE_FELLES = Set.of(SYKEPENGER, SVANGERSKAPSPENGER, FORELDREPENGER,
        PÅRØRENDESYKDOM, PLEIEPENGER_SYKT_BARN, PLEIEPENGER_NÆRSTÅENDE, OMSORGSPENGER, OPPLÆRINGSPENGER, FRISINN, DAGPENGER);

    private static final Map<FagsakYtelseType, Set<RelatertYtelseType>> OPPTJENING_RELATERTYTELSE_CONFIG = Map.of(
        FagsakYtelseType.FORELDREPENGER, Set.of(ENSLIG_FORSØRGER, ARBEIDSAVKLARINGSPENGER),
        FagsakYtelseType.SVANGERSKAPSPENGER, Collections.emptySet());

    private static final Map<String, RelatertYtelseType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "RELATERT_YTELSE_TYPE";

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

    RelatertYtelseType(String kode) {
        this.kode = kode;
    }

    RelatertYtelseType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, RelatertYtelseType> kodeMap() {
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

    public boolean girOpptjeningsTid(FagsakYtelseType ytelseType) {
        final var relatertYtelseTypeSet = OPPTJENING_RELATERTYTELSE_CONFIG.get(ytelseType);
        if (relatertYtelseTypeSet == null) {
            throw new IllegalStateException("Støtter ikke fagsakYtelseType" + ytelseType);
        }
        return OPPTJENING_RELATERTYTELSE_FELLES.contains(this) || relatertYtelseTypeSet.contains(this);
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<RelatertYtelseType, String> {
        @Override
        public String convertToDatabaseColumn(RelatertYtelseType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public RelatertYtelseType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static RelatertYtelseType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent RelatertYtelseType: " + kode);
            }
            return ad;
        }
    }

}
