package no.nav.foreldrepenger.behandling.revurdering.felles;

import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class LagBeregningsgrunnlagTjeneste {
    public static Beregningsgrunnlag lagBeregningsgrunnlag(LocalDate skjæringstidspunktBeregning,
                                                           boolean medOppjustertDagsat,
                                                           boolean skalDeleAndelMellomArbeidsgiverOgBruker,
                                                           List<ÅpenDatoIntervallEntitet> perioder,
                                                           LagAndelTjeneste lagAndelTjeneste) {
        var bgBuilder = Beregningsgrunnlag.builder()
                .medSkjæringstidspunkt(skjæringstidspunktBeregning)
                .medGrunnbeløp(BigDecimal.valueOf(91425L))
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder()
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER).build());

        for (var datoPeriode : perioder) {
            var periode = byggBGPeriode(datoPeriode, medOppjustertDagsat,
                    skalDeleAndelMellomArbeidsgiverOgBruker, lagAndelTjeneste);
            bgBuilder.leggTilBeregningsgrunnlagPeriode(periode);
        }
        return bgBuilder.build();
    }

    private static BeregningsgrunnlagPeriode byggBGPeriode(ÅpenDatoIntervallEntitet datoPeriode,
            boolean medOppjustertDagsat,
            boolean skalDeleAndelMellomArbeidsgiverOgBruker,
            LagAndelTjeneste lagAndelTjeneste) {
        var andeler = lagAndelTjeneste.lagAndeler(medOppjustertDagsat, skalDeleAndelMellomArbeidsgiverOgBruker);
        var periodeBuilder = BeregningsgrunnlagPeriode.builder()
                .medBeregningsgrunnlagPeriode(datoPeriode.getFomDato(), datoPeriode.getTomDato());
        andeler.forEach(periodeBuilder::leggTilBeregningsgrunnlagPrStatusOgAndel);
        return periodeBuilder.build();
    }
}
