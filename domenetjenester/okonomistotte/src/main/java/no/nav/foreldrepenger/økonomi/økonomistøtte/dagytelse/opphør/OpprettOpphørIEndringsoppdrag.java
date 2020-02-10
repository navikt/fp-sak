package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.opphør;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.økonomi.økonomistøtte.Oppdragsmottaker;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OpprettOppdragTjeneste;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.FinnStatusForMottakere;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.TidligereOppdragTjeneste;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.OppdragInput;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.oppdrag110.OpprettOppdrag110Tjeneste;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.oppdragslinje150.Oppdragslinje150Util;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.oppdragslinje150.OpprettOppdragslinje150Tjeneste;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.oppdragslinje150.OpprettOppdragsmeldingerRelatertTil150;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelseAndel;

@ApplicationScoped
public class OpprettOpphørIEndringsoppdrag {

    private OppdragskontrollOpphør oppdragskontrollOpphør;

    OpprettOpphørIEndringsoppdrag() {
        // For CDI
    }

    @Inject
    public OpprettOpphørIEndringsoppdrag(OppdragskontrollOpphør oppdragskontrollOpphør) {
        this.oppdragskontrollOpphør = oppdragskontrollOpphør;
    }

    public void lagOppdragForMottakereSomSkalOpphøre(OppdragInput behandlingInfo, Oppdragskontroll oppdragskontroll, List<TilkjentYtelseAndel> forrigeTilkjentYtelseAndeler) {
        List<Oppdragsmottaker> oppdragsmottakerList = FinnStatusForMottakere.finnStatusForMottakere(behandlingInfo, forrigeTilkjentYtelseAndeler);
        List<Oppdragsmottaker> ikkeMottakerLengerList = oppdragsmottakerList.stream()
            .filter(mottaker -> mottaker.getStatus() != null)
            .filter(Oppdragsmottaker::erStatusOpphør)
            .collect(Collectors.toList());
        for (Oppdragsmottaker mottaker : ikkeMottakerLengerList) {
            if (mottaker.erBruker()) {
                lagOpphørsoppdragForBruker(behandlingInfo, oppdragskontroll, mottaker);
            } else {
                lagOpphørsoppdragForArbeidsgiver(behandlingInfo, oppdragskontroll, mottaker);
            }
        }
    }

    private void lagOpphørsoppdragForBruker(OppdragInput behandlingInfo, Oppdragskontroll oppdragskontroll, Oppdragsmottaker mottaker) {

        boolean erDetFlereInntektskategoriBruker = OpprettOppdragslinje150Tjeneste.finnesFlereKlassekodeIForrigeOppdrag(behandlingInfo);
        if (erDetFlereInntektskategoriBruker) {
            opprettOpphørsoppdragForBrukerMedFlereKlassekode(behandlingInfo, oppdragskontroll, mottaker, false);
        } else {
            opprettOpphørIEndringsoppdragBruker(behandlingInfo, oppdragskontroll, mottaker, false);
        }
    }

    public Optional<Oppdrag110> opprettOpphørsoppdragForBrukerMedFlereKlassekode(OppdragInput behandlingInfo, Oppdragskontroll nyOppdragskontroll,
                                                                                 Oppdragsmottaker mottaker, boolean opphFørEndringsoppdrFeriepg) {
        return oppdragskontrollOpphør.opprettOpphørsoppdragForBrukerMedFlereKlassekode(behandlingInfo,
            nyOppdragskontroll, mottaker, opphFørEndringsoppdrFeriepg);
    }

