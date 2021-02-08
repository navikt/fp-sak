package no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdragslinje150;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.økonomistøtte.Oppdragsmottaker;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.FinnMottakerInfoITilkjentYtelse;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.KlassekodeUtleder;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.OppdragskontrollConstants;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.TidligereOppdragTjeneste;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.VurderFeriepengerBeregning;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.fp.OppdragInput;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.wrapper.OppdragsmottakerInfo;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.wrapper.TilkjentYtelse;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.wrapper.TilkjentYtelseAndel;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.wrapper.TilkjentYtelsePeriode;

public class OpprettOppdragslinje150Tjeneste {

    private static final int INITIAL_TELLER = 100;
    private static final int INITIAL_COUNT = 0;

    private OpprettOppdragslinje150Tjeneste() {
        // skjul default constructor
    }

    public static List<Oppdragslinje150> opprettOppdragslinje150(OppdragInput oppdragInput, Oppdrag110 oppdrag110,
                                                                 List<TilkjentYtelseAndel> andelListe, Oppdragsmottaker mottaker) {
        List<String> klassekodeListe = KlassekodeUtleder.getKlassekodeListe(andelListe);
        if (mottaker.erBruker() && klassekodeListe.size() > 1) {
            List<List<TilkjentYtelseAndel>> andelerGruppertMedKlassekode = gruppereAndelerMedKlassekode(andelListe);
            return opprettOppdr150ForBrukerMedFlereKlassekode(oppdragInput, oppdrag110,
                andelerGruppertMedKlassekode, mottaker, Collections.emptyList());
        }
        return opprettOppdragslinje150(oppdragInput, oppdrag110, andelListe, mottaker, null);
    }

    public static List<Oppdragslinje150> opprettOppdr150ForBrukerMedFlereKlassekode(OppdragInput oppdragInput,
                                                                                    Oppdrag110 oppdrag110,
                                                                                    List<List<TilkjentYtelseAndel>> andelerGruppertMedKlassekode,
                                                                                    Oppdragsmottaker mottaker, List<Oppdragslinje150> tidligereOppdr150Liste) {
        List<Oppdragslinje150> oppdrlinje150Liste = new ArrayList<>();
        int teller = INITIAL_TELLER;
        int count = INITIAL_COUNT;
        for (List<TilkjentYtelseAndel> andelListe : andelerGruppertMedKlassekode) {
            List<Long> delYtelseIdListe = new ArrayList<>();
            for (TilkjentYtelseAndel andel : andelListe) {
                List<Oppdrag110> alleTidligereOppdrag110ForMottaker = finnOpprag110ForGittFagsystemId(oppdragInput, oppdrag110);
                OppdragsmottakerInfo oppdragInfo = new OppdragsmottakerInfo(mottaker, andel, alleTidligereOppdrag110ForMottaker, tidligereOppdr150Liste);

                Oppdragslinje150 oppdragslinje150 = opprettOppdragslinje150FørsteOppdrag(oppdragInput, oppdragInfo, oppdrag110,
                    delYtelseIdListe, count, teller++, andel.getUtbetalingsgrad());
                oppdrlinje150Liste.add(oppdragslinje150);
            }
            count = count + andelListe.size();
        }
        long sisteSattDelYtelseId = oppdrlinje150Liste.get(oppdrlinje150Liste.size() - 1).getDelytelseId();
        if (tidligereOppdr150Liste.isEmpty() || VurderFeriepengerBeregning.erFeriepengerEndret(oppdragInput, tidligereOppdr150Liste, mottaker)) {
            List<Oppdragslinje150> opp150FeriepengerList = OpprettOppdragslinje150FeriepengerTjeneste.lagOppdragslinje150ForFeriepenger(oppdragInput,
                oppdrag110, mottaker, tidligereOppdr150Liste, sisteSattDelYtelseId);
            oppdrlinje150Liste.addAll(opp150FeriepengerList);
        }
        return oppdrlinje150Liste;
    }

