package no.nav.foreldrepenger.domene.personopplysning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;

public class PersonopplysningTjenesteTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;

    private PersonopplysningTjeneste personopplysningTjeneste;
    private final ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();

    @BeforeEach
    public void before() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        personopplysningTjeneste = new PersonopplysningTjeneste(repositoryProvider.getPersonopplysningRepository());
    }

    @Test
    public void skal_hente_gjeldende_personinformasjon_på_tidspunkt() {
        var tidspunkt = LocalDate.now();
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
        scenario.medFordeling(new OppgittFordelingEntitet(Collections.singletonList(OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(LocalDate.now().plusWeeks(8), LocalDate.now().plusWeeks(12))
            .build()), true));

        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var personopplysningerAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunkt(behandling.getId(),
            behandling.getAktørId(), tidspunkt);
        // Assert
        assertThat(personopplysningerAggregat.getPersonstatuserFor(behandling.getAktørId())).isNotEmpty();
    }

}
