package no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper;

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
            case FORELDREPENGER -> KodeFagområde.FPREF;
            case SVANGERSKAPSPENGER -> KodeFagområde.SVPREF;
            default -> throw new IllegalArgumentException("Ikke-støttet ytelse-type: " + ytelseType);
        };
    }

    public static KodeFagområde tilFagområdeBruker(FagsakYtelseType ytelseType) {
        return switch (ytelseType) {
            case ENGANGSTØNAD -> KodeFagområde.REFUTG;
            case FORELDREPENGER -> KodeFagområde.FP;
            case SVANGERSKAPSPENGER -> KodeFagområde.SVP;
            default -> throw new IllegalArgumentException("Ikke-støttet ytelse-type: " + ytelseType);
        };
    }
}
