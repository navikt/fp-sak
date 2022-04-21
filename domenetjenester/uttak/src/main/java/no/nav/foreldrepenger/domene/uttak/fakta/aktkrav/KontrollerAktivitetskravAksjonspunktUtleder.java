package no.nav.foreldrepenger.domene.uttak.fakta.aktkrav;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import no.nav.foreldrepenger.regler.uttak.felles.Virkedager;

@ApplicationScoped
public class KontrollerAktivitetskravAksjonspunktUtleder {

    private static final Set<UtsettelseÅrsak> BFHR_MED_AKTIVITETSKRAV = Set.of(UtsettelseÅrsak.ARBEID, UtsettelseÅrsak.FERIE,
        UtsettelseÅrsak.SYKDOM, UtsettelseÅrsak.INSTITUSJON_BARN, UtsettelseÅrsak.INSTITUSJON_SØKER, UtsettelseÅrsak.FRI);

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
        if (helePeriodenErHelg(periode) || erMor(behandlingReferanse) || UttakOmsorgUtil.harAleneomsorg(
            ytelseFordelingAggregat) || familieHendelse.erStebarnsadopsjon() || MorsAktivitet.UFØRE.equals(
            periode.getMorsAktivitet()) || ytelseFordelingAggregat.getGjeldendeEndringsdatoHvisEksisterer().isEmpty()) {
            return ikkeKontrollerer();
        }
        var periodeType = periode.getPeriodeType();
        var harKravTilAktivitet =
            !periode.isFlerbarnsdager() && (periodeType.equals(UttakPeriodeType.FELLESPERIODE) || periodeType.equals(
                UttakPeriodeType.FORELDREPENGER) || bareFarHarRettOgSøkerUtsettelse(periode, annenForelderHarRett));
        if (!harKravTilAktivitet) {
            return ikkeKontrollerer();
        }

        var avklaring = finnAvklartePerioderSomDekkerSøknadsperiode(periode, ytelseFordelingAggregat);
        return new SkalKontrollereAktiviteskravResultat(true, avklaring);
    }

    private static boolean helePeriodenErHelg(OppgittPeriodeEntitet periode) {
        return Virkedager.beregnAntallVirkedager(periode.getFom(), periode.getTom()) == 0;
    }

    private static SkalKontrollereAktiviteskravResultat ikkeKontrollerer() {
        return new SkalKontrollereAktiviteskravResultat(false, Set.of());
    }

    private static boolean bareFarHarRettOgSøkerUtsettelse(OppgittPeriodeEntitet periode,
                                                           boolean annenForelderHarRett) {
        //Reglene sjekker ikke aktivitetskrav hvis tiltak nav eller hv
        return !annenForelderHarRett && (BFHR_MED_AKTIVITETSKRAV.contains(periode.getÅrsak()));
    }

    private static Set<AktivitetskravPeriodeEntitet> finnAvklartePerioderSomDekkerSøknadsperiode(OppgittPeriodeEntitet periode,
                                                                                                 YtelseFordelingAggregat ytelseFordelingAggregat) {
        var avklartePerioder = finnRelevanteAvklartePerioder(ytelseFordelingAggregat);
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

    private static List<AktivitetskravPeriodeEntitet> finnRelevanteAvklartePerioder(YtelseFordelingAggregat ytelseFordelingAggregat) {
        if (ytelseFordelingAggregat.getSaksbehandledeAktivitetskravPerioder().isPresent()) {
            return ytelseFordelingAggregat.getSaksbehandledeAktivitetskravPerioder().get().getPerioder();
        }
        return ytelseFordelingAggregat.getOpprinneligeAktivitetskravPerioder()
            .stream()
            .flatMap(perioder -> perioder.getPerioder().stream())
            .flatMap(p -> {
                //Behandlinger med nye oppgitte perioder (endringssøknader) må avklare aktivitetskrav uansett avklaringer gjort i tidligere behandlinger
                if (inneholderNyePerioder(ytelseFordelingAggregat)) {
                    return fjernPerioderEtterDato(p, ytelseFordelingAggregat.getGjeldendeEndringsdato()).stream();
                }
                return Stream.of(p);
            })
            .collect(Collectors.toList());
    }

    private static boolean inneholderNyePerioder(YtelseFordelingAggregat ytelseFordelingAggregat) {
        return ytelseFordelingAggregat.getOppgittFordeling() != null && !ytelseFordelingAggregat.getOppgittFordeling()
            .getOppgittePerioder()
            .isEmpty();
    }

    private static List<AktivitetskravPeriodeEntitet> fjernPerioderEtterDato(AktivitetskravPeriodeEntitet periode,
                                                                             LocalDate dato) {
        if (periode.getTidsperiode().inkluderer(dato) && !periode.getTidsperiode().getFomDato().isEqual(dato)) {
            return List.of(new AktivitetskravPeriodeEntitet(periode.getTidsperiode().getFomDato(), dato.minusDays(1),
                periode.getAvklaring(), periode.getBegrunnelse()));
        }
        if (!periode.getTidsperiode().getFomDato().isBefore(dato)) {
            return List.of();
        }
        return List.of(periode);
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
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandlingReferanse.behandlingId());
        return ytelseFordelingAggregat.getGjeldendeSøknadsperioder().getOppgittePerioder().stream().anyMatch(p -> {
            var resultat = skalKontrollereAktivitetskrav(behandlingReferanse, p, ytelseFordelingAggregat,
                familieHendelse, harAnnenForelderRett(ytelseFordelingAggregat, fpGrunnlag));
            return resultat.isKravTilAktivitet() && !resultat.isAvklart();
        });
    }

    private static boolean erMor(BehandlingReferanse behandlingReferanse) {
        return RelasjonsRolleType.erMor(behandlingReferanse.relasjonRolle());
    }

    private boolean harAnnenForelderRett(YtelseFordelingAggregat ytelseFordelingAggregat,
                                         ForeldrepengerGrunnlag ytelsespesifiktGrunnlag) {
        var annenpart = ytelsespesifiktGrunnlag.getAnnenpart();
        return UttakOmsorgUtil.harAnnenForelderRett(ytelseFordelingAggregat,
            annenpart.isEmpty() ? Optional.empty() : foreldrepengerUttakTjeneste.hentUttakHvisEksisterer(
                annenpart.get().gjeldendeVedtakBehandlingId()));
    }
}