    public Optional<Oppdrag110> opprettOpphørIEndringsoppdragBruker(OppdragInput behandlingInfo, Oppdragskontroll oppdragskontroll,
                                                                    Oppdragsmottaker mottaker, boolean opphFørEndringsoppdrFeriepg) {
        if (TidligereOppdragTjeneste.erEndringsdatoEtterSisteTomDatoAvAlleTidligereOppdrag(behandlingInfo)) {
            return Optional.empty();
        }
        Optional<Oppdrag110> nyOppdrag110Opt = opprettOppdrag110Og150ForOpphørBruker(behandlingInfo, oppdragskontroll, mottaker, opphFørEndringsoppdrFeriepg);
        if (!nyOppdrag110Opt.isPresent()) {
            return Optional.empty();
        }
        List<Oppdragslinje150> opp150OpphList = TidligereOppdragTjeneste.getOppdragslinje150ForOpphør(nyOppdrag110Opt.get());
        OpprettOppdragsmeldingerRelatertTil150.opprettAttestant180(opp150OpphList, behandlingInfo.getAnsvarligSaksbehandler());

        return nyOppdrag110Opt;
    }

    private Optional<Oppdrag110> opprettOppdrag110Og150ForOpphørBruker(OppdragInput behandlingInfo, Oppdragskontroll oppdragskontroll, Oppdragsmottaker mottaker, boolean opphFørEndringsoppdrFeriepg) {
        Oppdragslinje150 sisteOppdr150Bruker = TidligereOppdragTjeneste.finnSisteLinjeIKjedeForBruker(behandlingInfo)
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Mangler tidligere oppdragslinje150"));
        Oppdrag110.Builder nyOppdrag110Builder = OpprettOppdrag110Tjeneste.opprettOppdrag110MedRelaterteOppdragsmeldinger(behandlingInfo, sisteOppdr150Bruker, mottaker);
        Oppdrag110 nyOppdrag110 = null;
        boolean skalSendeOpphør = VurderOpphørForYtelse.vurder(behandlingInfo, sisteOppdr150Bruker, mottaker);
        if (skalSendeOpphør) {
            nyOppdrag110 = opprettOppdrag110Og150ForOpphørBruker(behandlingInfo, oppdragskontroll, mottaker, sisteOppdr150Bruker, nyOppdrag110Builder);
        }
        if (nyOppdrag110 == null) {
            Optional<Oppdrag110> oppdrag110Opt = oppdragskontrollOpphør.opprettOppdr150LinjeForFeriepengerOPPH(behandlingInfo, opphFørEndringsoppdrFeriepg,
                nyOppdrag110Builder, sisteOppdr150Bruker.getOppdrag110(), oppdragskontroll);
            nyOppdrag110 = oppdrag110Opt.orElse(null);
        } else {
            oppdragskontrollOpphør.opprettOppdr150LinjeForFeriepengerOPPH(behandlingInfo, opphFørEndringsoppdrFeriepg,
                nyOppdrag110, sisteOppdr150Bruker.getOppdrag110());
        }
        return Optional.ofNullable(nyOppdrag110);
    }

    private Oppdrag110 opprettOppdrag110Og150ForOpphørBruker(OppdragInput behandlingInfo, Oppdragskontroll oppdragskontroll, Oppdragsmottaker mottaker, Oppdragslinje150 sisteOppdr150Bruker, Oppdrag110.Builder nyOppdrag110Builder) {
        Oppdrag110 nyOppdrag110 = nyOppdrag110Builder.medOppdragskontroll(oppdragskontroll).build();
        OpprettOppdragTjeneste.opprettOppdragsenhet120(nyOppdrag110);
        List<Oppdragslinje150> tidligereOppdr150Liste = TidligereOppdragTjeneste.hentTidligereGjeldendeOppdragslinje150(behandlingInfo, false);
        LocalDate datoStatusFom = FinnOpphørFomDato.finnOpphørFom(tidligereOppdr150Liste, behandlingInfo, mottaker);
        oppdragskontrollOpphør.opprettOppdragslinje150ForStatusOPPH(behandlingInfo, sisteOppdr150Bruker,
            nyOppdrag110, datoStatusFom);
        return nyOppdrag110;
    }

