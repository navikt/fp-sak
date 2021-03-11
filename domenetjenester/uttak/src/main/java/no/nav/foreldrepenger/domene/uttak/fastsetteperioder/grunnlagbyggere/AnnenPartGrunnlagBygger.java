package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.domene.uttak.UttakEnumMapper;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Trekkdager;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AnnenPart;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AnnenpartUttakPeriode;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AnnenpartUttakPeriodeAktivitet;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Utbetalingsgrad;

@ApplicationScoped
public class AnnenPartGrunnlagBygger {

    private FpUttakRepository fpUttakRepository;

    @Inject
    public AnnenPartGrunnlagBygger(FpUttakRepository fpUttakRepository) {
        this.fpUttakRepository = fpUttakRepository;
    }

    AnnenPartGrunnlagBygger() {
        // CDI
    }

    public Optional<AnnenPart.Builder> byggGrunnlag(ForeldrepengerGrunnlag fpGrunnlag) {
        if (fpGrunnlag.getAnnenpart().isEmpty()) {
            return Optional.empty();
        }
        var annenpart = fpGrunnlag.getAnnenpart().get();

        var annenpartUttak = fpUttakRepository.hentUttakResultatHvisEksisterer(annenpart.getGjeldendeVedtakBehandlingId());
        if (annenpartUttak.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new AnnenPart.Builder()
            .medUttaksperioder(uttaksperioder(annenpartUttak.get())));
    }

    private List<AnnenpartUttakPeriode> uttaksperioder(UttakResultatEntitet annenpartUttak) {
        return annenpartUttak.getGjeldendePerioder().getPerioder().stream()
            .filter(this::erInnvilgetPeriodeEllerHarTrekkdager)
            .sorted(Comparator.comparing(UttakResultatPeriodeEntitet::getFom))
            .map(AnnenPartGrunnlagBygger::map)
            .collect(Collectors.toList());
    }

    public static AnnenpartUttakPeriode map(UttakResultatPeriodeEntitet periode) {
        var builder = utledBuilder(periode)
            .medSamtidigUttak(periode.isSamtidigUttak())
            .medFlerbarnsdager(periode.isFlerbarnsdager())
            .medInnvilget(PeriodeResultatType.INNVILGET.equals(periode.getResultatType()))
            .medMottattDato(periode.getPeriodeSøknad().map(ps -> ps.getMottattDato()).orElse(null));

        for (var aktivitet : periode.getAktiviteter()) {
            var utbetalingsgrad = new Utbetalingsgrad(aktivitet.getUtbetalingsgrad().decimalValue());
            var trekkdager = new Trekkdager(aktivitet.getTrekkdager().decimalValue());
            var trekkonto = UttakEnumMapper.map(aktivitet.getTrekkonto());
            var mapped = new AnnenpartUttakPeriodeAktivitet(UttakEnumMapper.map(aktivitet.getUttakAktivitet()),
                trekkonto, trekkdager, utbetalingsgrad);
            builder.medUttakPeriodeAktivitet(mapped);
        }
        return builder.build();
    }

    private static AnnenpartUttakPeriode.Builder utledBuilder(UttakResultatPeriodeEntitet periode) {
        if (periode.erUtsettelse()) {
            return utsettelseBuilder(periode);
        } else if (erOpphold(periode)) {
            return oppholdBuilder(periode);
        }
        return uttakBuilder(periode);
    }

    private static AnnenpartUttakPeriode.Builder uttakBuilder(UttakResultatPeriodeEntitet periode) {
        return AnnenpartUttakPeriode.Builder.uttak(periode.getFom(), periode.getTom());
    }

    private static AnnenpartUttakPeriode.Builder oppholdBuilder(UttakResultatPeriodeEntitet periode) {
        return AnnenpartUttakPeriode.Builder.opphold(periode.getFom(), periode.getTom(), UttakEnumMapper.map(periode.getOppholdÅrsak()));
    }

    private static boolean erOpphold(UttakResultatPeriodeEntitet periode) {
        return !OppholdÅrsak.UDEFINERT.equals(periode.getOppholdÅrsak());
    }

    private static AnnenpartUttakPeriode.Builder utsettelseBuilder(UttakResultatPeriodeEntitet periode) {
        return AnnenpartUttakPeriode.Builder.utsettelse(periode.getFom(), periode.getTom());
    }

    private boolean erInnvilgetPeriodeEllerHarTrekkdager(UttakResultatPeriodeEntitet p) {
        var harTrekkdager = p.getAktiviteter().stream().anyMatch(akt -> akt.getTrekkdager().merEnn0());
        return p.isInnvilget() || harTrekkdager;
    }
}
