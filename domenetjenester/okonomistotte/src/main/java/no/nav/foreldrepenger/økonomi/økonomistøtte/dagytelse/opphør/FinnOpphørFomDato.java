package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.opphør;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.økonomi.økonomistøtte.Oppdragsmottaker;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.KlassekodeUtleder;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.TidligereOppdragTjeneste;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.OppdragInput;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.oppdragslinje150.Oppdragslinje150Util;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelseAndel;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelsePeriode;

class FinnOpphørFomDato {
    private static final Logger logger = LoggerFactory.getLogger(FinnOpphørFomDato.class);
    private FinnOpphørFomDato() {
    }

    static LocalDate finnOpphørFom(List<Oppdragslinje150> opp150Liste, OppdragInput behandlingInfo, Oppdragsmottaker mottaker) {
        List<TilkjentYtelsePeriode> forrigeTilkjentYtelsePeriodeListe = behandlingInfo.getForrigeTilkjentYtelsePerioder();
        String kodeKlassifik = opp150Liste.get(0).getKodeKlassifik();
        if (mottaker.erStatusOpphør()) {
            return finnForrigeFørsteInnvilgetPeriodeFom(kodeKlassifik, mottaker, forrigeTilkjentYtelsePeriodeListe);
        }
        LocalDate endringsdato = behandlingInfo.getEndringsdato()
            .orElse(forrigeTilkjentYtelsePeriodeListe.get(0).getFom());
        boolean finnesOpphør = opp150Liste.stream().anyMatch(Oppdragslinje150::gjelderOpphør);
        if (finnesOpphør) {
            LocalDate forrigeFørsteUttaksdatoForMottakeren = finnForrigeFørsteInnvilgetPeriodeFom(kodeKlassifik, mottaker, forrigeTilkjentYtelsePeriodeListe);
            return endringsdato.isBefore(forrigeFørsteUttaksdatoForMottakeren) ? forrigeFørsteUttaksdatoForMottakeren : endringsdato;
        }
        LocalDate førsteDatoVedtakFom = finnFørsteDatoVedtakFom(opp150Liste);
        return førsteDatoVedtakFom.isAfter(endringsdato) ? førsteDatoVedtakFom : endringsdato;
    }

    static LocalDate finnOpphørFomForBruker(Oppdragslinje150 sisteOppdr150Bruker, OppdragInput behandlingInfo) {
        String ident = behandlingInfo.getPersonIdent().getIdent();
        String kodeKlassifik = sisteOppdr150Bruker.getKodeKlassifik();
        List<TilkjentYtelsePeriode> forrigeTilkjentYtelsePeriodeListe = behandlingInfo.getForrigeTilkjentYtelsePerioder();

        return finnForrigeFørsteInnvilgetPeriodeFom(kodeKlassifik, new Oppdragsmottaker(ident, true), forrigeTilkjentYtelsePeriodeListe);
    }

    static LocalDate finnOpphørFomForArbeidsgiver(List<Oppdragslinje150> tidligereGjeldendeOpp150Liste, OppdragInput behandlingInfo) {
        String refunderesId = tidligereGjeldendeOpp150Liste.get(0).getRefusjonsinfo156().getRefunderesId();
        String orgnr = Oppdragslinje150Util.endreTilNiSiffer(refunderesId);
        String kodeKlassifik = tidligereGjeldendeOpp150Liste.get(0).getKodeKlassifik();
        List<TilkjentYtelsePeriode> forrigeTilkjentYtelsePeriodeListe = behandlingInfo.getForrigeTilkjentYtelsePerioder();

        return finnForrigeFørsteInnvilgetPeriodeFom(kodeKlassifik, new Oppdragsmottaker(orgnr, false), forrigeTilkjentYtelsePeriodeListe);
    }

    private static LocalDate finnFørsteDatoVedtakFom(List<Oppdragslinje150> opp150Liste) {
        return opp150Liste.stream()
            .map(Oppdragslinje150::getDatoVedtakFom)
            .min(Comparator.comparing(Function.identity()))
            .orElseThrow(() -> new IllegalStateException("Utvikler feil: Mangler vedtak fom dato"));
    }

    private static LocalDate finnForrigeFørsteInnvilgetPeriodeFom(String kodeKlassifik, Oppdragsmottaker mottaker, List<TilkjentYtelsePeriode> forrigeTilkjentYtelsePeriodeListe) {
        List<TilkjentYtelseAndel> forrigeTilkjentYtelseAndelListe = TidligereOppdragTjeneste.finnAndelerIOppdragPerioder(mottaker, forrigeTilkjentYtelsePeriodeListe);
        TilkjentYtelsePeriode forrigeFørsteInnvilgetPeriode = finnForrigeFørsteInnvilgetPeriodeForMottaker(mottaker,
            forrigeTilkjentYtelseAndelListe, kodeKlassifik);

        return forrigeFørsteInnvilgetPeriode.getFom();
    }

    private static TilkjentYtelsePeriode finnForrigeFørsteInnvilgetPeriodeForMottaker(Oppdragsmottaker mottaker, List<TilkjentYtelseAndel> forrigeTilkjentYtelseAndelListe,
                                                                                      String kodeKlassifik) {
        if (mottaker.erBruker()) {
            return getFørstePeriodeMedBrukersandel(forrigeTilkjentYtelseAndelListe, kodeKlassifik);
        }
        Optional<TilkjentYtelsePeriode> tilkjentYtelsePeriode = getFørstePeriodeMedArbeidsgiversandel(mottaker, forrigeTilkjentYtelseAndelListe);
        if(tilkjentYtelsePeriode.isEmpty()){
            logger.info("mottaker = {}, tilkjentytelseandelListe = {} ", mottaker, forrigeTilkjentYtelseAndelListe);
            throw new IllegalStateException("Utvikler feil: Mangler beregningsresultat periode for arbeidsgiver andel");
        }
        return tilkjentYtelsePeriode.get();
    }

    private static TilkjentYtelsePeriode getFørstePeriodeMedBrukersandel(List<TilkjentYtelseAndel> andelListe, String kodeKlassifik) {
        return andelListe.stream()
            .filter(andel -> KlassekodeUtleder.utled(andel).equals(kodeKlassifik))
            .sorted(Comparator.comparing(brAndel -> brAndel.getTilkjentYtelsePeriode().getFom()))
            .map(TilkjentYtelseAndel::getTilkjentYtelsePeriode)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Utvikler feil: Mangler beregningsresultat periode for bruker andel og inntektskategori: " + kodeKlassifik));
    }

    private static Optional<TilkjentYtelsePeriode> getFørstePeriodeMedArbeidsgiversandel(Oppdragsmottaker mottaker, List<TilkjentYtelseAndel> andelListe) {
        return andelListe.stream()
            .filter(andel -> andel.getArbeidsforholdOrgnr().equals(mottaker.getId()))
            .sorted(Comparator.comparing(andel -> andel.getTilkjentYtelsePeriode().getFom()))
            .map(TilkjentYtelseAndel::getTilkjentYtelsePeriode)
            .findFirst();
    }
}
