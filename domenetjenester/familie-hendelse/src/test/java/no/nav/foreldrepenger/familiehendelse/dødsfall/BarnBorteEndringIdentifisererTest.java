package no.nav.foreldrepenger.familiehendelse.dødsfall;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class BarnBorteEndringIdentifisererTest {
    private AktørId AKTØRID_SØKER = AktørId.dummy();
    private AktørId AKTØRID_BARN = AktørId.dummy();

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    @Inject
    private BarnBorteEndringIdentifiserer endringIdentifiserer;

    private PersonopplysningRepository personopplysningRepository;

    private Behandling behandlingOrig;
    private Behandling behandlingNy;


    @Before
    public void setup() {
        personopplysningRepository = repositoryProvider.getPersonopplysningRepository();

        behandlingOrig = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBruker(AKTØRID_SØKER, NavBrukerKjønn.KVINNE)
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .lagre(repositoryProvider);
        behandlingNy = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(behandlingOrig, BehandlingÅrsakType.RE_ANNET)
            .medBruker(AKTØRID_SØKER, NavBrukerKjønn.KVINNE)
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .lagre(repositoryProvider);
    }

    @Test
    public void ingen_endring_i_registrerte_barn() {
        opprettPersonopplysningGrunnlag(behandlingOrig, true);
        opprettPersonopplysningGrunnlag(behandlingNy, true);

        boolean erEndret = endringIdentifiserer.erEndret(lagReferanse());

        assertThat(erEndret).as("Idenfifiserer færre registrerte barn på ny behandling").isFalse();
    }

    private BehandlingReferanse lagReferanse() {
        return BehandlingReferanse.fra(behandlingNy);
    }

    @Test
    public void barn_fjernet_fra_tps_på_ny_behandling() {
        opprettPersonopplysningGrunnlag(behandlingOrig, true);
        opprettPersonopplysningGrunnlag(behandlingNy, false);

        boolean erEndret = endringIdentifiserer.erEndret(lagReferanse());

        assertThat(erEndret).as("Idenfifiserer færre registrerte barn på ny behandling").isTrue();
    }

    @Test
    public void barn_lagt_til_i_tps_på_ny_behandling() {
        opprettPersonopplysningGrunnlag(behandlingOrig, false);
        opprettPersonopplysningGrunnlag(behandlingNy, true);

        boolean erEndret = endringIdentifiserer.erEndret(lagReferanse());

        assertThat(erEndret).as("Idenfifiserer færre registrerte barn på ny behandling").isFalse();
    }


    private void opprettPersonopplysningGrunnlag(Behandling behandling, boolean registrerMedBarn) {
        final PersonInformasjonBuilder builder = personopplysningRepository.opprettBuilderForRegisterdata(behandlingNy.getId());
        builder.leggTil(builder.getPersonopplysningBuilder(AKTØRID_SØKER).medFødselsdato(LocalDate.now().minusYears(30)));
        if (registrerMedBarn) {
            builder.leggTil(builder.getPersonopplysningBuilder(AKTØRID_BARN).medFødselsdato(LocalDate.now().minusMonths(1)));
            builder.leggTil(builder.getRelasjonBuilder(AKTØRID_SØKER, AKTØRID_BARN, RelasjonsRolleType.BARN));
        }
        personopplysningRepository.lagre(behandling.getId(), builder);
    }

}
