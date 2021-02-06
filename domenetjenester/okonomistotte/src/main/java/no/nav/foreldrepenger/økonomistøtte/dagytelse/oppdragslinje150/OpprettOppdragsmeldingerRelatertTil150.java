package no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdragslinje150;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;
import no.nav.foreldrepenger.økonomistøtte.Oppdragsmottaker;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.fp.OppdragInput;

public class OpprettOppdragsmeldingerRelatertTil150 {

    private OpprettOppdragsmeldingerRelatertTil150() {}

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
