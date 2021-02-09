package no.nav.foreldrepenger.økonomistøtte.dagytelse.endring;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.økonomistøtte.OppdragskontrollManager;
import no.nav.foreldrepenger.økonomistøtte.Oppdragsmottaker;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.KlassekodeUtleder;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.TidligereOppdragTjeneste;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.adapter.BehandlingTilOppdragMapperTjeneste;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.fp.OppdragInput;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdrag110.OpprettOppdrag110Tjeneste;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdragslinje150.Oppdragslinje150Util;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdragslinje150.OpprettOppdragslinje150Tjeneste;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdragslinje150.OpprettOppdragsmeldingerRelatertTil150;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.opphør.OpprettOpphørIEndringsoppdrag;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.wrapper.TilkjentYtelseAndel;

@ApplicationScoped
public class OppdragskontrollEndring implements OppdragskontrollManager {

    private static final Logger LOG = LoggerFactory.getLogger(OppdragskontrollEndring.class);

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
        OppdragInput oppdragInput = behandlingTilOppdragMapperTjenesteFP.map(behandling);
        if (oppdragInput.getAlleTidligereOppdrag110().isEmpty()) {
            throw new IllegalStateException("Fant ikke forrige oppdrag");
        }

        List<TilkjentYtelseAndel> andelerOriginal = OpprettOppdragslinje150Tjeneste.hentForrigeTilkjentYtelseAndeler(oppdragInput.getForrigeTilkjentYtelsePerioder());
        if (!andelerOriginal.isEmpty()) {
            opprettOpphørIEndringsoppdragFP.lagOppdragForMottakereSomSkalOpphøre(oppdragInput, nyOppdragskontroll, andelerOriginal);
        }

        Map<Oppdragsmottaker, List<TilkjentYtelseAndel>> andelPrMottakerMap = OpprettMottakereMapEndringsoppdrag.finnMottakereMedDeresAndelForEndringsoppdrag(oppdragInput, andelerOriginal);

