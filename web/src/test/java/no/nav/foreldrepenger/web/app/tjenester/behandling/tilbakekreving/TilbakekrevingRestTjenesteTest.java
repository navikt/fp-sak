package no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingValg;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class TilbakekrevingRestTjenesteTest {

    private BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);
    private TilbakekrevingRepository tilbakekrevingRepository = mock(TilbakekrevingRepository.class);
    private TilbakekrevingRestTjeneste tilbakekrevingRestTjeneste = new TilbakekrevingRestTjeneste(behandlingRepository, tilbakekrevingRepository);

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() {
        when(behandlingRepository.hentBehandling((Long) Mockito.any())).thenAnswer(invocation -> lagBehandling());
        when(behandlingRepository.hentBehandling((UUID) Mockito.any())).thenAnswer(invocation -> lagBehandling());
    }

    @Test
    public void skal_hentTilbakekrevingValg_når_tilbakekrevingsvalg_finnes() {
        when(tilbakekrevingRepository.hent(Mockito.any())).thenReturn(Optional.of(TilbakekrevingValg.medMulighetForInntrekk(true, true, TilbakekrevingVidereBehandling.INNTREKK)));
        TilbakekrevingValgDto tilbakekrevingValgDto = tilbakekrevingRestTjeneste.hentTilbakekrevingValg(new UuidDto("1098c6f4-4ae2-4794-8a23-9224675a1f99"));
        assertThat(tilbakekrevingValgDto).isNotNull();
        assertThat(tilbakekrevingValgDto.erTilbakekrevingVilkårOppfylt()).isTrue();
    }

    @Test
    public void skal_feil_hentTilbakekrevingValg_når_tilbakekrevingsvalg_ikke_finnes() {
        when(tilbakekrevingRepository.hent(Mockito.any())).thenReturn(Optional.empty());
        TilbakekrevingValgDto tilbakekrevingValgDto = tilbakekrevingRestTjeneste.hentTilbakekrevingValg(new UuidDto("1098c6f4-4ae2-4794-8a23-9224675a1f99"));
        assertThat(tilbakekrevingValgDto).isNull();
    }

    @Test
    public void skal_hente_varseltekst_ved_henting_av_tilbakekrevingsvalg() {
        String forventetVarselTekst = "varseltekst her";
        when(tilbakekrevingRepository.hent(Mockito.any())).thenReturn(Optional.of(TilbakekrevingValg.utenMulighetForInntrekk(TilbakekrevingVidereBehandling.INNTREKK, forventetVarselTekst)));
        TilbakekrevingValgDto tilbakekrevingValgDto = tilbakekrevingRestTjeneste.hentTilbakekrevingValg(new UuidDto("1098c6f4-4ae2-4794-8a23-9224675a1f99"));
        assertThat(tilbakekrevingValgDto.getVarseltekst()).isEqualTo(forventetVarselTekst);

    }

    private Behandling lagBehandling() {
        Personinfo personinfo = new Personinfo.Builder()
            .medAktørId(AktørId.dummy())
            .medPersonIdent(new PersonIdent("12345678901"))
            .medNavBrukerKjønn(NavBrukerKjønn.KVINNE)
            .medNavn("navn")
            .medFødselsdato(LocalDate.now().minusYears(25))
            .medForetrukketSpråk(Språkkode.nb)
            .build();
        NavBruker navBruker = NavBruker.opprettNy(personinfo);
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, navBruker, RelasjonsRolleType.MORA, new Saksnummer("123456"));
        return Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
    }

}
