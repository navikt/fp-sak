package no.nav.foreldrepenger.økonomistøtte.dagytelse.opphør;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Sats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.OppdragsmottakerStatus;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.økonomistøtte.OppdragskontrollManager;
import no.nav.foreldrepenger.økonomistøtte.Oppdragsmottaker;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.TidligereOppdragTjeneste;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.VurderFeriepengerBeregning;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.adapter.BehandlingTilOppdragMapperTjeneste;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.fp.OppdragInput;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdrag110.KodeFagområdeTjenesteProvider;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdrag110.OpprettOppdrag110Tjeneste;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdragslinje150.Oppdragslinje150FeriepengerUtil;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdragslinje150.Oppdragslinje150Util;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdragslinje150.OpprettOppdragslinje150Tjeneste;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdragslinje150.OpprettOppdragsmeldingerRelatertTil150;

@ApplicationScoped
public class OppdragskontrollOpphør implements OppdragskontrollManager {

    private BehandlingTilOppdragMapperTjeneste behandlingTilOppdragMapperTjeneste;

    OppdragskontrollOpphør() {
        // For CDI
    }

    @Inject
    public OppdragskontrollOpphør(BehandlingTilOppdragMapperTjeneste behandlingTilOppdragMapperTjeneste) {
        this.behandlingTilOppdragMapperTjeneste = behandlingTilOppdragMapperTjeneste;
    }

    @Override
    public Oppdragskontroll opprettØkonomiOppdrag(Behandling behandling, Oppdragskontroll oppdragskontroll) {
        OppdragInput behandlingInfo = behandlingTilOppdragMapperTjeneste.map(behandling);
        boolean erDetFlereKlassekodeForBruker = OpprettOppdragslinje150Tjeneste.finnesFlereKlassekodeIForrigeOppdrag(behandlingInfo);
        if (erDetFlereKlassekodeForBruker) {
            opprettOpphørsoppdragForBrukerMedFlereKlassekode(behandlingInfo, oppdragskontroll);
        } else {
            opprettOpphørsoppdragForBruker(behandlingInfo, oppdragskontroll);
        }
        opprettOpphørsoppdragForArbeidsgiver(behandlingInfo, oppdragskontroll);
        return oppdragskontroll;
    }

    private Optional<Oppdrag110> opprettOpphørsoppdragForBrukerMedFlereKlassekode(OppdragInput behandlingInfo,
                                                                                  Oppdragskontroll nyOppdragskontroll) {

        String id = behandlingInfo.getPersonIdent().getIdent();
        Oppdragsmottaker mottaker = new Oppdragsmottaker(id, true);
        mottaker.setStatus(OppdragsmottakerStatus.OPPH);
        return opprettOpphørsoppdragForBrukerMedFlereKlassekode(behandlingInfo, nyOppdragskontroll, mottaker, false);
    }

    Optional<Oppdrag110> opprettOpphørsoppdragForBrukerMedFlereKlassekode(OppdragInput behandlingInfo,
                                                                          Oppdragskontroll nyOppdragskontroll,
                                                                          Oppdragsmottaker mottaker,
                                                                          boolean opphFørEndringsoppdrFeriepg) {

        if (TidligereOppdragTjeneste.erEndringsdatoEtterSisteTomDatoAvAlleTidligereOppdrag(behandlingInfo)) {
            return Optional.empty();
        }
        List<Oppdragslinje150> tidligereGjeldendeOppdr150Liste = TidligereOppdragTjeneste.hentTidligereGjeldendeOppdragslinje150(behandlingInfo, false);
        if (tidligereGjeldendeOppdr150Liste.isEmpty()) {
            return Optional.empty();
        }
        Oppdragslinje150 forrigeOpp150 = tidligereGjeldendeOppdr150Liste.get(0);
        Oppdrag110 forrigeOppdrag110 = forrigeOpp150.getOppdrag110();
        Oppdrag110.Builder nyOppdrag110Builder = OpprettOppdrag110Tjeneste.opprettOppdrag110MedRelaterteOppdragsmeldinger(behandlingInfo, forrigeOpp150, mottaker);

        Optional<Oppdrag110> nyOppdragOpt;
        Optional<Oppdrag110> oppdrag110Opt = opprettOpphørPåSisteOpp150ForBrukerSinYtelse(behandlingInfo, mottaker, tidligereGjeldendeOppdr150Liste,
            nyOppdrag110Builder, nyOppdragskontroll);
        if (oppdrag110Opt.isPresent()) {
            Oppdrag110 oppdrag110 = oppdrag110Opt.get();
            opprettOppdr150LinjeForFeriepengerOPPH(behandlingInfo, opphFørEndringsoppdrFeriepg, oppdrag110, forrigeOppdrag110);
            nyOppdragOpt = Optional.of(oppdrag110);
        } else {
            nyOppdragOpt = opprettOppdr150LinjeForFeriepengerOPPH(behandlingInfo, opphFørEndringsoppdrFeriepg,
                nyOppdrag110Builder, forrigeOppdrag110, nyOppdragskontroll);
        }
        List<Oppdragslinje150> opp150List = nyOppdragOpt.map(Oppdrag110::getOppdragslinje150Liste)
            .orElse(Collections.emptyList());
        Oppdragslinje150Util.getOppdragslinje150ForOpphør(opp150List);
        return nyOppdragOpt;
    }

