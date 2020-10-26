package no.nav.foreldrepenger.domene.registerinnhenting.impl.startpunkt;

import static no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

public class StartpunktUtlederFamilieHendelseTest {

    @Test
    public void skal_returnere_startpunkt_opplysningsplikt_dersom_familiehendelse_bekreftes_og_endrer_skjæringspunkt() {
        // Arrange
        LocalDate origSkjæringsdato = LocalDate.now();
        LocalDate nyBekreftetfødselsdato = origSkjæringsdato.minusDays(1); // fødselsdato før skjæringstidspunkt

        ScenarioMorSøkerForeldrepenger førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var repositoryProvider = førstegangScenario.mockBehandlingRepositoryProvider();
        Behandling originalBehandling = førstegangScenario.lagre(repositoryProvider);

        var skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(origSkjæringsdato).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(originalBehandling.getId())).thenReturn(skjæringstidspunkt);

        ScenarioMorSøkerForeldrepenger revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING);
        revurderingScenario.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);
        revurderingScenario.medBekreftetHendelse().medFødselsDato(nyBekreftetfødselsdato);
        Behandling revurdering = revurderingScenario.lagre(repositoryProvider);

        // Act/Assert
        var familieHendelseTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());
        var utleder = new StartpunktUtlederFamilieHendelse(skjæringstidspunktTjeneste, familieHendelseTjeneste);
        assertThat(utleder.utledStartpunkt(BehandlingReferanse.fra(revurdering, skjæringstidspunkt), 1L, 2L)).isEqualTo(INNGANGSVILKÅR_OPPLYSNINGSPLIKT);
    }

    @Test
    public void skal_returnere_startpunkt_opplysningsplikt_dersom_familiehendelse_flyttes_til_tidligere_dato() {
        // Arrange
        LocalDate origSkjæringsdato = LocalDate.now();
        LocalDate origBekreftetfødselsdato = origSkjæringsdato;
        LocalDate nyBekreftetfødselsdato = origSkjæringsdato.minusDays(1); // fødselsdato før skjæringstidspunkt

        ScenarioMorSøkerForeldrepenger førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medBekreftetHendelse().medFødselsDato(origBekreftetfødselsdato);
        var repositoryProvider = førstegangScenario.mockBehandlingRepositoryProvider();
        Behandling originalBehandling = førstegangScenario.lagre(repositoryProvider);

        var skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(origSkjæringsdato).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(originalBehandling.getId())).thenReturn(skjæringstidspunkt);

        ScenarioMorSøkerForeldrepenger revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING);
        revurderingScenario.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);
        revurderingScenario.medBekreftetHendelse().medFødselsDato(nyBekreftetfødselsdato);
        Behandling revurdering = revurderingScenario.lagre(repositoryProvider);

        // Act/Assert
        var familieHendelseTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());
        var utleder = new StartpunktUtlederFamilieHendelse(skjæringstidspunktTjeneste, familieHendelseTjeneste);
        assertThat(utleder.utledStartpunkt(BehandlingReferanse.fra(revurdering, skjæringstidspunkt), 1L, 2L)).isEqualTo(INNGANGSVILKÅR_OPPLYSNINGSPLIKT);
    }

    @Test
    public void skal_returnere_startpunkt_opplysningsplikt_dersom_orig_skjæringstidspunkt_flyttes_tidligere() {
        // Arrange
        LocalDate origSkjæringsdato = LocalDate.now();
        LocalDate nySkjæringsdato = LocalDate.now().minusDays(1);

        ScenarioMorSøkerForeldrepenger førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var repositoryProvider = førstegangScenario.mockBehandlingRepositoryProvider();
        Behandling originalBehandling = førstegangScenario.lagre(repositoryProvider);

        var skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(origSkjæringsdato).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(originalBehandling.getId())).thenReturn(skjæringstidspunkt);


        ScenarioMorSøkerForeldrepenger revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING);
        revurderingScenario.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);
        Behandling revurdering = revurderingScenario.lagre(repositoryProvider);
        var nySkjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(nySkjæringsdato).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(revurdering.getId())).thenReturn(nySkjæringstidspunkt);

        // Act/Assert
        var familieHendelseTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());
        var utleder = new StartpunktUtlederFamilieHendelse(skjæringstidspunktTjeneste, familieHendelseTjeneste);
        assertThat(utleder.utledStartpunkt(BehandlingReferanse.fra(revurdering, nySkjæringstidspunkt), 1L, 2L)).isEqualTo(INNGANGSVILKÅR_OPPLYSNINGSPLIKT);
    }

}
