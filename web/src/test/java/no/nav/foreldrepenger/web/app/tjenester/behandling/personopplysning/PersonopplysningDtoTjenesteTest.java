package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;

@CdiDbAwareTest
class PersonopplysningDtoTjenesteTest {

    @Inject
    PersonopplysningTjeneste personopplysningTjeneste;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    private PersonopplysningDtoTjeneste tjeneste;


    @BeforeEach
    public void setUp() {
        tjeneste = new PersonopplysningDtoTjeneste(this.personopplysningTjeneste, repositoryProvider);
    }

    @Test
    void skal_takle_at_man_spør_etter_opplysninger_utenfor_tidsserien() {
        // sørger for at vi bommer når vi spør etter personstatus
        var enTilfeldigDato = LocalDate.of(1989, 9, 29);
        var behandling = lagBehandling();

        var personopplysningDto = tjeneste.lagPersonversiktDto(behandling.getId(), enTilfeldigDato);

        assertThat(personopplysningDto).isPresent();
        assertThat(personopplysningDto.get().getBruker().getSivilstand()).isEqualByComparingTo(SivilstandType.UOPPGITT);
    }

    private Behandling lagBehandling() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(AktørId.dummy());

        scenario.medDefaultFordeling(LocalDate.now());
        return scenario.lagre(repositoryProvider);
    }
}
