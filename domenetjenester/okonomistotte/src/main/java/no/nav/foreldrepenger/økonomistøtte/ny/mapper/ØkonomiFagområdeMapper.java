package no.nav.foreldrepenger.økonomistøtte.ny.mapper;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeFagområde;

public class ØkonomiFagområdeMapper {

    public static ØkonomiKodeFagområde tilFagområde(FagsakYtelseType ytelseType, Boolean gjelderRefusjon) {
        return gjelderRefusjon
            ? tilFagområdeRefusjon(ytelseType)
            : tilFagområdeBruker(ytelseType);
    }

    public static ØkonomiKodeFagområde tilFagområdeRefusjon(FagsakYtelseType ytelseType) {
        return switch (ytelseType) {
            case FORELDREPENGER -> ØkonomiKodeFagområde.FPREF;
            case SVANGERSKAPSPENGER -> ØkonomiKodeFagområde.SVPREF;
            default -> throw new IllegalArgumentException("Ikke-støttet ytelse-type: " + ytelseType);
        };
    }

    public static ØkonomiKodeFagområde tilFagområdeBruker(FagsakYtelseType ytelseType) {
        return switch (ytelseType) {
            case ENGANGSTØNAD -> ØkonomiKodeFagområde.REFUTG;
            case FORELDREPENGER -> ØkonomiKodeFagområde.FP;
            case SVANGERSKAPSPENGER -> ØkonomiKodeFagområde.SVP;
            default -> throw new IllegalArgumentException("Ikke-støttet ytelse-type: " + ytelseType);
        };
    }
}
