package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.ytelse.beregning.rest.VurderTilbaketrekkDto;

public class VurderTilbaketrekkOppdatererTest extends EntityManagerAwareTest {

    private VurderTilbaketrekkOppdaterer vurderTilbaketrekkOppdaterer;
    private BeregningsresultatRepository beregningsresultatRepository;

    @BeforeEach
    public void setup() {
        var repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        var historikkAdapter = new HistorikkTjenesteAdapter(new HistorikkRepository(getEntityManager()), null);
        vurderTilbaketrekkOppdaterer = new VurderTilbaketrekkOppdaterer(repositoryProvider, historikkAdapter);
        beregningsresultatRepository = new BeregningsresultatRepository(getEntityManager());
    }

    @Test
    public void skal_teste_at_oppdatering_gjøres_riktig_dersom_tilbaketrekk_skal_utføres() {
        // Arrange
        Behandling behandling = opprettBehandling();
        VurderTilbaketrekkDto dto = new VurderTilbaketrekkDto("Begrunnelse", false);

        // Act
        vurderTilbaketrekkOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));
        Optional<BehandlingBeregningsresultatEntitet> test = beregningsresultatRepository.hentBeregningsresultatAggregat(behandling.getId());

        // Assert
        assertThat(test).isPresent();
        assertThat(test.get().skalHindreTilbaketrekk().orElseThrow()).isFalse();
    }

    private Behandling opprettBehandling() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(new BehandlingRepositoryProvider(getEntityManager()));
        opprettBehandlingsresultat(behandling);
        return behandling;
    }

    @Test
    public void skal_teste_at_oppdatering_gjøres_riktig_dersom_tilbaketrekk_ikke_skal_utføres() {
        // Arrange
        Behandling behandling = opprettBehandling();
        VurderTilbaketrekkDto dto = new VurderTilbaketrekkDto("Begrunnelse", true);

        // Act
        vurderTilbaketrekkOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));
        Optional<BehandlingBeregningsresultatEntitet> test = beregningsresultatRepository.hentBeregningsresultatAggregat(behandling.getId());

        // Assert
        assertThat(test).isPresent();
        assertThat(test.get().skalHindreTilbaketrekk().orElseThrow()).isTrue();
    }

    private void buildBeregningsresultatAndel(BeregningsresultatPeriode beregningsresultatPeriode) {
        BeregningsresultatAndel.builder()
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

    private void opprettBehandlingsresultat(Behandling behandling) {
        BeregningsresultatEntitet.Builder builder = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2");
        BeregningsresultatEntitet beregningsresultat = builder.build();
        BeregningsresultatPeriode brPeriode = buildBeregningsresultatPeriode(beregningsresultat);
        buildBeregningsresultatAndel(brPeriode);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);
    }

}
