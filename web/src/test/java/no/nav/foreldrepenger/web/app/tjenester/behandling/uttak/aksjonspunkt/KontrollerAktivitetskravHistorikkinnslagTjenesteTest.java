package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.KontrollerAktivitetskravAvklaring.IKKE_I_AKTIVITET_DOKUMENTERT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.KontrollerAktivitetskravAvklaring.IKKE_I_AKTIVITET_IKKE_DOKUMENTERT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.KontrollerAktivitetskravAvklaring.I_AKTIVITET;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AktivitetskravPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.KontrollerAktivitetskravDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.KontrollerAktivitetskravPeriodeDto;

@ExtendWith(JpaExtension.class)
class KontrollerAktivitetskravHistorikkinnslagTjenesteTest {

    @Test
    public void oppretterHistorikkinnslag(EntityManager entityManager) {
        var repository = new HistorikkRepository(entityManager);
        var adapter = new HistorikkTjenesteAdapter(repository, null, new BehandlingRepository(entityManager));
        var tjeneste = new KontrollerAktivitetskravHistorikkinnslagTjeneste(adapter);
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .lagre(new BehandlingRepositoryProvider(entityManager));

        var periode1 = new KontrollerAktivitetskravPeriodeDto();
        periode1.setBegrunnelse("begrunnelse1");
        periode1.setAvklaring(I_AKTIVITET);
        periode1.setFom(LocalDate.now());
        periode1.setTom(LocalDate.now().plusDays(1));

        var periode2 = new KontrollerAktivitetskravPeriodeDto();
        periode2.setBegrunnelse("begrunnelse2");
        periode2.setAvklaring(IKKE_I_AKTIVITET_DOKUMENTERT);
        periode2.setFom(periode1.getTom().plusDays(1));
        periode2.setTom(periode1.getTom().plusDays(2));

        var periode3 = new KontrollerAktivitetskravPeriodeDto();
        periode3.setBegrunnelse("begrunnelse3");
        periode3.setAvklaring(IKKE_I_AKTIVITET_IKKE_DOKUMENTERT);
        periode3.setFom(periode2.getTom().plusDays(1));
        periode3.setTom(periode2.getTom().plusDays(2));

        var dto = new KontrollerAktivitetskravDto();
        dto.setAvklartePerioder(List.of(periode1, periode2, periode3));
        var eksisterendePeriode1 = new AktivitetskravPeriodeEntitet(periode1.getFom(), periode1.getTom(), periode1.getAvklaring(),
            periode1.getBegrunnelse());
        //Endrer på en eksisterende periode
        var eksisterendePeriode2 = new AktivitetskravPeriodeEntitet(periode2.getFom(), periode2.getTom(), I_AKTIVITET,
            "eksisterende begrunnelse");
        tjeneste.opprettHistorikkinnslag(behandling.getId(), dto, List.of(eksisterendePeriode1, eksisterendePeriode2));

        var historikk = repository.hentHistorikk(behandling.getId());
        assertThat(historikk).hasSize(1);
        assertThat(historikk.get(0).getHistorikkinnslagDeler()).hasSize(2);
    }

    @Test
    public void ikkeOppretterHistorikkinnslagHvisIngenEndring(EntityManager entityManager) {
        var repository = new HistorikkRepository(entityManager);
        var adapter = new HistorikkTjenesteAdapter(repository, null, new BehandlingRepository(entityManager));
        var tjeneste = new KontrollerAktivitetskravHistorikkinnslagTjeneste(adapter);
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .lagre(new BehandlingRepositoryProvider(entityManager));

        var periode1 = new KontrollerAktivitetskravPeriodeDto();
        periode1.setBegrunnelse("begrunnelse1");
        periode1.setAvklaring(I_AKTIVITET);
        periode1.setFom(LocalDate.now());
        periode1.setTom(LocalDate.now().plusDays(1));

        var dto = new KontrollerAktivitetskravDto();
        dto.setAvklartePerioder(List.of(periode1));
        var eksisterendePeriode1 = new AktivitetskravPeriodeEntitet(periode1.getFom(), periode1.getTom(), periode1.getAvklaring(),
            periode1.getBegrunnelse());
        tjeneste.opprettHistorikkinnslag(behandling.getId(), dto, List.of(eksisterendePeriode1));

        var historikk = repository.hentHistorikk(behandling.getId());
        assertThat(historikk).isEmpty();
    }

    @Test
    public void oppretteHistorikkinnslagHvisBareBegrunnelseErEndret(EntityManager entityManager) {
        var repository = new HistorikkRepository(entityManager);
        var adapter = new HistorikkTjenesteAdapter(repository, null, new BehandlingRepository(entityManager));
        var tjeneste = new KontrollerAktivitetskravHistorikkinnslagTjeneste(adapter);
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .lagre(new BehandlingRepositoryProvider(entityManager));

        var nyPeriode = new KontrollerAktivitetskravPeriodeDto();
        nyPeriode.setBegrunnelse("begrunnelse1");
        nyPeriode.setAvklaring(I_AKTIVITET);
        nyPeriode.setFom(LocalDate.now());
        nyPeriode.setTom(LocalDate.now().plusDays(1));
        var dto = new KontrollerAktivitetskravDto();
        dto.setAvklartePerioder(List.of(nyPeriode));
        var eksisterendePeriode = new AktivitetskravPeriodeEntitet(nyPeriode.getFom(), nyPeriode.getTom(), nyPeriode.getAvklaring(),
            "ny begrunnelse");
        tjeneste.opprettHistorikkinnslag(behandling.getId(), dto, List.of(eksisterendePeriode));

        var historikk = repository.hentHistorikk(behandling.getId());
        assertThat(historikk).hasSize(1);
    }
}
