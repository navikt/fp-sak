package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.endring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragskontrollManager;
import no.nav.foreldrepenger.økonomi.økonomistøtte.Oppdragsmottaker;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.KlassekodeUtleder;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.TidligereOppdragTjeneste;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.adapter.BehandlingTilOppdragMapperTjeneste;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.OppdragInput;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.oppdrag110.OpprettOppdrag110Tjeneste;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.oppdragslinje150.Oppdragslinje150Util;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.oppdragslinje150.OpprettOppdragslinje150Tjeneste;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.oppdragslinje150.OpprettOppdragsmeldingerRelatertTil150;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.opphør.OpprettOpphørIEndringsoppdrag;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelseAndel;

@ApplicationScoped
public class OppdragskontrollEndring implements OppdragskontrollManager {

    private OpprettOpphørIEndringsoppdrag opprettOpphørIEndringsoppdragFP;
    private BehandlingTilOppdragMapperTjeneste behandlingTilOppdragMapperTjenesteFP;

    OppdragskontrollEndring() {
        // For CDI
    }

    @Inject
    public OppdragskontrollEndring(BehandlingTilOppdragMapperTjeneste behandlingTilOppdragMapperTjenesteFP,
                                   OpprettOpphørIEndringsoppdrag opprettOpphørIEndringsoppdragFP) {
        this.behandlingTilOppdragMapperTjenesteFP = behandlingTilOppdragMapperTjenesteFP;
        this.opprettOpphørIEndringsoppdragFP = opprettOpphørIEndringsoppdragFP;
    }

    @Override
    public Oppdragskontroll opprettØkonomiOppdrag(Behandling behandling, Oppdragskontroll nyOppdragskontroll) {
        OppdragInput behandlingInfo = behandlingTilOppdragMapperTjenesteFP.map(behandling);
        if (behandlingInfo.getAlleTidligereOppdrag110().isEmpty()) {
            throw new IllegalStateException("Fant ikke forrige oppdrag");
        }

        List<TilkjentYtelseAndel> andelerOriginal = finnAndelerIForrigeBehandling(nyOppdragskontroll, behandlingInfo);
        Map<Oppdragsmottaker, List<TilkjentYtelseAndel>> andelPrMottakerMap = OpprettMottakereMapEndringsoppdrag.finnMottakereMedDeresAndelForEndringsoppdrag(behandlingInfo, andelerOriginal);

        if (andelPrMottakerMap.isEmpty()) {
            throw new IllegalStateException("Finnes ingen oppdragsmottakere i behandling " + behandlingInfo.getBehandlingId());
        }
        opprettEndringsoppdrag(behandlingInfo, andelPrMottakerMap, nyOppdragskontroll);
        return nyOppdragskontroll;
    }

    private void opprettEndringsoppdrag(OppdragInput behandlingInfo,
                                        Map<Oppdragsmottaker, List<TilkjentYtelseAndel>> andelPrMottakerMap, Oppdragskontroll nyOppdragskontroll) {

        long løpVerdiForFørstegangsoppdrag = finnInitialLøpenummerVerdi(behandlingInfo);

        for (Map.Entry<Oppdragsmottaker, List<TilkjentYtelseAndel>> entry : andelPrMottakerMap.entrySet()) {
            Oppdragsmottaker mottaker = entry.getKey();
            if (mottaker.getStatus() != null && mottaker.erStatusNy()) {
                opprettOppdragForNyeMottakere(behandlingInfo, nyOppdragskontroll, entry, løpVerdiForFørstegangsoppdrag);
                løpVerdiForFørstegangsoppdrag++;
            }
            if (mottaker.getStatus() != null && erMottakerIBådeForrigeOgNyTilkjentYtelse(mottaker)) {
                if (mottaker.erBruker()) {
                    opprettEndringsoppdragForBruker(behandlingInfo, nyOppdragskontroll, entry);
                } else {
                    opprettEndringsoppdragForArbeidsgiver(behandlingInfo, nyOppdragskontroll, entry);
                }
            }
        }
    }

