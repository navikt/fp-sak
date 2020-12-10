package no.nav.foreldrepenger.domene.uttak.fakta.aktkrav;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AktivitetskravPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakOmsorgUtil;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.vedtak.util.env.Environment;

@ApplicationScoped
public class KontrollerAktivitetskravAksjonspunktUtleder {

    private static final Environment ENV = Environment.current();
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;

    @Inject
    public KontrollerAktivitetskravAksjonspunktUtleder(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                                       ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.foreldrepengerUttakTjeneste = foreldrepengerUttakTjeneste;
    }

    KontrollerAktivitetskravAksjonspunktUtleder() {
        //CDI
    }

    public List<AksjonspunktDefinisjon> utledFor(UttakInput uttakInput) {
        ForeldrepengerGrunnlag ytelsespesifiktGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        if (skalKontrollereAktivitetskrav(uttakInput.getBehandlingReferanse(),
            ytelsespesifiktGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse(), ytelsespesifiktGrunnlag)) {
            return List.of(AksjonspunktDefinisjon.KONTROLLER_AKTIVITETSKRAV);
        }
        return List.of();
    }

    public static SkalKontrollereAktiviteskravResultat skalKontrollereAktivitetskrav(BehandlingReferanse behandlingReferanse,
                                                                                     OppgittPeriodeEntitet periode,
                                                                                     YtelseFordelingAggregat ytelseFordelingAggregat,
                                                                                     FamilieHendelse familieHendelse,
                                                                                     boolean annenForelderHarRett) {
        if (ENV.isProd()) {
            return new SkalKontrollereAktiviteskravResultat(false, Set.of());
        }
        if (erMor(behandlingReferanse) || UttakOmsorgUtil.harAleneomsorg(ytelseFordelingAggregat)
            || familieHendelse.erStebarnsadopsjon() || MorsAktivitet.UFØRE.equals(periode.getMorsAktivitet())) {
            return new SkalKontrollereAktiviteskravResultat(false, Set.of());
        }
        var periodeType = periode.getPeriodeType();
        var harKravTilAktivitet =
            !periode.isFlerbarnsdager() && (periodeType.equals(UttakPeriodeType.FELLESPERIODE) || periodeType.equals(
                UttakPeriodeType.FORELDREPENGER) || bareFarHarRettOgSøkerUtsettelse(periode, annenForelderHarRett));
        if (!harKravTilAktivitet) {
            return new SkalKontrollereAktiviteskravResultat(false, Set.of());
        }

        var avklaring = finnAvklartePerioderSomDekkerSøknadsperiode(periode, ytelseFordelingAggregat);
        return new SkalKontrollereAktiviteskravResultat(true, avklaring);
    }

    private static boolean bareFarHarRettOgSøkerUtsettelse(OppgittPeriodeEntitet periode,
                                                           boolean annenForelderHarRett) {
        //Bare arbeid og ferie nå, må kanskje utvides med alle utsettelser
        return !annenForelderHarRett && (UtsettelseÅrsak.ARBEID.equals(periode.getÅrsak())
            || UtsettelseÅrsak.FERIE.equals(periode.getÅrsak()));
    }

    private static Set<AktivitetskravPeriodeEntitet> finnAvklartePerioderSomDekkerSøknadsperiode(OppgittPeriodeEntitet periode,
                                                                                                 YtelseFordelingAggregat ytelseFordelingAggregat) {
        var avklartePerioder = ytelseFordelingAggregat.getGjeldendeAktivitetskravPerioder()
            .stream()
            .flatMap(perioder -> perioder.getPerioder().stream())
            .collect(Collectors.toList());
        var dekkendeAvklartePerioder = new HashSet<AktivitetskravPeriodeEntitet>();
        var dato = periode.getFom();
        do {
            if (!erHelg(dato)) {
                var dekkendeAvklartPeriode = finnDekkendePeriode(dato, avklartePerioder);
                if (dekkendeAvklartPeriode.isEmpty()) {
                    return Set.of();
                }
                dekkendeAvklartePerioder.add(dekkendeAvklartPeriode.get());
            }
            dato = dato.plusDays(1);
        } while (!dato.isAfter(periode.getTom()));
        return dekkendeAvklartePerioder;
    }

    private static boolean erHelg(LocalDate dato) {
        return dato.getDayOfWeek() == DayOfWeek.SATURDAY || dato.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private static Optional<AktivitetskravPeriodeEntitet> finnDekkendePeriode(LocalDate dato,
                                                                              List<AktivitetskravPeriodeEntitet> avklartePerioder) {
        return avklartePerioder.stream().filter(p -> p.getTidsperiode().inkluderer(dato)).findFirst();
    }

    private boolean skalKontrollereAktivitetskrav(BehandlingReferanse behandlingReferanse,
                                                  FamilieHendelse familieHendelse,
                                                  ForeldrepengerGrunnlag fpGrunnlag) {
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandlingReferanse.getBehandlingId());
        return ytelseFordelingAggregat.getGjeldendeSøknadsperioder().getOppgittePerioder().stream().anyMatch(p -> {
            var resultat = skalKontrollereAktivitetskrav(behandlingReferanse, p, ytelseFordelingAggregat,
                familieHendelse, harAnnenForelderRett(ytelseFordelingAggregat, fpGrunnlag));
            return resultat.isKravTilAktivitet() && !resultat.isAvklart();
        });
    }

    private static boolean erMor(no.nav.foreldrepenger.behandling.BehandlingReferanse behandlingReferanse) {
        return RelasjonsRolleType.erMor(behandlingReferanse.getRelasjonsRolleType());
    }

    private boolean harAnnenForelderRett(YtelseFordelingAggregat ytelseFordelingAggregat,
                                         ForeldrepengerGrunnlag ytelsespesifiktGrunnlag) {
        var annenpart = ytelsespesifiktGrunnlag.getAnnenpart();
        return UttakOmsorgUtil.harAnnenForelderRett(ytelseFordelingAggregat,
            annenpart.isEmpty() ? Optional.empty() : foreldrepengerUttakTjeneste.hentUttakHvisEksisterer(
                annenpart.get().getGjeldendeVedtakBehandlingId()));
    }
}
