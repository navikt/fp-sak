package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYScenarioBuilder;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.historikk.VurderArbeidsforholdHistorikkinnslag;

@ExtendWith(MockitoExtension.class)
@ExtendWith(JpaExtension.class)
public class ArbeidsforholdHistorikkinnslagTjenesteTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    @Mock
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;

    private IAYRepositoryProvider provider;
    private HistorikkTjenesteAdapter historikkAdapter;
    private ArbeidsforholdHistorikkinnslagTjeneste arbeidsforholdHistorikkinnslagTjeneste;

    private Behandling behandling;

    @BeforeEach
    void setup(EntityManager entityManager) {
        provider = new IAYRepositoryProvider(entityManager);
        var historikkRepository = new HistorikkRepository(entityManager);
        historikkAdapter = new HistorikkTjenesteAdapter(historikkRepository, mock(DokumentArkivTjeneste.class),
                provider.getBehandlingRepository());
        arbeidsforholdHistorikkinnslagTjeneste = new ArbeidsforholdHistorikkinnslagTjeneste(historikkAdapter, arbeidsgiverHistorikkinnslagTjeneste);
        behandling = opprettBehandling();

    }

    private Behandling opprettBehandling() {
        return IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER).lagre(provider);
    }

    @Test
    public void skal_ikke_opprette_noen_historikkinnslag_når_arbeidsforholdet_kun_har_null_verdier() {

        var arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setBrukArbeidsforholdet(true);
        arbeidsforholdDto.setFomDato(SKJÆRINGSTIDSPUNKT.minusDays(1));
        when(arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(any(), any(), any())).thenReturn("navn");
        opprettHistorikkInnslag(behandling, arbeidsforholdDto);
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).hasSize(0);
    }

    @Test
    public void skal_opprette_kun_et_historikkinnslag_når_arbeidsforholdet_skal_kun_bruke_permisjon() {
        var arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setBrukPermisjon(true);
        arbeidsforholdDto.setBrukArbeidsforholdet(true);
        arbeidsforholdDto.setFomDato(SKJÆRINGSTIDSPUNKT.minusDays(1));

        when(arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(any(), any(), any())).thenReturn("navn");

        opprettHistorikkInnslag(behandling, arbeidsforholdDto);

        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).hasSize(1);
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).anySatisfy(del -> {
            assertThat(del.getHistorikkinnslagFelt()).hasSize(3);
            assertThat(del.getHistorikkinnslagFelt()).anySatisfy(
                    felt -> assertThat(felt.getTilVerdi()).isEqualTo(VurderArbeidsforholdHistorikkinnslag.SØKER_ER_I_PERMISJON.getKode()));
        });

    }

    @Test
    public void skal_opprette_kun_et_historikkinnslag_når_arbeidsforholdet_skal_forsette_uten_inntektsmelding() {

        var arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setFortsettBehandlingUtenInntektsmelding(true);
        arbeidsforholdDto.setBrukArbeidsforholdet(true);
        arbeidsforholdDto.setFomDato(SKJÆRINGSTIDSPUNKT.minusDays(1));

        when(arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(any(), any(), any())).thenReturn("navn");

        opprettHistorikkInnslag(behandling, arbeidsforholdDto);

        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).hasSize(1);
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).anySatisfy(del -> {
            assertThat(del.getHistorikkinnslagFelt()).hasSize(3);
            assertThat(del.getHistorikkinnslagFelt()).anySatisfy(
                    felt -> assertThat(felt.getTilVerdi()).isEqualTo(VurderArbeidsforholdHistorikkinnslag.BENYTT_A_INNTEKT_I_BG.getKode()));
        });

    }

    @Test
    public void skal_opprette_to_historikkinnslag_når_arbeidsforholdet_skal_forsette_uten_inntektsmelding_og_ikke_bruke_permisjon() {

        var arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setBrukPermisjon(false);
        arbeidsforholdDto.setFortsettBehandlingUtenInntektsmelding(true);
        arbeidsforholdDto.setBrukArbeidsforholdet(true);
        arbeidsforholdDto.setFomDato(SKJÆRINGSTIDSPUNKT.minusDays(1));

        when(arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(any(), any(), any())).thenReturn("navn");

        opprettHistorikkInnslag(behandling, arbeidsforholdDto);

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

        var arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setBrukArbeidsforholdet(false);
        arbeidsforholdDto.setFomDato(SKJÆRINGSTIDSPUNKT.minusDays(1));

        when(arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(any(), any(), any())).thenReturn("navn");

        opprettHistorikkInnslag(behandling, arbeidsforholdDto);

        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).hasSize(1);
        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).anySatisfy(del -> {
            assertThat(del.getHistorikkinnslagFelt()).hasSize(3);
            assertThat(del.getHistorikkinnslagFelt())
                    .anySatisfy(felt -> assertThat(felt.getTilVerdi()).isEqualTo(VurderArbeidsforholdHistorikkinnslag.IKKE_BRUK.getKode()));
        });

    }

    @Test
    public void skal_ikke_opprette_noen_historikkinnslag_når_arbeidsforholdet_starter_på_stp() {

        var arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setBrukArbeidsforholdet(true);
        arbeidsforholdDto.setFomDato(SKJÆRINGSTIDSPUNKT);

        when(arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(any(), any(), any())).thenReturn("navn");

        opprettHistorikkInnslag(behandling, arbeidsforholdDto);

        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).hasSize(0);

    }

    @Test
    public void skal_ikke_opprette_noen_historikkinnslag_når_arbeidsforholdet_starter_etter_stp() {
        var arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setBrukArbeidsforholdet(true);
        arbeidsforholdDto.setFomDato(SKJÆRINGSTIDSPUNKT.plusDays(1));

        when(arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(any(), any(), any())).thenReturn("navn");

        opprettHistorikkInnslag(behandling, arbeidsforholdDto);

        assertThat(historikkAdapter.tekstBuilder().getHistorikkinnslagDeler()).hasSize(0);

    }

    private void opprettHistorikkInnslag(Behandling behandling, ArbeidsforholdDto arbeidsforholdDto) {
        var aksjonspunkt = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);
        var skjæringstidspunkt = Skjæringstidspunkt.builder()
                .medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
                .build();
        arbeidsforholdHistorikkinnslagTjeneste.opprettHistorikkinnslag(
                new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, skjæringstidspunkt, arbeidsforholdDto.getBegrunnelse()),
                arbeidsforholdDto, Arbeidsgiver.virksomhet("1"), InternArbeidsforholdRef.nyRef(), List.of());
    }

}
