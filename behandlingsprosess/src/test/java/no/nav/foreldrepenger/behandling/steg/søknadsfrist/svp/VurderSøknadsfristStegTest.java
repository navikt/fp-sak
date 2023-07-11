package no.nav.foreldrepenger.behandling.steg.søknadsfrist.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.Søknadsfrister;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;

@CdiDbAwareTest
class VurderSøknadsfristStegTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;

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

        fastsettUttaksgrunnlagOgVurderSøknadsfristSteg = new VurderSøknadsfristSteg(uttaksperiodegrenseRepository,
                førsteLovligeUttaksdatoTjeneste);
    }

    @Test
    void ingen_aksjonspunkt_når_søkt_i_tide(EntityManager em) {
        var behovFraDato = LocalDate.of(2019, Month.MAY, 5);
        var termindato = LocalDate.of(2019, Month.JULY, 1);
        svpHelper.lagreTerminbekreftelse(behandling, termindato);
        svpHelper.lagreIngenTilrettelegging(behandling, behovFraDato, behovFraDato);
        var søknad = opprettSøknad(behandling, behovFraDato, behovFraDato);
        repositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);
        em.flush();
        em.clear();
        var fagsak = behandling.getFagsak();

        // Act
        var kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
                behandlingRepository.taSkriveLås(behandling));
        var behandleStegResultat = fastsettUttaksgrunnlagOgVurderSøknadsfristSteg.utførSteg(kontekst);
        em.flush();
        em.clear();
        // Assert
        assertThat(behandleStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
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
        svpHelper.lagreTerminbekreftelse(behandling, termindato);
        svpHelper.lagreIngenTilrettelegging(behandling, behovFraDato, tidligstMottatt);
        var søknad = opprettSøknad(behandling, behovFraDato, søknadMotattt);
        repositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);
        em.flush();
        em.clear();
        var fagsak = behandling.getFagsak();

        // Act
        var kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
                behandlingRepository.taSkriveLås(behandling));
        var behandleStegResultat = fastsettUttaksgrunnlagOgVurderSøknadsfristSteg.utførSteg(kontekst);
        em.flush();
        em.clear();

        // Assert
        assertThat(behandleStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
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
        svpHelper.lagreTerminbekreftelse(behandling, termindato);
        svpHelper.lagreDelvisTilrettelegging(behandling, jordsmorsdato, jordsmorsdato, new BigDecimal(60));
        var søknad = opprettSøknad(behandling, termindato, jordsmorsdato);
        repositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);
        em.flush();
        em.clear();
        var fagsak = behandling.getFagsak();

        // Act
        var kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
            behandlingRepository.taSkriveLås(behandling));
        var behandleStegResultat = fastsettUttaksgrunnlagOgVurderSøknadsfristSteg.utførSteg(kontekst);
        em.flush();
        em.clear();
        // Assert
        assertThat(behandleStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
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
        svpHelper.lagreTerminbekreftelse(revurdering, LocalDate.of(2019, Month.DECEMBER, 1));
        svpHelper.lagreDelvisTilrettelegging(revurdering, jordsmorsdato, jordsmorsdato.plusMonths(4), new BigDecimal(20));
        var søknadRevurder = opprettSøknad(revurdering, termindato, jordsmorsdato.plusMonths(4));
        repositoryProvider.getSøknadRepository().lagreOgFlush(revurdering, søknadRevurder);
        em.flush();
        em.clear();
        // Act
        var rkontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
            behandlingRepository.taSkriveLås(revurdering));
        var rbehandleStegResultat = fastsettUttaksgrunnlagOgVurderSøknadsfristSteg.utførSteg(rkontekst);
        em.flush();
        em.clear();
        // Assert
        assertThat(rbehandleStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(rbehandleStegResultat.getAksjonspunktListe()).isEmpty();

        gjeldendeUttaksperiodegrense = repositoryProvider.getUttaksperiodegrenseRepository()
            .hentHvisEksisterer(revurdering.getId());
        assertThat(gjeldendeUttaksperiodegrense).hasValueSatisfying(upg -> {
            assertThat(upg).isNotNull();
            assertThat(upg.getMottattDato()).isEqualTo(jordsmorsdato);
            assertThat(Søknadsfrister.tidligsteDatoDagytelse(upg.getMottattDato())).isEqualTo(LocalDate.of(2019, Month.FEBRUARY, 1));
        });

    }



    private SøknadEntitet opprettSøknad(Behandling behandling, LocalDate termindato, LocalDate mottattDato) {
        var søknadHendelse = repositoryProvider.getFamilieHendelseRepository()
            .opprettBuilderFor(behandling)
            .medAntallBarn(1)
            .medFødselsDato(termindato);
        repositoryProvider.getFamilieHendelseRepository().lagre(behandling, søknadHendelse);

        return new SøknadEntitet.Builder()
                .medSøknadsdato(LocalDate.now())
                .medMottattDato(mottattDato)
                .medElektroniskRegistrert(true)
                .build();
    }

}
