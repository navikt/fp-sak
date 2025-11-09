package no.nav.foreldrepenger.behandlingslager.ytelse;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum RelatertYtelseType implements Kodeverdi {

    ENSLIG_FORSØRGER("EF", "Enslig forsørger"),
    SYKEPENGER("SP", "Sykepenger"),
    SVANGERSKAPSPENGER("SVP", "Svangerskapspenger"),
    FORELDREPENGER("FP", "Foreldrepenger"),
    ENGANGSTØNAD("ES", "Engangsstønad"),
    FRISINN("FRISINN", "FRISINN"),
    PLEIEPENGER_SYKT_BARN("PSB", "Pleiepenger sykt barn"),
    PLEIEPENGER_NÆRSTÅENDE("PPN", "Pleiepenger nærstående"),
    OMSORGSPENGER("OMP", "Omsorgspenger"),
    OPPLÆRINGSPENGER("OLP", "Opplæringspenger"),
    ARBEIDSAVKLARINGSPENGER("AAP", "Arbeidsavklaringspenger"),
    DAGPENGER("DAG", "Dagpenger"),
    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke definert"),
    ;

    public static final Set<RelatertYtelseType> PLEIEPENGER = Set.of(PLEIEPENGER_SYKT_BARN, PLEIEPENGER_NÆRSTÅENDE, OPPLÆRINGSPENGER);

    private static final Set<RelatertYtelseType> OPPTJENING_RELATERTYTELSE_FELLES = Set.of(SYKEPENGER, SVANGERSKAPSPENGER, FORELDREPENGER,
        PLEIEPENGER_SYKT_BARN, PLEIEPENGER_NÆRSTÅENDE, OMSORGSPENGER, OPPLÆRINGSPENGER, FRISINN, DAGPENGER);

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

    private final String navn;
    @JsonValue
    private final String kode;

    RelatertYtelseType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
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
        var relatertYtelseTypeSet = OPPTJENING_RELATERTYTELSE_CONFIG.get(ytelseType);
        if (relatertYtelseTypeSet == null) {
            throw new IllegalStateException("Støtter ikke fagsakYtelseType" + ytelseType);
        }
        return OPPTJENING_RELATERTYTELSE_FELLES.contains(this) || relatertYtelseTypeSet.contains(this);
    }


}
