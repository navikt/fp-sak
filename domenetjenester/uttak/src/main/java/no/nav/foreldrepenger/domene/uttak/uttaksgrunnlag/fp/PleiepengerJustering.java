package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import static no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem.K9SAK;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.OppgittPeriodeUtil.finnesOverlapp;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.OppgittPeriodeUtil.slåSammenLikePerioder;

import java.time.LocalDateTime;
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
import no.nav.fpsak.tidsserie.LocalDateInterval;
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

        var k9pleiepenger = aktørYtelseFraRegister.get().getAlleYtelser().stream()
            .filter(ytelse -> K9SAK.equals(ytelse.getKilde()))
            .filter(ytelse -> ytelse.getRelatertYtelseType().equals(RelatertYtelseType.PLEIEPENGER_SYKT_BARN))
            .toList();
        LOG.info("Pleiepenger k9 i IAY {} {}", k9pleiepenger, k9pleiepenger.stream().map(ytelse -> ytelse.getYtelseAnvist()).toList());
        var pleiepengerUtsettelser = k9pleiepenger.stream()
            .flatMap(ytelse -> ytelse.getYtelseAnvist().stream()
                .filter(ya -> !ya.getUtbetalingsgradProsent().orElse(Stillingsprosent.ZERO).erNulltall())
                .map(ya -> new PleiepengerUtsettelse(ytelse.getVedtattTidspunkt(), map(ya))))
            .toList();

        LOG.info("Pleiepenger utsettelser {}", pleiepengerUtsettelser);

        exceptionHvisOverlapp(pleiepengerUtsettelser);

        var combine = combine(pleiepengerUtsettelser, oppgittePerioder);

        LOG.info("Pleiepenger etter combine {}", combine);
        return combine;
    }

    private static void exceptionHvisOverlapp(List<PleiepengerUtsettelse> pleiepengerUtsettelser) {
        var oppgittPerioder = pleiepengerUtsettelser.stream().map(u -> u.oppgittPeriode()).toList();
        if (finnesOverlapp(oppgittPerioder)) {
            throw new IllegalStateException("Utviklerfeil: Overlappende utsettelser pga pleiepenger " + oppgittPerioder);
        }
    }

    static List<OppgittPeriodeEntitet> combine(List<PleiepengerUtsettelse> pleiepengerUtsettelser,
                                               List<OppgittPeriodeEntitet> foreldrepenger) {
        var foreldrepengerTimeline = oppgittPeriodeTimeline(foreldrepenger);
        var pleiepengerTimeline = pleiepengerUtsettelseTimeline(pleiepengerUtsettelser);
        var førsteSøkteDag = foreldrepengerTimeline.getMinLocalDate();
        var sisteSøkteDag = foreldrepengerTimeline.getMaxLocalDate();
        var fellesTimeline = foreldrepengerTimeline.union(pleiepengerTimeline,
            (interval, fp, pp) -> {
                if (pp == null) {
                    return copy(interval, fp.getValue());
                }
                if (interval.getTomDato().isBefore(førsteSøkteDag) || interval.getFomDato().isAfter(sisteSøkteDag)) {
                    return null;
                }
                if (fp != null) {
                    //Hvis søknad om periode er mottatt etter vedtak så beholder vi søknadsperiode
                    var vedtakdato = pp.getValue().vedtakstidspunkt();
                    var mottattDato = fp.getValue().getMottattDato();
                    if (vedtakdato != null && mottattDato != null && mottattDato.isAfter(vedtakdato.toLocalDate())) {
                        return copy(interval, fp.getValue());
                    }
                }
                return copy(interval, pp.getValue().oppgittPeriode());

            });
        var combined = fellesTimeline.toSegments().stream().map(s -> s.getValue()).toList();
        return slåSammenLikePerioder(combined);
    }

    private static LocalDateSegment<OppgittPeriodeEntitet> copy(LocalDateInterval interval, OppgittPeriodeEntitet eksisterende) {
        return new LocalDateSegment<>(interval, OppgittPeriodeBuilder.fraEksisterende(eksisterende)
            .medPeriode(interval.getFomDato(), interval.getTomDato())
            .build());
    }

    private static LocalDateTimeline<OppgittPeriodeEntitet> oppgittPeriodeTimeline(List<OppgittPeriodeEntitet> oppgittePerioder) {
        var segments = oppgittePerioder.stream()
            .map(op -> new LocalDateSegment<>(op.getFom(), op.getTom(), op)).toList();
        return new LocalDateTimeline<>(segments);
    }

    private static LocalDateTimeline<PleiepengerUtsettelse> pleiepengerUtsettelseTimeline(List<PleiepengerUtsettelse> pleiepengerUtsettelser) {
        var segments = pleiepengerUtsettelser.stream()
            .map(op -> new LocalDateSegment<>(op.oppgittPeriode.getFom(), op.oppgittPeriode().getTom(), op)).toList();
        return new LocalDateTimeline<>(segments);
    }

    private static OppgittPeriodeEntitet map(YtelseAnvist periodeMedUtbetaltPleiepenger) {
        return OppgittPeriodeBuilder.ny()
            .medPeriode(periodeMedUtbetaltPleiepenger.getAnvistFOM(), periodeMedUtbetaltPleiepenger.getAnvistTOM())
            .medÅrsak(UtsettelseÅrsak.INSTITUSJON_BARN)
            .medPeriodeKilde(FordelingPeriodeKilde.ANDRE_NAV_VEDTAK)
            .build();
    }

    static record PleiepengerUtsettelse(LocalDateTime vedtakstidspunkt, OppgittPeriodeEntitet oppgittPeriode) { }
}
