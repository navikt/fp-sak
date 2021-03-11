package no.nav.foreldrepenger.domene.uttak.saldo.fp;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.MaksDatoUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.saldo.SaldoUtregning;
import no.nav.foreldrepenger.regler.uttak.felles.Virkedager;
import no.nav.foreldrepenger.regler.uttak.felles.grunnlag.Stønadskontotype;

@ApplicationScoped
@FagsakYtelseTypeRef("FP")
public class MaksDatoUttakTjenesteImpl implements MaksDatoUttakTjeneste {

    private FpUttakRepository fpUttakRepository;
    private StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste;

    MaksDatoUttakTjenesteImpl() {
        //CDI
    }

    @Inject
    public MaksDatoUttakTjenesteImpl(FpUttakRepository fpUttakRepository,
                                     StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste) {
        this.fpUttakRepository = fpUttakRepository;
        this.stønadskontoSaldoTjeneste = stønadskontoSaldoTjeneste;
    }

    public Optional<LocalDate> beregnMaksDatoUttak(UttakInput uttakInput) {
        var ref = uttakInput.getBehandlingReferanse();
        var uttakResultat = fpUttakRepository.hentUttakResultatHvisEksisterer(ref.getBehandlingId());
        ForeldrepengerGrunnlag foreldrepengerGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        Optional<UttakResultatEntitet> annenpartResultat =
            foreldrepengerGrunnlag == null ? Optional.empty() : annenPartUttak(foreldrepengerGrunnlag);

        var sisteUttaksdato = finnSisteUttaksdato(uttakResultat, annenpartResultat);

        if (sisteUttaksdato.isPresent()) {
            var saldoUtregning = stønadskontoSaldoTjeneste.finnSaldoUtregning(uttakInput);
            if (ref.getRelasjonsRolleType().equals(RelasjonsRolleType.MORA)) {
                return Optional.of(beregnMaksDato(saldoUtregning,
                    List.of(Stønadskontotype.MØDREKVOTE, Stønadskontotype.FELLESPERIODE,
                        Stønadskontotype.FORELDREPENGER), sisteUttaksdato.get()));
            } else {
                return Optional.of(beregnMaksDato(saldoUtregning,
                    List.of(Stønadskontotype.FEDREKVOTE, Stønadskontotype.FELLESPERIODE,
                        Stønadskontotype.FORELDREPENGER), sisteUttaksdato.get()));
            }
        }
        return Optional.empty();
    }

    private Optional<UttakResultatEntitet> annenPartUttak(ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        var annenpart = foreldrepengerGrunnlag.getAnnenpart();

        if (annenpart.isPresent()) {
            return fpUttakRepository.hentUttakResultatHvisEksisterer(annenpart.get().getGjeldendeVedtakBehandlingId());
        }
        return Optional.empty();
    }

    private Optional<LocalDate> finnSisteUttaksdato(Optional<UttakResultatEntitet> uttakResultat,
                                                    Optional<UttakResultatEntitet> uttakResultatAnnenPart) {
        if (uttakResultat.isPresent()) {
            var erManuellBehandling = uttakResultat.get()
                .getGjeldendePerioder()
                .getPerioder()
                .stream()
                .anyMatch(UttakResultatPeriodeEntitet::opprinneligSendtTilManuellBehandling);

            if (erManuellBehandling) {
                return Optional.empty();
            }
        }

        List<UttakResultatPeriodeEntitet> allePerioder = new ArrayList<>();

        uttakResultatAnnenPart.ifPresent(
            uttakResultatEntitet -> allePerioder.addAll(uttakResultatEntitet.getGjeldendePerioder().getPerioder()));
        uttakResultat.ifPresent(
            uttakResultatEntitet -> allePerioder.addAll(uttakResultatEntitet.getGjeldendePerioder().getPerioder()));

        return allePerioder.stream()
            .filter(this::erInnvilgetEllerAvslåttMedTrekkdager)
            .max(Comparator.comparing(UttakResultatPeriodeEntitet::getTom))
            .map(UttakResultatPeriodeEntitet::getTom);
    }

    private boolean erInnvilgetEllerAvslåttMedTrekkdager(UttakResultatPeriodeEntitet periode) {
        var harTrekkdager = PeriodeResultatType.AVSLÅTT.equals(periode.getResultatType()) && periode.getAktiviteter()
            .stream()
            .anyMatch(aktivitet -> aktivitet.getTrekkdager().merEnn0());
        return periode.isInnvilget() || harTrekkdager;

    }

    private LocalDate beregnMaksDato(SaldoUtregning saldoUtregning,
                                     List<Stønadskontotype> gyldigeStønadskontoer,
                                     LocalDate sisteUttaksdato) {
        var tilgjengeligeDager = 0;

        for (var stønadskonto : gyldigeStønadskontoer) {
            tilgjengeligeDager += saldoUtregning.saldo(stønadskonto);
        }
        var maksdato = Virkedager.plusVirkedager(sisteUttaksdato, tilgjengeligeDager);
        return korrigerBortFraHelg(maksdato);
    }

    private LocalDate korrigerBortFraHelg(LocalDate maksdato) {
        if (erHelg(maksdato)) {
            return maksdato.with(DayOfWeek.FRIDAY);
        }
        return maksdato;
    }

    private boolean erHelg(LocalDate dato) {
        return dato.getDayOfWeek().equals(DayOfWeek.SATURDAY) || dato.getDayOfWeek().equals(DayOfWeek.SUNDAY);
    }
}
