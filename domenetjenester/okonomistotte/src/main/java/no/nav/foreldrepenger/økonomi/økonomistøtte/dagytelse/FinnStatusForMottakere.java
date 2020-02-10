package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.OppdragsmottakerStatus;
import no.nav.foreldrepenger.økonomi.økonomistøtte.Oppdragsmottaker;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.OppdragInput;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.oppdragslinje150.Oppdragslinje150Util;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelseAndel;

public class FinnStatusForMottakere {

    private FinnStatusForMottakere() {
    }

    public static List<Oppdragsmottaker> finnStatusForMottakere(OppdragInput behandlingInfo, List<TilkjentYtelseAndel> andelerOriginal) {
        List<Oppdragsmottaker> oppdragsmottakerList = new ArrayList<>();
        List<TilkjentYtelseAndel> andelerRevurdering = behandlingInfo.getTilkjentYtelseAndelerFomEndringsdato();

        boolean finnesOppdragForBrukerFraFør = TidligereOppdragTjeneste.finnesOppdrag110ForBruker(behandlingInfo);
        OppdragsmottakerStatus statusBruker = finnStatusForMottakerBruker(behandlingInfo, andelerOriginal, andelerRevurdering, finnesOppdragForBrukerFraFør);

        Oppdragsmottaker mottakerBruker = new Oppdragsmottaker(behandlingInfo.getPersonIdent().getIdent(), true);
        mottakerBruker.setStatus(statusBruker);
        oppdragsmottakerList.add(mottakerBruker);

        List<Oppdragsmottaker> arbeidsgiverList = FinnStatusForMottakere.fastsettMottakerStatusForArbeidsgiver(behandlingInfo, andelerOriginal, andelerRevurdering);
        oppdragsmottakerList.addAll(arbeidsgiverList);

        return oppdragsmottakerList;
    }

    public static OppdragsmottakerStatus finnStatusForMottakerBruker(OppdragInput behandlingInfo, List<TilkjentYtelseAndel> andelerOriginal, List<TilkjentYtelseAndel> andelerRevurdering, boolean finnesOppdragFraFør) {

        boolean erBrukerEllerPrivatpersonMottakerIForrigeTY = andelerOriginal.stream()
            .anyMatch(TilkjentYtelseAndel::skalTilBrukerEllerPrivatperson);
        boolean erBrukerEllerPrivatpersonMottakerINyTY = andelerRevurdering.stream()
            .anyMatch(TilkjentYtelseAndel::skalTilBrukerEllerPrivatperson);

        return finnStatus(behandlingInfo, finnesOppdragFraFør, erBrukerEllerPrivatpersonMottakerIForrigeTY, erBrukerEllerPrivatpersonMottakerINyTY);
    }

    public static List<Oppdragsmottaker> finnStatusForMottakerArbeidsgiver(OppdragInput behandlingInfo, List<TilkjentYtelseAndel> andelerOriginal,
                                                                           List<TilkjentYtelseAndel> andelerRevurdering, List<String> orgnrListeFraTidligereOppdrag) {

        List<String> orgnrFraOriginalBehandlingListe = andelerOriginal.stream()
            .filter(andel -> !andel.skalTilBrukerEllerPrivatperson())
            .map(TilkjentYtelseAndel::getArbeidsforholdOrgnr)
            .distinct()
            .collect(Collectors.toList());

        return grupperArbeidsgivere(behandlingInfo, orgnrFraOriginalBehandlingListe,
            andelerRevurdering, orgnrListeFraTidligereOppdrag);
    }

    public static Oppdragsmottaker fastsettMottakerStatusForBruker(OppdragInput behandlingInfo, List<TilkjentYtelseAndel> andelerOriginal,
                                                                   List<TilkjentYtelseAndel> andelerFomEndringsdatoListe) {

        Oppdragsmottaker mottakerBruker = new Oppdragsmottaker(behandlingInfo.getPersonIdent().getIdent(), true);

        boolean finnesOppdragFraFør = TidligereOppdragTjeneste.finnesOppdrag110ForBruker(behandlingInfo);
        OppdragsmottakerStatus brukerStatus = FinnStatusForMottakere.finnStatusForMottakerBruker(behandlingInfo, andelerOriginal, andelerFomEndringsdatoListe, finnesOppdragFraFør);
        mottakerBruker.setStatus(brukerStatus);

        return mottakerBruker;
    }

