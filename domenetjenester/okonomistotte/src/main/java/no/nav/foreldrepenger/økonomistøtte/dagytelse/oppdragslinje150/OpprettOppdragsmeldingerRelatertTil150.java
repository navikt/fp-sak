package no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdragslinje150;

import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Grad170;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;
import no.nav.foreldrepenger.økonomistøtte.Oppdragsmottaker;
import no.nav.foreldrepenger.økonomistøtte.OpprettOppdragTjeneste;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.OppdragskontrollConstants;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.fp.OppdragInput;

public class OpprettOppdragsmeldingerRelatertTil150 {

    private OpprettOppdragsmeldingerRelatertTil150() {
    }

    public static void opprettAttestant180(List<Oppdragslinje150> oppdragslinje150, String ansvarligSaksbehandler) {
        for (Oppdragslinje150 oppdrLinje150 : oppdragslinje150) {
            OpprettOppdragTjeneste.opprettAttestant180(oppdrLinje150, ansvarligSaksbehandler);
        }
    }

    public static void opprettGrad170(Oppdragslinje150 oppdragslinje150, int grad) {
        Grad170.builder()
            .medTypeGrad(OppdragskontrollConstants.TYPE_GRAD)
            .medGrad(grad)
            .medOppdragslinje150(oppdragslinje150)
            .build();
    }

    public static void opprettRefusjonsinfo156(OppdragInput behandlingInfo, Oppdragslinje150 nyOppdragslinje150, Refusjonsinfo156 forrigeRefusjonsinfo156) {
        Refusjonsinfo156.builder()
            .medDatoFom(behandlingInfo.getVedtaksdato())
            .medMaksDato(forrigeRefusjonsinfo156.getMaksDato())
            .medRefunderesId(forrigeRefusjonsinfo156.getRefunderesId())
            .medOppdragslinje150(nyOppdragslinje150)
            .build();
    }

    static void opprettRefusjonsinfo156(OppdragInput behandlingInfo, Oppdragslinje150 oppdragslinje150, Oppdragsmottaker mottaker, LocalDate maksDato) {
        Refusjonsinfo156.builder()
            .medMaksDato(maksDato)
            .medDatoFom(behandlingInfo.getVedtaksdato())
            .medRefunderesId(Oppdragslinje150Util.endreTilElleveSiffer(mottaker.getId()))
            .medOppdragslinje150(oppdragslinje150)
            .build();
    }
}
