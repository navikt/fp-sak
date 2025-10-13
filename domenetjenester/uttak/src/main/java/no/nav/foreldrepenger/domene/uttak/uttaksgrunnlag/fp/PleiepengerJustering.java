package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import static no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem.K9SAK;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.OppgittPeriodeUtil.finnesOverlapp;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.OppgittPeriodeUtil.slåSammenLikePerioder;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.AktørYtelse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvist;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Virkedager;
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
        if (oppgittePerioder.isEmpty()) {
            LOG.info("Oppgitte perioder er empty. Justerer ikke for pleiepenger");
            return oppgittePerioder;
        }

        if (finnesOverlapp(oppgittePerioder)) {
            LOG.warn("Finnes overlapp i oppgitte perioder");
            //Støtter ikke overlapp videre
            return oppgittePerioder;
        }

        var pleiepengerUtsettelser = pleiepengerUtsettelser(aktørId, inntektArbeidYtelseGrunnlag);
        if (pleiepengerUtsettelser.isEmpty()) {
            LOG.info("Ingen pleiepenger fra register");
            return oppgittePerioder;
        } else {
            LOG.info("Behandlingen har vedtak om pleiepenger. Oppretter utsettelser");
        }

        return combine(pleiepengerUtsettelser, oppgittePerioder);
    }

    static List<LocalDateSegment<PleiepengerUtsettelse>> pleiepengerUtsettelser(AktørId aktørId, InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        return inntektArbeidYtelseGrunnlag.getAktørYtelseFraRegister(aktørId)
            .map(AktørYtelse::getAlleYtelser).orElse(List.of()).stream()
            .filter(ytelse1 -> K9SAK.equals(ytelse1.getKilde()))
            .filter(ytelse1 -> RelatertYtelseType.PLEIEPENGER.contains(ytelse1.getRelatertYtelseType()))
            .flatMap(ytelse -> ytelse.getYtelseAnvist().stream()
                .filter(ya -> !ya.getUtbetalingsgradProsent().orElse(Stillingsprosent.ZERO).erNulltall())
                .map(ya -> mapTilSegment(ytelse, ya)))
            .toList();
    }

    static List<OppgittPeriodeEntitet> combine(List<LocalDateSegment<PleiepengerUtsettelse>> pleiepengerUtsettelser,
                                               List<OppgittPeriodeEntitet> foreldrepenger) {
        var foreldrepengerTimeline = oppgittPeriodeTimeline(foreldrepenger);
        var pleiepengerTimeline = new LocalDateTimeline<>(pleiepengerUtsettelser, PleiepengerJustering::slåSammenOverlappendePleiepenger);
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
                return copy(interval, OppgittPeriodeBuilder.ny()
                    .medPeriode(pp.getFom(), pp.getTom())
                    .medÅrsak(pp.getValue().årsak())
                    .medPeriodeKilde(FordelingPeriodeKilde.ANDRE_NAV_VEDTAK)
                    .build());

            });
        var combined = fellesTimeline.toSegments().stream().map(LocalDateSegment::getValue)
            .sorted(Comparator.comparing(OppgittPeriodeEntitet::getFom))
            .filter(p -> Virkedager.beregnAntallVirkedager(p.getFom(), p.getTom()) > 0)
            .toList();
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

    private static LocalDateSegment<PleiepengerUtsettelse> slåSammenOverlappendePleiepenger(LocalDateInterval dateInterval,
                                                                                            LocalDateSegment<PleiepengerUtsettelse> lhs,
                                                                                            LocalDateSegment<PleiepengerUtsettelse> rhs) {
        if (lhs != null && rhs != null) {
            var årsak = UtsettelseÅrsak.INSTITUSJON_BARN.equals(lhs.getValue().årsak()) || UtsettelseÅrsak.INSTITUSJON_BARN.equals(rhs.getValue().årsak()) ?
                UtsettelseÅrsak.INSTITUSJON_BARN : UtsettelseÅrsak.FRI;
            var senesteVedtak =  lhs.getValue().vedtakstidspunkt().isAfter(rhs.getValue().vedtakstidspunkt()) ?
                lhs.getValue().vedtakstidspunkt() : rhs.getValue().vedtakstidspunkt();
            return new LocalDateSegment<>(dateInterval, new PleiepengerUtsettelse(senesteVedtak, årsak));
        } else {
            return lhs == null ? new LocalDateSegment<>(dateInterval, rhs.getValue()) : new LocalDateSegment<>(dateInterval, lhs.getValue());
        }
    }

    private static LocalDateSegment<PleiepengerUtsettelse> mapTilSegment(Ytelse ytelse, YtelseAnvist periodeMedUtbetaltPleiepenger) {
        var utsettelseÅrsak = RelatertYtelseType.PLEIEPENGER_SYKT_BARN.equals(ytelse.getRelatertYtelseType()) ?
            UtsettelseÅrsak.INSTITUSJON_BARN : UtsettelseÅrsak.FRI;
        return new LocalDateSegment<>(periodeMedUtbetaltPleiepenger.getAnvistFOM(), periodeMedUtbetaltPleiepenger.getAnvistTOM(),
            new PleiepengerUtsettelse(ytelse.getVedtattTidspunkt(), utsettelseÅrsak));
    }

    record PleiepengerUtsettelse(LocalDateTime vedtakstidspunkt, UtsettelseÅrsak årsak) { }
}
