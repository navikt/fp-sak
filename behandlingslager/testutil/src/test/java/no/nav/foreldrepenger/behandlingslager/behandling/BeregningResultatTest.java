package no.nav.foreldrepenger.behandlingslager.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.EngangsstønadBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.EngangsstønadBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
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
    private EngangsstønadBeregningRepository beregningRepository;


    private final long sats = 1L;
    private final long antallBarn = 1L;
    private final long tilkjentYtelse = 2L;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        behandlingRepository = new BehandlingRepository(entityManager);
        fagsakReposiory = new FagsakRepository(entityManager);
        beregningRepository = new EngangsstønadBeregningRepository(entityManager);
    }

    private Behandling opprettBehandling() {
        var fagsak = FagsakBuilder.nyEngangstønadForMor().build();
        fagsakReposiory.opprettNy(fagsak);
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, new BehandlingRepository(getEntityManager()).taSkriveLås(behandling.getId()));
        return behandling;
    }

    private void lagreBeregningResultat(Behandling behandling, EngangsstønadBeregning beregningResultat) {
        beregningRepository.lagre(behandling.getId(), beregningResultat);
    }

    @Test
    void skal_opprette_nytt_beregningsresultat_med_beregning_dersom_ikke_finnes_fra_før() {
        // Act
        // TX_1: Opprette nytt beregningsresultat med beregningsinfo
        var behandling1 = opprettBehandling();
        var beregning = new EngangsstønadBeregning(behandling1.getId(), sats, antallBarn, tilkjentYtelse, LocalDateTime.now());
        lagreBeregningResultat(behandling1, beregning);

        // Assert
        var hentetResultat = beregningRepository.hentEngangsstønadBeregning(behandling1.getId());
        assertThat(hentetResultat).isPresent().contains(beregning);
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
        var beregning1 = new EngangsstønadBeregning(behandling1.getId(), sats, antallBarn, tilkjentYtelse, LocalDateTime.now());
        lagreBeregningResultat(behandling1, beregning1);

        // TX_2: Opprette nyTerminbekreftelse behandling fra tidligere behandling
        behandling1 = behandlingRepository.hentBehandling(behandling1.getId());
        var behandling2 = Behandling.fraTidligereBehandling(behandling1, BehandlingType.REVURDERING)
            .medKopiAvForrigeBehandlingsresultat()
            .build();
        lagreBehandling(behandling2);

        // TX_3: Oppdatere nyTerminbekreftelse behandling med beregning
        behandling2 = behandlingRepository.hentBehandling(behandling2.getId());
        var beregning2 = new EngangsstønadBeregning(behandling2.getId(), sats + 1, antallBarn, tilkjentYtelse, LocalDateTime.now());
        lagreBeregningResultat(behandling2, beregning2);

        // Assert
        var hentetResultat1 = beregningRepository.hentEngangsstønadBeregning(behandling1.getId());
        var hentetResultat2 = beregningRepository.hentEngangsstønadBeregning(behandling2.getId());
        assertThat(hentetResultat1).isPresent().contains(beregning1);
        assertThat(hentetResultat2).isPresent().contains(beregning2);
        assertThat(hentetResultat2.get()).isNotSameAs(hentetResultat1.get());
    }

    @Test
    void skal_ikke_opprette_nytt_beregningsresultat_dersom_resultat_fra_tidligere_behandling_allerede_er_oppdatert() {
        // Act
        // TX_1: Opprette nytt beregningsresultat
        var behandling1 = opprettBehandling();
        var beregning1 = new EngangsstønadBeregning(behandling1.getId(), sats, antallBarn, tilkjentYtelse, LocalDateTime.now());
        lagreBeregningResultat(behandling1, beregning1);

        // TX_2: Opprette nyTerminbekreftelse behandling fra tidligere behandling
        behandling1 = behandlingRepository.hentBehandling(behandling1.getId());
        var behandling2 = Behandling.fraTidligereBehandling(behandling1, BehandlingType.REVURDERING)
            .medKopiAvForrigeBehandlingsresultat()
            .build();
        lagreBehandling(behandling2);

        // TX_3: Oppdatere nyTerminbekreftelse behandling med beregning
        behandling2 = behandlingRepository.hentBehandling(behandling2.getId());
        var beregning2 = new EngangsstønadBeregning(behandling2.getId(), sats + 1, antallBarn, tilkjentYtelse, LocalDateTime.now());
        lagreBeregningResultat(behandling2, beregning2);

        // TX_4: Oppdatere nyTerminbekreftelse behandling med beregning (samme som TX_3, men nyTerminbekreftelse beregning med nyTerminbekreftelse verdi)
        behandling2 = behandlingRepository.hentBehandling(behandling2.getId());
        var beregning3 = new EngangsstønadBeregning(behandling2.getId(), sats + 2, antallBarn, tilkjentYtelse, LocalDateTime.now());
        lagreBeregningResultat(behandling2, beregning3);

        // Assert
        var hentetResultat1 = beregningRepository.hentEngangsstønadBeregning(behandling1.getId());
        var hentetResultat2 = beregningRepository.hentEngangsstønadBeregning(behandling2.getId());
        assertThat(hentetResultat1).isPresent().contains(beregning1);
        assertThat(hentetResultat2).isPresent().contains(beregning3);
        assertThat(hentetResultat2.get()).isNotSameAs(hentetResultat1.get());
    }

}
