package no.nav.foreldrepenger.domene.registerinnhenting.impl.startpunkt;

import static no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT;
import static no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType.SØKERS_RELASJON_TIL_BARNET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.time.LocalDate;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

public class StartpunktUtlederFamilieHendelseTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    @Mock
    SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private StartpunktUtlederFamilieHendelse utleder;
    private FamilieHendelseTjeneste familieHendelseTjeneste = new FamilieHendelseTjeneste(null, null, repositoryProvider);

    @Before
    public void oppsett() {
        initMocks(this);
        utleder = new StartpunktUtlederFamilieHendelse(skjæringstidspunktTjeneste, familieHendelseTjeneste);
    }

    @Test
    public void skal_returnere_startpunkt_opplysningsplikt_dersom_familiehendelse_bekreftes_og_endrer_skjæringspunkt() {
        // Arrange
        LocalDate origSkjæringsdato = LocalDate.now();
        LocalDate nyBekreftetfødselsdato = origSkjæringsdato.minusDays(1); // fødselsdato før skjæringstidspunkt

        ScenarioMorSøkerForeldrepenger førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        Behandling originalBehandling = førstegangScenario.lagre(repositoryProvider);

        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(origSkjæringsdato).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(originalBehandling.getId())).thenReturn(skjæringstidspunkt);

        ScenarioMorSøkerForeldrepenger revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING);
        revurderingScenario.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);
        revurderingScenario.medBekreftetHendelse().medFødselsDato(nyBekreftetfødselsdato);
        Behandling revurdering = revurderingScenario.lagre(repositoryProvider);

        // Act/Assert
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
        Behandling originalBehandling = førstegangScenario.lagre(repositoryProvider);

        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(origSkjæringsdato).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(originalBehandling.getId())).thenReturn(skjæringstidspunkt);

        ScenarioMorSøkerForeldrepenger revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING);
        revurderingScenario.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);
        revurderingScenario.medBekreftetHendelse().medFødselsDato(nyBekreftetfødselsdato);
        Behandling revurdering = revurderingScenario.lagre(repositoryProvider);

        // Act/Assert
        assertThat(utleder.utledStartpunkt(BehandlingReferanse.fra(revurdering, skjæringstidspunkt), 1L, 2L)).isEqualTo(INNGANGSVILKÅR_OPPLYSNINGSPLIKT);
    }

    @Test
    public void skal_returnere_startpunkt_opplysningsplikt_dersom_orig_skjæringstidspunkt_flyttes_tidligere() {
        // Arrange
        LocalDate origSkjæringsdato = LocalDate.now();
        LocalDate nySkjæringsdato = LocalDate.now().minusDays(1);

        ScenarioMorSøkerForeldrepenger førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        Behandling originalBehandling = førstegangScenario.lagre(repositoryProvider);

        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(origSkjæringsdato).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(originalBehandling.getId())).thenReturn(skjæringstidspunkt);


        ScenarioMorSøkerForeldrepenger revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING);
        revurderingScenario.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);
        Behandling revurdering = revurderingScenario.lagre(repositoryProvider);
        var nySkjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(nySkjæringsdato).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(revurdering.getId())).thenReturn(nySkjæringstidspunkt);

        // Act/Assert
        assertThat(utleder.utledStartpunkt(BehandlingReferanse.fra(revurdering, nySkjæringstidspunkt), 1L, 2L)).isEqualTo(INNGANGSVILKÅR_OPPLYSNINGSPLIKT);
    }

    @Test
    public void skal_returnere_startpunkt_relasjon_dersom_mangler_bekreftet_etter_frist() {
        // Arrange
        LocalDate fødselSøknad = LocalDate.now().minusDays(20);
        LocalDate origSkjæringsdato = fødselSøknad.minusWeeks(3);

        ScenarioMorSøkerForeldrepenger førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFødselAdopsjonsdato(List.of(fødselSøknad))
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        Behandling originalBehandling = førstegangScenario.lagre(repositoryProvider);

        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(origSkjæringsdato).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(originalBehandling.getId())).thenReturn(skjæringstidspunkt);

        ScenarioMorSøkerForeldrepenger revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFødselAdopsjonsdato(List.of(fødselSøknad))
            .medBehandlingType(BehandlingType.REVURDERING);
        revurderingScenario.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);
        Behandling revurdering = revurderingScenario.lagre(repositoryProvider);
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(revurdering.getId())).thenReturn(skjæringstidspunkt);
        var fgf = repositoryProvider.getFamilieHendelseRepository().hentIdPåAktivFamiliehendelse(originalBehandling.getId());
        var fgr = repositoryProvider.getFamilieHendelseRepository().hentIdPåAktivFamiliehendelse(revurdering.getId());

        // Act/Assert
        assertThat(utleder.utledStartpunkt(BehandlingReferanse.fra(revurdering, skjæringstidspunkt), fgf.get(), fgr.get())).isEqualTo(SØKERS_RELASJON_TIL_BARNET);
    }

    @Test
    public void skal_returnere_startpunkt_relasjon_dersom_mangler_bekreftet_etter_frist_med_overstyrt_termin() {
        // Arrange
        LocalDate fødselSøknad = LocalDate.now().minusWeeks(5);
        LocalDate origSkjæringsdato = fødselSøknad.minusWeeks(3);

        ScenarioMorSøkerForeldrepenger førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medSøknadHendelse().medAntallBarn(1)
            .medTerminbekreftelse(førstegangScenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medNavnPå("Lege Legesen")
                .medTermindato(fødselSøknad)
                .medUtstedtDato(origSkjæringsdato));
        førstegangScenario.medOverstyrtHendelse().medAntallBarn(1)
            .medTerminbekreftelse(førstegangScenario.medOverstyrtHendelse().getTerminbekreftelseBuilder()
                .medNavnPå("Lege Legesen")
                .medTermindato(fødselSøknad)
                .medUtstedtDato(origSkjæringsdato));

        Behandling originalBehandling = førstegangScenario.lagre(repositoryProvider);

        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(origSkjæringsdato).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(originalBehandling.getId())).thenReturn(skjæringstidspunkt);

        Behandling revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING).build();
        repositoryProvider.getBehandlingRepository().lagre(revurdering, repositoryProvider.getBehandlingRepository().taSkriveLås(revurdering));
        familieHendelseTjeneste.kopierGrunnlag(originalBehandling.getId(), revurdering.getId());
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(revurdering.getId())).thenReturn(skjæringstidspunkt);
        var fgf = repositoryProvider.getFamilieHendelseRepository().hentIdPåAktivFamiliehendelse(originalBehandling.getId());
        var fgr = repositoryProvider.getFamilieHendelseRepository().hentIdPåAktivFamiliehendelse(revurdering.getId());

        // Act/Assert
        assertThat(utleder.utledStartpunkt(BehandlingReferanse.fra(revurdering, skjæringstidspunkt), fgf.get(), fgr.get())).isEqualTo(SØKERS_RELASJON_TIL_BARNET);
    }
}
