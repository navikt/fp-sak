package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import static no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem.K9SAK;
import static no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet.fraOgMedTilOgMed;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.AktørYtelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvist;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.ytelser.Pleiepenger;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.ytelser.PleiepengerPeriode;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.ytelser.Ytelser;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

@ApplicationScoped
public class YtelserGrunnlagBygger {

    public Ytelser byggGrunnlag(UttakInput uttakInput) {
        var pleiepengerMedInnleggelse = getPleiepengerMedInnleggelse(uttakInput);
        return new Ytelser(pleiepengerMedInnleggelse.orElse(null));
    }

    private Optional<Pleiepenger> getPleiepengerMedInnleggelse(UttakInput uttakInput) {
        var iayGrunnlag = uttakInput.getIayGrunnlag();
        var aktørYtelseFraRegister = iayGrunnlag.getAktørYtelseFraRegister(uttakInput.getBehandlingReferanse().aktørId());
        if (aktørYtelseFraRegister.isEmpty()) {
            return Optional.empty();
        }

        ForeldrepengerGrunnlag fpGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();

        var perioder = pleiepengerAnvistePerioderMedUtbetaling(aktørYtelseFraRegister.get()).map(
            ya -> new PleiepengerPeriode(ya.getAnvistFOM(), ya.getAnvistTOM(), erInnlagt(ya, fpGrunnlag))).toList();
        var slåttSammen = slåSammenLike(perioder);
        return Optional.of(new Pleiepenger(slåttSammen));
    }

    private List<PleiepengerPeriode> slåSammenLike(List<PleiepengerPeriode> perioder) {
        var segments = perioder.stream()
            .map(p -> new LocalDateSegment<>(p.getFom(), VirkedagUtil.fredagLørdagTilSøndag(p.getTom()), p.isBarnInnlagt()))
            .toList();
        var timeline = new LocalDateTimeline<>(segments, YtelserGrunnlagBygger::slåSammenOverlappendePleiepenger);

        return timeline.compress(Objects::equals, StandardCombinators::leftOnly)
            .stream()
            .map(s -> new PleiepengerPeriode(s.getFom(), VirkedagUtil.tomVirkedag(s.getTom()), s.getValue()))
            .toList();
    }

    private static LocalDateSegment<Boolean> slåSammenOverlappendePleiepenger(LocalDateInterval dateInterval,
                                                                              LocalDateSegment<Boolean> lhs,
                                                                              LocalDateSegment<Boolean> rhs) {
        var innlagt = lhs != null && Boolean.TRUE.equals(lhs.getValue()) || rhs != null && Boolean.TRUE.equals(rhs.getValue());
        return new LocalDateSegment<>(dateInterval, innlagt);

    }

    private Stream<YtelseAnvist> pleiepengerAnvistePerioderMedUtbetaling(AktørYtelse aktørYtelseFraRegister) {
        return aktørYtelseFraRegister.getAlleYtelser()
            .stream()
            .filter(y -> K9SAK.equals(y.getKilde()))
            .filter(y -> RelatertYtelseType.PLEIEPENGER.contains(y.getRelatertYtelseType()))
            .flatMap(
                ytelse -> ytelse.getYtelseAnvist().stream().filter(ya -> !ya.getUtbetalingsgradProsent().orElse(Stillingsprosent.ZERO).erNulltall()));
    }

    private boolean erInnlagt(YtelseAnvist ya, ForeldrepengerGrunnlag fpGrunnlag) {
        var ppGrunnlag = fpGrunnlag.getPleiepengerGrunnlag();
        if (ppGrunnlag.isEmpty()) {
            return false;
        }
        var perioderMedInnleggelse = ppGrunnlag.get().getPerioderMedInnleggelse();
        if (perioderMedInnleggelse.isEmpty()) {
            return false;
        }
        return perioderMedInnleggelse.get()
            .getInnleggelser()
            .stream()
            .anyMatch(p -> fraOgMedTilOgMed(ya.getAnvistFOM(), ya.getAnvistTOM()).erOmsluttetAv(p.getPeriode()));
    }
}
