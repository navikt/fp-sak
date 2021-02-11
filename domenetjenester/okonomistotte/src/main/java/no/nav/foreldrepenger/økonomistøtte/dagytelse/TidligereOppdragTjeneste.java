package no.nav.foreldrepenger.økonomistøtte.dagytelse;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.økonomistøtte.Oppdragsmottaker;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.fp.OppdragInput;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdrag110.KodeFagområdeTjeneste;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdrag110.KodeFagområdeTjenesteProvider;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdragslinje150.Oppdragslinje150Util;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.opphør.FinnOppdragslinje150FomSisteOpphør;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.wrapper.OppdragsmottakerInfo;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.wrapper.TilkjentYtelseAndel;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.wrapper.TilkjentYtelsePeriode;

public class TidligereOppdragTjeneste {

    private TidligereOppdragTjeneste() {
    }

    public static Optional<Oppdragslinje150> finnSisteLinjeIKjedeForBruker(OppdragInput behandlingInfo) {
        List<Oppdragslinje150> tidligereOpp150Liste = hentTidligereGjeldendeOppdragslinje150(behandlingInfo, false);

        return tidligereOpp150Liste.stream()
            .max(Comparator.comparing(Oppdragslinje150::getDelytelseId).thenComparing(Oppdragslinje150::getKodeStatusLinje, Comparator.nullsFirst(Comparator.naturalOrder())));
    }

    public static List<Oppdragslinje150> finnSisteLinjeKjedeForAlleArbeidsgivere(OppdragInput behandlingInfo) {

        List<Oppdragslinje150> sisteLinjeKjedeForAlleArbeidsgivereListe = new ArrayList<>();
        List<Oppdragslinje150> oppdrag150ForAlleArbeidsgivere = getAlleTidligereOppdr150ForArbeidsgivere(behandlingInfo);

        List<String> refunderesIdListe = finnRefunderesIdITidligereOppdragslinje150(oppdrag150ForAlleArbeidsgivere);

        for (String refunderesId : refunderesIdListe) {
            List<Oppdragslinje150> tidligereOpp150linjeListe = hentTidligereGjeldendeOppdr150ForMottakeren(behandlingInfo,
                new Oppdragsmottaker(refunderesId, false), false);
            sisteLinjeKjedeForAlleArbeidsgivereListe.addAll(tidligereOpp150linjeListe);
        }
        return sisteLinjeKjedeForAlleArbeidsgivereListe;
    }

    public static List<Oppdragslinje150> hentTidligereGjeldendeOppdragslinje150(OppdragInput behandlingInfo, boolean medFeriepenger) {
        String id = behandlingInfo.getPersonIdent().getIdent();
        return hentTidligereGjeldendeOppdr150ForMottakeren(behandlingInfo, new Oppdragsmottaker(id, true), medFeriepenger);
    }

    private static List<Oppdragslinje150> hentTidligereGjeldendeOppdr150ForMottakeren(OppdragInput behandlingInfo,
                                                                                      Oppdragsmottaker mottaker, boolean medFeriepenger) {

        List<Oppdragslinje150> alleOppdr150ListeForDenneMottakeren = hentAlleTidligereOppdr150ForMottakerenListe(behandlingInfo, mottaker);
        List<Oppdragslinje150> alleOppdr150UtenFeriepengerListeForDenneMottakeren = getOppdragslinje150UtenFeriepenger(alleOppdr150ListeForDenneMottakeren);
        List<Oppdragslinje150> gjeldendeOppdr150ListeForMottakeren = getGjeldendeOppdragslinje150(alleOppdr150UtenFeriepengerListeForDenneMottakeren);
        if (!medFeriepenger) {
            return sortOppdragslinje150Liste(gjeldendeOppdr150ListeForMottakeren);
        }
        List<Oppdragslinje150> opp150FeriepengerListe = Oppdragslinje150Util.getOpp150ForFeriepengerMedKlassekode(alleOppdr150ListeForDenneMottakeren);
        gjeldendeOppdr150ListeForMottakeren.addAll(opp150FeriepengerListe);

        return sortOppdragslinje150Liste(gjeldendeOppdr150ListeForMottakeren);
    }

