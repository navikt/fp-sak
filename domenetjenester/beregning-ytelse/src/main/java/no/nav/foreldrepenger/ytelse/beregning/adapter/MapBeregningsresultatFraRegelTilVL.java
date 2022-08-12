package no.nav.foreldrepenger.ytelse.beregning.adapter;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.Beregningsresultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.ReferanseType;

public final class MapBeregningsresultatFraRegelTilVL {

    public static BeregningsresultatEntitet mapFra(Beregningsresultat resultat, BeregningsresultatEntitet eksisterendeResultat) {
        if (eksisterendeResultat.getBeregningsresultatPerioder().isEmpty()) {
            resultat.getBeregningsresultatPerioder().forEach(p -> mapFraPeriode(p, eksisterendeResultat));
        } else {
            throw new IllegalArgumentException("Forventer ingen beregningsresultatPerioder");
        }
        return eksisterendeResultat;
    }

    private static no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode mapFraPeriode(BeregningsresultatPeriode resultatPeriode, BeregningsresultatEntitet eksisterendeResultat) {
        var nyPeriode = no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(resultatPeriode.getFom(), resultatPeriode.getTom())
            .build(eksisterendeResultat);
        resultatPeriode.getBeregningsresultatAndelList().forEach(bra -> mapFraAndel(bra, nyPeriode));
        return nyPeriode;
    }

    private static BeregningsresultatAndel mapFraAndel(no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatAndel bra, no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode brp) {
        var dagsats = nullSafeLong(bra.getDagsats()).intValue();
        var dagsatsFraBg = nullSafeLong(bra.getDagsatsFraBg()).intValue();
        return BeregningsresultatAndel.builder()
            .medArbeidsgiver(finnArbeidsgiver(bra))
            .medBrukerErMottaker(bra.erBrukerMottaker())
            .medDagsats(dagsats)
            .medStillingsprosent(bra.getStillingsprosent())
            .medUtbetalingsgrad(bra.getUtbetalingsgrad())
            .medDagsatsFraBg(dagsatsFraBg)
            .medAktivitetStatus(AktivitetStatusMapper.fraRegelTilVl(bra))
            .medArbeidsforholdRef(bra.getArbeidsforhold() == null
                ? null : InternArbeidsforholdRef.ref(bra.getArbeidsforhold().getArbeidsforholdId()))
            .medInntektskategori(InntektskategoriMapper.fraRegelTilVL(bra.getInntektskategori()))
            .build(brp);
    }

    private static Arbeidsgiver finnArbeidsgiver(no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatAndel bra) {
        if (bra.getArbeidsforhold() == null) {
            return null;
        }
        var identifikator = bra.getArbeidsforhold().getIdentifikator();
        var referanseType = bra.getArbeidsforhold().getReferanseType();
        if (referanseType == ReferanseType.AKTØR_ID) {
            return Arbeidsgiver.person(new AktørId(identifikator));
        }
        if (referanseType == ReferanseType.ORG_NR) {
            return Arbeidsgiver.virksomhet(identifikator);
        }
        return null;
    }

    private static Long nullSafeLong(Long input) {
        if (input != null) {
            return input;
        }
        return 0L;
    }
}