    public static List<Oppdragslinje150> opprettOppdragslinje150(OppdragInput oppdragInput, Oppdrag110 nyOppdrag110,
                                                                 List<TilkjentYtelseAndel> andelerListe, Oppdragsmottaker mottaker,
                                                                 Oppdragslinje150 sisteOppdr150) {
        List<Oppdragslinje150> oppdrlinje150Liste = new ArrayList<>();
        List<Long> delYtelseIdListe = new ArrayList<>();

        int teller = INITIAL_TELLER;
        List<Oppdragslinje150> tidligereOppdr150Liste = sisteOppdr150 != null ? Collections.singletonList(sisteOppdr150) : Collections.emptyList();
        for (TilkjentYtelseAndel andel : andelerListe) {
            List<Oppdrag110> alleTidligereOppdrag110ForMottaker = finnOpprag110ForGittFagsystemId(oppdragInput, nyOppdrag110);
            OppdragsmottakerInfo oppdragInfo = new OppdragsmottakerInfo(mottaker, andel, alleTidligereOppdrag110ForMottaker, tidligereOppdr150Liste);
            Oppdragslinje150 oppdragslinje150 = opprettOppdragslinje150FørsteOppdrag(oppdragInput, oppdragInfo, nyOppdrag110, delYtelseIdListe, teller++, andel.getUtbetalingsgrad());
            if (!mottaker.erBruker()) {
                LocalDate maksDato = FinnMottakerInfoITilkjentYtelse.finnSisteDagMedUtbetalingTilMottaker(oppdragInput, mottaker);
                OpprettOppdragsmeldingerRelatertTil150.opprettRefusjonsinfo156(oppdragInput, oppdragslinje150, mottaker, maksDato);
            }
            oppdrlinje150Liste.add(oppdragslinje150);
        }
        long sisteSattDelYtelseId = delYtelseIdListe.get(delYtelseIdListe.size() - 1);
        if (tidligereOppdr150Liste.isEmpty() || VurderFeriepengerBeregning.erFeriepengerEndret(oppdragInput, tidligereOppdr150Liste, mottaker)) {
            List<Oppdragslinje150> opp150FeriepengerList = OpprettOppdragslinje150FeriepengerTjeneste.lagOppdragslinje150ForFeriepenger(oppdragInput,
                nyOppdrag110, mottaker, tidligereOppdr150Liste, sisteSattDelYtelseId);
            OpprettOppdragslinje150FeriepengerTjeneste.kobleAndreMeldingselementerTilOpp150NyFeriepenger(oppdragInput, opp150FeriepengerList, mottaker);
            oppdrlinje150Liste.addAll(opp150FeriepengerList);
        }
        return oppdrlinje150Liste;
    }

    private static Oppdragslinje150 opprettOppdragslinje150FørsteOppdrag(OppdragInput oppdragInput, OppdragsmottakerInfo oppdragInfo, Oppdrag110 nyOppdrag110,
                                                                         List<Long> delYtelseIdListe, int teller, BigDecimal utbetalingsgrad) {
        return opprettOppdragslinje150FørsteOppdrag(oppdragInput, oppdragInfo, nyOppdrag110,
            delYtelseIdListe, INITIAL_COUNT, teller, utbetalingsgrad);
    }

    private static Oppdragslinje150 opprettOppdragslinje150FørsteOppdrag(OppdragInput oppdragInput, OppdragsmottakerInfo oppdragInfo, Oppdrag110 nyOppdrag110,
                                                                         List<Long> delYtelseIdListe, int count, int teller, BigDecimal utbetalingsgrad) {

        Oppdragslinje150.Builder oppdragslinje150Builder = opprettOppdragslinje150Builder(oppdragInput, oppdragInfo, nyOppdrag110, utbetalingsgrad);

        List<Oppdragslinje150> tidligereOppdr150Liste = oppdragInfo.getTidligereOppdr150MottakerListe();
        if (tidligereOppdr150Liste.isEmpty()) {
            UtledDelytelseOgFagsystemIdI150.settRefDelytelseOgFagsystemId(nyOppdrag110, delYtelseIdListe, count, teller, oppdragslinje150Builder);
        } else {
            int antallIter = teller - (INITIAL_TELLER + count);
            UtledDelytelseOgFagsystemIdI150.settRefDelytelseOgFagsystemId(oppdragInfo, nyOppdrag110, oppdragslinje150Builder, delYtelseIdListe, antallIter);
        }
        return oppdragslinje150Builder.build();
    }

    private static Oppdragslinje150.Builder opprettOppdragslinje150Builder(OppdragInput oppdragInput, OppdragsmottakerInfo oppdragInfo, Oppdrag110 oppdrag110, BigDecimal utbetalingsgrad) {
        TilkjentYtelseAndel andel = oppdragInfo.getTilkjentYtelseAndel();
        Oppdragsmottaker mottaker = oppdragInfo.getMottaker();

        LocalDate vedtakFom = andel.getOppdragPeriodeFom();
        LocalDate vedtakTom = andel.getOppdragPeriodeTom();
        String kodeKlassifik = KlassekodeUtleder.utled(andel);
        int dagsats = andel.getDagsats();

        Oppdragslinje150.Builder oppdragslinje150Builder = Oppdragslinje150.builder();
        settFellesFelterIOppdr150(oppdragInput, oppdragslinje150Builder, false, false);
        oppdragslinje150Builder.medKodeKlassifik(kodeKlassifik)
            .medOppdrag110(oppdrag110)
            .medVedtakFomOgTom(vedtakFom, vedtakTom)
            .medSats(dagsats)
            .medUtbetalingsgrad(Utbetalingsgrad.prosent(utbetalingsgrad));
        if (mottaker.erBruker()) {
            oppdragslinje150Builder.medUtbetalesTilId(Oppdragslinje150Util.endreTilElleveSiffer(mottaker.getId()));
        }
        return oppdragslinje150Builder;
    }

