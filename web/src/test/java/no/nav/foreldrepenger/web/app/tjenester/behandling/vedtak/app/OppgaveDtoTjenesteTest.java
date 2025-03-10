package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OppgaveType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Tema;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgave;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgavestatus;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgavetype;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Prioritet;

@ExtendWith(MockitoExtension.class)
class OppgaveDtoTjenesteTest {

    private static final AktørId AKTØR_ID = AktørId.dummy();

    @Mock
    private OppgaveTjeneste oppgaveTjenesteMock;
    @InjectMocks
    private OppgaveDtoTjeneste oppgaveDtoTjeneste;

    @Test
    void henter_oppgaver_og_mapper_til_dto() {
        Oppgave vurderDokument1 = opprettOppgave(Oppgavetype.VURDER_DOKUMENT, "vurderDokumentBeskrivelse1");
        Oppgave vurderDokument2 = opprettOppgave(Oppgavetype.VURDER_DOKUMENT, "vurderDokumentBeskrivelse2");
        Oppgave vurderKonsekvens1 = opprettOppgave(Oppgavetype.VURDER_KONSEKVENS_YTELSE, "vurderKonsekvensBeskrivelse1");
        Oppgave vurderKonsekvens2 = opprettOppgave(Oppgavetype.VURDER_KONSEKVENS_YTELSE, "vurderKonsekvensBeskrivelse2");
        List<Oppgave> forventedeOppgaver = List.of(vurderDokument1, vurderDokument2, vurderKonsekvens1, vurderKonsekvens2);
        when(oppgaveTjenesteMock.hentÅpneVurderDokumentOgVurderKonsekvensOppgaver(AKTØR_ID)).thenReturn(forventedeOppgaver);

        var oppgaver = oppgaveDtoTjeneste.mapTilDto(AKTØR_ID);

        assertThat(oppgaver).hasSize(4);
        for (int i = 0; i < oppgaver.size(); i++) {
            assertThat(oppgaver.get(i).oppgavetype()).isEqualTo(OppgaveType.fraKode(forventedeOppgaver.get(i).oppgavetype()));
            assertThat(oppgaver.get(i).beskrivelse()).isEqualTo(forventedeOppgaver.get(i).beskrivelse());
        }
    }

    private static Oppgave opprettOppgave(Oppgavetype oppgavetype, String beskrivelse) {
        return new Oppgave(99L, null, null, null, null, Tema.FOR.getOffisiellKode(), null, oppgavetype.getKode(), null, 2, "4805",
            LocalDate.now().plusDays(1), LocalDate.now(), Prioritet.NORM, Oppgavestatus.AAPNET, beskrivelse, null);
    }
}
