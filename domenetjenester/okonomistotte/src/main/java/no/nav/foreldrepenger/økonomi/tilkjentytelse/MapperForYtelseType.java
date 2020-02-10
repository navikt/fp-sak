package no.nav.foreldrepenger.økonomi.tilkjentytelse;

import java.util.Map;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.v1.TilkjentYtelseV1;

class MapperForYtelseType {

    private static final Map<FagsakYtelseType, TilkjentYtelseV1.YtelseType> YTELSE_TYPE_MAP = Map.of(
        FagsakYtelseType.ENGANGSTØNAD, TilkjentYtelseV1.YtelseType.ENGANGSTØNAD,
        FagsakYtelseType.FORELDREPENGER, TilkjentYtelseV1.YtelseType.FORELDREPENGER,
        FagsakYtelseType.SVANGERSKAPSPENGER, TilkjentYtelseV1.YtelseType.SVANGERSKAPSPENGER
    );

    private MapperForYtelseType() {
        //for å unngå instansiering, slik at SonarQube blir glad
    }

    static TilkjentYtelseV1.YtelseType mapYtelseType(FagsakYtelseType fagsakYtelseType) {
        TilkjentYtelseV1.YtelseType resultat = YTELSE_TYPE_MAP.get(fagsakYtelseType);
        if (resultat != null) {
            return resultat;
        }
        throw new IllegalArgumentException("Utvikler-feil: FagsakYtelseType " + fagsakYtelseType + " er ikke støttet i mapping");
    }
}
