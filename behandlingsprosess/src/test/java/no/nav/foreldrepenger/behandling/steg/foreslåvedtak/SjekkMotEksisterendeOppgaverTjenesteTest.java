package no.nav.foreldrepenger.behandling.steg.foreslåvedtak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Tema;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgave;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgavestatus;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgavetype;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Prioritet;

@ExtendWith(MockitoExtension.class)
class SjekkMotEksisterendeOppgaverTjenesteTest {

    private static final AktørId AKTØR_ID = AktørId.dummy();

    @Mock
    private OppgaveTjeneste oppgaveTjenesteMock;
    @InjectMocks
    private SjekkMotEksisterendeOppgaverTjeneste sjekkMotEksisterendeOppgaverTjeneste;

    @Test
    void henter_aksjonspunkter_for_behandling() {
        var behandling = mock(Behandling.class);
        var vurderDokumentOppgave = opprettOppgave(Oppgavetype.VURDER_DOKUMENT);
        var vurderKonsekvensOppgave = opprettOppgave(Oppgavetype.VURDER_KONSEKVENS_YTELSE);
        when(oppgaveTjenesteMock.hentÅpneVurderDokumentOgVurderKonsekvensOppgaver(AKTØR_ID)).thenReturn(
            List.of(vurderDokumentOppgave, vurderKonsekvensOppgave));

        var aksjonspunktDefinisjoner = sjekkMotEksisterendeOppgaverTjeneste.sjekkMotEksisterendeGsakOppgaver(AKTØR_ID, behandling);

        assertThat(aksjonspunktDefinisjoner).containsOnlyOnce(AksjonspunktDefinisjon.VURDERE_DOKUMENT_FØR_VEDTAK);
        assertThat(aksjonspunktDefinisjoner).containsOnlyOnce(AksjonspunktDefinisjon.VURDERE_ANNEN_YTELSE_FØR_VEDTAK);
    }

    @Test
    void henter_ikke_utførte_aksjonspunkter_for_behandling() {
        var behandling = mock(Behandling.class);
        var aksjonspunkt = mock(Aksjonspunkt.class);
        when(aksjonspunkt.getAksjonspunktDefinisjon()).thenReturn(AksjonspunktDefinisjon.VURDERE_ANNEN_YTELSE_FØR_VEDTAK);
        when(aksjonspunkt.erUtført()).thenReturn(true);
        when(behandling.getAksjonspunkter()).thenReturn(Set.of(aksjonspunkt));

        var aksjonspunktDefinisjoner = sjekkMotEksisterendeOppgaverTjeneste.sjekkMotEksisterendeGsakOppgaver(AKTØR_ID, behandling);

        assertThat(aksjonspunktDefinisjoner).isEmpty();
    }

    @Test
    void mapper_til_unike_aksjonspunkter_for_oppgaver() {
        var vurderDokumentOppgave = opprettOppgave(Oppgavetype.VURDER_DOKUMENT);
        var vurderKonsekvensOppgave1 = opprettOppgave(Oppgavetype.VURDER_KONSEKVENS_YTELSE);
        var vurderKonsekvensOppgave2 = opprettOppgave(Oppgavetype.VURDER_KONSEKVENS_YTELSE);

        var aksjonspunktDefinisjoner = SjekkMotEksisterendeOppgaverTjeneste.mapTilAksjonspunktDefinisjonerForOppgaver(
            List.of(vurderDokumentOppgave, vurderKonsekvensOppgave1, vurderKonsekvensOppgave2));

        assertThat(aksjonspunktDefinisjoner).containsOnlyOnce(AksjonspunktDefinisjon.VURDERE_DOKUMENT_FØR_VEDTAK);
        assertThat(aksjonspunktDefinisjoner).containsOnlyOnce(AksjonspunktDefinisjon.VURDERE_ANNEN_YTELSE_FØR_VEDTAK);
    }

    private static Oppgave opprettOppgave(Oppgavetype oppgavetype) {
        return new Oppgave(99L, null, null, null, null, Tema.FOR.getOffisiellKode(), null, oppgavetype, null, 2, "4805",
            LocalDate.now().plusDays(1), LocalDate.now(), Prioritet.NORM, Oppgavestatus.AAPNET, "beskrivelse", null);
    }
}
