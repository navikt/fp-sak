package no.nav.foreldrepenger.domene.arbeidsforhold.svp;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType.FRILANSER;
import static no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER;
import static no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType.ORDINÆRT_ARBEIDSFORHOLD;
import static no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

class UtbetalingsgradBeregner {

    private static final BigDecimal NULL_PROSENT = BigDecimal.ZERO;
    private static final BigDecimal HUNDRE_PROSENT = BigDecimal.valueOf(100);
    private static final BigDecimal TUSEN = BigDecimal.valueOf(1000);

    static TilretteleggingMedUtbelingsgrad beregn(Collection<AktivitetsAvtale> avtalerAAreg, SvpTilretteleggingEntitet svpTilrettelegging, LocalDate termindato) {
        var termindatoMinus3UkerOg1Dag = termindato.minusWeeks(3).minusDays(1);

        var tidsserienForSøknad = byggTidsserienForSøknad(svpTilrettelegging.getTilretteleggingFOMListe(), svpTilrettelegging.getBehovForTilretteleggingFom(), termindatoMinus3UkerOg1Dag);
        var ferdigBeregnet = byggTidsserienForAareg(avtalerAAreg, svpTilrettelegging.getBehovForTilretteleggingFom(), termindatoMinus3UkerOg1Dag)
            .combine(tidsserienForSøknad, UtbetalingsgradBeregner::regnUtUtbetalingsgrad, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        return kappOgBehold(svpTilrettelegging, termindatoMinus3UkerOg1Dag, ferdigBeregnet);
    }

    static TilretteleggingMedUtbelingsgrad beregnUtenAAreg(SvpTilretteleggingEntitet svpTilrettelegging, LocalDate termindato) {
        var termindatoMinus3UkerOg1Dag = termindato.minusWeeks(3).minusDays(1);
        var hundreProsentHelePerioden = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(svpTilrettelegging.getBehovForTilretteleggingFom(),
            termindatoMinus3UkerOg1Dag, HUNDRE_PROSENT)));

        var tidsserienForSøknad = byggTidsserienForSøknad(svpTilrettelegging.getTilretteleggingFOMListe(), svpTilrettelegging.getBehovForTilretteleggingFom(), termindatoMinus3UkerOg1Dag);
        var hundreProsentKombinertMedSøknad = hundreProsentHelePerioden
            .combine(tidsserienForSøknad, UtbetalingsgradBeregner::regnUtUtbetalingsgrad, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        return kappOgBehold(svpTilrettelegging, termindatoMinus3UkerOg1Dag, hundreProsentKombinertMedSøknad);
    }

    private static TilretteleggingMedUtbelingsgrad kappOgBehold(SvpTilretteleggingEntitet svpTilrettelegging, LocalDate termindatoMinus3UkerOg1Dag, LocalDateTimeline<BigDecimal> aaregKombinertMedSøknad) {
        LocalDateTimeline<BigDecimal> ferdigKappet = aaregKombinertMedSøknad.intersection(new LocalDateInterval(svpTilrettelegging.getBehovForTilretteleggingFom(), termindatoMinus3UkerOg1Dag));

        List<PeriodeMedUtbetalingsgrad> periodeMedUtbetalingsgrad = beholdUtbetalingsgradSelvOmArbeidFrafaller(ferdigKappet).toSegments()
            .stream()
            .map(s -> new PeriodeMedUtbetalingsgrad(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()), s.getValue()))
            .collect(Collectors.toList());

        var tilretteleggingArbeidsforhold = new TilretteleggingArbeidsforhold(
            svpTilrettelegging.getArbeidsgiver().orElse(null),
            svpTilrettelegging.getInternArbeidsforholdRef().orElse(InternArbeidsforholdRef.nullRef()),
            finnUttakArbeidType(svpTilrettelegging.getArbeidType()));

