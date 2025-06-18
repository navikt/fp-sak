package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingRevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.BeregningRevurderingTestUtil;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndring;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingHistorikk;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjenesteFelles;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingskontrollTjenesteImpl;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingGrunnlagRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.typer.PersonIdent;

@CdiDbAwareTest
class RevurderingTjenesteImplTest {

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @Inject
    private BeregningRevurderingTestUtil revurderingTestUtil;

    @Inject
    private BehandlingskontrollServiceProvider serviceProvider;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BehandlingGrunnlagRepositoryProvider grunnlagRepositoryProvider;

    @Inject
    @FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
    private RevurderingEndring revurderingEndringES;
    @Inject
    @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
    private RevurderingEndring revurderingEndringFP;

    @Inject
    private VergeRepository vergeRepository;
    @Inject
    private BehandlingRevurderingTjeneste behandlingRevurderingTjeneste;


    @Test
    void skal_opprette_historikkinnslag_for_registrert_fødsel() {
        var fødselsdato = LocalDate.parse("2017-09-04");
        var barn = Collections.singletonList(byggBaby(fødselsdato));
        var behandling = opprettRevurderingsKandidat();
        tjeneste().opprettHistorikkinnslagForFødsler(behandling, barn);

        var historikkinnslag = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer()).getFirst();

        assertThat(historikkinnslag.getTittel()).isEqualTo("Opplysning om fødsel");
        assertThat(historikkinnslag.getLinjer().getFirst().getTekst()).contains("Fødselsdato: 04.09.2017");
        assertThat(historikkinnslag.getLinjer().get(1).getTekst()).contains("Antall barn: 1");
    }

    private RevurderingHistorikk tjeneste() {
        return new RevurderingHistorikk(repositoryProvider.getHistorikkinnslagRepository());
    }

    @Test
    void skal_opprette_korrekt_historikkinnslag_for_trillingfødsel_over_2_dager() {
        var fødselsdato1 = LocalDate.parse("2017-09-04");
        var fødselsdato2 = LocalDate.parse("2017-09-05");
        List<FødtBarnInfo> barn = new ArrayList<>();
        barn.add(byggBaby(fødselsdato1));
        barn.add(byggBaby(fødselsdato1));
        barn.add(byggBaby(fødselsdato2));

        var behandling = opprettRevurderingsKandidat();
        tjeneste().opprettHistorikkinnslagForFødsler(behandling, barn);

        var historikkinnslag = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer()).getFirst();

        assertThat(historikkinnslag.getTittel()).isEqualTo("Opplysning om fødsel");
        assertThat(historikkinnslag.getLinjer().getFirst().getTekst()).contains("Fødselsdato: 04.09.2017, 05.09.2017");
        assertThat(historikkinnslag.getLinjer().get(1).getTekst()).contains("Antall barn: 3");
    }

    @Test
    void skal_opprette_revurdering_for_foreldrepenger() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingVedtak()
                .medVedtakstidspunkt(LocalDateTime.now())
                .medVedtakResultatType(VedtakResultatType.INNVILGET);

        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_ADOPSJONSDOKUMENTAJON,
                BehandlingStegType.KONTROLLER_FAKTA);
        scenario.medBehandlingstidFrist(LocalDate.now().minusDays(5));
        var behandlingSomSkalRevurderes = scenario.lagre(repositoryProvider);
        repositoryProvider.getOpptjeningRepository()
                .lagreOpptjeningsperiode(behandlingSomSkalRevurderes, LocalDate.now().minusYears(1), LocalDate.now(),
                        false);
        revurderingTestUtil.avsluttBehandling(behandlingSomSkalRevurderes);

        var behandlingskontrollTjeneste = new BehandlingskontrollTjenesteImpl(serviceProvider);
        var revurderingTjenesteFelles = new RevurderingTjenesteFelles(repositoryProvider, behandlingRevurderingTjeneste, behandlingskontrollTjeneste);
        RevurderingTjeneste revurderingTjeneste = new RevurderingTjenesteImpl(repositoryProvider,
                grunnlagRepositoryProvider, iayTjeneste, revurderingEndringES, revurderingTjenesteFelles, vergeRepository);

        // Act
        var revurdering = revurderingTjeneste
                .opprettAutomatiskRevurdering(behandlingSomSkalRevurderes.getFagsak(),
                        BehandlingÅrsakType.RE_HENDELSE_FØDSEL, new OrganisasjonsEnhet("1234", "Test"));

        // Assert
        assertThat(revurdering.getFagsak()).isEqualTo(behandlingSomSkalRevurderes.getFagsak());
        assertThat(revurdering.getBehandlingÅrsaker().get(0).getBehandlingÅrsakType()).isEqualTo(
                BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        assertThat(revurdering.getType()).isEqualTo(BehandlingType.REVURDERING);
        assertThat(revurdering.getAksjonspunkter()).isEmpty();
        assertThat(revurdering.getBehandlingstidFrist()).isNotEqualTo(
                behandlingSomSkalRevurderes.getBehandlingstidFrist());
    }

    private Behandling opprettRevurderingsKandidat() {

        var terminDato = LocalDate.now().minusDays(70);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknadHendelse()
                .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                        .medTermindato(terminDato).medUtstedtDato(terminDato))
                .medAntallBarn(1);
        scenario.medBekreftetHendelse()
                .medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                        .medTermindato(terminDato).medUtstedtDato(terminDato.minusDays(40)))
                .medAntallBarn(1);
        return scenario.lagre(repositoryProvider);
    }

    private FødtBarnInfo byggBaby(LocalDate fødselsdato) {
        return new FødtBarnInfo.Builder()
                .medFødselsdato(fødselsdato)
                .medIdent(PersonIdent.fra("12345678901"))
                .build();
    }
}
