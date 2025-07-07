package no.nav.foreldrepenger.økonomistøtte.oppdrag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeStatusLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.TypeSats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomioppdragRepository;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Betalingsmottaker;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.KjedeNøkkel;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Periode;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Satsen;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Ytelse;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.YtelsePeriode;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.samlinger.GruppertYtelse;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.samlinger.OverordnetOppdragKjedeOversikt;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper.EksisterendeOppdragMapper;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper.OppdragInput;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.tjeneste.OppdragskontrollTjenesteImpl;

class NyOppdragskontrollTjenesteImplKontantytelseTest {

    public static final long PROSESS_TASK_ID = 23L;
    public static final String BRUKER_FNR = "12345678901";
    public static final Saksnummer SAKSNUMMER = new Saksnummer("101000");
    public static final long BEHANDLING_ID = 123456L;
    public static final String ANSVARLIG_SAKSBEHANDLER = "Katarzyna";
    public static final LocalDate VEDTAKSDATO = LocalDate.now();
    public static final long SATS_ES = 63330L;

    protected OppdragskontrollTjenesteImpl oppdragskontrollTjeneste;

    @BeforeEach
    void setUp() {
        oppdragskontrollTjeneste = new OppdragskontrollTjenesteImpl(mock(ØkonomioppdragRepository.class));
    }

    @Test
    void opprettOppdragTestES() {
        // Arrange
        var gruppertYtelseBuilder = getGruppertYtelseBuilder(KodeKlassifik.ES_FØDSEL, VEDTAKSDATO, SATS_ES);
        var inputBuilder = getInputStandardBuilder(gruppertYtelseBuilder.build());

        // Act
        var oppdragskontroll = oppdragskontrollTjeneste.opprettOppdrag(inputBuilder.build());

        // Assert
        assertThat(oppdragskontroll).isPresent();
        verifiserOppdragskontroll(oppdragskontroll.get());
        var oppdrag110Liste = verifiserOppdrag110(oppdragskontroll.get());
        verifiserAvstemming(oppdrag110Liste);
        verifiserOppdragslinje150(oppdrag110Liste);
    }

    @Test
    void hentOppdragskontrollTestES() {
        // Arrange
        // Arrange
        var gruppertYtelseBuilder = getGruppertYtelseBuilder(KodeKlassifik.ES_ADOPSJON, VEDTAKSDATO.minusDays(1), SATS_ES);
        var inputBuilder = getInputStandardBuilder(gruppertYtelseBuilder.build());

        // Act
        var oppdragskontroll = oppdragskontrollTjeneste.opprettOppdrag(inputBuilder.build());

        // Assert
        assertThat(oppdragskontroll).isPresent();
        assertThat(oppdragskontroll.get().getOppdrag110Liste()).hasSize(1);

        var oppdrag110 = oppdragskontroll.get().getOppdrag110Liste().getFirst();
        assertThat(oppdrag110).isNotNull();
        assertThat(oppdrag110.getOppdragslinje150Liste()).hasSize(1);
        assertThat(oppdrag110.getAvstemming()).isNotNull();

        var oppdrlinje150 = oppdrag110.getOppdragslinje150Liste().getFirst();
        assertThat(oppdrlinje150).isNotNull();
        assertThat(oppdrlinje150.getOppdrag110()).isNotNull();
    }

