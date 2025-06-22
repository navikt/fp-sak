package no.nav.foreldrepenger.økonomistøtte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomioppdragRepository;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.tjeneste.OppdragskontrollTjenesteImpl;
import no.nav.foreldrepenger.økonomistøtte.simulering.klient.OppdragsKontrollDtoMapper;

@ExtendWith(MockitoExtension.class)
class SimulerOppdragTjenesteESTest {

    private SimulerOppdragTjeneste simulerOppdragTjeneste;

    @Mock
    private PersoninfoAdapter personinfoAdapter;
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private BehandlingVedtakRepository behandlingVedtakRepository;
    @Mock
    private ØkonomioppdragRepository økonomioppdragRepository;
    @Mock
    private LegacyESBeregningRepository beregningRepository;
    @Mock
    private FamilieHendelseRepository familieHendelseRepository;
    @Mock
    private TilbakekrevingRepository tilbakekrevingRepository;

    private long behandlingId;

    @BeforeEach
    void setup() {
        var behandling = Behandling.nyBehandlingFor(
            Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, NavBruker.opprettNyNB(AktørId.dummy()), new Saksnummer("123456789")),
            BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandlingId = 1234L;

        when(behandlingRepository.hentBehandling(behandlingId)).thenReturn(behandling);
        when(behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandlingId)).thenReturn(Optional.of(BehandlingVedtak.builder()
            .medVedtakstidspunkt(LocalDateTime.now())
            .medAnsvarligSaksbehandler("VL")
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .build()));
        when(personinfoAdapter.hentFnrForAktør(any())).thenReturn(PersonIdent.fra("0987654321"));
        when(beregningRepository.getSisteBeregning(behandlingId)).thenReturn(
            Optional.of(new LegacyESBeregning(behandlingId, 15000, 1, 15000, LocalDateTime.now())));
        var familieHendelseGrunnlag = mock(FamilieHendelseGrunnlagEntitet.class);
        when(familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId)).thenReturn(Optional.of(familieHendelseGrunnlag));
        var familieHendelse = mock(FamilieHendelseEntitet.class);
        when(familieHendelseGrunnlag.getGjeldendeVersjon()).thenReturn(familieHendelse);
        when(familieHendelse.getGjelderAdopsjon()).thenReturn(false);

        var oppdragInputTjeneste = new OppdragInputTjeneste(behandlingRepository, null, behandlingVedtakRepository, familieHendelseRepository,
            tilbakekrevingRepository, personinfoAdapter, økonomioppdragRepository, beregningRepository);

        simulerOppdragTjeneste = new SimulerOppdragTjeneste(new OppdragskontrollTjenesteImpl(økonomioppdragRepository),
            oppdragInputTjeneste);
    }

    @Test
    void simulerOppdrag_uten_behandling_vedtak_ES() {
        // Act
        var oppdragskontroll = simulerOppdragTjeneste.hentOppdragskontrollForBehandling(behandlingId);
        var resultat = OppdragsKontrollDtoMapper.tilDto(oppdragskontroll.get());

        // Assert
        assertThat(resultat.oppdrag()).hasSize(1);
    }
}