    private void opprettOppdragForNyeMottakere(OppdragInput behandlingInfo, Oppdragskontroll oppdragskontroll,
                                               Map.Entry<Oppdragsmottaker, List<TilkjentYtelseAndel>> entry, long løpVerdiForFørstegangsoppdr) {

        long fagsystemId = OpprettOppdrag110Tjeneste.settFagsystemId(behandlingInfo.getSaksnummer(), løpVerdiForFørstegangsoppdr, true);
        Oppdrag110 oppdrag110 = OpprettOppdrag110Tjeneste.opprettNyOppdrag110(behandlingInfo, oppdragskontroll, entry.getKey(), fagsystemId);
        Oppdragsmottaker mottaker = entry.getKey();
        List<TilkjentYtelseAndel> andelListe = entry.getValue();
        List<Oppdragslinje150> oppdragslinje150List = OpprettOppdragslinje150Tjeneste.opprettOppdragslinje150(behandlingInfo, oppdrag110, andelListe, mottaker);
        OpprettOppdragsmeldingerRelatertTil150.opprettAttestant180(oppdragslinje150List, behandlingInfo.getAnsvarligSaksbehandler());
    }

    private void opprettEndringsoppdragForBruker(OppdragInput behandlingInfo, Oppdragskontroll nyOppdragskontroll,
                                                 Map.Entry<Oppdragsmottaker, List<TilkjentYtelseAndel>> entry) {

        boolean erDetFlereKlassekodeIForrigeOppdrag = OpprettOppdragslinje150Tjeneste.finnesFlereKlassekodeIForrigeOppdrag(behandlingInfo);
        boolean erDetFlereKlassekodeINyOppdrag = KlassekodeUtleder.getKlassekodeListe(entry.getValue()).size() > 1;
        if (!erDetFlereKlassekodeINyOppdrag && !erDetFlereKlassekodeIForrigeOppdrag) {
            opprettEndringsoppdragForBrukerMedEnKlassekode(behandlingInfo, nyOppdragskontroll, entry);
        } else {
            opprettEndringsoppdragForBrukerMedFlereKlassekode(behandlingInfo, nyOppdragskontroll, entry);
        }
    }

    private void opprettEndringsoppdragForBrukerMedEnKlassekode(OppdragInput behandlingInfo, Oppdragskontroll nyOppdragskontroll,
                                                                Map.Entry<Oppdragsmottaker, List<TilkjentYtelseAndel>> entry) {

        Optional<Oppdragslinje150> sisteOppdr150BrukerOpt = TidligereOppdragTjeneste.finnSisteLinjeIKjedeForBruker(behandlingInfo);
        if (sisteOppdr150BrukerOpt.isPresent()) {
            Oppdragsmottaker mottaker = entry.getKey();
            List<TilkjentYtelseAndel> andelListe = entry.getValue();
            Oppdragslinje150 sisteOppdr150Bruker = sisteOppdr150BrukerOpt.get();
            Optional<Oppdrag110> nyOppdrag110Opt = opprettOpphørIEndringsoppdragFP.opprettOpphørIEndringsoppdragBruker(behandlingInfo, nyOppdragskontroll,
                mottaker, true);
            if (ingenAndelSkalOpphøresEllerEndres(andelListe, nyOppdrag110Opt)) {
                return;
            }
            Oppdrag110 nyOppdrag110 = OpprettOppdrag110Tjeneste.fastsettOppdrag110(behandlingInfo, nyOppdragskontroll, nyOppdrag110Opt, sisteOppdr150Bruker, mottaker);
            if (mottaker.erStatusUendret()) {
                return;
            }
            List<Oppdragslinje150> oppdragslinje150List = OpprettOppdragslinje150Tjeneste.opprettOppdragslinje150(behandlingInfo, nyOppdrag110,
                andelListe, mottaker, sisteOppdr150Bruker);
            OpprettOppdragsmeldingerRelatertTil150.opprettAttestant180(oppdragslinje150List, behandlingInfo.getAnsvarligSaksbehandler());
        }
    }

