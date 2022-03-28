package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.time.LocalDate;
import java.time.LocalDateTime;

import no.nav.foreldrepenger.behandling.impl.FinnAnsvarligSaksbehandler;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Ompostering116;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Sats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeStatusLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.TypeSats;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.oppdrag.OppdragPatchDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.oppdrag.OppdragslinjePatchDto;
import no.nav.foreldrepenger.økonomistøtte.ny.util.OppdragOrgnrUtil;
import no.nav.foreldrepenger.økonomistøtte.ØkonomistøtteUtils;

public class OppdragMapper {

    private final OppdragPatchDto dto;

    private final Behandling behandling;
    private final String fnrBruker;
    private final String ansvarligSaksbehandler;
    private final BehandlingVedtak behandlingVedtak;

    public OppdragMapper(OppdragPatchDto dto, Behandling behandling, String fnrBruker, BehandlingVedtak behandlingVedtak) {
        this.dto = dto;
        this.behandling = behandling;
        this.fnrBruker = fnrBruker;
        ansvarligSaksbehandler = FinnAnsvarligSaksbehandler.finn(behandling);
        this.behandlingVedtak = behandlingVedtak;
    }

    public void mapTil(Oppdragskontroll oppdragskontroll) {
        var oppdrag110 = mapOppdrag110(oppdragskontroll);
        for (var linje : dto.getOppdragslinjer()) {
            mapOppdragslinje(oppdrag110, linje);
        }
    }

    private LocalDate finnSisteDato() {
        return dto.getOppdragslinjer().stream().map(OppdragslinjePatchDto::getTom).max(LocalDate::compareTo).orElseThrow();
    }

    private void mapOppdragslinje(Oppdrag110 oppdrag110, OppdragslinjePatchDto linje) {
        var builder = Oppdragslinje150.builder()
            .medOppdrag110(oppdrag110)
            .medVedtakFomOgTom(linje.getFom(), linje.getTom())
            .medSats(Sats.på(linje.getSats()))
            .medTypeSats(TypeSats.valueOf(linje.getSatsType()))
            .medDelytelseId(linje.getDelytelseId())
            .medRefDelytelseId(linje.getRefDelytelseId())
            .medRefFagsystemId(linje.getRefFagsystemId())
            .medVedtakId(behandlingVedtak.getVedtaksdato().toString())
            .medKodeEndringLinje(KodeEndringLinje.valueOf(linje.getKodeEndring()))
            .medKodeKlassifik(KodeKlassifik.fraKode(linje.getKodeKlassifik()));
        if (linje.getOpphørFom() != null) {
            builder.medDatoStatusFom(linje.getOpphørFom());
            builder.medKodeStatusLinje(KodeStatusLinje.OPPH);
        }
        if (dto.erBrukerMottaker()) {
            builder.medUtbetalesTilId(fnrBruker);
        }
        var oppdragslinje = builder.build();
        if (!dto.erBrukerMottaker()) {
            Refusjonsinfo156.builder()
                .medOppdragslinje150(oppdragslinje)
                .medRefunderesId(OppdragOrgnrUtil.endreTilElleveSiffer(dto.getArbeidsgiverOrgNr()))
                .medDatoFom(behandlingVedtak.getVedtaksdato())
                .medMaksDato(finnSisteDato())
                .build();
        }
    }

    private Oppdrag110 mapOppdrag110(Oppdragskontroll oppdragskontroll) {
        return Oppdrag110.builder()
            .medOppdragskontroll(oppdragskontroll)
            .medAvstemming(Avstemming.ny())
            .medKodeEndring(KodeEndring.valueOf(dto.getKodeEndring()))
            .medKodeFagomrade(utledFagområde(behandling, dto.erBrukerMottaker()))
            .medOppdragGjelderId(fnrBruker)
            .medFagSystemId(dto.getFagsystemId())
            .medSaksbehId(ansvarligSaksbehandler)
            .medOmpostering116(mapOmpostering116())
            .build();
    }

    private Ompostering116 mapOmpostering116() {
        if (dto.taMedOmpostering116()) {
            var erAvslåttInntrekk = dto.getOmposterFom() == null;
            var builder = new Ompostering116.Builder().medTidspktReg(ØkonomistøtteUtils.tilSpesialkodetDatoOgKlokkeslett(LocalDateTime.now()))
                .medOmPostering(!erAvslåttInntrekk);
            if (!erAvslåttInntrekk) {
                builder.medDatoOmposterFom(dto.getOmposterFom());
            }
            return builder.build();
        }
        return null;
    }

    private KodeFagområde utledFagområde(Behandling behandling, boolean erBrukerMottaker) {
        switch (behandling.getFagsakYtelseType()) {
            case ENGANGSTØNAD:
                if (!erBrukerMottaker) {
                    throw new ForvaltningException("Engangstønad skal kun utbetales til bruker");
                }
                return KodeFagområde.ENGANGSSTØNAD;
            case FORELDREPENGER:
                return erBrukerMottaker ? KodeFagområde.FORELDREPENGER_BRUKER : KodeFagområde.FORELDREPENGER_ARBEIDSGIVER;
            case SVANGERSKAPSPENGER:
                return erBrukerMottaker ? KodeFagområde.SVANGERSKAPSPENGER_BRUKER : KodeFagområde.SVANGERSKAPSPENGER_ARBEIDSGIVER;
            default:
                throw new ForvaltningException("Ukjent ytelsetype i behandlingId=" + behandling.getId());
        }
    }
}
