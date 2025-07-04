package no.nav.foreldrepenger.mottak.vurderfagsystem.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlendeFagsystem;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Journalpost;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystem;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesTjeneste;

@ExtendWith(MockitoExtension.class)
class VurderFagsystemFellesTjenesteTest {

    private VurderFagsystemFellesTjeneste vurderFagsystemFellesTjeneste;
    @Mock
    private FagsakTjeneste fagsakTjenesteMock;

    private Fagsak fagsakFødselES = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, BehandlingslagerTestUtil.lagNavBruker(), null,
            VurderFagsystemTestUtils.ÅPEN_SAKSNUMMER_1);

    @BeforeEach
    void setup() {
        vurderFagsystemFellesTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjenesteMock, null, null);
    }

    @Test
    void skal_returnere_vl_med_saknsummer_hvis_journalpost_allerede_er_journaltført_på_vl_sak() {
        var vurderFagsystem = new VurderFagsystem();
        vurderFagsystem.setBehandlingTema(BehandlingTema.ENGANGSSTØNAD_FØDSEL);
        var journalpostId = new JournalpostId(123L);
        vurderFagsystem.setJournalpostId(journalpostId);

        var journalpost = new Journalpost(journalpostId, fagsakFødselES);
        when(fagsakTjenesteMock.hentJournalpost(any())).thenReturn(Optional.of(journalpost));

        var result = vurderFagsystemFellesTjeneste.vurderFagsystem(vurderFagsystem);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).contains(fagsakFødselES.getSaksnummer());
    }
}
