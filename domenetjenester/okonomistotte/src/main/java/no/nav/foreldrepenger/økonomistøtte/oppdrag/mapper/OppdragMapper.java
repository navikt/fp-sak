package no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper;

import static no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper.OmposteringUtil.erOpphørForMottaker;
import static no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper.OmposteringUtil.harGjeldendeUtbetalingerFraTidligere;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Ompostering116;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Sats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeStatusLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.TypeSats;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Betalingsmottaker;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.KjedeNøkkel;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Oppdrag;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.OppdragLinje;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.SatsType;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.samlinger.MottakerOppdragKjedeOversikt;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.samlinger.OverordnetOppdragKjedeOversikt;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.util.OppdragOrgnrUtil;
import no.nav.foreldrepenger.økonomistøtte.ØkonomistøtteUtils;

public class OppdragMapper {

    private final OppdragInput input;
    private final String fnrBruker;
    private final String ansvarligSaksbehandler;
    private final OverordnetOppdragKjedeOversikt tidligereOppdrag;

    public OppdragMapper(String fnrBruker, OverordnetOppdragKjedeOversikt tidligereOppdrag, OppdragInput input) {
        this.fnrBruker = fnrBruker;
        this.tidligereOppdrag = tidligereOppdrag;
        this.input = input;
        this.ansvarligSaksbehandler = input.getAnsvarligSaksbehandler() != null
            ? input.getAnsvarligSaksbehandler()
            : "VL";
    }

    public void mapTilOppdrag110(Oppdrag oppdrag, Oppdragskontroll oppdragskontroll) {
        var builder = Oppdrag110.builder()
            .medOppdragskontroll(oppdragskontroll)
            .medKodeEndring(utledKodeEndring(oppdrag))
            .medKodeFagomrade(oppdrag.getKodeFagområde())
            .medFagSystemId(Long.parseLong(oppdrag.getFagsystemId().toString()))
            .medOppdragGjelderId(fnrBruker)
            .medSaksbehId(ansvarligSaksbehandler)
            .medAvstemming(Avstemming.ny());

        // Det skal prøves å ompostere om:
        // - det er utbetaling til bruker
        // - det er ikke en utbetaling til ny mottaker
        // - mottaker har gjeldende utbetalinger fra tidligere
        // - gjeldende oppdrag fører ikke til full opphør for mottaker
        var tidligerOppdragForMottaker = tidligereOppdrag.filter(oppdrag.getBetalingsmottaker());
        if (oppdrag.getBetalingsmottaker() == Betalingsmottaker.BRUKER
            && !oppdragErTilNyMottaker(oppdrag.getBetalingsmottaker())
            && harGjeldendeUtbetalingerFraTidligere(tidligerOppdragForMottaker)
            && !erOpphørForMottaker(tidligerOppdragForMottaker.utvidMed(oppdrag))) {
            builder.medOmpostering116(opprettOmpostering116(oppdrag, input.brukInntrekk()));
        }

        var oppdrag110 = builder.build();

        for (var entry : oppdrag.getKjeder().entrySet()) {
            var kjedeNøkkel = entry.getKey();
            var refusjonsinfoBuilder = byggRefusjonsinfoBuilderFor(oppdrag, kjedeNøkkel, input.getVedtaksdato());
            for (var oppdragLinje : entry.getValue().getOppdragslinjer()) {
                var oppdragslinje150 = mapTilOppdragslinje150(oppdrag110, kjedeNøkkel, oppdragLinje, input.getVedtaksdato());
                refusjonsinfoBuilder.ifPresent(o156Builder -> o156Builder.medOppdragslinje150(oppdragslinje150).build());
            }
        }
    }

    private Optional<Refusjonsinfo156.Builder> byggRefusjonsinfoBuilderFor(final Oppdrag oppdrag, final KjedeNøkkel kjedeNøkkel, LocalDate vedtaksdato) {
        Refusjonsinfo156.Builder refusjonsinfoBuilder = null;
        if (kjedeNøkkel.getBetalingsmottaker().erArbeidsgiver()) {
            var mottaker = (Betalingsmottaker.ArbeidsgiverOrgnr) kjedeNøkkel.getBetalingsmottaker();
            var gjelderFeriepenger = kjedeNøkkel.getKlassekode().gjelderFeriepenger();
            refusjonsinfoBuilder = Refusjonsinfo156.builder()
                .medDatoFom(vedtaksdato)
                .medMaksDato(gjelderFeriepenger ? kjedeNøkkel.getFeriepengeMaksdato() : hentSisteUtbetalingsdato(oppdrag))
                .medRefunderesId(OppdragOrgnrUtil.endreTilElleveSiffer(mottaker.getOrgnr()));
        }
        return Optional.ofNullable(refusjonsinfoBuilder);
    }

    private boolean oppdragErTilNyMottaker(Betalingsmottaker mottaker) {
        return !tidligereOppdrag.getBetalingsmottakere().contains(mottaker);
    }

    private KodeEndring utledKodeEndring(Oppdrag oppdrag) {
        if (oppdragErTilNyMottaker(oppdrag.getBetalingsmottaker())) {
            return KodeEndring.NY;
        }
        return KodeEndring.ENDR;
    }

