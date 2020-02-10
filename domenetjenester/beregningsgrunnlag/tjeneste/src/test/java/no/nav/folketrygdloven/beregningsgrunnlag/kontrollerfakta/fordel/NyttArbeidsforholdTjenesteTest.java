package no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.fordel;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.tid.IntervalUtils;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class NyttArbeidsforholdTjenesteTest {
    private static final Arbeidsgiver AG1 = Arbeidsgiver.virksomhet("999999999");
    private static final Arbeidsgiver AG2 = Arbeidsgiver.virksomhet("888888888");
    private static final InternArbeidsforholdRef REF1 = InternArbeidsforholdRef.nyRef();
    private static final InternArbeidsforholdRef REF2 = InternArbeidsforholdRef.nyRef();
    private static final LocalDate STP = LocalDate.of(2020,1,1);
    private static final ÅpenDatoIntervallEntitet LØPENDE_PERIODE = ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(6), IntervalUtils.TIDENES_ENDE);
    private static BeregningsgrunnlagEntitet BG;
    private static BeregningsgrunnlagPeriode BG_PERIODE;

    @Before
    public void setup() {
        BG = BeregningsgrunnlagEntitet.builder().medSkjæringstidspunkt(STP).build();
        BG_PERIODE = BeregningsgrunnlagPeriode.builder().medBeregningsgrunnlagPeriode(STP, IntervalUtils.TIDENES_ENDE).build(BG);
    }

    @Test
    public void skal_teste_at_arbeidsforhold_uten_ref_etter_stp_ikke_matcher_på_arbfør_før_stp_med_ref() {
        BeregningAktivitetEntitet akt1 = lagAktivitet(AG1, REF1);
        BeregningAktivitetAggregatEntitet agg = BeregningAktivitetAggregatEntitet.builder().medSkjæringstidspunktOpptjening(STP).leggTilAktivitet(akt1).build();
        BeregningsgrunnlagPrStatusOgAndel bgAndel = lagAndel(AG1, InternArbeidsforholdRef.nullRef());

        boolean erNytt = NyttArbeidsforholdTjeneste.erNyttArbeidsforhold(bgAndel, agg);

        assertThat(erNytt).isTrue();
    }

    @Test
    public void skal_teste_at_arbeidsforhold_med_ref_som_matcher_aggregat_ikke_regnes_som_ny() {
        BeregningAktivitetEntitet akt1 = lagAktivitet(AG1, REF1);
        BeregningAktivitetAggregatEntitet agg = BeregningAktivitetAggregatEntitet.builder().medSkjæringstidspunktOpptjening(STP).leggTilAktivitet(akt1).build();
        BeregningsgrunnlagPrStatusOgAndel bgAndel = lagAndel(AG1, REF1);

        boolean erNytt = NyttArbeidsforholdTjeneste.erNyttArbeidsforhold(bgAndel, agg);

        assertThat(erNytt).isFalse();
    }

    @Test
    public void skal_teste_at_andel_med_ag_som_ikke_finnes_i_aggregat_regnes_som_ny() {
        BeregningAktivitetEntitet akt1 = lagAktivitet(AG1, REF1);
        BeregningAktivitetAggregatEntitet agg = BeregningAktivitetAggregatEntitet.builder().medSkjæringstidspunktOpptjening(STP).leggTilAktivitet(akt1).build();
        BeregningsgrunnlagPrStatusOgAndel bgAndel = lagAndel(AG2, REF2);

        boolean erNytt = NyttArbeidsforholdTjeneste.erNyttArbeidsforhold(bgAndel, agg);

        assertThat(erNytt).isTrue();
    }

    @Test
    public void skal_teste_at_andeler_uten_arbeidsforhol_ikke_regnes_som_nye() {
        BeregningAktivitetEntitet akt1 = lagAktivitet(AG1, REF1);
        BeregningAktivitetAggregatEntitet agg = BeregningAktivitetAggregatEntitet.builder().medSkjæringstidspunktOpptjening(STP).leggTilAktivitet(akt1).build();
        BeregningsgrunnlagPrStatusOgAndel bgAndel = lagAndelUtenArbfor();

        boolean erNytt = NyttArbeidsforholdTjeneste.erNyttArbeidsforhold(bgAndel, agg);

        assertThat(erNytt).isFalse();
    }


    public BeregningsgrunnlagPrStatusOgAndel lagAndel(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef ref) {
        BGAndelArbeidsforhold.Builder builder = BGAndelArbeidsforhold.builder().medArbeidsgiver(arbeidsgiver).medArbeidsforholdRef(ref).medArbeidsperiodeFom(LocalDate.now());
        return BeregningsgrunnlagPrStatusOgAndel.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER).medBeregnetPrÅr(BigDecimal.ZERO).medBGAndelArbeidsforhold(builder).build(BG_PERIODE);
    }

    public BeregningsgrunnlagPrStatusOgAndel lagAndelUtenArbfor() {
        return BeregningsgrunnlagPrStatusOgAndel.builder().medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE).medBeregnetPrÅr(BigDecimal.ZERO).build(BG_PERIODE);
    }

    public BeregningAktivitetEntitet lagAktivitet(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef ref) {
        return BeregningAktivitetEntitet.builder()
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdRef(ref)
            .medPeriode(LØPENDE_PERIODE)
            .build();
    }

}
