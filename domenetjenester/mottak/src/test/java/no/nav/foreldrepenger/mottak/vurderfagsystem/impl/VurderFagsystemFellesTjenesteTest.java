package no.nav.foreldrepenger.mottak.vurderfagsystem.impl;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import no.nav.foreldrepenger.behandling.BehandlendeFagsystem;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Journalpost;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystem;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesTjeneste;

public class VurderFagsystemFellesTjenesteTest {

    private VurderFagsystemFellesTjeneste vurderFagsystemFellesTjeneste;
    private FagsakTjeneste fagsakTjenesteMock;

    private Fagsak fagsakFødselES = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, BehandlingslagerTestUtil.lagNavBruker(), null, VurderFagsystemTestUtils.ÅPEN_SAKSNUMMER_1);


    @Before
    public void setup() {
        fagsakTjenesteMock = mock(FagsakTjeneste.class);
        vurderFagsystemFellesTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjenesteMock, null, null);
    }

    @Test
    public void skal_returnere_vl_med_saknsummer_hvis_journalpost_allerede_er_journaltført_på_vl_sak() {
        VurderFagsystem vurderFagsystem = new VurderFagsystem();
        vurderFagsystem.setBehandlingTema(BehandlingTema.ENGANGSSTØNAD_FØDSEL);
        JournalpostId journalpostId = new JournalpostId(123L);
        vurderFagsystem.setJournalpostId(journalpostId);

        Journalpost journalpost = new Journalpost(journalpostId, fagsakFødselES);
        when(fagsakTjenesteMock.hentJournalpost(any())).thenReturn(Optional.of(journalpost));

        BehandlendeFagsystem result = vurderFagsystemFellesTjeneste.vurderFagsystem(vurderFagsystem);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer().get()).isEqualTo(fagsakFødselES.getSaksnummer());
    }
}
