package no.nav.foreldrepenger.behandlingslager.behandling.beregning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.vedtak.felles.testutilities.db.Repository;

public class SatsTypeTest {
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final Repository repository = repoRule.getRepository();
    private final BeregningsresultatRepository beregningRepository = new BeregningsresultatRepository(repoRule.getEntityManager());

    @Test
    public void skal_teste_verdier_for_sats_gbeløp_og_gsnitt() {
        List<BeregningSats> grunnbeløpListe = repository.hentAlle(BeregningSats.class).stream().filter(sats -> sats.getSatsType().equals(BeregningSatsType.GRUNNBELØP)).collect(Collectors.toList());
        List<BeregningSats> gsnittListe = repository.hentAlle(BeregningSats.class).stream().filter(sats -> sats.getSatsType().equals(BeregningSatsType.GSNITT)).collect(Collectors.toList());

        assertThat(grunnbeløpListe).isNotEmpty();
        assertThat(gsnittListe).isNotEmpty();
    }

    @Test
    public void skal_teste_gsnitt_fom_tom_er_1jan_og_31des() {
        List<BeregningSats> gsnittListe = repository.hentAlle(BeregningSats.class).stream().filter(sats -> sats.getSatsType().equals(BeregningSatsType.GSNITT)).collect(Collectors.toList());

        for (BeregningSats sats : gsnittListe) {
            final DatoIntervallEntitet satsPeriode = sats.getPeriode();
            assertThat(satsPeriode.getFomDato()).isEqualTo(satsPeriode.getFomDato().getYear() + "-01-01");
            assertThat(satsPeriode.getTomDato()).isEqualTo(satsPeriode.getFomDato().getYear() + "-12-31");
        }
    }

    @Test
    public void skal_teste_hvert_år_mellom_1964_2018_har_gverdi_for_feb15() {
        for (int i = 1967; i <= 2018; i++) {
            final BeregningSats sats = beregningRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.of(i, 2, 15));
            assertThat(sats).isNotNull();
            assertThat(sats.getVerdi()).isGreaterThan(5300);
        }
    }

    @Test
    public void skal_teste_gverdi_stiger_hvert_år() {
        for (int i = 1967; i <= 2016; i++) {
            final LocalDate localDate = LocalDate.of(i, 9, 15);
            final BeregningSats sats = beregningRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, localDate);
            final BeregningSats satsNestÅr = beregningRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, localDate.plusYears(1));

            assertThat(satsNestÅr.getVerdi()).isGreaterThan(sats.getVerdi());
        }
    }
}
