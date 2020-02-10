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
            return BehandleØkonomioppdragKvitteringImplTest.TYPESATS_FP;
        }
        return BehandleØkonomioppdragKvitteringImplTest.TYPESATS_ES;
    }

    static String hentKodeKlassifik(Boolean gjelderFP) {
        if (gjelderFP) {
            return BehandleØkonomioppdragKvitteringImplTest.KODEKLASSIFIK_FP;
        }
        return BehandleØkonomioppdragKvitteringImplTest.KODEKLASSIFIK_ES;
    }

    static String hentKodeFagomrade(Boolean gjelderFP, Boolean brukerErMottaker) {
        if (gjelderFP) {
            return brukerErMottaker ? BehandleØkonomioppdragKvitteringImplTest.KODEFAGOMRADE_FP : BehandleØkonomioppdragKvitteringImplTest.KODEFAGOMRADE_FPREF;
        }
        return BehandleØkonomioppdragKvitteringImplTest.KODEFAGOMRADE_ES;
    }

    static void leggTilRefusjons156(List<Oppdragslinje150> o150Liste) {
        for (Oppdragslinje150 o150 : o150Liste) {
            Refusjonsinfo156.builder()
                .medMaksDato(LocalDate.now())
                .medRefunderesId(BehandleØkonomioppdragKvitteringImplTest.REFUNDERES_ID)
                .medDatoFom(LocalDate.now())
                .medOppdragslinje150(o150)
                .build();
        }
    }

    static void leggTilGrad170(List<Oppdragslinje150> o150Liste) {
        for (Oppdragslinje150 o150 : o150Liste) {
            Grad170.builder()
                .medGrad(BehandleØkonomioppdragKvitteringImplTest.GRAD)
                .medTypeGrad(BehandleØkonomioppdragKvitteringImplTest.TYPE_GRAD)
                .medOppdragslinje150(o150)
                .build();
        }
    }

    static void setupOppdrag110(Oppdragskontroll oppdrag, Avstemming115 a115, Boolean gjelderFP) {
        Oppdrag110 o110_1 = new Oppdrag110.Builder()
            .medAvstemming115(a115)
            .medKodeAksjon(BehandleØkonomioppdragKvitteringImplTest.KODEAKSJON)
            .medKodeEndring(BehandleØkonomioppdragKvitteringImplTest.KODEENDRING)
            .medKodeFagomrade(hentKodeFagomrade(gjelderFP, true))
            .medFagSystemId(BehandleØkonomioppdragKvitteringImplTest.FAGSYSTEMID_BRUKER)
            .medUtbetFrekvens(BehandleØkonomioppdragKvitteringImplTest.UTBETFREKVENS)
            .medOppdragGjelderId(BehandleØkonomioppdragKvitteringImplTest.OPPDRAGGJELDERID)
            .medDatoOppdragGjelderFom(LocalDate.now())
            .medSaksbehId(BehandleØkonomioppdragKvitteringImplTest.SAKSBEHID)
            .medOppdragskontroll(oppdrag)
            .build();
        new Oppdragsenhet120.Builder()
            .medTypeEnhet(BehandleØkonomioppdragKvitteringImplTest.TYPEENHET)
            .medDatoEnhetFom(LocalDate.now())
            .medEnhet(BehandleØkonomioppdragKvitteringImplTest.ENHET)
            .medOppdrag110(o110_1)
            .build();
        Oppdragslinje150 o150_1 = new Oppdragslinje150.Builder()
            .medVedtakId(BehandleØkonomioppdragKvitteringImplTest.VEDTAKID)
            .medDelytelseId(101002100100L)
            .medKodeEndringLinje(BehandleØkonomioppdragKvitteringImplTest.KODEENDRINGLINJE)
            .medKodeKlassifik(hentKodeKlassifik(gjelderFP))
            .medVedtakFomOgTom(LocalDate.now(), LocalDate.now())
            .medSats(BehandleØkonomioppdragKvitteringImplTest.SATS)
            .medFradragTillegg(BehandleØkonomioppdragKvitteringImplTest.FRADRAGTILLEGG)
            .medTypeSats(hentTypeSats(gjelderFP))
            .medBrukKjoreplan("N")
            .medSaksbehId(BehandleØkonomioppdragKvitteringImplTest.SAKSBEHID)
            .medUtbetalesTilId(BehandleØkonomioppdragKvitteringImplTest.OPPDRAGGJELDERID)
            .medHenvisning(gjelderFP ? BehandleØkonomioppdragKvitteringImplTest.BEHANDLINGID_FP : BehandleØkonomioppdragKvitteringImplTest.BEHANDLINGID_ES)
            .medOppdrag110(o110_1)
            .build();
        Attestant180.builder()
            .medAttestantId(BehandleØkonomioppdragKvitteringImplTest.SAKSBEHID)
            .medOppdragslinje150(o150_1)
            .build();
        if (gjelderFP) {
            leggTilGrad170(Collections.singletonList(o150_1));
        }

        if (gjelderFP) {
            Oppdrag110 o110_2 = new Oppdrag110.Builder()
                .medAvstemming115(a115)
                .medKodeAksjon(BehandleØkonomioppdragKvitteringImplTest.KODEAKSJON)
                .medKodeEndring(BehandleØkonomioppdragKvitteringImplTest.KODEENDRING)
                .medKodeFagomrade(hentKodeFagomrade(gjelderFP, false))
                .medFagSystemId(BehandleØkonomioppdragKvitteringImplTest.FAGSYSTEMID_ARBEIDSGIVER)
                .medUtbetFrekvens(BehandleØkonomioppdragKvitteringImplTest.UTBETFREKVENS)
                .medOppdragGjelderId(BehandleØkonomioppdragKvitteringImplTest.OPPDRAGGJELDERID)
                .medDatoOppdragGjelderFom(LocalDate.now())
                .medSaksbehId(BehandleØkonomioppdragKvitteringImplTest.SAKSBEHID)
                .medOppdragskontroll(oppdrag)
                .build();
            new Oppdragsenhet120.Builder()
                .medTypeEnhet(BehandleØkonomioppdragKvitteringImplTest.TYPEENHET)
                .medDatoEnhetFom(LocalDate.now())
                .medEnhet(BehandleØkonomioppdragKvitteringImplTest.ENHET)
                .medOppdrag110(o110_2)
                .build();
            Oppdragslinje150 o150_2 = new Oppdragslinje150.Builder()
                .medVedtakId(BehandleØkonomioppdragKvitteringImplTest.VEDTAKID)
                .medDelytelseId(101002101100L)
                .medKodeEndringLinje(BehandleØkonomioppdragKvitteringImplTest.KODEENDRINGLINJE)
                .medKodeKlassifik(hentKodeKlassifik(gjelderFP))
                .medVedtakFomOgTom(LocalDate.now(), LocalDate.now())
                .medSats(BehandleØkonomioppdragKvitteringImplTest.SATS)
                .medFradragTillegg(BehandleØkonomioppdragKvitteringImplTest.FRADRAGTILLEGG)
                .medTypeSats(hentTypeSats(gjelderFP))
                .medBrukKjoreplan("N")
                .medSaksbehId(BehandleØkonomioppdragKvitteringImplTest.SAKSBEHID)
                .medHenvisning(gjelderFP ? BehandleØkonomioppdragKvitteringImplTest.BEHANDLINGID_FP : BehandleØkonomioppdragKvitteringImplTest.BEHANDLINGID_ES)
                .medOppdrag110(o110_2)
                .build();
            Attestant180.builder()
                .medAttestantId(BehandleØkonomioppdragKvitteringImplTest.SAKSBEHID)
                .medOppdragslinje150(o150_2)
                .build();
            leggTilGrad170(Collections.singletonList(o150_2));
            leggTilRefusjons156(Collections.singletonList(o150_2));

        }
    }
}
