package no.nav.foreldrepenger.behandlingslager.behandling.repository;


import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSats;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.vedtak.exception.TekniskException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;

class LegacyESBeregningRepositoryTest extends EntityManagerAwareTest {

    @Test
    void skal_kaste_feil_dersom_eksakt_sats_ikke_kan_identifiseres() {
        var entityManager = getEntityManager();
        var repository = new LegacyESBeregningRepository(entityManager);

        var dato = LocalDate.now();
        var sats = new BeregningSats(BeregningSatsType.ENGANG, DatoIntervallEntitet.fraOgMed(dato.minusMonths(1)), 123L);
        var satsSomOverlapperEksisterendeSats = new BeregningSats(BeregningSatsType.ENGANG, DatoIntervallEntitet.fraOgMed(dato.minusDays(1)), 123L);
        entityManager.persist(sats);
        entityManager.persist(satsSomOverlapperEksisterendeSats);
        entityManager.flush();

        assertThrows(TekniskException.class, () -> repository.finnEksaktSats(BeregningSatsType.ENGANG, dato));
    }
}
