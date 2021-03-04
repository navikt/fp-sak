package no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdrag110;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Ompostering116;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomistøtte.Oppdragsmottaker;
import no.nav.foreldrepenger.økonomistøtte.OpprettOppdragTjeneste;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.FinnMottakerInfoITilkjentYtelse;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.fp.OppdragInput;
import no.nav.foreldrepenger.økonomistøtte.ØkonomistøtteUtils;

public class OpprettOppdrag110Tjeneste {

    private OpprettOppdrag110Tjeneste() {
        // Skjuler default
    }

    public static Oppdrag110 opprettNyOppdrag110(OppdragInput behandlingInfo, Oppdragskontroll oppdragskontroll,
                                                 Oppdragsmottaker mottaker, long fagsystemId) {

        Oppdrag110.Builder oppdrag110Builder = opprettOppdrag110Builder(behandlingInfo, mottaker,
            true, fagsystemId);
        Oppdrag110 oppdrag110 = oppdrag110Builder.medOppdragskontroll(oppdragskontroll).build();

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
        }
        return nyOppdrag110;
    }

    public static Oppdrag110.Builder opprettOppdrag110MedRelaterteOppdragsmeldinger(OppdragInput behandlingInfo,
                                                                                    Oppdragslinje150 sisteOppdr150ForMottakeren,
                                                                                    Oppdragsmottaker mottaker) {
        Oppdrag110 forrigeOppdrag110 = sisteOppdr150ForMottakeren.getOppdrag110();
        long fagsystemId = forrigeOppdrag110.getFagsystemId();

        return opprettOppdrag110Builder(behandlingInfo,
            mottaker, false, fagsystemId);
    }

    private static Oppdrag110.Builder opprettOppdrag110Builder(OppdragInput behandlingInfo,
                                                               Oppdragsmottaker mottaker, boolean erNyMottakerIEndring, long fagsystemId) {

        KodeEndring kodeEndring = ØkonomiKodeEndringUtleder.finnKodeEndring(behandlingInfo, mottaker, erNyMottakerIEndring);
        KodeFagområde kodeFagområde = KodeFagområdeTjenesteProvider.getKodeFagområdeTjeneste(behandlingInfo).finn(mottaker.erBruker());
        Oppdrag110.Builder builder = Oppdrag110.builder()
            .medKodeEndring(kodeEndring)
            .medKodeFagomrade(kodeFagområde)
            .medFagSystemId(fagsystemId)
            .medOppdragGjelderId(behandlingInfo.getPersonIdent().getIdent())
            .medSaksbehId(behandlingInfo.getAnsvarligSaksbehandler())
            .medAvstemming(Avstemming.ny());

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
            .medTidspktReg(ØkonomistøtteUtils.tilSpesialkodetDatoOgKlokkeslett(LocalDateTime.now()))
            .medOmPostering(!erAvslåttInntrekk);
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
