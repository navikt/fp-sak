package no.nav.foreldrepenger.tilganger;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppe;


class AnsattInfoKlientMapperTest {

    @Test
    void skalMappeSaksbehandlerGruppeTilKanSaksbehandleRettighet() {
        var brukerUtenforSaksbehandlerGruppe = getTestBruker(false, true, false, false, false);
        var brukerISaksbehandlerGruppe = getTestSaksbehandler(false, false);

        var innloggetBrukerUtenSaksbehandlerRettighet = AnsattInfoKlient.mapTilDomene(brukerUtenforSaksbehandlerGruppe);
        var innloggetBrukerMedSaksbehandlerRettighet = AnsattInfoKlient.mapTilDomene(brukerISaksbehandlerGruppe);

        assertThat(innloggetBrukerUtenSaksbehandlerRettighet.kanSaksbehandle()).isFalse();
        assertThat(innloggetBrukerMedSaksbehandlerRettighet.kanSaksbehandle()).isTrue();
    }

    @Test
    void skalMappeVeilederGruppeTilKanVeiledeRettighet() {
        var brukerUtenforVeilederGruppe = getTestBruker(false, false, false, false, false);
        var brukerIVeilederGruppe = getTestBruker(false, true, false, false, false);

        var innloggetBrukerUtenVeilederRettighet = AnsattInfoKlient.mapTilDomene(brukerUtenforVeilederGruppe);
        var innloggetBrukerMedVeilederRettighet = AnsattInfoKlient.mapTilDomene(brukerIVeilederGruppe);

        assertThat(innloggetBrukerUtenVeilederRettighet.kanVeilede()).isFalse();
        assertThat(innloggetBrukerMedVeilederRettighet.kanVeilede()).isTrue();
    }

    @Test
    void skalMappeOverstyrerGruppeTilKanOverstyreRettighet() {
        var brukerUtenforOverstyrerGruppe = getTestSaksbehandler(false, false);
        var brukerIOverstyrerGruppe = getTestSaksbehandler(true, false);

        var innloggetBrukerUtenOverstyrerRettighet = AnsattInfoKlient.mapTilDomene(brukerUtenforOverstyrerGruppe);
        var innloggetBrukerMedOverstyrerRettighet = AnsattInfoKlient.mapTilDomene(brukerIOverstyrerGruppe);

        assertThat(innloggetBrukerUtenOverstyrerRettighet.kanOverstyre()).isFalse();
        assertThat(innloggetBrukerMedOverstyrerRettighet.kanOverstyre()).isTrue();
    }

    @Test
    void skalMappeKode6GruppeTilKanBehandleKode6Rettighet() {
        var brukerUtenforKode6Gruppe = getTestSaksbehandler(false, false);
        var brukerIKode6Gruppe = getTestSaksbehandler( false, true);

        var innloggetBrukerUtenKode6Rettighet = AnsattInfoKlient.mapTilDomene(brukerUtenforKode6Gruppe);
        var innloggetBrukerMedKode6Rettighet = AnsattInfoKlient.mapTilDomene( brukerIKode6Gruppe);

        assertThat(innloggetBrukerUtenKode6Rettighet.kanBehandleKode6()).isFalse();
        assertThat(innloggetBrukerMedKode6Rettighet.kanBehandleKode6()).isTrue();
    }

    private static InnloggetNavAnsatt getTestSaksbehandler(boolean kanOverstyre, boolean kanBehandleKode6) {
        return getTestBruker(true, true, kanOverstyre, false, kanBehandleKode6);
    }

    private static InnloggetNavAnsatt getTestBruker(boolean kanSaksbehandle, boolean kanveilede, boolean kanOverstyre,
                                                    boolean kanOppgavestyre, boolean kanBehandleKode6) {
        Set<AnsattGruppe> grupper = new LinkedHashSet<>();
        if (kanSaksbehandle) grupper.add(AnsattGruppe.SAKSBEHANDLER);
        if (kanveilede) grupper.add(AnsattGruppe.VEILEDER);
        if (kanOverstyre) grupper.add(AnsattGruppe.OVERSTYRER);
        if (kanOppgavestyre) grupper.add(AnsattGruppe.OPPGAVESTYRER);
        if (kanBehandleKode6) grupper.add(AnsattGruppe.STRENGTFORTROLIG);
        return new InnloggetNavAnsatt("T123456", "Test Bruker", grupper);
    }

}
