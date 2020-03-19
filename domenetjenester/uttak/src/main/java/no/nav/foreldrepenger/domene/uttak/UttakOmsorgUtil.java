package no.nav.foreldrepenger.domene.uttak;

import static java.lang.Boolean.TRUE;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderAleneOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderAnnenforelderHarRettEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;

public final class UttakOmsorgUtil {

    private UttakOmsorgUtil() {
    }

    public static boolean harAleneomsorg(YtelseFordelingAggregat ytelseFordelingAggregat) {
        Optional<PerioderAleneOmsorgEntitet> perioderAleneOmsorg = ytelseFordelingAggregat.getPerioderAleneOmsorg();
        if (perioderAleneOmsorg.isPresent()) {
            return !perioderAleneOmsorg.get().getPerioder().isEmpty();
        }
        return TRUE.equals(ytelseFordelingAggregat.getOppgittRettighet().getHarAleneomsorgForBarnet());
    }

    public static boolean harAnnenForelderRett(YtelseFordelingAggregat ytelseFordelingAggregat,
                                               Optional<UttakResultatEntitet> annenpartsGjeldendeUttaksplan) {
        if (annenForelderHarUttakMedUtbetaling(annenpartsGjeldendeUttaksplan)) {
            return true;
        }
        Optional<PerioderAnnenforelderHarRettEntitet> perioderAnnenforelderHarRett = ytelseFordelingAggregat.getPerioderAnnenforelderHarRett();
        if (perioderAnnenforelderHarRett.isPresent()) {
            return !perioderAnnenforelderHarRett.get().getPerioder().isEmpty();
        }
        OppgittRettighetEntitet oppgittRettighet = ytelseFordelingAggregat.getOppgittRettighet();
        Objects.requireNonNull(oppgittRettighet, "oppgittRettighet");
        return oppgittRettighet.getHarAnnenForeldreRett() == null || oppgittRettighet.getHarAnnenForeldreRett();
    }

    public static boolean annenForelderHarUttakMedUtbetaling(Optional<UttakResultatEntitet> annenpartsGjeldendeUttaksplan) {
        return annenpartsGjeldendeUttaksplan.isPresent() && harUtbetaling(annenpartsGjeldendeUttaksplan.get());
    }

    private static boolean harUtbetaling(UttakResultatEntitet resultat) {
        return resultat.getGjeldendePerioder().getPerioder().stream().anyMatch(UttakOmsorgUtil::harUtbetaling);
    }

    private static boolean harUtbetaling(UttakResultatPeriodeEntitet periode) {
        return periode.getAktiviteter().stream().anyMatch(UttakOmsorgUtil::harUtbetaling);
    }

    private static boolean harUtbetaling(UttakResultatPeriodeAktivitetEntitet aktivitet) {
        return aktivitet.getUtbetalingsprosent().compareTo(BigDecimal.ZERO) > 0;
    }
}
