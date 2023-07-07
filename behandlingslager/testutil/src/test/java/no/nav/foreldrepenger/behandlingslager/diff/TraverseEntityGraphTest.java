package no.nav.foreldrepenger.behandlingslager.diff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapKildeType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;

class TraverseEntityGraphTest {

    @Test
    void skal_traverse_entity_graph() {
        var scenario = lagTestScenario();

        var behandling = scenario.lagMocked();

        var traverser = lagTraverser();

        assertThatCode(() -> traverser.traverse(behandling)).doesNotThrowAnyException();
    }

    @Test
    void skal_ikke_ha_diff_for_seg_selv() {

        var scenario = lagTestScenario();
        var target = scenario.lagMocked();

        var differ = new DiffEntity(lagTraverser());

        var diffResult = differ.diff(target, target);

        assertThat(diffResult.getLeafDifferences()).isEmpty();
    }

    @Test
    void skal_sammenligne_Lists_med_forskjellig_rekkefølge() {

        var differ = new DiffEntity(lagTraverser());

        var en = new DummyEntitetMedListe();

        en.leggTil(new DummyEntitet("a"));
        en.leggTil(new DummyEntitet("b"));

        // sjekk med annen rekkefølge
        var to = new DummyEntitetMedListe();
        to.leggTil(new DummyEntitet("b"));
        to.leggTil(new DummyEntitet("a"));
        assertThat(differ.diff(en, to).getLeafDifferences()).isEmpty();

        // sjekk også med kopi av seg selv
        var tre = new DummyEntitetMedListe();
        tre.leggTil(new DummyEntitet("a"));
        tre.leggTil(new DummyEntitet("b"));
        assertThat(differ.diff(en, tre).getLeafDifferences()).isEmpty();

        // sjekk med noe annerledes
        var fem = new DummyEntitetMedListe();
        fem.leggTil(new DummyEntitet("a"));
        fem.leggTil(new DummyEntitet("c"));
        assertThat(differ.diff(en, fem).getLeafDifferences()).hasSize(2);

    }

    @Test
    void skal_sammenligne_Lists_med_forskjellig_størrelse() {
        var differ = new DiffEntity(lagTraverser());

        var en = new DummyEntitetMedListe();

        en.leggTil(new DummyEntitet("a"));
        en.leggTil(new DummyEntitet("b"));

        // sjekk med noe mer
        var fire = new DummyEntitetMedListe();
        fire.leggTil(new DummyEntitet("a"));
        fire.leggTil(new DummyEntitet("b"));
        fire.leggTil(new DummyEntitet("c"));
        assertThat(differ.diff(en, fire).getLeafDifferences()).hasSize(1);

    }

    @Test
    void skal_diffe_fødselsdato() {

        var scenario = lagTestScenario();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now().plusDays(2));
        var target1 = scenario.lagMocked();
        var scenario1 = lagTestScenario();
        scenario1.medSøknadHendelse().medFødselsDato(LocalDate.now().plusDays(3));
        var target2 = scenario.lagMocked();

        var differ = new DiffEntity(lagTraverser());

        var diffResult = differ.diff(target1, target2);

        var leafDifferences = diffResult.getLeafDifferences();
        assertThat(leafDifferences.size()).isNotNegative();
        assertThat(containsKey(leafDifferences, "Behandlingsgrunnlag.søknad.familieHendelse.barna.[0].fødselsdato")).isFalse();

        // System.out.println(diffResult.getLeafDifferences());
    }

    @Test
    void skal_kun_diffe_på_markerte_felt() {
        // Arrange
        var medlemskap1 = new MedlemskapPerioderBuilder()
                .medMedlId(1L) // MedlId er ikke markert
                .medErMedlem(true)
                .build();

        var medlemskap2 = new MedlemskapPerioderBuilder()
                .medMedlId(2L) // MedlId er ikke markert
                .medErMedlem(false)
                .build();

        var differ = new DiffEntity(lagTraverserForTrackedFields());

        // Act
        var diffResult = differ.diff(medlemskap1, medlemskap2);

        // Assert
        assertThat(diffResult.getLeafDifferences()).hasSize(1);
    }

    @Test
    void skal_oppdage_diff_når_det_kommer_ny_entry() {
        // Arrange
        var periode1 = new MedlemskapPerioderBuilder().medErMedlem(true).build();
        var periode2 = new MedlemskapPerioderBuilder().medErMedlem(false).build();

        var differ = new DiffEntity(lagTraverserForTrackedFields());

        // Act
        var diffResult = differ.diff(List.of(periode1), List.of(periode1, periode2));

        // Assert
        assertThat(diffResult.getLeafDifferences()).hasSize(1);
    }

    @Test
    void skal_oppdage_diff_i_kodeverk() {

        // Arrange
        var periode1 = new MedlemskapPerioderBuilder().medKildeType(MedlemskapKildeType.ANNEN).build();
        var periode2 = new MedlemskapPerioderBuilder().medKildeType(MedlemskapKildeType.TPS).build();

        var differ = new DiffEntity(lagTraverser());

        // Act
        var diffResult = differ.diff(periode1, periode2);

        // Assert
        var leafDiffs = diffResult.getLeafDifferences();
        assertThat(leafDiffs).hasSize(1);

        // diff mot kopi
        var nyPeriode1 = new MedlemskapPerioderBuilder().medKildeType(MedlemskapKildeType.ANNEN).build();
        var diffResultNy = differ.diff(periode1, nyPeriode1);
        assertThat(diffResultNy.getLeafDifferences()).isEmpty();

    }

    private boolean containsKey(Map<Node, Pair> leafDifferences, String key) {
        for (var node : leafDifferences.keySet()) {
            if (node.toString().equals(key)) {
                return true;
            }
        }
        return false;
    }

    private TraverseGraph lagTraverserForTrackedFields() {
        var config = new TraverseJpaEntityGraphConfig();
        config.setIgnoreNulls(true);
        config.addRootClasses(Behandling.class);
        config.setOnlyCheckTrackedFields(true);
        return new TraverseGraph(config);
    }

    private TraverseGraph lagTraverser() {
        var config = new TraverseJpaEntityGraphConfig();
        config.setIgnoreNulls(true);
        config.addRootClasses(Behandling.class);
        return new TraverseGraph(config);
    }

    private ScenarioMorSøkerEngangsstønad lagTestScenario() {
        return ScenarioMorSøkerEngangsstønad
                .forFødsel()
                .medTilleggsopplysninger("hello");
    }

    @Entity
    static class DummyEntitetMedListe {

        @ChangeTracked
        @OneToMany
        private List<DummyEntitet> entiteter = new ArrayList<>();

        public void leggTil(DummyEntitet dummyEntitet) {
            entiteter.add(dummyEntitet);
        }
    }

    @Entity
    static class DummyEntitet {
        @Column(name = "kode")
        String kode;

        public DummyEntitet(String kode) {
            this.kode = kode;
        }

        @Override
        public int hashCode() {
            return Objects.hash(kode);
        }

        @Override
        public boolean equals(Object obj) {
            return Objects.equals(kode, ((DummyEntitet) obj).kode);
        }

        @Override
        public String toString() {
            return "Dummy<" + kode + ">";
        }

    }
}
