package no.nav.foreldrepenger.økonomi.økonomistøtte;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Attestant180;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming115;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Grad170;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragsenhet120;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;

class ØkonomiOppdragUtils {

    static String hentTypeSats(Boolean gjelderFP) {
        if (gjelderFP) {
            return BehandleØkonomioppdragKvitteringTest.TYPESATS_FP;
        }
        return BehandleØkonomioppdragKvitteringTest.TYPESATS_ES;
    }

    static String hentKodeKlassifik(Boolean gjelderFP) {
        if (gjelderFP) {
            return BehandleØkonomioppdragKvitteringTest.KODEKLASSIFIK_FP;
        }
        return BehandleØkonomioppdragKvitteringTest.KODEKLASSIFIK_ES;
    }

    static String hentKodeFagomrade(Boolean gjelderFP, Boolean brukerErMottaker) {
        if (gjelderFP) {
            return brukerErMottaker ? BehandleØkonomioppdragKvitteringTest.KODEFAGOMRADE_FP : BehandleØkonomioppdragKvitteringTest.KODEFAGOMRADE_FPREF;
        }
        return BehandleØkonomioppdragKvitteringTest.KODEFAGOMRADE_ES;
    }

    static void leggTilRefusjons156(List<Oppdragslinje150> o150Liste) {
        for (Oppdragslinje150 o150 : o150Liste) {
            Refusjonsinfo156.builder()
                .medMaksDato(LocalDate.now())
                .medRefunderesId(BehandleØkonomioppdragKvitteringTest.REFUNDERES_ID)
                .medDatoFom(LocalDate.now())
                .medOppdragslinje150(o150)
                .build();
        }
    }

    static void leggTilGrad170(List<Oppdragslinje150> o150Liste) {
        for (Oppdragslinje150 o150 : o150Liste) {
            Grad170.builder()
                .medGrad(BehandleØkonomioppdragKvitteringTest.GRAD)
                .medTypeGrad(BehandleØkonomioppdragKvitteringTest.TYPE_GRAD)
                .medOppdragslinje150(o150)
                .build();
        }
    }

