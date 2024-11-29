package no.nav.foreldrepenger.behandlingslager.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingKandidaterRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
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

class BehandlingRepositoryTest extends EntityManagerAwareTest {

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

    private final Saksnummer saksnummer = new Saksnummer("29999");
    private final Fagsak fagsak = FagsakBuilder.nyEngangstønadForMor().medSaksnummer(saksnummer).build();
    private Behandling behandling;

    private final LocalDateTime imorgen = LocalDateTime.now().plusDays(1);
    private final LocalDateTime igår = LocalDateTime.now().minusDays(1);

    @BeforeEach
    void setUp() {
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
    void skal_finne_behandling_gitt_id() {

        // Arrange
        var behandling = opprettBuilderForBehandling().build();
        lagreBehandling(behandling);

        // Act
        var resultat = behandlingRepository.hentBehandling(behandling.getId());

        // Assert
        assertThat(resultat).isNotNull();
    }

    @Test
    void skal_finne_behandling_gitt_uuid() {

        // Arrange
        var behandling = opprettBuilderForBehandling().build();
        lagreBehandling(behandling);

        // Act
        var resultat = behandlingRepository.hentBehandling(behandling.getUuid());

        // Assert
        assertThat(resultat).isNotNull();
    }

    private void lagreBehandling(Behandling... behandlinger) {
        for (var behandling : behandlinger) {
            var lås = behandlingRepository.taSkriveLås(behandling);
            behandlingRepository.lagre(behandling, lås);
        }
    }

    @Test
    void skal_hente_alle_behandlinger_fra_fagsak() {

        var builder = opprettBuilderForBehandling();
        lagreBehandling(builder);

        var behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(saksnummer);

        assertThat(behandlinger).hasSize(1);

    }

    private void lagreBehandling(Behandling.Builder builder) {
        var behandling = builder.build();
        var lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
    }

    @Test
    void skal_finne_behandling_med_årsak() {
        var behandling = opprettRevurderingsKandidat(REVURDERING_DAGER_TILBAKE + 2);

        var revurderingsBehandling = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN)).build();

        behandlingRepository.lagre(revurderingsBehandling, behandlingRepository.taSkriveLås(revurderingsBehandling));

