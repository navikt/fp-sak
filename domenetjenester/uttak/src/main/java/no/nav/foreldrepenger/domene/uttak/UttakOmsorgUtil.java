package no.nav.foreldrepenger.domene.uttak;

import static java.lang.Boolean.TRUE;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.konfig.Environment;

public final class UttakOmsorgUtil {

    private static final LocalDate IKRAFTTREDELSE = LocalDate.of(2022,8,2); // LA STÅ inntil gitt dato - fjern etter det
    private static final boolean ER_PROD = Environment.current().isProd();

    private UttakOmsorgUtil() {
    }

    public static boolean harAleneomsorg(YtelseFordelingAggregat ytelseFordelingAggregat) {
        return Optional.ofNullable(ytelseFordelingAggregat.getAleneomsorgAvklaring())
            .orElseGet(() -> TRUE.equals(ytelseFordelingAggregat.getOppgittRettighet().getHarAleneomsorgForBarnet()));
    }

    public static boolean harAnnenForelderRett(YtelseFordelingAggregat ytelseFordelingAggregat,
                                               Optional<ForeldrepengerUttak> annenpartsGjeldendeUttaksplan) {
        if (annenForelderHarUttakMedUtbetaling(annenpartsGjeldendeUttaksplan)) {
            return true;
        }
        return Optional.ofNullable(ytelseFordelingAggregat.getAnnenForelderRettAvklaring())
            .orElseGet(() -> {
                var oppgittRettighet = ytelseFordelingAggregat.getOppgittRettighet();
                Objects.requireNonNull(oppgittRettighet, "oppgittRettighet");
                return oppgittRettighet.getHarAnnenForeldreRett() == null || oppgittRettighet.getHarAnnenForeldreRett();
            });
    }

    public static boolean morMottarUføretrygd(UføretrygdGrunnlagEntitet uføretrygdGrunnlag) {
        // Inntil videre er oppgittrettighet ikke komplett - derfor ser vi på om det finnes et UFO-grunnlag
        return uføretrygdGrunnlag != null && uføretrygdGrunnlag.annenForelderMottarUføretrygd();
    }

    public static boolean morMottarForeldrepengerEØS(YtelseFordelingAggregat ytelseFordelingAggregat) {
        return godtasStønadFraEØS() && TRUE.equals(ytelseFordelingAggregat.getMorStønadEØSAvklaring());
    }

    public static boolean morOppgittForeldrepengerEØS(YtelseFordelingAggregat ytelseFordelingAggregat) {
        return godtasStønadFraEØS() && TRUE.equals(ytelseFordelingAggregat.getOppgittRettighet().getMorMottarStønadEØS());
    }

    public static boolean annenForelderHarUttakMedUtbetaling(Optional<ForeldrepengerUttak> annenpartsGjeldendeUttaksplan) {
        return annenpartsGjeldendeUttaksplan.isPresent() && harUtbetaling(annenpartsGjeldendeUttaksplan.get());
    }

    private static boolean harUtbetaling(ForeldrepengerUttak resultat) {
        return resultat.getGjeldendePerioder().stream().anyMatch(p -> p.harUtbetaling());
    }

    private static boolean godtasStønadFraEØS() {
        return !(ER_PROD && LocalDate.now().isBefore(IKRAFTTREDELSE));
    }

}
