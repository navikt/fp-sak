package no.nav.foreldrepenger.behandling.steg.avklarfakta.fp;

import static no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling.forceOppdaterBehandlingSteg;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.StegTransisjon;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.Transisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;

@CdiDbAwareTest
class KontrollerFaktaRevurderingStegImplTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    @Any
    private KontrollerFaktaRevurderingStegImpl steg;

    @Test
    void skal_ikke_fjerne_aksjonspunkter_som_er_utledet_etter_startpunktet() {
        var behandling = opprettRevurdering();
        // Arrange
        var lås = behandlingRepository.taSkriveLås(behandling);
        var kontekst = new BehandlingskontrollKontekst(behandling, lås);

        // Act
        var aksjonspunkter = steg.utførSteg(kontekst).getAksjonspunktListe();

        // Assert
        assertThat(aksjonspunkter).contains(AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL);
        // Må verifisere at startpunkt er før aksjonpunktet for at assert ovenfor skal
        // ha mening
        assertThat(behandling.getStartpunkt()).isEqualTo(StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT);
    }

    @Test
    void må_nullstille_fordelingsperiode_hvis_ikke_er_endringssøknad() {
        var behandling = opprettRevurdering();
        // Arrange
        var lås = behandlingRepository.taSkriveLås(behandling);
        var kontekst = new BehandlingskontrollKontekst(behandling, lås);

        // Act
        steg.utførSteg(kontekst).getAksjonspunktListe();
        var ytelseFordelingAggregat = repositoryProvider.getYtelsesFordelingRepository()
                .hentAggregatHvisEksisterer(behandling.getId());
        assertThat(ytelseFordelingAggregat).isPresent();
        var aggregat = ytelseFordelingAggregat.get();
        assertThat(aggregat.getOppgittFordeling()).isNotNull();
        assertThat(aggregat.getOppgittFordeling().getPerioder()).isEmpty();
        assertThat(aggregat.getOppgittFordeling().getErAnnenForelderInformert()).isTrue();
    }

    @Test
    void må_ikke_nullstille_fordelingsperiode_hvis_er_revurdering_med_førstegangssøknad_uten_uttak() {
        var søknadsperiode = OppgittPeriodeBuilder.ny()
                .medPeriode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 5, 5))
                .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
                .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
                .build();
        var avslåttFørstegangsbehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medFordeling(new OppgittFordelingEntitet(List.of(OppgittPeriodeBuilder.fraEksisterende(søknadsperiode).build()), true))
                .medFødselAdopsjonsdato(List.of(LocalDate.of(2020, 1, 1)))
                .medDefaultOppgittTilknytning()
                .lagre(repositoryProvider);
        var revurdering = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medOriginalBehandling(avslåttFørstegangsbehandling, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_BEREGNINGSGRUNNLAG)
                .medFordeling(new OppgittFordelingEntitet(List.of(OppgittPeriodeBuilder.fraEksisterende(søknadsperiode).build()), true))
                .medFødselAdopsjonsdato(List.of(LocalDate.of(2020, 1, 1)))
                .medDefaultOppgittTilknytning()
                .lagre(repositoryProvider);

        var kontekst = new BehandlingskontrollKontekst(revurdering, behandlingRepository.taSkriveLås(revurdering.getId()));
        steg.utførSteg(kontekst);

        var ytelseFordelingAggregat = repositoryProvider.getYtelsesFordelingRepository()
                .hentAggregat(revurdering.getId());
        assertThat(ytelseFordelingAggregat.getOppgittFordeling().getPerioder()).isNotEmpty();
        assertThat(ytelseFordelingAggregat.getOppgittFordeling().getErAnnenForelderInformert()).isTrue();
    }

    @Test
    void må_ikke_nullstille_fordelingsperiode_hvis_er_revurdering_av_revurdering_uten_uttak() {
        var avslåttFørstegangsbehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
                .lagre(repositoryProvider);
        var søknadsperiode = OppgittPeriodeBuilder.ny()
                .medPeriode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 5, 5))
                .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
                .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
                .build();
        var avslåttRevurdering1 = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medOriginalBehandling(avslåttFørstegangsbehandling, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_BEREGNINGSGRUNNLAG)
                .medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.AVSLÅTT))
                .medFordeling(new OppgittFordelingEntitet(List.of(søknadsperiode), true))
                .medFødselAdopsjonsdato(List.of(LocalDate.of(2020, 1, 1)))
                // Behandling avslås i inngangsvilkår, derfor ikke noe uttak
                .lagre(repositoryProvider);
        var revurdering = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medOriginalBehandling(avslåttRevurdering1, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_BEREGNINGSGRUNNLAG)
                .medFordeling(new OppgittFordelingEntitet(List.of(OppgittPeriodeBuilder.fraEksisterende(søknadsperiode).build()), true))
                .medFødselAdopsjonsdato(List.of(LocalDate.of(2020, 1, 1)))
                .medDefaultOppgittTilknytning()
                .lagre(repositoryProvider);

        var kontekst = new BehandlingskontrollKontekst(revurdering, behandlingRepository.taSkriveLås(revurdering.getId()));
        steg.utførSteg(kontekst);

        var ytelseFordelingAggregat = repositoryProvider.getYtelsesFordelingRepository()
                .hentAggregat(revurdering.getId());
        assertThat(ytelseFordelingAggregat.getOppgittFordeling().getPerioder()).isNotEmpty();
    }

    @Test
    void må_ikke_nullstille_fordelingsperiode_hvis_er_endringssøknad() {
        var behandling = opprettRevurdering();
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusWeeks(30);

        var revurdering = opprettRevurderingPgaEndringsSøknad(behandling, fom, tom);

        // Arrange
        var lås = behandlingRepository.taSkriveLås(revurdering);
        var kontekst = new BehandlingskontrollKontekst(revurdering, lås);

        // Act
        steg.utførSteg(kontekst).getAksjonspunktListe();
        var ytelseFordelingAggregat = repositoryProvider.getYtelsesFordelingRepository()
                .hentAggregatHvisEksisterer(revurdering.getId());
        assertThat(ytelseFordelingAggregat).isPresent();
        var aggregat = ytelseFordelingAggregat.get();
        assertThat(aggregat.getOppgittFordeling()).isNotNull();
        assertThat(aggregat.getOppgittFordeling().getPerioder()).isNotEmpty();
        assertThat(aggregat.getOppgittFordeling().getPerioder()).size().isEqualTo(1);
        assertThat(aggregat.getOppgittFordeling().getPerioder().get(0).getFom()).isEqualTo(fom);
        assertThat(aggregat.getOppgittFordeling().getPerioder().get(0).getTom()).isEqualTo(tom);
        assertThat(aggregat.getOppgittFordeling().getErAnnenForelderInformert()).isTrue();
    }

    @Test
    void skal_gå_til_nærmeste_startpunkt_før_åpen_overstyring() {
        var behandling = opprettAutomatiskRevurderingMedÅpenOverstyring();
        // Arrange
        var lås = behandlingRepository.taSkriveLås(behandling);
        var kontekst = new BehandlingskontrollKontekst(behandling, lås);

        // Act
        var aksjonspunkter = steg.utførSteg(kontekst).getAksjonspunktListe();

        // Assert
        assertThat(aksjonspunkter).isEmpty();
        assertThat(behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.OVERSTYRING_AV_AVKLART_STARTDATO)).isPresent();
        // Må verifisere at startpunkt er før aksjonpunktet for at assert ovenfor skal
        // ha mening
        assertThat(behandling.getStartpunkt()).isEqualTo(StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP);
    }

    @Test
    void feriepenge_berørt_hopper_til_tilkjent() {
        var behandling = opprettRevurdering(BehandlingÅrsakType.REBEREGN_FERIEPENGER);
        // Arrange
        var lås = behandlingRepository.taSkriveLås(behandling);
        var kontekst = new BehandlingskontrollKontekst(behandling, lås);
        var expectedTransisjon =  new Transisjon(StegTransisjon.FLYOVER, StartpunktType.TILKJENT_YTELSE.getBehandlingSteg());

        // Act
        var stegResultat = steg.utførSteg(kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(expectedTransisjon);
    }

    private Behandling opprettRevurderingPgaEndringsSøknad(Behandling originalBehandling, LocalDate fom, LocalDate tom) {
        repositoryProvider.getOpptjeningRepository().lagreOpptjeningsperiode(originalBehandling, LocalDate.now().minusYears(1), LocalDate.now(),
                false);

        var foreldrepenger = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(fom, tom)
            .build();
        var fordeling = new OppgittFordelingEntitet(List.of(foreldrepenger), true);
        var revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingType(BehandlingType.REVURDERING)
                .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
            .medFordeling(fordeling);
        revurderingScenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now()).build());
        revurderingScenario.medDefaultOppgittTilknytning();

        revurderingScenario.medSøknadHendelse().medFødselsDato(LocalDate.now().minusDays(10));
        var revurdering = revurderingScenario.lagre(repositoryProvider);

        // Nødvendig å sette aktivt steg for KOFAK revurdering
        forceOppdaterBehandlingSteg(revurdering, BehandlingStegType.KONTROLLER_FAKTA);
        var behandlingLås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, behandlingLås);

        return revurdering;
    }

    @Test
    void skal_utlede_startpunkt_dersom_uttaksplan_på_original_behandling_mangler() {
        // Arrange
        var revurdering = opprettRevurderingPgaBerørtBehandling();
        var lås = behandlingRepository.taSkriveLås(revurdering);
        var kontekst = new BehandlingskontrollKontekst(revurdering, lås);

        // Act
        steg.utførSteg(kontekst).getAksjonspunktListe();

        // Assert
        var behandlingEtterSteg = behandlingRepository.hentBehandling(revurdering.getId());
        assertThat(behandlingEtterSteg.getStartpunkt()).isEqualTo(StartpunktType.UTTAKSVILKÅR);
    }

    @Test
    void skal_ikke_utlede_startpunkt_dekningsgrad_dersom_endret_dekningsgrad_i_berørt_revurdering() {
        // Arrange
        var revurdering = opprettRevurderingPgaBerørtBehandling();
        repositoryProvider.getFagsakRelasjonRepository().oppdaterDekningsgrad(revurdering.getFagsak(), Dekningsgrad._100);
        endreDekningsgrad(revurdering.getId(), Dekningsgrad._80);
        var lås = behandlingRepository.taSkriveLås(revurdering);
        var kontekst = new BehandlingskontrollKontekst(revurdering, lås);

        // Act
        steg.utførSteg(kontekst).getAksjonspunktListe();

        // Assert
        var behandlingEtterSteg = behandlingRepository.hentBehandling(revurdering.getId());
        assertThat(behandlingEtterSteg.getStartpunkt()).isEqualTo(StartpunktType.UTTAKSVILKÅR);
    }

    private void endreDekningsgrad(Long behandlingId, Dekningsgrad dekningsgrad) {
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var yfa = ytelsesFordelingRepository
            .opprettBuilder(behandlingId)
            .medSakskompleksDekningsgrad(dekningsgrad)
            .build();
        ytelsesFordelingRepository.lagre(behandlingId, yfa);
    }

    @Test
    void skal_utlede_startpunkt_dekningsgrad_dersom_endret_dekningsgrad_i_dekningsgrad_revurdering() {
        // Arrange
        var revurdering = opprettRevurdering(BehandlingÅrsakType.ENDRE_DEKNINGSGRAD);
        repositoryProvider.getFagsakRelasjonRepository().oppdaterDekningsgrad(revurdering.getFagsak(), Dekningsgrad._80);
        endreDekningsgrad(revurdering.getId(), Dekningsgrad._100);
        var lås = behandlingRepository.taSkriveLås(revurdering);
        var kontekst = new BehandlingskontrollKontekst(revurdering, lås);

        // Act
        steg.utførSteg(kontekst).getAksjonspunktListe();

        // Assert
        var behandlingEtterSteg = behandlingRepository.hentBehandling(revurdering.getId());
        assertThat(behandlingEtterSteg.getStartpunkt()).isEqualTo(StartpunktType.DEKNINGSGRAD);
    }

    @Test
    void skal_sette_startpunkt_inngangsvilkår_for_manuelt_opprettet_revurdering() {
        var behandling = opprettRevurdering();
        // Arrange
        var builder = BehandlingÅrsak.builder(List.of(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_SØKERS_REL));
        behandling.getBehandlingÅrsaker().add(builder.medManueltOpprettet(true).buildFor(behandling).get(0));

        // Nødvendig å sette aktivt steg for KOFAK revurdering
        forceOppdaterBehandlingSteg(behandling, BehandlingStegType.KONTROLLER_FAKTA);
        var behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);

        var lås = behandlingRepository.taSkriveLås(behandling);
        var kontekst = new BehandlingskontrollKontekst(behandling, lås);

        // Act
        steg.utførSteg(kontekst).getAksjonspunktListe();

        // Assert
        var behandlingEtterSteg = behandlingRepository.hentBehandling(behandling.getId());
        assertThat(behandlingEtterSteg.getStartpunkt()).isEqualTo(StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT);
    }

    private Behandling opprettRevurderingPgaBerørtBehandling() {
        var førstegangsbehandling = opprettFørstegangsbehandling(new Behandlingsresultat.Builder());
        repositoryProvider.getOpptjeningRepository().lagreOpptjeningsperiode(førstegangsbehandling, LocalDate.now().minusYears(1), LocalDate.now(),
                false);

        var fordeling = new OppgittFordelingEntitet(List.of(), true);
        var revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(førstegangsbehandling, BehandlingÅrsakType.BERØRT_BEHANDLING)
            .medDefaultOppgittDekningsgrad()
            .medFordeling(fordeling);
        revurderingScenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now()).build());
        revurderingScenario.medDefaultOppgittTilknytning();

        revurderingScenario.medSøknadHendelse().medFødselsDato(LocalDate.now().minusDays(10));
        var revurdering = revurderingScenario.lagre(repositoryProvider);

        // Nødvendig å sette aktivt steg for KOFAK revurdering
        forceOppdaterBehandlingSteg(revurdering, BehandlingStegType.KONTROLLER_FAKTA);
        var behandlingLås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, behandlingLås);

        return revurdering;
    }

    private Behandling opprettAutomatiskRevurderingMedÅpenOverstyring() {
        var førstegangsbehandling = opprettFørstegangsbehandling(new Behandlingsresultat.Builder());
        repositoryProvider.getOpptjeningRepository().lagreOpptjeningsperiode(førstegangsbehandling, LocalDate.now().minusYears(1), LocalDate.now(),
            false);

        var fordeling = new OppgittFordelingEntitet(List.of(), true);
        var revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(førstegangsbehandling, BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING)
            .medDefaultOppgittDekningsgrad()
            .medFordeling(fordeling);
        revurderingScenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now()).build());
        revurderingScenario.medDefaultOppgittTilknytning();

        revurderingScenario.medSøknadHendelse().medFødselsDato(LocalDate.now().minusDays(10));
        revurderingScenario.medBekreftetHendelse().medFødselsDato(LocalDate.now().minusDays(10));
        revurderingScenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.OVERSTYRING_AV_AVKLART_STARTDATO, null);

        var revurdering = revurderingScenario.lagre(repositoryProvider);

        // Nødvendig å sette aktivt steg for KOFAK revurdering
        forceOppdaterBehandlingSteg(revurdering, BehandlingStegType.KONTROLLER_FAKTA);
        var behandlingLås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, behandlingLås);

        return revurdering;
    }

    private Behandling opprettFørstegangsbehandling(Behandlingsresultat.Builder behandlingsresultat) {

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .medBehandlingsresultat(behandlingsresultat)
            .medDefaultOppgittDekningsgrad()
            .medBehandlingStegStart(BehandlingStegType.KONTROLLER_FAKTA);

        førstegangScenario.medSøknadHendelse().medFødselsDato(LocalDate.now().minusDays(10));

        førstegangScenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now()).build());

        var behandling = førstegangScenario.lagre(repositoryProvider);
        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(behandling.getFagsak());
        repositoryProvider.getFagsakRelasjonRepository().oppdaterDekningsgrad(behandling.getFagsak(), Dekningsgrad._100);
        return behandling;
    }

    private Behandling opprettRevurdering() {
        return opprettRevurdering(BehandlingÅrsakType.RE_MANGLER_FØDSEL);
    }

    private Behandling opprettRevurdering(BehandlingÅrsakType årsak) {

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
                .medBehandlingStegStart(BehandlingStegType.KONTROLLER_FAKTA)
                .medUttak(new UttakResultatPerioderEntitet());

        førstegangScenario.medDefaultOppgittTilknytning();

        var søkerAktørId = førstegangScenario.getDefaultBrukerAktørId();

        var personInformasjon = førstegangScenario
                .opprettBuilderForRegisteropplysninger()
                .medPersonas()
                .kvinne(søkerAktørId, SivilstandType.SAMBOER).statsborgerskap(Landkoder.USA)
                .build();

        førstegangScenario.medRegisterOpplysninger(personInformasjon);

        var fødselsDato = LocalDate.now().minusDays(10);
        førstegangScenario.medBekreftetHendelse().medFødselsDato(fødselsDato);

        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder()
                .medFørsteUttaksdato(LocalDate.now())
                .build();
        var foreldrepenger = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(LocalDate.now(), LocalDate.now().plusWeeks(20))
            .build();
        var fordeling = new OppgittFordelingEntitet(List.of(foreldrepenger), true);
        førstegangScenario.medAvklarteUttakDatoer(avklarteUttakDatoer)
            .medFordeling(fordeling);

        var originalBehandling = førstegangScenario.lagre(repositoryProvider);
        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(førstegangScenario.getFagsak());
        // Legg til Uttaksperiodegrense -> dessverre ikke tilgjengelig i scenariobygger
        var lås = behandlingRepository.taSkriveLås(originalBehandling);
        behandlingRepository.lagre(originalBehandling, lås);
        var uttaksperiodegrense = new Uttaksperiodegrense(LocalDate.now());
        repositoryProvider.getUttaksperiodegrenseRepository().lagre(originalBehandling.getId(), uttaksperiodegrense);
        // Legg til Opptjeningsperidoe -> dessverre ikke tilgjengelig i scenariobygger
        repositoryProvider.getOpptjeningRepository().lagreOpptjeningsperiode(originalBehandling, LocalDate.now().minusYears(1), LocalDate.now(),
                false);

        var revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(originalBehandling, årsak)
            .medFordeling(fordeling)
            .medRegisterOpplysninger(personInformasjon);
        revurderingScenario.medDefaultOppgittTilknytning();

        revurderingScenario.medAvklarteUttakDatoer(avklarteUttakDatoer);

        revurderingScenario.medBekreftetHendelse().medFødselsDato(fødselsDato);
        var behandling = revurderingScenario.lagre(repositoryProvider);

        // Nødvendig å sette aktivt steg for KOFAK revurdering
        forceOppdaterBehandlingSteg(behandling, BehandlingStegType.KONTROLLER_FAKTA);
        var behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);
        return behandling;
    }
}