    @Test
    void innvilgelseSomReferererTilTidligereOppdragPåSammeSak() {
        // Act 1: Førstegangsbehandling
        // Arrange
        var gruppertYtelseBuilder = getGruppertYtelseBuilder(KodeKlassifik.ES_FØDSEL, VEDTAKSDATO, SATS_ES);
        var inputBuilder = getInputStandardBuilder(gruppertYtelseBuilder.build());

        // Act
        var originaltOppdrag = oppdragskontrollTjeneste.opprettOppdrag(inputBuilder.build());

        assertThat(originaltOppdrag).isPresent();
        var originaltOppdrag110 = originaltOppdrag.get().getOppdrag110Liste().getFirst();

        // Arrange 2: Revurdering
        var gruppertYtelseBuilder2 = getGruppertYtelseBuilder(KodeKlassifik.ES_FØDSEL, VEDTAKSDATO, SATS_ES * 2);
        var inputBuilder2 = getInputStandardBuilder(gruppertYtelseBuilder2.build()).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag.get())));

        var oppdragRevurdering = oppdragskontrollTjeneste.opprettOppdrag(inputBuilder2.build());

        // Assert 2: Revurdering
        assertThat(oppdragRevurdering).isPresent();
        var originalOppdragslinje150 = originaltOppdrag110.getOppdragslinje150Liste().getFirst();
        var oppdragslinje150 = verifiserOppdrag110(oppdragRevurdering.get(), KodeEndring.ENDR,
            originaltOppdrag110.getFagsystemId());
        verifiserOppdragslinje150(oppdragslinje150, KodeEndringLinje.NY, null,
            originalOppdragslinje150.getDelytelseId() + 1, originalOppdragslinje150.getDelytelseId(),
            originaltOppdrag110.getFagsystemId(), 2 * SATS_ES);
    }

    @Test
    void avslagSomReferererTilTidligereOppdragPåSammeSak() {
        // Act 1: Førstegangsbehandling
        // Arrange
        var gruppertYtelseBuilder = getGruppertYtelseBuilder(KodeKlassifik.ES_FØDSEL, VEDTAKSDATO, SATS_ES);
        var inputBuilder = getInputStandardBuilder(gruppertYtelseBuilder.build());

        // Act
        var originaltOppdrag = oppdragskontrollTjeneste.opprettOppdrag(inputBuilder.build());

        assertThat(originaltOppdrag).isPresent();
        var originaltOppdrag110 = originaltOppdrag.get().getOppdrag110Liste().getFirst();

        // Arrange 2: Revurdering
        var inputBuilder2 = getInputStandardBuilder(GruppertYtelse.TOM).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag.get())));

        var oppdragRevurdering = oppdragskontrollTjeneste.opprettOppdrag(inputBuilder2.build());

        // Assert 2: Revurdering
        assertThat(oppdragRevurdering).isPresent();
        var originalOppdragslinje150 = originaltOppdrag110.getOppdragslinje150Liste().getFirst();
        var oppdragslinje150 = verifiserOppdrag110(oppdragRevurdering.get(), KodeEndring.ENDR,
            originaltOppdrag110.getFagsystemId());
        verifiserOppdragslinje150(oppdragslinje150, KodeEndringLinje.ENDR, KodeStatusLinje.OPPH,
            originalOppdragslinje150.getDelytelseId(), null, null, SATS_ES);
    }

    @Test
    void avslagSomReferererTilForrigeOppdragSomTilhørerFørsteRevurderingPåSammeSak() {
        // Act 1: Førstegangsbehandling
        // Arrange
        var gruppertYtelseBuilder = getGruppertYtelseBuilder(KodeKlassifik.ES_ADOPSJON, VEDTAKSDATO, SATS_ES);
        var inputBuilder = getInputStandardBuilder(gruppertYtelseBuilder.build());

        // Act
        var originaltOppdrag = oppdragskontrollTjeneste.opprettOppdrag(inputBuilder.build());
        assertThat(originaltOppdrag).isPresent();
        var førsteOppdrag = originaltOppdrag.get();
        // Arrange 2: Første revurdering
        var gruppertYtelseBuilder2 = getGruppertYtelseBuilder(KodeKlassifik.ES_ADOPSJON, VEDTAKSDATO, SATS_ES);
        var inputBuilder2 = getInputStandardBuilder(gruppertYtelseBuilder2.build()).medTidligereOppdrag(mapTidligereOppdrag(List.of(førsteOppdrag)));

        var oppdragFørsteRevurdering = oppdragskontrollTjeneste.opprettOppdrag(inputBuilder2.build());

        assertThat(oppdragFørsteRevurdering).isNotPresent();

        // Arrange 3: Andre revurdering
        var inputBuilder3 = getInputStandardBuilder(GruppertYtelse.TOM).medTidligereOppdrag(mapTidligereOppdrag(List.of(førsteOppdrag)));

        var oppdragAndreRevurdering = oppdragskontrollTjeneste.opprettOppdrag(inputBuilder3.build());

        // Assert 3: Revurdering
        assertThat(oppdragAndreRevurdering).isPresent();
        var oppdrag110 = førsteOppdrag.getOppdrag110Liste().getFirst();
        assertThat(oppdrag110.getOppdragslinje150Liste()).hasSize(1);
        var førstegangsOpp150 = oppdrag110.getOppdragslinje150Liste().getFirst();
        var andreRevurderingopp150 = verifiserOppdrag110(oppdragAndreRevurdering.get(), KodeEndring.ENDR,
            oppdrag110.getFagsystemId());
        assertThat(andreRevurderingopp150.getDatoVedtakFom()).isEqualTo(førstegangsOpp150.getDatoVedtakFom());
        assertThat(andreRevurderingopp150.getDatoVedtakTom()).isEqualTo(førstegangsOpp150.getDatoVedtakTom());
        assertThat(andreRevurderingopp150.getDatoStatusFom()).isEqualTo(førstegangsOpp150.getDatoVedtakFom());

        verifiserOppdragslinje150(andreRevurderingopp150, KodeEndringLinje.ENDR, KodeStatusLinje.OPPH,
            førstegangsOpp150.getDelytelseId(), null, null, SATS_ES);
    }

    private Oppdragslinje150 verifiserOppdrag110(Oppdragskontroll oppdragskontroll,
                                                 KodeEndring kodeEndring,
                                                 Long fagsystemId) {
        assertThat(oppdragskontroll.getOppdrag110Liste()).hasSize(1);
        var oppdrag110 = oppdragskontroll.getOppdrag110Liste().getFirst();
        assertThat(oppdrag110.getKodeEndring()).isEqualTo(kodeEndring);
        assertThat(oppdrag110.getFagsystemId()).isEqualTo(fagsystemId);
        assertThat(oppdrag110.getOppdragslinje150Liste()).hasSize(1);
        return oppdrag110.getOppdragslinje150Liste().getFirst();
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
        var oppdragslinje150List = oppdrag110Liste.stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .toList();

        var løpenummer = 100L;
        for (var oppdrag110 : oppdrag110Liste) {
            assertThat(oppdrag110.getOppdragslinje150Liste()).hasSize(1);
            var oppdragslinje150 = oppdragslinje150List.getFirst();
            assertThat(oppdragslinje150.getKodeEndringLinje()).isEqualTo(KodeEndringLinje.NY);
            assertThat(oppdragslinje150.getVedtakId()).isEqualTo(VEDTAKSDATO.toString());
            assertThat(oppdragslinje150.getDelytelseId()).isEqualTo(
                concatenateValues(oppdrag110.getFagsystemId(), løpenummer));
            assertThat(oppdragslinje150.getKodeKlassifik()).isEqualTo(KodeKlassifik.ES_FØDSEL);
            assertThat(oppdragslinje150.getDatoVedtakFom()).isEqualTo(VEDTAKSDATO);
            assertThat(oppdragslinje150.getDatoVedtakTom()).isEqualTo(VEDTAKSDATO);
            assertThat(Long.valueOf(oppdragslinje150.getSats().getVerdi())).isEqualTo(SATS_ES);
            assertThat(oppdragslinje150.getTypeSats()).isEqualTo(TypeSats.ENG);
            assertThat(oppdragslinje150.getUtbetalesTilId()).isEqualTo(BRUKER_FNR);
            assertThat(oppdragslinje150.getOppdrag110()).isEqualTo(oppdrag110);
        }
        return oppdragslinje150List;
    }

    private void verifiserAvstemming(List<Oppdrag110> oppdrag110Liste) {
        assertThat(oppdrag110Liste).isNotEmpty().allSatisfy(oppdrag110 -> {
            var avstemming = oppdrag110.getAvstemming();
            assertThat(avstemming).isNotNull();
            assertThat(avstemming.getNøkkel()).isNotNull();
            assertThat(avstemming.getNøkkel()).isEqualTo(avstemming.getTidspunkt());
        });
    }

    private List<Oppdrag110> verifiserOppdrag110(Oppdragskontroll oppdragskontroll) {
        var oppdrag110List = oppdragskontroll.getOppdrag110Liste();

        var initialLøpenummer = 100L;
        for (var oppdrag110 : oppdrag110List) {
            assertThat(oppdrag110.getKodeEndring()).isEqualTo(KodeEndring.NY);
            assertThat(oppdrag110.getKodeFagomrade()).isEqualTo(KodeFagområde.REFUTG);
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
        var valueList = List.of(values);
        var result = valueList.stream().map(Object::toString).collect(Collectors.joining());

        return Long.valueOf(result);
    }

    private OppdragInput.Builder getInputStandardBuilder(GruppertYtelse gruppertYtelse) {
        return OppdragInput.builder()
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
