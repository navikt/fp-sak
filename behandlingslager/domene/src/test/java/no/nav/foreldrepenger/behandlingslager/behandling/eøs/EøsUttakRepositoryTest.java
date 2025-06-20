package no.nav.foreldrepenger.behandlingslager.behandling.eøs;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

@ExtendWith(JpaExtension.class)
class EøsUttakRepositoryTest {

    private EøsUttakRepository repository;

    @BeforeEach
    public void setup(EntityManager em) {
        this.repository = new EøsUttakRepository(em);
    }

    @Test
    void skal_lagre() {
        var tidsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusMonths(1));
        var trekkonto = UttakPeriodeType.FELLESPERIODE;
        var trekkdager = new Trekkdager(5);
        var perioder = new EøsUttaksperioderEntitet.Builder().leggTil(perioder(tidsperiode, trekkdager, trekkonto)).build();
        repository.lagreEøsUttak(1L, perioder);

        var entitet1 = repository.hentGrunnlag(1L).orElseThrow();

        assertThat(entitet1.getBehandlingId()).isEqualTo(1L);
        assertThat(entitet1.isAktiv()).isTrue();
        assertThat(entitet1.getPerioder()).hasSize(1);

        var førstePeriode1 = entitet1.getPerioder().getFirst();
        assertThat(førstePeriode1.getPeriode()).isEqualTo(tidsperiode);
        assertThat(førstePeriode1.getTrekkonto()).isEqualTo(trekkonto);
        assertThat(førstePeriode1.getTrekkdager()).isEqualTo(trekkdager);

        var tidsperiode2 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1));
        var tidsperiode3 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusYears(1), LocalDate.now().minusYears(1).plusMonths(1));
        var trekkonto2 = UttakPeriodeType.FEDREKVOTE;
        var trekkdager2 = new Trekkdager(10);
        var trekkdager3 = new Trekkdager(123);
        var trekkonto3 = UttakPeriodeType.MØDREKVOTE;
        var perioder2 = new EøsUttaksperioderEntitet.Builder().leggTil(perioder(tidsperiode2, trekkdager2, trekkonto2))
            .leggTil(perioder(tidsperiode3, trekkdager3, trekkonto3))
            .build();
        repository.lagreEøsUttak(1L, perioder2);

        var entitet2 = repository.hentGrunnlag(1L).orElseThrow();

        assertThat(entitet2.getBehandlingId()).isEqualTo(1L);
        assertThat(entitet2.isAktiv()).isTrue();
        assertThat(entitet2.getPerioder()).hasSize(2);

        var sortertePerioder = entitet2.getPerioder().stream().sorted(
            Comparator.comparing(EøsUttaksperiodeEntitet::getPeriode)).toList();
        var førstePeriode2 = sortertePerioder.getFirst();
        assertThat(førstePeriode2.getPeriode()).isEqualTo(tidsperiode3);
        assertThat(førstePeriode2.getTrekkonto()).isEqualTo(trekkonto3);
        assertThat(førstePeriode2.getTrekkdager()).isEqualTo(trekkdager3);

        var andrePeriode = sortertePerioder.get(1);
        assertThat(andrePeriode.getPeriode()).isEqualTo(tidsperiode2);
        assertThat(andrePeriode.getTrekkonto()).isEqualTo(trekkonto2);
        assertThat(andrePeriode.getTrekkdager()).isEqualTo(trekkdager2);

        assertThat(repository.hentGrunnlag(2L)).isEmpty();
    }

    private static List<EøsUttaksperiodeEntitet> perioder(DatoIntervallEntitet tidsperiode, Trekkdager trekkdager, UttakPeriodeType trekkonto) {
        var eøsUttakperiodeEntitet = new EøsUttaksperiodeEntitet.Builder().medPeriode(tidsperiode).medTrekkdager(trekkdager).medTrekkonto(trekkonto);
        return List.of(eøsUttakperiodeEntitet.build());
    }
}