    public static List<Oppdragslinje150> hentAlleTidligereOppdragslinje150(OppdragInput behandlingInfo, Oppdrag110 forrigeOppdrag110) {
        return behandlingInfo.getAlleTidligereOppdrag110().stream()
            .filter(oppdrag110 -> oppdrag110.getFagsystemId() == forrigeOppdrag110.getFagsystemId())
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .collect(Collectors.toList());
    }

    public static List<Oppdragslinje150> hentAlleTidligereOppdr150ForFeriepenger(OppdragInput behandlingInfo, List<Oppdragslinje150> tidligereOppdr150MottakerListe) {
        if (tidligereOppdr150MottakerListe.isEmpty()) {
            return Collections.emptyList();
        }
        Oppdragslinje150 sisteOppdr150 = tidligereOppdr150MottakerListe.get(0);
        Oppdrag110 sisteOppdr110 = sisteOppdr150.getOppdrag110();
        List<Oppdragslinje150> alleTidligereOppdr150 = hentAlleTidligereOppdragslinje150(behandlingInfo, sisteOppdr110);
        return Oppdragslinje150Util.getOpp150ForFeriepengerMedKlassekode(alleTidligereOppdr150);
    }

    public static List<TilkjentYtelseAndel> finnAndelerIOppdragPerioder(Oppdragsmottaker mottaker, List<TilkjentYtelsePeriode> forrigeTilkjentYtelsePeriodeListe) {
        List<TilkjentYtelseAndel> tilkjentYtelseAndelerForMottaker = forrigeTilkjentYtelsePeriodeListe.stream()
            .flatMap(oppdragPeriode -> oppdragPeriode.getTilkjentYtelseAndeler().stream())
            .filter(andel -> mottaker.erBruker() == andel.skalTilBrukerEllerPrivatperson())
            .filter(andel -> andel.getDagsats() > 0)
            .collect(Collectors.toList());
        if (mottaker.erBruker()) {
            return tilkjentYtelseAndelerForMottaker;
        }
        return tilkjentYtelseAndelerForMottaker.stream()
            .filter(andel -> andel.getArbeidsforholdOrgnr().equals(mottaker.getOrgnr()))
            .collect(Collectors.toList());
    }

    public static long finnMaxDelytelseIdITidligereOppdragForMottakeren(OppdragsmottakerInfo oppdragInfo) {
        return oppdragInfo.getTidligereOpp110MottakerListe().stream()
            .flatMap(opp110 -> opp110.getOppdragslinje150Liste().stream())
            .map(Oppdragslinje150::getDelytelseId)
            .max(Comparator.comparing(Function.identity()))
            .orElseThrow(() -> new IllegalStateException("Utvikler feil: Mangler delytelseId"));
    }

    public static long finnMaxDelytelseIdForEnOppdrag110(Oppdrag110 nyOppdrag110, Oppdragslinje150 sisteOppdr150) {
        List<Oppdragslinje150> opp150UtenFeriepengerListe = getOppdragslinje150UtenFeriepenger(nyOppdrag110);
        Optional<Oppdragslinje150> sisteOpprettetOpp150ForDenneOpp110 = opp150UtenFeriepengerListe.stream()
            .max(Comparator.comparing(Oppdragslinje150::getDelytelseId).thenComparing(Oppdragslinje150::getKodeStatusLinje, Comparator.nullsFirst(Comparator.naturalOrder())));

        return sisteOpprettetOpp150ForDenneOpp110.map(Oppdragslinje150::getDelytelseId)
            .orElseGet(sisteOppdr150::getDelytelseId);
    }

    public static List<Oppdragslinje150> getOppdragslinje150ForOpphør(Oppdrag110 nyOppdrag110) {
        if (nyOppdrag110 == null) {
            return Collections.emptyList();
        }
        return nyOppdrag110.getOppdragslinje150Liste()
            .stream()
            .filter(Oppdragslinje150::gjelderOpphør)
            .collect(Collectors.toList());
    }

    static boolean finnesOppdrag110ForBruker(OppdragInput behandlingInfo) {
        KodeFagområdeTjeneste fagområdeTjeneste = KodeFagområdeTjenesteProvider.getKodeFagområdeTjeneste(behandlingInfo);
        return behandlingInfo.getAlleTidligereOppdrag110()
            .stream()
            .anyMatch(fagområdeTjeneste::gjelderBruker);
    }

