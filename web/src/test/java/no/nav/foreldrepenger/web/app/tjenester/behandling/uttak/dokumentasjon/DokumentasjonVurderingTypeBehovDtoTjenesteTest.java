package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dokumentasjon;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.SYKDOM_ANNEN_FORELDER_GODKJENT;
import static no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dokumentasjon.DokumentasjonVurderingBehovDto.Vurdering.GODKJENT;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravArbeidRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.uttak.fakta.uttak.DokumentasjonVurderingBehov;
import no.nav.foreldrepenger.domene.uttak.fakta.uttak.VurderUttakDokumentasjonAksjonspunktUtleder;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;

@CdiDbAwareTest
class DokumentasjonVurderingTypeBehovDtoTjenesteTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private AktivitetskravArbeidRepository aktivitetskravArbeidRepository;
    @Inject
    private VurderUttakDokumentasjonAksjonspunktUtleder vurderUttakDokumentasjonAksjonspunktUtleder;
    @Inject
    private UttakInputTjeneste uttakInputTjeneste;

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
            .medDokumentasjonVurdering(new DokumentasjonVurdering(SYKDOM_ANNEN_FORELDER_GODKJENT))
            .build();
        var overføringUtenVurdering = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now().plusWeeks(3), LocalDate.now().plusWeeks(4).minusDays(1))
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medÅrsak(OverføringÅrsak.SYKDOM_ANNEN_FORELDER)
            .build();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(mødrekvoteUtenBehov, overføringMedVurdering, overføringUtenVurdering), true));
        scenario.medBekreftetHendelse().medAntallBarn(1).medFødselsDato(LocalDate.now());
        var behandling = scenario.lagre(repositoryProvider);

        var tjeneste = new DokumentasjonVurderingBehovDtoTjeneste(repositoryProvider.getBehandlingRepository(), uttakInputTjeneste,
            vurderUttakDokumentasjonAksjonspunktUtleder, aktivitetskravArbeidRepository);
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
