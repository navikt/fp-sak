package no.nav.foreldrepenger.domene.registerinnhenting.impl.startpunkt;

import static no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType.BEREGNING;
import static no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType.SØKERS_RELASJON_TIL_BARNET;
import static no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType.UDEFINERT;
import static no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType.UTTAKSVILKÅR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonRelasjon;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.Personopplysning;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.dødsfall.BarnBorteEndringIdentifiserer;

@ExtendWith(MockitoExtension.class)
public class StartpunktUtlederPersonopplysningTest extends EntityManagerAwareTest {

    private static final AktørId BARN_AKTØR_ID = AktørId.dummy();
    private static final AktørId EKTE_AKTØR_ID = AktørId.dummy();

    @Mock
    private BarnBorteEndringIdentifiserer barnBorteEndringIdentifiserer;

    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        when(barnBorteEndringIdentifiserer.erEndret(any())).thenReturn(false);
    }

    @Test
    public void skal_returnere_startpunkt_udefinert_dersom_kun_fødselsregistrering_deretter_beregning_ved_død() {
        // Arrange
        var origSkjæringsdato = LocalDate.now();
        var nyBekreftetfødselsdato = origSkjæringsdato.minusDays(1); // fødselsdato før skjæringstidspunkt

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var søkerAktørId = førstegangScenario.getDefaultBrukerAktørId();
        var builderForRegisteropplysninger = førstegangScenario.opprettBuilderForRegisteropplysninger()
            .leggTilPersonopplysninger(Personopplysning.builder().aktørId(søkerAktørId).navn("Kari K").brukerKjønn(NavBrukerKjønn.KVINNE).fødselsdato(LocalDate.now().minusYears(28)).sivilstand(SivilstandType.UGIFT).region(Region.NORDEN));
        førstegangScenario.medRegisterOpplysninger(builderForRegisteropplysninger.build());
        var originalBehandling = førstegangScenario.lagre(repositoryProvider);
        var behandlingId = originalBehandling.getId();

        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(origSkjæringsdato).build();

        var revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING);
        revurderingScenario.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);
        revurderingScenario.medBekreftetHendelse().medFødselsDato(nyBekreftetfødselsdato);
        var revurderingPOBuilder = revurderingScenario.opprettBuilderForRegisteropplysninger()
            .leggTilPersonopplysninger(Personopplysning.builder().aktørId(søkerAktørId).navn("Kari Mari").brukerKjønn(NavBrukerKjønn.KVINNE).fødselsdato(LocalDate.now().minusYears(28)).sivilstand(SivilstandType.UGIFT).region(Region.NORDEN))
            .leggTilPersonopplysninger(Personopplysning.builder().aktørId(BARN_AKTØR_ID).navn("Barn Abbas").brukerKjønn(NavBrukerKjønn.KVINNE).fødselsdato(nyBekreftetfødselsdato).sivilstand(SivilstandType.UGIFT).region(Region.NORDEN))
            .leggTilRelasjon(PersonRelasjon.builder().fraAktørId(søkerAktørId).tilAktørId(BARN_AKTØR_ID).relasjonsrolle(RelasjonsRolleType.BARN))
            .leggTilRelasjon(PersonRelasjon.builder().fraAktørId(BARN_AKTØR_ID).tilAktørId(søkerAktørId).relasjonsrolle(RelasjonsRolleType.MORA));
        revurderingScenario.medRegisterOpplysninger(revurderingPOBuilder.build());
        var revurdering = revurderingScenario.lagre(repositoryProvider);
        var revurderingId = revurdering.getId();

        // Act/Assert
        var utleder = new StartpunktUtlederPersonopplysning(repositoryProvider.getPersonopplysningRepository(), barnBorteEndringIdentifiserer);
        assertThat(utleder.utledStartpunkt(BehandlingReferanse.fra(revurdering, skjæringstidspunkt),
            repositoryProvider.getPersonopplysningRepository().hentPersonopplysninger(behandlingId).getId(),
            repositoryProvider.getPersonopplysningRepository().hentPersonopplysninger(revurderingId).getId())).isEqualTo(UDEFINERT);

        // Arrange
        var revurdering2Scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING);
        revurdering2Scenario.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);
        revurdering2Scenario.medBekreftetHendelse().medFødselsDato(nyBekreftetfødselsdato);
        var revurdering2POBuilder = revurdering2Scenario.opprettBuilderForRegisteropplysninger()
            .leggTilPersonopplysninger(Personopplysning.builder().aktørId(søkerAktørId).navn("Kari Mari").brukerKjønn(NavBrukerKjønn.KVINNE).fødselsdato(LocalDate.now().minusYears(28)).sivilstand(SivilstandType.UGIFT).region(Region.NORDEN))
            .leggTilPersonopplysninger(Personopplysning.builder().aktørId(BARN_AKTØR_ID).navn("Barn Abbas").brukerKjønn(NavBrukerKjønn.KVINNE).fødselsdato(nyBekreftetfødselsdato).dødsdato(origSkjæringsdato).sivilstand(SivilstandType.UGIFT).region(Region.NORDEN))
            .leggTilRelasjon(PersonRelasjon.builder().fraAktørId(søkerAktørId).tilAktørId(BARN_AKTØR_ID).relasjonsrolle(RelasjonsRolleType.BARN))
            .leggTilRelasjon(PersonRelasjon.builder().fraAktørId(BARN_AKTØR_ID).tilAktørId(søkerAktørId).relasjonsrolle(RelasjonsRolleType.MORA));
        revurdering2Scenario.medRegisterOpplysninger(revurdering2POBuilder.build());
        var revurdering2 = revurdering2Scenario.lagre(repositoryProvider);
        var revurdering2Id = revurdering2.getId();

        // Act/Assert
        assertThat(utleder.utledStartpunkt(BehandlingReferanse.fra(revurdering, skjæringstidspunkt),
            repositoryProvider.getPersonopplysningRepository().hentPersonopplysninger(revurdering2Id).getId(),
            repositoryProvider.getPersonopplysningRepository().hentPersonopplysninger(revurderingId).getId())).isEqualTo(BEREGNING);
    }

    @Test
    public void skal_returnere_startpunkt_srb_ved_ekteskap_med_annen_enn_annenpart() {
        // Arrange
        var origSkjæringsdato = LocalDate.now();
        var nyBekreftetfødselsdato = origSkjæringsdato.minusDays(1); // fødselsdato før skjæringstidspunkt

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var søkerAktørId = førstegangScenario.getDefaultBrukerAktørId();
        var builderForRegisteropplysninger = førstegangScenario.opprettBuilderForRegisteropplysninger()
            .leggTilPersonopplysninger(Personopplysning.builder().aktørId(søkerAktørId).navn("Kari K").brukerKjønn(NavBrukerKjønn.KVINNE).fødselsdato(LocalDate.now().minusYears(28)).sivilstand(SivilstandType.UGIFT).region(Region.NORDEN));
        førstegangScenario.medRegisterOpplysninger(builderForRegisteropplysninger.build());
        var originalBehandling = førstegangScenario.lagre(repositoryProvider);
        var behandlingId = originalBehandling.getId();

        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(origSkjæringsdato).build();

        var revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING);
        revurderingScenario.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);
        revurderingScenario.medBekreftetHendelse().medFødselsDato(nyBekreftetfødselsdato);
        var revurderingPOBuilder = revurderingScenario.opprettBuilderForRegisteropplysninger()
            .leggTilPersonopplysninger(Personopplysning.builder().aktørId(søkerAktørId).navn("Kari Mari").brukerKjønn(NavBrukerKjønn.KVINNE).fødselsdato(LocalDate.now().minusYears(28)).sivilstand(SivilstandType.GIFT).region(Region.NORDEN))
            .leggTilPersonopplysninger(Personopplysning.builder().aktørId(EKTE_AKTØR_ID).navn("Far Mor").brukerKjønn(NavBrukerKjønn.MANN).fødselsdato(LocalDate.now().minusYears(28)).sivilstand(SivilstandType.GIFT).region(Region.NORDEN))
            .leggTilRelasjon(PersonRelasjon.builder().fraAktørId(søkerAktørId).tilAktørId(EKTE_AKTØR_ID).relasjonsrolle(RelasjonsRolleType.EKTE))
            .leggTilRelasjon(PersonRelasjon.builder().fraAktørId(EKTE_AKTØR_ID).tilAktørId(søkerAktørId).relasjonsrolle(RelasjonsRolleType.EKTE));
        revurderingScenario.medRegisterOpplysninger(revurderingPOBuilder.build());
        var revurdering = revurderingScenario.lagre(repositoryProvider);
        var revurderingId = revurdering.getId();

        // Act/Assert
        var utleder = new StartpunktUtlederPersonopplysning(repositoryProvider.getPersonopplysningRepository(), barnBorteEndringIdentifiserer);
        assertThat(utleder.utledStartpunkt(BehandlingReferanse.fra(revurdering, skjæringstidspunkt),
            repositoryProvider.getPersonopplysningRepository().hentPersonopplysninger(behandlingId).getId(),
            repositoryProvider.getPersonopplysningRepository().hentPersonopplysninger(revurderingId).getId())).isEqualTo(SØKERS_RELASJON_TIL_BARNET);
    }

    @Test
    public void skal_returnere_startpunkt_uttak_ved_endring_har_samme_bosted() {
        // Arrange
        var fødselsdato = LocalDate.now().minusDays(10);

        var førstegangScenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var søkerAktørId = førstegangScenario.getDefaultBrukerAktørId();
        førstegangScenario.medBekreftetHendelse().medFødselsDato(fødselsdato);
        var builderForRegisteropplysninger = førstegangScenario.opprettBuilderForRegisteropplysninger()
            .leggTilPersonopplysninger(Personopplysning.builder().aktørId(søkerAktørId).navn("Ola N").brukerKjønn(NavBrukerKjønn.MANN).fødselsdato(LocalDate.now().minusYears(28)).sivilstand(SivilstandType.UGIFT).region(Region.NORDEN))
            .leggTilPersonopplysninger(Personopplysning.builder().aktørId(BARN_AKTØR_ID).navn("Barn Abbas").brukerKjønn(NavBrukerKjønn.KVINNE).fødselsdato(fødselsdato).sivilstand(SivilstandType.UGIFT).region(Region.NORDEN))
            .leggTilRelasjon(PersonRelasjon.builder().fraAktørId(søkerAktørId).tilAktørId(BARN_AKTØR_ID).harSammeBosted(true).relasjonsrolle(RelasjonsRolleType.BARN))
            .leggTilRelasjon(PersonRelasjon.builder().fraAktørId(BARN_AKTØR_ID).tilAktørId(søkerAktørId).harSammeBosted(true).relasjonsrolle(RelasjonsRolleType.FARA));
        førstegangScenario.medRegisterOpplysninger(builderForRegisteropplysninger.build());
        var originalBehandling = førstegangScenario.lagre(repositoryProvider);
        var behandlingId = originalBehandling.getId();

        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(fødselsdato).build();

        var revurderingScenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING);
        revurderingScenario.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_ANNET);
        revurderingScenario.medBekreftetHendelse().medFødselsDato(fødselsdato);
        var revurderingPOBuilder = revurderingScenario.opprettBuilderForRegisteropplysninger()
            .leggTilPersonopplysninger(Personopplysning.builder().aktørId(søkerAktørId).navn("Ola N").brukerKjønn(NavBrukerKjønn.MANN).fødselsdato(LocalDate.now().minusYears(28)).sivilstand(SivilstandType.UGIFT).region(Region.NORDEN))
            .leggTilPersonopplysninger(Personopplysning.builder().aktørId(BARN_AKTØR_ID).navn("Barn Abbas").brukerKjønn(NavBrukerKjønn.KVINNE).fødselsdato(fødselsdato).sivilstand(SivilstandType.UGIFT).region(Region.NORDEN))
            .leggTilRelasjon(PersonRelasjon.builder().fraAktørId(søkerAktørId).tilAktørId(BARN_AKTØR_ID).harSammeBosted(false).relasjonsrolle(RelasjonsRolleType.BARN))
            .leggTilRelasjon(PersonRelasjon.builder().fraAktørId(BARN_AKTØR_ID).tilAktørId(søkerAktørId).harSammeBosted(false).relasjonsrolle(RelasjonsRolleType.FARA));
        revurderingScenario.medRegisterOpplysninger(revurderingPOBuilder.build());
        var revurdering = revurderingScenario.lagre(repositoryProvider);
        var revurderingId = revurdering.getId();

        // Act/Assert
        var utleder = new StartpunktUtlederPersonopplysning(repositoryProvider.getPersonopplysningRepository(), barnBorteEndringIdentifiserer);
        assertThat(utleder.utledStartpunkt(BehandlingReferanse.fra(revurdering, skjæringstidspunkt),
            repositoryProvider.getPersonopplysningRepository().hentPersonopplysninger(behandlingId).getId(),
            repositoryProvider.getPersonopplysningRepository().hentPersonopplysninger(revurderingId).getId())).isEqualTo(UTTAKSVILKÅR);
    }

}
