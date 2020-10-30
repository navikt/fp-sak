package no.nav.foreldrepenger.behandlingslager.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingKandidaterRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.NavBrukerBuilder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioKlageEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.testutilities.db.Repository;

public class BehandlingRepositoryTest extends EntityManagerAwareTest {

    private static final String ANSVARLIG_SAKSBEHANDLER = "Ansvarlig Saksbehandler";
    private final static int REVURDERING_DAGER_TILBAKE = 60;

    private BehandlingRepositoryProvider repositoryProvider;

    private BehandlingRepository behandlingRepository;

    private BehandlingKandidaterRepository behandlingKandidaterRepository;

    private BehandlingVedtakRepository behandlingVedtakRepository;

    private BehandlingsresultatRepository behandlingsresultatRepository;

    private FagsakRepository fagsakRepository;

    private KlageRepository klageRepository;

    private LegacyESBeregningRepository beregningRepository;

    private Saksnummer saksnummer = new Saksnummer("2");
    private Fagsak fagsak = FagsakBuilder.nyEngangstønadForMor().medSaksnummer(saksnummer).build();
    private Behandling behandling;

    private LocalDateTime imorgen = LocalDateTime.now().plusDays(1);
    private LocalDateTime igår = LocalDateTime.now().minusDays(1);

