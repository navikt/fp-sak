package no.nav.foreldrepenger.behandlingslager.behandling.repository;


import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSats;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.SatsRepository;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.vedtak.exception.TekniskException;

@ExtendWith(JpaExtension.class)
class EngangsstønadBeregningRepositoryTest extends EntityManagerAwareTest {

    @Test
    @Disabled("Feiler random siden PK sequence i baseline ligger før id fra insert i baseline")
    void skal_kaste_feil_dersom_eksakt_sats_ikke_kan_identifiseres() {
        var entityManager = getEntityManager();
        var repository = new SatsRepository(entityManager);

        var dato = LocalDate.now();
        var sats = new BeregningSats(BeregningSatsType.ENGANG, DatoIntervallEntitet.fraOgMed(dato.minusMonths(1)), 123L);
        var satsSomOverlapperEksisterendeSats = new BeregningSats(BeregningSatsType.ENGANG, DatoIntervallEntitet.fraOgMed(dato.minusDays(1)), 123L);
        entityManager.persist(sats);
        entityManager.persist(satsSomOverlapperEksisterendeSats);
        entityManager.flush();

        assertThrows(TekniskException.class, () -> repository.finnEksaktSats(BeregningSatsType.ENGANG, dato));
    }
}