    private static List<Oppdragslinje150> getOppdragslinje150UtenFeriepenger(List<Oppdragslinje150> alleOppdr150Liste) {
        return alleOppdr150Liste.stream()
            .filter(TidligereOppdragTjeneste::erIkkeFeriepenger)
            .collect(Collectors.toList());
    }

    private static boolean erIkkeFeriepenger(Oppdragslinje150 oppdragslinje150) {
        return !oppdragslinje150.getKodeKlassifik().gjelderFeriepenger();
    }

    private static List<Oppdragslinje150> sortOppdragslinje150Liste(List<Oppdragslinje150> oppdragslinje150Liste) {
        return oppdragslinje150Liste.stream()
            .sorted(Comparator.comparing(Oppdragslinje150::getDelytelseId).thenComparing(Oppdragslinje150::getKodeStatusLinje, Comparator.nullsFirst(Comparator.naturalOrder())))
            .collect(Collectors.toList());
    }

    private static List<Oppdragslinje150> hentAlleTidligereOppdr150ForMottakerenListe(OppdragInput behandlingInfo, Oppdragsmottaker mottaker) {
        KodeFagområdeTjeneste fagområdeTjeneste = finnFagområdeTjeneste(behandlingInfo);
        String økonomiKodeFagområde = fagområdeTjeneste.finn(mottaker.erBruker());
        List<Oppdragslinje150> oppdragslinje150Liste = behandlingInfo.getAlleTidligereOppdrag110().stream()
            .filter(oppdrag110 -> oppdrag110.getKodeFagomrade().equals(økonomiKodeFagområde))
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .collect(Collectors.toList());
        if (mottaker.erBruker()) {
            return oppdragslinje150Liste;
        }
        return oppdragslinje150Liste.stream()
            .filter(opp150 -> opp150.getRefusjonsinfo156().getRefunderesId().equals(Oppdragslinje150Util.endreTilElleveSiffer(mottaker.getId())))
            .collect(Collectors.toList());
    }

