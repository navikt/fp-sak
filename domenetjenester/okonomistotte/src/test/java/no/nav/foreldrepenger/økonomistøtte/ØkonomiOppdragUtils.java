package no.nav.foreldrepenger.økonomistøtte;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Sats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.TypeSats;

class ØkonomiOppdragUtils {

    static TypeSats hentTypeSats(Boolean gjelderFP) {
        if (gjelderFP) {
            return TypeSats.DAG;
        }
        return TypeSats.ENG;
    }

    static KodeKlassifik hentKodeKlassifik(Boolean gjelderFP) {
        if (gjelderFP) {
            return KodeKlassifik.FPF_ARBEIDSTAKER;
        }
        return KodeKlassifik.ES_FØDSEL;
    }

    static KodeFagområde hentKodeFagomrade(Boolean gjelderFP, Boolean brukerErMottaker) {
        if (gjelderFP) {
            return brukerErMottaker ? BehandleØkonomioppdragKvitteringTest.KODEFAGOMRADE_FP : BehandleØkonomioppdragKvitteringTest.KODEFAGOMRADE_FPREF;
        }
        return BehandleØkonomioppdragKvitteringTest.KODEFAGOMRADE_ES;
    }

    static void leggTilRefusjons156(List<Oppdragslinje150> o150Liste) {
        for (var o150 : o150Liste) {
            Refusjonsinfo156.builder()
                .medMaksDato(LocalDate.now())
                .medRefunderesId(BehandleØkonomioppdragKvitteringTest.REFUNDERES_ID)
                .medDatoFom(LocalDate.now())
                .medOppdragslinje150(o150)
                .build();
        }
    }

    static void setupOppdrag110(Oppdragskontroll oppdrag, Boolean gjelderFP) {
        var o110_1 = new Oppdrag110.Builder()
            .medKodeEndring(BehandleØkonomioppdragKvitteringTest.KODEENDRING)
            .medKodeFagomrade(hentKodeFagomrade(gjelderFP, true))
            .medFagSystemId(BehandleØkonomioppdragKvitteringTest.FAGSYSTEMID_BRUKER)
            .medOppdragGjelderId(BehandleØkonomioppdragKvitteringTest.OPPDRAGGJELDERID)
            .medSaksbehId(BehandleØkonomioppdragKvitteringTest.SAKSBEHID)
            .medAvstemming(Avstemming.ny())
            .medOppdragskontroll(oppdrag)
            .build();
        var builder = new Oppdragslinje150.Builder()
            .medVedtakId(BehandleØkonomioppdragKvitteringTest.VEDTAKID)
            .medDelytelseId(101002100100L)
            .medKodeEndringLinje(KodeEndringLinje.NY)
            .medKodeKlassifik(hentKodeKlassifik(gjelderFP))
            .medVedtakFomOgTom(LocalDate.now(), LocalDate.now())
            .medSats(Sats.på(654L))
            .medTypeSats(hentTypeSats(gjelderFP))
            .medUtbetalesTilId(BehandleØkonomioppdragKvitteringTest.OPPDRAGGJELDERID)
            .medOppdrag110(o110_1);
        if (gjelderFP) {
            builder.medUtbetalingsgrad(Utbetalingsgrad.prosent(BehandleØkonomioppdragKvitteringTest.GRAD));
        }
        builder.build();

        if (gjelderFP) {
            var o110_2 = new Oppdrag110.Builder()
                .medAvstemming(Avstemming.ny())
                .medKodeEndring(BehandleØkonomioppdragKvitteringTest.KODEENDRING)
                .medKodeFagomrade(hentKodeFagomrade(gjelderFP, false))
                .medFagSystemId(BehandleØkonomioppdragKvitteringTest.FAGSYSTEMID_ARBEIDSGIVER)
                .medOppdragGjelderId(BehandleØkonomioppdragKvitteringTest.OPPDRAGGJELDERID)
                .medSaksbehId(BehandleØkonomioppdragKvitteringTest.SAKSBEHID)
                .medOppdragskontroll(oppdrag)
                .build();
            var o150_2 = new Oppdragslinje150.Builder()
                .medVedtakId(BehandleØkonomioppdragKvitteringTest.VEDTAKID)
                .medDelytelseId(101002101100L)
                .medKodeEndringLinje(KodeEndringLinje.NY)
                .medKodeKlassifik(hentKodeKlassifik(gjelderFP))
                .medVedtakFomOgTom(LocalDate.now(), LocalDate.now())
                .medSats(Sats.på(654L))
                .medTypeSats(hentTypeSats(gjelderFP))
                .medOppdrag110(o110_2)
                .medUtbetalingsgrad(Utbetalingsgrad.prosent(BehandleØkonomioppdragKvitteringTest.GRAD))
                .build();
            leggTilRefusjons156(Collections.singletonList(o150_2));

        }
    }
}
