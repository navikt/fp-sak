package no.nav.foreldrepenger.domene.mappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.inject.Instance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.MapKalkulusYtelsegrunnlag;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.MapKalkulusYtelsegrunnlagFP;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktiviteter;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningForBeregningTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(MockitoExtension.class)
class KalkulusInputTjenesteTest {
    private KalkulusInputTjeneste kalkulusInputTjeneste;

    @Mock
    private Instance<MapKalkulusYtelsegrunnlag> ytelsegrunnlagMappere;
    @Mock
    private InntektArbeidYtelseTjeneste iayTjeneste;
    @Mock
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    @Mock
    private OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste;
    @Mock
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    @Mock
    private MapKalkulusYtelsegrunnlagFP mapKalkulusYtelsegrunnlagFP;
    private Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();

    @BeforeEach
    void before() {
        ytelsegrunnlagMappere = new UnitTestLookupInstanceImpl<>(mapKalkulusYtelsegrunnlagFP);
        kalkulusInputTjeneste = new KalkulusInputTjeneste(ytelsegrunnlagMappere, iayTjeneste, skjæringstidspunktTjeneste, opptjeningForBeregningTjeneste, inntektsmeldingTjeneste);
    }

    @Test
    void skal_hente_alle_inntektsmeldinger_for_sak_under_mapping_av_kravperioder() {
        // Arrange
        var stpDato = LocalDate.of(2025,6,1);
        var iayGrunnlag = mock(InntektArbeidYtelseGrunnlag.class);
        var opptjeningAktiviteter = mock(OpptjeningAktiviteter.class);
        var gammelIM = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("999999999"))
            .medInnsendingstidspunkt(LocalDateTime.now().minusMonths(3))
            .medBeløp(BigDecimal.TEN)
            .medRefusjon(BigDecimal.TEN)
            .build();
        var ag = Arbeidsgiver.virksomhet("999999999");
        var nyIM = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(ag)
            .medInnsendingstidspunkt(LocalDateTime.now())
            .medBeløp(BigDecimal.TEN)
            .medRefusjon(BigDecimal.TEN)
            .build();

        var ref = BehandlingReferanse.fra(behandling);
        var stp = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(stpDato).medSkjæringstidspunktOpptjening(stpDato).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any()))
            .thenReturn(stp);
        when(iayTjeneste.hentGrunnlag(anyLong())).thenReturn(iayGrunnlag);
        when(opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(any(), any(), any())).thenReturn(Optional.of(opptjeningAktiviteter));
        when(inntektsmeldingTjeneste.hentAlleInntektsmeldingerForFagsak(any())).thenReturn(List.of(gammelIM, nyIM));
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(ref, stp.getUtledetSkjæringstidspunkt(), iayGrunnlag, true)).thenReturn(List.of(nyIM));
        when(iayGrunnlag.getInntektsmeldinger()).thenReturn(Optional.of(new InntektsmeldingAggregat(List.of(nyIM))));
        var arbeid = mock(AktørArbeid.class);
        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aaBuilder = yaBuilder.getAktivitetsAvtaleBuilder();
        var aa = aaBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE));
        yaBuilder.leggTilAktivitetsAvtale(aa)
            .medArbeidsgiver(ag)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        when(arbeid.hentAlleYrkesaktiviteter()).thenReturn(List.of(yaBuilder.build()));

        when(iayGrunnlag.getAktørArbeidFraRegister(any())).thenReturn(Optional.of(arbeid));
        // Act
        var input = kalkulusInputTjeneste.lagKalkulusInput(ref);

        // Assert
        assertThat(input).isNotNull();
        assertThat(input.getRefusjonskravPrArbeidsforhold()).isNotEmpty();
        assertThat(input.getRefusjonskravPrArbeidsforhold().getFirst().getSisteSøktePerioder().getInnsendingsdato()).isEqualTo(nyIM.getInnsendingstidspunkt().toLocalDate());
        assertThat(input.getRefusjonskravPrArbeidsforhold().getFirst().getAlleSøktePerioder()).hasSize(2);
    }


}
