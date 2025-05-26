package no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.dto.Kilde;

public class FaktaFødselTjenesteTest extends EntityManagerAwareTest {
    private static final LocalDate FØDSELSDATO = LocalDate.now();
    private static final LocalDate TERMINDATO = LocalDate.now();
    private BehandlingRepositoryProvider repositoryProvider;
    private FaktaFødselTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        var fhTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());
        tjeneste = new FaktaFødselTjeneste(fhTjeneste);
    }

    @Test
    void skal_hente_fakta_om_fødsel_med_register_barn_og_overstyrbar_termindato() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        var søknadHendelse = scenario.medSøknadHendelse();
        søknadHendelse.medAntallBarn(1)
            .medFødselsDato(FØDSELSDATO)
            .medTerminbekreftelse(scenario.medSøknadHendelse()
            .getTerminbekreftelseBuilder()
            .medTermindato(TERMINDATO)
            .medNavnPå("LEGEN LEGESEN")
            .medUtstedtDato(TERMINDATO.minusMonths(1)));
        scenario.medBruker(AktørId.dummy()).medSøknad().medMottattDato(LocalDate.now().minusWeeks(2));
        scenario.medSøknadAnnenPart().medAktørId(AktørId.dummy());

        var bekreftetHendelse = scenario.medBekreftetHendelse();
        bekreftetHendelse.medFødselsDato(FØDSELSDATO).medAntallBarn(1);

        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var fødselDto = tjeneste.hentFaktaOmFødsel(behandling.getId());

        // Assert
        var gjeldende = fødselDto.gjeldende();
        assertThat(gjeldende).isNotNull();
        assertThat(gjeldende.termindato().termindato()).isEqualTo(TERMINDATO);
        assertThat(gjeldende.termindato().kanOverstyres()).isTrue();
        assertThat(gjeldende.barn()).hasSize(1);
        assertThat(gjeldende.barn().getFirst().barn().getFodselsdato()).isEqualTo(FØDSELSDATO);
        assertThat(gjeldende.barn().getFirst().kilde()).isEqualTo(Kilde.FOLKEREGISTER);
        assertThat(gjeldende.barn().getFirst().kanOverstyres()).isFalse();
    }

}