    @BeforeEach
    public void setUp() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        behandlingKandidaterRepository = new BehandlingKandidaterRepository(entityManager);
        behandlingVedtakRepository = new BehandlingVedtakRepository(entityManager);
        behandlingsresultatRepository = new BehandlingsresultatRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        klageRepository = new KlageRepository(entityManager);
        beregningRepository = new LegacyESBeregningRepository(entityManager);
    }

    @Test
    public void skal_finne_behandling_gitt_id() {

        // Arrange
        Behandling behandling = opprettBuilderForBehandling().build();
        lagreBehandling(behandling);

        // Act
        Behandling resultat = behandlingRepository.hentBehandling(behandling.getId());

        // Assert
        assertThat(resultat).isNotNull();
    }

    private void lagreBehandling(Behandling... behandlinger) {
        for (Behandling behandling : behandlinger) {
            BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
            behandlingRepository.lagre(behandling, lås);
        }
    }

    @Test
    public void skal_hente_alle_behandlinger_fra_fagsak() {

        Behandling.Builder builder = opprettBuilderForBehandling();
        lagreBehandling(builder);

        List<Behandling> behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(saksnummer);

        assertThat(behandlinger).hasSize(1);

    }

    private void lagreBehandling(Behandling.Builder builder) {
        Behandling behandling = builder.build();
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
    }

    @Test
    public void skal_finne_behandling_med_årsak() {
        Behandling behandling = opprettRevurderingsKandidat(REVURDERING_DAGER_TILBAKE + 2);

        Behandling revurderingsBehandling = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN)).build();

        behandlingRepository.lagre(revurderingsBehandling, behandlingRepository.taSkriveLås(revurderingsBehandling));

        List<Behandling> result = behandlingRepository.hentBehandlingerMedÅrsakerForFagsakId(behandling.getFagsakId(),
            BehandlingÅrsakType.årsakerForEtterkontroll());
        assertThat(result).isNotEmpty();
    }

    @Test
    public void skal_hente_siste_behandling_basert_på_fagsakId() {

        Behandling.Builder builder = opprettBuilderForBehandling();

        lagreBehandling(builder);

        Optional<Behandling> sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());

        assertThat(sisteBehandling).isPresent();
        assertThat(sisteBehandling.get().getFagsakId()).isEqualTo(fagsak.getId());
    }

    @Test
    public void skal_hente_siste_klage_basert_på_fagsakId() {

        Behandling.Builder builder = opprettBuilderForBehandling();

        lagreBehandling(builder);

        Optional<Behandling> sisteBehandling = behandlingRepository.finnSisteIkkeHenlagteBehandlingavAvBehandlingTypeFor(fagsak.getId(), BehandlingType.KLAGE);

        assertThat(sisteBehandling).isEmpty();

        // Part 2
        Behandling klage = Behandling.forKlage(fagsak).build();

        BehandlingLås lås = behandlingRepository.taSkriveLås(klage);
        behandlingRepository.lagre(klage, lås);

        Optional<Behandling> sisteKlage = behandlingRepository.finnSisteIkkeHenlagteBehandlingavAvBehandlingTypeFor(fagsak.getId(), BehandlingType.KLAGE);

        assertThat(sisteKlage).isPresent();
        assertThat(sisteKlage.get().getFagsakId()).isEqualTo(fagsak.getId());
    }

    @Test
    public void skal_hente_siste_innvilget_eller_endret_på_fagsakId() {
        BehandlingVedtak.Builder forVedtak = opprettBuilderForVedtak();
        Behandlingsresultat behandlingsresultat = getBehandlingsresultat(behandling);
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultat).medBehandlingResultatType(BehandlingResultatType.INNVILGET);

        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingsresultatRepository.lagre(behandling.getId(), behandlingsresultat);
        behandlingVedtakRepository.lagre(forVedtak.medBehandlingsresultat(getBehandlingsresultat(behandling)).medIverksettingStatus(IverksettingStatus.IVERKSATT).build(), lås);
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, lås);

        Optional<Behandling> innvilgetBehandling = behandlingRepository.finnSisteInnvilgetBehandling(behandling.getFagsakId());

        assertThat(innvilgetBehandling).isPresent();
        assertThat(innvilgetBehandling.get().getFagsakId()).isEqualTo(behandling.getFagsak().getId());
    }

    @Test
    public void skal_hente_siste_behandling_ekskluder_basert_på_fagsakId() {
        ScenarioKlageEngangsstønad scenario = ScenarioKlageEngangsstønad.forUtenVurderingResultat(ScenarioMorSøkerEngangsstønad.forAdopsjon());
        Behandling klageBehandling = scenario.lagre(repositoryProvider, klageRepository);

        List<Behandling> alleBehandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(klageBehandling.getFagsak().getSaksnummer());
        assertThat(alleBehandlinger).as("Forventer at alle behandlinger opprettet skal eksistere").hasSize(2);

        Optional<Behandling> sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(klageBehandling.getFagsak().getId());

        assertThat(sisteBehandling).isPresent();
        assertThat(sisteBehandling.get().getFagsakId()).isEqualTo(klageBehandling.getFagsak().getId());
        assertThat(sisteBehandling.get().getType()).isNotEqualTo(BehandlingType.KLAGE);
    }

    @Test
    public void skal_kunne_lagre_vedtak() {
        BehandlingVedtak vedtak = opprettBuilderForVedtak().build();

        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);

        behandlingRepository.lagre(behandling, lås);
        behandlingVedtakRepository.lagre(vedtak, lås);

        Long id = vedtak.getId();
        assertThat(id).isNotNull();

        BehandlingVedtak vedtakLest = behandlingVedtakRepository.hentForBehandling(vedtak.getBehandlingsresultat().getBehandlingId());
        assertThat(vedtakLest).isNotNull();

    }

    @Test
    public void skal_finne_behandling_gitt_korrekt_uuid() {
        // Arrange
        Behandling behandling = opprettBuilderForBehandling().build();
        lagreBehandling(behandling);

        // Act
        Optional<Behandling> resultat = behandlingRepository.hentBehandlingHvisFinnes(behandling.getUuid());

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get()).isEqualTo(behandling);
    }

    @Test
    public void skal_ikke_finne_behandling_gitt_feil_uuid() {
        // Arrange
        Behandling behandling = opprettBuilderForBehandling().build();
        lagreBehandling(behandling);

        // Act
        Optional<Behandling> resultat = behandlingRepository.hentBehandlingHvisFinnes(UUID.randomUUID());

        // Assert
        assertThat(resultat).isNotPresent();
    }


    @Test
    public void skal_kunne_lagre_konsekvens_for_ytelsen() {
        behandling = opprettBehandlingMedTermindato();
        Behandlingsresultat behandlingsresultat = oppdaterMedBehandlingsresultatOgLagre(behandling, true, false);

        setKonsekvensForYtelsen(behandlingsresultat, List.of(KonsekvensForYtelsen.ENDRING_I_BEREGNING, KonsekvensForYtelsen.ENDRING_I_UTTAK));
        var brKonsekvenser = behandlingsresultatRepository.hent(behandling.getId()).getKonsekvenserForYtelsen();
        assertThat(brKonsekvenser).containsExactlyInAnyOrder(KonsekvensForYtelsen.ENDRING_I_BEREGNING, KonsekvensForYtelsen.ENDRING_I_UTTAK);
    }

    @Test
    public void dersom_man_lagrer_konsekvens_for_ytelsen_flere_ganger_skal_kun_den_siste_lagringen_gjelde() {
        behandling = opprettBehandlingMedTermindato();
        Behandlingsresultat behandlingsresultat = oppdaterMedBehandlingsresultatOgLagre(behandling, true, false);

        setKonsekvensForYtelsen(behandlingsresultat, List.of(KonsekvensForYtelsen.ENDRING_I_BEREGNING, KonsekvensForYtelsen.ENDRING_I_UTTAK));
        behandling = behandlingRepository.hentBehandling(behandling.getId());
        Behandlingsresultat.builderEndreEksisterende(getBehandlingsresultat(behandling)).fjernKonsekvenserForYtelsen();
        setKonsekvensForYtelsen(getBehandlingsresultat(behandling), List.of(KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN));

        var brKonsekvenser = behandlingsresultatRepository.hent(behandling.getId())
            .getKonsekvenserForYtelsen();
        assertThat(brKonsekvenser).hasSize(1);
        assertThat(brKonsekvenser).containsExactlyInAnyOrder(KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN);
    }

    private void setKonsekvensForYtelsen(Behandlingsresultat behandlingsresultat, List<KonsekvensForYtelsen> konsekvenserForYtelsen) {
        Behandlingsresultat.Builder builder = Behandlingsresultat.builderEndreEksisterende(behandlingsresultat);
        konsekvenserForYtelsen.forEach(builder::leggTilKonsekvensForYtelsen);
        builder.buildFor(behandling);

        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
    }

    @Test
    public void skal_hente_liste_over_revurderingsaarsaker() {
        Map<String, VurderÅrsak> stringVurderÅrsakMap = VurderÅrsak.kodeMap();
        assertThat(stringVurderÅrsakMap).hasSize(5);
        assertThat(stringVurderÅrsakMap.containsValue(VurderÅrsak.FEIL_FAKTA)).isTrue();
    }

    @Test
    public void skal_slette_vilkår_som_blir_fjernet_til_tross_for_at_Hibernate_har_problemer_med_orphan_removal() {
        // Arrange
        Fagsak fagsak = byggFagsak(AktørId.dummy(), RelasjonsRolleType.MORA, NavBrukerKjønn.KVINNE);
        behandling = byggBehandlingForElektroniskSøknadOmFødsel(fagsak, LocalDate.now(), LocalDate.now());

        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        VilkårResultat vilkårResultat = VilkårResultat.builder()
            .leggTilVilkår(VilkårType.OMSORGSVILKÅRET, VilkårUtfallType.IKKE_VURDERT)
            .buildFor(behandling);

        // Act
        behandlingRepository.lagre(vilkårResultat, lås);

        behandlingRepository.lagre(behandling, lås);

        // Assert
        assertThat(vilkårResultat.getVilkårene()).hasSize(1);
        assertThat(vilkårResultat.getVilkårene().iterator().next().getVilkårType()).isEqualTo(VilkårType.OMSORGSVILKÅRET);

        // Arrange
        VilkårResultat.builderFraEksisterende(vilkårResultat)
            .leggTilVilkår(VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD, VilkårUtfallType.IKKE_VURDERT)
            .fjernVilkår(VilkårType.OMSORGSVILKÅRET)
            .buildFor(behandling);

        // Act
        behandlingRepository.lagre(behandling, lås);
        var vilkårId = behandlingRepository.lagre(vilkårResultat, lås);

        // Assert
        Behandling opphentetBehandling = behandlingRepository.hentBehandling(behandling.getId());
        assertThat(getBehandlingsresultat(opphentetBehandling).getVilkårResultat().getVilkårene()).hasSize(1);
        assertThat(getBehandlingsresultat(opphentetBehandling).getVilkårResultat().getVilkårene().iterator().next().getVilkårType())
            .isEqualTo(VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD);
        var vilkårResultat1 = new VilkårResultatRepository(getEntityManager()).hentHvisEksisterer(behandling.getId());
        assertThat(vilkårResultat1).isPresent();
        assertThat(vilkårResultat1.get().getVilkårene()).isEqualTo(getBehandlingsresultat(opphentetBehandling).getVilkårResultat().getVilkårene());
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
    }


    @Test
    public void skal_finne_for_automatisk_gjenopptagelse_naar_alle_kriterier_oppfylt() {

        // Arrange
        Behandling behandling1 = opprettBehandlingForAutomatiskGjenopptagelse();
        opprettAksjonspunkt(behandling1, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, igår);
        opprettAksjonspunkt(behandling1, AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD, igår);

        Behandling behandling2 = opprettBehandlingForAutomatiskGjenopptagelse();
        opprettAksjonspunkt(behandling2, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, igår);

        Behandling behandling3 = opprettBehandlingForAutomatiskGjenopptagelse();
        opprettAksjonspunkt(behandling3, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, igår);
        lagreBehandling(behandling1, behandling2, behandling3);

        // Act
        List<Behandling> liste = behandlingKandidaterRepository.finnBehandlingerForAutomatiskGjenopptagelse();

        // Assert
        assertThat(liste).hasSize(3);
        assertThat(liste).contains(behandling1);
        assertThat(liste).contains(behandling2);
        assertThat(liste).contains(behandling3);
    }

    @Test
    public void skal_ikke_finne_for_automatisk_gjenopptagelse_naar_naar_manuelt_aksjonspunkt() {

        // Arrange
        Behandling behandling1 = opprettBehandlingForAutomatiskGjenopptagelse();
        opprettAksjonspunkt(behandling1, AksjonspunktDefinisjon.MANUELL_VURDERING_AV_OMSORGSVILKÅRET, igår);
        lagreBehandling(behandling1);

        // Act
        List<Behandling> liste = behandlingKandidaterRepository.finnBehandlingerForAutomatiskGjenopptagelse();

        // Assert
        assertThat(liste).isEmpty();
    }

    @Test
    public void skal_ikke_finne_for_automatisk_gjenopptagelse_naar_naar_lukket_aksjonspunkt() {
        Behandling behandling1 = opprettBehandlingForAutomatiskGjenopptagelse();
        Aksjonspunkt aksjonspunkt = opprettAksjonspunkt(behandling1, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, igår);
        AksjonspunktTestSupport.setTilUtført(aksjonspunkt, "ferdig");
        lagreBehandling(behandling1);

        // Act
        List<Behandling> liste = behandlingKandidaterRepository.finnBehandlingerForAutomatiskGjenopptagelse();

        // Assert
        assertThat(liste).isEmpty();
    }

    @Test
    public void skal_ikke_finne_for_automatisk_gjenopptagelse_naar_aksjonspunkt_frist_ikke_utgaatt() {

        // Arrange
        Behandling behandling1 = opprettBehandlingForAutomatiskGjenopptagelse();
        opprettAksjonspunkt(behandling1, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, imorgen);

        // Act
        List<Behandling> liste = behandlingKandidaterRepository.finnBehandlingerForAutomatiskGjenopptagelse();

        // Assert
        assertThat(liste).isEmpty();
    }

    @Test
    public void skal_ikke_finne_for_automatisk_gjenopptagelse_naar_aksjonspunkt_er_køet() {

        // Arrange
        Behandling behandling1 = opprettBehandlingForAutomatiskGjenopptagelse();
        opprettAksjonspunkt(behandling1, AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING, imorgen);

        // Act
        List<Behandling> liste = behandlingKandidaterRepository.finnBehandlingerForAutomatiskGjenopptagelse();

        // Assert
        assertThat(liste).isEmpty();
    }

    @Test
    public void skal_ikke_finne_for_automatisk_gjenopptagelse_når_aksjonspunt_er_avbrutt() throws Exception {
        // Arrange
        Behandling behandling = opprettBehandlingForAutomatiskGjenopptagelse();
        Aksjonspunkt aksjonspunkt = opprettAksjonspunkt(behandling, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, igår);
        AksjonspunktTestSupport.setTilAvbrutt(aksjonspunkt);
        lagreBehandling(behandling);

        // Act
        List<Behandling> liste = behandlingKandidaterRepository.finnBehandlingerForAutomatiskGjenopptagelse();

        // Assert
        assertThat(liste).isEmpty();
    }

    @Test
    public void skal_finne_for_gjenopplivelse_naar_alle_kriterier_oppfylt() {

        // Arrange
        Behandling behandling1 = opprettBehandlingForAutomatiskGjenopptagelse();
        opprettAksjonspunkt(behandling1, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, igår);
        opprettAksjonspunkt(behandling1, AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD, igår);

        Behandling behandling2 = opprettBehandlingForAutomatiskGjenopptagelse();
        Aksjonspunkt ap2 = opprettAksjonspunkt(behandling2, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, igår);

        Behandling behandling3 = opprettBehandlingForAutomatiskGjenopptagelse();
        Aksjonspunkt ap3 = opprettAksjonspunkt(behandling3, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, igår);
        lagreBehandling(behandling1, behandling2, behandling3);

        // Act
        List<Behandling> liste = behandlingKandidaterRepository.finnÅpneBehandlingerUtenÅpneAksjonspunktEllerAutopunkt();

        // Assert
        assertThat(liste).doesNotContain(behandling1, behandling2, behandling3);

        // Arrange
        AksjonspunktTestSupport.setTilUtført(ap2, "Begrunnelse");
        AksjonspunktTestSupport.setTilUtført(ap3, "Begrunnelse");
        lagreBehandling(behandling2, behandling3);

        // Act
        liste = behandlingKandidaterRepository.finnÅpneBehandlingerUtenÅpneAksjonspunktEllerAutopunkt();

        // Assert
        assertThat(liste).contains(behandling2, behandling3);
    }


    @Test
    public void skal_finne_førstegangsbehandling_naar_frist_er_utgatt() {
        // Arrange
        LocalDate tidsfrist = LocalDate.now().minusDays(1);
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medBehandlingstidFrist(tidsfrist);
        FamilieHendelseBuilder familieHendelseBuilder = scenario.medSøknadHendelse();
        familieHendelseBuilder.medAntallBarn(1)
            .medFødselsDato(LocalDate.now());
        scenario.lagre(repositoryProvider);

        // Act
        List<Behandling> liste = behandlingKandidaterRepository.finnBehandlingerMedUtløptBehandlingsfrist();

        // Assert
        assertThat(liste).hasSize(1);
    }

    @Test
    public void skal_ikke_finne_revurderingsbehandling() {
        // Arrange
        Behandling behandling = opprettRevurderingsKandidat(REVURDERING_DAGER_TILBAKE + 2);

        LocalDate tidsfrist = LocalDate.now().minusDays(1);
        Behandling revurderingsBehandling = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medBehandlingstidFrist(tidsfrist).build();
        //Tidsfristen blir overstyrt
        revurderingsBehandling.setBehandlingstidFrist(tidsfrist);
        behandlingRepository.lagre(revurderingsBehandling, behandlingRepository.taSkriveLås(revurderingsBehandling));

        // Act
        List<Behandling> liste = behandlingKandidaterRepository.finnBehandlingerMedUtløptBehandlingsfrist();

        // Assert
        assertThat(liste).isEmpty();
    }

    @Test
    public void skal_finne_revurderingsbehandling_med_endringssøknad() {
        // Arrange
        Behandling behandling = opprettRevurderingsKandidat(REVURDERING_DAGER_TILBAKE + 2);

        LocalDate tidsfrist = LocalDate.now().minusDays(1);
        Behandling revurderingsBehandling = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medBehandlingstidFrist(tidsfrist)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER))
            .build();
        //Tidsfristen blir overstyrt
        revurderingsBehandling.setBehandlingstidFrist(tidsfrist);
        behandlingRepository.lagre(revurderingsBehandling, behandlingRepository.taSkriveLås(revurderingsBehandling));
        // Act
        List<Behandling> liste = behandlingKandidaterRepository.finnBehandlingerMedUtløptBehandlingsfrist();
        // Assert
        assertThat(liste).hasSize(1);
    }

    @Test
    public void skal_opprettholde_id_etter_endringer() {

        // Lagre Personopplysning
        AbstractTestScenario<?> scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medAntallBarn(1).medFødselsDato(LocalDate.now());
        scenario.lagre(repositoryProvider);
    }

    @Test
    public void skal_finne_årsaker_for_behandling() {

        // Arrange
        Behandling behandling = opprettBuilderForBehandling()
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
                .medManueltOpprettet(false))
            .build();
        lagreBehandling(behandling);

        // Act
        List<BehandlingÅrsak> liste = behandlingRepository.finnÅrsakerForBehandling(behandling);

        // Assert
        assertThat(liste).hasSize(1);
        assertThat(liste.get(0).getBehandlingÅrsakType()).isEqualTo(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
    }

    @Test
    public void skal_finne_årsakstyper_for_behandling() {

        // Arrange
        Behandling behandling = opprettBuilderForBehandling()
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ANNET)
                .medManueltOpprettet(false))
            .build();
        lagreBehandling(behandling);

        // Act
        List<BehandlingÅrsakType> liste = behandlingRepository.finnÅrsakTyperForBehandling(behandling);

        // Assert
        assertThat(liste).hasSize(1);
        assertThat(liste.get(0)).isEqualTo(BehandlingÅrsakType.RE_ANNET);
    }

    @Test
    public void skal_ikke_finne_noen_årsakstyper_hvis_ingen() {

        // Arrange
        Behandling behandling = opprettBuilderForBehandling()
            .build();
        lagreBehandling(behandling);

        // Act
        List<BehandlingÅrsakType> liste = behandlingRepository.finnÅrsakTyperForBehandling(behandling);

        // Assert
        assertThat(liste).isEmpty();
    }

    @Test
    public void skal_ikke_finne_noen_årsaker_hvis_ingen() {

        // Arrange
        Behandling behandling = opprettBuilderForBehandling()
            .build();
        lagreBehandling(behandling);

        // Act
        List<BehandlingÅrsak> liste = behandlingRepository.finnÅrsakerForBehandling(behandling);

        // Assert
        assertThat(liste).isEmpty();
    }

    @Test
    public void avsluttet_dato_skal_ha_dato_og_tid() {
        // Arrange
        LocalDateTime avsluttetDato = LocalDateTime.now();
        Behandling behandling = opprettBuilderForBehandling().medAvsluttetDato(avsluttetDato)
            .build();

        lagreBehandling(behandling);
        new Repository(getEntityManager()).flushAndClear();

        // Act
        Optional<Behandling> resultatBehandling = behandlingRepository.hentBehandlingHvisFinnes(behandling.getUuid());

        // Assert
        assertThat(resultatBehandling).isNotEmpty();
        LocalDateTime avsluttetDatoResultat = resultatBehandling.get().getAvsluttetDato();

        assertThat(avsluttetDatoResultat).isEqualTo(avsluttetDato.withNano(0)); // Oracle is not returning milliseconds.
        assertThat(avsluttetDatoResultat).isNotEqualTo(avsluttetDato);
    }

    private Behandling opprettBehandlingForAutomatiskGjenopptagelse() {

        LocalDate terminDato = LocalDate.now().plusDays(5);
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(terminDato)
                .medUtstedtDato(LocalDate.now())
                .medNavnPå("Lege Legesen"))
            .medAntallBarn(1);
        scenario.medBekreftetHendelse(scenario.medBekreftetHendelse()
            .medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                .medTermindato(terminDato)
                .medNavnPå("NAVNSENASDA ")
                .medUtstedtDato(terminDato.minusDays(40)))
            .medAntallBarn(1));

        Behandling behandling = scenario.lagre(repositoryProvider);
        return behandling;
    }

    private Aksjonspunkt opprettAksjonspunkt(Behandling behandling,
                                             AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                             LocalDateTime frist) {

        Aksjonspunkt aksjonspunkt = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon);
        AksjonspunktTestSupport.setFrist(aksjonspunkt, frist, Venteårsak.UDEFINERT);
        return aksjonspunkt;
    }

    private Fagsak byggFagsak(AktørId aktørId, RelasjonsRolleType rolle, NavBrukerKjønn kjønn) {
        NavBruker navBruker = new NavBrukerBuilder()
            .medAktørId(aktørId)
            .medKjønn(kjønn)
            .build();
        Fagsak fagsak = FagsakBuilder.nyEngangstønad(rolle)
            .medBruker(navBruker).build();
        fagsakRepository.opprettNy(fagsak);
        return fagsak;
    }

    private Behandling byggBehandlingForElektroniskSøknadOmFødsel(Fagsak fagsak, LocalDate fødselsdato, LocalDate mottattDato) {
        Behandling.Builder behandlingBuilder = Behandling.forFørstegangssøknad(fagsak);
        Behandling behandling = behandlingBuilder.build();
        behandling.setAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER);
        lagreBehandling(behandling);
        final FamilieHendelseBuilder søknadHendelse = repositoryProvider.getFamilieHendelseRepository().opprettBuilderFor(behandling)
            .medAntallBarn(1)
            .medFødselsDato(fødselsdato);
        repositoryProvider.getFamilieHendelseRepository().lagre(behandling, søknadHendelse);

        final SøknadEntitet søknad = new SøknadEntitet.Builder()
            .medSøknadsdato(LocalDate.now())
            .medMottattDato(mottattDato)
            .medElektroniskRegistrert(true)
            .build();
        repositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);

        return behandling;

    }

    private BehandlingVedtak.Builder opprettBuilderForVedtak() {
        behandling = opprettBehandlingMedTermindato();
        oppdaterMedBehandlingsresultatOgLagre(behandling, true, false);

        return BehandlingVedtak.builder().medVedtakstidspunkt(LocalDateTime.now())
            .medAnsvarligSaksbehandler("Janne Hansen")
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medIverksettingStatus(IverksettingStatus.IKKE_IVERKSATT)
            .medBehandlingsresultat(getBehandlingsresultat(behandling));
    }

    private Behandling opprettBehandlingMedTermindato() {

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medNavnPå("ASDASD ASD ASD")
                .medUtstedtDato(LocalDate.now())
                .medTermindato(LocalDate.now().plusDays(40)))
            .medAntallBarn(1);
        scenario.medBekreftetHendelse(scenario.medBekreftetHendelse()
            .medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                .medTermindato(LocalDate.now().plusDays(40))
                .medUtstedtDato(LocalDate.now().minusDays(7))
                .medNavnPå("NAVN"))
            .medAntallBarn(1));

        behandling = scenario.lagre(repositoryProvider);
        return behandling;
    }

    private Behandling opprettRevurderingsKandidat(int dagerTilbake) {

        LocalDate terminDato = LocalDate.now().minusDays(dagerTilbake);
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medNavnPå("ASDASD ASD ASD")
                .medUtstedtDato(LocalDate.now().minusDays(40))
                .medTermindato(terminDato))
            .medAntallBarn(1);
        scenario.medBekreftetHendelse(scenario.medBekreftetHendelse()
            .medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                .medTermindato(terminDato)
                .medNavnPå("LEGESEN")
                .medUtstedtDato(terminDato.minusDays(40)))
            .medAntallBarn(1));

        behandling = scenario.lagre(repositoryProvider);
        Behandlingsresultat behandlingsresultat = Behandlingsresultat.builder()
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET).buildFor(behandling);
        final BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder().medVedtakstidspunkt(LocalDateTime.now()).medBehandlingsresultat(behandlingsresultat)
            .medVedtakResultatType(VedtakResultatType.INNVILGET).medAnsvarligSaksbehandler("asdf").build();
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        behandlingVedtakRepository.lagre(behandlingVedtak, behandlingRepository.taSkriveLås(behandling));

        return behandling;
    }

    private Behandlingsresultat oppdaterMedBehandlingsresultatOgLagre(Behandling behandling, boolean innvilget, boolean henlegg) {

        Behandlingsresultat behandlingsresultat = getBehandlingsresultat(behandling);
        if (henlegg) {
            Behandlingsresultat.builderEndreEksisterende(behandlingsresultat).medBehandlingResultatType(BehandlingResultatType.HENLAGT_FEILOPPRETTET);
        }

        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        VilkårResultat.builder()
            .leggTilVilkårResultat(VilkårType.FØDSELSVILKÅRET_MOR, innvilget ? VilkårUtfallType.OPPFYLT : VilkårUtfallType.IKKE_OPPFYLT,
                null, new Properties(), null, false, false, null, null)
            .medVilkårResultatType(innvilget ? VilkårResultatType.INNVILGET : VilkårResultatType.AVSLÅTT)
            .buildFor(behandling);
        behandlingRepository.lagre(behandlingsresultat.getVilkårResultat(), lås);

        if (innvilget) {
            LegacyESBeregningsresultat.builder()
                .medBeregning(new LegacyESBeregning(48500L, 1L, 48500L, LocalDateTime.now()))
                .buildFor(behandling, behandlingsresultat);
            beregningRepository.lagre(behandlingsresultat.getBeregningResultat(), lås);
        }
        return behandlingsresultat;
    }

    private Behandling.Builder opprettBuilderForBehandling() {
        fagsakRepository.opprettNy(fagsak);
        return Behandling.forFørstegangssøknad(fagsak);

    }
}
