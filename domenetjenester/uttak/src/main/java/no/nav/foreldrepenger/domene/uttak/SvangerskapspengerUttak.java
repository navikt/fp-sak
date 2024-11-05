package no.nav.foreldrepenger.domene.uttak;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.uttak.svp.PeriodeIkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatArbeidsforholdEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatPeriodeEntitet;

public class SvangerskapspengerUttak implements Uttak {

    private final SvangerskapspengerUttakResultatEntitet intern;

    public SvangerskapspengerUttak(SvangerskapspengerUttakResultatEntitet intern) {
        this.intern = intern;
    }


    @Override
    public Optional<LocalDate> opphørsdato() {
        var opphørsdatoer = intern.getUttaksResultatArbeidsforhold().stream().flatMap(a -> opphørsdato(a).stream()).toList();
        if (opphørsdatoer.size() != getUttaksResultatArbeidsforhold().size()) {
            return Optional.empty();
        }
        return opphørsdatoer.stream().max(Comparator.naturalOrder());
    }

    private static Optional<LocalDate> opphørsdato(SvangerskapspengerUttakResultatArbeidsforholdEntitet arbeidsforhold) {
        var perioder = arbeidsforhold.getPerioder();
        if (arbeidsforhold.isAvslått()) {
            return perioder.stream().map(SvangerskapspengerUttakResultatPeriodeEntitet::getFom).min(Comparator.naturalOrder());
        }

        var sistePeriode = perioder.stream().max(Comparator.comparing(SvangerskapspengerUttakResultatPeriodeEntitet::getTom));
        if (sistePeriode.filter(SvangerskapspengerUttak::erOpphør).isEmpty()) {
            return Optional.empty();
        }

        var reversed = perioder.stream().sorted(Comparator.comparing(SvangerskapspengerUttakResultatPeriodeEntitet::getFom).reversed()).toList();

        // Fom dato i første periode av de siste sammenhengende periodene med opphør
        LocalDate opphørFom = null;
        for (var periode : reversed) {
            if (erOpphør(periode)) {
                opphørFom = periode.getFom();
            } else if (opphørFom != null) {
                return Optional.of(opphørFom);
            }
        }
        return Optional.ofNullable(opphørFom);
    }

    private static boolean erOpphør(SvangerskapspengerUttakResultatPeriodeEntitet periode) {
        return PeriodeIkkeOppfyltÅrsak.opphørsAvslagÅrsaker().contains(periode.getPeriodeIkkeOppfyltÅrsak());
    }

    @Override
    public boolean harUlikUttaksplan(Uttak other) {
        var uttakresultatSammenligneMed = (SvangerskapspengerUttak) other;
        if (!uttakresultatSammenligneMed.getSisteDagAvSistePeriode().isEqual(getSisteDagAvSistePeriode())) {
            return true;
        }

        if (!uttakresultatSammenligneMed.getFørsteDagAvFørstePeriode().isEqual(getFørsteDagAvFørstePeriode())) {
            return true;
        }
        return !erLikresultat(getUttaksResultatArbeidsforhold(), uttakresultatSammenligneMed.getUttaksResultatArbeidsforhold());
    }

    @Override
    public boolean harUlikKontoEllerMinsterett(Uttak uttak) {
        return false;
    }

    @Override
    public boolean harOpphørsUttakNyeInnvilgetePerioder(Uttak uttak) {
        return false;
    }

    @Override
    public boolean harAvslagPgaMedlemskap() {
        //TODO sync med svp brev
        return finnSisteUttaksperiodePrArbeidsforhold().stream().allMatch(SvangerskapspengerUttakResultatPeriodeEntitet::harAvslagPgaMedlemskap);
    }

    @Override
    public boolean altAvslått() {
        return getUttaksResultatArbeidsforhold().stream()
            .allMatch(a -> a.isAvslått() || a.getPerioder().stream().noneMatch(SvangerskapspengerUttakResultatPeriodeEntitet::isInnvilget));
    }

    private List<SvangerskapspengerUttakResultatArbeidsforholdEntitet> getUttaksResultatArbeidsforhold() {
        return intern.getUttaksResultatArbeidsforhold();
    }

    private static boolean erLikresultat(List<SvangerskapspengerUttakResultatArbeidsforholdEntitet> r1,
                                         List<SvangerskapspengerUttakResultatArbeidsforholdEntitet> r2) {
        if (r1.size() != r2.size()) {
            return false;
        }
        return r1.stream().allMatch(a1 -> r2.stream().anyMatch(a2 -> erLikArbeid(a1, a2)));
    }

    private static boolean erLikArbeid(SvangerskapspengerUttakResultatArbeidsforholdEntitet a1,
                                       SvangerskapspengerUttakResultatArbeidsforholdEntitet a2) {
        if (a1.getPerioder().size() != a2.getPerioder().size()) {
            return false;
        }
        var likeperioder = a1.getPerioder().stream().allMatch(p1 -> a2.getPerioder().stream().anyMatch(p2 -> erLikResPeriode(p1, p2)));
        return Objects.equals(a1.getArbeidsforholdIkkeOppfyltÅrsak(), a2.getArbeidsforholdIkkeOppfyltÅrsak()) && Objects.equals(a1.getArbeidsgiver(),
            a2.getArbeidsgiver()) && Objects.equals(a1.getArbeidsforholdRef(), a2.getArbeidsforholdRef()) && likeperioder;
    }

    private static boolean erLikResPeriode(SvangerskapspengerUttakResultatPeriodeEntitet r1, SvangerskapspengerUttakResultatPeriodeEntitet r2) {
        return Objects.equals(r1.getTidsperiode(), r2.getTidsperiode()) && Objects.equals(r1.getPeriodeIkkeOppfyltÅrsak(),
            r2.getPeriodeIkkeOppfyltÅrsak()) && Objects.equals(r1.getPeriodeResultatType(), r2.getPeriodeResultatType()) && (
            Objects.equals(r1.getUtbetalingsgrad(), r2.getUtbetalingsgrad()) || r1.getUtbetalingsgrad().compareTo(r2.getUtbetalingsgrad()) == 0);
    }

    private LocalDate getFørsteDagAvFørstePeriode() {
        return intern.finnFørsteUttaksdato().orElseThrow();
    }

    private LocalDate getSisteDagAvSistePeriode() {
        return intern.finnSisteUttaksdato().orElseThrow();
    }

    private List<SvangerskapspengerUttakResultatPeriodeEntitet> finnSisteUttaksperiodePrArbeidsforhold() {
        List<SvangerskapspengerUttakResultatPeriodeEntitet> sistePerioder = new ArrayList<>();
        intern.getUttaksResultatArbeidsforhold()
            .forEach(arbeidsforhold -> arbeidsforhold.getPerioder()
                .stream()
                .max(Comparator.comparing(SvangerskapspengerUttakResultatPeriodeEntitet::getTom))
                .ifPresent(sistePerioder::add));

        return sistePerioder;
    }
}