    Oppdragslinje150 mapTilOppdragslinje150(Oppdrag110 oppdrag110, KjedeNøkkel kjedeNøkkel, OppdragLinje linje, LocalDate vedtaksdato) {
        var builder = Oppdragslinje150.builder()
            .medOppdrag110(oppdrag110)
            .medDelytelseId(Long.valueOf(linje.getDelytelseId().toString()))
            .medKodeKlassifik(kjedeNøkkel.getKlassekode())
            .medVedtakFomOgTom(linje.getPeriode().getFom(), linje.getPeriode().getTom())
            .medSats(Sats.på(linje.getSats().getSats()))
            .medTypeSats(mapTypeSatsFraDomene(linje.getSats().getSatsType()))
            .medVedtakId(vedtaksdato.toString());

        if (linje.erOpphørslinje()) {
            builder.medKodeEndringLinje(KodeEndringLinje.ENDR);
            builder.medKodeStatusLinje(KodeStatusLinje.OPPH);
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

        if (linje.getUtbetalingsgrad() != null) {
            builder.medUtbetalingsgrad(Utbetalingsgrad.prosent(linje.getUtbetalingsgrad().getUtbetalingsgrad()));
        }

        return builder.build();
    }

    private TypeSats mapTypeSatsFraDomene(SatsType satsType) {
        return switch (satsType) {
            case DAG -> TypeSats.DAG;
            case ENG -> TypeSats.ENG;
        };
    }

    private Ompostering116 opprettOmpostering116(Oppdrag oppdrag, boolean brukInntrekk) {
        var ompostering116Builder = new Ompostering116.Builder()
            .medTidspktReg(ØkonomistøtteUtils.tilSpesialkodetDatoOgKlokkeslett(LocalDateTime.now()))
            .medOmPostering(brukInntrekk);
        if (brukInntrekk) {
            ompostering116Builder.medDatoOmposterFom(finnDatoOmposterFom(oppdrag));
        }
        return ompostering116Builder.build();
    }

    private LocalDate finnDatoOmposterFom(Oppdrag oppdrag) {
        var førsteDatoMedUtbetaling = hentFørsteUtbetalingsdatoFraForrige(oppdrag);
        // var endringsdato = oppdrag.getEndringsdato();
        // Gjelder følgende scenario:
        // Tidslinnjer:
        // Behandling 1 ----------[xxxxxxxxxx]-- tidligere ytelses periode
        // Behandling 2 -----[xxxxxxxxxxxx]----- ny forlenget ytelse
        // Endringsdato blir satt til Beh. 2 fom dato - dette vil føre til at økonomi vil klage på dette siden de ikke har noen utbetalinger fra denne datoen ennå.
        // Derfor må omposteringsdato settes til første uttaksdag fra forrige utbetaling - Beh. 1 fom.
        // Case 2:
        // Behandling 1 -----[xxxxxxxxxxxx]----- tidligere ytelses periode
        // Behandling 2 -----[xxxx]-[xxxxxxxxxx]-- ny ytelse med en helg uten periode
        // Endringsdato blir satt til første dagen i helgen - bør egentlig være satt til fom av Beh 1.
        return førsteDatoMedUtbetaling; // != null && endringsdato.isBefore(førsteDatoMedUtbetaling) ? førsteDatoMedUtbetaling : endringsdato;
    }

    private LocalDate hentFørsteUtbetalingsdatoFraForrige(Oppdrag nyttOppdrag) {
        var tidligerOppdragForMottaker = tidligereOppdrag.filter(nyttOppdrag.getBetalingsmottaker());
        return hentFørsteUtbetalingsdato(tidligerOppdragForMottaker);
    }

    private LocalDate hentFørsteUtbetalingsdato(MottakerOppdragKjedeOversikt oppdrag) {
        LocalDate førsteUtetalingsdato = null;
        for (var entry : oppdrag.getKjeder().entrySet()) {
            var nøkkel = entry.getKey();
            if (nøkkel.getKlassekode().gjelderFeriepenger()) {
                continue;
            }
            var kjede = entry.getValue();
            var perioder = kjede.tilYtelse().getPerioder();
            if (!perioder.isEmpty()) {
                var førstePeriode = perioder.get(0);
                var fom = førstePeriode.getPeriode().getFom();
                if (førsteUtetalingsdato == null || fom.isBefore(førsteUtetalingsdato)) {
                    førsteUtetalingsdato = fom;
                }
            }
        }
        return førsteUtetalingsdato;
    }

    private LocalDate hentSisteUtbetalingsdato(Oppdrag nyttOppdrag) {
        var tidligerOppdragForMottaker = tidligereOppdrag.filter(nyttOppdrag.getBetalingsmottaker());
        var utvidetMedNyttOppdrag = tidligerOppdragForMottaker.utvidMed(nyttOppdrag);
        var sisteUtbetalingsdato = hentSisteUtbetalingsdato(utvidetMedNyttOppdrag);
        if (sisteUtbetalingsdato != null) {
            return sisteUtbetalingsdato;
        }
        return hentSisteUtbetalingsdato(tidligerOppdragForMottaker);
    }

    private LocalDate hentSisteUtbetalingsdato(MottakerOppdragKjedeOversikt oppdrag) {
        LocalDate sisteUtbetalingsdato = null;
        for (var entry : oppdrag.getKjeder().entrySet()) {
            var nøkkel = entry.getKey();
            if (nøkkel.getKlassekode().gjelderFeriepenger()) {
                continue;
            }
            var kjede = entry.getValue();
            var perioder = kjede.tilYtelse().getPerioder();
            if (!perioder.isEmpty()) {
                var sistePeriode = perioder.get(perioder.size() - 1);
                var tom = sistePeriode.getPeriode().getTom();
                if (sisteUtbetalingsdato == null || tom.isAfter(sisteUtbetalingsdato)) {
                    sisteUtbetalingsdato = tom;
                }
            }
        }
        return sisteUtbetalingsdato;
    }

}
