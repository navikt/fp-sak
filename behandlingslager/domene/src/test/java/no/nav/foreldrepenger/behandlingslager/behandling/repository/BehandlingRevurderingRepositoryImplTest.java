package no.nav.foreldrepenger.behandlingslager.behandling.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.vedtak.felles.testutilities.db.Repository;

public class BehandlingRevurderingRepositoryImplTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private final BehandlingRevurderingRepository behandlingRevurderingRepository = repositoryProvider.getBehandlingRevurderingRepository();
    private final BehandlingVedtakRepository behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
    private final FagsakRepository fagsakRepository = repositoryProvider.getFagsakRepository();

    private Behandling behandling;

    @Test
    public void skal_finne_henlagte_behandlinger_etter_forrige_ferdigbehandlede_søknad() {

        behandling = opprettRevurderingsKandidat();

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
        nyRevurderingsBehandling = behandlingRepository.hentBehandling(nyRevurderingsBehandling.getId());

        Long revurderingsBehandlingId = revurderingsBehandling.getId();
        List<Behandling> result = behandlingRevurderingRepository.finnHenlagteBehandlingerEtterSisteInnvilgedeIkkeHenlagteBehandling(fagsakId);
        assertThat(result).isNotEmpty();
        result.forEach(r -> assertThat(getBehandlingsresultat(r).getBehandlingResultatType()).isEqualTo(BehandlingResultatType.HENLAGT_FEILOPPRETTET));
        assertThat(result).anySatisfy(r -> r.getId().equals(revurderingsBehandlingId));
        assertThat(result).hasSize(2);
    }

    @Test
    public void skal_finne_alle_innvilgete_avsluttede_behandling_som_ikke_er_henlagt() {

        behandling = opprettRevurderingsKandidat();

        Long fagsakId = behandling.getFagsakId();

        @SuppressWarnings("unused")
        Behandling revurderingsBehandling = opprettOgLagreRevurderingMedBehandlingÅrsak();

        Behandling nyRevurderingsBehandling = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)).build();
        behandlingRepository.lagreOgClear(nyRevurderingsBehandling, behandlingRepository.taSkriveLås(nyRevurderingsBehandling));

        nyRevurderingsBehandling = behandlingRepository.hentBehandling(nyRevurderingsBehandling.getId());
        oppdaterMedBehandlingsresultatAvslagOgLagre(nyRevurderingsBehandling);
        nyRevurderingsBehandling.avsluttBehandling();
        behandlingRepository.lagreOgClear(nyRevurderingsBehandling, behandlingRepository.taSkriveLås(nyRevurderingsBehandling));
        nyRevurderingsBehandling = behandlingRepository.hentBehandling(nyRevurderingsBehandling.getId());

        List<Behandling> result = behandlingRepository.finnAlleAvsluttedeIkkeHenlagteBehandlinger(fagsakId);
        assertThat(result).isNotEmpty();
        result.forEach(r -> assertThat(getBehandlingsresultat(r).getBehandlingResultatType()).isEqualTo(BehandlingResultatType.INNVILGET));
        assertThat(result).anySatisfy(r -> r.getId().equals(behandling.getId()));
        assertThat(result).hasSize(1);
    }

    private Behandling opprettOgLagreRevurderingMedBehandlingÅrsak() {
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

        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(lagPerson()));
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.forFørstegangssøknad(fagsak).build();
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

        Repository repository = repoRule.getRepository();

        repository.lagre(behandling);

        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(getBehandlingsresultat(behandling).getVilkårResultat(), lås);
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandling.getBehandlingsresultat();
    }

    private Personinfo lagPerson() {
        final Personinfo personinfo = new Personinfo.Builder()
            .medNavn("Navn navnesen")
            .medAktørId(AktørId.dummy())
            .medFødselsdato(LocalDate.now().minusYears(20))
            .medLandkode(Landkoder.NOR)
            .medNavBrukerKjønn(NavBrukerKjønn.KVINNE)
            .medPersonIdent(new PersonIdent("12345678901"))
            .medForetrukketSpråk(Språkkode.nb)
            .build();
        return personinfo;
    }
}
