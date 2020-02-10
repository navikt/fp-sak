package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;


@RunWith(CdiRunner.class)
public class PersonopplysningDtoTjenesteTest {


    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    @Inject
    PersonopplysningTjeneste personopplysningTjeneste;
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private PersonopplysningDtoTjeneste tjeneste;

    @Inject
    private VergeRepository vergeRepository;


    @Before
    public void setUp() {
        tjeneste = new PersonopplysningDtoTjeneste(this.personopplysningTjeneste, repositoryProvider, vergeRepository);
    }

    @Test
    public void skal_takle_at_man_spør_etter_opplysninger_utenfor_tidsserien() {
        //sørger for at vi bommer når vi spør etter personstatus
        LocalDate enTilfeldigDato = LocalDate.of(1989, 9, 29);
        Behandling behandling = lagBehandling();

        Optional<PersonopplysningDto> personopplysningDto = tjeneste.lagPersonopplysningDto(behandling.getId(), enTilfeldigDato);

        assertThat(personopplysningDto).isPresent();
        assertThat(personopplysningDto.get().getAvklartPersonstatus().getOverstyrtPersonstatus()).isEqualByComparingTo(PersonstatusType.UDEFINERT);
    }

    private Behandling lagBehandling() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger
            .forFødselMedGittAktørId(AktørId.dummy());

        scenario.medDefaultOppgittFordeling(LocalDate.now());
        return scenario.lagre(repositoryProvider);
    }
}
