package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.BeregningUttakTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.uttak.fakta.v2.FaktaUttakAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@CdiDbAwareTest
class FaktaUttakStegTest {

    @Inject
    @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
    private FaktaUttakSteg steg;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private FaktaUttakAksjonspunktUtleder faktaUttakAksjonspunktUtleder;
    @Inject
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    @Inject
    private MedlemTjeneste medlemTjeneste;
    @Inject
    private BeregningUttakTjeneste beregningUttakTjeneste;


    @BeforeEach
    void setup() {
        steg = new FaktaUttakSteg(faktaUttakAksjonspunktUtleder,
            new UttakInputTjeneste(repositoryProvider, new HentOgLagreBeregningsgrunnlagTjeneste(repositoryProvider.getEntityManager()), new AbakusInMemoryInntektArbeidYtelseTjeneste(),
                skjæringstidspunktTjeneste, medlemTjeneste, beregningUttakTjeneste, new YtelseFordelingTjeneste(repositoryProvider.getYtelsesFordelingRepository()),
                true));
    }

    @Test
    void skal_utlede_ap() {
        var fødselsdato = LocalDate.of(2022, 11, 16);
        var utsettelse = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(2).minusDays(1))
            .build();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(new OppgittRettighetEntitet(true, false, false, false))
            .medFordeling(new OppgittFordelingEntitet(List.of(utsettelse), true))
            .medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(fødselsdato.minusWeeks(2)).build())
            .medFødselAdopsjonsdato(fødselsdato);

        var behandling = scenario.lagre(repositoryProvider);

        var stegResultat = steg.utførSteg(
            new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), new BehandlingLås(behandling.getId())));

        assertThat(stegResultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.FAKTA_UTTAK_MANUELT_SATT_STARTDATO_ULIK_SØKNAD_STARTDATO);
    }

}
