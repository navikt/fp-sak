package no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingValg;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.økonomistøtte.simulering.klient.FpOppdragRestKlient;

@ExtendWith(MockitoExtension.class)
class TilbakekrevingRestTjenesteTest {

    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private TilbakekrevingRepository tilbakekrevingRepository;
    @Mock
    private FpOppdragRestKlient fpOppdragRestKlient;
    private TilbakekrevingRestTjeneste tilbakekrevingRestTjeneste;

    @BeforeEach
    void setup() {
        tilbakekrevingRestTjeneste = new TilbakekrevingRestTjeneste(behandlingRepository, tilbakekrevingRepository, fpOppdragRestKlient);
        lenient().when(behandlingRepository.hentBehandling((Long) Mockito.any())).thenAnswer(invocation -> lagBehandling());
        when(behandlingRepository.hentBehandling((UUID) Mockito.any())).thenAnswer(invocation -> lagBehandling());
    }

    @Test
    void skal_hentTilbakekrevingValg_når_tilbakekrevingsvalg_finnes() {
        when(tilbakekrevingRepository.hent(Mockito.any()))
                .thenReturn(Optional.of(TilbakekrevingValg.medMulighetForInntrekk(true, true, TilbakekrevingVidereBehandling.INNTREKK)));
        var tilbakekrevingValgDto = tilbakekrevingRestTjeneste
                .hentTilbakekrevingValg(new UuidDto("1098c6f4-4ae2-4794-8a23-9224675a1f99"));
        assertThat(tilbakekrevingValgDto).isNotNull();
        assertThat(tilbakekrevingValgDto.erTilbakekrevingVilkårOppfylt()).isTrue();
    }

    @Test
    void skal_feil_hentTilbakekrevingValg_når_tilbakekrevingsvalg_ikke_finnes() {
        when(tilbakekrevingRepository.hent(Mockito.any())).thenReturn(Optional.empty());
        var tilbakekrevingValgDto = tilbakekrevingRestTjeneste
                .hentTilbakekrevingValg(new UuidDto("1098c6f4-4ae2-4794-8a23-9224675a1f99"));
        assertThat(tilbakekrevingValgDto).isNull();
    }

    @Test
    void skal_hente_varseltekst_ved_henting_av_tilbakekrevingsvalg() {
        var forventetVarselTekst = "varseltekst her";
        when(tilbakekrevingRepository.hent(Mockito.any()))
                .thenReturn(Optional.of(TilbakekrevingValg.utenMulighetForInntrekk(TilbakekrevingVidereBehandling.INNTREKK, forventetVarselTekst)));
        var tilbakekrevingValgDto = tilbakekrevingRestTjeneste
                .hentTilbakekrevingValg(new UuidDto("1098c6f4-4ae2-4794-8a23-9224675a1f99"));
        assertThat(tilbakekrevingValgDto.getVarseltekst()).isEqualTo(forventetVarselTekst);

    }

    private Behandling lagBehandling() {
        var navBruker = NavBruker.opprettNyNB(AktørId.dummy());
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, navBruker, RelasjonsRolleType.MORA, new Saksnummer("123456"));
        return Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
    }

}