    private static KodeFagområdeTjeneste finnFagområdeTjeneste(OppdragInput behandlingInfo) {
        FagsakYtelseType ytelseType = behandlingInfo.getFagsakYtelseType();
        if (FagsakYtelseType.FORELDREPENGER.equals(ytelseType)) {
            return KodeFagområdeTjeneste.forForeldrepenger();
        }
        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(ytelseType)) {
            return KodeFagområdeTjeneste.forSvangerskapspenger();
        }
        throw new IllegalArgumentException("Utvikler-feil: FagsakYtelseType " + ytelseType + " er ikke støttet her");
    }

    private static List<Oppdragslinje150> getGjeldendeOppdragslinje150(List<Oppdragslinje150> alleOppdr150Liste) {

        List<Oppdragslinje150> gjeldendeOppdr150Liste = new ArrayList<>();
        Map<KodeKlassifik, List<Oppdragslinje150>> opp150PrKlassekodeMap = alleOppdr150Liste.stream()
            .collect(Collectors.groupingBy(
                Oppdragslinje150::getKodeKlassifik,
                LinkedHashMap::new,
                Collectors.mapping(Function.identity(), Collectors.toList())));

        for (Map.Entry<KodeKlassifik, List<Oppdragslinje150>> entry : opp150PrKlassekodeMap.entrySet()) {
            List<Oppdragslinje150> oppdragslinje150Liste = finnGjeldendeOppdragslinje150Liste(entry.getValue());
            gjeldendeOppdr150Liste.addAll(oppdragslinje150Liste);
        }
        return gjeldendeOppdr150Liste;
    }

    private static List<Oppdragslinje150> finnGjeldendeOppdragslinje150Liste(List<Oppdragslinje150> tidligereOpp150MedSammeKlassekodeListe) {

        boolean finnesIngenOpphør = tidligereOpp150MedSammeKlassekodeListe.stream().noneMatch(Oppdragslinje150::gjelderOpphør);
        if (finnesIngenOpphør) {
            return tidligereOpp150MedSammeKlassekodeListe;
        }
        return FinnOppdragslinje150FomSisteOpphør.finnOppdragslinje150FomSisteOpphør(tidligereOpp150MedSammeKlassekodeListe);
    }

    private static List<Oppdragslinje150> getOppdragslinje150UtenFeriepenger(Oppdrag110 oppdrag110) {
        return oppdrag110.getOppdragslinje150Liste().stream()
            .filter(TidligereOppdragTjeneste::erIkkeFeriepenger)
            .collect(Collectors.toList());
    }

    private static List<Oppdragslinje150> getAlleTidligereOppdr150ForArbeidsgivere(OppdragInput behandlingInfo) {
        return behandlingInfo.getAlleTidligereOppdrag110().stream()
            .filter(oppdrag110 -> ØkonomiKodeFagområde.gjelderRefusjonTilArbeidsgiver(oppdrag110.getKodeFagomrade()))
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .filter(TidligereOppdragTjeneste::erIkkeFeriepenger)
            .collect(Collectors.toList());
    }

    static List<String> finnRefunderesIdITidligereOppdrag(OppdragInput behandlingInfo) {
        return behandlingInfo.getAlleTidligereOppdrag110().stream()
            .filter(oppdrag110 -> ØkonomiKodeFagområde.gjelderRefusjonTilArbeidsgiver(oppdrag110.getKodeFagomrade()))
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .map(Oppdragslinje150::getRefusjonsinfo156)
            .map(Refusjonsinfo156::getRefunderesId)
            .distinct()
            .collect(Collectors.toList());
    }

    private static List<String> finnRefunderesIdITidligereOppdragslinje150(List<Oppdragslinje150> arbeidsgiversOpp150Liste) {
        return arbeidsgiversOpp150Liste.stream()
            .map(opp150 -> opp150.getRefusjonsinfo156().getRefunderesId())
            .distinct()
            .collect(Collectors.toList());
    }

    public static Map<String, List<Oppdragslinje150>> grupperOppdragslinje150MedOrgnr(List<Oppdragslinje150> arbeidsgiversOpp150Liste) {
        return arbeidsgiversOpp150Liste.stream()
            .collect(Collectors.groupingBy(oppdragslinje150 -> oppdragslinje150.getRefusjonsinfo156().getRefunderesId(),
                LinkedHashMap::new,
                Collectors.mapping(Function.identity(), Collectors.toList())));
    }

    public static boolean erEndringsdatoEtterSisteTomDatoAvAlleTidligereOppdrag(OppdragInput behandlingInfo) {
        Optional<LocalDate> endringsdatoOpt = behandlingInfo.getEndringsdato();
        if (endringsdatoOpt.isPresent()) {
            LocalDate endringsdato = endringsdatoOpt.get();
            List<Oppdragslinje150> tidligereOppdr150Liste = hentTidligereGjeldendeOppdragslinje150(behandlingInfo,
                true);
            LocalDate sisteDato = tidligereOppdr150Liste.stream()
                .map(Oppdragslinje150::getDatoVedtakTom)
                .max(Comparator.comparing(Function.identity()))
                .orElseThrow(() -> new IllegalStateException("Utvikler feil: Mangler dato vedtak tom"));
            return endringsdato.isAfter(sisteDato);
        }
        return false;
    }

    public static boolean erEndringsdatoEtterSisteDatoAvAlleTidligereOppdrag(OppdragInput oppdragInput,
                                                                             Oppdragslinje150 sisteOppdr150, Oppdragsmottaker mottaker) {
        Optional<LocalDate> endringsdatoOpt = oppdragInput.getEndringsdato();
        if (endringsdatoOpt.isPresent()) {
            LocalDate endringsdato = endringsdatoOpt.get();
            List<Oppdragslinje150> tidligereOppdr150MedFeriepengerListe = hentTidligereGjeldendeOppdr150ForMottakeren(oppdragInput,
                mottaker, true);
            String refunderesId = sisteOppdr150.getRefusjonsinfo156().getRefunderesId();
            Optional<LocalDate> sisteVedtakTomDato = tidligereOppdr150MedFeriepengerListe.stream()
                .filter(opp150 -> opp150.getRefusjonsinfo156().getRefunderesId().equals(refunderesId))
                .map(Oppdragslinje150::getDatoVedtakTom)
                .max(Comparator.comparing(Function.identity()));
            return sisteVedtakTomDato.map(endringsdato::isAfter).orElse(true);
        }
        return false;
    }
}