    private void opprettEndringsoppdragForBrukerMedFlereKlassekode(OppdragInput behandlingInfo, Oppdragskontroll nyOppdragskontroll,
                                                                   Map.Entry<Oppdragsmottaker, List<TilkjentYtelseAndel>> entry) {

        Oppdragsmottaker mottaker = entry.getKey();
        boolean erDetFlereKlassekodeIForrigeOppdrag = OpprettOppdragslinje150Tjeneste.finnesFlereKlassekodeIForrigeOppdrag(behandlingInfo);
        boolean erDetFlereKlassekodeINyOppdrag = KlassekodeUtleder.getKlassekodeListe(entry.getValue()).size() > 1;
        List<TilkjentYtelseAndel> andelListe = entry.getValue();
        if (!erDetFlereKlassekodeIForrigeOppdrag && erDetFlereKlassekodeINyOppdrag) {
            opprettOppdragForBrukerMedFlereKlassekodeIRevurdering(behandlingInfo, nyOppdragskontroll, mottaker, andelListe);
        } else if (erDetFlereKlassekodeIForrigeOppdrag && !erDetFlereKlassekodeINyOppdrag) {
            opprettOppdragForBrukerMedFlereKlassekodeIForrigeBehandling(behandlingInfo, nyOppdragskontroll, mottaker, andelListe);
        } else {
            opprettOppdragForBrukerMedFlereKlassekodeIBådeForrigeOgNyBehandling(behandlingInfo, nyOppdragskontroll, mottaker, andelListe);
        }
    }

    private void opprettEndringsoppdragForArbeidsgiver(OppdragInput behandlingInfo, Oppdragskontroll nyOppdragskontroll,
                                                       Map.Entry<Oppdragsmottaker, List<TilkjentYtelseAndel>> entry) {

        Oppdragsmottaker mottaker = entry.getKey();
        List<TilkjentYtelseAndel> andelListe = entry.getValue();
        List<Oppdragslinje150> sisteLinjeKjedeForAlleArbeidsgivereListe = TidligereOppdragTjeneste.finnSisteLinjeKjedeForAlleArbeidsgivere(behandlingInfo);

        List<Oppdragslinje150> sisteLinjeKjedeForDenneArbeidsgiveren = sisteLinjeKjedeForAlleArbeidsgivereListe.stream()
            .filter(opp150 -> opp150.getRefusjonsinfo156().getRefunderesId().equals(Oppdragslinje150Util.endreTilElleveSiffer(mottaker.getOrgnr())))
            .collect(Collectors.toList());

        Oppdragslinje150 sisteOppdr150ForDenneArbeidsgiveren = Oppdragslinje150Util.getOpp150MedMaxDelytelseId(sisteLinjeKjedeForDenneArbeidsgiveren);
        Optional<Oppdrag110> nyOppdrag110Opt = opprettOpphørIEndringsoppdragFP.opprettOpphørIEndringsoppdragArbeidsgiver(behandlingInfo, nyOppdragskontroll,
            sisteLinjeKjedeForDenneArbeidsgiveren, mottaker, true);
        if (ingenAndelSkalOpphøresEllerEndres(andelListe, nyOppdrag110Opt)) {
            return;
        }
        Oppdrag110 nyOppdrag110 = OpprettOppdrag110Tjeneste.fastsettOppdrag110(behandlingInfo, nyOppdragskontroll, nyOppdrag110Opt, sisteOppdr150ForDenneArbeidsgiveren, mottaker);
        if (mottaker.erStatusUendret()) {
            return;
        }
        List<Oppdragslinje150> oppdragslinje150List = OpprettOppdragslinje150Tjeneste.opprettOppdragslinje150(behandlingInfo, nyOppdrag110,
            andelListe, mottaker, sisteOppdr150ForDenneArbeidsgiveren);
        OpprettOppdragsmeldingerRelatertTil150.opprettAttestant180(oppdragslinje150List, behandlingInfo.getAnsvarligSaksbehandler());
    }

