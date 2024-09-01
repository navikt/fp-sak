package no.nav.foreldrepenger.inngangsvilkaar.adopsjon;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelKjønn;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;

@CdiDbAwareTest
class AdopsjonsvilkårOversetterTest {

    private AdopsjonsvilkårOversetter adopsjonsoversetter;

    @Inject
    private PersonopplysningTjeneste personopplysningTjeneste;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    @BeforeEach
    public void oppsett() {
        skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider
        );
        adopsjonsoversetter = new AdopsjonsvilkårOversetter(repositoryProvider, personopplysningTjeneste);
    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @Test
    void skal_mappe_fra_domeneadoosjon_til_regeladopsjon() {
        // Arrange
        var søknadsdato = LocalDate.now().plusDays(1);
        var søknadFødselsdato = LocalDate.now().plusDays(2);
        var fødselAdopsjonsdatoFraSøknad = LocalDate.now().plusDays(8);
        Map<Integer, LocalDate> map = new HashMap<>();
        map.put(1, fødselAdopsjonsdatoFraSøknad);

        var scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
        scenario.medSøknad()
                .medSøknadsdato(søknadsdato)
                .build();
        scenario.medSøknadHendelse().medFødselsDato(søknadFødselsdato);

        scenario.medBekreftetHendelse().medAdopsjon(
                scenario.medBekreftetHendelse().getAdopsjonBuilder()
                        .medErEktefellesBarn(true)
                        .medAdoptererAlene(true)
                        .medOmsorgsovertakelseDato(fødselAdopsjonsdatoFraSøknad))
                .leggTilBarn(fødselAdopsjonsdatoFraSøknad)
                // Adosjon
                .build();

        var søker = scenario.opprettBuilderForRegisteropplysninger()
                .medPersonas()
                .mann(scenario.getDefaultBrukerAktørId(), SivilstandType.UOPPGITT)
                .statsborgerskap(Landkoder.NOR)
                .build();
        scenario.medRegisterOpplysninger(søker);

        var behandling = lagre(scenario);

        var grunnlag = adopsjonsoversetter.oversettTilRegelModellAdopsjon(lagRef(behandling));

        // Assert
        assertThat(grunnlag.søkersKjønn()).isEqualTo(RegelKjønn.MANN);
        assertThat(grunnlag.bekreftetAdopsjonBarn().get(0).fødselsdato()).isEqualTo(map.get(1));
        assertThat(grunnlag.ektefellesBarn()).isTrue();
        assertThat(grunnlag.mannAdoptererAlene()).isTrue();
        assertThat(grunnlag.omsorgsovertakelsesdato()).isEqualTo(fødselAdopsjonsdatoFraSøknad);
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling);
    }

}
