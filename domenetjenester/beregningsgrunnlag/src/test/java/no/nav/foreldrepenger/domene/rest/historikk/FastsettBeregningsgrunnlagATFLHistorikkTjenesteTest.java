package no.nav.foreldrepenger.domene.rest.historikk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.entiteter.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.modell.kodeverk.Inntektskategori;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBeregningsgrunnlagATFLDto;
import no.nav.foreldrepenger.domene.rest.dto.InntektPrAndelDto;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@CdiDbAwareTest
class FastsettBeregningsgrunnlagATFLHistorikkTjenesteTest {
    private static final String NAV_ORGNR = "889640782";
    private static final String ARBEIDSGIVER_NAVN = "AF1";
    private static final Virksomhet VIRKSOMHET = new Virksomhet.Builder().medOrgnr(NAV_ORGNR).medNavn(ARBEIDSGIVER_NAVN).build();
    private static final BigDecimal GRUNNBELØP = BigDecimal.valueOf(90000);
    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.namedRef("TEST-REF");
    private static final int BRUTTO_PR_AR = 150000;
    private static final int OVERSTYRT_PR_AR = 200000;
    private static final int FRILANSER_INNTEKT = 4000;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    @Test
    void skal_generere_historikkinnslag_ved_fastsettelse_av_brutto_beregningsgrunnlag_AT() {
        var arbeidsgiverTjeneste = mock(ArbeidsgiverTjeneste.class);
        when(arbeidsgiverTjeneste.hent(any())).thenReturn(new ArbeidsgiverOpplysninger(NAV_ORGNR, ARBEIDSGIVER_NAVN));
        var inntektArbeidYtelseTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        var fastsettBeregningsgrunnlagATFLHistorikkTjeneste = new FastsettBeregningsgrunnlagATFLHistorikkTjeneste(
            new ArbeidsgiverHistorikkinnslag(arbeidsgiverTjeneste), inntektArbeidYtelseTjeneste, repositoryProvider.getHistorikkinnslag2Repository());

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandling.getId(),
            InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER));

        // Arrange
        var bg = buildOgLagreBeregningsgrunnlag(behandling, false);

        //Dto
        var dto = new FastsettBeregningsgrunnlagATFLDto("begrunnelse", Collections.singletonList(new InntektPrAndelDto(OVERSTYRT_PR_AR, 1L)), null);

        // Act
        var ref = BehandlingReferanse.fra(behandling);
        fastsettBeregningsgrunnlagATFLHistorikkTjeneste.lagHistorikk(new AksjonspunktOppdaterParameter(ref, dto), dto, bg);

        // Assert
        var historikkinnslag = repositoryProvider.getHistorikkinnslag2Repository().hent(behandling.getId()).getFirst();
        assertThat(historikkinnslag.getSkjermlenke()).isEqualTo(SkjermlenkeType.BEREGNING_FORELDREPENGER);
        assertThat(historikkinnslag.getLinjer()).hasSize(3);
        assertThat(historikkinnslag.getLinjer().getFirst().getTekst()).isEqualTo("Grunnlag for beregnet årsinntekt:");
        assertThat(historikkinnslag.getLinjer().get(1).getTekst()).isEqualTo("__Inntekt fra AF1 (889640782) ...050d__ er satt til __200000__.");
        assertThat(historikkinnslag.getLinjer().get(2).getTekst()).contains(dto.getBegrunnelse());
    }

    @Test
    void skal_generere_historikkinnslag_ved_fastsettelse_av_brutto_beregningsgrunnlag_FL() {
        var inntektArbeidYtelseTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        var fastsettBeregningsgrunnlagATFLHistorikkTjeneste = new FastsettBeregningsgrunnlagATFLHistorikkTjeneste(
            new ArbeidsgiverHistorikkinnslag(mock(ArbeidsgiverTjeneste.class)), inntektArbeidYtelseTjeneste, repositoryProvider.getHistorikkinnslag2Repository());

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandling.getId(),
            InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER));

        // Arrange
        var bg = buildOgLagreBeregningsgrunnlag(behandling, true);

        //Dto
        var dto = new FastsettBeregningsgrunnlagATFLDto("begrunnelse", FRILANSER_INNTEKT);

        // Act
        var ref = BehandlingReferanse.fra(behandling);
        fastsettBeregningsgrunnlagATFLHistorikkTjeneste.lagHistorikk(new AksjonspunktOppdaterParameter(ref, dto), dto, bg);

        // Assert
        var historikkinnslag = repositoryProvider.getHistorikkinnslag2Repository().hent(behandling.getId()).getFirst();
        assertThat(historikkinnslag.getSkjermlenke()).isEqualTo(SkjermlenkeType.BEREGNING_FORELDREPENGER);
        assertThat(historikkinnslag.getLinjer()).hasSize(3);
        assertThat(historikkinnslag.getLinjer().getFirst().getTekst()).isEqualTo("Grunnlag for beregnet årsinntekt:");
        assertThat(historikkinnslag.getLinjer().get(1).getTekst()).isEqualTo("__Frilansinntekt__ er satt til __4000__.");
        assertThat(historikkinnslag.getLinjer().get(2).getTekst()).contains(dto.getBegrunnelse());

    }

    private BeregningsgrunnlagEntitet buildOgLagreBeregningsgrunnlag(Behandling behandling, boolean erFrilans) {
        var beregningsgrunnlagBuilder = BeregningsgrunnlagEntitet.ny().medGrunnbeløp(GRUNNBELØP).medSkjæringstidspunkt(LocalDate.now().minusDays(5));

        var fom = LocalDate.now().minusDays(20);
        leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagBuilder, fom, erFrilans);

        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlagBuilder.build(), BeregningsgrunnlagTilstand.FASTSATT);

        return beregningsgrunnlagBuilder.build();
    }

    private void leggTilBeregningsgrunnlagPeriode(BeregningsgrunnlagEntitet.Builder beregningsgrunnlagBuilder, LocalDate fomDato, boolean erFrilans) {
        var beregningsgrunnlagPeriodeBuilder = BeregningsgrunnlagPeriode.ny().medBeregningsgrunnlagPeriode(fomDato, null);
        leggTilBeregningsgrunnlagPrStatusOgAndel(beregningsgrunnlagPeriodeBuilder, erFrilans);
        beregningsgrunnlagBuilder.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriodeBuilder);
    }

    private void leggTilBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPeriode.Builder beregningsgrunnlagPeriodeBuilder, boolean erFrilans) {

        var builder = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(1L)
            .medInntektskategori(erFrilans ? Inntektskategori.FRILANSER : Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(erFrilans ? AktivitetStatus.FRILANSER : AktivitetStatus.ARBEIDSTAKER)
            .medBeregnetPrÅr(BigDecimal.valueOf(BRUTTO_PR_AR));
        if (!erFrilans) {
            var bga = BGAndelArbeidsforhold.builder()
                .medArbeidsforholdRef(FastsettBeregningsgrunnlagATFLHistorikkTjenesteTest.ARBEIDSFORHOLD_ID)
                .medArbeidsgiver(Arbeidsgiver.virksomhet(VIRKSOMHET.getOrgnr()))
                .medArbeidsperiodeFom(LocalDate.now().minusYears(1))
                .medArbeidsperiodeTom(LocalDate.now().plusYears(2));
            builder.medBGAndelArbeidsforhold(bga);
        }
        beregningsgrunnlagPeriodeBuilder.leggTilBeregningsgrunnlagPrStatusOgAndel(builder);
    }
}