    static void setupOppdrag110(Oppdragskontroll oppdrag, Avstemming115 a115, Boolean gjelderFP) {
        Oppdrag110 o110_1 = new Oppdrag110.Builder()
            .medAvstemming115(a115)
            .medKodeAksjon(BehandleØkonomioppdragKvitteringTest.KODEAKSJON)
            .medKodeEndring(BehandleØkonomioppdragKvitteringTest.KODEENDRING)
            .medKodeFagomrade(hentKodeFagomrade(gjelderFP, true))
            .medFagSystemId(BehandleØkonomioppdragKvitteringTest.FAGSYSTEMID_BRUKER)
            .medUtbetFrekvens(BehandleØkonomioppdragKvitteringTest.UTBETFREKVENS)
            .medOppdragGjelderId(BehandleØkonomioppdragKvitteringTest.OPPDRAGGJELDERID)
            .medDatoOppdragGjelderFom(LocalDate.now())
            .medSaksbehId(BehandleØkonomioppdragKvitteringTest.SAKSBEHID)
            .medOppdragskontroll(oppdrag)
            .build();
        new Oppdragsenhet120.Builder()
            .medTypeEnhet(BehandleØkonomioppdragKvitteringTest.TYPEENHET)
            .medDatoEnhetFom(LocalDate.now())
            .medEnhet(BehandleØkonomioppdragKvitteringTest.ENHET)
            .medOppdrag110(o110_1)
            .build();
        Oppdragslinje150 o150_1 = new Oppdragslinje150.Builder()
            .medVedtakId(BehandleØkonomioppdragKvitteringTest.VEDTAKID)
            .medDelytelseId(101002100100L)
            .medKodeEndringLinje(BehandleØkonomioppdragKvitteringTest.KODEENDRINGLINJE)
            .medKodeKlassifik(hentKodeKlassifik(gjelderFP))
            .medVedtakFomOgTom(LocalDate.now(), LocalDate.now())
            .medSats(BehandleØkonomioppdragKvitteringTest.SATS)
            .medFradragTillegg(BehandleØkonomioppdragKvitteringTest.FRADRAGTILLEGG)
            .medTypeSats(hentTypeSats(gjelderFP))
            .medBrukKjoreplan("N")
            .medSaksbehId(BehandleØkonomioppdragKvitteringTest.SAKSBEHID)
            .medUtbetalesTilId(BehandleØkonomioppdragKvitteringTest.OPPDRAGGJELDERID)
            .medHenvisning(gjelderFP ? BehandleØkonomioppdragKvitteringTest.BEHANDLINGID_FP : BehandleØkonomioppdragKvitteringTest.BEHANDLINGID_ES)
            .medOppdrag110(o110_1)
            .build();
        Attestant180.builder()
            .medAttestantId(BehandleØkonomioppdragKvitteringTest.SAKSBEHID)
            .medOppdragslinje150(o150_1)
            .build();
        if (gjelderFP) {
            leggTilGrad170(Collections.singletonList(o150_1));
        }

        if (gjelderFP) {
            Oppdrag110 o110_2 = new Oppdrag110.Builder()
                .medAvstemming115(a115)
                .medKodeAksjon(BehandleØkonomioppdragKvitteringTest.KODEAKSJON)
                .medKodeEndring(BehandleØkonomioppdragKvitteringTest.KODEENDRING)
                .medKodeFagomrade(hentKodeFagomrade(gjelderFP, false))
                .medFagSystemId(BehandleØkonomioppdragKvitteringTest.FAGSYSTEMID_ARBEIDSGIVER)
                .medUtbetFrekvens(BehandleØkonomioppdragKvitteringTest.UTBETFREKVENS)
                .medOppdragGjelderId(BehandleØkonomioppdragKvitteringTest.OPPDRAGGJELDERID)
                .medDatoOppdragGjelderFom(LocalDate.now())
                .medSaksbehId(BehandleØkonomioppdragKvitteringTest.SAKSBEHID)
                .medOppdragskontroll(oppdrag)
                .build();
            new Oppdragsenhet120.Builder()
                .medTypeEnhet(BehandleØkonomioppdragKvitteringTest.TYPEENHET)
                .medDatoEnhetFom(LocalDate.now())
                .medEnhet(BehandleØkonomioppdragKvitteringTest.ENHET)
                .medOppdrag110(o110_2)
                .build();
            Oppdragslinje150 o150_2 = new Oppdragslinje150.Builder()
                .medVedtakId(BehandleØkonomioppdragKvitteringTest.VEDTAKID)
                .medDelytelseId(101002101100L)
                .medKodeEndringLinje(BehandleØkonomioppdragKvitteringTest.KODEENDRINGLINJE)
                .medKodeKlassifik(hentKodeKlassifik(gjelderFP))
                .medVedtakFomOgTom(LocalDate.now(), LocalDate.now())
                .medSats(BehandleØkonomioppdragKvitteringTest.SATS)
                .medFradragTillegg(BehandleØkonomioppdragKvitteringTest.FRADRAGTILLEGG)
                .medTypeSats(hentTypeSats(gjelderFP))
                .medBrukKjoreplan("N")
                .medSaksbehId(BehandleØkonomioppdragKvitteringTest.SAKSBEHID)
                .medHenvisning(gjelderFP ? BehandleØkonomioppdragKvitteringTest.BEHANDLINGID_FP : BehandleØkonomioppdragKvitteringTest.BEHANDLINGID_ES)
                .medOppdrag110(o110_2)
                .build();
            Attestant180.builder()
                .medAttestantId(BehandleØkonomioppdragKvitteringTest.SAKSBEHID)
                .medOppdragslinje150(o150_2)
                .build();
            leggTilGrad170(Collections.singletonList(o150_2));
            leggTilRefusjons156(Collections.singletonList(o150_2));

        }
    }
}
