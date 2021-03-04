package no.nav.foreldrepenger.økonomistøtte.ny.mapper;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;

public class FagområdeMapper {

    public static KodeFagområde tilFagområde(FagsakYtelseType ytelseType, Boolean gjelderRefusjon) {
        return gjelderRefusjon
            ? tilFagområdeRefusjon(ytelseType)
            : tilFagområdeBruker(ytelseType);
    }

    public static KodeFagområde tilFagområdeRefusjon(FagsakYtelseType ytelseType) {
        return switch (ytelseType) {
            case FORELDREPENGER -> KodeFagområde.FORELDREPENGER_ARBEIDSGIVER;
            case SVANGERSKAPSPENGER -> KodeFagområde.SVANGERSKAPSPENGER_ARBEIDSGIVER;
            default -> throw new IllegalArgumentException("Ikke-støttet ytelse-type: " + ytelseType);
        };
    }

    public static KodeFagområde tilFagområdeBruker(FagsakYtelseType ytelseType) {
        return switch (ytelseType) {
            case ENGANGSTØNAD -> KodeFagområde.ENGANGSSTØNAD;
            case FORELDREPENGER -> KodeFagområde.FORELDREPENGER_BRUKER;
            case SVANGERSKAPSPENGER -> KodeFagområde.SVANGERSKAPSPENGER_BRUKER;
            default -> throw new IllegalArgumentException("Ikke-støttet ytelse-type: " + ytelseType);
        };
    }
}
