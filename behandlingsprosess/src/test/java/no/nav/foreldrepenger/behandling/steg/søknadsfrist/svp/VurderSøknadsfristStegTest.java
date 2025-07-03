package no.nav.foreldrepenger.behandling.steg.søknadsfrist.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.Søknadsfrister;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.StegTransisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;

@CdiDbAwareTest
class VurderSøknadsfristStegTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private SvangerskapspengerRepository svangerskapspengerRepository;

    @Inject
    private FørsteLovligeUttaksdatoTjeneste førsteLovligeUttaksdatoTjeneste;

    private Behandling behandling;
    private SvpHelper svpHelper;

    private VurderSøknadsfristSteg fastsettUttaksgrunnlagOgVurderSøknadsfristSteg;

    @BeforeEach
    public void setup(EntityManager em) {

        svpHelper = new SvpHelper(repositoryProvider, svangerskapspengerRepository);
        behandling = svpHelper.lagreBehandling();
        em.flush();
        em.clear();

        fastsettUttaksgrunnlagOgVurderSøknadsfristSteg = new VurderSøknadsfristSteg(førsteLovligeUttaksdatoTjeneste);
    }

    @Test
    void ingen_aksjonspunkt_når_søkt_i_tide(EntityManager em) {
        var behovFraDato = LocalDate.of(2019, Month.MAY, 5);
        var termindato = LocalDate.of(2019, Month.JULY, 1);
        svpHelper.lagreTerminbekreftelse(termindato, behandling.getId());
        svpHelper.lagreIngenTilrettelegging(behandling, behovFraDato, behovFraDato);
        var søknad = opprettSøknad(behovFraDato, behovFraDato, behandling.getId());
        repositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);
        em.flush();
        em.clear();

        // Act
        var kontekst = new BehandlingskontrollKontekst(behandling,
                behandlingRepository.taSkriveLås(behandling));
        var behandleStegResultat = fastsettUttaksgrunnlagOgVurderSøknadsfristSteg.utførSteg(kontekst);
        em.flush();
        em.clear();
        // Assert
        assertThat(behandleStegResultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.UTFØRT);
        assertThat(behandleStegResultat.getAksjonspunktListe()).isEmpty();

        var gjeldendeUttaksperiodegrense = repositoryProvider.getUttaksperiodegrenseRepository()
            .hentHvisEksisterer(behandling.getId());
        assertThat(gjeldendeUttaksperiodegrense).hasValueSatisfying(upg -> {
            assertThat(upg).isNotNull();
            assertThat(upg.getMottattDato()).isEqualTo(behovFraDato);
            assertThat(Søknadsfrister.tidligsteDatoDagytelse(upg.getMottattDato())).isEqualTo(LocalDate.of(2019, Month.FEBRUARY, 1));
        });
    }

    @Test
    void aksjonspunkt_når_søkt_for_sent(EntityManager em) {
        var behovFraDato = LocalDate.of(2019, Month.MAY, 5);
        var søknadMotattt = LocalDate.of(2019, Month.NOVEMBER, 3);
        var tidligstMottatt = LocalDate.of(2019, Month.SEPTEMBER, 3);;
        var termindato = LocalDate.of(2019, Month.JULY, 1);
        svpHelper.lagreTerminbekreftelse(termindato, behandling.getId());
        svpHelper.lagreIngenTilrettelegging(behandling, behovFraDato, tidligstMottatt);
        var søknad = opprettSøknad(behovFraDato, søknadMotattt, behandling.getId());
        repositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);
        em.flush();
        em.clear();

        // Act
        var kontekst = new BehandlingskontrollKontekst(behandling, behandlingRepository.taSkriveLås(behandling));
        var behandleStegResultat = fastsettUttaksgrunnlagOgVurderSøknadsfristSteg.utførSteg(kontekst);
        em.flush();
        em.clear();

        // Assert
        assertThat(behandleStegResultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.UTFØRT);
        assertThat(behandleStegResultat.getAksjonspunktListe()).hasSize(1);
        assertThat(behandleStegResultat.getAksjonspunktListe().get(0)).isEqualTo(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRIST);

        var gjeldendeUttaksperiodegrense = repositoryProvider.getUttaksperiodegrenseRepository()
            .hentHvisEksisterer(behandling.getId());
        assertThat(gjeldendeUttaksperiodegrense).hasValueSatisfying(upg -> {
            assertThat(upg).isNotNull();
            assertThat(upg.getMottattDato()).isEqualTo(tidligstMottatt);
            assertThat(Søknadsfrister.tidligsteDatoDagytelse(upg.getMottattDato())).isEqualTo(LocalDate.of(2019, Month.JUNE, 1));
        });
    }

    @Test
    void ingen_aksjonspunkt_revurdering_søkt_i_tide(EntityManager em) {
        var jordsmorsdato = LocalDate.of(2019, Month.MAY, 5);
        var termindato = LocalDate.of(2019, Month.DECEMBER, 1);
        svpHelper.lagreTerminbekreftelse(termindato, behandling.getId());
        svpHelper.lagreDelvisTilrettelegging(behandling, jordsmorsdato, jordsmorsdato, new BigDecimal(60));
        var søknad = opprettSøknad(termindato, jordsmorsdato, behandling.getId());
        repositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);
        em.flush();
        em.clear();

        // Act
        var kontekst = new BehandlingskontrollKontekst(behandling,
            behandlingRepository.taSkriveLås(behandling));
        var behandleStegResultat = fastsettUttaksgrunnlagOgVurderSøknadsfristSteg.utførSteg(kontekst);
        em.flush();
        em.clear();
        // Assert
        assertThat(behandleStegResultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.UTFØRT);
        assertThat(behandleStegResultat.getAksjonspunktListe()).isEmpty();

        var gjeldendeUttaksperiodegrense = repositoryProvider.getUttaksperiodegrenseRepository()
            .hentHvisEksisterer(behandling.getId());
        assertThat(gjeldendeUttaksperiodegrense).hasValueSatisfying(upg -> {
            assertThat(upg).isNotNull();
            assertThat(upg.getMottattDato()).isEqualTo(jordsmorsdato);
            assertThat(Søknadsfrister.tidligsteDatoDagytelse(upg.getMottattDato())).isEqualTo(LocalDate.of(2019, Month.FEBRUARY, 1));
        });
        behandling.avsluttBehandling();

        var revurdering = svpHelper.lagreRevurdering(behandling);
        svpHelper.lagreTerminbekreftelse(LocalDate.of(2019, Month.DECEMBER, 1), revurdering.getId());
        svpHelper.lagreDelvisTilrettelegging(revurdering, jordsmorsdato, jordsmorsdato.plusMonths(4), new BigDecimal(20));
        var søknadRevurder = opprettSøknad(termindato, jordsmorsdato.plusMonths(4), revurdering.getId());
        repositoryProvider.getSøknadRepository().lagreOgFlush(revurdering, søknadRevurder);
        em.flush();
        em.clear();
        // Act
        var rkontekst = new BehandlingskontrollKontekst(revurdering,
            behandlingRepository.taSkriveLås(revurdering));
        var rbehandleStegResultat = fastsettUttaksgrunnlagOgVurderSøknadsfristSteg.utførSteg(rkontekst);
        em.flush();
        em.clear();
        // Assert
        assertThat(behandleStegResultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.UTFØRT);
        assertThat(rbehandleStegResultat.getAksjonspunktListe()).isEmpty();

        gjeldendeUttaksperiodegrense = repositoryProvider.getUttaksperiodegrenseRepository()
            .hentHvisEksisterer(revurdering.getId());
        assertThat(gjeldendeUttaksperiodegrense).hasValueSatisfying(upg -> {
            assertThat(upg).isNotNull();
            assertThat(upg.getMottattDato()).isEqualTo(jordsmorsdato);
            assertThat(Søknadsfrister.tidligsteDatoDagytelse(upg.getMottattDato())).isEqualTo(LocalDate.of(2019, Month.FEBRUARY, 1));
        });

    }

    private SøknadEntitet opprettSøknad(LocalDate termindato, LocalDate mottattDato, Long behandlingId) {
        var søknadHendelse = repositoryProvider.getFamilieHendelseRepository()
            .opprettBuilderForSøknad(behandlingId)
            .medAntallBarn(1)
            .medFødselsDato(termindato);
        repositoryProvider.getFamilieHendelseRepository().lagreSøknadHendelse(behandlingId, søknadHendelse);

        return new SøknadEntitet.Builder()
                .medSøknadsdato(LocalDate.now())
                .medMottattDato(mottattDato)
                .medElektroniskRegistrert(true)
                .build();
    }

}
