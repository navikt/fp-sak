package no.nav.foreldrepenger.domene.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.abakus.iaygrunnlag.Periode;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktiviteter;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningForBeregningTjeneste;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;

@ExtendWith(MockitoExtension.class)
class AAPKombinertMedATFLTjenesteTest {

    private static final LocalDate STP_DATO = LocalDate.of(2024, 6, 1);
    private static final String ORGNR = OrgNummer.KUNSTIG_ORG;
    private static final Skjæringstidspunkt STP = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(STP_DATO).build();

    @Mock
    private OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste;
    @Mock
    private BeregningTjeneste beregningTjeneste;

    private AAPKombinertMedATFLTjeneste tjeneste;
    private BehandlingReferanse behandlingReferanse;

    @BeforeEach
    void setUp() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        behandlingReferanse = BehandlingReferanse.fra(behandling);
        AbakusInMemoryInntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandlingReferanse.behandlingId(),
            InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER));
        tjeneste = new AAPKombinertMedATFLTjeneste(inntektArbeidYtelseTjeneste, opptjeningForBeregningTjeneste, beregningTjeneste);
    }

    @Test
    void skal_gi_true_når_aap_på_stp_og_atfl_i_siste_tre_måneder() {
        var opptjening = new OpptjeningAktiviteter(
            OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEIDSAVKLARING, new Periode(STP_DATO.minusMonths(6), STP_DATO)),
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, new Periode(STP_DATO.minusMonths(2), STP_DATO), ORGNR));
        when(opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(any(), any(), any())).thenReturn(Optional.of(opptjening));

        var resultat = tjeneste.harAAPKombinertMedATFL(behandlingReferanse, STP);

        assertThat(resultat).isTrue();
    }

    @Test
    void skal_gi_false_når_kun_aap_ingen_atfl() {
        var opptjening = OpptjeningAktiviteter.fra(OpptjeningAktivitetType.ARBEIDSAVKLARING, new Periode(STP_DATO.minusMonths(6), STP_DATO));
        when(opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(any(), any(), any())).thenReturn(Optional.of(opptjening));

        var resultat = tjeneste.harAAPKombinertMedATFL(behandlingReferanse, STP);

        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_false_når_kun_atfl_ingen_aap() {
        var opptjening = OpptjeningAktiviteter.fraOrgnr(OpptjeningAktivitetType.ARBEID, new Periode(STP_DATO.minusMonths(6), STP_DATO), ORGNR);
        when(opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(any(), any(), any())).thenReturn(Optional.of(opptjening));

        var resultat = tjeneste.harAAPKombinertMedATFL(behandlingReferanse, STP);

        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_false_når_atfl_avsluttet_mer_enn_tre_måneder_før_stp() {
        var opptjening = new OpptjeningAktiviteter(
            OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEIDSAVKLARING, new Periode(STP_DATO.minusMonths(6), STP_DATO)),
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, new Periode(STP_DATO.minusMonths(6), STP_DATO.minusMonths(4)),
                ORGNR));
        when(opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(any(), any(), any())).thenReturn(Optional.of(opptjening));

        var resultat = tjeneste.harAAPKombinertMedATFL(behandlingReferanse, STP);

        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_false_når_beregningsgrunnlag_er_overstyrt() {
        var bg = Beregningsgrunnlag.builder().medSkjæringstidspunkt(LocalDate.now()).medOverstyring(true).build();
        var overstyrtGrunnlag = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(bg).build(BeregningsgrunnlagTilstand.FORESLÅTT);
        when(beregningTjeneste.hent(any())).thenReturn(Optional.of(overstyrtGrunnlag));

        var resultat = tjeneste.harAAPKombinertMedATFL(behandlingReferanse, STP);

        assertThat(resultat).isFalse();
    }
}
