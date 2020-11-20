package no.nav.foreldrepenger.økonomi.ny.mapper;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.InntektskategoriKlassekodeMapper;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeKlassifik;

public class KlassekodeUtleder {

    private KlassekodeUtleder() {
    }

    public static ØkonomiKodeKlassifik utled(BeregningsresultatAndel andel, FamilieYtelseType familieYtelseType) {
        if (andel.skalTilBrukerEllerPrivatperson()) {
            String kode = InntektskategoriKlassekodeMapper.mapTilKlassekode(andel.getInntektskategori(), familieYtelseType);
            return ØkonomiKodeKlassifik.fraKode(kode);
        } else {
            switch (familieYtelseType) {
                case FØDSEL:
                    return ØkonomiKodeKlassifik.FPREFAG_IOP;
                case ADOPSJON:
                    return ØkonomiKodeKlassifik.FPADREFAG_IOP;
                case SVANGERSKAPSPENGER:
                    return ØkonomiKodeKlassifik.FPSVREFAG_IOP;
                default:
                    throw new IllegalArgumentException("Utvikler feil: Opdrag andel har ikke-støttet familie ytelse type: " + familieYtelseType);
            }
        }
    }

    public static ØkonomiKodeKlassifik utledForFeriepenger() {
        return ØkonomiKodeKlassifik.FPATFER;
    }

    public static ØkonomiKodeKlassifik utledForFeriepengeRefusjon(FamilieYtelseType familieYtelseType) {
        switch (familieYtelseType) {
            case FØDSEL:
                return ØkonomiKodeKlassifik.FPREFAGFER_IOP;
            case ADOPSJON:
                return ØkonomiKodeKlassifik.FPADREFAGFER_IOP;
            case SVANGERSKAPSPENGER:
                return ØkonomiKodeKlassifik.FPSVREFAGFER_IOP;
            default:
                throw new IllegalArgumentException("Utvikler feil: Ikke-støttet familie ytelse type: " + familieYtelseType);
        }
    }

}