    private void opprettOppdragForBrukerMedFlereKlassekodeIRevurdering(OppdragInput behandlingInfo,
                                                                       Oppdragskontroll nyOppdragskontroll,
                                                                       Oppdragsmottaker mottaker, List<TilkjentYtelseAndel> andelListe) {

        Optional<Oppdragslinje150> sisteOppdr150BrukerOpt = TidligereOppdragTjeneste.finnSisteLinjeIKjedeForBruker(behandlingInfo);
        if (sisteOppdr150BrukerOpt.isPresent()) {
            Oppdragslinje150 sisteOppdr150Bruker = sisteOppdr150BrukerOpt.get();
            Optional<Oppdrag110> nyOppdrag110Opt = opprettOpphørIEndringsoppdragFP.opprettOpphørIEndringsoppdragBruker(behandlingInfo, nyOppdragskontroll,
                mottaker, true);
            Oppdrag110 nyOppdrag110 = OpprettOppdrag110Tjeneste.fastsettOppdrag110(behandlingInfo, nyOppdragskontroll, nyOppdrag110Opt, sisteOppdr150Bruker, mottaker);
            List<List<TilkjentYtelseAndel>> andelerGruppertMedKlassekode = OpprettOppdragslinje150Tjeneste.gruppereAndelerMedKlassekode(andelListe);
            List<Oppdragslinje150> oppdragslinje150List = OpprettOppdragslinje150Tjeneste.opprettOppdr150ForBrukerMedFlereKlassekode(behandlingInfo, nyOppdrag110,
                andelerGruppertMedKlassekode, mottaker, Collections.singletonList(sisteOppdr150Bruker));
            OpprettOppdragsmeldingerRelatertTil150.opprettAttestant180(oppdragslinje150List, behandlingInfo.getAnsvarligSaksbehandler());
        }
    }

    private void opprettOppdragForBrukerMedFlereKlassekodeIForrigeBehandling(OppdragInput behandlingInfo,
                                                                             Oppdragskontroll nyOppdragskontroll,
                                                                             Oppdragsmottaker mottaker,
                                                                             List<TilkjentYtelseAndel> andelListe) {

        Optional<Oppdrag110> nyOppdrag110Opt = opprettOpphørIEndringsoppdragFP.opprettOpphørsoppdragForBrukerMedFlereKlassekode(behandlingInfo,
            nyOppdragskontroll, mottaker, true);
        List<Oppdragslinje150> tidligereOppdr150Liste = TidligereOppdragTjeneste.hentTidligereGjeldendeOppdragslinje150(behandlingInfo,
            false);
        Oppdragslinje150 oppdr150MedMaxDelytelseId = tidligereOppdr150Liste.stream()
            .max(Comparator.comparing(Oppdragslinje150::getDelytelseId))
            .orElseThrow(() -> new IllegalStateException("Utvikler feil: Mangler forrige oppdrag"));
        if (ingenAndelSkalOpphøresEllerEndres(andelListe, nyOppdrag110Opt)) {
            return;
        }
        Oppdrag110 nyOppdrag110 = OpprettOppdrag110Tjeneste.fastsettOppdrag110(behandlingInfo, nyOppdragskontroll, nyOppdrag110Opt, oppdr150MedMaxDelytelseId, mottaker);
        if (mottaker.erStatusUendret()) {
            return;
        }
        TilkjentYtelseAndel andel = andelListe.stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Mangler oppdrag andel"));
        String kodeKlassifik = KlassekodeUtleder.utled(andel);
        Oppdragslinje150 sisteOppdr150Bruker = tidligereOppdr150Liste.stream()
            .filter(oppdr150 -> oppdr150.getKodeKlassifik().equals(kodeKlassifik))
            .max(Comparator.comparing(Oppdragslinje150::getDelytelseId))
            .orElse(oppdr150MedMaxDelytelseId);
        List<Oppdragslinje150> oppdragslinje150List = OpprettOppdragslinje150Tjeneste.opprettOppdragslinje150(behandlingInfo, nyOppdrag110,
            andelListe, mottaker, sisteOppdr150Bruker);
        OpprettOppdragsmeldingerRelatertTil150.opprettAttestant180(oppdragslinje150List, behandlingInfo.getAnsvarligSaksbehandler());
    }

