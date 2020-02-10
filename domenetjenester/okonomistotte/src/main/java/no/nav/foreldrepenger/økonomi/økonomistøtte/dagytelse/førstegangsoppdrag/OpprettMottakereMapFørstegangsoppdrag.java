package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.førstegangsoppdrag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.OppdragsmottakerStatus;
import no.nav.foreldrepenger.økonomi.økonomistøtte.Oppdragsmottaker;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.FinnStatusForMottakere;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.SlåAndelerMedSammePeriodeOgKlassekodeSammen;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.OppdragInput;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelseAndel;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelsePeriode;

class OpprettMottakereMapFørstegangsoppdrag {

    private OpprettMottakereMapFørstegangsoppdrag() {
    }

    static Map<Oppdragsmottaker, List<TilkjentYtelseAndel>> finnMottakereMedDeresAndel(OppdragInput behandlingInfo) {
        Map<Oppdragsmottaker, List<TilkjentYtelseAndel>> andelPrMottakerMap = new LinkedHashMap<>();

        List<TilkjentYtelseAndel> alleTilkjentYtelseAndelerListe = behandlingInfo.getTilkjentYtelseAndelerFomEndringsdato();
        List<TilkjentYtelseAndel> brukersAndelerListe = FinnStatusForMottakere.getAndelerForMottakeren(alleTilkjentYtelseAndelerListe, true);
        List<TilkjentYtelseAndel> alleArbeidsgivereAndelerListe = FinnStatusForMottakere.getAndelerForMottakeren(alleTilkjentYtelseAndelerListe, false);

        List<TilkjentYtelsePeriode> tilkjentYtelsePerioder = behandlingInfo.getTilkjentYtelsePerioderFomEndringsdato();
        List<TilkjentYtelseAndel> andelerForBruker = SlåAndelerMedSammePeriodeOgKlassekodeSammen.slåAndelerMedSammePeriodeOgKlassekodeSammen(
            tilkjentYtelsePerioder, brukersAndelerListe, true);

        if (!andelerForBruker.isEmpty()) {
            Oppdragsmottaker oppdragsmottaker = new Oppdragsmottaker(behandlingInfo.getPersonIdent().getIdent(), true);
            oppdragsmottaker.setStatus(OppdragsmottakerStatus.NY);
            andelPrMottakerMap.put(oppdragsmottaker, andelerForBruker);
        }
        Map<Oppdragsmottaker, List<TilkjentYtelseAndel>> arbeidsgiverAndelerPerOrgnrMap = finnArbeidsgiverAndelerPerOrgnr(alleArbeidsgivereAndelerListe, tilkjentYtelsePerioder);
        andelPrMottakerMap.putAll(arbeidsgiverAndelerPerOrgnrMap);

        return andelPrMottakerMap;
    }

    private static Map<Oppdragsmottaker, List<TilkjentYtelseAndel>> finnArbeidsgiverAndelerPerOrgnr(List<TilkjentYtelseAndel> arbeidsgiversAndelListe, List<TilkjentYtelsePeriode> tilkjentYtelsePeriodeListe) {

        Map<Oppdragsmottaker, List<TilkjentYtelseAndel>> arbeidsgiverAndelerPerOrgnrMap = new LinkedHashMap<>();
        Map<String, List<TilkjentYtelseAndel>> groupedByOrgnr = grupperOppdragAndelerMedOrgnr(arbeidsgiversAndelListe);

        for (Map.Entry<String, List<TilkjentYtelseAndel>> entry : groupedByOrgnr.entrySet()) {
            List<TilkjentYtelseAndel> andelerListeForDenneArbeidsgiveren = entry.getValue();
            List<TilkjentYtelseAndel> fastsattAndelerForDenneArbeidsgiveren = SlåAndelerMedSammePeriodeOgKlassekodeSammen.slåAndelerMedSammePeriodeOgKlassekodeSammen(tilkjentYtelsePeriodeListe,
                andelerListeForDenneArbeidsgiveren, false);
            Oppdragsmottaker mottaker = new Oppdragsmottaker(entry.getKey(), false);
            mottaker.setStatus(OppdragsmottakerStatus.NY);
            arbeidsgiverAndelerPerOrgnrMap.put(mottaker, fastsattAndelerForDenneArbeidsgiveren);
        }
        return arbeidsgiverAndelerPerOrgnrMap;
    }

    private static Map<String, List<TilkjentYtelseAndel>> grupperOppdragAndelerMedOrgnr(List<TilkjentYtelseAndel> arbeidsgiversAndelListe) {
        return arbeidsgiversAndelListe.stream()
            .collect(Collectors.groupingBy(
                TilkjentYtelseAndel::getArbeidsforholdOrgnr,
                LinkedHashMap::new,
                Collectors.mapping(Function.identity(), Collectors.toList())));
    }
}
