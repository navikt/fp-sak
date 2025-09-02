package no.nav.foreldrepenger.domene.prosess;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.modell.kodeverk.Hjemmel;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

@ExtendWith(MockitoExtension.class)
class FullAapAnnenStatusLoggTjenesteTest {

    @Mock
    private BeregningTjeneste beregningTjeneste;
    @Mock
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private FullAapAnnenStatusLoggTjeneste fullAapAnnenStatusLoggTjeneste;

    @BeforeEach
    void setUp() {
        fullAapAnnenStatusLoggTjeneste = new FullAapAnnenStatusLoggTjeneste(beregningTjeneste, iayTjeneste);
    }

    @Test
    void skal_logge_hvis_det_er_treff() {
        var stp = LocalDate.of(2025,6,1);
        var AT = BeregningsgrunnlagAktivitetStatus.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medHjemmel(Hjemmel.F_14_7)
            .build();
        var AAP = BeregningsgrunnlagAktivitetStatus.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSAVKLARINGSPENGER)
            .medHjemmel(Hjemmel.F_14_7)
            .build();
        var bg = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(stp)
            .leggTilAktivitetStatus(AT)
            .leggTilAktivitetStatus(AAP)
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

}
