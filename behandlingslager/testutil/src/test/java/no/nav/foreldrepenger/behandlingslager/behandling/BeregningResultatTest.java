package no.nav.foreldrepenger.behandlingslager.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Properties;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.vedtak.felles.testutilities.db.Repository;

/* BeregningResultat regnes som et selvstendig aggregat, men har to overliggende nivåer for aggregat:
        Behandling -> Behandlingsresultat -> Beregningsresultat
  Denne testklassen fokuserer på at aggregatet (Beregningsresultat) bygges opp korrekt over suksessive transaksjoner
    som er forventet i use-caser.
 */
public class BeregningResultatTest {

    private final LocalDateTime nå = LocalDateTime.now();
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private Repository repository = repoRule.getRepository();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private final FagsakRepository fagsakReposiory = new FagsakRepository(repoRule.getEntityManager());
    private LegacyESBeregningRepository beregningRepository = new LegacyESBeregningRepository(repoRule.getEntityManager());

    private Fagsak fagsak = FagsakBuilder.nyEngangstønadForMor().build();
    private Behandling.Builder behandlingBuilder = Behandling.forFørstegangssøknad(fagsak);
    private Behandling behandling1;
    private final long sats = 1L;
    private final long antallBarn = 1L;
    private final long tilkjentYtelse = 2L;

    @Before
    public void setup() {
        fagsakReposiory.opprettNy(fagsak);
        behandling1 = behandlingBuilder.build();
        lagreBehandling(behandling1);
    }

    @Test
    public void skal_opprette_nytt_beregningsresultat_uten_beregning_dersom_ikke_finnes_fra_før() {
        // Act
        // TX_1: Opprette nytt beregningsresultat
        LegacyESBeregningsresultat beregningResultat = LegacyESBeregningsresultat.builder().buildFor(behandling1);
        lagreBeregningResultat(behandling1, beregningResultat);

        // Assert
        Behandling hentetBehandling = repository.hent(Behandling.class, behandling1.getId());
        LegacyESBeregningsresultat hentetResultat = getBehandlingsresultat(hentetBehandling).getBeregningResultat();
        assertThat(hentetResultat).isNotNull();
    }

    @Test
    public void skal_koble_beregning_til_beregningsresultat() {
        LegacyESBeregning beregning = new LegacyESBeregning(1000L, antallBarn, antallBarn*1000, nå);
        assertThat(beregning.getBeregningResultat()).isNull();
        LegacyESBeregningsresultat beregningResultat = LegacyESBeregningsresultat.builder().medBeregning(beregning).buildFor(behandling1);
        assertThat(beregning.getBeregningResultat()).isNull();

        assertThat(beregningResultat.getBeregninger()).hasSize(1);
        assertThat(beregningResultat.getBeregninger().get(0).getBeregningResultat()).isNotNull();
    }

