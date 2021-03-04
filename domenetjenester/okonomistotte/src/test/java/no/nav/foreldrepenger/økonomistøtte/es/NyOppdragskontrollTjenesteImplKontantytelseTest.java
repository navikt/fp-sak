package no.nav.foreldrepenger.økonomistøtte.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeStatusLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.TypeSats;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomistøtte.OppdragMedPositivKvitteringTestUtil;
import no.nav.foreldrepenger.økonomistøtte.OpprettBehandlingForOppdrag;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Betalingsmottaker;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.KjedeNøkkel;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Periode;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Satsen;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Ytelse;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.YtelsePeriode;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.samlinger.GruppertYtelse;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.samlinger.OverordnetOppdragKjedeOversikt;
import no.nav.foreldrepenger.økonomistøtte.ny.mapper.EksisterendeOppdragMapper;
import no.nav.foreldrepenger.økonomistøtte.ny.mapper.Input;
import no.nav.foreldrepenger.økonomistøtte.ny.mapper.LagOppdragTjeneste;
import no.nav.foreldrepenger.økonomistøtte.ny.tjeneste.NyOppdragskontrollTjenesteImpl;
import no.nav.foreldrepenger.økonomistøtte.ØkonomioppdragRepository;

public class NyOppdragskontrollTjenesteImplKontantytelseTest {

    public static final long PROSESS_TASK_ID = 23L;
    public static final String BRUKER_FNR = "12345678901";
    public static final Saksnummer SAKSNUMMER = Saksnummer.infotrygd("101000");
    public static final long BEHANDLING_ID = 123456L;
    public static final String ANSVARLIG_SAKSBEHANDLER = "Katarzyna";
    public static final LocalDate VEDTAKSDATO = LocalDate.now();
    public static final long SATS_ES = 63330L;

    protected NyOppdragskontrollTjenesteImpl nyOppdragskontrollTjeneste;

    @BeforeEach
    public void setUp() {
        nyOppdragskontrollTjeneste = new NyOppdragskontrollTjenesteImpl(new LagOppdragTjeneste(), mock(ØkonomioppdragRepository.class));
    }

    @Test
    public void opprettOppdragTestES() {
        // Arrange
        GruppertYtelse.Builder gruppertYtelseBuilder = getGruppertYtelseBuilder(KodeKlassifik.ES_FØDSEL, VEDTAKSDATO, SATS_ES);
        var inputBuilder = getInputStandardBuilder(gruppertYtelseBuilder.build());

        // Act
        var oppdragskontroll = nyOppdragskontrollTjeneste.opprettOppdrag(inputBuilder.build());

        // Assert
        assertThat(oppdragskontroll).isPresent();
        verifiserOppdragskontroll(oppdragskontroll.get());
        List<Oppdrag110> oppdrag110Liste = verifiserOppdrag110(oppdragskontroll.get());
        verifiserAvstemming(oppdrag110Liste);
        verifiserOppdragslinje150(oppdrag110Liste);
    }

    @Test
    public void hentOppdragskontrollTestES() {
        // Arrange
        // Arrange
        GruppertYtelse.Builder gruppertYtelseBuilder = getGruppertYtelseBuilder(KodeKlassifik.ES_ADOPSJON, VEDTAKSDATO.minusDays(1), SATS_ES);
        var inputBuilder = getInputStandardBuilder(gruppertYtelseBuilder.build());

        // Act
        var oppdragskontroll = nyOppdragskontrollTjeneste.opprettOppdrag(inputBuilder.build());

        // Assert
        assertThat(oppdragskontroll).isPresent();
        assertThat(oppdragskontroll.get().getOppdrag110Liste()).hasSize(1);

        Oppdrag110 oppdrag110 = oppdragskontroll.get().getOppdrag110Liste().get(0);
        assertThat(oppdrag110).isNotNull();
        assertThat(oppdrag110.getOppdragslinje150Liste()).hasSize(1);
        assertThat(oppdrag110.getAvstemming()).isNotNull();

        Oppdragslinje150 oppdrlinje150 = oppdrag110.getOppdragslinje150Liste().get(0);
        assertThat(oppdrlinje150).isNotNull();
        assertThat(oppdrlinje150.getOppdrag110()).isNotNull();
    }