    private Optional<Oppdrag110> opprettOpphørPåSisteOpp150ForBrukerSinYtelse(OppdragInput behandlingInfo, Oppdragsmottaker mottaker,
                                                                              List<Oppdragslinje150> tidligereOppdr150Liste, Oppdrag110.Builder nyOppdrag110Builder,
                                                                              Oppdragskontroll nyOppdragskontroll) {

        Map<KodeKlassifik, List<Oppdragslinje150>> opp150ListePerKlassekodeMap = tidligereOppdr150Liste.stream()
            .collect(Collectors.groupingBy(Oppdragslinje150::getKodeKlassifik));

        int iter = 0;
        Oppdrag110 nyOppdrag110 = null;
        for (Map.Entry<KodeKlassifik, List<Oppdragslinje150>> opp150ListePerKlassekode : opp150ListePerKlassekodeMap.entrySet()) {
            if (OpphørUtil.erBrukerAllredeFullstendigOpphørtForKlassekode(behandlingInfo, opp150ListePerKlassekode.getKey()))
                continue;
            List<Oppdragslinje150> opp150MedSammeKlassekodeListe = opp150ListePerKlassekode.getValue();
            Optional<Oppdragslinje150> sisteOppdr150BrukerOpt = opp150MedSammeKlassekodeListe
                .stream()
                .max(Comparator.comparing(Oppdragslinje150::getDelytelseId).thenComparing(Oppdragslinje150::getKodeStatusLinje, Comparator.nullsFirst(Comparator.naturalOrder())));
            if (sisteOppdr150BrukerOpt.isPresent()) {
                Oppdragslinje150 sisteOppdr150Bruker = sisteOppdr150BrukerOpt.get();
                boolean skalSendeOpphør = VurderOpphørForYtelse.vurder(behandlingInfo, sisteOppdr150Bruker, mottaker);
                if (!skalSendeOpphør) {
                    continue;
                }
                if (iter == 0) {
                    nyOppdrag110 = nyOppdrag110Builder.medOppdragskontroll(nyOppdragskontroll).build();
                }
                LocalDate opphørFom = FinnOpphørFomDato.finnOpphørFom(opp150MedSammeKlassekodeListe, behandlingInfo, mottaker);
                opprettOppdragslinje150ForStatusOPPH(behandlingInfo, sisteOppdr150Bruker, nyOppdrag110, opphørFom);
                iter++;
            }
        }
        return Optional.ofNullable(nyOppdrag110);
    }

    private void opprettOpphørsoppdragForBruker(OppdragInput behandlingInfo, Oppdragskontroll oppdragskontroll) {
        if (OpphørUtil.erBrukerAllredeFullstendigOpphørt(behandlingInfo)) {
            return;
        }
        Optional<Oppdragslinje150> sisteOppdr150BrukerOpt = TidligereOppdragTjeneste.finnSisteLinjeIKjedeForBruker(behandlingInfo);
        if (sisteOppdr150BrukerOpt.isPresent()) {
            Oppdragslinje150 sisteOppdr150Bruker = sisteOppdr150BrukerOpt.get();
            LocalDate opphørStatusFomForBruker = FinnOpphørFomDato.finnOpphørFomForBruker(sisteOppdr150Bruker, behandlingInfo);
            opprettOppdragForOpphørBruker(behandlingInfo, oppdragskontroll, opphørStatusFomForBruker, sisteOppdr150Bruker);
        }
    }

