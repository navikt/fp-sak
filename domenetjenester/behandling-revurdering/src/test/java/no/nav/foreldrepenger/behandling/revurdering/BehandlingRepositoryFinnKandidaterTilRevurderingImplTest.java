package no.nav.foreldrepenger.behandling.revurdering;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.Etterkontroll;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.KontrollType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioKlageEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class BehandlingRepositoryFinnKandidaterTilRevurderingImplTest {

    private final static int revurderingDagerTilbake = 0;
    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private BehandlingVedtakRepository behandlingVedtakRepository;
    
    @Inject
    private EtterkontrollRepository etterkontrollRepository;
    
    @Inject
    private KlageRepository klageRepository;

    private Behandling behandling;

    @Test
    public void skal_finne_kandidat_til_revurdering() {
        Behandling behandling = opprettRevurderingsKandidat(revurderingDagerTilbake + 2);

        Etterkontroll etterkontroll = new Etterkontroll.Builder(behandling.getFagsakId()).medErBehandlet(false).medKontrollTidspunkt(LocalDate.now().atStartOfDay().minusDays(revurderingDagerTilbake)).medKontrollType(KontrollType.MANGLENDE_FØDSEL).build();
        etterkontrollRepository.lagre(etterkontroll);

        final List<Behandling> behandlings = etterkontrollRepository
                .finnKandidaterForAutomatiskEtterkontroll(Period.parse("P" + revurderingDagerTilbake + "D"));

        assertThat(behandlings).hasSize(1);
        assertThat(behandlings.get(0).getId()).isEqualTo(behandling.getId());
    }


    @Test
    public void sjekk_at_behandlingen_blir_etterkontrollert_og_ikke_klagen() {
        ScenarioKlageEngangsstønad scenario = ScenarioKlageEngangsstønad.forUtenVurderingResultat(ScenarioMorSøkerEngangsstønad.forAdopsjon());
        Behandling klageBehandling = scenario.lagre(repositoryProvider, klageRepository);

        Etterkontroll etterkontroll = new Etterkontroll.Builder(klageBehandling.getFagsakId()).medErBehandlet(false).medKontrollTidspunkt(LocalDate.now().atStartOfDay().minusDays(revurderingDagerTilbake)).medKontrollType(KontrollType.MANGLENDE_FØDSEL).build();
        etterkontrollRepository.lagre(etterkontroll);

        final List<Behandling> behandlings = etterkontrollRepository
            .finnKandidaterForAutomatiskEtterkontroll(Period.parse("P" + revurderingDagerTilbake + "D"));

        assertThat(behandlings).hasSize(1);
        assertThat(behandlings.get(0).getId()).isNotEqualTo(klageBehandling.getId());
    }


    @Test
    public void behandling_som_har_vært_etterkontrollert_skal_ikke_være_kandidat_til_revurdering() {
        Behandling behandling = opprettRevurderingsKandidat(revurderingDagerTilbake + 2);

        Etterkontroll etterkontroll = new Etterkontroll.Builder(behandling.getFagsakId()).medErBehandlet(false).medKontrollTidspunkt(LocalDate.now().atStartOfDay().minusDays(revurderingDagerTilbake)).medKontrollType(KontrollType.MANGLENDE_FØDSEL).build();
        etterkontrollRepository.lagre(etterkontroll);

        etterkontrollRepository.avflaggDersomEksisterer(behandling.getFagsakId(),KontrollType.MANGLENDE_FØDSEL);

        final List<Behandling> behandlings = etterkontrollRepository
            .finnKandidaterForAutomatiskEtterkontroll(Period.parse("P" + revurderingDagerTilbake + "D"));

        assertThat(behandlings).isEmpty();
    }

    @Test
    public void skal_ikke_velge_henlagt_behandling() {
        Behandling behandling = opprettRevurderingsKandidat(revurderingDagerTilbake);

        Behandlingsresultat innvilget = new Behandlingsresultat.Builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET)
            .buildFor(behandling);
        behandling.setBehandlingresultat(innvilget);
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        Etterkontroll etterkontroll = new Etterkontroll.Builder(behandling.getFagsakId()).medErBehandlet(false).medKontrollTidspunkt(LocalDate.now().atStartOfDay().minusDays(revurderingDagerTilbake)).medKontrollType(KontrollType.MANGLENDE_FØDSEL).build();
        etterkontrollRepository.lagre(etterkontroll);

        Behandling henlagtBehandling = Behandling.fraTidligereBehandling(behandling, BehandlingType.FØRSTEGANGSSØKNAD).build();
        Behandlingsresultat henlagt = new Behandlingsresultat.Builder()
            .medBehandlingResultatType(BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET).buildFor(henlagtBehandling);
        henlagtBehandling.setBehandlingresultat(henlagt);
        henlagtBehandling.avsluttBehandling();
        behandlingRepository.lagre(henlagtBehandling, behandlingRepository.taSkriveLås(henlagtBehandling));

        final List<Behandling> behandlings = etterkontrollRepository
            .finnKandidaterForAutomatiskEtterkontroll(Period.parse("P0D"));

        assertThat(behandlings).hasSize(1);
        assertThat(behandlings.get(0).getId()).isEqualTo(behandling.getId());
    }

    @Test
    public void fagsak_som_har_eksisterende_etterkontrollsbehandling_skal_ikke_være_kandidat_til_revurdering() {
        Behandling behandling = opprettRevurderingsKandidat(revurderingDagerTilbake + 2);

        Etterkontroll etterkontroll = new Etterkontroll.Builder(behandling.getFagsakId()).medErBehandlet(false).medKontrollTidspunkt(LocalDate.now().atStartOfDay().minusDays(revurderingDagerTilbake)).medKontrollType(KontrollType.MANGLENDE_FØDSEL).build();
        etterkontrollRepository.lagre(etterkontroll);

        Behandling revurderingsBehandling = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
                .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN)).build();

        behandlingRepository.lagre(revurderingsBehandling, behandlingRepository.taSkriveLås(revurderingsBehandling));

        etterkontrollRepository.avflaggDersomEksisterer(behandling.getFagsakId(), KontrollType.MANGLENDE_FØDSEL);

        List<Behandling> fagsakList = etterkontrollRepository
                .finnKandidaterForAutomatiskEtterkontroll(Period.parse("P" + revurderingDagerTilbake + "D"));

        assertThat(fagsakList).isEmpty();
    }

    @Test
    public void skal_hente_ut_siste_vedtak_til_revurdering() {
        final FamilieHendelseRepository grunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
        Behandling behandling = opprettRevurderingsKandidat(revurderingDagerTilbake + 2);
        LocalDate terminDato = LocalDate.now().minusDays(revurderingDagerTilbake + 2);

        Behandling.Builder revurderingBuilder = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
                .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ANNET));
        Behandling revurderingsBehandling = revurderingBuilder.build();

        Behandlingsresultat behandlingsresultat = Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET)
                .buildFor(revurderingsBehandling);
        final BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder().medVedtakstidspunkt(LocalDateTime.now())
                .medBehandlingsresultat(behandlingsresultat).medVedtakResultatType(VedtakResultatType.INNVILGET)
                .medAnsvarligSaksbehandler("asdf").build();
        revurderingsBehandling.avsluttBehandling();
        behandlingRepository.lagre(revurderingsBehandling, behandlingRepository.taSkriveLås(revurderingsBehandling));
        grunnlagRepository.kopierGrunnlagFraEksisterendeBehandling(behandling.getId(), revurderingsBehandling.getId());
        final FamilieHendelseBuilder oppdatere = grunnlagRepository.opprettBuilderFor(revurderingsBehandling);
        oppdatere.medTerminbekreftelse(oppdatere.getTerminbekreftelseBuilder()
                .medTermindato(terminDato)
                .medNavnPå("Lege Legsen")
                .medUtstedtDato(terminDato.minusDays(40)))
                .medAntallBarn(1);
        grunnlagRepository.lagre(revurderingsBehandling, oppdatere);
        behandlingVedtakRepository.lagre(behandlingVedtak, behandlingRepository.taSkriveLås(revurderingsBehandling));

        Etterkontroll etterkontroll = new Etterkontroll.Builder(revurderingsBehandling.getFagsakId()).medErBehandlet(false).medKontrollTidspunkt(LocalDate.now().atStartOfDay().minusDays(revurderingDagerTilbake)).medKontrollType(KontrollType.MANGLENDE_FØDSEL).build();
        etterkontrollRepository.lagre(etterkontroll);

        List<Behandling> fagsakList = etterkontrollRepository
                .finnKandidaterForAutomatiskEtterkontroll(Period.parse("P" + revurderingDagerTilbake + "D"));

        assertThat(fagsakList).containsOnly(revurderingsBehandling);
    }

    @Test
    public void behandling_med_nyere_termindato_skal_ikke_være_kandidat_til_revurdering() {
        opprettRevurderingsKandidat(0);

        Etterkontroll etterkontroll = new Etterkontroll.Builder(behandling.getFagsakId()).medErBehandlet(false).medKontrollTidspunkt(LocalDate.now().atStartOfDay().minusDays(revurderingDagerTilbake)).medKontrollType(KontrollType.MANGLENDE_FØDSEL).build();
        etterkontrollRepository.lagre(etterkontroll);

        List<Behandling> fagsakList = etterkontrollRepository
                .finnKandidaterForAutomatiskEtterkontroll(Period.parse("P5D"));

        assertThat(fagsakList).isEmpty();
    }

    private Behandling opprettRevurderingsKandidat(int dagerTilbake) {
        LocalDate terminDato = LocalDate.now().minusDays(dagerTilbake);
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse()
                .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                        .medTermindato(terminDato)
                        .medUtstedtDato(LocalDate.now())
                        .medNavnPå("Lege Legesen"))
                .medAntallBarn(1);
        scenario.medBekreftetHendelse()
                .medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                        .medTermindato(terminDato)
                        .medNavnPå("LEGEN MIN")
                        .medUtstedtDato(terminDato.minusDays(40)))
                .medAntallBarn(1);

        behandling = scenario.lagre(repositoryProvider);
        Behandlingsresultat behandlingsresultat = Behandlingsresultat.builder()
                .medBehandlingResultatType(BehandlingResultatType.INNVILGET).buildFor(behandling);
        final BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder().medVedtakstidspunkt(LocalDateTime.now().minusDays(1))
                .medBehandlingsresultat(behandlingsresultat).medVedtakResultatType(VedtakResultatType.INNVILGET)
                .medAnsvarligSaksbehandler("asdf").build();

        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        behandlingVedtakRepository.lagre(behandlingVedtak, behandlingRepository.taSkriveLås(behandling));

        return behandling;
    }

    @Test
    public void skal_finne_nyeste_innvilgete_avsluttede_behandling_som_ikke_er_henlagt() {
        Behandling behandling = opprettRevurderingsKandidat(revurderingDagerTilbake + 2);

        Behandlingsresultat innvilget = new Behandlingsresultat.Builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET)
                .buildFor(behandling);
        behandling.setBehandlingresultat(innvilget);
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        Behandling henlagtBehandling = Behandling.fraTidligereBehandling(behandling, BehandlingType.FØRSTEGANGSSØKNAD).build();
        Behandlingsresultat henlagt = new Behandlingsresultat.Builder()
                .medBehandlingResultatType(BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET).buildFor(henlagtBehandling);
        henlagtBehandling.setBehandlingresultat(henlagt);
        henlagtBehandling.avsluttBehandling();
        behandlingRepository.lagre(henlagtBehandling, behandlingRepository.taSkriveLås(henlagtBehandling));

        Optional<Behandling> resultatOpt = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandling.getFagsak().getId());
        assertThat(resultatOpt).hasValueSatisfying(resultat ->
            assertThat(resultat.getId()).isEqualTo(behandling.getId())
        );
    }

}
