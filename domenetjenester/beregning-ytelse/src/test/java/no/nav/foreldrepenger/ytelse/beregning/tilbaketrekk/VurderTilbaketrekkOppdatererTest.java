package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.historikk.dto.HistorikkInnslagKonverter;
import no.nav.foreldrepenger.ytelse.beregning.rest.VurderTilbaketrekkDto;

public class VurderTilbaketrekkOppdatererTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private Behandling behandling;
    private HistorikkTjenesteAdapter historikkAdapter;
    private VurderTilbaketrekkOppdaterer vurderTilbaketrekkOppdaterer;
    private BeregningsresultatRepository beregningsresultatRepository;

    @SuppressWarnings("unused")
    private BeregningsresultatEntitet beregningsresultat;

    @Before
    public void setup() {
        HistorikkInnslagKonverter historikkInnslagKonverter = new HistorikkInnslagKonverter();
        historikkAdapter = new HistorikkTjenesteAdapter(repositoryProvider.getHistorikkRepository(), historikkInnslagKonverter, null);
        vurderTilbaketrekkOppdaterer = new VurderTilbaketrekkOppdaterer(repositoryProvider, historikkAdapter);
        beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        behandling = scenario.lagre(repositoryProvider);
        beregningsresultat = buildBeregningsresultatFP();
    }

    @Test
    public void skal_teste_at_oppdatering_gjøres_riktig_dersom_tilbaketrekk_skal_utføres() {
        // Arrange
        VurderTilbaketrekkDto dto = new VurderTilbaketrekkDto("Begrunnelse", false);

        // Act
        vurderTilbaketrekkOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));
        Optional<BehandlingBeregningsresultatEntitet> test = beregningsresultatRepository.hentBeregningsresultatAggregat(behandling.getId());

        // Assert
        assertThat(test).isPresent();
        assertThat(test.get().skalHindreTilbaketrekk().get()).isFalse();
    }

    @Test
    public void skal_teste_at_oppdatering_gjøres_riktig_dersom_tilbaketrekk_ikke_skal_utføres() {
        // Arrange
        VurderTilbaketrekkDto dto = new VurderTilbaketrekkDto("Begrunnelse", true);

        // Act
        vurderTilbaketrekkOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));
        Optional<BehandlingBeregningsresultatEntitet> test = beregningsresultatRepository.hentBeregningsresultatAggregat(behandling.getId());

        // Assert
        assertThat(test).isPresent();
        assertThat(test.get().skalHindreTilbaketrekk().get()).isTrue();
    }


    private BeregningsresultatAndel buildBeregningsresultatAndel(BeregningsresultatPeriode beregningsresultatPeriode) {
        return BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medArbeidsgiver(Arbeidsgiver.person(AktørId.dummy()))
            .medDagsats(2160)
            .medDagsatsFraBg(2160)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .build(beregningsresultatPeriode);
    }

    private BeregningsresultatPeriode buildBeregningsresultatPeriode(BeregningsresultatEntitet beregningsresultat) {
        return BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(LocalDate.now().minusDays(20), LocalDate.now().minusDays(15))
            .build(beregningsresultat);
    }

    private BeregningsresultatEntitet buildBeregningsresultatFP() {
        BeregningsresultatEntitet.Builder builder = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2");
        BeregningsresultatEntitet beregningsresultat = builder.build();
        BeregningsresultatPeriode brPeriode = buildBeregningsresultatPeriode(beregningsresultat);
        buildBeregningsresultatAndel(brPeriode);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);
        return beregningsresultat;
    }

}