    private void opprettOpphørsoppdragForArbeidsgiver(OppdragInput behandlingInfo, Oppdragskontroll oppdragskontroll) {
        List<Oppdragslinje150> sisteLinjeKjedeForArbeidsgivereListe = TidligereOppdragTjeneste.finnSisteLinjeKjedeForAlleArbeidsgivere(behandlingInfo);

        Map<String, List<Oppdragslinje150>> groupedByOrgnr = TidligereOppdragTjeneste.grupperOppdragslinje150MedOrgnr(sisteLinjeKjedeForArbeidsgivereListe);

        for (Map.Entry<String, List<Oppdragslinje150>> entryOpp150 : groupedByOrgnr.entrySet()) {
            if (OpphørUtil.erArbeidsgiverAllredeFullstendigOpphørt(behandlingInfo, entryOpp150.getKey())) {
                continue;
            }
            List<Oppdragslinje150> sisteOppdr150ForDenneArbgvrenListe = entryOpp150.getValue();
            Oppdragslinje150 sisteOppdr150 = Oppdragslinje150Util.getOpp150MedMaxDelytelseId(sisteOppdr150ForDenneArbgvrenListe);
            LocalDate opphørStatusFom = FinnOpphørFomDato.finnOpphørFomForArbeidsgiver(sisteOppdr150ForDenneArbgvrenListe, behandlingInfo);
            opprettOppdragForOpphørArbeidsgiver(behandlingInfo, oppdragskontroll, opphørStatusFom, sisteOppdr150);
        }
    }

    Optional<Oppdrag110> opprettOppdr150LinjeForFeriepengerOPPH(OppdragInput behandlingInfo, boolean opphFørEndringsoppdrFeriepg,
                                                                Oppdrag110.Builder nyOppdrag110Builder, Oppdrag110 forrigeOppdrag110,
                                                                Oppdragskontroll nyOppdragskontroll) {

        if (!opphFørEndringsoppdrFeriepg || VurderOpphørForFeriepenger.vurder(behandlingInfo, forrigeOppdrag110, null)) {
            Oppdrag110 nyOppdrag110 = nyOppdrag110Builder.medOppdragskontroll(nyOppdragskontroll).build();
            opprettOppdr150LinjeForFeriepengerOPPH(behandlingInfo, nyOppdrag110,
                forrigeOppdrag110, true, opphFørEndringsoppdrFeriepg);
            return Optional.of(nyOppdrag110);
        }
        return Optional.empty();
    }

    void opprettOppdr150LinjeForFeriepengerOPPH(OppdragInput behandlingInfo, boolean opphFørEndringsoppdrFeriepg,
                                                Oppdrag110 nyOppdrag110, Oppdrag110 forrigeOppdrag110) {

        if (!opphFørEndringsoppdrFeriepg || VurderOpphørForFeriepenger.vurder(behandlingInfo, forrigeOppdrag110, null)) {
            opprettOppdr150LinjeForFeriepengerOPPH(behandlingInfo, nyOppdrag110,
                forrigeOppdrag110, true, opphFørEndringsoppdrFeriepg);
        }
    }

    private Oppdrag110 opprettOppdragForOpphørBruker(OppdragInput behandlingInfo, Oppdragskontroll oppdragskontroll, LocalDate
        opphørStatusFom, Oppdragslinje150 sisteOppdr150Bruker) {
        return opprettOppdragForOpphørBruker(behandlingInfo, oppdragskontroll, opphørStatusFom, sisteOppdr150Bruker, false);
    }

