package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AktivitetskravPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AktivitetskravPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.KontrollerAktivitetskravAvklaring;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.uttak.fakta.aktkrav.KontrollerAktivitetskravAksjonspunktUtleder;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.KontrollerAktivitetskravDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.KontrollerAktivitetskravPeriodeDto;

@ExtendWith(JpaExtension.class)
public class KontrollerAktivitetskravOppdatererTest {

    private KontrollerAktivitetskravOppdaterer oppdaterer;
    private BehandlingRepositoryProvider repositoryProvider;
    private KontrollerAktivitetskravAksjonspunktUtleder utleder;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var historikkinnslagTjeneste = mock(KontrollerAktivitetskravHistorikkinnslagTjeneste.class);
        utleder = mock(KontrollerAktivitetskravAksjonspunktUtleder.class);
        var uttakInputTjeneste = mock(UttakInputTjeneste.class);
        oppdaterer = new KontrollerAktivitetskravOppdaterer(repositoryProvider.getYtelsesFordelingRepository(),
            historikkinnslagTjeneste, utleder, uttakInputTjeneste);
    }

    @Test
    public void kopiereAvklaringerSomLiggerFørEndringsdato() {
        var dto = new KontrollerAktivitetskravDto();
        var avklartPeriode = new KontrollerAktivitetskravPeriodeDto();
        var avklartFom = LocalDate.of(2021, 1, 1);
        var avklartTom = LocalDate.of(2021, 2, 2);
        avklartPeriode.setFom(avklartFom);
        avklartPeriode.setTom(avklartTom);
        avklartPeriode.setBegrunnelse("ok.");
        avklartPeriode.setAvklaring(KontrollerAktivitetskravAvklaring.I_AKTIVITET);
        dto.setAvklartePerioder(List.of(avklartPeriode));
        var endringsdato = avklartFom;
        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(avklartFom)
            .medJustertEndringsdato(endringsdato)
            .build();
        var fødselsdato = LocalDate.of(2020, 12, 12);
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medAvklarteUttakDatoer(avklarteUttakDatoer)
            .lagre(repositoryProvider);
        var eksisterendeAktivitetskravPeriode = new AktivitetskravPeriodeEntitet(fødselsdato, avklartTom,
            KontrollerAktivitetskravAvklaring.IKKE_I_AKTIVITET_IKKE_DOKUMENTERT, "ok.");
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medOpprinneligeAktivitetskravPerioder(
                new AktivitetskravPerioderEntitet().leggTil(eksisterendeAktivitetskravPeriode));
        ytelsesFordelingRepository.lagre(behandling.getId(), yfBuilder.build());
        var resultat = kjørOppdaterer(dto, behandling);

        assertThat(resultat.skalUtføreAksjonspunkt()).isTrue();

        var ytelseFordelingAggregat = ytelsesFordelingRepository
            .hentAggregat(behandling.getId());
        var aktivitetskravPerioder = ytelseFordelingAggregat.getGjeldendeAktivitetskravPerioder()
            .orElseThrow()
            .getPerioder()
            .stream()
            .sorted(Comparator.comparing(AktivitetskravPeriodeEntitet::getTidsperiode))
            .collect(Collectors.toList());
        assertThat(aktivitetskravPerioder).hasSize(2);

        assertThat(aktivitetskravPerioder.get(0).getAvklaring()).isEqualTo(
            eksisterendeAktivitetskravPeriode.getAvklaring());
        assertThat(aktivitetskravPerioder.get(0).getTidsperiode().getFomDato()).isEqualTo(
            eksisterendeAktivitetskravPeriode.getTidsperiode().getFomDato());
        assertThat(aktivitetskravPerioder.get(0).getTidsperiode().getTomDato()).isEqualTo(endringsdato.minusDays(1));
        assertThat(aktivitetskravPerioder.get(0).getBegrunnelse()).isEqualTo(
            eksisterendeAktivitetskravPeriode.getBegrunnelse());

        assertThat(aktivitetskravPerioder.get(1).getAvklaring()).isEqualTo(
            dto.getAvklartePerioder().get(0).getAvklaring());
        assertThat(aktivitetskravPerioder.get(1).getTidsperiode().getFomDato()).isEqualTo(
            dto.getAvklartePerioder().get(0).getFom());
        assertThat(aktivitetskravPerioder.get(1).getTidsperiode().getTomDato()).isEqualTo(
            dto.getAvklartePerioder().get(0).getTom());
        assertThat(aktivitetskravPerioder.get(1).getBegrunnelse()).isEqualTo(
            dto.getAvklartePerioder().get(0).getBegrunnelse());
    }

    @Test
    public void holdeAksjonspunktÅpentHvisIkkeAllePerioderErAvklart() {
        when(utleder.utledFor(any())).thenReturn(List.of(AksjonspunktDefinisjon.KONTROLLER_AKTIVITETSKRAV));

        var dto = new KontrollerAktivitetskravDto();
        var avklartPeriode = new KontrollerAktivitetskravPeriodeDto();
        var avklartFom = LocalDate.of(2021, 1, 1);
        var avklartTom = LocalDate.of(2021, 2, 2);
        avklartPeriode.setFom(avklartFom);
        avklartPeriode.setTom(avklartTom);
        avklartPeriode.setBegrunnelse("ok.");
        avklartPeriode.setAvklaring(KontrollerAktivitetskravAvklaring.I_AKTIVITET);
        dto.setAvklartePerioder(List.of(avklartPeriode));
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medDefaultOppgittDekningsgrad()
            .lagre(repositoryProvider);
        var resultat = kjørOppdaterer(dto, behandling);

        assertThat(resultat.skalUtføreAksjonspunkt()).isFalse();
    }

    private OppdateringResultat kjørOppdaterer(KontrollerAktivitetskravDto dto, Behandling behandling) {
        var param = new AksjonspunktOppdaterParameter(behandling, null, null, "");
        return oppdaterer.oppdater(dto, param);
    }
}
