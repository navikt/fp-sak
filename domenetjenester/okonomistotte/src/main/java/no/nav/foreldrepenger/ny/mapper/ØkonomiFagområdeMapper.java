package no.nav.foreldrepenger.ny.mapper;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeFagområde;

public class ØkonomiFagområdeMapper {

    public static ØkonomiKodeFagområde tilFagområde(FagsakYtelseType ytelseType, Boolean gjelderRefusjon) {
        return gjelderRefusjon
            ? tilFagområdeRefusjon(ytelseType)
            : tilFagområdeBruker(ytelseType);
    }

    public static ØkonomiKodeFagområde tilFagområdeRefusjon(FagsakYtelseType ytelseType) {
        switch (ytelseType) {
            case FORELDREPENGER:
                return ØkonomiKodeFagområde.FPREF;
            case SVANGERSKAPSPENGER:
                return ØkonomiKodeFagområde.SVPREF;
            default:
                throw new IllegalArgumentException("Ikke-støttet ytelse-type: " + ytelseType);
        }
    }

    public static ØkonomiKodeFagområde tilFagområdeBruker(FagsakYtelseType ytelseType) {
        switch (ytelseType) {
            case ENGANGSTØNAD:
                return ØkonomiKodeFagområde.REFUTG;
            case FORELDREPENGER:
                return ØkonomiKodeFagområde.FP;
            case SVANGERSKAPSPENGER:
                return ØkonomiKodeFagområde.SVP;
            default:
                throw new IllegalArgumentException("Ikke-støttet ytelse-type: " + ytelseType);
        }
    }
}
