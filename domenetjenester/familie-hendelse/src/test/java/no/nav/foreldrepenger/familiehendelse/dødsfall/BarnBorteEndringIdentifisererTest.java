package no.nav.foreldrepenger.familiehendelse.dødsfall;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.typer.AktørId;

@ExtendWith(JpaExtension.class)
class BarnBorteEndringIdentifisererTest {
    private final AktørId AKTØRID_SØKER = AktørId.dummy();
    private final AktørId AKTØRID_BARN = AktørId.dummy();

    private BarnBorteEndringIdentifiserer endringIdentifiserer;

    private PersonopplysningRepository personopplysningRepository;
    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    public void setup(EntityManager em) {
        repositoryProvider = new BehandlingRepositoryProvider(em);
        endringIdentifiserer = new BarnBorteEndringIdentifiserer(repositoryProvider);
        personopplysningRepository = new PersonopplysningRepository(em);
    }

    @Test
    void ingen_endring_i_registrerte_barn() {
        var behandlingOrig = førstegangsbehandling();
        var behandlingNy = revurdering(behandlingOrig);
        opprettPersonopplysningGrunnlag(behandlingOrig, true);
        opprettPersonopplysningGrunnlag(behandlingNy, true);

        var erEndret = endringIdentifiserer.erEndret(lagReferanse(behandlingNy));

        assertThat(erEndret).as("Idenfifiserer færre registrerte barn på ny behandling").isFalse();
    }

    private Behandling revurdering(Behandling behandlingOrig) {
        return ScenarioMorSøkerForeldrepenger.forFødsel()
                .medOriginalBehandling(behandlingOrig, BehandlingÅrsakType.RE_ANNET)
                .medBruker(AKTØRID_SØKER, NavBrukerKjønn.KVINNE)
                .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
                .lagre(repositoryProvider);
    }

    private Behandling førstegangsbehandling() {
        return ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBruker(AKTØRID_SØKER, NavBrukerKjønn.KVINNE)
                .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
                .lagre(repositoryProvider);
    }

    BehandlingReferanse lagReferanse(Behandling behandling) {
        return BehandlingReferanse.fra(behandling);
    }

    @Test
    void barn_fjernet_fra_tps_på_ny_behandling() {
        var behandlingOrig = førstegangsbehandling();
        var behandlingNy = revurdering(behandlingOrig);
        opprettPersonopplysningGrunnlag(behandlingOrig, true);
        opprettPersonopplysningGrunnlag(behandlingNy, false);

        var erEndret = endringIdentifiserer.erEndret(lagReferanse(behandlingNy));

        assertThat(erEndret).as("Idenfifiserer færre registrerte barn på ny behandling").isTrue();
    }

    @Test
    void barn_lagt_til_i_tps_på_ny_behandling() {
        var behandlingOrig = førstegangsbehandling(ScenarioMorSøkerForeldrepenger.forFødsel());
        var behandlingNy = revurdering(behandlingOrig);
        opprettPersonopplysningGrunnlag(behandlingOrig, false);
        opprettPersonopplysningGrunnlag(behandlingNy, true);

        var erEndret = endringIdentifiserer.erEndret(lagReferanse(behandlingNy));

        assertThat(erEndret).as("Idenfifiserer færre registrerte barn på ny behandling").isFalse();
    }

    private Behandling førstegangsbehandling(ScenarioMorSøkerForeldrepenger scenarioMorSøkerForeldrepenger) {
        return scenarioMorSøkerForeldrepenger
                .medBruker(AKTØRID_SØKER, NavBrukerKjønn.KVINNE)
                .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
                .lagre(repositoryProvider);
    }

    private void opprettPersonopplysningGrunnlag(Behandling behandling, boolean registrerMedBarn) {
        final var builder = personopplysningRepository.opprettBuilderForRegisterdata(behandling.getId());
        builder.leggTil(builder.getPersonopplysningBuilder(AKTØRID_SØKER).medFødselsdato(LocalDate.now().minusYears(30)));
        if (registrerMedBarn) {
            builder.leggTil(builder.getPersonopplysningBuilder(AKTØRID_BARN).medFødselsdato(LocalDate.now().minusMonths(1)));
            builder.leggTil(builder.getRelasjonBuilder(AKTØRID_SØKER, AKTØRID_BARN, RelasjonsRolleType.BARN));
        }
        personopplysningRepository.lagre(behandling.getId(), builder);
    }

}
