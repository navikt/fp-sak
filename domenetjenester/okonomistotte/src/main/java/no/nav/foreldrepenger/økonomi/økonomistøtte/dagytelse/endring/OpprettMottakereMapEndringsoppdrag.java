package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.endring;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.økonomi.økonomistøtte.Oppdragsmottaker;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.FinnStatusForMottakere;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.SlåAndelerMedSammePeriodeOgKlassekodeSammen;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.OppdragInput;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelseAndel;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelsePeriode;

class OpprettMottakereMapEndringsoppdrag {

    private OpprettMottakereMapEndringsoppdrag() {
    }

    static Map<Oppdragsmottaker, List<TilkjentYtelseAndel>> finnMottakereMedDeresAndelForEndringsoppdrag(OppdragInput oppdragInput, List<TilkjentYtelseAndel> andelerOriginal) {

        List<TilkjentYtelseAndel> andelerFomEndringsdatoListe = oppdragInput.getTilkjentYtelseAndelerFomEndringsdato();
        List<TilkjentYtelseAndel> brukersAndelerListe = FinnStatusForMottakere.getAndelerForMottakeren(andelerFomEndringsdatoListe, true);
        List<TilkjentYtelseAndel> arbeidsgivereAndelerListe = FinnStatusForMottakere.getAndelerForMottakeren(andelerFomEndringsdatoListe, false);

        List<TilkjentYtelsePeriode> tilkjentYtelsePerioder = oppdragInput.getTilkjentYtelsePerioderFomEndringsdato();
        List<TilkjentYtelseAndel> andelerForBruker = SlåAndelerMedSammePeriodeOgKlassekodeSammen.slåAndelerMedSammePeriodeOgKlassekodeSammen(
            tilkjentYtelsePerioder, brukersAndelerListe, true);
        Oppdragsmottaker mottakerBruker = FinnStatusForMottakere.fastsettMottakerStatusForBruker(oppdragInput, andelerOriginal, andelerFomEndringsdatoListe);
        List<Oppdragsmottaker> mottakerArbeidsgiverList = FinnStatusForMottakere.fastsettMottakerStatusForArbeidsgiver(oppdragInput, andelerOriginal, arbeidsgivereAndelerListe);

        return opprettMottakereMap(andelerForBruker, arbeidsgivereAndelerListe, tilkjentYtelsePerioder, mottakerBruker, mottakerArbeidsgiverList);
    }

    private static Map<Oppdragsmottaker, List<TilkjentYtelseAndel>> opprettMottakereMap(List<TilkjentYtelseAndel> brukersAndelerListe, List<TilkjentYtelseAndel> arbeidsgiversAndelerListe,
                                                                                        List<TilkjentYtelsePeriode> tilkjentYtelsePeriodeListe, Oppdragsmottaker mottakerBruker,
                                                                                        List<Oppdragsmottaker> mottakerArbeidsgiverList) {

        Map<Oppdragsmottaker, List<TilkjentYtelseAndel>> andelPrMottakerMap = new LinkedHashMap<>();

        if (!brukersAndelerListe.isEmpty() || mottakerBruker.erStatusUendret()) {
            List<TilkjentYtelseAndel> tilkjentYtelseAndelerBrukerListe = finnAndelerBasertPåMottakerStatus(brukersAndelerListe, mottakerBruker);
            andelPrMottakerMap.put(mottakerBruker, tilkjentYtelseAndelerBrukerListe);
        }
        if (!arbeidsgiversAndelerListe.isEmpty() || finnesArbeidsgiverMedStatusUendret(mottakerArbeidsgiverList)) {
            for (Oppdragsmottaker mottakerArbeidsgiver : mottakerArbeidsgiverList) {
                List<TilkjentYtelseAndel> tilkjentYtelseAndelForDetteOrgnr = finnAndelerBasertPåMottakerStatus(tilkjentYtelsePeriodeListe, mottakerArbeidsgiver, arbeidsgiversAndelerListe);
                andelPrMottakerMap.put(mottakerArbeidsgiver, tilkjentYtelseAndelForDetteOrgnr);
            }
        }
        return andelPrMottakerMap;
    }

    private static List<TilkjentYtelseAndel> finnAndelerBasertPåMottakerStatus(List<TilkjentYtelseAndel> brukersAndelerListe, Oppdragsmottaker mottakerBruker) {
        if (mottakerBruker.erStatusUendret()) {
            return Collections.emptyList();
        } else {
            return brukersAndelerListe;
        }
    }

    private static List<TilkjentYtelseAndel> finnAndelerBasertPåMottakerStatus(List<TilkjentYtelsePeriode> tilkjentYtelsePerioder, Oppdragsmottaker mottakerArbeidsgiver,
                                                                               List<TilkjentYtelseAndel> alleArbeidsgivereAndelerListe) {
        if (mottakerArbeidsgiver.erStatusUendret()) {
            return Collections.emptyList();
        }
        List<TilkjentYtelseAndel> andelerForDenneArbeidsgiveren = alleArbeidsgivereAndelerListe.stream()
            .filter(andel -> andel.getArbeidsforholdOrgnr().equals(mottakerArbeidsgiver.getOrgnr()))
            .collect(Collectors.toList());

        return SlåAndelerMedSammePeriodeOgKlassekodeSammen.slåAndelerMedSammePeriodeOgKlassekodeSammen(tilkjentYtelsePerioder,
            andelerForDenneArbeidsgiveren, false);
    }

    private static boolean finnesArbeidsgiverMedStatusUendret(List<Oppdragsmottaker> mottakerArbeidsgiverList) {
        return mottakerArbeidsgiverList.stream()
            .anyMatch(Oppdragsmottaker::erStatusUendret);
    }
}
