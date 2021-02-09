package no.nav.foreldrepenger.økonomistøtte.dagytelse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.wrapper.TilkjentYtelseAndel;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.wrapper.TilkjentYtelsePeriode;

public class SlåAndelerMedSammePeriodeOgKlassekodeSammen {

    private SlåAndelerMedSammePeriodeOgKlassekodeSammen() {
    }

    public static List<TilkjentYtelseAndel> slåAndelerMedSammePeriodeOgKlassekodeSammen(List<TilkjentYtelsePeriode> tilkjentYtelsePerioder, List<TilkjentYtelseAndel> andelerListeForMottakeren,
                                                                                        boolean erBrukerMottaker) {
        List<TilkjentYtelseAndel> andelerKombinertMedSammeKlassekodeForMottakeren = new ArrayList<>();
        if (andelerListeForMottakeren.isEmpty()) {
            return andelerKombinertMedSammeKlassekodeForMottakeren;
        }
        for (TilkjentYtelsePeriode tilkjentYtelsePeriode : tilkjentYtelsePerioder) {
            if (erBrukerMottaker) {
                List<TilkjentYtelseAndel> andelerListeIDennePerioden = getOppdragAndelerIEnPeriodeForBruker(andelerListeForMottakeren, tilkjentYtelsePeriode);
                andelerKombinertMedSammeKlassekodeForMottakeren.addAll(finnAndelerMedSammeKlassekode(andelerListeIDennePerioden));
            } else {
                List<TilkjentYtelseAndel> tilkjentYtelseAndelListeForArbeidsgiveren = getAndelerForArbeidsgiveren(andelerListeForMottakeren, tilkjentYtelsePeriode);
                andelerKombinertMedSammeKlassekodeForMottakeren.addAll(finnAndelerMedSammeKlassekode(tilkjentYtelseAndelListeForArbeidsgiveren));
            }
        }
        List<TilkjentYtelseAndel> fastsattAndelerListeForMottakeren = fastsettAndelerListe(andelerListeForMottakeren, andelerKombinertMedSammeKlassekodeForMottakeren);
        return sorterOppdragAndeler(fastsattAndelerListeForMottakeren);
    }

    private static List<TilkjentYtelseAndel> getAndelerForArbeidsgiveren(List<TilkjentYtelseAndel> andelerListeForMottakeren, TilkjentYtelsePeriode tilkjentYtelsePeriode) {
        return andelerListeForMottakeren.stream()
            .filter(andelArbeidsgiver -> andelArbeidsgiver.getTilkjentYtelsePeriode().equals(tilkjentYtelsePeriode))
            .collect(Collectors.toList());
    }

    private static List<TilkjentYtelseAndel> sorterOppdragAndeler(List<TilkjentYtelseAndel> fastsattAndelerListeForMottakeren) {
        return fastsattAndelerListeForMottakeren.stream()
            .sorted(Comparator.comparing(TilkjentYtelseAndel::getOppdragPeriodeFom))
            .collect(Collectors.toList());
    }

    private static List<TilkjentYtelseAndel> getOppdragAndelerIEnPeriodeForBruker(List<TilkjentYtelseAndel> andelerListeForMottakeren, TilkjentYtelsePeriode tilkjentYtelsePeriode) {
        return andelerListeForMottakeren.stream()
            .filter(andel -> andel.getTilkjentYtelsePeriode().equals(tilkjentYtelsePeriode))
            .filter(TilkjentYtelseAndel::skalTilBrukerEllerPrivatperson)
            .collect(Collectors.toList());
    }

    private static List<TilkjentYtelseAndel> finnAndelerMedSammeKlassekode(List<TilkjentYtelseAndel> andelListeIEnPeriodeForMottakeren) {

        List<TilkjentYtelseAndel> andelerFiltrertListe = new ArrayList<>();
        boolean erBrukerMottaker = andelListeIEnPeriodeForMottakeren.stream()
            .anyMatch(TilkjentYtelseAndel::skalTilBrukerEllerPrivatperson);
        if (erBrukerMottaker) {
            andelerFiltrertListe.addAll(slåAndelerSammenForBruker(andelListeIEnPeriodeForMottakeren));
        } else {
            andelerFiltrertListe.addAll(slåAndelerSammenForArbeidsgiver(andelListeIEnPeriodeForMottakeren));
        }
        return andelerFiltrertListe;
    }

