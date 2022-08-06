package no.nav.foreldrepenger.behandling.steg.uttak.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.Søknadsfrister;
import no.nav.foreldrepenger.behandling.steg.søknadsfrist.svp.FastsettUttaksgrunnlagOgVurderSøknadsfristSteg;
import no.nav.foreldrepenger.behandling.steg.søknadsfrist.svp.FørsteLovligeUttaksdatoTjeneste;
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
public class FastsettUttaksgrunnlagOgVurderSøknadsfristStegTest {

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

    private FastsettUttaksgrunnlagOgVurderSøknadsfristSteg fastsettUttaksgrunnlagOgVurderSøknadsfristSteg;

    @BeforeEach
    public void setup(EntityManager em) {

        svpHelper = new SvpHelper(repositoryProvider, svangerskapspengerRepository);
        behandling = svpHelper.lagreBehandling();
        em.flush();
        em.clear();

        fastsettUttaksgrunnlagOgVurderSøknadsfristSteg = new FastsettUttaksgrunnlagOgVurderSøknadsfristSteg(uttaksperiodegrenseRepository,
                førsteLovligeUttaksdatoTjeneste);
    }

    @Test
    public void ingen_aksjonspunkt_når_søkt_i_tide(EntityManager em) {
        var jordsmorsdato = LocalDate.of(2019, Month.MAY, 5);
        var mottatdato = jordsmorsdato;
        svpHelper.lagreTerminbekreftelse(behandling, LocalDate.of(2019, Month.JULY, 1));
        svpHelper.lagreIngenTilrettelegging(behandling, jordsmorsdato);
        var søknad = opprettSøknad(jordsmorsdato, mottatdato);
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
        assertThat(behandleStegResultat.getAksjonspunktListe()).hasSize(0);

        var gjeldendeUttaksperiodegrense = repositoryProvider.getUttaksperiodegrenseRepository()
            .hentHvisEksisterer(behandling.getId());
        assertThat(gjeldendeUttaksperiodegrense).hasValueSatisfying(upg -> {
            assertThat(upg).isNotNull();
            assertThat(upg.getMottattDato()).isEqualTo(mottatdato);
            assertThat(Søknadsfrister.tidligsteDatoDagytelse(upg.getMottattDato())).isEqualTo(LocalDate.of(2019, Month.FEBRUARY, 1));
        });
    }

    @Test
    public void aksjonspunkt_når_søkt_for_sent(EntityManager em) {
        var jordsmorsdato = LocalDate.of(2019, Month.MAY, 5);
        var mottatdato = LocalDate.of(2019, Month.SEPTEMBER, 3);
        svpHelper.lagreTerminbekreftelse(behandling, LocalDate.of(2019, Month.JULY, 1));
        svpHelper.lagreIngenTilrettelegging(behandling, jordsmorsdato);
        var søknad = opprettSøknad(jordsmorsdato, mottatdato);
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
            assertThat(upg.getMottattDato()).isEqualTo(mottatdato);
            assertThat(Søknadsfrister.tidligsteDatoDagytelse(upg.getMottattDato())).isEqualTo(LocalDate.of(2019, Month.JUNE, 1));
        });
    }

    private SøknadEntitet opprettSøknad(LocalDate fødselsdato, LocalDate mottattDato) {
        final var søknadHendelse = repositoryProvider.getFamilieHendelseRepository().opprettBuilderFor(behandling)
                .medAntallBarn(1)
                .medFødselsDato(fødselsdato);
        repositoryProvider.getFamilieHendelseRepository().lagre(behandling, søknadHendelse);

        return new SøknadEntitet.Builder()
                .medSøknadsdato(LocalDate.now())
                .medMottattDato(mottattDato)
                .medElektroniskRegistrert(true)
                .build();
    }

}
