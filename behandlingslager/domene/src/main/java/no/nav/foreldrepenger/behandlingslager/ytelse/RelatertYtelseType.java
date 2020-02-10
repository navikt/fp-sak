package no.nav.foreldrepenger.behandlingslager.ytelse;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum RelatertYtelseType implements Kodeverdi {

    ENSLIG_FORSØRGER("ENSLIG_FORSØRGER", "Enslig forsørger"),
    SYKEPENGER("SYKEPENGER", "Sykepenger"),
    SVANGERSKAPSPENGER("SVANGERSKAPSPENGER", "Svangerskapspenger"),
    FORELDREPENGER("FORELDREPENGER", "Foreldrepenger"),
    ENGANGSSTØNAD("ENGANGSSTØNAD", "Engangsstønad"),
    PÅRØRENDESYKDOM("PÅRØRENDESYKDOM", "Pårørendesykdom"),
    ARBEIDSAVKLARINGSPENGER("ARBEIDSAVKLARINGSPENGER", "Arbeidsavklaringspenger"),
    DAGPENGER("DAGPENGER", "Dagpenger"),
    UDEFINERT("-", "Ikke definert"),
    ;

    private static final Map<FagsakYtelseType, Set<RelatertYtelseType>> OPPTJENING_RELATERTYTELSE_CONFIG = Map.of(
        FagsakYtelseType.FORELDREPENGER,
        Set.of(ENSLIG_FORSØRGER, SYKEPENGER, SVANGERSKAPSPENGER, FORELDREPENGER, PÅRØRENDESYKDOM, ARBEIDSAVKLARINGSPENGER, DAGPENGER),
        FagsakYtelseType.SVANGERSKAPSPENGER,
        Set.of(SYKEPENGER, SVANGERSKAPSPENGER, FORELDREPENGER, PÅRØRENDESYKDOM, DAGPENGER));

    private static final Map<String, RelatertYtelseType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "RELATERT_YTELSE_TYPE";

    @Deprecated
    public static final String DISCRIMINATOR = "RELATERT_YTELSE_TYPE";

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

    private RelatertYtelseType(String kode) {
        this.kode = kode;
    }

    private RelatertYtelseType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static RelatertYtelseType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent RelatertYtelseType: " + kode);
        }
        return ad;
    }

    public static Map<String, RelatertYtelseType> kodeMap() {
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

    public boolean girOpptjeningsTid(FagsakYtelseType ytelseType) {
        final var relatertYtelseTypeSet = OPPTJENING_RELATERTYTELSE_CONFIG.get(ytelseType);
        if (relatertYtelseTypeSet == null) {
            throw new IllegalStateException("Støtter ikke fagsakYtelseType" + ytelseType);
        }
        return relatertYtelseTypeSet.contains(this);
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet().stream().map(k -> "'" + k + "'").collect(Collectors.toList()));
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
    }

}
