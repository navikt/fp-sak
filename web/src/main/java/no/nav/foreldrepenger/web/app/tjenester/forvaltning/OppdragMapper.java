package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeAksjon;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeStatusLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiUtbetFrekvens;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.oppdrag.OppdragPatchDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.oppdrag.OppdragslinjePatchDto;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OpprettOppdragTjeneste;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.OppdragskontrollConstants;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.oppdragslinje150.Oppdragslinje150Util;

class OppdragMapper {

    private final OppdragPatchDto dto;

    private final Behandling behandling;
    private final String fnrBruker;

    public OppdragMapper(OppdragPatchDto dto, Behandling behandling, String fnrBruker) {
        this.dto = dto;
        this.behandling = behandling;
        this.fnrBruker = fnrBruker;
    }

    public void mapTil(Oppdragskontroll oppdragskontroll) {
        Oppdrag110 oppdrag110 = mapOppdrag110(oppdragskontroll);
        for (OppdragslinjePatchDto linje : dto.getOppdragslinjer()) {
            mapOppdragslinje(oppdrag110, linje);
        }
    }

    private LocalDate finnSisteDato() {
        return dto.getOppdragslinjer()
            .stream()
            .map(OppdragslinjePatchDto::getTom)
            .max(LocalDate::compareTo)
            .orElseThrow();
    }

    private void mapOppdragslinje(Oppdrag110 oppdrag110, OppdragslinjePatchDto linje) {
        Oppdragslinje150.Builder builder = Oppdragslinje150.builder()
            .medOppdrag110(oppdrag110)
            .medFradragTillegg(OppdragskontrollConstants.FRADRAG_TILLEGG)
            .medBrukKjoreplan(OppdragskontrollConstants.BRUK_KJOREPLAN)
            .medSaksbehId(behandling.getAnsvarligSaksbehandler())
            .medHenvisning(behandling.getId())
            .medVedtakFomOgTom(linje.getFom(), linje.getTom())
            .medSats(linje.getSats())
            .medTypeSats(linje.getSatsType())
            .medDelytelseId(linje.getDelytelseId())
            .medRefDelytelseId(linje.getRefDelytelseId())
            .medRefFagsystemId(linje.getRefFagsystemId())
            .medVedtakId(behandling.getOriginalVedtaksDato().toString())
            .medKodeEndringLinje(linje.getKodeEndring())
            .medKodeKlassifik(linje.getKodeKlassifik());
        if (linje.getOpphørFom() != null) {
            builder.medDatoStatusFom(linje.getOpphørFom());
            builder.medKodeStatusLinje(ØkonomiKodeStatusLinje.OPPH.name());
        }
        if (dto.erBrukerMottaker()) {
            builder.medUtbetalesTilId(fnrBruker);
        }
        Oppdragslinje150 oppdragslinje = builder.build();
        if (!dto.erBrukerMottaker()) {
            Refusjonsinfo156.builder()
                .medOppdragslinje150(oppdragslinje)
                .medRefunderesId(Oppdragslinje150Util.endreTilElleveSiffer(dto.getArbeidsgiverOrgNr()))
                .medDatoFom(behandling.getOriginalVedtaksDato())
                .medMaksDato(finnSisteDato())
                .build();
        }
    }

    private Oppdrag110 mapOppdrag110(Oppdragskontroll oppdragskontroll) {
        return Oppdrag110.builder()
            .medOppdragskontroll(oppdragskontroll)
            .medAvstemming115(OpprettOppdragTjeneste.opprettAvstemming115())
            .medKodeAksjon(ØkonomiKodeAksjon.EN.getKodeAksjon())
            .medKodeEndring(dto.getKodeEndring())
            .medDatoOppdragGjelderFom(LocalDate.of(2000, 1, 1))
            .medKodeFagomrade(utledFagområde(behandling, dto.erBrukerMottaker()).name())
            .medOppdragGjelderId(fnrBruker)
            .medFagSystemId(dto.getFagsystemId())
            .medSaksbehId(behandling.getAnsvarligSaksbehandler())
            .medUtbetFrekvens(ØkonomiUtbetFrekvens.MÅNED.getUtbetFrekvens())
            .build();
    }

    private ØkonomiKodeFagområde utledFagområde(Behandling behandling, boolean erBrukerMottaker) {
        switch (behandling.getFagsakYtelseType()) {
            case ENGANGSTØNAD:
                if (!erBrukerMottaker) {
                    throw new IllegalArgumentException("Engangstønad skal kun utbetales til bruker");
                }
                return ØkonomiKodeFagområde.REFUTG;
            case FORELDREPENGER:
                return erBrukerMottaker ? ØkonomiKodeFagområde.FP : ØkonomiKodeFagområde.FPREF;
            case SVANGERSKAPSPENGER:
                return erBrukerMottaker ? ØkonomiKodeFagområde.SVP : ØkonomiKodeFagområde.SVPREF;
            default:
                throw new IllegalArgumentException("Ukjent ytelsetype i behandlingId=" + behandling.getId());
        }
    }
}
