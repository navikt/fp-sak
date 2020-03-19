package no.nav.foreldrepenger.behandling.revurdering;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittDekningsgradEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.InnvilgetÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;

public class TapendeBehandlingTjenesteTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    @Test
    public void tapende_berørtBehandling() {
        var tjeneste = tjeneste();

        var morSøknadMottattDato = LocalDate.of(2019, 9, 30);
        Behandling morBehandling = morFørstegangsbehandling(morSøknadMottattDato);
        Behandling farBehandling = farFørstegangsbehandling(morSøknadMottattDato.plusWeeks(1), morBehandling);
        ScenarioFarSøkerForeldrepenger berørtBehandlingScenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        berørtBehandlingScenario.medOriginalBehandling(farBehandling, BehandlingÅrsakType.BERØRT_BEHANDLING);
        var berørtBehandling = berørtBehandlingScenario.lagre(repositoryProvider);

        var tapende = tjeneste.erTapendeBehandling(berørtBehandling);
        assertThat(tapende).isTrue();
    }

    @Test
    public void tapende_endringssøknadErMottattHosAnnenpartEtterSøkersSøknad() {
        var tjeneste = tjeneste();

        var morSøknadMottattDato = LocalDate.of(2019, 9, 30);
        var farSøknadMottattDato = morSøknadMottattDato.plusWeeks(1);
        Behandling morBehandling = morFørstegangsbehandling(morSøknadMottattDato);
        Behandling farBehandling = farFørstegangsbehandling(farSøknadMottattDato, morBehandling);

        opprettBehandlingAvEndringssøknadMor(morBehandling, farSøknadMottattDato.plusWeeks(1));

        var tapende = tjeneste.erTapendeBehandling(farBehandling);
        assertThat(tapende).isTrue();
    }

    @Test
    public void endringssøknadErMottattHosAnnenpartSammeDagSomSøkersSøknad() throws InterruptedException {
        var tjeneste = tjeneste();

        var søknaderMottattDato = LocalDate.of(2019, 9, 30);
        var morBehandling = morFørstegangsbehandling(søknaderMottattDato);
        var endringMor = opprettBehandlingAvEndringssøknadMor(morBehandling, søknaderMottattDato);

        //Bruker opprettet tidspunkt på søknad-entiteten
        Thread.sleep(10);
        var farBehandling = farFørstegangsbehandling(søknaderMottattDato, morBehandling);
        var erFarTapende = tjeneste.erTapendeBehandling(farBehandling);

        avsluttMedUttak(farBehandling);
        var erMorTapende = tjeneste.erTapendeBehandling(endringMor);

        assertThat(erFarTapende).isFalse();
        assertThat(erMorTapende).isTrue();
    }

    @Test
    public void ikkeTapende_manuellRevurderingFordelingOgEndringssøknadErMottattHosAnnenpartEtterSøkersSøknad() {
        var tjeneste = tjeneste();

        var morSøknadMottattDato = LocalDate.of(2019, 9, 30);
        var farSøknadMottattDato = morSøknadMottattDato.plusWeeks(1);
        Behandling morBehandling = morFørstegangsbehandling(morSøknadMottattDato);
        Behandling farBehandling = farFørstegangsbehandling(farSøknadMottattDato, morBehandling);

        opprettBehandlingAvEndringssøknadMor(morBehandling, farSøknadMottattDato.plusWeeks(1));
        var manuellRevurderingFar = opprettManuellRevurderingFar(farBehandling, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_FORDELING);

        var tapende = tjeneste.erTapendeBehandling(manuellRevurderingFar);
        assertThat(tapende).isFalse();
    }

    @Test
    public void tapende_manuellRevurderingAnnetEnnFordelingOgEndringssøknadErMottattHosAnnenpartEtterSøkersSøknad() {
        var tjeneste = tjeneste();

        var morSøknadMottattDato = LocalDate.of(2019, 9, 30);
        var farSøknadMottattDato = morSøknadMottattDato.plusWeeks(1);
        Behandling morBehandling = morFørstegangsbehandling(morSøknadMottattDato);
        Behandling farBehandling = farFørstegangsbehandling(farSøknadMottattDato, morBehandling);

        opprettBehandlingAvEndringssøknadMor(morBehandling, farSøknadMottattDato.plusWeeks(1));
        var manuellRevurderingFar = opprettManuellRevurderingFar(farBehandling, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_INNTEKT);

        var tapende = tjeneste.erTapendeBehandling(manuellRevurderingFar);
        assertThat(tapende).isTrue();
    }

    @Test
    public void ikkeTapende_endringssøknadErMottattHosMorEtterFarsSøknadMenManuellRevurderingIEtterkant() {
        var tjeneste = tjeneste();

        var morSøknadMottattDato = LocalDate.of(2019, 9, 30);
        var farSøknadMottattDato = morSøknadMottattDato.plusWeeks(1);
        Behandling morBehandling = morFørstegangsbehandling(morSøknadMottattDato);
        Behandling farBehandling = farFørstegangsbehandling(farSøknadMottattDato, morBehandling);

        opprettBehandlingAvEndringssøknadMor(morBehandling, farSøknadMottattDato.plusWeeks(1));

        var tapende = tjeneste.erTapendeBehandling(farBehandling);
        assertThat(tapende).isTrue();
    }

    @Test
    public void ikkeTapende_endringssøknadErMottattHosMorFørFarsSøknad() {
        var tjeneste = tjeneste();

        var morSøknadMottattDato = LocalDate.of(2019, 9, 30);
        var morEndringssøknadMottattDato = morSøknadMottattDato.plusWeeks(1);
        var farSøknadMottattDato = morEndringssøknadMottattDato.plusWeeks(1);
        Behandling morBehandling = morFørstegangsbehandling(morSøknadMottattDato);
        opprettBehandlingAvEndringssøknadMor(morBehandling, morEndringssøknadMottattDato);
        Behandling farBehandling = farFørstegangsbehandling(farSøknadMottattDato, morBehandling);

        var tapende = tjeneste.erTapendeBehandling(farBehandling);
        assertThat(tapende).isFalse();
    }

    @Test
    public void ikkeTapende_endringssøknadErIkkeMottattHosMor() {
        var tjeneste = tjeneste();

        var morSøknadMottattDato = LocalDate.of(2019, 9, 30);
        Behandling morBehandling = morFørstegangsbehandling(morSøknadMottattDato);
        Behandling farBehandling = farFørstegangsbehandling(morSøknadMottattDato.plusWeeks(1), morBehandling);

        var tapende = tjeneste.erTapendeBehandling(farBehandling);
        assertThat(tapende).isFalse();
    }

    @Test
    public void ikkeTapende_farUtenMor() {
        var tjeneste = tjeneste();
        Behandling farBehandling = farFørstegangsbehandlingUtenMor(LocalDate.of(2019, 10, 1).plusWeeks(1));

        var tapende = tjeneste.erTapendeBehandling(farBehandling);
        assertThat(tapende).isFalse();
    }

    @Test
    public void ikkeTapende_endringssøknadErMottattHosAnnenpartEtterSøkersSøknad_men_annenpart_har_ikke_uttak() {
        var tjeneste = tjeneste();

        var morSøknadMottattDato = LocalDate.of(2019, 9, 30);
        var farSøknadMottattDato = morSøknadMottattDato.plusWeeks(1);
        Behandling morBehandling = morFørstegangsbehandling(morSøknadMottattDato);
        Behandling farBehandling = farFørstegangsbehandling(farSøknadMottattDato, morBehandling);

        ScenarioMorSøkerForeldrepenger morEndringssøknadScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        morEndringssøknadScenario.medOriginalBehandling(morBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        morEndringssøknadScenario.medSøknad().medMottattDato(farSøknadMottattDato.plusWeeks(1));
        morEndringssøknadScenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET);
        var endringssøknadMorBehandling = morEndringssøknadScenario.lagre(repositoryProvider);
        endringssøknadMorBehandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(endringssøknadMorBehandling, repositoryProvider.getBehandlingLåsRepository().taLås(endringssøknadMorBehandling.getId()));

        var tapende = tjeneste.erTapendeBehandling(farBehandling);
        assertThat(tapende).isFalse();
    }

    private Behandling farFørstegangsbehandlingUtenMor(LocalDate søknadMottattDato) {
        Behandling farBehandling = farFørstegangsbehandling(søknadMottattDato);
        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(farBehandling.getFagsak(), Dekningsgrad._100);
        return farBehandling;
    }

    private Behandling farFørstegangsbehandling(LocalDate søknadMottattDato, Behandling morBehandling) {
        Behandling farBehandling = farFørstegangsbehandling(søknadMottattDato);
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(morBehandling.getFagsak(), farBehandling.getFagsak(), morBehandling);
        return farBehandling;
    }

    private Behandling farFørstegangsbehandling(LocalDate søknadMottattDato) {
        var farScenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        farScenario.medSøknad().medMottattDato(søknadMottattDato);
        farScenario.medBekreftetHendelse().medFødselsDato(søknadMottattDato).medAntallBarn(1);
        return farScenario.lagre(repositoryProvider);
    }

    private Behandling morFørstegangsbehandling(LocalDate søknadMottattDato) {
        var morScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        morScenario.medSøknad().medMottattDato(søknadMottattDato);
        morScenario.medOppgittDekningsgrad(OppgittDekningsgradEntitet.bruk100());
        return avsluttMedVedtak(morScenario);
    }

    private Behandling opprettBehandlingAvEndringssøknadMor(Behandling originalBehandling, LocalDate søknadMottattDato) {
        ScenarioMorSøkerForeldrepenger endringBehandlingMor = ScenarioMorSøkerForeldrepenger.forFødsel();
        endringBehandlingMor.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        endringBehandlingMor.medSøknad().medMottattDato(søknadMottattDato);
        return avsluttMedVedtak(endringBehandlingMor);
    }

    private Behandling opprettManuellRevurderingFar(Behandling originalBehandling, BehandlingÅrsakType årsak) {
        ScenarioFarSøkerForeldrepenger manuellRevurdering = ScenarioFarSøkerForeldrepenger.forFødsel();
        manuellRevurdering.medOriginalBehandling(originalBehandling, årsak);
        var behandling = avsluttMedVedtak(manuellRevurdering);
        repositoryProvider.getSøknadRepository().lagreOgFlush(behandling, repositoryProvider.getSøknadRepository().hentSøknad(originalBehandling));
        return behandling;
    }

    private Behandling avsluttMedVedtak(AbstractTestScenario<?> scenario) {
        var behandling = scenario.lagre(repositoryProvider);
        return avsluttMedUttak(behandling);
    }

    private Behandling avsluttMedUttak(Behandling behandling) {
        var uttaksperiode = new UttakResultatPeriodeEntitet.Builder(LocalDate.now(), LocalDate.now().plusWeeks(6))
            .medPeriodeResultat(PeriodeResultatType.INNVILGET, InnvilgetÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .build();
        var uttak = new UttakResultatPerioderEntitet();
        repositoryProvider.getUttakRepository().lagreOpprinneligUttakResultatPerioder(behandling.getId(), uttak);
        uttak.leggTilPeriode(uttaksperiode);
        behandling.avsluttBehandling();
        var lås = repositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId());
        var vedtak = BehandlingVedtak.builder()
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medVedtakstidspunkt(LocalDateTime.now())
            .medAnsvarligSaksbehandler(" ")
            .medBehandlingsresultat(repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId()))
            .build();
        repositoryProvider.getBehandlingVedtakRepository().lagre(vedtak, lås);
        repositoryProvider.getBehandlingRepository().lagre(behandling, lås);
        return behandling;
    }

    private TapendeBehandlingTjeneste tjeneste() {
        return new TapendeBehandlingTjeneste(repositoryProvider.getSøknadRepository(), new RelatertBehandlingTjeneste(repositoryProvider), repositoryProvider.getUttakRepository());
    }
}
