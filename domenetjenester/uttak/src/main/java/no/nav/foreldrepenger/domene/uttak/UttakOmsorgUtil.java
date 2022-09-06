package no.nav.foreldrepenger.domene.uttak;

import static java.lang.Boolean.TRUE;

import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.konfig.Environment;

public final class UttakOmsorgUtil {

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
        if (ytelseFordelingAggregat.getAnnenForelderRettAvklaring() == null &&
            (!oppgittAnnenForelderRettEØS(ytelseFordelingAggregat) || ytelseFordelingAggregat.getAnnenForelderRettEØSAvklaring() == null)) {
            var oppgittRettighet = ytelseFordelingAggregat.getOppgittRettighet();
            Objects.requireNonNull(oppgittRettighet, "oppgittRettighet");
            return oppgittRettighet.getHarAnnenForeldreRett() == null || oppgittRettighet.getHarAnnenForeldreRett();
        } else if (ytelseFordelingAggregat.getAnnenForelderRettAvklaring() == null && oppgittAnnenForelderRettEØS(ytelseFordelingAggregat)) {
            return avklartAnnenForelderHarRettEØS(ytelseFordelingAggregat);
        }
        return ytelseFordelingAggregat.getAnnenForelderRettAvklaring() || avklartAnnenForelderHarRettEØS(ytelseFordelingAggregat);
    }

    public static boolean morMottarUføretrygd(UføretrygdGrunnlagEntitet uføretrygdGrunnlag) {
        // Inntil videre er oppgittrettighet ikke komplett - derfor ser vi på om det finnes et UFO-grunnlag
        return uføretrygdGrunnlag != null && uføretrygdGrunnlag.annenForelderMottarUføretrygd();
    }

    public static Boolean avklartAnnenForelderHarRettEØS(YtelseFordelingAggregat ytelseFordelingAggregat) {
        return godtasStønadFraEØS() && TRUE.equals(ytelseFordelingAggregat.getAnnenForelderRettEØSAvklaring());
    }

    public static boolean oppgittAnnenForelderRettEØS(YtelseFordelingAggregat ytelseFordelingAggregat) {
        return godtasStønadFraEØS() && TRUE.equals(ytelseFordelingAggregat.getOppgittRettighet().getAnnenForelderRettEØS());
    }

    public static boolean annenForelderHarUttakMedUtbetaling(Optional<ForeldrepengerUttak> annenpartsGjeldendeUttaksplan) {
        return annenpartsGjeldendeUttaksplan.isPresent() && harUtbetaling(annenpartsGjeldendeUttaksplan.get());
    }

    private static boolean harUtbetaling(ForeldrepengerUttak resultat) {
        return resultat.getGjeldendePerioder().stream().anyMatch(p -> p.harUtbetaling());
    }

    private static boolean godtasStønadFraEØS() {
        return !ER_PROD;
    }

}
