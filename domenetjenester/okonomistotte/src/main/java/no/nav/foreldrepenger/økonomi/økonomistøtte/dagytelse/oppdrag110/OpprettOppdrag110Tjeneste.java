package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.oppdrag110;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming115;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Ompostering116;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeAksjon;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiUtbetFrekvens;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomi.økonomistøtte.Oppdragsmottaker;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OpprettOppdragTjeneste;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.FinnMottakerInfoITilkjentYtelse;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.OppdragskontrollConstants;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.OppdragInput;
import no.nav.foreldrepenger.økonomi.økonomistøtte.ØkonomistøtteUtils;

public class OpprettOppdrag110Tjeneste {

    private OpprettOppdrag110Tjeneste() {
        // Skjuler default
    }

    public static Oppdrag110 opprettNyOppdrag110(OppdragInput behandlingInfo, Oppdragskontroll oppdragskontroll,
                                                 Oppdragsmottaker mottaker, long fagsystemId) {

        Avstemming115 avstemming115 = OpprettOppdragTjeneste.opprettAvstemming115();
        Oppdrag110.Builder oppdrag110Builder = opprettOppdrag110Builder(behandlingInfo, avstemming115, mottaker,
            true, fagsystemId);
        Oppdrag110 oppdrag110 = oppdrag110Builder.medOppdragskontroll(oppdragskontroll).build();
        OpprettOppdragTjeneste.opprettOppdragsenhet120(oppdrag110);

        return oppdrag110;
    }

    public static Oppdrag110 fastsettOppdrag110(OppdragInput behandlingInfo, Oppdragskontroll oppdragskontroll,
                                                Optional<Oppdrag110> nyOppdrag110Opt, Oppdragslinje150 tidligereOpp150ForMottakeren,
                                                Oppdragsmottaker oppdragsmottaker) {
        Oppdrag110 nyOppdrag110;
        if (nyOppdrag110Opt.isPresent()) {
            nyOppdrag110 = nyOppdrag110Opt.get();
        } else {
            Oppdrag110.Builder oppdrag110Builder = opprettOppdrag110MedRelaterteOppdragsmeldinger(behandlingInfo, tidligereOpp150ForMottakeren, oppdragsmottaker);
            nyOppdrag110 = oppdrag110Builder.medOppdragskontroll(oppdragskontroll).build();
            OpprettOppdragTjeneste.opprettOppdragsenhet120(nyOppdrag110);
        }
        return nyOppdrag110;
    }

    public static Oppdrag110.Builder opprettOppdrag110MedRelaterteOppdragsmeldinger(OppdragInput behandlingInfo,
                                                                                    Oppdragslinje150 sisteOppdr150ForMottakeren,
                                                                                    Oppdragsmottaker mottaker) {

        Avstemming115 avstemming115 = OpprettOppdragTjeneste.opprettAvstemming115();
        Oppdrag110 forrigeOppdrag110 = sisteOppdr150ForMottakeren.getOppdrag110();
        long fagsystemId = forrigeOppdrag110.getFagsystemId();

        return opprettOppdrag110Builder(behandlingInfo,
            avstemming115, mottaker, false, fagsystemId);
    }

    private static Oppdrag110.Builder opprettOppdrag110Builder(OppdragInput behandlingInfo, Avstemming115 avstemming115,
                                                               Oppdragsmottaker mottaker, boolean erNyMottakerIEndring, long fagsystemId) {

        String kodeEndring = ØkonomiKodeEndringUtleder.finnKodeEndring(behandlingInfo, mottaker, erNyMottakerIEndring);
        String kodeFagområde = KodeFagområdeTjenesteProvider.getKodeFagområdeTjeneste(behandlingInfo).finn(mottaker.erBruker());
        Oppdrag110.Builder builder = Oppdrag110.builder()
            .medKodeAksjon(ØkonomiKodeAksjon.EN.getKodeAksjon())
            .medKodeEndring(kodeEndring)
            .medKodeFagomrade(kodeFagområde)
            .medFagSystemId(fagsystemId)
            .medUtbetFrekvens(ØkonomiUtbetFrekvens.MÅNED.getUtbetFrekvens())
            .medOppdragGjelderId(behandlingInfo.getPersonIdent().getIdent())
            .medDatoOppdragGjelderFom(LocalDate.of(2000, 1, 1))
            .medSaksbehId(behandlingInfo.getAnsvarligSaksbehandler())
            .medAvstemming115(avstemming115);

        opprettOmpostering116(behandlingInfo, mottaker).ifPresent(builder::medOmpostering116);
        return builder;
    }

    private static Optional<Ompostering116> opprettOmpostering116(OppdragInput behandlingInfo, Oppdragsmottaker mottaker) {
        if (mottaker.erBruker() && mottaker.erStatusEndret() && FinnMottakerInfoITilkjentYtelse.erBrukerMottakerIForrigeTilkjentYtelse(behandlingInfo)) {
            return opprettOmpostering116(behandlingInfo);
        }
        return Optional.empty();
    }

    private static Optional<Ompostering116> opprettOmpostering116(OppdragInput behandlingInfo) {
        boolean erAvslåttInntrekk = behandlingInfo.isAvslåttInntrekk();
        Ompostering116.Builder ompostering116Builder = new Ompostering116.Builder()
            .medSaksbehId(behandlingInfo.getAnsvarligSaksbehandler())
            .medTidspktReg(ØkonomistøtteUtils.tilSpesialkodetDatoOgKlokkeslett(LocalDateTime.now()))
            .medOmPostering(erAvslåttInntrekk ? OppdragskontrollConstants.OMPOSTERING_N : OppdragskontrollConstants.OMPOSTERING_J);
        if (!erAvslåttInntrekk) {
            LocalDate datoOmposterFom = finnDatoOmposterFom(behandlingInfo);
            ompostering116Builder.medDatoOmposterFom(datoOmposterFom);
        }
        return Optional.of(ompostering116Builder.build());
    }

    private static LocalDate finnDatoOmposterFom(OppdragInput behandlingInfo) {
        LocalDate endringsdato = behandlingInfo.getEndringsdato()
            .orElseThrow(() -> new IllegalStateException("Utvikler feil: Mangler endringsdato for revurdering"));

        LocalDate korrigeringsdato = FinnMottakerInfoITilkjentYtelse.førsteUttaksdatoBrukersForrigeBehandling(behandlingInfo);

        return korrigeringsdato.isAfter(endringsdato)
            ? korrigeringsdato
            : endringsdato;
    }

    public static long settFagsystemId(Saksnummer saksnummer, long initialLøpenummer, boolean gjelderEndring) {
        if (gjelderEndring) {
            return OpprettOppdragTjeneste.incrementInitialValue(initialLøpenummer);
        }
        long saksnummerLong = Long.parseLong(saksnummer.getVerdi());
        return OpprettOppdragTjeneste.genererFagsystemId(saksnummerLong, initialLøpenummer);
    }
}
