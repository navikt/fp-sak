package no.nav.foreldrepenger.økonomistøtte.ny.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Ompostering116;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.TypeSats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeAksjon;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiUtbetFrekvens;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.OppdragskontrollConstants;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdragslinje150.Oppdragslinje150Util;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Betalingsmottaker;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.KjedeNøkkel;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Oppdrag;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.OppdragKjede;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.OppdragKjedeFortsettelse;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.OppdragLinje;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.YtelsePeriode;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.samlinger.MottakerOppdragKjedeOversikt;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.samlinger.OverordnetOppdragKjedeOversikt;
import no.nav.foreldrepenger.økonomistøtte.ØkonomistøtteUtils;

public class OppdragMapper {

    private final Input input;
    private final String fnrBruker;
    private final String ansvarligSaksbehandler;
    private final OverordnetOppdragKjedeOversikt tidligereOppdrag;

    public OppdragMapper(String fnrBruker, OverordnetOppdragKjedeOversikt tidligereOppdrag, Input input) {
        this.fnrBruker = fnrBruker;
        this.tidligereOppdrag = tidligereOppdrag;
        this.input = input;
        this.ansvarligSaksbehandler = input.getAnsvarligSaksbehandler() != null
            ? input.getAnsvarligSaksbehandler()
            : "VL";
    }

    public void mapTilOppdrag110(Oppdrag oppdrag, Oppdragskontroll oppdragskontroll) {
        Oppdrag110.Builder builder = Oppdrag110.builder()
            .medOppdragskontroll(oppdragskontroll)
            .medKodeAksjon(ØkonomiKodeAksjon.EN.getKodeAksjon())
            .medKodeEndring(utledKodeEndring(oppdrag).name())
            .medKodeFagomrade(oppdrag.getØkonomiFagområde().name())
            .medFagSystemId(Long.parseLong(oppdrag.getFagsystemId().toString()))
            .medUtbetFrekvens(ØkonomiUtbetFrekvens.MÅNED.getUtbetFrekvens())
            .medOppdragGjelderId(fnrBruker)
            .medDatoOppdragGjelderFom(LocalDate.of(2000, 1, 1))
            .medSaksbehId(ansvarligSaksbehandler)
            .medAvstemming(Avstemming.ny());

        if (oppdrag.getBetalingsmottaker() == Betalingsmottaker.BRUKER && !oppdragErTilNyMottaker(oppdrag) && !erOpphørForMottaker(oppdrag)) {
            builder.medOmpostering116(opprettOmpostering116(oppdrag.getEndringsdato(), input.brukInntrekk(), ansvarligSaksbehandler));
        }

        Oppdrag110 oppdrag110 = builder.build();

        LocalDate maxdatoRefusjon = getMaxdatoRefusjon(oppdrag);

        for (Map.Entry<KjedeNøkkel, OppdragKjedeFortsettelse> entry : oppdrag.getKjeder().entrySet()) {
            for (OppdragLinje oppdragLinje : entry.getValue().getOppdragslinjer()) {
                mapTilOppdragslinje150(oppdrag110, entry.getKey(), oppdragLinje, maxdatoRefusjon, input.getVedtaksdato(), input.getBehandlingId());
            }
        }
    }

    private boolean oppdragErTilNyMottaker(Oppdrag oppdrag) {
        return !tidligereOppdrag.getBetalingsmottakere().contains(oppdrag.getBetalingsmottaker());
    }

    public ØkonomiKodeEndring utledKodeEndring(Oppdrag oppdrag) {
        //usikker på nøyaktig hva som bør sendes her,
        //dette er reverse-engineered fra gammel implementasjon
        if (oppdragErTilNyMottaker(oppdrag)) {
            return ØkonomiKodeEndring.NY;
        }
        if (oppdrag.getBetalingsmottaker() == Betalingsmottaker.BRUKER && !erOpphørForMottaker(oppdrag)) {
            return ØkonomiKodeEndring.ENDR;
        }
        return ØkonomiKodeEndring.UEND;
    }

