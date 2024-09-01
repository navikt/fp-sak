package no.nav.foreldrepenger.domene.rest.historikk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.entiteter.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.Inntektskategori;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBeregningsgrunnlagATFLDto;
import no.nav.foreldrepenger.domene.rest.dto.InntektPrAndelDto;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ExtendWith(JpaExtension.class)
class FastsettBeregningsgrunnlagATFLHistorikkTjenesteTest {
    private static final String NAV_ORGNR = "889640782";
    private static final BigDecimal GRUNNBELØP = BigDecimal.valueOf(90000);
    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.namedRef("TEST-REF");
    private static final int BRUTTO_PR_AR = 150000;
    private static final int OVERSTYRT_PR_AR = 200000;
    private static final int FRILANSER_INNTEKT = 4000;

    private BehandlingRepositoryProvider repositoryProvider;

    private final HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();
    private final VirksomhetTjeneste virksomhetTjeneste = mock(VirksomhetTjeneste.class);

    private final InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste = mock(InntektArbeidYtelseTjeneste.class);

    private FastsettBeregningsgrunnlagATFLHistorikkTjeneste fastsettBeregningsgrunnlagATFLHistorikkTjeneste;

    private Behandling behandling;
    private Virksomhet virk;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var arbeidsgiverHistorikkinnslagTjeneste = new ArbeidsgiverHistorikkinnslag(
            new ArbeidsgiverTjeneste(null, virksomhetTjeneste));
        when(inntektArbeidYtelseTjeneste.hentGrunnlag(anyLong())).thenReturn(
            InntektArbeidYtelseGrunnlagBuilder.nytt().build());
        fastsettBeregningsgrunnlagATFLHistorikkTjeneste = new FastsettBeregningsgrunnlagATFLHistorikkTjeneste(
            lagMockHistory(), arbeidsgiverHistorikkinnslagTjeneste, inntektArbeidYtelseTjeneste);
        virk = new Virksomhet.Builder().medOrgnr(NAV_ORGNR).medNavn("AF1").build();
        when(virksomhetTjeneste.hentOrganisasjon(NAV_ORGNR)).thenReturn(Virksomhet.getBuilder().medOrgnr(NAV_ORGNR).medNavn("AF1").build());
    }

    @Test
    void skal_generere_historikkinnslag_ved_fastsettelse_av_brutto_beregningsgrunnlag_AT() {
        // Arrange
        var bg = buildOgLagreBeregningsgrunnlag(false);

        //Dto
        var dto = new FastsettBeregningsgrunnlagATFLDto("begrunnelse",
            Collections.singletonList(new InntektPrAndelDto(OVERSTYRT_PR_AR, 1L)), null);

        // Act
        fastsettBeregningsgrunnlagATFLHistorikkTjeneste.lagHistorikk(
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto), dto, bg);
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        var historikkInnslag = tekstBuilder.build(historikkinnslag);

        // Assert
        assertThat(historikkInnslag).hasSize(1);

        var del = historikkInnslag.get(0);
        var feltList = del.getEndredeFelt();
        assertThat(feltList).hasSize(1);
        assertThat(feltList.get(0)).satisfies(felt -> {
            assertThat(felt.getNavn()).as("navn")
                .isEqualTo(HistorikkEndretFeltType.INNTEKT_FRA_ARBEIDSFORHOLD.getKode());
            assertThat(felt.getFraVerdi()).as("fraVerdi").isNull();
            assertThat(felt.getTilVerdi()).as("tilVerdi").isEqualTo("200000");
        });
        assertThat(del.getBegrunnelse()).hasValueSatisfying(
            begrunnelse -> assertThat(begrunnelse).isEqualTo("begrunnelse"));
    }

    @Test
    void skal_generere_historikkinnslag_ved_fastsettelse_av_brutto_beregningsgrunnlag_FL() {
        // Arrange
        var bg = buildOgLagreBeregningsgrunnlag(true);

        //Dto
        var dto = new FastsettBeregningsgrunnlagATFLDto("begrunnelse", FRILANSER_INNTEKT);

        // Act
        fastsettBeregningsgrunnlagATFLHistorikkTjeneste.lagHistorikk(
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto), dto, bg);
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        var historikkInnslag = tekstBuilder.build(historikkinnslag);

        // Assert
        assertThat(historikkInnslag).hasSize(1);

        var del = historikkInnslag.get(0);
        var feltList = del.getEndredeFelt();
        assertThat(feltList).hasSize(1);
        assertThat(feltList.get(0)).satisfies(felt -> {
            assertThat(felt.getNavn()).as("navn").isEqualTo(HistorikkEndretFeltType.FRILANS_INNTEKT.getKode());
            assertThat(felt.getFraVerdi()).as("fraVerdi").isNull();
            assertThat(felt.getTilVerdi()).as("tilVerdi").isEqualTo("4000");
        });
        assertThat(del.getBegrunnelse()).hasValueSatisfying(
            begrunnelse -> assertThat(begrunnelse).isEqualTo("begrunnelse"));
    }

    private HistorikkTjenesteAdapter lagMockHistory() {
        var mockHistory = mock(HistorikkTjenesteAdapter.class);
        when(mockHistory.tekstBuilder()).thenReturn(tekstBuilder);
        return mockHistory;
    }

    private BeregningsgrunnlagEntitet buildOgLagreBeregningsgrunnlag(boolean erFrilans) {
        AbstractTestScenario<?> scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        behandling = scenario.lagre(repositoryProvider);
        var beregningsgrunnlagBuilder = BeregningsgrunnlagEntitet.ny()
            .medGrunnbeløp(GRUNNBELØP)
            .medSkjæringstidspunkt(LocalDate.now().minusDays(5));

        var fom = LocalDate.now().minusDays(20);
        leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagBuilder, fom, erFrilans);

        return beregningsgrunnlagBuilder.build();
    }

    private void leggTilBeregningsgrunnlagPeriode(BeregningsgrunnlagEntitet.Builder beregningsgrunnlagBuilder,
                                                  LocalDate fomDato,
                                                  boolean erFrilans) {
        var beregningsgrunnlagPeriodeBuilder = BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(fomDato, null);
        leggTilBeregningsgrunnlagPrStatusOgAndel(beregningsgrunnlagPeriodeBuilder, virk, erFrilans);
        beregningsgrunnlagBuilder.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriodeBuilder);
    }

    private void leggTilBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPeriode.Builder beregningsgrunnlagPeriodeBuilder,
                                                          Virksomhet virksomheten, boolean erFrilans) {

        var builder = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(1L)
            .medInntektskategori(erFrilans ? Inntektskategori.FRILANSER : Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(erFrilans ? AktivitetStatus.FRILANSER : AktivitetStatus.ARBEIDSTAKER)
            .medBeregnetPrÅr(BigDecimal.valueOf(BRUTTO_PR_AR));
        if (virksomheten != null && !erFrilans) {
            var bga = BGAndelArbeidsforhold.builder()
                .medArbeidsforholdRef(FastsettBeregningsgrunnlagATFLHistorikkTjenesteTest.ARBEIDSFORHOLD_ID)
                .medArbeidsgiver(Arbeidsgiver.virksomhet(virksomheten.getOrgnr()))
                .medArbeidsperiodeFom(LocalDate.now().minusYears(1))
                .medArbeidsperiodeTom(LocalDate.now().plusYears(2));
            builder.medBGAndelArbeidsforhold(bga);
        }
        beregningsgrunnlagPeriodeBuilder.leggTilBeregningsgrunnlagPrStatusOgAndel(builder);
    }
}
