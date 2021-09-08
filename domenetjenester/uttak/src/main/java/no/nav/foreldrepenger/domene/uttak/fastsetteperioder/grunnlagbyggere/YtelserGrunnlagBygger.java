package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import static no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem.K9SAK;
import static no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet.fraOgMedTilOgMed;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

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
        var aktørYtelseFraRegister = iayGrunnlag.getAktørYtelseFraRegister(
            uttakInput.getBehandlingReferanse().getAktørId());
        if (aktørYtelseFraRegister.isEmpty()) {
            return Optional.empty();
        }

        ForeldrepengerGrunnlag fpGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();

        var perioder = pleiepengerAnvistePerioderMedUtbetaling(aktørYtelseFraRegister.get())
            .map(ya -> new PleiepengerPeriode(ya.getAnvistFOM(), ya.getAnvistTOM(), erInnlagt(ya, fpGrunnlag)))
            .toList();
        var slåttSammen = slåSammenLike(perioder);
        return Optional.of(new Pleiepenger(slåttSammen));
    }

    private List<PleiepengerPeriode> slåSammenLike(List<PleiepengerPeriode> perioder) {
        var segments = perioder.stream()
            .map(p -> new LocalDateSegment<>(p.getFom(), VirkedagUtil.tomSøndag(p.getTom()), p.isBarnInnlagt()))
            .collect(Collectors.toList());
        var timeline = new LocalDateTimeline<>(segments);

        return timeline.compress((b1, b2) -> b1 == b2, StandardCombinators::leftOnly)
            .stream()
            .map(s -> new PleiepengerPeriode(s.getFom(), VirkedagUtil.tomVirkedag(s.getTom()), s.getValue()))
            .toList();
    }

    private Stream<YtelseAnvist> pleiepengerAnvistePerioderMedUtbetaling(AktørYtelse aktørYtelseFraRegister) {
        return aktørYtelseFraRegister.getAlleYtelser().stream()
            .filter(y -> K9SAK.equals(y.getKilde()))
            .filter(y -> y.getRelatertYtelseType().equals(RelatertYtelseType.PLEIEPENGER_SYKT_BARN))
            .flatMap(ytelse -> ytelse.getYtelseAnvist().stream()
                .filter(ya -> !ya.getUtbetalingsgradProsent().orElse(Stillingsprosent.ZERO).erNulltall()));
    }

    private boolean erInnlagt(YtelseAnvist ya, ForeldrepengerGrunnlag fpGrunnlag) {
        if (fpGrunnlag.getPleiepengerGrunnlag().isEmpty()) {
            return false;
        }
        var perioderMedInnleggelse = fpGrunnlag.getPleiepengerGrunnlag().get()
            .getPerioderMedInnleggelse();
        if (perioderMedInnleggelse.isEmpty()) {
            return false;
        }
        return perioderMedInnleggelse.get().getInnleggelser().stream()
            .anyMatch(p -> fraOgMedTilOgMed(ya.getAnvistFOM(), ya.getAnvistTOM()).erOmsluttetAv(p.getPeriode())
        );
    }
}
