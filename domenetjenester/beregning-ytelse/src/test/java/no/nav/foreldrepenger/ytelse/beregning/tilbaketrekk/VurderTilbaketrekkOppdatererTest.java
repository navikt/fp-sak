package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
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
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.ytelse.beregning.rest.VurderTilbaketrekkDto;

@ExtendWith(JpaExtension.class)
public class VurderTilbaketrekkOppdatererTest {

    private VurderTilbaketrekkOppdaterer vurderTilbaketrekkOppdaterer;
    private BeregningsresultatRepository beregningsresultatRepository;
    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    public void setup(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var historikkAdapter = new HistorikkTjenesteAdapter(new HistorikkRepository(entityManager), null,
                repositoryProvider.getBehandlingRepository());
        vurderTilbaketrekkOppdaterer = new VurderTilbaketrekkOppdaterer(repositoryProvider, historikkAdapter);
        beregningsresultatRepository = new BeregningsresultatRepository(entityManager);
    }

    @Test
    public void skal_teste_at_oppdatering_gjøres_riktig_dersom_tilbaketrekk_skal_utføres() {
        // Arrange
        var behandling = opprettBehandling();
        var dto = new VurderTilbaketrekkDto("Begrunnelse", false);

        // Act
        vurderTilbaketrekkOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));
        var test = beregningsresultatRepository.hentBeregningsresultatAggregat(behandling.getId());

        // Assert
        assertThat(test).isPresent();
        assertThat(test.get().skalHindreTilbaketrekk().orElseThrow()).isFalse();
    }

    private Behandling opprettBehandling() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        opprettBehandlingsresultat(behandling);
        return behandling;
    }

    @Test
    public void skal_teste_at_oppdatering_gjøres_riktig_dersom_tilbaketrekk_ikke_skal_utføres() {
        // Arrange
        var behandling = opprettBehandling();
        var dto = new VurderTilbaketrekkDto("Begrunnelse", true);

        // Act
        vurderTilbaketrekkOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));
        var test = beregningsresultatRepository.hentBeregningsresultatAggregat(behandling.getId());

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
        var builder = BeregningsresultatEntitet.builder()
                .medRegelInput("clob1")
                .medRegelSporing("clob2");
        var beregningsresultat = builder.build();
        var brPeriode = buildBeregningsresultatPeriode(beregningsresultat);
        buildBeregningsresultatAndel(brPeriode);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);
    }

}
