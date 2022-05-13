package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.uttak.fakta.KontrollerFaktaUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder.KontrollerFaktaPeriode;
import no.nav.foreldrepenger.historikk.HistorikkAvklartSoeknadsperiodeType;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.ArbeidsgiverHistorikkinnslag;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.FaktaUttakHistorikkTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.KontrollerOppgittFordelingTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.BekreftetOppgittPeriodeDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.KontrollerFaktaPeriodeLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.OverstyringFaktaUttakDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.SlettetUttakPeriodeDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring.FaktaUttakOverstyringFelles;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring.FaktaUttakOverstyringshåndterer;

@CdiDbAwareTest
public class FaktaUttakOverstyringshåndtererTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private KontrollerOppgittFordelingTjeneste fordelingTjeneste;

    @Inject
    private UttakInputTjeneste uttakInputTjeneste;

    private KontrollerFaktaUttakTjeneste kontrollerFaktaUttakTjeneste = Mockito.mock(KontrollerFaktaUttakTjeneste.class);
    private HistorikkTjenesteAdapter historikkApplikasjonTjeneste = Mockito.mock(HistorikkTjenesteAdapter.class);
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste = mock(ArbeidsgiverHistorikkinnslag.class);
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste = mock(InntektArbeidYtelseTjeneste.class);

    private FaktaUttakHistorikkTjeneste faktaUttakHistorikkTjeneste;

    private FaktaUttakOverstyringshåndterer faktaUttakOverstyringshåndterer;

    @BeforeEach
    public void setup() {
        when(inntektArbeidYtelseTjeneste.hentGrunnlag(anyLong())).thenReturn(InntektArbeidYtelseGrunnlagBuilder.nytt().build());
        this.faktaUttakHistorikkTjeneste = new FaktaUttakHistorikkTjeneste(historikkApplikasjonTjeneste,
                arbeidsgiverHistorikkinnslagTjeneste, inntektArbeidYtelseTjeneste);

        this.faktaUttakOverstyringshåndterer = new FaktaUttakOverstyringshåndterer(historikkApplikasjonTjeneste,
                faktaUttakHistorikkTjeneste,
                new FaktaUttakOverstyringFelles(fordelingTjeneste, kontrollerFaktaUttakTjeneste, uttakInputTjeneste));
    }

    @Test
    public void skal_generere_historikkinnslag_ved_slettet_søknadsperiode() {
        var behandling = opprettBehandling();

        var dto = opprettOverstyringSøknadsperioderDto();
        faktaUttakOverstyringshåndterer.håndterAksjonspunktForOverstyringPrecondition(dto, behandling);
        faktaUttakOverstyringshåndterer.håndterAksjonspunktForOverstyringHistorikk(dto, behandling);

        // Verifiserer HistorikkinnslagDto
        var historikkCapture = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkApplikasjonTjeneste).lagInnslag(historikkCapture.capture());
        var historikkinnslag = historikkCapture.getValue();
        assertThat(historikkinnslag.getType()).isEqualTo(HistorikkinnslagType.UTTAK);
        assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        var del = historikkinnslag.getHistorikkinnslagDeler().get(0);
        assertThat(del.getSkjermlenke()).as("skjermlenke")
                .hasValueSatisfying(skjermlenke -> assertThat(skjermlenke).isEqualTo(SkjermlenkeType.FAKTA_OM_UTTAK.getKode()));
        assertThat(del.getResultat())
                .hasValueSatisfying(resultat -> assertThat(resultat).isEqualTo(HistorikkResultatType.OVERSTYRING_FAKTA_UTTAK.getKode()));
        assertThat(del.getAvklartSoeknadsperiode()).as("soeknadsperiode")
                .hasValueSatisfying(soeknadsperiode -> assertThat(soeknadsperiode.getNavn()).as("navn")
                        .isEqualTo(HistorikkAvklartSoeknadsperiodeType.SLETTET_SOEKNASPERIODE.getKode()));
    }

    private OverstyringFaktaUttakDto.OverstyrerFaktaUttakDto opprettOverstyringSøknadsperioderDto() {
        var dto = new OverstyringFaktaUttakDto.OverstyrerFaktaUttakDto();
        var bekreftetUttakPeriodeDto = getBekreftetUttakPeriodeDto(LocalDate.now().minusDays(20), LocalDate.now().minusDays(11));
        var slettetPeriodeDto = new SlettetUttakPeriodeDto();
        slettetPeriodeDto.setBegrunnelse("ugyldig søknadsperiode");
        slettetPeriodeDto.setUttakPeriodeType(UttakPeriodeType.FORELDREPENGER);
        slettetPeriodeDto.setFom(LocalDate.now().minusDays(10));
        slettetPeriodeDto.setTom(LocalDate.now());
        dto.setSlettedePerioder(Collections.singletonList(slettetPeriodeDto));
        dto.setBekreftedePerioder(Collections.singletonList(bekreftetUttakPeriodeDto));
        return dto;
    }

    private Behandling opprettBehandling() {
        var rettighet = new OppgittRettighetEntitet(false, true, false);
        var periode_1 = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now().minusDays(10), LocalDate.now())
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        var periode_2 = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now().minusDays(20), LocalDate.now().minusDays(11))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        var fordeling = new OppgittFordelingEntitet(List.of(periode_1, periode_2), true);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(rettighet)
            .medFordeling(fordeling);
        scenario.medSøknad();
        return scenario.lagre(repositoryProvider);
    }

    private BekreftetOppgittPeriodeDto getBekreftetUttakPeriodeDto(LocalDate fom, LocalDate tom) {
        var bekreftetUttakPeriodeDto = new BekreftetOppgittPeriodeDto();
        var bekreftetperiode = OppgittPeriodeBuilder.ny()
                .medPeriode(fom, tom)
                .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
                .build();
        var bekreftetPeriodeDto = new KontrollerFaktaPeriodeLagreDto.Builder(KontrollerFaktaPeriode.ubekreftet(bekreftetperiode),
                null).build();
        bekreftetUttakPeriodeDto.setBekreftetPeriode(bekreftetPeriodeDto);
        bekreftetUttakPeriodeDto.setOrginalFom(fom);
        bekreftetUttakPeriodeDto.setOrginalTom(tom);
        return bekreftetUttakPeriodeDto;
    }
}
