package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import static no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem.K9SAK;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger.PleiepengerGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger.PleiepengerPerioderEntitet;
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
        ForeldrepengerGrunnlag fpGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();

        var anvistFraKilde = iayGrunnlag.getAktørYtelseFraRegister(uttakInput.getBehandlingReferanse().aktørId())
            .map(this::pleiepengerAnvistePerioderMedUtbetaling).orElseGet(Stream::empty)
            .toList();
        if (anvistFraKilde.isEmpty()) {
            return Optional.empty();
        }
        // Lager tidslinjer av utbetalte perioder og innleggelser
        var anvist = anvistFraKilde.stream()
            .map(a -> new LocalDateSegment<>(a.getAnvistFOM(), VirkedagUtil.fredagLørdagTilSøndag(a.getAnvistTOM()), Boolean.TRUE))
            .collect(Collectors.collectingAndThen(Collectors.toList(), l -> new LocalDateTimeline<>(l, StandardCombinators::alwaysTrueForMatch)))
            .compress();
        var innlagt = fpGrunnlag.getPleiepengerGrunnlag()
            .flatMap(PleiepengerGrunnlagEntitet::getPerioderMedInnleggelse)
            .map(PleiepengerPerioderEntitet::getInnleggelser).orElseGet(List::of).stream()
            .map(i -> new LocalDateSegment<>(i.getPeriode().getFomDato(), VirkedagUtil.fredagLørdagTilSøndag(i.getPeriode().getTomDato()), Boolean.TRUE))
            .collect(Collectors.collectingAndThen(Collectors.toList(), l -> new LocalDateTimeline<>(l, StandardCombinators::alwaysTrueForMatch)))
            .compress();
        // Både utbetalt og innlagt
        var anvistInnlagt = anvist.intersection(innlagt).compress();
        // Både utbetalt og ikke innlagt - gjør disse til false for å vise ikke innlagt
        var anvistIkkeInnlagt = anvist.disjoint(innlagt).compress().stream()
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), Boolean.FALSE));
        // Slår sammen og lager PleiepengerPeriode
        var alleAnviste = Stream.concat(anvistInnlagt.stream(), anvistIkkeInnlagt)
            .map(a -> new PleiepengerPeriode(a.getFom(), VirkedagUtil.tomVirkedag(a.getTom()), a.getValue()))
            .toList();

        return Optional.of(new Pleiepenger(alleAnviste));
    }

    private Stream<YtelseAnvist> pleiepengerAnvistePerioderMedUtbetaling(AktørYtelse aktørYtelseFraRegister) {
        return aktørYtelseFraRegister.getAlleYtelser().stream()
            .filter(y -> K9SAK.equals(y.getKilde()))
            .filter(y -> RelatertYtelseType.PLEIEPENGER.contains(y.getRelatertYtelseType()))
            .flatMap(ytelse -> ytelse.getYtelseAnvist().stream()
                .filter(ya -> !ya.getUtbetalingsgradProsent().orElse(Stillingsprosent.ZERO).erNulltall()));
    }
}
