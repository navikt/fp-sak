package no.nav.foreldrepenger.behandlingslager.behandling.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;
import no.nav.vedtak.felles.testutilities.db.Repository;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class BehandlingRevurderingRepositoryTest extends EntityManagerAwareTest {

    private BehandlingRepository behandlingRepository;
    private BehandlingRevurderingRepository behandlingRevurderingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private FagsakRepository fagsakRepository;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        behandlingRepository = new BehandlingRepository(entityManager);
        var ytelsesFordelingRepository = new YtelsesFordelingRepository(entityManager);
        var fagsakLåsRepository = new FagsakLåsRepository(entityManager);
        var fagsakRelasjonRepository = new FagsakRelasjonRepository(entityManager, ytelsesFordelingRepository, fagsakLåsRepository);
        var søknadRepository = new SøknadRepository(entityManager, behandlingRepository);
        var behandlingLåsRepository = new BehandlingLåsRepository(entityManager);
        behandlingRevurderingRepository = new BehandlingRevurderingRepository(entityManager,
            behandlingRepository, fagsakRelasjonRepository, søknadRepository, behandlingLåsRepository);
        behandlingVedtakRepository = new BehandlingVedtakRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
    }

    @Test
    public void skal_finne_henlagte_behandlinger_etter_forrige_ferdigbehandlede_søknad() {

        var behandling = opprettRevurderingsKandidat();

        Long fagsakId = behandling.getFagsakId();

        Behandling revurderingsBehandling = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN)).build();
        behandlingRepository.lagreOgClear(revurderingsBehandling, behandlingRepository.taSkriveLås(revurderingsBehandling));

        revurderingsBehandling = behandlingRepository.hentBehandling(revurderingsBehandling.getId());
        oppdaterMedBehandlingsresultatAvslagOgLagre(revurderingsBehandling);
        revurderingsBehandling.avsluttBehandling();
        behandlingRepository.lagreOgClear(revurderingsBehandling, behandlingRepository.taSkriveLås(revurderingsBehandling));
        revurderingsBehandling = behandlingRepository.hentBehandling(revurderingsBehandling.getId());

        Behandling nyRevurderingsBehandling = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)).build();
        behandlingRepository.lagreOgClear(nyRevurderingsBehandling, behandlingRepository.taSkriveLås(nyRevurderingsBehandling));

        nyRevurderingsBehandling = behandlingRepository.hentBehandling(nyRevurderingsBehandling.getId());
        oppdaterMedBehandlingsresultatAvslagOgLagre(nyRevurderingsBehandling);
        nyRevurderingsBehandling.avsluttBehandling();
        behandlingRepository.lagreOgClear(nyRevurderingsBehandling, behandlingRepository.taSkriveLås(nyRevurderingsBehandling));

        Long revurderingsBehandlingId = revurderingsBehandling.getId();
        List<Behandling> result = behandlingRevurderingRepository.finnHenlagteBehandlingerEtterSisteInnvilgedeIkkeHenlagteBehandling(fagsakId);
        assertThat(result).isNotEmpty();
        result.forEach(r -> assertThat(getBehandlingsresultat(r).getBehandlingResultatType()).isEqualTo(BehandlingResultatType.HENLAGT_FEILOPPRETTET));
        assertThat(result).anyMatch(r -> r.getId().equals(revurderingsBehandlingId));
        assertThat(result).hasSize(2);
    }

    @Test
    public void skal_finne_alle_innvilgete_avsluttede_behandling_som_ikke_er_henlagt() {

        var behandling = opprettRevurderingsKandidat();

        Long fagsakId = behandling.getFagsakId();

        opprettOgLagreRevurderingMedBehandlingÅrsak(behandling);

        Behandling nyRevurderingsBehandling = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)).build();
        behandlingRepository.lagreOgClear(nyRevurderingsBehandling, behandlingRepository.taSkriveLås(nyRevurderingsBehandling));

        nyRevurderingsBehandling = behandlingRepository.hentBehandling(nyRevurderingsBehandling.getId());
        oppdaterMedBehandlingsresultatAvslagOgLagre(nyRevurderingsBehandling);
        nyRevurderingsBehandling.avsluttBehandling();
        behandlingRepository.lagreOgClear(nyRevurderingsBehandling, behandlingRepository.taSkriveLås(nyRevurderingsBehandling));

        List<Behandling> result = behandlingRepository.finnAlleAvsluttedeIkkeHenlagteBehandlinger(fagsakId);
        assertThat(result).isNotEmpty();
        result.forEach(r -> assertThat(getBehandlingsresultat(r).getBehandlingResultatType()).isEqualTo(BehandlingResultatType.INNVILGET));
        assertThat(result).anyMatch(r -> r.getId().equals(behandling.getId()));
        assertThat(result).hasSize(1);
    }

    private Behandling opprettOgLagreRevurderingMedBehandlingÅrsak(Behandling behandling) {
        Behandling revurderingsBehandling = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN)).build();
        behandlingRepository.lagreOgClear(revurderingsBehandling, behandlingRepository.taSkriveLås(revurderingsBehandling));

        revurderingsBehandling = behandlingRepository.hentBehandling(revurderingsBehandling.getId());
        oppdaterMedBehandlingsresultatAvslagOgLagre(revurderingsBehandling);
        revurderingsBehandling.avsluttBehandling();
        behandlingRepository.lagreOgClear(revurderingsBehandling, behandlingRepository.taSkriveLås(revurderingsBehandling));
        revurderingsBehandling = behandlingRepository.hentBehandling(revurderingsBehandling.getId());
        return revurderingsBehandling;
    }


    private Behandling opprettRevurderingsKandidat() {

        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak);
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        Behandlingsresultat behandlingsresultat = Behandlingsresultat.builder()
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET).buildFor(behandling);
        final BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder().medVedtakstidspunkt(LocalDateTime.now()).medBehandlingsresultat(behandlingsresultat)
            .medVedtakResultatType(VedtakResultatType.INNVILGET).medAnsvarligSaksbehandler("asdf").build();
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        behandlingVedtakRepository.lagre(behandlingVedtak, behandlingRepository.taSkriveLås(behandling));

        return behandling;
    }

    private void oppdaterMedBehandlingsresultatAvslagOgLagre(Behandling behandling) {
        VilkårResultat.builder()
            .leggTilVilkårResultat(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.IKKE_OPPFYLT,
                null, new Properties(), null, false, false, null, null)
            .medVilkårResultatType(VilkårResultatType.AVSLÅTT)
            .buildFor(behandling);

        Behandlingsresultat.builderEndreEksisterende(getBehandlingsresultat(behandling))
            .medBehandlingResultatType(BehandlingResultatType.HENLAGT_FEILOPPRETTET);

        Repository repository = new Repository(getEntityManager());

        repository.lagre(behandling);

        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(getBehandlingsresultat(behandling).getVilkårResultat(), lås);
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandling.getBehandlingsresultat();
    }
}
