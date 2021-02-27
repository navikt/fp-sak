package no.nav.foreldrepenger.økonomistøtte.ny.mapper;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.InntektskategoriKlassekodeMapper;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;

public class KlassekodeUtleder {

    private KlassekodeUtleder() {
    }

    public static KodeKlassifik utled(BeregningsresultatAndel andel, FamilieYtelseType familieYtelseType) {
        boolean erRefusjonTilArbeidsgiver = !andel.skalTilBrukerEllerPrivatperson();
        return utled(andel.getInntektskategori(), familieYtelseType, erRefusjonTilArbeidsgiver);
    }

    public static KodeKlassifik utled(Inntektskategori inntektskategori, FamilieYtelseType familieYtelseType, boolean refusjonArbeidsgiver) {
        if (refusjonArbeidsgiver) {
            return switch (familieYtelseType) {
                case FØDSEL -> KodeKlassifik.FPF_REFUSJON_AG;
                case ADOPSJON -> KodeKlassifik.FPA_REFUSJON_AG;
                case SVANGERSKAPSPENGER -> KodeKlassifik.SVP_REFUSJON_AG;
                default -> throw new IllegalArgumentException("Utvikler feil: Opdrag andel har ikke-støttet familie ytelse type: " + familieYtelseType);
            };
        } else {
            return InntektskategoriKlassekodeMapper.mapTilKlassekode(inntektskategori, familieYtelseType);
        }
    }

    public static KodeKlassifik utledForFeriepenger() {
        return KodeKlassifik.FERIEPENGER_BRUKER;
    }

    public static KodeKlassifik utledForFeriepengeRefusjon(FamilieYtelseType familieYtelseType) {
        return switch (familieYtelseType) {
            case FØDSEL -> KodeKlassifik.FPF_FERIEPENGER_AG;
            case ADOPSJON -> KodeKlassifik.FPA_FERIEPENGER_AG;
            case SVANGERSKAPSPENGER -> KodeKlassifik.SVP_FERIEPENGER_AG;
            default -> throw new IllegalArgumentException("Utvikler feil: Ikke-støttet familie ytelse type: " + familieYtelseType);
        };
    }

}
