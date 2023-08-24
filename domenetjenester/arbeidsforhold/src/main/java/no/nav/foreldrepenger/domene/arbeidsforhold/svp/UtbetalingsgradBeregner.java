package no.nav.foreldrepenger.domene.arbeidsforhold.svp;

import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType.*;
import static no.nav.foreldrepenger.domene.arbeidsforhold.svp.FinnAktivitetsavtalerForUtbetalingsgrad.finnAktivitetsavtalerSomSkalBrukes;

class UtbetalingsgradBeregner {

    private static final BigDecimal NULL_PROSENT = BigDecimal.ZERO;
    private static final BigDecimal HUNDRE_PROSENT = BigDecimal.valueOf(100);
    private static final BigDecimal TUSEN = BigDecimal.valueOf(1000);

    static TilretteleggingMedUtbelingsgrad beregn(Collection<AktivitetsAvtale> avtalerAAreg, SvpTilretteleggingEntitet svpTilrettelegging,
            LocalDate termindato, Collection<Permisjon> velferdspermisjoner) {
        var termindatoMinus3UkerOg1Dag = termindato.minusWeeks(3).minusDays(1);

        var tidsserienForSøknad = byggTidsserienForSøknad(svpTilrettelegging.getTilretteleggingFOMListe(),
                svpTilrettelegging.getBehovForTilretteleggingFom(), termindatoMinus3UkerOg1Dag);
        var ferdigBeregnet = byggTidsserienForAareg(avtalerAAreg, svpTilrettelegging.getBehovForTilretteleggingFom(), termindatoMinus3UkerOg1Dag,
                velferdspermisjoner)
                        .combine(tidsserienForSøknad, UtbetalingsgradBeregner::regnUtUtbetalingsgrad, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        return kappOgBehold(svpTilrettelegging, termindatoMinus3UkerOg1Dag, ferdigBeregnet);
    }

    static TilretteleggingMedUtbelingsgrad beregnUtenAAreg(SvpTilretteleggingEntitet svpTilrettelegging, LocalDate termindato) {
        var termindatoMinus3UkerOg1Dag = termindato.minusWeeks(3).minusDays(1);
        var hundreProsentHelePerioden = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(svpTilrettelegging.getBehovForTilretteleggingFom(),
                termindatoMinus3UkerOg1Dag, HUNDRE_PROSENT)));

        var tidsserienForSøknad = byggTidsserienForSøknad(svpTilrettelegging.getTilretteleggingFOMListe(),
                svpTilrettelegging.getBehovForTilretteleggingFom(), termindatoMinus3UkerOg1Dag);
        var hundreProsentKombinertMedSøknad = hundreProsentHelePerioden
                .combine(tidsserienForSøknad, UtbetalingsgradBeregner::regnUtUtbetalingsgrad, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        return kappOgBehold(svpTilrettelegging, termindatoMinus3UkerOg1Dag, hundreProsentKombinertMedSøknad);
    }

    private static TilretteleggingMedUtbelingsgrad kappOgBehold(SvpTilretteleggingEntitet svpTilrettelegging, LocalDate termindatoMinus3UkerOg1Dag,
            LocalDateTimeline<BigDecimal> aaregKombinertMedSøknad) {
        var ferdigKappet = aaregKombinertMedSøknad
                .intersection(new LocalDateInterval(svpTilrettelegging.getBehovForTilretteleggingFom(), termindatoMinus3UkerOg1Dag));

        var periodeMedUtbetalingsgrad = beholdUtbetalingsgradSelvOmArbeidFrafaller(ferdigKappet).toSegments()
                .stream()
                .map(s -> new PeriodeMedUtbetalingsgrad(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()), s.getValue()))
                .toList();

        var tilretteleggingArbeidsforhold = new TilretteleggingArbeidsforhold(
                svpTilrettelegging.getArbeidsgiver().orElse(null),
                svpTilrettelegging.getInternArbeidsforholdRef().orElse(InternArbeidsforholdRef.nullRef()),
                finnUttakArbeidType(svpTilrettelegging.getArbeidType()));