    // returnerer 0 eller 1 TilkjentYtelseAndel
    // Collections.emptyList() hvis tilkjentYtelseAndelListeForMottakeren.size() == 1
    private static List<TilkjentYtelseAndel> slåAndelerSammenForArbeidsgiver(List<TilkjentYtelseAndel> tilkjentYtelseAndelListeForMottakeren) {
        List<TilkjentYtelseAndel> andelerArbeidsgiver = new ArrayList<>();
        if (tilkjentYtelseAndelListeForMottakeren.size() > 1) {
            TilkjentYtelseAndel tilkjentYtelseAndel = slåSammenOppdragsAndelerMedSammeKlassekode(tilkjentYtelseAndelListeForMottakeren);
            andelerArbeidsgiver.add(tilkjentYtelseAndel);
        }
        return andelerArbeidsgiver;
    }

    private static List<TilkjentYtelseAndel> slåAndelerSammenForBruker(List<TilkjentYtelseAndel> andelListeIEnPeriodeForMottakeren) {
        List<TilkjentYtelseAndel> andelerBruker = new ArrayList<>();
        List<KodeKlassifik> klassekodeListe = KlassekodeUtleder.getKlassekodeListe(andelListeIEnPeriodeForMottakeren);
        for (KodeKlassifik klassekode : klassekodeListe) {
            List<TilkjentYtelseAndel> andelerMedSammeKlassekode = finnOppdragAndelerMedSammeKlassekode(andelListeIEnPeriodeForMottakeren, klassekode);
            if (andelerMedSammeKlassekode.size() > 1) {
                TilkjentYtelseAndel tilkjentYtelseAndel = slåSammenOppdragsAndelerMedSammeKlassekode(andelerMedSammeKlassekode);
                andelerBruker.add(tilkjentYtelseAndel);
            }
        }
        return andelerBruker;
    }

    private static List<TilkjentYtelseAndel> finnOppdragAndelerMedSammeKlassekode(List<TilkjentYtelseAndel> tilkjentYtelseAndelListe, KodeKlassifik klassekode) {
        return tilkjentYtelseAndelListe.stream()
            .filter(andel -> klassekode.equals(mapTilKlassekode(andel)))
            .collect(Collectors.toList());
    }

    private static List<TilkjentYtelseAndel> fastsettAndelerListe(List<TilkjentYtelseAndel> alleAndelerListeForMottakeren, List<TilkjentYtelseAndel> andelerKombinertMedSammeKlassekodeForMottakeren) {
        List<TilkjentYtelseAndel> fastsattAndelerListeForMottakeren = new ArrayList<>();
        for (TilkjentYtelseAndel kombinasjonAvFlereSomEnAndel : andelerKombinertMedSammeKlassekodeForMottakeren) {
            KodeKlassifik klassekode = mapTilKlassekode(kombinasjonAvFlereSomEnAndel);
            boolean erDetFjernet = alleAndelerListeForMottakeren.removeIf(eneAndel -> eneAndel.getTilkjentYtelsePeriode().equals(kombinasjonAvFlereSomEnAndel.getTilkjentYtelsePeriode())
                && klassekode.equals(mapTilKlassekode(eneAndel)));
            if (erDetFjernet) {
                fastsattAndelerListeForMottakeren.add(kombinasjonAvFlereSomEnAndel);
            }
        }
        fastsattAndelerListeForMottakeren.addAll(alleAndelerListeForMottakeren);
        return fastsattAndelerListeForMottakeren;
    }

    private static TilkjentYtelseAndel slåSammenOppdragsAndelerMedSammeKlassekode(List<TilkjentYtelseAndel> andelerMedSammeMottakerOgKlassekode) {

        TilkjentYtelseAndel nyAndel = andelerMedSammeMottakerOgKlassekode.get(0);
        int dagsatsSum = beregnDagsatsSum(andelerMedSammeMottakerOgKlassekode);

        TilkjentYtelseAndel.builder(nyAndel)
            .medDagsats(dagsatsSum);

        return nyAndel;
    }

    private static int beregnDagsatsSum(List<TilkjentYtelseAndel> andelerMedSammeKlassekode) {
        return andelerMedSammeKlassekode.stream()
            .mapToInt(TilkjentYtelseAndel::getDagsats)
            .sum();
    }

    private static KodeKlassifik mapTilKlassekode(TilkjentYtelseAndel andel) {
        return KlassekodeUtleder.utled(andel);
    }
}
