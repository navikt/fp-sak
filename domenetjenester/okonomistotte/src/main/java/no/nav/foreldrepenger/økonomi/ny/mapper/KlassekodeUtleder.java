package no.nav.foreldrepenger.økonomi.ny.mapper;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.InntektskategoriKlassekodeMapper;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeKlassifik;

public class KlassekodeUtleder {

    private KlassekodeUtleder() {
    }

    public static ØkonomiKodeKlassifik utled(BeregningsresultatAndel andel, FamilieYtelseType familieYtelseType) {
        boolean erRefusjonTilArbeidsgiver = !andel.skalTilBrukerEllerPrivatperson();
        return utled(andel.getInntektskategori(), familieYtelseType, erRefusjonTilArbeidsgiver);
    }

    public static ØkonomiKodeKlassifik utled(Inntektskategori inntektskategori, FamilieYtelseType familieYtelseType, boolean refusjonArbeidsgiver) {
        if (refusjonArbeidsgiver) {
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
        } else {
            String kode = InntektskategoriKlassekodeMapper.mapTilKlassekode(inntektskategori, familieYtelseType);
            return ØkonomiKodeKlassifik.fraKode(kode);
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
