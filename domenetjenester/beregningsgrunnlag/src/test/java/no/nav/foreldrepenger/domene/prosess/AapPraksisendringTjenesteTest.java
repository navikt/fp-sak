package no.nav.foreldrepenger.domene.prosess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektspostBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitet;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetAggregat;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.modell.kodeverk.Hjemmel;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(MockitoExtension.class)
class AapPraksisendringTjenesteTest {

    @Mock
    private BeregningTjeneste beregningTjeneste;
    @Mock
    private InntektArbeidYtelseTjeneste iayTjeneste;
    @Mock
    BehandlingRepository behandlingRepository;
    private AapPraksisendringTjeneste fullAapAnnenStatusLoggTjeneste;

    @BeforeEach
    void setUp() {
        fullAapAnnenStatusLoggTjeneste = new AapPraksisendringTjeneste(beregningTjeneste, iayTjeneste, behandlingRepository);
    }

    @Test
    void skal_logge_hvis_det_er_treff() {
        var stp = LocalDate.of(2025,6,1);
        var bg = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(stp)
            .leggTilAktivitetStatus(lagAktivitetstatus(AktivitetStatus.ARBEIDSTAKER))
            .leggTilAktivitetStatus(lagAktivitetstatus(AktivitetStatus.ARBEIDSAVKLARINGSPENGER))
            .build();
        var gr = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(bg).build(BeregningsgrunnlagTilstand.FASTSATT);

        var ytelseBuilder = YtelseBuilder.oppdatere(Optional.empty())
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(stp.minusMonths(10), stp.minusDays(1)))
            .medStatus(RelatertYtelseTilstand.LØPENDE)
            .medKilde(Fagsystem.ARENA)
            .medYtelseType(RelatertYtelseType.ARBEIDSAVKLARINGSPENGER);
        var ytelseAnvist = ytelseBuilder.getAnvistBuilder()
            .medAnvistPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(stp.minusMonths(10), stp.minusDays(1)))
            .medUtbetalingsgradProsent(BigDecimal.valueOf(160))
            .build();
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagMocked();
        var ref = BehandlingReferanse.fra(behandling);
        ytelseBuilder.medYtelseAnvist(ytelseAnvist);
        var aktørYtelse = InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder.oppdatere(Optional.empty()).leggTilYtelse(ytelseBuilder).medAktørId(ref.aktørId());
        var iayGr = InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medData(InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER).leggTilAktørYtelse(aktørYtelse))
            .build();

        when(beregningTjeneste.hent(ref)).thenReturn(Optional.of(gr));
        when(iayTjeneste.finnGrunnlag(any())).thenReturn(Optional.of(iayGr));

        fullAapAnnenStatusLoggTjeneste.loggVedFullAapOgAnnenStatus(ref);
    }

    @Test
    void skal_ikke_revurdere_sak_som_mangler_aksjonspunkt() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();

        when(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(1L)).thenReturn(List.of(behandling));

        var erPåvirket = fullAapAnnenStatusLoggTjeneste.erPåvirketAvPraksisendring(1L);

        assertThat(erPåvirket).isFalse();
    }

    @Test
    void skal_ikke_revurdere_sak_som_har_blitt_beregnet_som_arbeidstaker() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        AksjonspunktTestSupport.leggTilAksjonspunkt(behandling,
            AksjonspunktDefinisjon.AVKLAR_AKTIVITETER).setStatus(AksjonspunktStatus.UTFØRT, "Ferdig");

        var stp = LocalDate.of(2025,6,1);
        var bg = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(stp)
            .leggTilAktivitetStatus(lagAktivitetstatus(AktivitetStatus.ARBEIDSTAKER))
            .leggTilAktivitetStatus(lagAktivitetstatus(AktivitetStatus.ARBEIDSAVKLARINGSPENGER))
            .build();
        var gr = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(bg).build(BeregningsgrunnlagTilstand.FASTSATT);
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(1L)).thenReturn(List.of(behandling));
        when(beregningTjeneste.hent(any())).thenReturn(Optional.of(gr));

        var erPåvirket = fullAapAnnenStatusLoggTjeneste.erPåvirketAvPraksisendring(1L);

        assertThat(erPåvirket).isFalse();
    }

    @Test
    void skal_ikke_revurdere_sak_dersom_ingen_arbeidsforhold_er_fjernet() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        AksjonspunktTestSupport.leggTilAksjonspunkt(behandling,
            AksjonspunktDefinisjon.AVKLAR_AKTIVITETER).setStatus(AksjonspunktStatus.UTFØRT, "Ferdig");

        var stp = LocalDate.of(2025,6,1);
        var bg = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(stp)
            .leggTilAktivitetStatus(lagAktivitetstatus(AktivitetStatus.ARBEIDSTAKER))
            .leggTilAktivitetStatus(lagAktivitetstatus(AktivitetStatus.ARBEIDSAVKLARINGSPENGER))
            .build();
        var register = BeregningAktivitetAggregat.builder()
            .leggTilAktivitet(lagAktivitet(OpptjeningAktivitetType.ARBEID, "999999999"))
            .leggTilAktivitet(lagAktivitet(OpptjeningAktivitetType.ARBEIDSAVKLARING, null))
            .medSkjæringstidspunktOpptjening(stp)
            .build();
        var gr = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(bg).medRegisterAktiviteter(register).medSaksbehandletAktiviteter(register).build(BeregningsgrunnlagTilstand.FASTSATT);
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(1L)).thenReturn(List.of(behandling));
        when(beregningTjeneste.hent(any())).thenReturn(Optional.of(gr));

        var erPåvirket = fullAapAnnenStatusLoggTjeneste.erPåvirketAvPraksisendring(1L);

        assertThat(erPåvirket).isFalse();
    }

    @Test
    void skal_ikke_revurdere_sak_dersom_ingen_inntekt_er_rapportert() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        AksjonspunktTestSupport.leggTilAksjonspunkt(behandling,
            AksjonspunktDefinisjon.AVKLAR_AKTIVITETER).setStatus(AksjonspunktStatus.UTFØRT, "Ferdig");
        var stp = LocalDate.of(2025,6,1);

        var iayBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var inntektBuilder = InntektBuilder.oppdatere(Optional.empty())
            .medInntektsKilde(InntektsKilde.INNTEKT_BEREGNING)
            .medArbeidsgiver(Arbeidsgiver.virksomhet("99999998")) // Ikke samme orgnr som i beregning
            .leggTilInntektspost(InntektspostBuilder.ny().medPeriode(stp.minusMonths(3), stp.minusMonths(1)).medBeløp(BigDecimal.valueOf(1)));
        var aktørInntektBuilder = iayBuilder.getAktørInntektBuilder(behandling.getAktørId())
            .leggTilInntekt(inntektBuilder);
        var grIay = iayBuilder.leggTilAktørArbeid(
                InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
                    .medAktørId(behandling.getAktørId()))
            .leggTilAktørInntekt(aktørInntektBuilder);
        var iay = InntektArbeidYtelseGrunnlagBuilder.nytt().medData(grIay).build();
        var bg = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(stp)
            .leggTilAktivitetStatus(lagAktivitetstatus(AktivitetStatus.ARBEIDSAVKLARINGSPENGER))
            .build();
        var register = BeregningAktivitetAggregat.builder()
            .leggTilAktivitet(lagAktivitet(OpptjeningAktivitetType.ARBEID, "999999999"))
            .leggTilAktivitet(lagAktivitet(OpptjeningAktivitetType.ARBEIDSAVKLARING, null))
            .medSkjæringstidspunktOpptjening(stp)
            .build();
        var saksbehandlet = BeregningAktivitetAggregat.builder()
            .leggTilAktivitet(lagAktivitet(OpptjeningAktivitetType.ARBEIDSAVKLARING, null))
            .medSkjæringstidspunktOpptjening(stp)
            .build();
        var gr = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(bg).medRegisterAktiviteter(register).medSaksbehandletAktiviteter(saksbehandlet).build(BeregningsgrunnlagTilstand.FASTSATT);
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(1L)).thenReturn(List.of(behandling));
        when(beregningTjeneste.hent(any())).thenReturn(Optional.of(gr));
        when(iayTjeneste.finnGrunnlag(any())).thenReturn(Optional.of(iay));
        var erPåvirket = fullAapAnnenStatusLoggTjeneste.erPåvirketAvPraksisendring(1L);

        assertThat(erPåvirket).isFalse();
    }

    @Test
    void skal_revurdere_sak_dersom_inntekt_er_rapportert_på_fjernet_orgnr() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        AksjonspunktTestSupport.leggTilAksjonspunkt(behandling,
            AksjonspunktDefinisjon.AVKLAR_AKTIVITETER).setStatus(AksjonspunktStatus.UTFØRT, "Ferdig");
        var stp = LocalDate.of(2025,6,1);

        var iayBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var inntektBuilder = InntektBuilder.oppdatere(Optional.empty())
            .medInntektsKilde(InntektsKilde.INNTEKT_BEREGNING)
            .medArbeidsgiver(Arbeidsgiver.virksomhet("999999999"))
            .leggTilInntektspost(InntektspostBuilder.ny().medPeriode(stp.minusMonths(3), stp.minusMonths(1)).medBeløp(BigDecimal.valueOf(1)));
        var aktørInntektBuilder = iayBuilder.getAktørInntektBuilder(behandling.getAktørId())
            .leggTilInntekt(inntektBuilder);
        var grIay = iayBuilder.leggTilAktørArbeid(
                InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty()).medAktørId(behandling.getAktørId()))
            .leggTilAktørInntekt(aktørInntektBuilder);
        var iay = InntektArbeidYtelseGrunnlagBuilder.nytt().medData(grIay).build();
        var bg = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(stp)
            .leggTilAktivitetStatus(lagAktivitetstatus(AktivitetStatus.ARBEIDSAVKLARINGSPENGER))
            .build();
        var register = BeregningAktivitetAggregat.builder()
            .leggTilAktivitet(lagAktivitet(OpptjeningAktivitetType.ARBEID, "999999999"))
            .leggTilAktivitet(lagAktivitet(OpptjeningAktivitetType.ARBEIDSAVKLARING, null))
            .medSkjæringstidspunktOpptjening(stp)
            .build();
        var saksbehandlet = BeregningAktivitetAggregat.builder()
            .leggTilAktivitet(lagAktivitet(OpptjeningAktivitetType.ARBEIDSAVKLARING, null))
            .medSkjæringstidspunktOpptjening(stp)
            .build();
        var gr = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(bg).medRegisterAktiviteter(register).medSaksbehandletAktiviteter(saksbehandlet).build(BeregningsgrunnlagTilstand.FASTSATT);
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(1L)).thenReturn(List.of(behandling));
        when(beregningTjeneste.hent(any())).thenReturn(Optional.of(gr));
        when(iayTjeneste.finnGrunnlag(any())).thenReturn(Optional.of(iay));
        var erPåvirket = fullAapAnnenStatusLoggTjeneste.erPåvirketAvPraksisendring(1L);

        assertThat(erPåvirket).isTrue();
    }

    @Test
    void skal_ikke_revurdere_hvis_beregnet_som_frilanser() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        AksjonspunktTestSupport.leggTilAksjonspunkt(behandling,
            AksjonspunktDefinisjon.AVKLAR_AKTIVITETER).setStatus(AksjonspunktStatus.UTFØRT, "Ferdig");
        var stp = LocalDate.of(2025,6,1);

        var bg = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(stp)
            .leggTilAktivitetStatus(lagAktivitetstatus(AktivitetStatus.FRILANSER))
            .leggTilAktivitetStatus(lagAktivitetstatus(AktivitetStatus.ARBEIDSAVKLARINGSPENGER))
            .build();
        var register = BeregningAktivitetAggregat.builder()
            .leggTilAktivitet(lagAktivitet(OpptjeningAktivitetType.FRILANS, null))
            .leggTilAktivitet(lagAktivitet(OpptjeningAktivitetType.ARBEIDSAVKLARING, null))
            .medSkjæringstidspunktOpptjening(stp)
            .build();
        var saksbehandlet = BeregningAktivitetAggregat.builder()
            .leggTilAktivitet(lagAktivitet(OpptjeningAktivitetType.ARBEIDSAVKLARING, null))
            .medSkjæringstidspunktOpptjening(stp)
            .build();
        var gr = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(bg).medRegisterAktiviteter(register).medSaksbehandletAktiviteter(saksbehandlet).build(BeregningsgrunnlagTilstand.FASTSATT);
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(1L)).thenReturn(List.of(behandling));
        when(beregningTjeneste.hent(any())).thenReturn(Optional.of(gr));
        var erPåvirket = fullAapAnnenStatusLoggTjeneste.erPåvirketAvPraksisendring(1L);

        assertThat(erPåvirket).isFalse();
    }

    @Test
    void skal_revurdere_sak_dersom_ingen_inntekt_I_register_men_finnes_i_inntektsmelding() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        AksjonspunktTestSupport.leggTilAksjonspunkt(behandling,
            AksjonspunktDefinisjon.AVKLAR_AKTIVITETER).setStatus(AksjonspunktStatus.UTFØRT, "Ferdig");
        var stp = LocalDate.of(2025,6,1);

        var iayBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var inntektBuilder = InntektBuilder.oppdatere(Optional.empty())
            .medInntektsKilde(InntektsKilde.INNTEKT_BEREGNING)
            .medArbeidsgiver(Arbeidsgiver.virksomhet("999999999"))
            .leggTilInntektspost(InntektspostBuilder.ny().medPeriode(stp.minusMonths(3), stp.minusMonths(1)).medBeløp(BigDecimal.valueOf(0)));
        var aktørInntektBuilder = iayBuilder.getAktørInntektBuilder(behandling.getAktørId())
            .leggTilInntekt(inntektBuilder);
        var grIay = iayBuilder.leggTilAktørArbeid(
                InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty()).medAktørId(behandling.getAktørId()))
            .leggTilAktørInntekt(aktørInntektBuilder);
        var iay = InntektArbeidYtelseGrunnlagBuilder.nytt().medData(grIay)
            .medInntektsmeldinger(List.of(InntektsmeldingBuilder.builder().medArbeidsgiver(Arbeidsgiver.virksomhet("999999999")).medBeløp(BigDecimal.valueOf(1)).build()))
            .build();
        var bg = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(stp)
            .leggTilAktivitetStatus(lagAktivitetstatus(AktivitetStatus.ARBEIDSAVKLARINGSPENGER))
            .build();
        var register = BeregningAktivitetAggregat.builder()
            .leggTilAktivitet(lagAktivitet(OpptjeningAktivitetType.ARBEID, "999999999"))
            .leggTilAktivitet(lagAktivitet(OpptjeningAktivitetType.ARBEIDSAVKLARING, null))
            .medSkjæringstidspunktOpptjening(stp)
            .build();
        var saksbehandlet = BeregningAktivitetAggregat.builder()
            .leggTilAktivitet(lagAktivitet(OpptjeningAktivitetType.ARBEIDSAVKLARING, null))
            .medSkjæringstidspunktOpptjening(stp)
            .build();
        var gr = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(bg).medRegisterAktiviteter(register).medSaksbehandletAktiviteter(saksbehandlet).build(BeregningsgrunnlagTilstand.FASTSATT);
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(1L)).thenReturn(List.of(behandling));
        when(beregningTjeneste.hent(any())).thenReturn(Optional.of(gr));
        when(iayTjeneste.finnGrunnlag(any())).thenReturn(Optional.of(iay));
        var erPåvirket = fullAapAnnenStatusLoggTjeneste.erPåvirketAvPraksisendring(1L);

        assertThat(erPåvirket).isTrue();
    }

    private BeregningAktivitet lagAktivitet(OpptjeningAktivitetType opptjeningAktivitetType, String orgnr) {
        return BeregningAktivitet.builder().medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE)).medOpptjeningAktivitetType(opptjeningAktivitetType).medArbeidsgiver(orgnr == null ? null : Arbeidsgiver.virksomhet(orgnr)).build();
    }

    private static BeregningsgrunnlagAktivitetStatus lagAktivitetstatus(AktivitetStatus arbeidstaker) {
        return BeregningsgrunnlagAktivitetStatus.builder()
            .medAktivitetStatus(arbeidstaker)
            .medHjemmel(Hjemmel.F_14_7)
            .build();
    }

}
