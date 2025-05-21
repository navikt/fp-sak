package no.nav.foreldrepenger.inngangsvilkaar.fødsel;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelKjønn;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelSøkerRolle;

@CdiDbAwareTest
class FødselsvilkårOversetterTest {

    private FødselsvilkårOversetter fødselsoversetter;

    @Inject
    private PersonopplysningTjeneste personopplysningTjeneste;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;


    @BeforeEach
    void oppsett() {
        fødselsoversetter = new FødselsvilkårOversetter(repositoryProvider, personopplysningTjeneste, null);
    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @Test
    void skal_mappe_fra_domenefødsel_til_regelfødsel() {
        var now = LocalDate.now();
        var fødselFødselsdato = now.plusDays(7);
        var søknadFødselsdato = now.plusDays(2);
        var behandling = lagre(opprettBehandlingForFødsel(søknadFødselsdato, now, fødselFødselsdato, RelasjonsRolleType.MORA));

        var grunnlag = fødselsoversetter.oversettTilRegelModellFødsel(lagRef(behandling), false);

        // Assert
        assertThat(grunnlag.søkersKjønn()).isEqualTo(RegelKjønn.KVINNE);
        assertThat(grunnlag.bekreftetFødselsdato()).isEqualTo(fødselFødselsdato);
        assertThat(grunnlag.antallBarn()).isEqualTo(1);
        assertThat(grunnlag.terminbekreftelseTermindato()).isNull();
        assertThat(grunnlag.søkerRolle()).isEqualTo(RegelSøkerRolle.MORA);
        assertThat(grunnlag.behandlingsdato()).isEqualTo(now);
        assertThat(grunnlag.erSøktOmTermin()).isFalse();
    }

    @Test
    void skal_mappe_fra_domenefødsel_til_regelfødsel_dersom_søker_er_medmor() {
        var now = LocalDate.now();
        var fødselFødselsdato = now.plusDays(7);
        var søknadFødselsdato = now.plusDays(2);
        var behandling = lagre(opprettBehandlingForFødsel(søknadFødselsdato, now, fødselFødselsdato, RelasjonsRolleType.FARA));

        var grunnlag = fødselsoversetter.oversettTilRegelModellFødsel(lagRef(behandling), false);

        // Assert
        assertThat(grunnlag.søkersKjønn()).isEqualTo(RegelKjønn.KVINNE); // snodig, men søker er kvinne her med rolle FARA
        assertThat(grunnlag.bekreftetFødselsdato()).isEqualTo(fødselFødselsdato);
        assertThat(grunnlag.terminbekreftelseTermindato()).isNull();
        assertThat(grunnlag.søkerRolle()).isEqualTo(RegelSøkerRolle.FARA);
        assertThat(grunnlag.behandlingsdato()).isEqualTo(now);
        assertThat(grunnlag.erSøktOmTermin()).isFalse();
    }

    @Test
    void skal_mappe_tilfelle_medmor_2mnd_mor_fersk() {
        var now = LocalDate.now();
        var fødselFødselsdato = now.minusDays(1);
        var søknadFødselsdato = now.plusDays(2);
        var barn0AktørId = AktørId.dummy();

        var scenario = opprettBehandlingForFødsel(søknadFødselsdato, now, fødselFødselsdato, RelasjonsRolleType.MORA);

        var fødtBarn0 = scenario.opprettBuilderForRegisteropplysninger()
            .medPersonas()
            .fødtBarn(barn0AktørId, fødselFødselsdato.minusMonths(2))
            .relasjonTil(scenario.getDefaultBrukerAktørId(), RelasjonsRolleType.MEDMOR, null)
            .build();
        scenario.medRegisterOpplysninger(fødtBarn0);

        var behandling = lagre(scenario);

        var grunnlag = fødselsoversetter.oversettTilRegelModellFødsel(lagRef(behandling), false);

        // Assert
        assertThat(grunnlag.søkersKjønn()).isEqualTo(RegelKjønn.KVINNE);
        assertThat(grunnlag.bekreftetFødselsdato()).isEqualTo(fødselFødselsdato);
        assertThat(grunnlag.terminbekreftelseTermindato()).isNull();
        assertThat(grunnlag.søkerRolle()).isEqualTo(RegelSøkerRolle.MORA);
        assertThat(grunnlag.behandlingsdato()).isEqualTo(now);
        assertThat(grunnlag.erSøktOmTermin()).isFalse();
    }

    private ScenarioMorSøkerEngangsstønad opprettBehandlingForFødsel(LocalDate søknadFødselsdato, LocalDate søknadsdato, LocalDate fødselFødselsdato,
            RelasjonsRolleType rolle) {

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();

        scenario.medSøknad()
                .medSøknadsdato(søknadsdato);

        scenario.medSøknadHendelse().medFødselsDato(søknadFødselsdato);

        scenario.medBekreftetHendelse()
                // Fødsel
                .leggTilBarn(fødselFødselsdato)
                .medAntallBarn(1);

        var builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        var barnAktørId = AktørId.dummy();
        var søkerAktørId = scenario.getDefaultBrukerAktørId();

        var fødtBarn = builderForRegisteropplysninger
                .medPersonas()
                .fødtBarn(barnAktørId, fødselFødselsdato)
                .relasjonTil(søkerAktørId, rolle, null)
                .build();

        var søker = builderForRegisteropplysninger
                .medPersonas()
                .kvinne(søkerAktørId, SivilstandType.GIFT)
                .statsborgerskap(Landkoder.NOR)
                .relasjonTil(barnAktørId, RelasjonsRolleType.BARN, null)
                .build();
        scenario.medRegisterOpplysninger(søker);
        scenario.medRegisterOpplysninger(fødtBarn);

        return scenario;
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling);
    }

}
