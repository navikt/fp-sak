package no.nav.foreldrepenger.behandlingslager.behandling.beregning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.dbstoette.DBTestUtil;
import no.nav.foreldrepenger.dbstoette.JpaExtension;

@ExtendWith(JpaExtension.class)
public class SatsTypeTest {

    private BeregningsresultatRepository beregningRepository;
    private EntityManager entityManager;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        this.entityManager = entityManager;
        beregningRepository = new BeregningsresultatRepository(entityManager);
    }

    @Test
    public void skal_teste_verdier_for_sats_gbeløp_og_gsnitt() {
        //Denne testen går ut i fra at satser i db ikke er endret på siden migrering ble kjørt
        var grunnbeløpListe = DBTestUtil.hentAlle(entityManager, BeregningSats.class)
            .stream()
            .filter(sats -> sats.getSatsType().equals(BeregningSatsType.GRUNNBELØP))
            .collect(Collectors.toList());
        var gsnittListe = DBTestUtil.hentAlle(entityManager, BeregningSats.class)
            .stream()
            .filter(sats -> sats.getSatsType().equals(BeregningSatsType.GSNITT))
            .collect(Collectors.toList());

        assertThat(grunnbeløpListe).isNotEmpty();
        assertThat(gsnittListe).isNotEmpty();
    }

    @Test
    public void skal_teste_gsnitt_fom_tom_er_1jan_og_31des() {
        //Denne testen går ut i fra at satser i db ikke er endret på siden migrering ble kjørt
        var gsnittListe = DBTestUtil.hentAlle(entityManager, BeregningSats.class)
            .stream()
            .filter(sats -> sats.getSatsType().equals(BeregningSatsType.GSNITT))
            .collect(Collectors.toList());

        for (var sats : gsnittListe) {
            final var satsPeriode = sats.getPeriode();
            assertThat(satsPeriode.getFomDato()).isEqualTo(satsPeriode.getFomDato().getYear() + "-01-01");
            assertThat(satsPeriode.getTomDato()).isEqualTo(satsPeriode.getFomDato().getYear() + "-12-31");
        }
    }

    @Test
    public void skal_teste_hvert_år_mellom_1964_2018_har_gverdi_for_feb15() {
        for (var i = 1967; i <= 2018; i++) {
            var sats = beregningRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.of(i, 2, 15));
            assertThat(sats).isNotNull();
            assertThat(sats.getVerdi()).isGreaterThan(5300);
        }
    }

    @Test
    public void skal_teste_gverdi_stiger_hvert_år() {
        for (var i = 1967; i <= 2016; i++) {
            final var localDate = LocalDate.of(i, 9, 15);
            final var sats = beregningRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, localDate);
            final var satsNestÅr = beregningRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP,
                localDate.plusYears(1));

            assertThat(satsNestÅr.getVerdi()).isGreaterThan(sats.getVerdi());
        }
    }
}