        return new TilretteleggingMedUtbelingsgrad(tilretteleggingArbeidsforhold, periodeMedUtbetalingsgrad);
    }

    private static UttakArbeidType finnUttakArbeidType(ArbeidType arbeidType) {
        if (arbeidType.equals(ORDINÆRT_ARBEIDSFORHOLD)) {
            return UttakArbeidType.ORDINÆRT_ARBEID;
        }
        if (arbeidType.equals(SELVSTENDIG_NÆRINGSDRIVENDE)) {
            return UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE;
        }
        if (arbeidType.equals(FRILANSER) || arbeidType.equals(FRILANSER_OPPDRAGSTAKER_MED_MER)) {
            return UttakArbeidType.FRILANS;
        }
        return UttakArbeidType.ANNET;
    }

    private static LocalDateTimeline<BigDecimal> byggTidsserienForAareg(Collection<AktivitetsAvtale> avtalerAAreg, LocalDate jordmorsdato,
            LocalDate termindato, Collection<Permisjon> velferdspermisjoner) {
        var aktiviteter = finnAktivitetsavtalerSomSkalBrukes(avtalerAAreg, jordmorsdato, termindato);
        var summertStillingsprosent = finnSummertStillingsprosent(aktiviteter);
        LocalDate startDato;
        if (aktiviteter.stream().noneMatch(a -> a.getPeriode().inkluderer(jordmorsdato.minusDays(1)))) {
            startDato = aktiviteter.stream().map(AktivitetsAvtale::getPeriode).map(DatoIntervallEntitet::getFomDato).min(Comparator.naturalOrder())
                    .orElse(jordmorsdato);
        } else {
            startDato = jordmorsdato;
        }

        var stillingsprosentFraAareg = finnStillingsprosentFraAareg(velferdspermisjoner, summertStillingsprosent, startDato);
        var segment = new LocalDateSegment<BigDecimal>(startDato, termindato, stillingsprosentFraAareg);
        return new LocalDateTimeline<>(List.of(segment));

    }

    private static BigDecimal finnStillingsprosentFraAareg(Collection<Permisjon> velferdspermisjoner, BigDecimal summertStillingsprosent,
            LocalDate startDato) {
        var summertVelferdspermisjon = velferdspermisjoner.stream().filter(p -> p.getPeriode().inkluderer(startDato))
                .map(Permisjon::getProsentsats)
                .map(Stillingsprosent::getVerdi)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
        var stillingsprosentFraAktivitetsavtaler = nullBlirTilHundre(ikkeHøyereEnn100(summertStillingsprosent));
        return stillingsprosentFraAktivitetsavtaler.subtract(summertVelferdspermisjon).max(BigDecimal.ZERO);
    }

    private static BigDecimal finnSummertStillingsprosent(List<AktivitetsAvtale> aktiviteter) {
        // Stillingsprosent kan vere null her
        return aktiviteter.size() == 1
                ? nullBlirTilHundre(aktiviteter.get(0).getProsentsats() == null ? null : aktiviteter.get(0).getProsentsats().getVerdi())
                : aktiviteter.stream()
                        .map(AktivitetsAvtale::getProsentsats)
                        .reduce(UtbetalingsgradBeregner::summerStillingsprosent)
                        .map(Stillingsprosent::getVerdi)
                        .orElse(NULL_PROSENT);
    }

    private static LocalDateSegment<BigDecimal> regnUtUtbetalingsgrad(LocalDateInterval di,
            LocalDateSegment<BigDecimal> aareg,
            LocalDateSegment<UtbetalingsgradBeregningProsent> tilrettelegging) {
        if (tilrettelegging != null && tilrettelegging.getValue().overstyrtUtbetalingsgrad != null) {
            return new LocalDateSegment<>(di, tilrettelegging.getValue().overstyrtUtbetalingsgrad);
        }
        if (aareg != null && tilrettelegging != null) {
            var opprinnelig = aareg.getValue();
            var ny = tilrettelegging.getValue().stillingsprosent;
            var sum = opprinnelig.compareTo(NULL_PROSENT) == 0 ? NULL_PROSENT : opprinnelig.subtract(ny)
                .divide(opprinnelig, 2, RoundingMode.HALF_UP)
                .multiply(HUNDRE_PROSENT);

            // negativ sum blir satt til 0 utbetalingsgrad (Betaler ikke ut hvis man jobber
            // mer...)
            if (sum.compareTo(BigDecimal.ZERO) < 0) {
                sum = BigDecimal.ZERO;
            }
            return new LocalDateSegment<>(di, sum.setScale(0, RoundingMode.HALF_UP));
        }
        if (aareg == null) {
            return new LocalDateSegment<>(di, TUSEN);
        }
        return new LocalDateSegment<>(di, null);
    }

    private static Stillingsprosent summerStillingsprosent(Stillingsprosent førsteVersjon, Stillingsprosent sisteVersjon) {

        if (førsteVersjon != null && førsteVersjon.getVerdi() != null && sisteVersjon != null && sisteVersjon.getVerdi() != null) {
            return new Stillingsprosent(nullBlirTilHundre(ikkeHøyereEnn100(førsteVersjon.getVerdi().add(sisteVersjon.getVerdi()))));
        }
        if (førsteVersjon != null && førsteVersjon.getVerdi() != null) {
            return new Stillingsprosent(nullBlirTilHundre(ikkeHøyereEnn100(førsteVersjon.getVerdi())));
        }
        if (sisteVersjon != null && sisteVersjon.getVerdi() != null) {
            return new Stillingsprosent(nullBlirTilHundre(ikkeHøyereEnn100(sisteVersjon.getVerdi())));
        }
        return new Stillingsprosent(HUNDRE_PROSENT);
    }

    private static BigDecimal ikkeHøyereEnn100(BigDecimal verdi) {
        if (verdi.intValue() > 100) {
            return HUNDRE_PROSENT;
        }
        return verdi;
    }

    private static BigDecimal nullBlirTilHundre(BigDecimal verdi) {
        return verdi == null || verdi.compareTo(NULL_PROSENT) == 0 ? HUNDRE_PROSENT : verdi;
    }

    private static LocalDateTimeline<UtbetalingsgradBeregningProsent> byggTidsserienForSøknad(List<TilretteleggingFOM> tilretteleggingFOMListe,
            LocalDate jordmorsdato,
            LocalDate termindato) {
        var sortert = tilretteleggingFOMListe.stream().sorted(Comparator.comparing(TilretteleggingFOM::getFomDato))
                .toList();
        List<LocalDateSegment<UtbetalingsgradBeregningProsent>> segmenter = new ArrayList<>();

        for (var i = 0; i < sortert.size(); i++) {
            if (i == 0 && sortert.get(i).getFomDato().isAfter(jordmorsdato)) {
                segmenter.add(new LocalDateSegment<>(jordmorsdato, sortert.get(i).getFomDato().minusDays(1),
                    new UtbetalingsgradBeregningProsent(NULL_PROSENT, null)));
            }
            segmenter.add(new LocalDateSegment<>(sortert.get(i).getFomDato(), finnSluttdato(i, sortert, termindato), finnProsent(sortert.get(i))));
        }
        return new LocalDateTimeline<>(segmenter);
    }

    private static LocalDate finnSluttdato(int index, List<TilretteleggingFOM> sortert, LocalDate termindato) {
        var antallIndekser = sortert.size() - 1;
        if (index == antallIndekser) {
            if (sortert.get(index).getFomDato().isAfter(termindato)) {
                return sortert.get(index).getFomDato();
            }
            return termindato;
        }
        return sortert.get(index + 1).getFomDato().minusDays(1);
    }

    private static UtbetalingsgradBeregningProsent finnProsent(TilretteleggingFOM tilretteleggingFOM) {
        var overstyrtUtbetalingsgrad = tilretteleggingFOM.getOverstyrtUtbetalingsgrad();
        if (tilretteleggingFOM.getType().equals(TilretteleggingType.INGEN_TILRETTELEGGING)) {
            return new UtbetalingsgradBeregningProsent(NULL_PROSENT, overstyrtUtbetalingsgrad);
        }
        if (tilretteleggingFOM.getType().equals(TilretteleggingType.HEL_TILRETTELEGGING)) {
            return new UtbetalingsgradBeregningProsent(HUNDRE_PROSENT, overstyrtUtbetalingsgrad);
        }
        return new UtbetalingsgradBeregningProsent(tilretteleggingFOM.getStillingsprosent(), overstyrtUtbetalingsgrad);
    }

    private static LocalDateTimeline<BigDecimal> beholdUtbetalingsgradSelvOmArbeidFrafaller(LocalDateTimeline<BigDecimal> ferdigBeregnetOgKappet) {
        List<LocalDateSegment<BigDecimal>> segmenter = new ArrayList<>();
        var iterator = ferdigBeregnetOgKappet.toSegments().iterator();

        BigDecimal forrigeVerdi = null;
        while (iterator.hasNext()) {
            var next = iterator.next();
            if (TUSEN.equals(next.getValue()) && forrigeVerdi == null) {
                segmenter.add(new LocalDateSegment<>(next.getLocalDateInterval(), NULL_PROSENT));
            } else if (TUSEN.equals(next.getValue())) {
                segmenter.add(new LocalDateSegment<>(next.getLocalDateInterval(), forrigeVerdi));
            } else {
                segmenter.add(next);
                forrigeVerdi = next.getValue();
            }
        }
        return new LocalDateTimeline<>(segmenter).compress();
    }

    private record UtbetalingsgradBeregningProsent(BigDecimal stillingsprosent, BigDecimal overstyrtUtbetalingsgrad) {
    }
}
