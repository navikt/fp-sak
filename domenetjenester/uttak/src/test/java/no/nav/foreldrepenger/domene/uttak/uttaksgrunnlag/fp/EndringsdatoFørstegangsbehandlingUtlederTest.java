package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;

class EndringsdatoFørstegangsbehandlingUtlederTest {

    private static final String ORGNR = KUNSTIG_ORG;

    private static final LocalDate FØRSTE_UTTAKSDATO_OPPGITT = LocalDate.now().plusDays(10);

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();

    private final EndringsdatoFørstegangsbehandlingUtleder endringsdatoFørstegangsbehandlingUtleder = new EndringsdatoFørstegangsbehandlingUtleder(
        repositoryProvider.getYtelsesFordelingRepository());

    @Test
    void skal_utlede_at_endringsdatoen_er_første_uttaksdato_i_søknaden_når_det_ikke_finnes_manuell_vurdering() {
        // Arrange
        var perioder = opprettOppgittePerioder();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medFordeling(new OppgittFordelingEntitet(perioder, false));
        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var endringsdato = endringsdatoFørstegangsbehandlingUtleder.utledEndringsdato(behandling.getId(), perioder);

        // Assert
        assertThat(endringsdato).isEqualTo(FØRSTE_UTTAKSDATO_OPPGITT);
    }

    @Test
    void skal_utlede_at_endringsdatoen_er_første_uttaksdato_i_søknaden_når_manuell_vurdering_er_senere() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medAvklarteUttakDatoer(opprettAvklarteUttakDatoer(FØRSTE_UTTAKSDATO_OPPGITT.plusDays(1)));
        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var endringsdato = endringsdatoFørstegangsbehandlingUtleder.utledEndringsdato(behandling.getId(), opprettOppgittePerioder());

        // Assert
        assertThat(endringsdato).isEqualTo(FØRSTE_UTTAKSDATO_OPPGITT);
    }

    @Test
    void skal_utlede_at_endringsdatoen_er_manuelt_vurdert_uttaksdato_når_manuell_vurdering_er_tidligere() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medAvklarteUttakDatoer(opprettAvklarteUttakDatoer(FØRSTE_UTTAKSDATO_OPPGITT.minusDays(1)));
        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var endringsdato = endringsdatoFørstegangsbehandlingUtleder.utledEndringsdato(behandling.getId(), opprettOppgittePerioder());

        // Assert
        assertThat(endringsdato).isEqualTo(FØRSTE_UTTAKSDATO_OPPGITT.minusDays(1));
    }

    private List<OppgittPeriodeEntitet> opprettOppgittePerioder() {
        var periode = OppgittPeriodeBuilder.ny()
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
