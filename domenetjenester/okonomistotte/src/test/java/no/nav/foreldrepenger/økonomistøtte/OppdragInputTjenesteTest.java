package no.nav.foreldrepenger.økonomistøtte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.EngangsstønadBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.EngangsstønadBeregningRepository;
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
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Sats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.TypeSats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomioppdragRepository;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Betalingsmottaker;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.KjedeNøkkel;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.SatsType;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper.OppdragInput;

@ExtendWith(MockitoExtension.class)
class OppdragInputTjenesteTest {

    private static final int TILKJENT_YTELSE = 15000;
    @Mock
    private PersoninfoAdapter personInfoAdapter;
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private BehandlingVedtakRepository behandlingVedtakRepository;
    @Mock
    private ØkonomioppdragRepository økonomioppdragRepository;
    @Mock
    private EngangsstønadBeregningRepository beregningRepository;
    @Mock
    private FamilieHendelseRepository familieHendelseRepository;
    @Mock
    private TilbakekrevingRepository tilbakekrevingRepository;

    private Behandling behandling;
    private long behandlingId;
    private OppdragInputTjeneste oppdragInputTjeneste;

    @BeforeEach
    void setup() {
        behandling = Behandling.nyBehandlingFor(
            Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, NavBruker.opprettNyNB(AktørId.dummy()), new Saksnummer("123456789")),
            BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandlingId = 123L;

        when(behandlingRepository.hentBehandling(behandlingId)).thenReturn(behandling);
        when(behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandlingId)).thenReturn(Optional.of(BehandlingVedtak.builder()
            .medVedtakstidspunkt(LocalDateTime.now())
            .medAnsvarligSaksbehandler("VL")
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .build()));
        when(personInfoAdapter.hentFnrForAktør(any())).thenReturn(PersonIdent.fra("0987654321"));
        var familieHendelseGrunnlag = mock(FamilieHendelseGrunnlagEntitet.class);
        when(familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId)).thenReturn(Optional.of(familieHendelseGrunnlag));
        var familieHendelse = mock(FamilieHendelseEntitet.class);
        when(familieHendelseGrunnlag.getGjeldendeVersjon()).thenReturn(familieHendelse);
        when(familieHendelse.getGjelderAdopsjon()).thenReturn(false);

        oppdragInputTjeneste = new OppdragInputTjeneste(behandlingRepository, null, behandlingVedtakRepository, familieHendelseRepository,
            tilbakekrevingRepository, personInfoAdapter, økonomioppdragRepository, beregningRepository);

    }

    @Test
    @DisplayName("Simulering oppdrag input for ES fødsel uten tidligere utbetalinger.")
    void oppdragInputSimuleringESFørstegang() {
        // Prepare
        when(beregningRepository.hentEngangsstønadBeregning(behandlingId)).thenReturn(
            Optional.of(new EngangsstønadBeregning(behandlingId, TILKJENT_YTELSE, 1, TILKJENT_YTELSE, LocalDateTime.now())));

        // Act
        var input = oppdragInputTjeneste.lagSimuleringInput(behandlingId);

        // Assert
        var brukerKjedde = getBrukerKjeddeNøkkel();

        assertFellesFelter(input, behandling.getSaksnummer());
        assertTilkjentYtelse(input, brukerKjedde, LocalDate.now());

        // Inger oppdrag fra før.
        assertThat(input.getTidligereOppdrag()).isNotNull();
        assertThat(input.getTidligereOppdrag().getKjeder()).isEmpty();
    }

    @Test
    @DisplayName("Simulering oppdrag input for ES fødsel med en tidligere utbetaling.")
    void oppdragInputSimuleringESRevurderingMedTidligereOppdrag() {
        // Prepare
        var saksnummer = behandling.getSaksnummer();
        when(beregningRepository.hentEngangsstønadBeregning(behandlingId)).thenReturn(
            Optional.of(new EngangsstønadBeregning(behandlingId, TILKJENT_YTELSE, 1, TILKJENT_YTELSE, LocalDateTime.now())));

        var oppdragskontroll = lagOppdragskontroll(saksnummer);
        var oppdrag = lagOppdrag(oppdragskontroll, saksnummer);
        var førsteVedtaksDato = LocalDate.now().minusDays(7);
        lagOppdragslinje(oppdrag, førsteVedtaksDato);
        OppdragKvitteringTestUtil.lagPositivKvitting(oppdrag);

        when(økonomioppdragRepository.finnAlleOppdragForSak(saksnummer)).thenReturn(List.of(oppdragskontroll));

        // Act
        var input = oppdragInputTjeneste.lagSimuleringInput(behandlingId);

        // Assert
        var brukerKjedde = getBrukerKjeddeNøkkel();
        assertFellesFelter(input, saksnummer);
        assertTilkjentYtelse(input, brukerKjedde, førsteVedtaksDato);
        assertTidligereOppdrag(input, brukerKjedde);
    }

    @Test
    @DisplayName("Simulering oppdrag input for ES fødsel med to tidligere utbetalinger.")
    void oppdragInputSimuleringESRevurderingMedToTidligereOppdrag() {
        // Prepare
        var saksnummer = behandling.getSaksnummer();
        when(beregningRepository.hentEngangsstønadBeregning(behandlingId)).thenReturn(
            Optional.of(new EngangsstønadBeregning(behandlingId, TILKJENT_YTELSE, 1, TILKJENT_YTELSE, LocalDateTime.now())));

        var oppdragskontroll = lagOppdragskontroll(saksnummer);
        var oppdrag = lagOppdrag(oppdragskontroll, saksnummer);
        var førsteVedtaksDato = LocalDate.now().minusDays(7);
        lagOppdragslinje(oppdrag, førsteVedtaksDato);
        OppdragKvitteringTestUtil.lagPositivKvitting(oppdrag);

        var oppdragskontroll2 = lagOppdragskontroll(saksnummer);
        var oppdrag2 = lagOppdrag(oppdragskontroll2, saksnummer);
        var andreVedtaksDato = LocalDate.now().minusDays(3);
        lagOppdragslinje(oppdrag2, andreVedtaksDato);
        OppdragKvitteringTestUtil.lagPositivKvitting(oppdrag2);

        when(økonomioppdragRepository.finnAlleOppdragForSak(saksnummer)).thenReturn(List.of(oppdragskontroll, oppdragskontroll2));

        // Act
        var input = oppdragInputTjeneste.lagSimuleringInput(behandlingId);

        // Assert
        var brukerKjedde = getBrukerKjeddeNøkkel();
        assertFellesFelter(input, saksnummer);
        assertTilkjentYtelse(input, brukerKjedde, førsteVedtaksDato);
        assertTidligereOppdrag(input, brukerKjedde);
    }

    @Test
    @DisplayName("Simulering oppdrag input for ES fødsel med to tidligere utbetalinger men med en positiv kvittering.")
    void oppdragInputSimuleringESRevurderingMedToTidligereOppdragMenKunEnPositivKvittering() {
        // Prepare
        var saksnummer = behandling.getSaksnummer();
        when(beregningRepository.hentEngangsstønadBeregning(behandlingId)).thenReturn(
            Optional.of(new EngangsstønadBeregning(behandlingId, TILKJENT_YTELSE, 1, TILKJENT_YTELSE, LocalDateTime.now())));

        var oppdragskontroll = lagOppdragskontroll(saksnummer);
        var oppdrag = lagOppdrag(oppdragskontroll, saksnummer);
        var førsteVedtaksDato = LocalDate.now().minusDays(7);
        lagOppdragslinje(oppdrag, førsteVedtaksDato);
        OppdragKvitteringTestUtil.lagNegativKvitting(oppdrag);

        var oppdragskontroll2 = lagOppdragskontroll(saksnummer);
        var oppdrag2 = lagOppdrag(oppdragskontroll2, saksnummer);
        var andreVedtaksDato = LocalDate.now().minusDays(3);
        lagOppdragslinje(oppdrag2, andreVedtaksDato);
        OppdragKvitteringTestUtil.lagPositivKvitting(oppdrag2);

        when(økonomioppdragRepository.finnAlleOppdragForSak(saksnummer)).thenReturn(List.of(oppdragskontroll, oppdragskontroll2));

        // Act
        var input = oppdragInputTjeneste.lagSimuleringInput(behandlingId);

        // Assert
        var brukerKjedde = getBrukerKjeddeNøkkel();
        assertFellesFelter(input, saksnummer);
        assertTilkjentYtelse(input, brukerKjedde, andreVedtaksDato);
        assertTidligereOppdrag(input, brukerKjedde);
    }

    private KjedeNøkkel getBrukerKjeddeNøkkel() {
        return KjedeNøkkel.builder().medKlassekode(KodeKlassifik.ES_FØDSEL).medBetalingsmottaker(Betalingsmottaker.BRUKER).build();
    }

    private void assertFellesFelter(final OppdragInput input, final Saksnummer saksnummer) {
        assertThat(input.getAnsvarligSaksbehandler()).isNotNull();
        assertThat(input.getSaksnummer()).isEqualTo(saksnummer);
        assertThat(input.getYtelseType()).isEqualTo(FagsakYtelseType.ENGANGSTØNAD);
        assertThat(input.getBrukerFnr()).isNotNull();
        assertThat(input.getProsessTaskId()).isNotNull();
        assertThat(input.getVedtaksdato()).isNotNull();
    }

    private void assertTilkjentYtelse(final OppdragInput input, KjedeNøkkel brukerKjedde, final LocalDate forventetVedtakstado) {
        var tilkjentYtelse = input.getTilkjentYtelse();

        assertThat(tilkjentYtelse).isNotNull();
        assertThat(tilkjentYtelse.getNøkler()).contains(brukerKjedde);
        var ytelse = tilkjentYtelse.getYtelsePrNøkkel().get(brukerKjedde);

        assertThat(ytelse).isNotNull();
        assertThat(ytelse.getPerioder().size()).isEqualTo(1);

        assertThat(ytelse.getPerioder()).allSatisfy(ytelsePeriode -> {
            assertThat(ytelsePeriode.getSats().getSats()).isEqualTo(TILKJENT_YTELSE);
            assertThat(ytelsePeriode.getSats().getSatsType()).isEqualTo(SatsType.ENG);
            assertThat(ytelsePeriode.getPeriode().getFom()).isEqualTo(forventetVedtakstado);
            assertThat(ytelsePeriode.getPeriode().getTom()).isEqualTo(forventetVedtakstado);
        });
    }

    private void assertTidligereOppdrag(final OppdragInput input, final KjedeNøkkel brukerKjedde) {
        assertThat(input.getTidligereOppdrag()).isNotNull();
        assertThat(input.getTidligereOppdrag().getKjeder()).isNotEmpty();
        assertThat(input.getTidligereOppdrag().getKjeder()).containsKey(brukerKjedde);
    }

    private Oppdragskontroll lagOppdragskontroll(Saksnummer saksnummer) {
        return Oppdragskontroll.builder().medBehandlingId(1L).medProsessTaskId(1000L).medSaksnummer(saksnummer).medVenterKvittering(false).build();
    }

    private Oppdrag110 lagOppdrag(Oppdragskontroll oppdragskontroll, Saksnummer saksnummer) {
        return Oppdrag110.builder()
            .medKodeEndring(KodeEndring.NY)
            .medKodeFagomrade(KodeFagområde.REFUTG)
            .medOppdragGjelderId(saksnummer.getVerdi())
            .medSaksbehId("Z100000")
            .medAvstemming(Avstemming.ny())
            .medFagSystemId(Long.parseLong(saksnummer.getVerdi() + "100"))
            .medOppdragskontroll(oppdragskontroll)
            .build();
    }

    private Oppdragslinje150 lagOppdragslinje(Oppdrag110 oppdrag110, LocalDate vedtaksdato) {
        return Oppdragslinje150.builder()
            .medKodeEndringLinje(KodeEndringLinje.NY)
            .medVedtakId(vedtaksdato.toString())
            .medDelytelseId(Long.parseLong(oppdrag110.getFagsystemId() + "100"))
            .medVedtakFomOgTom(vedtaksdato, vedtaksdato)
            .medSats(Sats.på(1122L))
            .medTypeSats(TypeSats.ENG)
            .medUtbetalesTilId("123456789")
            .medKodeKlassifik(KodeKlassifik.ES_FØDSEL)
            .medOppdrag110(oppdrag110)
            .build();
    }
}
