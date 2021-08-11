package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import static no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem.K9SAK;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvist;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

final class PleiepengerJustering {

    private static final Logger LOG = LoggerFactory.getLogger(PleiepengerJustering.class);

    private PleiepengerJustering() {
    }

    static List<OppgittPeriodeEntitet> juster(AktørId aktørId,
                                              InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag,
                                              List<OppgittPeriodeEntitet> oppgittePerioder) {
        var aktørYtelseFraRegister = inntektArbeidYtelseGrunnlag.getAktørYtelseFraRegister(aktørId);
        if (aktørYtelseFraRegister.isEmpty()) {
            LOG.warn("Mangler ytelser fra register");
            return oppgittePerioder;
        }

        var perioderMedUtbetaltPleiepenger = aktørYtelseFraRegister.get()
            .getAlleYtelser()
            .stream()
            .filter(ytelse -> K9SAK.equals(ytelse.getKilde()))
            .filter(ytelse -> ytelse.getRelatertYtelseType().equals(RelatertYtelseType.PLEIEPENGER_SYKT_BARN))
            .flatMap(ytelse -> ytelse.getYtelseAnvist().stream())
            .filter(ya -> !ya.getUtbetalingsgradProsent().orElse(Stillingsprosent.ZERO).erNulltall())
            .toList();

        var utsettelser = opprettUtsettelser(perioderMedUtbetaltPleiepenger);

        return combine(utsettelser, oppgittePerioder);
    }

    static List<OppgittPeriodeEntitet> combine(List<OppgittPeriodeEntitet> pleiepengerUtsettelser,
                                               List<OppgittPeriodeEntitet> foreldrepenger) {
        var foreldrepengerTimeline = timeline(foreldrepenger);
        var pleiepengerTimeline = timeline(pleiepengerUtsettelser);
        var fellesTimeline = foreldrepengerTimeline.combine(pleiepengerTimeline,
            (interval, fp, pp) -> {
                if (pp == null) {
                    return new LocalDateSegment<>(interval, OppgittPeriodeBuilder.fraEksisterende(fp.getValue())
                        .medPeriode(interval.getFomDato(), interval.getTomDato())
                        .build());
                }
                return new LocalDateSegment<>(interval, OppgittPeriodeBuilder.fraEksisterende(pp.getValue())
                    .medPeriode(interval.getFomDato(), interval.getTomDato())
                    .build());
            }, LocalDateTimeline.JoinStyle.LEFT_JOIN);
        return fellesTimeline.toSegments().stream().map(s -> s.getValue()).toList();
    }

    private static LocalDateTimeline<OppgittPeriodeEntitet> timeline(List<OppgittPeriodeEntitet> oppgittePerioder) {
        var segments = oppgittePerioder.stream()
            .map(op -> new LocalDateSegment<>(op.getFom(), op.getTom(), op)).toList();
        return new LocalDateTimeline<>(segments);
    }

    private static List<OppgittPeriodeEntitet> opprettUtsettelser(List<YtelseAnvist> perioderMedUtbetaltPleiepenger) {
        return perioderMedUtbetaltPleiepenger.stream().map(p -> map(p)).toList();
    }

    private static OppgittPeriodeEntitet map(YtelseAnvist periodeMedUtbetaltPleiepenger) {
        return OppgittPeriodeBuilder.ny()
            .medPeriode(periodeMedUtbetaltPleiepenger.getAnvistFOM(), periodeMedUtbetaltPleiepenger.getAnvistTOM())
            .medÅrsak(UtsettelseÅrsak.INSTITUSJON_BARN)
            .medPeriodeKilde(FordelingPeriodeKilde.ANDRE_NAV_VEDTAK)
            .build();
    }
}
