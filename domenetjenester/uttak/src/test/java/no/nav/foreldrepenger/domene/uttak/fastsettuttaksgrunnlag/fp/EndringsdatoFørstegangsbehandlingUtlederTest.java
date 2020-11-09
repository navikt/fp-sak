package no.nav.foreldrepenger.domene.uttak.fastsettuttaksgrunnlag.fp;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class EndringsdatoFørstegangsbehandlingUtlederTest {

    private static final String ORGNR = KUNSTIG_ORG;

    private static final LocalDate FØRSTE_UTTAKSDATO_OPPGITT = LocalDate.now().plusDays(10);

    private UttakRepositoryProvider repositoryProvider;

    private EndringsdatoFørstegangsbehandlingUtleder endringsdatoFørstegangsbehandlingUtleder;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        repositoryProvider = new UttakRepositoryProvider(entityManager);
        endringsdatoFørstegangsbehandlingUtleder = new EndringsdatoFørstegangsbehandlingUtleder(repositoryProvider.getYtelsesFordelingRepository());
    }

    @Test
    public void skal_utlede_at_endringsdatoen_er_første_uttaksdato_i_søknaden_når_det_ikke_finnes_manuell_vurdering() {
        // Arrange
        var perioder = opprettOppgittePerioder();
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(perioder, false));
        Behandling behandling = scenario.lagre(repositoryProvider);

        // Act
        LocalDate endringsdato = endringsdatoFørstegangsbehandlingUtleder.utledEndringsdato(behandling.getId(), perioder);

        // Assert
        assertThat(endringsdato).isEqualTo(FØRSTE_UTTAKSDATO_OPPGITT);
    }

    @Test
    public void skal_utlede_at_endringsdatoen_er_første_uttaksdato_i_søknaden_når_manuell_vurdering_er_senere() {
        // Arrange
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medAvklarteUttakDatoer(opprettAvklarteUttakDatoer(FØRSTE_UTTAKSDATO_OPPGITT.plusDays(1)));
        Behandling behandling = scenario.lagre(repositoryProvider);

        // Act
        LocalDate endringsdato = endringsdatoFørstegangsbehandlingUtleder.utledEndringsdato(behandling.getId(), opprettOppgittePerioder());

        // Assert
        assertThat(endringsdato).isEqualTo(FØRSTE_UTTAKSDATO_OPPGITT);
    }

    @Test
    public void skal_utlede_at_endringsdatoen_er_manuelt_vurdert_uttaksdato_når_manuell_vurdering_er_tidligere() {
        // Arrange
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medAvklarteUttakDatoer(opprettAvklarteUttakDatoer(FØRSTE_UTTAKSDATO_OPPGITT.minusDays(1)));
        Behandling behandling = scenario.lagre(repositoryProvider);

        // Act
        LocalDate endringsdato = endringsdatoFørstegangsbehandlingUtleder.utledEndringsdato(behandling.getId(), opprettOppgittePerioder());

        // Assert
        assertThat(endringsdato).isEqualTo(FØRSTE_UTTAKSDATO_OPPGITT.minusDays(1));
    }

    private List<OppgittPeriodeEntitet> opprettOppgittePerioder() {
        OppgittPeriodeBuilder periode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(FØRSTE_UTTAKSDATO_OPPGITT, FØRSTE_UTTAKSDATO_OPPGITT.plusWeeks(2))
            .medArbeidsgiver(opprettOgLagreVirksomhet());

        return List.of(periode.build());
    }

    private Arbeidsgiver opprettOgLagreVirksomhet() {
        return Arbeidsgiver.virksomhet(ORGNR);
    }

    private AvklarteUttakDatoerEntitet opprettAvklarteUttakDatoer(LocalDate førsteUttaksdato) {
        return new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(førsteUttaksdato).build();
    }
}