    private void lagOpphørsoppdragForArbeidsgiver(OppdragInput behandlingInfo, Oppdragskontroll oppdragskontroll, Oppdragsmottaker mottaker) {
        List<Oppdragslinje150> sisteLinjeKjedeForArbeidsgivereListe = TidligereOppdragTjeneste.finnSisteLinjeKjedeForAlleArbeidsgivere(behandlingInfo);
        List<Oppdragslinje150> sisteLinjeKjedeForDenneArbgvren = Oppdragslinje150Util.finnOppdragslinje150MedRefunderesId(mottaker, sisteLinjeKjedeForArbeidsgivereListe);

        opprettOpphørIEndringsoppdragArbeidsgiver(behandlingInfo, oppdragskontroll, sisteLinjeKjedeForDenneArbgvren, mottaker, false);
    }

    public Optional<Oppdrag110> opprettOpphørIEndringsoppdragArbeidsgiver(OppdragInput behandlingInfo, Oppdragskontroll oppdragskontroll,
                                                                          List<Oppdragslinje150> tidligereOpp150ListeForArbgvren, Oppdragsmottaker mottaker,
                                                                          boolean opphFørEndringsoppdrFeriepg) {
        Optional<Oppdrag110> nyOppdrag110Opt = Optional.empty();
        Oppdrag110 nyOppdrag110 = null;
        Oppdragslinje150 sisteOppdr150 = Oppdragslinje150Util.getOpp150MedMaxDelytelseId(tidligereOpp150ListeForArbgvren);
        Oppdrag110 forrigeOppdrag110 = sisteOppdr150.getOppdrag110();
        boolean endringsdatoEtterSisteDatoAvAlleTidligereOppdrag = TidligereOppdragTjeneste.erEndringsdatoEtterSisteDatoAvAlleTidligereOppdrag(behandlingInfo, sisteOppdr150, mottaker);
        if (endringsdatoEtterSisteDatoAvAlleTidligereOppdrag) {
            return Optional.empty();
        }
        Oppdrag110.Builder nyOppdrag110Builder = OpprettOppdrag110Tjeneste.opprettOppdrag110MedRelaterteOppdragsmeldinger(behandlingInfo, sisteOppdr150, mottaker);

        boolean skalSendeOpphør = VurderOpphørForYtelse.vurder(behandlingInfo, sisteOppdr150, mottaker);
        if (skalSendeOpphør) {
            LocalDate datoStatusFom = FinnOpphørFomDato.finnOpphørFom(tidligereOpp150ListeForArbgvren, behandlingInfo, mottaker);
            nyOppdrag110 = kobleOppdrag110TilOppdragskontroll(oppdragskontroll, nyOppdrag110Builder);
            oppdragskontrollOpphør.opprettOppdragslinje150ForStatusOPPH(behandlingInfo, sisteOppdr150, nyOppdrag110, datoStatusFom);
            nyOppdrag110Opt = Optional.of(nyOppdrag110);
        }
        if (!opphFørEndringsoppdrFeriepg || VurderOpphørForFeriepenger.vurder(behandlingInfo, forrigeOppdrag110, Optional.of(mottaker))) {
            nyOppdrag110 = nyOppdrag110Opt
                .orElseGet(() -> kobleOppdrag110TilOppdragskontroll(oppdragskontroll, nyOppdrag110Builder));
            oppdragskontrollOpphør.opprettOppdr150LinjeForFeriepengerOPPH(behandlingInfo, nyOppdrag110,
                forrigeOppdrag110, false, opphFørEndringsoppdrFeriepg);
        }
        List<Oppdragslinje150> opp150OpphList = TidligereOppdragTjeneste.getOppdragslinje150ForOpphør(nyOppdrag110);
        oppdragskontrollOpphør.kobleAndreMeldingselementerTilOpp150Opphør(behandlingInfo, sisteOppdr150, opp150OpphList);

        return Optional.ofNullable(nyOppdrag110);
    }

    private Oppdrag110 kobleOppdrag110TilOppdragskontroll(Oppdragskontroll oppdragskontroll, Oppdrag110.Builder nyOppdrag110Builder) {
        Oppdrag110 nyOppdrag110 = nyOppdrag110Builder.medOppdragskontroll(oppdragskontroll).build();
        OpprettOppdragTjeneste.opprettOppdragsenhet120(nyOppdrag110);

        return nyOppdrag110;
    }
}
