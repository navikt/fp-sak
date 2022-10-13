package no.nav.foreldrepenger.web.app.tjenester.historikk;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.historikk.dto.HistorikkinnslagDelDto;
import no.nav.foreldrepenger.historikk.dto.HistorikkinnslagDto;
import no.nav.foreldrepenger.historikk.dto.HistorikkinnslagHendelseDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.historikk.HistorikkRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;

@ExtendWith(MockitoExtension.class)
public class HistorikkRestTjenesteTest {

    @Mock
    private HistorikkTjenesteAdapter historikkApplikasjonTjenesteMock;
    private HistorikkRestTjeneste historikkRestTjeneste;

    @BeforeEach
    public void setUp() {
        historikkRestTjeneste = new HistorikkRestTjeneste(historikkApplikasjonTjenesteMock);
    }

    @Test
    public void hentAlleInnslag() {
        var innslagDto = new HistorikkinnslagDto();
        lagHistorikkinnslagDel(innslagDto);
        innslagDto.setDokumentLinks(Collections.emptyList());
        when(historikkApplikasjonTjenesteMock.hentAlleHistorikkInnslagForSak(Mockito.any(Saksnummer.class), any()))
                .thenReturn(Collections.singletonList(innslagDto));

        historikkRestTjeneste.hentAlleInnslag(null, new SaksnummerDto("1234"));

        verify(historikkApplikasjonTjenesteMock).hentAlleHistorikkInnslagForSak(Mockito.any(Saksnummer.class), any());
    }

    private void lagHistorikkinnslagDel(HistorikkinnslagDto innslagDto) {
        var delDto = new HistorikkinnslagDelDto();
        lagHendelseDto(delDto);
        innslagDto.setHistorikkinnslagDeler(Collections.singletonList(delDto));
    }

    private void lagHendelseDto(HistorikkinnslagDelDto delDto) {
        var hendelseDto = new HistorikkinnslagHendelseDto();
        hendelseDto.setNavn(HistorikkinnslagType.BEH_STARTET);
        delDto.setHendelse(hendelseDto);
    }
}