    public static List<Oppdragsmottaker> fastsettMottakerStatusForArbeidsgiver(OppdragInput behandlingInfo, List<TilkjentYtelseAndel> andelerOriginal,
                                                                               List<TilkjentYtelseAndel> arbeidsgiversAndelerListe) {

        List<String> orgnrListeFraTidligereOppdrag = TidligereOppdragTjeneste.finnRefunderesIdITidligereOppdrag(behandlingInfo);

        return FinnStatusForMottakere.finnStatusForMottakerArbeidsgiver(behandlingInfo, andelerOriginal,
            arbeidsgiversAndelerListe, orgnrListeFraTidligereOppdrag);
    }

    private static OppdragsmottakerStatus finnStatus(OppdragInput behandlingInfo,
                                                     boolean finnesOppdragFraFør,
                                                     boolean erBrukerEllerPrivatpersonMottakerIForrigeTY,
                                                     boolean erBrukerEllerPrivatpersonMottakerIEndretTY) {
        if (erBrukerEllerPrivatpersonMottakerIForrigeTY) {
            if (erBrukerEllerPrivatpersonMottakerIEndretTY) {
                return OppdragsmottakerStatus.ENDR;
            } else {
                return finnStatusNårMottakerFinnesIForrigeTilkjentYtelse(behandlingInfo);
            }
        } else {
            return finnStatusNårMottakerIkkeFinnesIForrigeTilkjentYtelse(behandlingInfo, finnesOppdragFraFør);
        }
    }

    private static OppdragsmottakerStatus finnStatusNårMottakerIkkeFinnesIForrigeTilkjentYtelse(OppdragInput behandlingInfo, boolean finnesOppdragFraFør) {
        boolean erMottaker = FinnMottakerInfoITilkjentYtelse.erBrukerMottakerITilkjentYtelse(behandlingInfo);
        if (!erMottaker) {
            return OppdragsmottakerStatus.IKKE_MOTTAKER;
        }
        return finnesOppdragFraFør ? OppdragsmottakerStatus.ENDR : OppdragsmottakerStatus.NY;
    }

    private static OppdragsmottakerStatus finnStatusNårMottakerFinnesIForrigeTilkjentYtelse(OppdragInput behandlingInfo) {
        boolean erMottakerFørEndringsdato = FinnMottakerInfoITilkjentYtelse.erBrukerMottakerITilkjentYtelse(behandlingInfo);
        return erMottakerFørEndringsdato ? OppdragsmottakerStatus.UENDR : OppdragsmottakerStatus.OPPH;
    }

    private static List<String> finnArbeidsgiverSomIkkeErMottakerLenger(List<String> orgnrFraNyBehandlingListe, List<String> orgnrFraOriginalBehandlingListe) {
        List<String> fjernetOrgnrIRevurderingListe = new ArrayList<>();
        for (String orgnrOriginal : orgnrFraOriginalBehandlingListe) {
            boolean finnesIkkeIRevurdering = orgnrFraNyBehandlingListe
                .stream()
                .noneMatch(orgnrINyBehandling -> orgnrINyBehandling.equals(orgnrOriginal));
            if (finnesIkkeIRevurdering) {
                fjernetOrgnrIRevurderingListe.add(orgnrOriginal);
            }
        }
        return fjernetOrgnrIRevurderingListe;
    }

    private static List<String> getOrngrITilkjentYtelseFomEndringsdato(List<TilkjentYtelseAndel> andelerRevurdering) {
        return andelerRevurdering.stream()
            .filter(andel -> !andel.skalTilBrukerEllerPrivatperson())
            .map(TilkjentYtelseAndel::getArbeidsforholdOrgnr)
            .distinct()
            .collect(Collectors.toList());
    }

