package no.nav.foreldrepenger.behandling.revurdering.felles;

import java.math.BigDecimal;
import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;

public class LagBeregningsresultatTjeneste {
    public static BeregningsresultatEntitet lagBeregningsresultatperiodeMedEndringstidspunkt(LocalDate endringsdato,
            boolean gjelderOriginalBehandling,
            String orgnr) {
        var brFP = BeregningsresultatEntitet.builder()
                .medRegelInput("clob1")
                .medRegelSporing("clob2")
                .build();
        BeregningsresultatPeriode periode;
        if (gjelderOriginalBehandling) {
            periode = BeregningsresultatPeriode.builder()
                    .medBeregningsresultatPeriodeFomOgTom(LocalDate.now().plusDays(1), LocalDate.now().plusDays(10))
                    .build(brFP);
        } else {
            periode = BeregningsresultatPeriode.builder()
                    .medBeregningsresultatPeriodeFomOgTom(endringsdato, LocalDate.now().plusDays(10))
                    .build(brFP);
        }
        buildBeregningsresultatAndel(periode, true, 1500, orgnr);
        buildBeregningsresultatAndel(periode, false, 500, orgnr);
        return brFP;
    }

    private static void buildBeregningsresultatAndel(BeregningsresultatPeriode beregningsresultatPeriode,
            Boolean brukerErMottaker,
            int dagsats,
            String orgnr) {
        BeregningsresultatAndel.builder()
                .medBrukerErMottaker(brukerErMottaker)
                .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr))
                .medDagsats(dagsats)
                .medDagsatsFraBg(dagsats)
                .medStillingsprosent(BigDecimal.valueOf(100))
                .medUtbetalingsgrad(BigDecimal.valueOf(100))
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .medInntektskategori(Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER)
                .build(beregningsresultatPeriode);
    }
}