        if (andelPrMottakerMap.isEmpty()) {
            throw new IllegalStateException("Finnes ingen oppdragsmottakere i behandling " + oppdragInput.getBehandlingId());
        }
        opprettEndringsoppdrag(oppdragInput, andelPrMottakerMap, nyOppdragskontroll);
        return nyOppdragskontroll;
    }

    private void opprettEndringsoppdrag(OppdragInput oppdragInput,
                                        Map<Oppdragsmottaker, List<TilkjentYtelseAndel>> andelPrMottakerMap, Oppdragskontroll nyOppdragskontroll) {

        long løpVerdiForFørstegangsoppdrag = finnInitialLøpenummerVerdi(oppdragInput);

        for (Map.Entry<Oppdragsmottaker, List<TilkjentYtelseAndel>> entry : andelPrMottakerMap.entrySet()) {
            Oppdragsmottaker mottaker = entry.getKey();
            if (mottaker.getStatus() != null && mottaker.erStatusNy()) {
                opprettOppdragForNyeMottakere(oppdragInput, nyOppdragskontroll, entry, løpVerdiForFørstegangsoppdrag);
                løpVerdiForFørstegangsoppdrag++;
            }
            if (mottaker.getStatus() != null && erMottakerIBådeForrigeOgNyTilkjentYtelse(mottaker)) {
                if (mottaker.erBruker()) {
                    opprettEndringsoppdragForBruker(oppdragInput, nyOppdragskontroll, entry);
                } else {
                    opprettEndringsoppdragForArbeidsgiver(oppdragInput, nyOppdragskontroll, entry);
                }
            }
        }
    }

    private void opprettOppdragForNyeMottakere(OppdragInput oppdragInput, Oppdragskontroll oppdragskontroll,
                                               Map.Entry<Oppdragsmottaker, List<TilkjentYtelseAndel>> entry, long løpVerdiForFørstegangsoppdr) {

        long fagsystemId = OpprettOppdrag110Tjeneste.settFagsystemId(oppdragInput.getSaksnummer(), løpVerdiForFørstegangsoppdr, true);
        Oppdrag110 oppdrag110 = OpprettOppdrag110Tjeneste.opprettNyOppdrag110(oppdragInput, oppdragskontroll, entry.getKey(), fagsystemId);
        Oppdragsmottaker mottaker = entry.getKey();
        List<TilkjentYtelseAndel> andelListe = entry.getValue();
        List<Oppdragslinje150> oppdragslinje150List = OpprettOppdragslinje150Tjeneste.opprettOppdragslinje150(oppdragInput, oppdrag110, andelListe, mottaker);
    }

    private void opprettEndringsoppdragForBruker(OppdragInput oppdragInput, Oppdragskontroll nyOppdragskontroll,
                                                 Map.Entry<Oppdragsmottaker, List<TilkjentYtelseAndel>> entry) {

        boolean erDetFlereKlassekodeIForrigeOppdrag = OpprettOppdragslinje150Tjeneste.finnesFlereKlassekodeIForrigeOppdrag(oppdragInput);
        boolean erDetFlereKlassekodeINyOppdrag = KlassekodeUtleder.getKlassekodeListe(entry.getValue()).size() > 1;

        if (!erDetFlereKlassekodeINyOppdrag && !erDetFlereKlassekodeIForrigeOppdrag) {
            opprettEndringsoppdragForBrukerMedEnKlassekode(oppdragInput, nyOppdragskontroll, entry);
        } else if (!erDetFlereKlassekodeIForrigeOppdrag) {
            opprettOppdragForBrukerMedFlereKlassekodeIRevurdering(oppdragInput, nyOppdragskontroll, entry.getKey(), entry.getValue());
        } else if (!erDetFlereKlassekodeINyOppdrag) {
            opprettOppdragForBrukerMedFlereKlassekodeIForrigeBehandling(oppdragInput, nyOppdragskontroll, entry.getKey(), entry.getValue());
        } else {
            opprettOppdragForBrukerMedFlereKlassekodeIBådeForrigeOgNyBehandling(oppdragInput, nyOppdragskontroll, entry.getKey(), entry.getValue());
        }
    }

    private void opprettEndringsoppdragForBrukerMedEnKlassekode(OppdragInput oppdragInput, Oppdragskontroll nyOppdragskontroll,
                                                                Map.Entry<Oppdragsmottaker, List<TilkjentYtelseAndel>> entry) {

        Optional<Oppdragslinje150> sisteOppdr150BrukerOpt = TidligereOppdragTjeneste.finnSisteLinjeIKjedeForBruker(oppdragInput);
        if (sisteOppdr150BrukerOpt.isPresent()) {
            Oppdragsmottaker mottaker = entry.getKey();
            List<TilkjentYtelseAndel> andelListe = entry.getValue();
            Oppdragslinje150 sisteOppdr150Bruker = sisteOppdr150BrukerOpt.get();
            Optional<Oppdrag110> nyOppdrag110Opt = opprettOpphørIEndringsoppdragFP.opprettOpphørIEndringsoppdragBruker(oppdragInput, nyOppdragskontroll,
                mottaker, true);
            if (ingenAndelSkalOpphøresEllerEndres(andelListe, nyOppdrag110Opt)) {
                return;
            }
            fastsettOppdrag110(oppdragInput, nyOppdragskontroll, mottaker, andelListe, nyOppdrag110Opt, sisteOppdr150Bruker);
        }
    }

    private void opprettEndringsoppdragForArbeidsgiver(OppdragInput oppdragInput, Oppdragskontroll nyOppdragskontroll,
                                                       Map.Entry<Oppdragsmottaker, List<TilkjentYtelseAndel>> entry) {

        Oppdragsmottaker mottaker = entry.getKey();
        List<TilkjentYtelseAndel> andelListe = entry.getValue();
        List<Oppdragslinje150> sisteLinjeKjedeForAlleArbeidsgivereListe = TidligereOppdragTjeneste.finnSisteLinjeKjedeForAlleArbeidsgivere(oppdragInput);

        List<Oppdragslinje150> sisteLinjeKjedeForDenneArbeidsgiveren = sisteLinjeKjedeForAlleArbeidsgivereListe.stream()
            .filter(opp150 -> opp150.getRefusjonsinfo156().getRefunderesId().equals(Oppdragslinje150Util.endreTilElleveSiffer(mottaker.getOrgnr())))
            .collect(Collectors.toList());

        Optional<Oppdrag110> nyOppdrag110Opt = opprettOpphørIEndringsoppdragFP.opprettOpphørIEndringsoppdragArbeidsgiver(
            oppdragInput,
            nyOppdragskontroll,
            sisteLinjeKjedeForDenneArbeidsgiveren,
            mottaker,
            true);

        if (ingenAndelSkalOpphøresEllerEndres(andelListe, nyOppdrag110Opt)) {
            return;
        }

        Oppdragslinje150 sisteOppdr150ForDenneArbeidsgiveren = Oppdragslinje150Util.getOpp150MedMaxDelytelseId(sisteLinjeKjedeForDenneArbeidsgiveren);
        fastsettOppdrag110(oppdragInput, nyOppdragskontroll, mottaker, andelListe, nyOppdrag110Opt, sisteOppdr150ForDenneArbeidsgiveren);
    }

    private void fastsettOppdrag110(OppdragInput oppdragInput, Oppdragskontroll nyOppdragskontroll, Oppdragsmottaker mottaker, List<TilkjentYtelseAndel> andelListe, Optional<Oppdrag110> nyOppdrag110Opt, Oppdragslinje150 sisteOppdr150ForDenneArbeidsgiveren) {
        Oppdrag110 nyOppdrag110 = OpprettOppdrag110Tjeneste.fastsettOppdrag110(oppdragInput, nyOppdragskontroll, nyOppdrag110Opt, sisteOppdr150ForDenneArbeidsgiveren, mottaker);
        if (mottaker.erStatusUendret()) {
            return;
        }
        OpprettOppdragslinje150Tjeneste.opprettOppdragslinje150(oppdragInput, nyOppdrag110,
            andelListe, mottaker, sisteOppdr150ForDenneArbeidsgiveren);
    }

    private void opprettOppdragForBrukerMedFlereKlassekodeIRevurdering(OppdragInput oppdragInput,
                                                                       Oppdragskontroll nyOppdragskontroll,
                                                                       Oppdragsmottaker mottaker, List<TilkjentYtelseAndel> andelListe) {

        Optional<Oppdragslinje150> sisteOppdr150BrukerOpt = TidligereOppdragTjeneste.finnSisteLinjeIKjedeForBruker(oppdragInput);
        if (sisteOppdr150BrukerOpt.isPresent()) {
            Oppdragslinje150 sisteOppdr150Bruker = sisteOppdr150BrukerOpt.get();
            Optional<Oppdrag110> nyOppdrag110Opt = opprettOpphørIEndringsoppdragFP.opprettOpphørIEndringsoppdragBruker(oppdragInput, nyOppdragskontroll,
                mottaker, true);
            Oppdrag110 nyOppdrag110 = OpprettOppdrag110Tjeneste.fastsettOppdrag110(oppdragInput, nyOppdragskontroll, nyOppdrag110Opt, sisteOppdr150Bruker, mottaker);
            List<List<TilkjentYtelseAndel>> andelerGruppertMedKlassekode = OpprettOppdragslinje150Tjeneste.gruppereAndelerMedKlassekode(andelListe);
            OpprettOppdragslinje150Tjeneste.opprettOppdr150ForBrukerMedFlereKlassekode(oppdragInput, nyOppdrag110,
                andelerGruppertMedKlassekode, mottaker, Collections.singletonList(sisteOppdr150Bruker));
        }
    }

    private void opprettOppdragForBrukerMedFlereKlassekodeIForrigeBehandling(OppdragInput oppdragInput,
                                                                             Oppdragskontroll nyOppdragskontroll,
                                                                             Oppdragsmottaker mottaker,
                                                                             List<TilkjentYtelseAndel> andelListe) {

        Optional<Oppdrag110> nyOppdrag110Opt = opprettOpphørIEndringsoppdragFP.opprettOpphørsoppdragForBrukerMedFlereKlassekode(oppdragInput,
            nyOppdragskontroll, mottaker, true);
        List<Oppdragslinje150> tidligereOppdr150Liste = TidligereOppdragTjeneste.hentTidligereGjeldendeOppdragslinje150(oppdragInput,
            false);
        Oppdragslinje150 oppdr150MedMaxDelytelseId = tidligereOppdr150Liste.stream()
            .max(Comparator.comparing(Oppdragslinje150::getDelytelseId).thenComparing(Oppdragslinje150::getKodeStatusLinje, Comparator.nullsFirst(Comparator.naturalOrder())))
            .orElseThrow(() -> new IllegalStateException("Utvikler feil: Mangler forrige oppdrag"));
        if (ingenAndelSkalOpphøresEllerEndres(andelListe, nyOppdrag110Opt)) {
            return;
        }
        Oppdrag110 nyOppdrag110 = OpprettOppdrag110Tjeneste.fastsettOppdrag110(oppdragInput, nyOppdragskontroll, nyOppdrag110Opt, oppdr150MedMaxDelytelseId, mottaker);
        if (mottaker.erStatusUendret()) {
            return;
        }
        TilkjentYtelseAndel andel = andelListe.stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Mangler oppdrag andel"));
        KodeKlassifik kodeKlassifik = KlassekodeUtleder.utled(andel);
        Oppdragslinje150 sisteOppdr150Bruker = tidligereOppdr150Liste.stream()
            .filter(oppdr150 -> oppdr150.getKodeKlassifik().equals(kodeKlassifik))
            .max(Comparator.comparing(Oppdragslinje150::getDelytelseId).thenComparing(Oppdragslinje150::getKodeStatusLinje, Comparator.nullsFirst(Comparator.naturalOrder())))
            .orElse(oppdr150MedMaxDelytelseId);
        OpprettOppdragslinje150Tjeneste.opprettOppdragslinje150(oppdragInput, nyOppdrag110,
            andelListe, mottaker, sisteOppdr150Bruker);
    }

    private void opprettOppdragForBrukerMedFlereKlassekodeIBådeForrigeOgNyBehandling(OppdragInput oppdragInput, Oppdragskontroll oppdragskontroll,
                                                                                     Oppdragsmottaker mottaker, List<TilkjentYtelseAndel> andelListe) {

        Optional<Oppdrag110> nyOppdrag110Opt = opprettOpphørIEndringsoppdragFP.opprettOpphørsoppdragForBrukerMedFlereKlassekode(oppdragInput, oppdragskontroll, mottaker,
            true);
        List<Oppdragslinje150> tidligereOpp150ListeForBruker = TidligereOppdragTjeneste.hentTidligereGjeldendeOppdragslinje150(oppdragInput, false);
        Oppdragslinje150 tidligereOppdr150Bruker = tidligereOpp150ListeForBruker.stream().findFirst()
            .orElseThrow(() -> new IllegalStateException("Utvikler feil: Mangler tidligere oppdragslinje for bruker"));
        Oppdrag110 nyOppdrag110 = OpprettOppdrag110Tjeneste.fastsettOppdrag110(oppdragInput, oppdragskontroll, nyOppdrag110Opt, tidligereOppdr150Bruker, mottaker);
        List<Oppdragslinje150> opphørtOppdragslinje150Liste = new ArrayList<>();
        for (Oppdragslinje150 nyOpp150 : nyOppdrag110.getOppdragslinje150Liste()) {
            tidligereOpp150ListeForBruker.stream()
                .filter(forrigeOpp150 -> forrigeOpp150.getDelytelseId().equals(nyOpp150.getDelytelseId()))
                .findFirst()
                .ifPresent(opphørtOppdragslinje150Liste::add);
        }
        List<Oppdragslinje150> tidligereOpp150ListForKjeding = !opphørtOppdragslinje150Liste.isEmpty() ? opphørtOppdragslinje150Liste : tidligereOpp150ListeForBruker;
        List<List<TilkjentYtelseAndel>> andelerGruppertMedKlassekode = OpprettOppdragslinje150Tjeneste.gruppereAndelerMedKlassekode(andelListe);
        OpprettOppdragslinje150Tjeneste.opprettOppdr150ForBrukerMedFlereKlassekode(oppdragInput, nyOppdrag110,
            andelerGruppertMedKlassekode, mottaker, tidligereOpp150ListForKjeding);
    }

    private boolean erMottakerIBådeForrigeOgNyTilkjentYtelse(Oppdragsmottaker mottaker) {
        return mottaker.erStatusEndret() || mottaker.erStatusUendret();
    }

    private long finnInitialLøpenummerVerdi(OppdragInput oppdragInput) {
        return oppdragInput.getAlleTidligereOppdrag110().stream()
            .map(Oppdrag110::getFagsystemId)
            .max(Comparator.comparing(Function.identity()))
            .orElseThrow(() -> new IllegalStateException("Utvikler feil: Forrige oppdrag mangler fagsystemId"));
    }

    /**
     * @param andelListe      Oppdrag andeler fom endringsdato i revurdering
     * @param nyOppdrag110Opt Hvis nyOppdragOpt ikke er present betyr det at det finnes ingen andeler som skal opphøres i forrige behandling ellers
     *                        blir det present og inneholder oppdragslinje150 for opphør
     * @return true hvis det ikke skal opprettes oppdrag for mottakeren
     */
    private boolean ingenAndelSkalOpphøresEllerEndres(List<TilkjentYtelseAndel> andelListe, Optional<Oppdrag110> nyOppdrag110Opt) {
        return nyOppdrag110Opt.isEmpty() && andelListe.isEmpty();
    }
}