        var result = behandlingRepository.hentBehandlingerMedÅrsakerForFagsakId(behandling.getFagsakId(),
            BehandlingÅrsakType.årsakerForEtterkontroll());
        assertThat(result).isNotEmpty();
    }

    @Test
    void skal_hente_siste_behandling_basert_på_fagsakId() {

        var builder = opprettBuilderForBehandling();

        lagreBehandling(builder);

        var sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());

        assertThat(sisteBehandling).isPresent();
        assertThat(sisteBehandling.get().getFagsakId()).isEqualTo(fagsak.getId());
    }

    @Test
    void skal_hente_siste_klage_basert_på_fagsakId() {

        var builder = opprettBuilderForBehandling();

        lagreBehandling(builder);

        var sisteBehandling = behandlingRepository.finnSisteIkkeHenlagteBehandlingavAvBehandlingTypeFor(fagsak.getId(), BehandlingType.KLAGE);

        assertThat(sisteBehandling).isEmpty();

        // Part 2
        var klage = Behandling.forKlage(fagsak).build();

        var lås = behandlingRepository.taSkriveLås(klage);
        behandlingRepository.lagre(klage, lås);

        var sisteKlage = behandlingRepository.finnSisteIkkeHenlagteBehandlingavAvBehandlingTypeFor(fagsak.getId(), BehandlingType.KLAGE);

        assertThat(sisteKlage).isPresent();
        assertThat(sisteKlage.get().getFagsakId()).isEqualTo(fagsak.getId());
    }

    @Test
    void skal_hente_siste_innvilget_eller_endret_på_fagsakId() {
        var forVedtak = opprettBuilderForVedtak();
        var behandlingsresultat = getBehandlingsresultat(behandling);
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultat).medBehandlingResultatType(BehandlingResultatType.INNVILGET);

        var lås = behandlingRepository.taSkriveLås(behandling);
        behandlingsresultatRepository.lagre(behandling.getId(), behandlingsresultat);
        behandlingVedtakRepository.lagre(forVedtak.medBehandlingsresultat(getBehandlingsresultat(behandling)).medIverksettingStatus(IverksettingStatus.IVERKSATT).build(), lås);
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, lås);

        var innvilgetBehandling = behandlingRepository.finnSisteInnvilgetBehandling(behandling.getFagsakId());

        assertThat(innvilgetBehandling).isPresent();
        assertThat(innvilgetBehandling.get().getFagsakId()).isEqualTo(behandling.getFagsak().getId());
    }

    @Test
    void skal_hente_siste_behandling_ekskluder_basert_på_fagsakId() {
        var scenario = ScenarioKlageEngangsstønad.forUtenVurderingResultat(ScenarioMorSøkerEngangsstønad.forAdopsjon());
        var klageBehandling = scenario.lagre(repositoryProvider, klageRepository);

        var alleBehandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(klageBehandling.getSaksnummer());
        assertThat(alleBehandlinger).as("Forventer at alle behandlinger opprettet skal eksistere").hasSize(2);

        var sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(klageBehandling.getFagsak().getId());

        assertThat(sisteBehandling).isPresent();
        assertThat(sisteBehandling.get().getFagsakId()).isEqualTo(klageBehandling.getFagsak().getId());
        assertThat(sisteBehandling.get().getType()).isNotEqualTo(BehandlingType.KLAGE);
    }

    @Test
    void skal_kunne_lagre_vedtak() {
        var vedtak = opprettBuilderForVedtak().build();

        var lås = behandlingRepository.taSkriveLås(behandling);

        behandlingRepository.lagre(behandling, lås);
        behandlingVedtakRepository.lagre(vedtak, lås);

        var id = vedtak.getId();
        assertThat(id).isNotNull();

        var vedtakLest = behandlingVedtakRepository.hentForBehandling(vedtak.getBehandlingsresultat().getBehandlingId());
        assertThat(vedtakLest).isNotNull();

    }

    @Test
    void skal_finne_behandling_gitt_korrekt_uuid() {
        // Arrange
        var behandling = opprettBuilderForBehandling().build();
        lagreBehandling(behandling);

        // Act
        var resultat = behandlingRepository.hentBehandlingHvisFinnes(behandling.getUuid());

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat).contains(behandling);
    }

    @Test
    void skal_ikke_finne_behandling_gitt_feil_uuid() {
        // Arrange
        var behandling = opprettBuilderForBehandling().build();
        lagreBehandling(behandling);

        // Act
        var resultat = behandlingRepository.hentBehandlingHvisFinnes(UUID.randomUUID());

        // Assert
        assertThat(resultat).isNotPresent();
    }


    @Test
    void skal_kunne_lagre_konsekvens_for_ytelsen() {
        behandling = opprettBehandlingMedTermindato();
        var behandlingsresultat = oppdaterMedBehandlingsresultatOgLagre(behandling, true, false);

        setKonsekvensForYtelsen(behandlingsresultat, List.of(KonsekvensForYtelsen.ENDRING_I_BEREGNING, KonsekvensForYtelsen.ENDRING_I_UTTAK));
        var brKonsekvenser = behandlingsresultatRepository.hent(behandling.getId()).getKonsekvenserForYtelsen();
        assertThat(brKonsekvenser).containsExactlyInAnyOrder(KonsekvensForYtelsen.ENDRING_I_BEREGNING, KonsekvensForYtelsen.ENDRING_I_UTTAK);
    }

    @Test
    void dersom_man_lagrer_konsekvens_for_ytelsen_flere_ganger_skal_kun_den_siste_lagringen_gjelde() {
        behandling = opprettBehandlingMedTermindato();
        var behandlingsresultat = oppdaterMedBehandlingsresultatOgLagre(behandling, true, false);

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
        var builder = Behandlingsresultat.builderEndreEksisterende(behandlingsresultat);
        konsekvenserForYtelsen.forEach(builder::leggTilKonsekvensForYtelsen);
        builder.buildFor(behandling);

        var lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
    }

    @Test
    void skal_slette_vilkår_som_blir_fjernet_til_tross_for_at_Hibernate_har_problemer_med_orphan_removal() {
        // Arrange
        var fagsak = byggFagsak(AktørId.dummy(), RelasjonsRolleType.MORA, NavBrukerKjønn.KVINNE);
        behandling = byggBehandlingForElektroniskSøknadOmFødsel(fagsak, LocalDate.now(), LocalDate.now());

        var lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        var vilkårResultat = VilkårResultat.builder()
            .leggTilVilkårIkkeVurdert(VilkårType.OMSORGSVILKÅRET)
            .buildFor(behandling);

        // Act
        behandlingRepository.lagre(vilkårResultat, lås);

        behandlingRepository.lagre(behandling, lås);

        // Assert
        assertThat(vilkårResultat.getVilkårene()).hasSize(1);
        assertThat(vilkårResultat.getVilkårene().iterator().next().getVilkårType()).isEqualTo(VilkårType.OMSORGSVILKÅRET);

        // Arrange
        VilkårResultat.builderFraEksisterende(vilkårResultat)
            .leggTilVilkårIkkeVurdert(VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD)
            .fjernVilkår(VilkårType.OMSORGSVILKÅRET)
            .buildFor(behandling);

        // Act
        behandlingRepository.lagre(behandling, lås);
        var vilkårId = behandlingRepository.lagre(vilkårResultat, lås);

        // Assert
        var opphentetBehandling = behandlingRepository.hentBehandling(behandling.getId());
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
    void skal_finne_for_automatisk_gjenopptagelse_naar_alle_kriterier_oppfylt() {

        // Arrange
        var behandling1 = opprettBehandlingForAutomatiskGjenopptagelse();
        opprettAksjonspunkt(behandling1, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, igår);
        opprettAksjonspunkt(behandling1, AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD, igår);

        var behandling2 = opprettBehandlingForAutomatiskGjenopptagelse();
        opprettAksjonspunkt(behandling2, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, igår);

        var behandling3 = opprettBehandlingForAutomatiskGjenopptagelse();
        opprettAksjonspunkt(behandling3, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, igår);
        lagreBehandling(behandling1, behandling2, behandling3);

        // Act
        var liste = behandlingKandidaterRepository.finnBehandlingerForAutomatiskGjenopptagelse();

        // Assert
        assertThat(liste).hasSize(3);
        assertThat(liste).contains(behandling1);
        assertThat(liste).contains(behandling2);
        assertThat(liste).contains(behandling3);
    }

    @Test
    void skal_ikke_finne_for_automatisk_gjenopptagelse_naar_naar_lukket_aksjonspunkt() {
        var behandling1 = opprettBehandlingForAutomatiskGjenopptagelse();
        var aksjonspunkt = opprettAksjonspunkt(behandling1, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, igår);
        AksjonspunktTestSupport.setTilUtført(aksjonspunkt, "ferdig");
        lagreBehandling(behandling1);

        // Act
        var liste = behandlingKandidaterRepository.finnBehandlingerForAutomatiskGjenopptagelse();

        // Assert
        assertThat(liste).isEmpty();
    }

    @Test
    void skal_ikke_finne_for_automatisk_gjenopptagelse_naar_aksjonspunkt_frist_ikke_utgaatt() {

        // Arrange
        var behandling1 = opprettBehandlingForAutomatiskGjenopptagelse();
        opprettAksjonspunkt(behandling1, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, imorgen);

        // Act
        var liste = behandlingKandidaterRepository.finnBehandlingerForAutomatiskGjenopptagelse();

        // Assert
        assertThat(liste).isEmpty();
    }

    @Test
    void skal_ikke_finne_for_automatisk_gjenopptagelse_naar_aksjonspunkt_er_køet() {

        // Arrange
        var behandling1 = opprettBehandlingForAutomatiskGjenopptagelse();
        opprettAksjonspunkt(behandling1, AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING, imorgen);

        // Act
        var liste = behandlingKandidaterRepository.finnBehandlingerForAutomatiskGjenopptagelse();

        // Assert
        assertThat(liste).isEmpty();
    }

    @Test
    void skal_ikke_finne_for_automatisk_gjenopptagelse_når_aksjonspunt_er_avbrutt() {
        // Arrange
        var behandling = opprettBehandlingForAutomatiskGjenopptagelse();
        var aksjonspunkt = opprettAksjonspunkt(behandling, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, igår);
        AksjonspunktTestSupport.setTilAvbrutt(aksjonspunkt);
        lagreBehandling(behandling);

        // Act
        var liste = behandlingKandidaterRepository.finnBehandlingerForAutomatiskGjenopptagelse();

        // Assert
        assertThat(liste).isEmpty();
    }

    @Test
    void skal_finne_for_gjenopplivelse_naar_alle_kriterier_oppfylt() {

        // Arrange
        var behandling1 = opprettBehandlingForAutomatiskGjenopptagelse();
        opprettAksjonspunkt(behandling1, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, igår);
        opprettAksjonspunkt(behandling1, AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD, igår);

        var behandling2 = opprettBehandlingForAutomatiskGjenopptagelse();
        var ap2 = opprettAksjonspunkt(behandling2, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, igår);

        var behandling3 = opprettBehandlingForAutomatiskGjenopptagelse();
        var ap3 = opprettAksjonspunkt(behandling3, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, igår);
        lagreBehandling(behandling1, behandling2, behandling3);

        // Act
        var liste = behandlingKandidaterRepository.finnÅpneBehandlingerUtenÅpneAksjonspunktEllerAutopunkt();

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
    void skal_opprettholde_id_etter_endringer() {

        // Lagre Personopplysning
        AbstractTestScenario<?> scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medAntallBarn(1).medFødselsDato(LocalDate.now());
        scenario.lagre(repositoryProvider);

        var behandlingId = scenario.getBehandling().getId();
        var beh = behandlingRepository.hentBehandling(behandlingId);
        beh.setAnsvarligBeslutter("Test");
        var lås = behandlingRepository.taSkriveLås(beh.getId());
        behandlingRepository.lagre(beh, lås);

        assertThat(beh.getId()).isEqualTo(behandlingId);
        assertThat(beh.getAnsvarligBeslutter()).isEqualTo("Test");
    }

    @Test
    void skal_finne_årsaker_for_behandling() {

        // Arrange
        var behandling = opprettBuilderForBehandling()
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
                .medManueltOpprettet(false))
            .build();
        lagreBehandling(behandling);

        // Act
        var liste = behandlingRepository.finnÅrsakerForBehandling(behandling);

        // Assert
        assertThat(liste).hasSize(1);
        assertThat(liste.get(0).getBehandlingÅrsakType()).isEqualTo(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
    }

    @Test
    void skal_finne_årsakstyper_for_behandling() {

        // Arrange
        var behandling = opprettBuilderForBehandling()
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ANNET)
                .medManueltOpprettet(false))
            .build();
        lagreBehandling(behandling);

        // Act
        var liste = behandlingRepository.finnÅrsakTyperForBehandling(behandling);

        // Assert
        assertThat(liste).hasSize(1);
        assertThat(liste.get(0)).isEqualTo(BehandlingÅrsakType.RE_ANNET);
    }

    @Test
    void skal_ikke_finne_noen_årsakstyper_hvis_ingen() {

        // Arrange
        var behandling = opprettBuilderForBehandling()
            .build();
        lagreBehandling(behandling);

        // Act
        var liste = behandlingRepository.finnÅrsakTyperForBehandling(behandling);

        // Assert
        assertThat(liste).isEmpty();
    }

    @Test
    void skal_ikke_finne_noen_årsaker_hvis_ingen() {

        // Arrange
        var behandling = opprettBuilderForBehandling()
            .build();
        lagreBehandling(behandling);

        // Act
        var liste = behandlingRepository.finnÅrsakerForBehandling(behandling);

        // Assert
        assertThat(liste).isEmpty();
    }

    @Test
    void avsluttet_dato_skal_ha_dato_og_tid() {
        // Arrange
        var avsluttetDato = LocalDateTime.now();
        var behandling = opprettBuilderForBehandling().medAvsluttetDato(avsluttetDato)
            .build();

        lagreBehandling(behandling);
        var entityManager = getEntityManager();
        entityManager.flush();
        entityManager.clear();

        // Act
        var resultatBehandling = behandlingRepository.hentBehandlingHvisFinnes(behandling.getUuid());

        // Assert
        assertThat(resultatBehandling).isNotEmpty();
        var avsluttetDatoResultat = resultatBehandling.get().getAvsluttetDato();

        assertThat(avsluttetDatoResultat).isEqualTo(avsluttetDato.withNano(0)); // Oracle is not returning milliseconds.
        assertThat(avsluttetDatoResultat).isNotEqualTo(avsluttetDato);
    }

    @Test
    void test_hentAbsoluttAlleBehandlingerForFagsak() throws Exception {
        // Arrange
        var behandling = opprettBuilderForBehandling().build();
        lagreBehandling(behandling);

        var fagsak = behandling.getFagsak();
        var saksnummer = fagsak.getSaksnummer();
        assertThat(fagsak.getId()).isNotNull();
        assertThat(saksnummer).isNotNull();

        // Act
        var behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsak.getId());

        // Assert
        assertThat(behandlinger).hasSize(1).map(Behandling::getId).containsOnly(behandling.getId());
        var beh1 = behandlinger.get(0);
        assertThat(beh1.getFagsak()).isNotNull();
        assertThat(beh1.getSaksnummer()).isEqualTo(saksnummer);

    }

    private Behandling opprettBehandlingForAutomatiskGjenopptagelse() {

        var terminDato = LocalDate.now().plusDays(5);
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
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

        return scenario.lagre(repositoryProvider);
    }

    private Aksjonspunkt opprettAksjonspunkt(Behandling behandling,
                                             AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                             LocalDateTime frist) {

        var aksjonspunkt = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon);
        AksjonspunktTestSupport.setFrist(aksjonspunkt, frist, Venteårsak.UDEFINERT);
        return aksjonspunkt;
    }

    private Fagsak byggFagsak(AktørId aktørId, RelasjonsRolleType rolle, NavBrukerKjønn kjønn) {
        var navBruker = new NavBrukerBuilder()
            .medAktørId(aktørId)
            .medKjønn(kjønn)
            .build();
        var fagsak = FagsakBuilder.nyEngangstønad(rolle)
            .medBruker(navBruker).build();
        fagsakRepository.opprettNy(fagsak);
        return fagsak;
    }

    private Behandling byggBehandlingForElektroniskSøknadOmFødsel(Fagsak fagsak, LocalDate fødselsdato, LocalDate mottattDato) {
        var behandlingBuilder = Behandling.forFørstegangssøknad(fagsak);
        var behandling = behandlingBuilder.build();
        behandling.setAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER);
        lagreBehandling(behandling);
        var søknadHendelse = repositoryProvider.getFamilieHendelseRepository()
            .opprettBuilderFor(behandling.getId())
            .medAntallBarn(1)
            .medFødselsDato(fødselsdato);
        repositoryProvider.getFamilieHendelseRepository().lagre(behandling.getId(), søknadHendelse);

        var søknad = new SøknadEntitet.Builder().medSøknadsdato(LocalDate.now()).medMottattDato(mottattDato).medElektroniskRegistrert(true).build();
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

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
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

        var terminDato = LocalDate.now().minusDays(dagerTilbake);
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
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
        var behandlingsresultat = Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET).buildFor(behandling);
        var behandlingVedtak = BehandlingVedtak.builder()
            .medVedtakstidspunkt(LocalDateTime.now())
            .medBehandlingsresultat(behandlingsresultat)
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medAnsvarligSaksbehandler("asdf")
            .build();
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        behandlingVedtakRepository.lagre(behandlingVedtak, behandlingRepository.taSkriveLås(behandling));

        return behandling;
    }

    private Behandlingsresultat oppdaterMedBehandlingsresultatOgLagre(Behandling behandling, boolean innvilget, boolean henlegg) {

        var behandlingsresultat = getBehandlingsresultat(behandling);
        if (henlegg) {
            Behandlingsresultat.builderEndreEksisterende(behandlingsresultat).medBehandlingResultatType(BehandlingResultatType.HENLAGT_FEILOPPRETTET);
        }

        var lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        var builder = VilkårResultat.builder();
        if (innvilget) {
            builder.leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR);
        } else {
            builder.leggTilVilkårAvslått(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallMerknad.VM_1026);
        }
        builder.buildFor(behandling);
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
