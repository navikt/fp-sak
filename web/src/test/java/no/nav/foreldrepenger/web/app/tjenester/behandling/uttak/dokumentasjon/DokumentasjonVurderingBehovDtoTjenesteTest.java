package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dokumentasjon;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.BeregningUttakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.HendelseVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.*;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.uttak.fakta.uttak.DokumentasjonVurderingBehov;
import no.nav.foreldrepenger.domene.uttak.fakta.uttak.VurderUttakDokumentasjonAksjonspunktUtleder;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dokumentasjon.DokumentasjonVurderingBehovDto.Vurdering.GODKJENT;
import static org.assertj.core.api.Assertions.assertThat;

@CdiDbAwareTest
class DokumentasjonVurderingBehovDtoTjenesteTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private VurderUttakDokumentasjonAksjonspunktUtleder vurderUttakDokumentasjonAksjonspunktUtleder;
    @Inject
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    @Inject
    private MedlemTjeneste medlemTjeneste;
    @Inject
    private BeregningUttakTjeneste beregningUttakTjeneste;

    @Test
    void skal_lage_dtos() {
        var mødrekvoteUtenBehov = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now(), LocalDate.now().plusWeeks(2).minusDays(1))
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();
        var overføringMedVurdering = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now().plusWeeks(2), LocalDate.now().plusWeeks(3).minusDays(1))
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medÅrsak(OverføringÅrsak.SYKDOM_ANNEN_FORELDER)
            .medDokumentasjonVurdering(DokumentasjonVurdering.SYKDOM_ANNEN_FORELDER_GODKJENT)
            .build();
        var overføringUtenVurdering = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now().plusWeeks(3), LocalDate.now().plusWeeks(4).minusDays(1))
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medÅrsak(OverføringÅrsak.SYKDOM_ANNEN_FORELDER)
            .build();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(mødrekvoteUtenBehov, overføringMedVurdering, overføringUtenVurdering), true))
            .medBekreftetHendelse(FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.BEKREFTET).medFødselsDato(LocalDate.now()));
        var behandling = scenario.lagre(repositoryProvider);

        var entityManager = repositoryProvider.getEntityManager();
        var uttakInputTjeneste = new UttakInputTjeneste(repositoryProvider,
            new HentOgLagreBeregningsgrunnlagTjeneste(entityManager), new AbakusInMemoryInntektArbeidYtelseTjeneste(),
            skjæringstidspunktTjeneste, medlemTjeneste, beregningUttakTjeneste);

        var tjeneste = new DokumentasjonVurderingBehovDtoTjeneste(repositoryProvider.getBehandlingRepository(), uttakInputTjeneste,
            vurderUttakDokumentasjonAksjonspunktUtleder);
        var resultat = tjeneste.lagDtos(new UuidDto(behandling.getUuid()));
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).vurdering()).isEqualTo(GODKJENT);
        assertThat(resultat.get(0).fom()).isEqualTo(overføringMedVurdering.getFom());
        assertThat(resultat.get(0).tom()).isEqualTo(overføringMedVurdering.getTom());
        assertThat(resultat.get(0).årsak()).isEqualTo(DokumentasjonVurderingBehov.Behov.Årsak.SYKDOM_ANNEN_FORELDER);
        assertThat(resultat.get(0).type()).isEqualTo(DokumentasjonVurderingBehov.Behov.Type.OVERFØRING);

        assertThat(resultat.get(1).vurdering()).isNull();
        assertThat(resultat.get(1).fom()).isEqualTo(overføringUtenVurdering.getFom());
        assertThat(resultat.get(1).tom()).isEqualTo(overføringUtenVurdering.getTom());
        assertThat(resultat.get(1).årsak()).isEqualTo(DokumentasjonVurderingBehov.Behov.Årsak.SYKDOM_ANNEN_FORELDER);
        assertThat(resultat.get(1).type()).isEqualTo(DokumentasjonVurderingBehov.Behov.Type.OVERFØRING);
    }

}