    private Oppdrag110 opprettOppdragForOpphørBruker(OppdragInput behandlingInfo, Oppdragskontroll nyOppdragskontroll, LocalDate
        opphørStatusFom, Oppdragslinje150 sisteOppdr150Bruker, boolean opphFørEndringsoppdrFeriepg) {

        Oppdrag110 forrigeOppdrag110 = sisteOppdr150Bruker.getOppdrag110();
        Oppdrag110.Builder oppdrag110Builder = OpprettOppdrag110Tjeneste.opprettOppdrag110MedRelaterteOppdragsmeldinger(behandlingInfo, sisteOppdr150Bruker,
            opprettMottakerInstance(behandlingInfo, sisteOppdr150Bruker));
        Oppdrag110 nyOppdrag110 = oppdrag110Builder.medOppdragskontroll(nyOppdragskontroll).build();
        opprettOppdragslinje150ForStatusOPPH(behandlingInfo, sisteOppdr150Bruker, nyOppdrag110, opphørStatusFom);
        opprettOppdr150LinjeForFeriepengerOPPH(behandlingInfo, opphFørEndringsoppdrFeriepg, nyOppdrag110, forrigeOppdrag110);
        TidligereOppdragTjeneste.getOppdragslinje150ForOpphør(nyOppdrag110);
        return nyOppdrag110;
    }

    void opprettOppdr150LinjeForFeriepengerOPPH(OppdragInput behandlingInfo, Oppdrag110 nyOppdrag110,
                                                Oppdrag110 forrigeOppdrag110, boolean erBrukerMottaker,
                                                boolean opphFørEndringsoppdrFeriepg) {

        List<Oppdragslinje150> tidligereOpp150List = TidligereOppdragTjeneste.hentAlleTidligereOppdragslinje150(behandlingInfo, forrigeOppdrag110);
        Map<Integer, List<Oppdragslinje150>> opp150PrFeriepengeårMap = Oppdragslinje150FeriepengerUtil.finnSisteOpp150ForFeriepenger(tidligereOpp150List);

        boolean erFeriepengerBeregningNullForDenneOpp150År = true;
        for (Map.Entry<Integer, List<Oppdragslinje150>> opp150FeriepengerPrFeriepengeÅr : opp150PrFeriepengeårMap.entrySet()) {
            Optional<Oppdragslinje150> opp150FeriepengerOpt = opp150FeriepengerPrFeriepengeÅr.getValue().stream()
                .filter(Oppdragslinje150::gjelderOpphør)
                .findFirst();
            boolean finnesOpphørForDenneOpp150 = opp150FeriepengerOpt.isPresent();
            Oppdragslinje150 oppdr150Feriepenger = opp150FeriepengerPrFeriepengeÅr.getValue()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Utvikler feil: Mangler forrige oppdragslinje150"));
            if (opphFørEndringsoppdrFeriepg) {
                erFeriepengerBeregningNullForDenneOpp150År = VurderFeriepengerBeregning.erFeriepengerBeregningNullForGittÅret(behandlingInfo, oppdr150Feriepenger, erBrukerMottaker);
            }
            if (!finnesOpphørForDenneOpp150 && erFeriepengerBeregningNullForDenneOpp150År) {
                LocalDate førsteUttaksDag = oppdr150Feriepenger.getDatoVedtakFom();
                opprettOppdragslinje150ForStatusOPPH(behandlingInfo, oppdr150Feriepenger, nyOppdrag110, førsteUttaksDag, true);
            }
        }
    }

    void kobleAndreMeldingselementerTilOpp150Opphør(OppdragInput behandlingInfo, Oppdragslinje150 sisteOppdr150, List<Oppdragslinje150> opp150OpphList) {
        opp150OpphList.forEach(nyOppdragslinje150 -> {
            Refusjonsinfo156 forrigeRefusjonsinfo156 = sisteOppdr150.getRefusjonsinfo156();
            OpprettOppdragsmeldingerRelatertTil150.opprettRefusjonsinfo156(behandlingInfo, nyOppdragslinje150, forrigeRefusjonsinfo156);
        });
    }

