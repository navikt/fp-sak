package no.nav.foreldrepenger.domene.personopplysning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;

class PersonopplysningTjenesteTest {


    @Test
    void skal_hente_gjeldende_personinformasjon_på_tidspunkt() {
        var tidspunkt = LocalDate.now();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medDefaultSøknadTerminbekreftelse();

        var søkerAktørId = scenario.getDefaultBrukerAktørId();

        var personInformasjon = scenario
            .opprettBuilderForRegisteropplysninger()
            .medPersonas()
            .kvinne(søkerAktørId, SivilstandType.SAMBOER)
            .statsborgerskap(Landkoder.NOR)
            .personstatus(PersonstatusType.BOSA)
            .build();

        scenario.medRegisterOpplysninger(personInformasjon);
        var personopplysningTjeneste = new PersonopplysningTjeneste(scenario.mockBehandlingRepositoryProvider().getPersonopplysningRepository());
        var behandling = scenario.lagMocked();

        var ref = BehandlingReferanse.fra(behandling);

        // Act
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(ref);
        // Assert
        assertThat(personopplysningerAggregat.getPersonstatuserFor(behandling.getAktørId(), SimpleLocalDateInterval.enDag(tidspunkt))).isNotEmpty();
    }

}