    private void lagreBeregningResultat(Behandling behandling, LegacyESBeregningsresultat beregningResultat) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        beregningRepository.lagre(beregningResultat, lås);
    }

    @Test
    public void skal_opprette_nytt_beregningsresultat_med_beregning_dersom_ikke_finnes_fra_før() {
        // Act
        // TX_1: Opprette nytt beregningsresultat med beregningsinfo
        LegacyESBeregning beregning = new LegacyESBeregning(sats, antallBarn, tilkjentYtelse, nå);
        LegacyESBeregningsresultat beregningResultat = LegacyESBeregningsresultat.builder()
                .medBeregning(beregning)
                .buildFor(behandling1);
        lagreBeregningResultat(behandling1, beregningResultat);

        // Assert
        Behandling hentetBehandling = repository.hent(Behandling.class, behandling1.getId());
        LegacyESBeregningsresultat hentetResultat = getBehandlingsresultat(hentetBehandling).getBeregningResultat();
        assertThat(hentetResultat.getBeregninger()).hasSize(1);
        assertThat(hentetResultat.getBeregninger().get(0)).isEqualTo(beregning);
    }

    @Test
    public void skal_gjenbruke_beregningsresultat_fra_tidligere_behandling_ved_opprettelse_av_ny_behandling() {
        // Act
        // TX_1: Opprette nytt beregningsresultat
        LegacyESBeregning beregning = new LegacyESBeregning(sats, antallBarn, tilkjentYtelse, nå);
        LegacyESBeregningsresultat beregningResultat = LegacyESBeregningsresultat.builder().medBeregning(beregning).buildFor(behandling1);
        lagreBeregningResultat(behandling1, beregningResultat);

        // TX_2: Opprette nyTerminbekreftelse behandling fra tidligere behandling
        behandling1 = repository.hent(Behandling.class, behandling1.getId());
        Behandling behandling2 = Behandling.fraTidligereBehandling(behandling1, BehandlingType.REVURDERING)
            .medKopiAvForrigeBehandlingsresultat()
            .build();
        lagreBeregningResultat(behandling2, getBehandlingsresultat(behandling2).getBeregningResultat());
        lagreBehandling(behandling2);

        // Assert
        behandling2 = repository.hent(Behandling.class, behandling2.getId());
        assertThat(getBehandlingsresultat(behandling2)).isNotSameAs(getBehandlingsresultat(behandling1));
        assertThat(getBehandlingsresultat(behandling2).getBeregningResultat())
                .isSameAs(getBehandlingsresultat(behandling1).getBeregningResultat());
        assertThat(getBehandlingsresultat(behandling2).getBeregningResultat())
                .isEqualTo(getBehandlingsresultat(behandling1).getBeregningResultat());
    }

    private void lagreBehandling(Behandling behandling) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
    }

    @Test
    public void skal_opprette_nytt_beregningsresultat_dersom_gjenbrukt_resultat_fra_tidligere_behandling_oppdateres() {
        // Act
        // TX_1: Opprette nytt beregningsresultat
        LegacyESBeregning beregning1 = new LegacyESBeregning(sats, antallBarn, tilkjentYtelse, nå);
        LegacyESBeregningsresultat beregningResultat = LegacyESBeregningsresultat.builder().medBeregning(beregning1).buildFor(behandling1);
        lagreBeregningResultat(behandling1, beregningResultat);

        // TX_2: Opprette nyTerminbekreftelse behandling fra tidligere behandling
        behandling1 = repository.hent(Behandling.class, behandling1.getId());
        Behandling behandling2 = Behandling.fraTidligereBehandling(behandling1, BehandlingType.REVURDERING)
            .medKopiAvForrigeBehandlingsresultat()
            .build();
        lagreBeregningResultat(behandling2, getBehandlingsresultat(behandling2).getBeregningResultat());
        lagreBehandling(behandling2);

        // TX_3: Oppdatere nyTerminbekreftelse behandling med beregning
        behandling2 = repository.hent(Behandling.class, behandling2.getId());
        LegacyESBeregning beregning2 = new LegacyESBeregning(sats + 1, antallBarn, tilkjentYtelse, nå);
        LegacyESBeregningsresultat.builder().medBeregning(beregning2).buildFor(behandling2);
        lagreBeregningResultat(behandling2, getBehandlingsresultat(behandling2).getBeregningResultat());

        // Assert
        behandling2 = repository.hent(Behandling.class, behandling2.getId());
        assertThat(getBehandlingsresultat(behandling2)).isNotSameAs(getBehandlingsresultat(behandling1));
        assertThat(getBehandlingsresultat(behandling2).getBeregningResultat())
                .isNotSameAs(getBehandlingsresultat(behandling1).getBeregningResultat());
        assertThat(getBehandlingsresultat(behandling2).getBeregningResultat())
                .isNotEqualTo(getBehandlingsresultat(behandling1).getBeregningResultat());
        assertThat(getBehandlingsresultat(behandling2).getBeregningResultat().getBeregninger().get(0))
                .isEqualTo(beregning2);
    }

    @Test
    public void skal_ikke_opprette_nytt_beregningsresultat_dersom_resultat_fra_tidligere_behandling_allerede_er_oppdatert() {
        // Act
        // TX_1: Opprette nytt beregningsresultat
        LegacyESBeregning beregning1 = new LegacyESBeregning(sats, antallBarn, tilkjentYtelse, nå);
        LegacyESBeregningsresultat beregningResultat = LegacyESBeregningsresultat.builder().medBeregning(beregning1).buildFor(behandling1);
        lagreBeregningResultat(behandling1, beregningResultat);

        // TX_2: Opprette nyTerminbekreftelse behandling fra tidligere behandling
        behandling1 = repository.hent(Behandling.class, behandling1.getId());
        Behandling behandling2 = Behandling.fraTidligereBehandling(behandling1, BehandlingType.REVURDERING)
            .medKopiAvForrigeBehandlingsresultat()
            .build();
        lagreBeregningResultat(behandling2, getBehandlingsresultat(behandling2).getBeregningResultat());
        lagreBehandling(behandling2);

        // TX_3: Oppdatere nyTerminbekreftelse behandling med beregning
        behandling2 = repository.hent(Behandling.class, behandling2.getId());
        LegacyESBeregning beregning2 = new LegacyESBeregning(sats + 1, antallBarn, tilkjentYtelse, nå);
        LegacyESBeregningsresultat.builder().medBeregning(beregning2).buildFor(behandling2);
        lagreBeregningResultat(behandling2, getBehandlingsresultat(behandling2).getBeregningResultat());

        // TX_4: Oppdatere nyTerminbekreftelse behandling med beregning (samme som TX_3, men nyTerminbekreftelse beregning med nyTerminbekreftelse verdi)
        behandling2 = repository.hent(Behandling.class, behandling2.getId());
        LegacyESBeregning beregning3 = new LegacyESBeregning(sats + 2, antallBarn, tilkjentYtelse, nå);
        LegacyESBeregningsresultat.builder().medBeregning(beregning3).buildFor(behandling2);
        lagreBeregningResultat(behandling2, getBehandlingsresultat(behandling2).getBeregningResultat());

        // Assert
        behandling2 = repository.hent(Behandling.class, behandling2.getId());
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
    public void skal_bevare_vilkårresultat_ved_oppdatering_av_beregingsresultat() {
        // Act
        // TX_1: Opprette Behandlingsresultat med Beregningsresultat
        LegacyESBeregning beregning1 = new LegacyESBeregning(sats, antallBarn, tilkjentYtelse, nå);
        LegacyESBeregningsresultat beregningResultat1 = LegacyESBeregningsresultat.builder().medBeregning(beregning1).buildFor(behandling1);
        lagreBeregningResultat(behandling1, beregningResultat1);

        // TX_2: Oppdatere Behandlingsresultat med VilkårResultat
        behandling1 = repository.hent(Behandling.class, behandling1.getId());
        VilkårResultat vilkårResultat = VilkårResultat.builder()
                .leggTilVilkårResultat(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT, VilkårUtfallMerknad.VM_1001, new Properties(), null, false, false, null, null)
                .buildFor(behandling1);

        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling1);
        behandlingRepository.lagre(vilkårResultat, lås);

        // TX_3: Oppdatere Behandlingsresultat med BeregningResultat
        behandling1 = repository.hent(Behandling.class, behandling1.getId());
        LegacyESBeregning oppdatertBeregning = new LegacyESBeregning(sats + 1, antallBarn, tilkjentYtelse, nå);
        LegacyESBeregningsresultat beregningResultat2 = LegacyESBeregningsresultat.builder().medBeregning(oppdatertBeregning).buildFor(behandling1);
        lagreBeregningResultat(behandling1, beregningResultat2);

        // Assert
        Behandling hentetBehandling = repository.hent(Behandling.class, behandling1.getId());
        assertThat(getBehandlingsresultat(hentetBehandling).getVilkårResultat())
                .isEqualTo(vilkårResultat);
    }
}