    private void opprettOppdragForOpphørArbeidsgiver(OppdragInput behandlingInfo, Oppdragskontroll nyOppdragskontroll,
                                                     LocalDate opphørStatusFom, Oppdragslinje150 sisteOppdr150) {

        Oppdrag110 forrigeOppdrag110 = sisteOppdr150.getOppdrag110();
        Oppdrag110.Builder oppdrag110Builder = OpprettOppdrag110Tjeneste.opprettOppdrag110MedRelaterteOppdragsmeldinger(behandlingInfo, sisteOppdr150,
            opprettMottakerInstance(behandlingInfo, sisteOppdr150));
        Oppdrag110 nyOppdrag110 = oppdrag110Builder.medOppdragskontroll(nyOppdragskontroll).build();
        opprettOppdragslinje150ForStatusOPPH(behandlingInfo, sisteOppdr150, nyOppdrag110, opphørStatusFom);
        opprettOppdr150LinjeForFeriepengerOPPH(behandlingInfo, nyOppdrag110, forrigeOppdrag110,
            false, false);
        List<Oppdragslinje150> opp150OpphList = TidligereOppdragTjeneste.getOppdragslinje150ForOpphør(nyOppdrag110);
        kobleAndreMeldingselementerTilOpp150Opphør(behandlingInfo, sisteOppdr150, opp150OpphList);
    }

    Oppdragslinje150 opprettOppdragslinje150ForStatusOPPH(OppdragInput behandlingInfo, Oppdragslinje150 forrigeOppdr150, Oppdrag110
        oppdrag110, LocalDate datoStatusFom) {

        return opprettOppdragslinje150ForStatusOPPH(behandlingInfo, forrigeOppdr150, oppdrag110, datoStatusFom, false);
    }

    private Oppdragslinje150 opprettOppdragslinje150ForStatusOPPH(OppdragInput behandlingInfo, Oppdragslinje150 forrigeOppdr150, Oppdrag110
        oppdrag110, LocalDate datoStatusFom, boolean gjelderFeriepenger) {
        LocalDate vedtakFom = forrigeOppdr150.getDatoVedtakFom();
        LocalDate vedtakTom = forrigeOppdr150.getDatoVedtakTom();
        Long delytelseId = forrigeOppdr150.getDelytelseId();
        KodeKlassifik kodeKlassifik = forrigeOppdr150.getKodeKlassifik();

        Oppdragslinje150.Builder oppdragslinje150Builder = Oppdragslinje150.builder();
        OpprettOppdragslinje150Tjeneste.settFellesFelterIOppdr150(behandlingInfo, oppdragslinje150Builder, true, gjelderFeriepenger);
        oppdragslinje150Builder
            .medDatoStatusFom(datoStatusFom)
            .medDelytelseId(delytelseId)
            .medKodeKlassifik(kodeKlassifik)
            .medVedtakFomOgTom(vedtakFom, vedtakTom)
            .medSats(forrigeOppdr150.getSats())
            .medUtbetalesTilId(forrigeOppdr150.getUtbetalesTilId())
            .medOppdrag110(oppdrag110);

        if (!gjelderFeriepenger) {
            int grad = Optional.ofNullable(forrigeOppdr150.getUtbetalingsgrad()).map(Utbetalingsgrad::getVerdi).orElse(100);
            oppdragslinje150Builder.medUtbetalingsgrad(Utbetalingsgrad.prosent(grad));
        }
        return oppdragslinje150Builder.build();
    }

    private Oppdragsmottaker opprettMottakerInstance(OppdragInput behandlingInfo, Oppdragslinje150 tidligereOpp150ForMottakeren) {
        if (KodeFagområdeTjenesteProvider.getKodeFagområdeTjeneste(behandlingInfo).gjelderBruker(tidligereOpp150ForMottakeren.getOppdrag110())) {
            Oppdragsmottaker mottaker = new Oppdragsmottaker(behandlingInfo.getPersonIdent().getIdent(), true);
            mottaker.setStatus(OppdragsmottakerStatus.OPPH);
            return mottaker;
        }
        Oppdragsmottaker mottaker = new Oppdragsmottaker(Oppdragslinje150Util.endreTilNiSiffer(tidligereOpp150ForMottakeren.getRefusjonsinfo156().getRefunderesId()), false);
        mottaker.setStatus(OppdragsmottakerStatus.OPPH);
        return mottaker;
    }
}