    @Test
    public void innvilgelseSomReferererTilTidligereOppdragPåSammeSak() {
        // Act 1: Førstegangsbehandling
        // Arrange
        GruppertYtelse.Builder gruppertYtelseBuilder = getGruppertYtelseBuilder(KodeKlassifik.ES_FØDSEL, VEDTAKSDATO, SATS_ES);
        var inputBuilder = getInputStandardBuilder(gruppertYtelseBuilder.build());

        // Act
        var originaltOppdrag = nyOppdragskontrollTjeneste.opprettOppdrag(inputBuilder.build());

        assertThat(originaltOppdrag).isPresent();
        Oppdrag110 originaltOppdrag110 = originaltOppdrag.get().getOppdrag110Liste().get(0);

        // Arrange 2: Revurdering
        GruppertYtelse.Builder gruppertYtelseBuilder2 = getGruppertYtelseBuilder(KodeKlassifik.ES_FØDSEL, VEDTAKSDATO, SATS_ES * 2);
        var inputBuilder2 = getInputStandardBuilder(gruppertYtelseBuilder2.build()).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag.get())));

        var oppdragRevurdering = nyOppdragskontrollTjeneste.opprettOppdrag(inputBuilder2.build());

        // Assert 2: Revurdering
        assertThat(oppdragRevurdering).isPresent();
        Oppdragslinje150 originalOppdragslinje150 = originaltOppdrag110.getOppdragslinje150Liste().get(0);
        Oppdragslinje150 oppdragslinje150 = verifiserOppdrag110(oppdragRevurdering.get(), KodeEndring.ENDRING,
            originaltOppdrag110.getFagsystemId());
        verifiserOppdragslinje150(oppdragslinje150, KodeEndringLinje.NY, null,
            originalOppdragslinje150.getDelytelseId() + 1, originalOppdragslinje150.getDelytelseId(),
            originaltOppdrag110.getFagsystemId(), 2 * SATS_ES);
    }

    @Test
    public void avslagSomReferererTilTidligereOppdragPåSammeSak() {
        // Act 1: Førstegangsbehandling
        // Arrange
        GruppertYtelse.Builder gruppertYtelseBuilder = getGruppertYtelseBuilder(KodeKlassifik.ES_FØDSEL, VEDTAKSDATO, SATS_ES);
        var inputBuilder = getInputStandardBuilder(gruppertYtelseBuilder.build());

        // Act
        var originaltOppdrag = nyOppdragskontrollTjeneste.opprettOppdrag(inputBuilder.build());

        assertThat(originaltOppdrag).isPresent();
        Oppdrag110 originaltOppdrag110 = originaltOppdrag.get().getOppdrag110Liste().get(0);

        // Arrange 2: Revurdering
        var inputBuilder2 = getInputStandardBuilder(GruppertYtelse.TOM).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag.get())));

        var oppdragRevurdering = nyOppdragskontrollTjeneste.opprettOppdrag(inputBuilder2.build());

        // Assert 2: Revurdering
        assertThat(oppdragRevurdering).isPresent();
        Oppdragslinje150 originalOppdragslinje150 = originaltOppdrag110.getOppdragslinje150Liste().get(0);
        Oppdragslinje150 oppdragslinje150 = verifiserOppdrag110(oppdragRevurdering.get(), KodeEndring.ENDRING,
            originaltOppdrag110.getFagsystemId());
        verifiserOppdragslinje150(oppdragslinje150, KodeEndringLinje.ENDRING, KodeStatusLinje.OPPHØR,
            originalOppdragslinje150.getDelytelseId(), null, null, SATS_ES);
    }

    @Test
    public void avslagSomReferererTilForrigeOppdragSomTilhørerFørsteRevurderingPåSammeSak() {
        // Act 1: Førstegangsbehandling
        // Arrange
        GruppertYtelse.Builder gruppertYtelseBuilder = getGruppertYtelseBuilder(KodeKlassifik.ES_ADOPSJON, VEDTAKSDATO, SATS_ES);
        var inputBuilder = getInputStandardBuilder(gruppertYtelseBuilder.build());

        // Act
        var originaltOppdrag = nyOppdragskontrollTjeneste.opprettOppdrag(inputBuilder.build());
        assertThat(originaltOppdrag).isPresent();
        var førsteOppdrag = originaltOppdrag.get();
        // Arrange 2: Første revurdering
        GruppertYtelse.Builder gruppertYtelseBuilder2 = getGruppertYtelseBuilder(KodeKlassifik.ES_ADOPSJON, VEDTAKSDATO, SATS_ES);
        var inputBuilder2 = getInputStandardBuilder(gruppertYtelseBuilder2.build()).medTidligereOppdrag(mapTidligereOppdrag(List.of(førsteOppdrag)));

        var oppdragFørsteRevurdering = nyOppdragskontrollTjeneste.opprettOppdrag(inputBuilder2.build());

        assertThat(oppdragFørsteRevurdering).isNotPresent();

        // Arrange 3: Andre revurdering
        var inputBuilder3 = getInputStandardBuilder(GruppertYtelse.TOM).medTidligereOppdrag(mapTidligereOppdrag(List.of(førsteOppdrag)));

        var oppdragAndreRevurdering = nyOppdragskontrollTjeneste.opprettOppdrag(inputBuilder3.build());

        // Assert 3: Revurdering
        assertThat(oppdragAndreRevurdering).isPresent();
        var oppdrag110 = førsteOppdrag.getOppdrag110Liste().get(0);
        assertThat(oppdrag110.getOppdragslinje150Liste()).hasSize(1);
        Oppdragslinje150 førstegangsOpp150 = oppdrag110.getOppdragslinje150Liste().get(0);
        Oppdragslinje150 andreRevurderingopp150 = verifiserOppdrag110(oppdragAndreRevurdering.get(), KodeEndring.ENDRING,
            oppdrag110.getFagsystemId());
        assertThat(andreRevurderingopp150.getDatoVedtakFom()).isEqualTo(førstegangsOpp150.getDatoVedtakFom());
        assertThat(andreRevurderingopp150.getDatoVedtakTom()).isEqualTo(førstegangsOpp150.getDatoVedtakTom());
        assertThat(andreRevurderingopp150.getDatoStatusFom()).isEqualTo(førstegangsOpp150.getDatoVedtakFom());

        verifiserOppdragslinje150(andreRevurderingopp150, KodeEndringLinje.ENDRING, KodeStatusLinje.OPPHØR,
            førstegangsOpp150.getDelytelseId(), null, null, SATS_ES);
    }

    private Oppdragslinje150 verifiserOppdrag110(Oppdragskontroll oppdragskontroll,
                                                 KodeEndring kodeEndring,
                                                 Long fagsystemId) {
        assertThat(oppdragskontroll.getOppdrag110Liste()).hasSize(1);
        Oppdrag110 oppdrag110 = oppdragskontroll.getOppdrag110Liste().get(0);
        assertThat(oppdrag110.getKodeEndring()).isEqualTo(kodeEndring);
        assertThat(oppdrag110.getFagsystemId()).isEqualTo(fagsystemId);
        assertThat(oppdrag110.getOppdragslinje150Liste()).hasSize(1);
        return oppdrag110.getOppdragslinje150Liste().get(0);
    }

    private void verifiserOppdragslinje150(Oppdragslinje150 oppdragslinje150,
                                           KodeEndringLinje kodeEndringLinje,
                                           KodeStatusLinje kodeStatusLinje,
                                           Long delYtelseId,
                                           Long refDelytelseId,
                                           Long refFagsystemId,
                                           long sats) {
        assertThat(oppdragslinje150.getKodeEndringLinje()).isEqualTo(kodeEndringLinje);
        if (kodeStatusLinje == null) {
            assertThat(oppdragslinje150.getKodeStatusLinje()).isNull();
        } else {
            assertThat(oppdragslinje150.getKodeStatusLinje()).isEqualTo(kodeStatusLinje);
        }
        assertThat(oppdragslinje150.getRefFagsystemId()).isEqualTo(refFagsystemId);
        assertThat(oppdragslinje150.getSats().getVerdi().longValue()).isEqualTo(sats);
        assertThat(oppdragslinje150.getRefDelytelseId()).isEqualTo(refDelytelseId);
        assertThat(oppdragslinje150.getDelytelseId()).isEqualTo(delYtelseId);
    }

    private List<Oppdragslinje150> verifiserOppdragslinje150(List<Oppdrag110> oppdrag110Liste) {
        List<Oppdragslinje150> oppdragslinje150List = oppdrag110Liste.stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .collect(Collectors.toList());

        long løpenummer = 100L;
        for (Oppdrag110 oppdrag110 : oppdrag110Liste) {
            assertThat(oppdrag110.getOppdragslinje150Liste()).hasSize(1);
            Oppdragslinje150 oppdragslinje150 = oppdragslinje150List.get(0);
            assertThat(oppdragslinje150.getKodeEndringLinje()).isEqualTo(KodeEndringLinje.NY);
            assertThat(oppdragslinje150.getVedtakId()).isEqualTo(VEDTAKSDATO.toString());
            assertThat(oppdragslinje150.getDelytelseId()).isEqualTo(
                concatenateValues(oppdrag110.getFagsystemId(), løpenummer));
            assertThat(oppdragslinje150.getKodeKlassifik()).isEqualTo(KodeKlassifik.ES_FØDSEL);
            assertThat(oppdragslinje150.getDatoVedtakFom()).isEqualTo(VEDTAKSDATO);
            assertThat(oppdragslinje150.getDatoVedtakTom()).isEqualTo(VEDTAKSDATO);
            assertThat(oppdragslinje150.getSats().getVerdi()).isEqualTo(SATS_ES);
            assertThat(oppdragslinje150.getTypeSats()).isEqualTo(TypeSats.ENGANG);
            assertThat(oppdragslinje150.getUtbetalesTilId()).isEqualTo(BRUKER_FNR);
            assertThat(oppdragslinje150.getOppdrag110()).isEqualTo(oppdrag110);
        }
        return oppdragslinje150List;
    }

    private void verifiserAvstemming(List<Oppdrag110> oppdrag110Liste) {
        assertThat(oppdrag110Liste).allSatisfy(oppdrag110 -> {
            Avstemming avstemming = oppdrag110.getAvstemming();
            assertThat(avstemming).isNotNull();
            assertThat(avstemming.getNøkkel()).isNotNull();
            assertEquals(avstemming.getNøkkel(), avstemming.getTidspunkt());
        });
    }

    private List<Oppdrag110> verifiserOppdrag110(Oppdragskontroll oppdragskontroll) {
        List<Oppdrag110> oppdrag110List = oppdragskontroll.getOppdrag110Liste();

        long initialLøpenummer = 100L;
        for (Oppdrag110 oppdrag110 : oppdrag110List) {
            assertThat(oppdrag110.getKodeEndring()).isEqualTo(KodeEndring.NY);
            assertThat(oppdrag110.getKodeFagomrade()).isEqualTo(KodeFagområde.ENGANGSSTØNAD);
            assertThat(oppdrag110.getFagsystemId()).isEqualTo(
                concatenateValues(Long.parseLong(SAKSNUMMER.getVerdi()), initialLøpenummer++));
            assertThat(oppdrag110.getSaksbehId()).isEqualTo(ANSVARLIG_SAKSBEHANDLER);
            assertThat(oppdrag110.getOppdragGjelderId()).isEqualTo(BRUKER_FNR);
            assertThat(oppdrag110.getOppdragskontroll()).isEqualTo(oppdragskontroll);
            assertThat(oppdrag110.getAvstemming()).isNotNull();
        }

        return oppdrag110List;
    }

    private void verifiserOppdragskontroll(Oppdragskontroll oppdrskontroll) {
        assertThat(oppdrskontroll.getSaksnummer()).isEqualTo(SAKSNUMMER);
        assertThat(oppdrskontroll.getVenterKvittering()).isEqualTo(Boolean.TRUE);
        assertThat(oppdrskontroll.getProsessTaskId()).isEqualTo(PROSESS_TASK_ID);
    }

    private Long concatenateValues(Long... values) {
        List<Long> valueList = List.of(values);
        String result = valueList.stream().map(Object::toString).collect(Collectors.joining());

        return Long.valueOf(result);
    }

    private Input.Builder getInputStandardBuilder(GruppertYtelse gruppertYtelse) {
        return Input.builder()
            .medTilkjentYtelse(gruppertYtelse)
            .medTidligereOppdrag(OverordnetOppdragKjedeOversikt.TOM)
            .medBrukerFnr(BRUKER_FNR)
            .medBehandlingId(BEHANDLING_ID)
            .medSaksnummer(SAKSNUMMER)
            .medFagsakYtelseType(FagsakYtelseType.ENGANGSTØNAD)
            .medAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER)
            .medVedtaksdato(VEDTAKSDATO)
            .medBrukInntrekk(true)
            .medProsessTaskId(PROSESS_TASK_ID);
    }

    protected YtelsePeriode lagPeriode(LocalDate referanseDato, Satsen sats) {
        return new YtelsePeriode(Periode.of(referanseDato, referanseDato), sats);
    }

    private GruppertYtelse.Builder getGruppertYtelseBuilder(KodeKlassifik kodeKlassifik, LocalDate vedtaksDato, long sats) {
        return GruppertYtelse.builder()
            .leggTilKjede(
                KjedeNøkkel.lag(kodeKlassifik, Betalingsmottaker.BRUKER),
                Ytelse.builder()
                    .leggTilPeriode(lagPeriode(vedtaksDato, Satsen.engang(sats)))
                    .build()
            );
    }

    private OverordnetOppdragKjedeOversikt mapTidligereOppdrag(List<Oppdragskontroll> tidligereOppdragskontroll) {
        return new OverordnetOppdragKjedeOversikt(EksisterendeOppdragMapper.tilKjeder(tidligereOppdragskontroll));
    }
}