    private void opprettOppdragForBrukerMedFlereKlassekodeIBådeForrigeOgNyBehandling(OppdragInput behandlingInfo, Oppdragskontroll oppdragskontroll,
                                                                                     Oppdragsmottaker mottaker, List<TilkjentYtelseAndel> andelListe) {

        Optional<Oppdrag110> nyOppdrag110Opt = opprettOpphørIEndringsoppdragFP.opprettOpphørsoppdragForBrukerMedFlereKlassekode(behandlingInfo, oppdragskontroll, mottaker,
            true);
        List<Oppdragslinje150> tidligereOpp150ListeForBruker = TidligereOppdragTjeneste.hentTidligereGjeldendeOppdragslinje150(behandlingInfo, false);
        Oppdragslinje150 tidligereOppdr150Bruker = tidligereOpp150ListeForBruker.stream().findFirst()
            .orElseThrow(() -> new IllegalStateException("Utvikler feil: Mangler tidligere oppdragslinje for bruker"));
        Oppdrag110 nyOppdrag110 = OpprettOppdrag110Tjeneste.fastsettOppdrag110(behandlingInfo, oppdragskontroll, nyOppdrag110Opt, tidligereOppdr150Bruker, mottaker);
        List<Oppdragslinje150> opphørtOppdragslinje150Liste = new ArrayList<>();
        for (Oppdragslinje150 nyOpp150 : nyOppdrag110.getOppdragslinje150Liste()) {
            tidligereOpp150ListeForBruker.stream()
                .filter(forrigeOpp150 -> forrigeOpp150.getDelytelseId().equals(nyOpp150.getDelytelseId()))
                .findFirst()
                .ifPresent(opphørtOppdragslinje150Liste::add);
        }
        List<Oppdragslinje150> tidligereOpp150ListForKjeding = !opphørtOppdragslinje150Liste.isEmpty() ? opphørtOppdragslinje150Liste : tidligereOpp150ListeForBruker;
        List<List<TilkjentYtelseAndel>> andelerGruppertMedKlassekode = OpprettOppdragslinje150Tjeneste.gruppereAndelerMedKlassekode(andelListe);
        List<Oppdragslinje150> oppdragslinje150List = OpprettOppdragslinje150Tjeneste.opprettOppdr150ForBrukerMedFlereKlassekode(behandlingInfo, nyOppdrag110,
            andelerGruppertMedKlassekode, mottaker, tidligereOpp150ListForKjeding);
        OpprettOppdragsmeldingerRelatertTil150.opprettAttestant180(oppdragslinje150List, behandlingInfo.getAnsvarligSaksbehandler());
    }

    private boolean erMottakerIBådeForrigeOgNyTilkjentYtelse(Oppdragsmottaker mottaker) {
        return mottaker.erStatusEndret() || mottaker.erStatusUendret();
    }

    private long finnInitialLøpenummerVerdi(OppdragInput behandlingInfoFP) {
        return behandlingInfoFP.getAlleTidligereOppdrag110().stream()
            .map(Oppdrag110::getFagsystemId)
            .max(Comparator.comparing(Function.identity()))
            .orElseThrow(() -> new IllegalStateException("Utvikler feil: Forrige oppdrag mangler fagsystemId"));
    }

    private List<TilkjentYtelseAndel> finnAndelerIForrigeBehandling(Oppdragskontroll oppdragskontroll, OppdragInput behandlingInfo) {
        List<TilkjentYtelseAndel> forrigeTilkjentYtelseAndeler = OpprettOppdragslinje150Tjeneste.hentForrigeTilkjentYtelseAndeler(behandlingInfo);
        if (!forrigeTilkjentYtelseAndeler.isEmpty()) {
            opprettOpphørIEndringsoppdragFP.lagOppdragForMottakereSomSkalOpphøre(behandlingInfo, oppdragskontroll, forrigeTilkjentYtelseAndeler);
        }
        return forrigeTilkjentYtelseAndeler;
    }

    /**
     * @param andelListe      Oppdrag andeler fom endringsdato i revurdering
     * @param nyOppdrag110Opt Hvis nyOppdragOpt ikke er present betyr det at det finnes ingen andeler som skal opphøres i forrige behandling ellers
     *                        blir det present og inneholder oppdragslinje150 for opphør
     * @return true hvis det ikke skal opprettes oppdrag for mottakeren
     */
    private boolean ingenAndelSkalOpphøresEllerEndres(List<TilkjentYtelseAndel> andelListe, Optional<Oppdrag110> nyOppdrag110Opt) {
        return !nyOppdrag110Opt.isPresent() && andelListe.isEmpty();
    }
}
