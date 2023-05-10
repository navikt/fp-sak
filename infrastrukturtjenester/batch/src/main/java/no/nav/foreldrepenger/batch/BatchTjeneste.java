package no.nav.foreldrepenger.batch;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

public interface BatchTjeneste {

    String FOM_KEY = "fom";
    String TOM_KEY = "tom";
    String ANTALL_DAGER_KEY = "antallDager";
    String FAGOMRÅDE_KEY = "fagomrade";

    /**
     * Launches a batch.
     *
     * @param properties job arguments
     * @return unique executionId. Er sammensatt av BATCHNAME-UniqueId
     */
    String launch(Properties properties);

    /**
     * Unikt batchnavn etter følgende mønster:
     *     B<appnavn><løpenummer>
     *     Eks: BVLFP001 - Grensesnittavstemning
     *
     * @return unikt batchnavn
     */
    String getBatchName();

    record Periode(LocalDate fom, LocalDate tom) {
        public Periode {
            Objects.requireNonNull(fom);
            Objects.requireNonNull(tom);
            if (tom.isBefore(fom)) {
                throw new IllegalArgumentException("Tom før fom");
            }
        }
    }

    default Periode lagPeriodeEvtOppTilIDag(Properties properties) {
        return lagPeriodeEvtOppTilIDag(properties, Period.ZERO);
    }

    default Periode lagPeriodeEvtOppTilIDag(Properties properties, Period leggTilPeriode) {
        var fom = Optional.ofNullable(properties.getProperty(FOM_KEY)).map(LocalDate::parse).orElse(null);
        var tom = Optional.ofNullable(properties.getProperty(TOM_KEY)).map(LocalDate::parse).orElse(null);
        int antallDager = Optional.ofNullable(properties.getProperty(ANTALL_DAGER_KEY)).map(Integer::valueOf).orElse(1);
        if (fom != null && tom != null) {
            return new Periode(fom, tom);
        } else  if (fom != null) {
            return new Periode(fom, fom.plusDays(antallDager).plus(leggTilPeriode));
        } else  if (tom != null) {
            return new Periode(tom.minusDays(antallDager).plus(leggTilPeriode), tom);
        }
        var idag = LocalDate.now();
        return new Periode(idag.minusDays(antallDager).plus(leggTilPeriode), idag.minusDays(1).plus(leggTilPeriode));
    }
}