    private static List<Oppdragsmottaker> fastsettStatusForArbeidsgiver(List<String> orgnrFraOriginalBehandlingListe, List<String> alleOrgnrINyTilkjentYtelseListe,
                                                                        List<String> orgnrFomEndringsdatoListe, List<String> orgnrListeFraTidligereOppdrag) {

        List<Oppdragsmottaker> oppdragsmottakerListe = new ArrayList<>();

        for (String orgnrIRevurdering : alleOrgnrINyTilkjentYtelseListe) {
            boolean erArbeidsgiverMottakerIForrigeTilkjentYtelse = orgnrFraOriginalBehandlingListe.contains(orgnrIRevurdering);
            boolean erArbeidsgiverMottakerIEndretTilkjentYtelse = orgnrFomEndringsdatoListe.contains(orgnrIRevurdering);
            boolean finnesOppdragFraFør = orgnrListeFraTidligereOppdrag.contains(Oppdragslinje150Util.endreTilElleveSiffer(orgnrIRevurdering));

            Oppdragsmottaker mottaker = new Oppdragsmottaker(orgnrIRevurdering, false);
            OppdragsmottakerStatus status = finnStatusArbeidsgiver(erArbeidsgiverMottakerIForrigeTilkjentYtelse,
                erArbeidsgiverMottakerIEndretTilkjentYtelse, finnesOppdragFraFør);
            mottaker.setStatus(status);

            oppdragsmottakerListe.add(mottaker);
        }
        return oppdragsmottakerListe;
    }

    private static OppdragsmottakerStatus finnStatusArbeidsgiver(boolean erArbeidsgiverMottakerIForrigeTilkjentYtelse, boolean erArbeidsgiverMottakerIEndretTilkjentYtelse,
                                                                 boolean finnesOppdragFraFør) {
        if (erArbeidsgiverMottakerIForrigeTilkjentYtelse) {
            if (erArbeidsgiverMottakerIEndretTilkjentYtelse) {
                return OppdragsmottakerStatus.ENDR;
            } else {
                return OppdragsmottakerStatus.UENDR;
            }
        } else {
            if (finnesOppdragFraFør) {
                return OppdragsmottakerStatus.ENDR;
            } else {
                return OppdragsmottakerStatus.NY;
            }
        }
    }

    public static List<TilkjentYtelseAndel> getAndelerForMottakeren(List<TilkjentYtelseAndel> alleAndelersListe, boolean erBruker) {
        return alleAndelersListe.stream()
            .filter(andel -> erBruker == andel.skalTilBrukerEllerPrivatperson())
            .collect(Collectors.toList());
    }

    private static List<Oppdragsmottaker> grupperArbeidsgivere(OppdragInput behandlingInfo, List<String> orgnrFraOriginalBehandlingListe,
                                                               List<TilkjentYtelseAndel> andelerRevurdering, List<String> orgnrListeFraTidligereOppdrag) {

        List<Oppdragsmottaker> orngrForArbeidsgiverSomSkalOpphøre = new ArrayList<>();

        List<String> alleOrgnrINyTilkjentYtelseListe = FinnMottakerInfoITilkjentYtelse.finnOrgnrForArbeidsgivereITilkjentYtelse(behandlingInfo);
        List<String> fjernetOrgnrIRevurderingListe = finnArbeidsgiverSomIkkeErMottakerLenger(alleOrgnrINyTilkjentYtelseListe, orgnrFraOriginalBehandlingListe);
        List<String> orgnrFomEndringsdatoListe = getOrngrITilkjentYtelseFomEndringsdato(andelerRevurdering);

        fjernetOrgnrIRevurderingListe.forEach(orgnr -> {
            Oppdragsmottaker oppdragsmottaker = new Oppdragsmottaker(orgnr, false);
            oppdragsmottaker.setStatus(OppdragsmottakerStatus.OPPH);
            orngrForArbeidsgiverSomSkalOpphøre.add(oppdragsmottaker);
        });

        List<Oppdragsmottaker> oppdragsmottakerList = fastsettStatusForArbeidsgiver(orgnrFraOriginalBehandlingListe, alleOrgnrINyTilkjentYtelseListe,
            orgnrFomEndringsdatoListe, orgnrListeFraTidligereOppdrag);
        oppdragsmottakerList.addAll(orngrForArbeidsgiverSomSkalOpphøre);

        return oppdragsmottakerList;
    }
}
