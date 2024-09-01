package no.nav.foreldrepenger.domene.registerinnhenting.impl.startpunkt;

import static no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.FamilieHendelseDato;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

class StartpunktUtlederFamilieHendelseTest {

    @Test
    void skal_returnere_startpunkt_opplysningsplikt_dersom_familiehendelse_bekreftes_og_endrer_skjæringspunkt() {
        // Arrange
        var origSkjæringsdato = LocalDate.now();
        var nyBekreftetfødselsdato = origSkjæringsdato.minusDays(1); // fødselsdato før skjæringstidspunkt

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var repositoryProvider = førstegangScenario.mockBehandlingRepositoryProvider();
        var originalBehandling = førstegangScenario.lagre(repositoryProvider);

        var skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);
        var skjæringstidspunkt = Skjæringstidspunkt.builder()
            .medFamilieHendelseDato(FamilieHendelseDato.forFødsel(null, nyBekreftetfødselsdato))
            .medUtledetSkjæringstidspunkt(origSkjæringsdato)
            .build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(originalBehandling.getId())).thenReturn(skjæringstidspunkt);

        var revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING);
        revurderingScenario.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);
        revurderingScenario.medBekreftetHendelse().medFødselsDato(nyBekreftetfødselsdato);
        var revurdering = revurderingScenario.lagre(repositoryProvider);

        // Act/Assert
        var familieHendelseTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());
        var dekningsgradTjeneste = new DekningsgradTjeneste(repositoryProvider.getYtelsesFordelingRepository());
        var utleder = new StartpunktUtlederFamilieHendelse(skjæringstidspunktTjeneste, familieHendelseTjeneste, dekningsgradTjeneste);
        assertThat(utleder.utledStartpunkt(BehandlingReferanse.fra(revurdering), skjæringstidspunkt, 1L, 2L)).isEqualTo(INNGANGSVILKÅR_OPPLYSNINGSPLIKT);
    }

    @Test
    void skal_returnere_startpunkt_opplysningsplikt_dersom_familiehendelse_flyttes_til_tidligere_dato() {
        // Arrange
        var origSkjæringsdato = LocalDate.now();
        var nyBekreftetfødselsdato = origSkjæringsdato.minusDays(1); // fødselsdato før skjæringstidspunkt

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medBekreftetHendelse().medFødselsDato(origSkjæringsdato);
        var repositoryProvider = førstegangScenario.mockBehandlingRepositoryProvider();
        var originalBehandling = førstegangScenario.lagre(repositoryProvider);

        var skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);
        var skjæringstidspunkt = Skjæringstidspunkt.builder()
            .medFamilieHendelseDato(FamilieHendelseDato.forFødsel(null, nyBekreftetfødselsdato))
            .medUtledetSkjæringstidspunkt(origSkjæringsdato).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(originalBehandling.getId())).thenReturn(skjæringstidspunkt);

        var revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING);
        revurderingScenario.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);
        revurderingScenario.medBekreftetHendelse().medFødselsDato(nyBekreftetfødselsdato);
        var revurdering = revurderingScenario.lagre(repositoryProvider);

        // Act/Assert
        var familieHendelseTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());
        var dekningsgradTjeneste = new DekningsgradTjeneste(repositoryProvider.getYtelsesFordelingRepository());
        var utleder = new StartpunktUtlederFamilieHendelse(skjæringstidspunktTjeneste, familieHendelseTjeneste, dekningsgradTjeneste);
        assertThat(utleder.utledStartpunkt(BehandlingReferanse.fra(revurdering), skjæringstidspunkt, 1L, 2L)).isEqualTo(INNGANGSVILKÅR_OPPLYSNINGSPLIKT);
    }

    @Test
    void skal_returnere_startpunkt_srb_dersom_færre_barn_enn_søknad() {
        // Arrange
        var fødselsdato = LocalDate.now();
        var skjæringsdato = LocalDate.now().minusWeeks(3);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medSøknadHendelse().medFødselsDato(fødselsdato, 2);
        var repositoryProvider = førstegangScenario.mockBehandlingRepositoryProvider();
        var originalBehandling = førstegangScenario.lagre(repositoryProvider);

        var skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);
        var skjæringstidspunkt = Skjæringstidspunkt.builder()
            .medFamilieHendelseDato(FamilieHendelseDato.forFødsel(fødselsdato, fødselsdato))
            .medUtledetSkjæringstidspunkt(skjæringsdato).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(originalBehandling.getId())).thenReturn(skjæringstidspunkt);

        var revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING);
        revurderingScenario.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);
        førstegangScenario.medSøknadHendelse().medFødselsDato(fødselsdato,2);
        revurderingScenario.medBekreftetHendelse().leggTilBarn(fødselsdato, fødselsdato);
        var revurdering = revurderingScenario.lagre(repositoryProvider);

        var g1 = repositoryProvider.getFamilieHendelseRepository().hentAggregat(originalBehandling.getId());
        var g2 = repositoryProvider.getFamilieHendelseRepository().hentAggregat(revurdering.getId());

        // Act/Assert
        var familieHendelseTjeneste = mock(FamilieHendelseTjeneste.class);
        when(familieHendelseTjeneste.hentAggregat(originalBehandling.getId())).thenReturn(g1);
        when(familieHendelseTjeneste.hentAggregat(revurdering.getId())).thenReturn(g2);
        when(familieHendelseTjeneste.hentGrunnlagPåId(1L)).thenReturn(g1);
        when(familieHendelseTjeneste.hentGrunnlagPåId(2L)).thenReturn(g2);
        var utleder = new StartpunktUtlederFamilieHendelse(skjæringstidspunktTjeneste, familieHendelseTjeneste, null);
        assertThat(utleder.utledStartpunkt(BehandlingReferanse.fra(revurdering), skjæringstidspunkt, 1L, 2L)).isEqualTo(StartpunktType.SØKERS_RELASJON_TIL_BARNET);
    }

    @Test
    void skal_returnere_startpunkt_dekningsgrad_dersom_tilkommet_dødfødsel_med_dekningsgrad80() {
        // Arrange
        var fødselsdato = LocalDate.now();
        var skjæringsdato = LocalDate.now().minusWeeks(3);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medSøknadHendelse().medFødselsDato(fødselsdato);
        var repositoryProvider = førstegangScenario.mockBehandlingRepositoryProvider();
        var originalBehandling = førstegangScenario.lagre(repositoryProvider);

        var skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);
        var skjæringstidspunkt = Skjæringstidspunkt.builder()
            .medFamilieHendelseDato(FamilieHendelseDato.forFødsel(null, fødselsdato))
            .medUtledetSkjæringstidspunkt(skjæringsdato).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(originalBehandling.getId())).thenReturn(skjæringstidspunkt);

        var revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING);
        revurderingScenario.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);
        førstegangScenario.medSøknadHendelse().medFødselsDato(fødselsdato);
        revurderingScenario.medBekreftetHendelse().leggTilBarn(fødselsdato, fødselsdato);
        var revurdering = revurderingScenario.lagre(repositoryProvider);

        var g1 = repositoryProvider.getFamilieHendelseRepository().hentAggregat(originalBehandling.getId());
        var g2 = repositoryProvider.getFamilieHendelseRepository().hentAggregat(revurdering.getId());

        var dekningsgradTjeneste = mock(DekningsgradTjeneste.class);
        when(dekningsgradTjeneste.finnGjeldendeDekningsgradHvisEksisterer(any(BehandlingReferanse.class))).thenReturn(Optional.of(Dekningsgrad._80));

        // Act/Assert
        var familieHendelseTjeneste = mock(FamilieHendelseTjeneste.class);
        when(familieHendelseTjeneste.hentAggregat(originalBehandling.getId())).thenReturn(g1);
        when(familieHendelseTjeneste.hentAggregat(revurdering.getId())).thenReturn(g2);
        when(familieHendelseTjeneste.hentGrunnlagPåId(1L)).thenReturn(g1);
        when(familieHendelseTjeneste.hentGrunnlagPåId(2L)).thenReturn(g2);

        var utleder = new StartpunktUtlederFamilieHendelse(skjæringstidspunktTjeneste, familieHendelseTjeneste, dekningsgradTjeneste);
        assertThat(utleder.utledStartpunkt(BehandlingReferanse.fra(revurdering), skjæringstidspunkt, 1L, 2L)).isEqualTo(StartpunktType.DEKNINGSGRAD);
    }

    @Test
    void skal_returnere_startpunkt_uttak_dersom_tilkommet_dødfødsel_med_dekningsgrad100() {
        // Arrange
        var fødselsdato = LocalDate.now();
        var skjæringsdato = LocalDate.now().minusWeeks(3);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medSøknadHendelse().medFødselsDato(fødselsdato);
        var repositoryProvider = førstegangScenario.mockBehandlingRepositoryProvider();
        var originalBehandling = førstegangScenario.lagre(repositoryProvider);

        var skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);
        var skjæringstidspunkt = Skjæringstidspunkt.builder()
            .medFamilieHendelseDato(FamilieHendelseDato.forFødsel(null, fødselsdato))
            .medUtledetSkjæringstidspunkt(skjæringsdato).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(originalBehandling.getId())).thenReturn(skjæringstidspunkt);

        var revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING);
        revurderingScenario.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);
        førstegangScenario.medSøknadHendelse().medFødselsDato(fødselsdato);
        revurderingScenario.medBekreftetHendelse().leggTilBarn(fødselsdato, fødselsdato);
        var revurdering = revurderingScenario.lagre(repositoryProvider);

        var g1 = repositoryProvider.getFamilieHendelseRepository().hentAggregat(originalBehandling.getId());
        var g2 = repositoryProvider.getFamilieHendelseRepository().hentAggregat(revurdering.getId());

        var dekningsgradTjeneste = mock(DekningsgradTjeneste.class);
        when(dekningsgradTjeneste.finnGjeldendeDekningsgradHvisEksisterer(any(BehandlingReferanse.class))).thenReturn(Optional.of(Dekningsgrad._100));

        // Act/Assert
        var familieHendelseTjeneste = mock(FamilieHendelseTjeneste.class);
        when(familieHendelseTjeneste.hentAggregat(originalBehandling.getId())).thenReturn(g1);
        when(familieHendelseTjeneste.hentAggregat(revurdering.getId())).thenReturn(g2);
        when(familieHendelseTjeneste.hentGrunnlagPåId(1L)).thenReturn(g1);
        when(familieHendelseTjeneste.hentGrunnlagPåId(2L)).thenReturn(g2);

        var utleder = new StartpunktUtlederFamilieHendelse(skjæringstidspunktTjeneste, familieHendelseTjeneste, dekningsgradTjeneste);
        assertThat(utleder.utledStartpunkt(BehandlingReferanse.fra(revurdering), skjæringstidspunkt, 1L, 2L)).isEqualTo(StartpunktType.UTTAKSVILKÅR);
    }

    @Test
    void skal_returnere_startpunkt_opplysningsplikt_dersom_orig_skjæringstidspunkt_flyttes_tidligere() {
        // Arrange
        var origSkjæringsdato = LocalDate.now();
        var nySkjæringsdato = LocalDate.now().minusDays(1);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var repositoryProvider = førstegangScenario.mockBehandlingRepositoryProvider();
        var originalBehandling = førstegangScenario.lagre(repositoryProvider);

        var skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(origSkjæringsdato).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(originalBehandling.getId())).thenReturn(skjæringstidspunkt);


        var revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING);
        revurderingScenario.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);
        var revurdering = revurderingScenario.lagre(repositoryProvider);
        var nySkjæringstidspunkt = Skjæringstidspunkt.builder()
            .medFamilieHendelseDato(FamilieHendelseDato.forFødsel(null, nySkjæringsdato))
            .medUtledetSkjæringstidspunkt(nySkjæringsdato).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(revurdering.getId())).thenReturn(nySkjæringstidspunkt);

        // Act/Assert
        var familieHendelseTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());

        var dekningsgradTjeneste = new DekningsgradTjeneste(repositoryProvider.getYtelsesFordelingRepository());
        var utleder = new StartpunktUtlederFamilieHendelse(skjæringstidspunktTjeneste, familieHendelseTjeneste, dekningsgradTjeneste);
        assertThat(utleder.utledStartpunkt(BehandlingReferanse.fra(revurdering), nySkjæringstidspunkt, 1L, 2L)).isEqualTo(INNGANGSVILKÅR_OPPLYSNINGSPLIKT);
    }

    @Test
    void skal_returnere_startpunkt_opplysningsplikt_dersom_far_justeres_ved_fødsel() {
        // Arrange
        var origSkjæringsdato = VirkedagUtil.fomVirkedag(LocalDate.now()).plusDays(2);
        var nySkjæringsdato = origSkjæringsdato.plusDays(1);

        var førstegangScenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var repositoryProvider = førstegangScenario.mockBehandlingRepositoryProvider();
        var originalBehandling = førstegangScenario.lagre(repositoryProvider);

        var skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(origSkjæringsdato).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(originalBehandling.getId())).thenReturn(skjæringstidspunkt);

        var revurderingScenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING);
        revurderingScenario.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);
        var revurdering = revurderingScenario.lagre(repositoryProvider);
        var nySkjæringstidspunkt = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(nySkjæringsdato)
            .medUttakSkalJusteresTilFødselsdato(true)
            .medFamilieHendelseDato(FamilieHendelseDato.forFødsel(null, nySkjæringsdato))
            .build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(revurdering.getId())).thenReturn(nySkjæringstidspunkt);

        // Act/Assert
        var familieHendelseTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());
        var dekningsgradTjeneste = new DekningsgradTjeneste(repositoryProvider.getYtelsesFordelingRepository());

        var utleder = new StartpunktUtlederFamilieHendelse(skjæringstidspunktTjeneste, familieHendelseTjeneste, dekningsgradTjeneste);
        assertThat(utleder.utledStartpunkt(BehandlingReferanse.fra(revurdering), nySkjæringstidspunkt, 1L, 2L)).isEqualTo(INNGANGSVILKÅR_OPPLYSNINGSPLIKT);
    }

}
