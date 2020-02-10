package no.nav.foreldrepenger.økonomi.økonomistøtte;

import java.time.LocalDate;
import java.time.LocalDateTime;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming115;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Grad170;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragsenhet120;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeAksjon;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodekomponent;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiTypeSats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiUtbetFrekvens;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.TfradragTillegg;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.TkodeStatusLinje;


public class OppdragTestDataHelper {
    private static final String KODE_KLASSIFIK_FODSEL = "FPENFOD-OP";

    static Oppdragslinje150 buildOppdragslinje150(Oppdrag110 oppdrag110) {
        Oppdragslinje150.Builder oppdrLinje150Builder = Oppdragslinje150.builder();

        return oppdrLinje150Builder
            .medKodeEndringLinje(TkodeStatusLinje.OPPH.value())
            .medKodeStatusLinje("ENDR")
            .medDatoStatusFom(LocalDate.now())
            .medVedtakId("456")
            .medDelytelseId(64L)
            .medKodeKlassifik(KODE_KLASSIFIK_FODSEL)
            .medVedtakFomOgTom(LocalDate.now(), LocalDate.now())
            .medSats(61122L)
            .medFradragTillegg(TfradragTillegg.F.value())
            .medTypeSats(ØkonomiTypeSats.UKE.name())
            .medBrukKjoreplan("B")
            .medSaksbehId("F2365245")
            .medUtbetalesTilId("123456789")
            .medOppdrag110(oppdrag110)
            .medHenvisning(47L)
            .build();

    }

    static Oppdragsenhet120 buildOppdragsEnhet120(Oppdrag110 oppdrag110) {
        Oppdragsenhet120.Builder oppdrsEnhet120Builder = Oppdragsenhet120.builder();

        return oppdrsEnhet120Builder
            .medTypeEnhet("BOS")
            .medEnhet("8020")
            .medDatoEnhetFom(LocalDate.now())
            .medOppdrag110(oppdrag110)
            .build();
    }

    static Avstemming115 buildAvstemming115() {
        Avstemming115.Builder avst115Builder = Avstemming115.builder();

        return avst115Builder
            .medKodekomponent(ØkonomiKodekomponent.VLFP.getKodekomponent())
            .medNokkelAvstemming(ØkonomistøtteUtils.tilSpesialkodetDatoOgKlokkeslett(LocalDateTime.now()))
            .medTidspnktMelding(ØkonomistøtteUtils.tilSpesialkodetDatoOgKlokkeslett(LocalDateTime.now().minusDays(1)))
            .build();
    }

    public static Oppdrag110 buildOppdrag110ES(Oppdragskontroll oppdragskontroll, Long fagsystemId) {
        Avstemming115 avstemming115 = buildAvstemming115();
        return buildOppdrag110(oppdragskontroll, fagsystemId, avstemming115, ØkonomiKodeFagområde.REFUTG);
    }

    static Oppdrag110 buildOppdrag110ES(Oppdragskontroll oppdragskontroll, Long fagsystemId, Avstemming115 avstemming115) {
        return buildOppdrag110(oppdragskontroll, fagsystemId, avstemming115, ØkonomiKodeFagområde.REFUTG);
    }

    public static Oppdrag110 buildOppdrag110FPBruker(Oppdragskontroll oppdragskontroll, Long fagsystemId) {
        Avstemming115 avstemming115 = buildAvstemming115();
        return buildOppdrag110(oppdragskontroll, fagsystemId, avstemming115, ØkonomiKodeFagområde.FP);
    }

    public static Oppdrag110 buildOppdrag110FPArbeidsgiver(Oppdragskontroll oppdragskontroll, Long fagsystemId) {
        Avstemming115 avstemming115 = buildAvstemming115();
        Oppdrag110 oppdrag110 = buildOppdrag110(oppdragskontroll, fagsystemId, avstemming115, ØkonomiKodeFagområde.FPREF);
        Oppdragslinje150 oppdrag150 = OppdragTestDataHelper.buildOppdragslinje150(oppdrag110);
        OppdragTestDataHelper.buildRefusjonsinfo156(oppdrag150);
        return oppdrag110;
    }

    private static Oppdrag110 buildOppdrag110(Oppdragskontroll oppdragskontroll, Long fagsystemId, Avstemming115 avstemming115, ØkonomiKodeFagområde økonomiKodeFagområde) {
        Oppdrag110.Builder oppdr110Builder = Oppdrag110.builder();

        Oppdrag110.Builder oppdrag110Builder = oppdr110Builder
            .medKodeAksjon(ØkonomiKodeAksjon.TRE.name())
            .medKodeEndring(ØkonomiKodeEndring.NY.name())
            .medKodeFagomrade(økonomiKodeFagområde.name())
            .medFagSystemId(fagsystemId)
            .medUtbetFrekvens(ØkonomiUtbetFrekvens.DAG.name())
            .medOppdragGjelderId("12345678901")
            .medDatoOppdragGjelderFom(LocalDate.of(2000, 1, 1))
            .medSaksbehId("J5624215")
            .medOppdragskontroll(oppdragskontroll)
            .medAvstemming115(avstemming115);
        return oppdrag110Builder
            .build();
    }

    static void buildGrad170(Oppdragslinje150 oppdragslinje150) {
        Grad170.Builder grad170Builder = Grad170.builder();

        grad170Builder
            .medGrad(100)
            .medTypeGrad("UFOR")
            .medOppdragslinje150(oppdragslinje150)
            .build();
    }

    static void buildRefusjonsinfo156(Oppdragslinje150 oppdragslinje150) {
        Refusjonsinfo156.Builder refusjonsinfo156Builder = Refusjonsinfo156.builder();

        refusjonsinfo156Builder
            .medMaksDato(LocalDate.now())
            .medDatoFom(LocalDate.now())
            .medRefunderesId("123456789")
            .medOppdragslinje150(oppdragslinje150)
            .build();
    }

    public static Oppdragskontroll buildOppdragskontroll() {
        return buildOppdragskontroll(new Saksnummer("35"), 128L);
    }

    static Oppdragskontroll buildOppdragskontroll(Saksnummer saksnummer, long behandlingId) {
        return buildOppdragskontroll(saksnummer, behandlingId, 56L);
    }

    static Oppdragskontroll buildOppdragskontroll(Saksnummer saksnummer, long behandlingId, long prosessTaskId) {
        Oppdragskontroll.Builder oppdrkontrollBuilder = Oppdragskontroll.builder();

        return oppdrkontrollBuilder
            .medBehandlingId(behandlingId)
            .medSaksnummer(saksnummer)
            .medVenterKvittering(Boolean.TRUE)
            .medProsessTaskId(prosessTaskId)
            .build();
    }
}
