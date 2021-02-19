package no.nav.foreldrepenger.behandling.revurdering.felles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.domene.modell.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;

public class LagBeregningsgrunnlagTjeneste {
    public static BeregningsgrunnlagEntitet lagBeregningsgrunnlag(LocalDate skjæringstidspunktBeregning,
            boolean medOppjustertDagsat,
            boolean skalDeleAndelMellomArbeidsgiverOgBruker,
            List<ÅpenDatoIntervallEntitet> perioder,
            LagAndelTjeneste lagAndelTjeneste) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
                .medSkjæringstidspunkt(skjæringstidspunktBeregning)
                .medGrunnbeløp(BigDecimal.valueOf(91425L))
                .build();
        BeregningsgrunnlagAktivitetStatus.builder()
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .build(beregningsgrunnlag);
        for (ÅpenDatoIntervallEntitet datoPeriode : perioder) {
            BeregningsgrunnlagPeriode periode = byggBGPeriode(beregningsgrunnlag, datoPeriode, medOppjustertDagsat,
                    skalDeleAndelMellomArbeidsgiverOgBruker, lagAndelTjeneste);
            BeregningsgrunnlagPeriode.oppdater(periode)
                    .build(beregningsgrunnlag);
        }
        return beregningsgrunnlag;
    }

    private static BeregningsgrunnlagPeriode byggBGPeriode(BeregningsgrunnlagEntitet beregningsgrunnlag,
            ÅpenDatoIntervallEntitet datoPeriode,
            boolean medOppjustertDagsat,
            boolean skalDeleAndelMellomArbeidsgiverOgBruker,
            LagAndelTjeneste lagAndelTjeneste) {
        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.ny()
                .medBeregningsgrunnlagPeriode(datoPeriode.getFomDato(), datoPeriode.getTomDato())
                .build(beregningsgrunnlag);
        lagAndelTjeneste.lagAndeler(periode, medOppjustertDagsat, skalDeleAndelMellomArbeidsgiverOgBruker);
        return periode;
    }
}