        return new TilretteleggingMedUtbelingsgrad(tilretteleggingArbeidsforhold, periodeMedUtbetalingsgrad);
    }

    private static UttakArbeidType finnUttakArbeidType(ArbeidType arbeidType) {
        if (arbeidType.equals(ORDINÆRT_ARBEIDSFORHOLD)) {
            return UttakArbeidType.ORDINÆRT_ARBEID;
        } else if (arbeidType.equals(SELVSTENDIG_NÆRINGSDRIVENDE)) {
            return UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE;
        } else if (arbeidType.equals(FRILANSER) || arbeidType.equals(FRILANSER_OPPDRAGSTAKER_MED_MER)) {
            return UttakArbeidType.FRILANS;
        }
        return UttakArbeidType.ANNET;
    }

    private static LocalDateTimeline<BigDecimal> byggTidsserienForAareg(Collection<AktivitetsAvtale> avtalerAAreg, LocalDate jordmorsdato, LocalDate termindato) {
        List<AktivitetsAvtale> aktiviteter = avtalerAAreg
            .stream()
            .filter(a -> a.getProsentsats() != null)
            .filter(a -> a.getPeriode().overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, termindato)))
            .collect(Collectors.toList());

        if (aktiviteter.size() == 1) {
            AktivitetsAvtale aktivitetsAvtale = aktiviteter.get(0);
            LocalDateSegment<BigDecimal> segment = new LocalDateSegment<>(aktivitetsAvtale.getPeriode().getFomDato().isBefore(jordmorsdato) ? jordmorsdato : aktivitetsAvtale.getPeriode().getFomDato(),
                aktivitetsAvtale.getPeriode().getTomDato().isAfter(termindato) ? termindato : aktivitetsAvtale.getPeriode().getTomDato(),
                nullBlirTilHundre(ikkeHøyereEnn100(aktivitetsAvtale.getProsentsats().getVerdi())));
            return new LocalDateTimeline<>(List.of(segment), UtbetalingsgradBeregner::summerStillingsprosent);
        }

        List<LocalDateSegment<BigDecimal>> collect = aktiviteter
            .stream()
            .map(a -> new LocalDateSegment<>(a.getPeriode().getFomDato().isBefore(jordmorsdato) ? jordmorsdato : a.getPeriode().getFomDato(),
                a.getPeriode().getTomDato().isAfter(termindato) ? termindato : a.getPeriode().getTomDato(), a.getProsentsats().getVerdi()))
            .collect(Collectors.toList());

        LocalDateTimeline<BigDecimal> summertTidslinje = new LocalDateTimeline<>(collect, UtbetalingsgradBeregner::summerStillingsprosent);
        //TODO(OJR) setter 0 prosent til 100 hvis det forsatt er 0 etter summering, dette gjøres fordi FPSAK ikke har støtte for å kunne sette denne prosent ved hjelp av ett aksjonspunkt..
        // er også dårlig datakvalitet i aareg...
        return new LocalDateTimeline<>(summertTidslinje.toSegments().stream().map(s -> new LocalDateSegment<>(s.getLocalDateInterval(), nullBlirTilHundre(s.getValue()))).collect(Collectors.toList()));
    }

    private static LocalDateSegment<BigDecimal> regnUtUtbetalingsgrad(LocalDateInterval di,
                                                                      LocalDateSegment<BigDecimal> aareg,
                                                                      LocalDateSegment<UtbetalingsgradBeregningProsent> tilrettelegging) {
        if (tilrettelegging != null && tilrettelegging.getValue().overstyrtUtbetalingsgrad != null) {
            return new LocalDateSegment<>(di, tilrettelegging.getValue().overstyrtUtbetalingsgrad);
        }
        if (aareg != null && tilrettelegging != null) {
            BigDecimal opprinnelig = aareg.getValue();
            BigDecimal ny = tilrettelegging.getValue().stillingsprosent;
            BigDecimal sum = opprinnelig
                .subtract(ny)
                .divide(opprinnelig, 2, RoundingMode.HALF_UP)
                .multiply(HUNDRE_PROSENT);

            // negativ sum blir satt til 0 utbetalingsgrad (Betaler ikke ut hvis man jobber mer...)
            if (sum.compareTo(BigDecimal.ZERO) < 0) {
                sum = BigDecimal.ZERO;
            }
            return new LocalDateSegment<>(di, sum.setScale(0, RoundingMode.HALF_UP));
        } else if (aareg == null) {
            return new LocalDateSegment<>(di, TUSEN);
        }
        return new LocalDateSegment<>(di, null);
    }

    private static LocalDateSegment<BigDecimal> summerStillingsprosent(LocalDateInterval di,
                                                                       LocalDateSegment<BigDecimal> førsteVersjon,
                                                                       LocalDateSegment<BigDecimal> sisteVersjon) {

        if (førsteVersjon != null && sisteVersjon != null) {
            return new LocalDateSegment<>(di,  nullBlirTilHundre(ikkeHøyereEnn100(førsteVersjon.getValue().add(sisteVersjon.getValue()))));
        }
        if (førsteVersjon != null) {
            return new LocalDateSegment<>(di, nullBlirTilHundre(ikkeHøyereEnn100(førsteVersjon.getValue())));
        }
        return new LocalDateSegment<>(di, nullBlirTilHundre(ikkeHøyereEnn100(sisteVersjon.getValue())));
    }

    private static BigDecimal ikkeHøyereEnn100(BigDecimal verdi) {
        if (verdi.intValue() > 100) {
            return HUNDRE_PROSENT;
        }
        return verdi;
    }

    private static BigDecimal nullBlirTilHundre(BigDecimal verdi) {
        return verdi.compareTo(NULL_PROSENT) == 0 ? HUNDRE_PROSENT : verdi;
    }

    private static LocalDateTimeline<UtbetalingsgradBeregningProsent> byggTidsserienForSøknad(List<TilretteleggingFOM> tilretteleggingFOMListe,
                                                                                              LocalDate jordmorsdato,
                                                                                              LocalDate termindato) {
        List<TilretteleggingFOM> sortert = tilretteleggingFOMListe.stream().sorted(Comparator.comparing(TilretteleggingFOM::getFomDato)).collect(Collectors.toList());
        List<LocalDateSegment<UtbetalingsgradBeregningProsent>> segmenter = new ArrayList<>();

        for (int i = 0; i < sortert.size(); i++) {
            if (i == 0 && sortert.get(i).getFomDato().isAfter(jordmorsdato)) {
                segmenter.add(new LocalDateSegment<>(jordmorsdato, sortert.get(i).getFomDato().minusDays(1),
                    new UtbetalingsgradBeregningProsent(NULL_PROSENT, null)));
            }
            segmenter.add(new LocalDateSegment<>(sortert.get(i).getFomDato(), finnSluttdato(i, sortert, termindato), finnProsent(sortert.get(i))));
        }
        return new LocalDateTimeline<>(segmenter);
    }

    private static LocalDate finnSluttdato(int index, List<TilretteleggingFOM> sortert, LocalDate termindato) {
        int antallIndekser = sortert.size() - 1;
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
        } else if (tilretteleggingFOM.getType().equals(TilretteleggingType.HEL_TILRETTELEGGING)) {
            return new UtbetalingsgradBeregningProsent(HUNDRE_PROSENT, overstyrtUtbetalingsgrad);
        }
        return new UtbetalingsgradBeregningProsent(tilretteleggingFOM.getStillingsprosent(), overstyrtUtbetalingsgrad);
    }

    private static LocalDateTimeline<BigDecimal> beholdUtbetalingsgradSelvOmArbeidFrafaller(LocalDateTimeline<BigDecimal> ferdigBeregnetOgKappet) {
        List<LocalDateSegment<BigDecimal>> segmenter = new ArrayList<>();
        Iterator<LocalDateSegment<BigDecimal>> iterator = ferdigBeregnetOgKappet.toSegments().iterator();

        BigDecimal forrigeVerdi = null;
        while (iterator.hasNext()) {
            LocalDateSegment<BigDecimal> next = iterator.next();
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

    private static class UtbetalingsgradBeregningProsent {
        private final BigDecimal stillingsprosent;
        private final BigDecimal overstyrtUtbetalingsgrad;

        public UtbetalingsgradBeregningProsent(BigDecimal stillingsprosent, BigDecimal overstyrtUtbetalingsgrad) {

            this.stillingsprosent = stillingsprosent;
            this.overstyrtUtbetalingsgrad = overstyrtUtbetalingsgrad;
        }
    }
}
