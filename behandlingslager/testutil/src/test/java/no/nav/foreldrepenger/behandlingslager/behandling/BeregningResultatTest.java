package no.nav.foreldrepenger.behandlingslager.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;

/* BeregningResultat regnes som et selvstendig aggregat, men har to overliggende nivåer for aggregat:
        Behandling -> Behandlingsresultat -> Beregningsresultat
  Denne testklassen fokuserer på at aggregatet (Beregningsresultat) bygges opp korrekt over suksessive transaksjoner
    som er forventet i use-caser.
 */
class BeregningResultatTest extends EntityManagerAwareTest {

    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakReposiory;
    private LegacyESBeregningRepository beregningRepository;


    private final long sats = 1L;
    private final long antallBarn = 1L;
    private final long tilkjentYtelse = 2L;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        behandlingRepository = new BehandlingRepository(entityManager);
        fagsakReposiory = new FagsakRepository(entityManager);
        beregningRepository = new LegacyESBeregningRepository(entityManager);
    }

    private Behandling opprettBehandling() {
        var fagsak = FagsakBuilder.nyEngangstønadForMor().build();
        fagsakReposiory.opprettNy(fagsak);
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, new BehandlingRepository(getEntityManager()).taSkriveLås(behandling.getId()));
        return behandling;
    }

    @Test
    void skal_opprette_nytt_beregningsresultat_uten_beregning_dersom_ikke_finnes_fra_før() {
        // Act
        // TX_1: Opprette nytt beregningsresultat
        var behandling1 = opprettBehandling();
        var beregningResultat = LegacyESBeregningsresultat.builder().buildFor(behandling1, null);
        lagreBeregningResultat(behandling1, beregningResultat);

        // Assert
        var hentetBehandling = behandlingRepository.hentBehandling(behandling1.getId());
        var hentetResultat = getBehandlingsresultat(hentetBehandling).getBeregningResultat();
        assertThat(hentetResultat).isNotNull();
    }

    @Test
    void skal_koble_beregning_til_beregningsresultat() {
        var behandling1 = opprettBehandling();
        var beregning = new LegacyESBeregning(behandling1.getId(), 1000L, antallBarn, antallBarn*1000, LocalDateTime.now());
        assertThat(beregning.getBeregningResultat()).isNull();
        var beregningResultat = LegacyESBeregningsresultat.builder().medBeregning(beregning)
            .buildFor(behandling1, null);
        assertThat(beregning.getBeregningResultat()).isNull();

        assertThat(beregningResultat.getBeregninger()).hasSize(1);
        assertThat(beregningResultat.getBeregninger().get(0).getBeregningResultat()).isNotNull();
    }

    private void lagreBeregningResultat(Behandling behandling, LegacyESBeregningsresultat beregningResultat) {
        var lås = behandlingRepository.taSkriveLås(behandling);
        beregningRepository.lagre(beregningResultat, lås);
    }

    @Test
    void skal_opprette_nytt_beregningsresultat_med_beregning_dersom_ikke_finnes_fra_før() {
        // Act
        // TX_1: Opprette nytt beregningsresultat med beregningsinfo
        var behandling1 = opprettBehandling();
        var beregning = new LegacyESBeregning(behandling1.getId(), sats, antallBarn, tilkjentYtelse, LocalDateTime.now());
        var beregningResultat = LegacyESBeregningsresultat.builder()
                .medBeregning(beregning)
                .buildFor(behandling1, null);
        lagreBeregningResultat(behandling1, beregningResultat);

        // Assert
        var hentetBehandling = behandlingRepository.hentBehandling(behandling1.getId());
        var hentetResultat = getBehandlingsresultat(hentetBehandling).getBeregningResultat();
        assertThat(hentetResultat.getBeregninger()).hasSize(1);
        assertThat(hentetResultat.getBeregninger().get(0)).isEqualTo(beregning);
    }

    @Test
    void skal_gjenbruke_beregningsresultat_fra_tidligere_behandling_ved_opprettelse_av_ny_behandling() {
        // Act
        // TX_1: Opprette nytt beregningsresultat
        var behandling1 = opprettBehandling();
        var beregning = new LegacyESBeregning(behandling1.getId(), sats, antallBarn, tilkjentYtelse, LocalDateTime.now());
        var beregningResultat = LegacyESBeregningsresultat.builder().medBeregning(beregning).buildFor(behandling1, null);
        lagreBeregningResultat(behandling1, beregningResultat);

        // TX_2: Opprette nyTerminbekreftelse behandling fra tidligere behandling
        behandling1 = behandlingRepository.hentBehandling(behandling1.getId());
        var behandling2 = Behandling.fraTidligereBehandling(behandling1, BehandlingType.REVURDERING)
            .medKopiAvForrigeBehandlingsresultat()
            .build();
        lagreBeregningResultat(behandling2, getBehandlingsresultat(behandling2).getBeregningResultat());
        lagreBehandling(behandling2);

        // Assert
        behandling2 = behandlingRepository.hentBehandling(behandling2.getId());
        assertThat(getBehandlingsresultat(behandling2)).isNotSameAs(getBehandlingsresultat(behandling1));
        assertThat(getBehandlingsresultat(behandling2).getBeregningResultat())
                .isSameAs(getBehandlingsresultat(behandling1).getBeregningResultat());
        assertThat(getBehandlingsresultat(behandling2).getBeregningResultat())
                .isEqualTo(getBehandlingsresultat(behandling1).getBeregningResultat());
    }

    private void lagreBehandling(Behandling behandling) {
        var lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
    }

    @Test
    void skal_opprette_nytt_beregningsresultat_dersom_gjenbrukt_resultat_fra_tidligere_behandling_oppdateres() {
        // Act
        // TX_1: Opprette nytt beregningsresultat
        var behandling1 = opprettBehandling();
        var beregning1 = new LegacyESBeregning(behandling1.getId(), sats, antallBarn, tilkjentYtelse, LocalDateTime.now());
        var beregningResultat = LegacyESBeregningsresultat.builder().medBeregning(beregning1)
            .buildFor(behandling1, null);
        lagreBeregningResultat(behandling1, beregningResultat);

        // TX_2: Opprette nyTerminbekreftelse behandling fra tidligere behandling
        behandling1 = behandlingRepository.hentBehandling(behandling1.getId());
        var behandling2 = Behandling.fraTidligereBehandling(behandling1, BehandlingType.REVURDERING)
            .medKopiAvForrigeBehandlingsresultat()
            .build();
        lagreBeregningResultat(behandling2, getBehandlingsresultat(behandling2).getBeregningResultat());
        lagreBehandling(behandling2);

        // TX_3: Oppdatere nyTerminbekreftelse behandling med beregning
        behandling2 = behandlingRepository.hentBehandling(behandling2.getId());
        var beregning2 = new LegacyESBeregning(behandling2.getId(), sats + 1, antallBarn, tilkjentYtelse, LocalDateTime.now());
        LegacyESBeregningsresultat.builder().medBeregning(beregning2).buildFor(behandling2, getBehandlingsresultat(behandling2));
        lagreBeregningResultat(behandling2, getBehandlingsresultat(behandling2).getBeregningResultat());

        // Assert
        behandling2 = behandlingRepository.hentBehandling(behandling2.getId());
        assertThat(getBehandlingsresultat(behandling2)).isNotSameAs(getBehandlingsresultat(behandling1));
        assertThat(getBehandlingsresultat(behandling2).getBeregningResultat())
                .isNotSameAs(getBehandlingsresultat(behandling1).getBeregningResultat());
        assertThat(getBehandlingsresultat(behandling2).getBeregningResultat())
                .isNotEqualTo(getBehandlingsresultat(behandling1).getBeregningResultat());
        assertThat(getBehandlingsresultat(behandling2).getBeregningResultat().getBeregninger().get(0))
                .isEqualTo(beregning2);
    }

    @Test
    void skal_ikke_opprette_nytt_beregningsresultat_dersom_resultat_fra_tidligere_behandling_allerede_er_oppdatert() {
        // Act
        // TX_1: Opprette nytt beregningsresultat
        var behandling1 = opprettBehandling();
        var beregning1 = new LegacyESBeregning(behandling1.getId(), sats, antallBarn, tilkjentYtelse, LocalDateTime.now());
        var beregningResultat = LegacyESBeregningsresultat.builder().medBeregning(beregning1).buildFor(behandling1, null);
        lagreBeregningResultat(behandling1, beregningResultat);

        // TX_2: Opprette nyTerminbekreftelse behandling fra tidligere behandling
        behandling1 = behandlingRepository.hentBehandling(behandling1.getId());
        var behandling2 = Behandling.fraTidligereBehandling(behandling1, BehandlingType.REVURDERING)
            .medKopiAvForrigeBehandlingsresultat()
            .build();
        lagreBeregningResultat(behandling2, getBehandlingsresultat(behandling2).getBeregningResultat());
        lagreBehandling(behandling2);

        // TX_3: Oppdatere nyTerminbekreftelse behandling med beregning
        behandling2 = behandlingRepository.hentBehandling(behandling2.getId());
        var beregning2 = new LegacyESBeregning(behandling2.getId(), sats + 1, antallBarn, tilkjentYtelse, LocalDateTime.now());
        LegacyESBeregningsresultat.builder().medBeregning(beregning2).buildFor(behandling2, getBehandlingsresultat(behandling2));
        lagreBeregningResultat(behandling2, getBehandlingsresultat(behandling2).getBeregningResultat());

        // TX_4: Oppdatere nyTerminbekreftelse behandling med beregning (samme som TX_3, men nyTerminbekreftelse beregning med nyTerminbekreftelse verdi)
        behandling2 = behandlingRepository.hentBehandling(behandling2.getId());
        var beregning3 = new LegacyESBeregning(behandling2.getId(), sats + 2, antallBarn, tilkjentYtelse, LocalDateTime.now());
        LegacyESBeregningsresultat.builder().medBeregning(beregning3).buildFor(behandling2, getBehandlingsresultat(behandling2));
        lagreBeregningResultat(behandling2, getBehandlingsresultat(behandling2).getBeregningResultat());

        // Assert
        behandling2 = behandlingRepository.hentBehandling(behandling2.getId());
        assertThat(getBehandlingsresultat(behandling2)).isNotSameAs(getBehandlingsresultat(behandling1));
        assertThat(getBehandlingsresultat(behandling2).getBeregningResultat())
                .isNotSameAs(getBehandlingsresultat(behandling1).getBeregningResultat());
        assertThat(getBehandlingsresultat(behandling2).getBeregningResultat())
                .isNotEqualTo(getBehandlingsresultat(behandling1).getBeregningResultat());
        assertThat(getBehandlingsresultat(behandling2).getBeregningResultat().getBeregninger().get(0))
                .isEqualTo(beregning3);
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandling.getBehandlingsresultat();
    }

    @Test
    void skal_bevare_vilkårresultat_ved_oppdatering_av_beregingsresultat() {
        // Act
        // TX_1: Opprette Behandlingsresultat med Beregningsresultat
        var behandling1 = opprettBehandling();
        var beregning1 = new LegacyESBeregning(behandling1.getId(), sats, antallBarn, tilkjentYtelse, LocalDateTime.now());
        var beregningResultat1 = LegacyESBeregningsresultat.builder().medBeregning(beregning1)
            .buildFor(behandling1, null);
        lagreBeregningResultat(behandling1, beregningResultat1);

        // TX_2: Oppdatere Behandlingsresultat med VilkårResultat
        behandling1 = behandlingRepository.hentBehandling(behandling1.getId());
        var vilkårResultat = VilkårResultat.builder()
                .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
                .buildFor(behandling1);

        var lås = behandlingRepository.taSkriveLås(behandling1);
        behandlingRepository.lagre(vilkårResultat, lås);

        // TX_3: Oppdatere Behandlingsresultat med BeregningResultat
        behandling1 = behandlingRepository.hentBehandling(behandling1.getId());
        var oppdatertBeregning = new LegacyESBeregning(behandling1.getId(), sats + 1, antallBarn, tilkjentYtelse, LocalDateTime.now());
        var beregningResultat2 = LegacyESBeregningsresultat.builder().medBeregning(oppdatertBeregning).buildFor(behandling1, getBehandlingsresultat(behandling1));
        lagreBeregningResultat(behandling1, beregningResultat2);

        // Assert
        var hentetBehandling = behandlingRepository.hentBehandling(behandling1.getId());
        assertThat(getBehandlingsresultat(hentetBehandling).getVilkårResultat())
                .isEqualTo(vilkårResultat);
    }
}
