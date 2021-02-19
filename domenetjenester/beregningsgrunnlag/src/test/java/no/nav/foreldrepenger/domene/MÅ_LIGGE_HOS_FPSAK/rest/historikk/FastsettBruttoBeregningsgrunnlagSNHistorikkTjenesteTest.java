package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.FastsettBruttoBeregningsgrunnlagSNDto;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.VurderVarigEndringEllerNyoppstartetSNDto;
import no.nav.foreldrepenger.domene.modell.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.Inntektskategori;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class FastsettBruttoBeregningsgrunnlagSNHistorikkTjenesteTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private static final int BRUTTO_BG = 200000;

    private final HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();
    private FastsettBruttoBeregningsgrunnlagSNHistorikkTjeneste fastsettBruttoBeregningsgrunnlagSNHistorikkTjeneste;
    private VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste vurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste;


    @BeforeEach
    void setUp(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        fastsettBruttoBeregningsgrunnlagSNHistorikkTjeneste = new FastsettBruttoBeregningsgrunnlagSNHistorikkTjeneste(lagMockHistory());
        vurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste = new VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste(lagMockHistory());
    }

    @Test
    public void skal_generere_historikkinnslag_ved_fastsettelse_av_brutto_beregningsgrunnlag_SN() {
        // Arrange
        boolean varigEndring = true;
        var behandling = buildOgLagreBeregningsgrunnlag();

        //Dto
        var vurderVarigEndringEllerNyoppstartetSNDto = new VurderVarigEndringEllerNyoppstartetSNDto("begrunnelse1", varigEndring);
        var fastsettBGDto = new FastsettBruttoBeregningsgrunnlagSNDto("begrunnelse2", BRUTTO_BG);

        // Act
        vurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste.lagHistorikkInnslag(new AksjonspunktOppdaterParameter(behandling, Optional.empty(), vurderVarigEndringEllerNyoppstartetSNDto), vurderVarigEndringEllerNyoppstartetSNDto);
        fastsettBruttoBeregningsgrunnlagSNHistorikkTjeneste.lagHistorikk(new AksjonspunktOppdaterParameter(behandling, Optional.empty(), fastsettBGDto), fastsettBGDto);

        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        List<HistorikkinnslagDel> historikkInnslag = tekstBuilder.medHendelse(HistorikkinnslagType.FAKTA_ENDRET).build(historikkinnslag);

        // Assert
        assertThat(historikkInnslag).hasSize(2);

        HistorikkinnslagDel del1 = historikkInnslag.get(0);
        assertThat(del1.getEndredeFelt()).hasSize(1);
        assertHistorikkinnslagFelt(del1, HistorikkEndretFeltType.ENDRING_NAERING, null, HistorikkEndretFeltVerdiType.VARIG_ENDRET_NAERING.getKode());
        assertThat(del1.getBegrunnelse()).as("begrunnelse").hasValueSatisfying(begrunnelse -> assertThat(begrunnelse).isEqualTo("begrunnelse1"));
        assertThat(del1.getSkjermlenke()).as("skjermlenkeOpt").hasValueSatisfying(skjermlenke ->
            assertThat(skjermlenke).as("skjermlenke1").isEqualTo(SkjermlenkeType.BEREGNING_FORELDREPENGER.getKode()));

        HistorikkinnslagDel del2 = historikkInnslag.get(1);
        assertThat(del2.getEndredeFelt()).hasSize(1);
        assertHistorikkinnslagFelt(del2, HistorikkEndretFeltType.BRUTTO_NAERINGSINNTEKT, null, "200000");
        assertThat(del2.getBegrunnelse()).as("begrunnelse").hasValueSatisfying(begrunnelse -> assertThat(begrunnelse).isEqualTo("begrunnelse2"));
        assertThat(del2.getSkjermlenke()).as("skjermlenke2").isNotPresent();
    }

    private void assertHistorikkinnslagFelt(HistorikkinnslagDel del, HistorikkEndretFeltType historikkEndretFeltType, String fraVerdi, String tilVerdi) {
        Optional<HistorikkinnslagFelt> feltOpt = del.getEndretFelt(historikkEndretFeltType);
        String feltNavn = historikkEndretFeltType.getKode();
        assertThat(feltOpt).hasValueSatisfying(felt -> {
            assertThat(felt.getNavn()).as(feltNavn + ".navn").isEqualTo(feltNavn);
            assertThat(felt.getFraVerdi()).as(feltNavn + ".fraVerdi").isEqualTo(fraVerdi);
            assertThat(felt.getTilVerdi()).as(feltNavn + ".tilVerdi").isEqualTo(tilVerdi);
        });
    }

    private HistorikkTjenesteAdapter lagMockHistory() {
        HistorikkTjenesteAdapter mockHistory = Mockito.mock(HistorikkTjenesteAdapter.class);
        Mockito.when(mockHistory.tekstBuilder()).thenReturn(tekstBuilder);
        return mockHistory;
    }

    private Behandling buildOgLagreBeregningsgrunnlag() {
        AbstractTestScenario<?> scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        BeregningsgrunnlagEntitet.Builder beregningsgrunnlagBuilder = BeregningsgrunnlagEntitet.ny()
            .medGrunnbeløp(BigDecimal.ONE)
            .medSkjæringstidspunkt(LocalDate.now().minusDays(5));

        leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagBuilder, LocalDate.now());

        return behandling;
    }

    private void leggTilBeregningsgrunnlagPeriode(BeregningsgrunnlagEntitet.Builder beregningsgrunnlagBuilder, LocalDate fomDato) {
        BeregningsgrunnlagPeriode.Builder beregningsgrunnlagPeriodeBuilder = BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(fomDato, null);
        leggTilBeregningsgrunnlagPrStatusOgAndel(beregningsgrunnlagPeriodeBuilder);
        beregningsgrunnlagBuilder.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriodeBuilder);
    }

    private void leggTilBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPeriode.Builder beregningsgrunnlagPeriodeBuilder) {

        BeregningsgrunnlagPrStatusOgAndel.Builder builder = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(1L)
            .medInntektskategori(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medBeregnetPrÅr(BigDecimal.valueOf(30000));
        beregningsgrunnlagPeriodeBuilder.leggTilBeregningsgrunnlagPrStatusOgAndel(
            builder);
    }
}
