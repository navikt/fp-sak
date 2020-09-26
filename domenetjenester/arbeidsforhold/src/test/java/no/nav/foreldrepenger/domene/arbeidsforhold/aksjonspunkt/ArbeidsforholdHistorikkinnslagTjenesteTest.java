package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYScenarioBuilder;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.historikk.VurderArbeidsforholdHistorikkinnslag;
import no.nav.foreldrepenger.historikk.dto.HistorikkInnslagKonverter;
import no.nav.vedtak.felles.integrasjon.journal.v3.JournalConsumer;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

public class ArbeidsforholdHistorikkinnslagTjenesteTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    @Rule
    public RepositoryRule repositoryRule = new UnittestRepositoryRule();

    @Mock
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste = Mockito.mock(ArbeidsgiverHistorikkinnslag.class);

    private JournalConsumer mockJournalProxyService = mock(JournalConsumer.class);
    private IAYRepositoryProvider provider = new IAYRepositoryProvider(repositoryRule.getEntityManager());
    private HistorikkRepository historikkRepository = new HistorikkRepository(repositoryRule.getEntityManager());
    private HistorikkInnslagKonverter historikkInnslagKonverter = new HistorikkInnslagKonverter();
    private DokumentArkivTjeneste dokumentApplikasjonTjeneste = new DokumentArkivTjeneste(mockJournalProxyService, provider.getFagsakRepository());
    private HistorikkTjenesteAdapter historikkAdapter;
    private ArbeidsforholdHistorikkinnslagTjeneste arbeidsforholdHistorikkinnslagTjeneste;
    private Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet("1");
    private InternArbeidsforholdRef ref = InternArbeidsforholdRef.nyRef();
    Behandling behandling;
    Aksjonspunkt aksjonspunkt;
    Skjæringstidspunkt skjæringstidspunkt;

    @Before
    public void setup() {
        historikkAdapter = new HistorikkTjenesteAdapter(historikkRepository, historikkInnslagKonverter, dokumentApplikasjonTjeneste);
        arbeidsforholdHistorikkinnslagTjeneste = new ArbeidsforholdHistorikkinnslagTjeneste(historikkAdapter, arbeidsgiverHistorikkinnslagTjeneste);
        IAYScenarioBuilder scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        behandling = scenario.lagre(provider);
        aksjonspunkt = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);
        skjæringstidspunkt = Skjæringstidspunkt.builder()
                .medUtledetSkjæringstidspunkt(LocalDate.now())
                .build();
    }

    @Test
    public void skal_ikke_opprette_noen_historikkinnslag_når_arbeidsforholdet_kun_har_null_verdier() {

        // Arrange
        ArbeidsforholdDto arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setBrukArbeidsforholdet(true);
        arbeidsforholdDto.setFomDato(SKJÆRINGSTIDSPUNKT.minusDays(1));

        when(arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(any(), any(), any())).thenReturn("navn");

        // Act
        arbeidsforholdHistorikkinnslagTjeneste.opprettHistorikkinnslag(
                new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, skjæringstidspunkt, arbeidsforholdDto.getBegrunnelse()),
                arbeidsforholdDto, virksomhet, ref, List.of());

        // Assert
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).hasSize(0);

    }

    @Test
    public void skal_opprette_kun_et_historikkinnslag_når_arbeidsforholdet_skal_kun_bruke_permisjon() {

        // Arrange
        ArbeidsforholdDto arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setBrukPermisjon(true);
        arbeidsforholdDto.setBrukArbeidsforholdet(true);
        arbeidsforholdDto.setFomDato(SKJÆRINGSTIDSPUNKT.minusDays(1));

        when(arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(any(), any(), any())).thenReturn("navn");

        // Act
        arbeidsforholdHistorikkinnslagTjeneste.opprettHistorikkinnslag(
                new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, skjæringstidspunkt, arbeidsforholdDto.getBegrunnelse()),
                arbeidsforholdDto, virksomhet, ref, List.of());

        // Assert
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).hasSize(1);
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).anySatisfy(del -> {
            assertThat(del.getHistorikkinnslagFelt()).hasSize(3);
            assertThat(del.getHistorikkinnslagFelt()).anySatisfy(
                    felt -> assertThat(felt.getTilVerdi()).isEqualTo(VurderArbeidsforholdHistorikkinnslag.SØKER_ER_I_PERMISJON.getKode()));
        });

    }

    @Test
    public void skal_opprette_kun_et_historikkinnslag_når_arbeidsforholdet_skal_forsette_uten_inntektsmelding() {

        // Arrange
        ArbeidsforholdDto arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setFortsettBehandlingUtenInntektsmelding(true);
        arbeidsforholdDto.setBrukArbeidsforholdet(true);
        arbeidsforholdDto.setFomDato(SKJÆRINGSTIDSPUNKT.minusDays(1));

        when(arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(any(), any(), any())).thenReturn("navn");

        // Act
        arbeidsforholdHistorikkinnslagTjeneste.opprettHistorikkinnslag(
                new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, skjæringstidspunkt, arbeidsforholdDto.getBegrunnelse()),
                arbeidsforholdDto, virksomhet, ref, List.of());

        // Assert
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).hasSize(1);
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).anySatisfy(del -> {
            assertThat(del.getHistorikkinnslagFelt()).hasSize(3);
            assertThat(del.getHistorikkinnslagFelt()).anySatisfy(
                    felt -> assertThat(felt.getTilVerdi()).isEqualTo(VurderArbeidsforholdHistorikkinnslag.BENYTT_A_INNTEKT_I_BG.getKode()));
        });

    }

    @Test
    public void skal_opprette_to_historikkinnslag_når_arbeidsforholdet_skal_forsette_uten_inntektsmelding_og_ikke_bruke_permisjon() {

        // Arrange
        ArbeidsforholdDto arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setBrukPermisjon(false);
        arbeidsforholdDto.setFortsettBehandlingUtenInntektsmelding(true);
        arbeidsforholdDto.setBrukArbeidsforholdet(true);
        arbeidsforholdDto.setFomDato(SKJÆRINGSTIDSPUNKT.minusDays(1));

        when(arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(any(), any(), any())).thenReturn("navn");

        // Act
        arbeidsforholdHistorikkinnslagTjeneste.opprettHistorikkinnslag(
                new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, skjæringstidspunkt, arbeidsforholdDto.getBegrunnelse()),
                arbeidsforholdDto, virksomhet, ref, List.of());

        // Assert
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).hasSize(2);
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).anySatisfy(del -> {
            assertThat(del.getHistorikkinnslagFelt()).hasSize(3);
            assertThat(del.getHistorikkinnslagFelt()).anySatisfy(
                    felt -> assertThat(felt.getTilVerdi()).isEqualTo(VurderArbeidsforholdHistorikkinnslag.SØKER_ER_IKKE_I_PERMISJON.getKode()));
        });
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).anySatisfy(del -> {
            assertThat(del.getHistorikkinnslagFelt()).hasSize(2);
            assertThat(del.getHistorikkinnslagFelt()).anySatisfy(
                    felt -> assertThat(felt.getTilVerdi()).isEqualTo(VurderArbeidsforholdHistorikkinnslag.BENYTT_A_INNTEKT_I_BG.getKode()));
        });

    }

    @Test
    public void skal_opprette_et_historikkinnslag_når_arbeidsforholdet_ikke_skal_brukes() {

        // Arrange
        ArbeidsforholdDto arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setBrukArbeidsforholdet(false);
        arbeidsforholdDto.setFomDato(SKJÆRINGSTIDSPUNKT.minusDays(1));

        when(arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(any(), any(), any())).thenReturn("navn");

        // Act
        arbeidsforholdHistorikkinnslagTjeneste.opprettHistorikkinnslag(
                new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, skjæringstidspunkt, arbeidsforholdDto.getBegrunnelse()),
                arbeidsforholdDto, virksomhet, ref, List.of());

        // Assert
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).hasSize(1);
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).anySatisfy(del -> {
            assertThat(del.getHistorikkinnslagFelt()).hasSize(3);
            assertThat(del.getHistorikkinnslagFelt())
                    .anySatisfy(felt -> assertThat(felt.getTilVerdi()).isEqualTo(VurderArbeidsforholdHistorikkinnslag.IKKE_BRUK.getKode()));
        });

    }

    @Test
    public void skal_ikke_opprette_noen_historikkinnslag_når_arbeidsforholdet_starter_på_stp() {

        // Arrange
        ArbeidsforholdDto arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setBrukArbeidsforholdet(true);
        arbeidsforholdDto.setFomDato(SKJÆRINGSTIDSPUNKT);

        when(arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(any(), any(), any())).thenReturn("navn");

        // Act
        arbeidsforholdHistorikkinnslagTjeneste.opprettHistorikkinnslag(
                new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, skjæringstidspunkt, arbeidsforholdDto.getBegrunnelse()),
                arbeidsforholdDto, virksomhet, ref, List.of());

        // Assert
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).hasSize(0);

    }

    @Test
    public void skal_ikke_opprette_noen_historikkinnslag_når_arbeidsforholdet_starter_etter_stp() {

        // Arrange
        ArbeidsforholdDto arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setBrukArbeidsforholdet(true);
        arbeidsforholdDto.setFomDato(SKJÆRINGSTIDSPUNKT.plusDays(1));

        when(arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(any(), any(), any())).thenReturn("navn");

        // Act
        arbeidsforholdHistorikkinnslagTjeneste.opprettHistorikkinnslag(
                new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, skjæringstidspunkt, arbeidsforholdDto.getBegrunnelse()),
                arbeidsforholdDto, virksomhet, ref, List.of());

        // Assert
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).hasSize(0);

    }

}