    Oppdragslinje150 mapTilOppdragslinje150(Oppdrag110 oppdrag110, KjedeNøkkel kjedeNøkkel, OppdragLinje linje, LocalDate maxdatoRefusjon, LocalDate vedtaksdato, Long behandlingId) {
        var builder = Oppdragslinje150.builder()
            .medOppdrag110(oppdrag110)
            .medDelytelseId(Long.valueOf(linje.getDelytelseId().toString()))
            .medKodeKlassifik(kjedeNøkkel.getKlassekode())
            .medVedtakFomOgTom(linje.getPeriode().getFom(), linje.getPeriode().getTom())
            .medSats(linje.getSats().getSats())
            .medTypeSats(TypeSats.fraKode(linje.getSats().getSatsType().getKode()))
            .medVedtakId(vedtaksdato.toString());

        if (linje.erOpphørslinje()) {
            builder.medKodeEndringLinje(KodeEndringLinje.ENDRING);
            builder.medKodeStatusLinje(OppdragskontrollConstants.KODE_STATUS_LINJE_OPPHØR);
            builder.medDatoStatusFom(linje.getOpphørFomDato());
        } else {
            builder.medKodeEndringLinje(KodeEndringLinje.NY);
            if (linje.getRefDelytelseId() != null) {
                builder.medRefDelytelseId(Long.valueOf(linje.getRefDelytelseId().toString()));
                builder.medRefFagsystemId(Long.valueOf(linje.getRefDelytelseId().getFagsystemId().toString()));
            }
        }
        if (kjedeNøkkel.getBetalingsmottaker() == Betalingsmottaker.BRUKER) {
            builder.medUtbetalesTilId(fnrBruker);
        }
        Oppdragslinje150 oppdragslinje150 = builder.build();

        if (kjedeNøkkel.getBetalingsmottaker() instanceof Betalingsmottaker.ArbeidsgiverOrgnr) {
            Betalingsmottaker.ArbeidsgiverOrgnr mottaker = (Betalingsmottaker.ArbeidsgiverOrgnr) kjedeNøkkel.getBetalingsmottaker();
            Refusjonsinfo156.builder()
                .medMaksDato(maxdatoRefusjon)
                .medDatoFom(vedtaksdato)
                .medRefunderesId(Oppdragslinje150Util.endreTilElleveSiffer(mottaker.getOrgnr()))
                .medOppdragslinje150(oppdragslinje150)
                .build();
        }
        if (linje.getUtbetalingsgrad() != null) {
            builder.medUtbetalingsgrad(Utbetalingsgrad.prosent(linje.getUtbetalingsgrad().getUtbetalingsgrad()));
        }
        return oppdragslinje150;
    }

    static Ompostering116 opprettOmpostering116(LocalDate endringsdatoBruker, boolean brukInntrekk, String ansvarligSaksbehandler) {
        Ompostering116.Builder ompostering116Builder = new Ompostering116.Builder()
            .medSaksbehId(ansvarligSaksbehandler)
            .medTidspktReg(ØkonomistøtteUtils.tilSpesialkodetDatoOgKlokkeslett(LocalDateTime.now()))
            .medOmPostering(brukInntrekk ? "J" : "N");
        if (brukInntrekk) {
            ompostering116Builder.medDatoOmposterFom(endringsdatoBruker);
        }
        return ompostering116Builder.build();
    }


    private LocalDate getMaxdatoRefusjon(Oppdrag nyttOppdrag) {
        MottakerOppdragKjedeOversikt tidligerOppdragForMottaker = tidligereOppdrag.filter(nyttOppdrag.getBetalingsmottaker());
        MottakerOppdragKjedeOversikt utvidetMedNyttOppdrag = tidligerOppdragForMottaker.utvidMed(nyttOppdrag);
        LocalDate sisteUtbetalingsdato = hentSisteUtbetalingsdato(utvidetMedNyttOppdrag);
        if (sisteUtbetalingsdato != null) {
            return sisteUtbetalingsdato;
        }
        //usikker på hvorfor... men ved opphør brukes siste utbetalingsdato for forrige oppdrag
        return hentSisteUtbetalingsdato(tidligerOppdragForMottaker);
    }

    private LocalDate hentSisteUtbetalingsdato(MottakerOppdragKjedeOversikt oppdrag) {
        LocalDate sisteUtbetalingsdato = null;
        for (Map.Entry<KjedeNøkkel, OppdragKjede> entry : oppdrag.getKjeder().entrySet()) {
            KjedeNøkkel nøkkel = entry.getKey();
            if (nøkkel.getKlassekode().gjelderFerie()) {
                //TODO?? kan være bedre å inkludere feriepenger for å få mer nøyaktig angivelse av tid for mottaker
                continue;
            }
            OppdragKjede kjede = entry.getValue();
            List<YtelsePeriode> perioder = kjede.tilYtelse().getPerioder();
            if (!perioder.isEmpty()) {
                YtelsePeriode sistePeriode = perioder.get(perioder.size() - 1);
                LocalDate tom = sistePeriode.getPeriode().getTom();
                if (sisteUtbetalingsdato == null || tom.isAfter(sisteUtbetalingsdato)) {
                    sisteUtbetalingsdato = tom;
                }
            }
        }
        return sisteUtbetalingsdato;
    }

    private boolean erOpphørForMottaker(Oppdrag nyttOppdrag) {
        MottakerOppdragKjedeOversikt tidligerOppdragForMottaker = tidligereOppdrag.filter(nyttOppdrag.getBetalingsmottaker());
        MottakerOppdragKjedeOversikt inklNyttOppdrag = tidligerOppdragForMottaker.utvidMed(nyttOppdrag);
        for (OppdragKjede kjede : inklNyttOppdrag.getKjeder().values()) {
            if (!kjede.tilYtelse().getPerioder().isEmpty()) {
                return false;
            }
        }
        return true;
    }

}