    private static List<Oppdrag110> finnOpprag110ForGittFagsystemId(OppdragInput oppdragInput, Oppdrag110 oppdrag110) {
        return oppdragInput.getAlleTidligereOppdrag110()
            .stream()
            .filter(tidligere110 -> tidligere110.getFagsystemId() == oppdrag110.getFagsystemId())
            .collect(Collectors.toList());
    }

    public static List<TilkjentYtelseAndel> hentForrigeTilkjentYtelseAndeler(List<TilkjentYtelsePeriode> tilkjentYtelsePerioder) {
        return tilkjentYtelsePerioder.stream()
            .sorted(Comparator.comparing(TilkjentYtelsePeriode::getFom))
            .map(TilkjentYtelsePeriode::getTilkjentYtelseAndeler)
            .flatMap(List::stream)
            .filter(a -> a.getDagsats() > 0)
            .collect(Collectors.toList());
    }

    public static boolean finnesFlereKlassekodeIForrigeOppdrag(OppdragInput oppdragInput) {
        List<Oppdragslinje150> tidligereOpp150Liste = TidligereOppdragTjeneste.hentTidligereGjeldendeOppdragslinje150(
            oppdragInput, false);

        return tidligereOpp150Liste.stream().map(Oppdragslinje150::getKodeKlassifik).distinct().count() > 1L;
    }

    public static List<List<TilkjentYtelseAndel>> gruppereAndelerMedKlassekode(List<TilkjentYtelseAndel> andelListe) {
        Map<String, List<TilkjentYtelseAndel>> andelPrKlassekodeMap = new LinkedHashMap<>();
        List<String> klassekodeListe = KlassekodeUtleder.getKlassekodeListe(andelListe);
        for (String klassekode : klassekodeListe) {
            List<TilkjentYtelseAndel> andelerMedSammeKlassekode = andelListe.stream()
                .filter(andel -> KlassekodeUtleder.utled(andel).equals(klassekode))
                .sorted(Comparator.comparing(TilkjentYtelseAndel::getOppdragPeriodeFom))
                .collect(Collectors.toList());
            andelPrKlassekodeMap.put(klassekode, andelerMedSammeKlassekode);
        }
        return new ArrayList<>(andelPrKlassekodeMap.values());
    }

    public static void settFellesFelterIOppdr150(OppdragInput oppdragInput, Oppdragslinje150.Builder oppdr150Builder, boolean gjelderOpphør, boolean gjelderFeriepenger) {
        LocalDate vedtaksdato = oppdragInput.getVedtaksdato();
        String kodeEndringLinje = gjelderOpphør ? OppdragskontrollConstants.KODE_ENDRING_LINJE_ENDRING : OppdragskontrollConstants.KODE_ENDRING_LINJE_NY;
        String typeSats = gjelderFeriepenger ? OppdragskontrollConstants.TYPE_SATS_FERIEPENGER : OppdragskontrollConstants.TYPE_SATS_DAG;
        boolean erEndringMedBortfallAvHeleYtelsen = summerHeleTilkjentYtelse(getPerioderForTilkjentYtelse(oppdragInput)) == 0 &&
            summerHeleTilkjentYtelse(oppdragInput.getForrigeTilkjentYtelsePerioder()) > 0;
        if (gjelderOpphør || erEndringMedBortfallAvHeleYtelsen) {
            oppdr150Builder.medKodeStatusLinje(OppdragskontrollConstants.KODE_STATUS_LINJE_OPPHØR);
        }
        oppdr150Builder.medKodeEndringLinje(kodeEndringLinje)
            .medVedtakId(vedtaksdato.toString())
            .medHenvisning(oppdragInput.getBehandlingId())
            .medTypeSats(typeSats);
    }

    private static List<TilkjentYtelsePeriode> getPerioderForTilkjentYtelse(OppdragInput oppdragInput) {
        return oppdragInput.getTilkjentYtelse().map(TilkjentYtelse::getTilkjentYtelsePerioder).orElse(Collections.emptyList());
    }

    private static int summerHeleTilkjentYtelse(List<TilkjentYtelsePeriode> perioder) {
        return perioder.stream()
            .flatMap(p -> p.getTilkjentYtelseAndeler().stream())
            .map(TilkjentYtelseAndel::getDagsats)
            .reduce(Integer::sum).orElse(0);
    }

}
